package business;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.cache.Cache;
import play.db.helper.JpaHelper;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import utils.DateUtil;
import utils.ErrorInfo;
import utils.Arith;
import constants.Constants;
import constants.DealType;
import constants.IPSConstants;
import constants.OptionKeys;
import constants.Templets;
import constants.UserEvent;
import constants.Constants.RechargeType;
import models.t_system_options;
import models.t_user_vip_records;
import models.v_user_for_details;

public class Vip implements Serializable{

	public long id;
	public long userId;
	public Date time;
	public Date startTime;
	public Date endTime;
	public boolean status;
	public int serviceTime;
	
	/**
	 * 
	 * @param time 申请vip的时长，月为单位
	 * @param user
	 * @param info
	 * @return
	 */
	public int renewal(User user, ErrorInfo error) {
		error.clear();
		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();
		
		int vipMinTimeType = backstageSet.vipMinTimeType;
		int vipMinTime = backstageSet.vipMinTimeLength;
		int vipTimeType = backstageSet.vipTimeType; 
		double vipFee = backstageSet.vipFee;
		
		if(vipMinTimeType != 1) {
			vipMinTime *= 12;
		}
		
		if(this.serviceTime < vipMinTime) {
			error.code = -3;
			error.msg = "vip开通不能少于最少时长";
			
			return error.code;
		}
		
		int timeLen = this.serviceTime;
		
		if(vipTimeType != 1) {
			serviceTime /= 12;
		}
		
		double fee = Arith.mul(vipFee, serviceTime);
		fee = fee*backstageSet.vipDiscount/10;
		fee = Arith.round(fee, 2);
		
		DataSafety data = new DataSafety();
		
		data.id = user.id;
		
		if(!data.signCheck(error)){
			
			JPA.setRollbackOnly();
			
			return error.code;
		}
		
		v_user_for_details forDetail = DealDetail.queryUserBalance(user.id, error);
		
		if(error.code < 0) {
			
			return error.code;
		}
		
		double balance = forDetail.user_amount;
		
		if (Constants.IPS_ENABLE) {
			if (fee > user.balance2) {
				error.code = -4;
				error.msg = "对不起，您可用余额不足";
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("rechargeType", RechargeType.VIP);
				map.put("serviceTime", timeLen);
				map.put("fee", fee);
				Cache.set("rechargePay"+user.id, map, IPSConstants.CACHE_TIME);
				
				return error.code;
			}
		} else if (fee > balance) {
			error.code = -4;
			error.msg = "对不起，您可用余额不足";
			
			return error.code;
		}
		
		t_user_vip_records vipRecord = new t_user_vip_records();
		t_user_vip_records record = null;
		
		int rows = 0; 
		
		if(user.vipStatus) {
			try{
				record = GenericModel.find("user_id = ? and status = 1", user.id).first();
				rows = JpaHelper.execute("update t_user_vip_records set status = 0 where user_id = ? and status = 1")
				.setParameter(1,user.id).executeUpdate();
			}catch(Exception e) {
				e.printStackTrace();
				Logger.info("申请vip时，查询系统设置中的vip设置时"+e.getMessage());
				error.code = -1;
				error.msg = "申请vip失败";
				
				return error.code;
			}
			
			if(rows == 0) {
				JPA.setRollbackOnly();
				error.code = -1;
				error.msg = "数据未更新，vip申请失败";
				
				return error.code;
			}
			
			vipRecord.start_time = record.expiry_date;
			vipRecord.expiry_date = DateUtil.dateAddMonth(record.expiry_date, vipTimeType !=1 ? this.serviceTime*12 : this.serviceTime);
		}else{
			vipRecord.start_time = new Date();
			vipRecord.expiry_date = DateUtil.dateAddMonth(new Date(), vipTimeType !=1 ? this.serviceTime*12 : this.serviceTime);
		}
		
		vipRecord.user_id = user.id;
		vipRecord.time = new Date();
		vipRecord.service_fee = fee;
		vipRecord.status = true;
		
		try{
			JpaHelper.execute("update t_user_vip_records set status = 0 where user_id = ? and status = 1").setParameter(1,user.id).executeUpdate();
			vipRecord.save();
			rows = JpaHelper.execute("update t_users set vip_status = true where id = ?", user.id).executeUpdate();
		}catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("申请vip时，查询系统设置中的vip设置时"+e.getMessage());
			error.code = -5;
			error.msg = "申请vip失败";
			
			return error.code;
		}
		
		if(rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";
			
			return error.code;
		}
		
		if (Constants.IPS_ENABLE) {
			error.code = DealDetail.minusUserFund2(user.id, fee);
		} else {
			error.code = DealDetail.minusUserFund(user.id, fee);
		}
		
		if(error.code < 0) {
			JPA.setRollbackOnly();
			
			return error.code;
		}
		
		DealDetail dealDetail = null;
		forDetail = DealDetail.queryUserBalance(user.id, error);
		
		/* 添加交易记录 */
		if (Constants.IPS_ENABLE) {
			dealDetail = new DealDetail(user.id, DealType.CHARGE_VIP, fee,
					vipRecord.id, forDetail.user_amount2, forDetail.freeze, forDetail.receive_amount, "vip扣费");

			dealDetail.addDealDetail2(error);
		} else {
			dealDetail = new DealDetail(user.id, DealType.CHARGE_VIP, fee,
					vipRecord.id, forDetail.user_amount, forDetail.freeze, forDetail.receive_amount, "vip扣费");

			dealDetail.addDealDetail(error);
		}
		
		if(error.code < 0) {
			JPA.setRollbackOnly();
			
			return error.code;
		}
		
		data.id = user.id;
		data.updateSign(error);
		
		if(error.code < 0) {
			JPA.setRollbackOnly();
			
			return error.code;
		}
		
		DealDetail.addPlatformDetail(DealType.VIP_FEE, vipRecord.id, user.id, -1,
				DealType.ACCOUNT, fee, 1, "vip费用", error);
		
		if(error.code < 0) {
			JPA.setRollbackOnly();
			
			return error.code;
		}
		
		DealDetail.userEvent(this.id, UserEvent.VIP, "申请vip", error);
		
		if(error.code < 0) {
			JPA.setRollbackOnly();
			
			return error.code;
		}
		
		//vip申请站内信
		TemplateStation station = new TemplateStation();
		station.id = Templets.M_VIP_SUCCESS;
		
		if(station.status) {
			TemplateStation.addMessageTask(userId, station.title, station.content.replace("vipFee", fee+""));
		}
		
		//发送邮件
		TemplateEmail email = new TemplateEmail();
		email.id = Templets.E_VIP_SUCCESS;
		
		if(email.status) {
			TemplateEmail.addEmailTask(user.email, email.title, email.content.replace("vipFee", fee+""));
		}
		
		//发送短信
		TemplateSms sms = new TemplateSms();
		
		if(StringUtils.isNotBlank(user.mobile)) {
			sms.id = Templets.S_VIP_SUCCESS;
			
			if(sms.status) {
				TemplateSms.addSmsTask(user.mobile, sms.content.replace("vipFee", fee+""));
			}
		}
		
		if (Constants.IPS_ENABLE) {
			user.balance2 = forDetail.user_amount2 - fee;
		} else {
			user.balance = forDetail.user_amount - fee;
		}
		
		user.vipStatus = true;
		
		User.setCurrUser(user);
		
		error.code = 0;
		error.msg = "申请vip成功！";
		
		return error.code;
	}
	
	/**
	 * 根据时间算出vip费用
	 * @param info
	 * @return
	 */
	public double vipMoney(ErrorInfo error) {
		error.clear();
		
		String sql = "select _value from t_system_options where _key = ? or _key =? or _key = ? order by id";
		List<String> keys = null;
		
		try {
			keys = GenericModel.find(sql, OptionKeys.VIP_MIN_TIME, OptionKeys.VIP_FEE, 
					OptionKeys.VIP_TIME_TYPE).fetch();
		} catch(Exception e) {
			e.printStackTrace();
			Logger.info("申请vip时，查询系统设置中的vip设置时"+e.getMessage());
			error.code = -1;
			error.msg = "申请vip失败";
			
			return error.code;
		}
		
		if(keys == null || keys.size() == 0) {
			error.code = -2;
			error.msg = "读取系统参数失败";
			
			return error.code;
		}
		
		if(keys.get(2).equals(Constants.YEAR+"")) {
			this.serviceTime *= 12;
		}
		
		int vipMinTime = Integer.parseInt(keys.get(1));
		
		if(this.serviceTime <= vipMinTime) {
			error.code = -3;
			error.msg = "至少开通"+vipMinTime+"月";
			
			return error.code;
		}
		
		double vipFee = Double.parseDouble(keys.get(0));
		double fee = Arith.mul(vipFee, serviceTime);
		
		error.code = 0;
		
		return fee;
	}
	
	/**
	 * 查询用户的vip记录
	 * @param userId
	 * @return
	 */
	public static List<t_user_vip_records> queryVipRecord(long userId, ErrorInfo error) {
		error.clear();
		
		List<t_user_vip_records> vipRecords = null;
		
		try {
			vipRecords = GenericModel.find("user_id = ?", userId).fetch();
		} catch(Exception e) {
			e.printStackTrace();
			Logger.info("查询vip记录时："+e.getMessage());
			
			error.code = -1;
			error.msg = "查询用户的vip记录时出现异常";
			
			return null;
		}
		
		error.code = 0;
		
		return vipRecords;
	}
	
	/**
	 * 定期处理会员过期
	 */
	public static void vipExpiredJob() {
		String sql = "select user_id from t_user_vip_records where status = 1 and expiry_date <= NOW()";
		List<Long> user_ids = null;
		
		try{
			user_ids = GenericModel.find(sql).fetch();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("定时任务处理过期vip时（查询）："+e.getMessage());
			
			return;
		}
		
		if(user_ids == null || user_ids.size() == 0) {
			return;
		}
		
		String idStr = StringUtils.join(user_ids, ",");
		
		String updateSql = "update t_user_vip_records set status = 0 where user_id in ( "+idStr+" )";
			
		int rows = 0; 
		
		try{
			rows = JpaHelper.execute(updateSql).executeUpdate();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("定时任务处理过期vip时（更新vip记录）："+e.getMessage());
			
			return;
		}
		
		if(rows == 0) {
			return;
		}
		
		String updateSql2 = "update t_users set vip_status = 0 where id in ( "+idStr+" )";
		
		try{
			JpaHelper.execute(updateSql2).executeUpdate();
		}catch(Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("定时任务处理过期vip时（更新用户vip状态）："+e.getMessage());
			
			return;
		}
	}
	
}
