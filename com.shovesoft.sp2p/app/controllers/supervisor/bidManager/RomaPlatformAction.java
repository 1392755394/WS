package controllers.supervisor.bidManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.google.zxing.BarcodeFormat;
import com.shove.code.Qrcode;
import models.t_roma_bids;
import models.t_roma_invests;
import play.Logger;
import play.db.jpa.Blob;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import utils.DateUtil;
import utils.ErrorInfo;
import utils.PageBean;
import business.DataSafety;
import business.DealDetail;
import business.TemplateStation;
import business.User;
import business.Bid.Purpose;
import constants.Constants;
import constants.DealType;
import constants.Templets;
import controllers.supervisor.SupervisorController;

public class RomaPlatformAction extends SupervisorController {

	// 发布一个标
	public static void creatRomaBid(long result) {

		ErrorInfo error = new ErrorInfo();
		/* 借款用途 */
		List<Purpose> purpose = Purpose.queryLoanPurpose(error, true);

		render(purpose, result);
	}

	// 确认发布一个标

	public static void sumitCreatRomaBid(t_roma_bids romaBid) {
		ErrorInfo error = new ErrorInfo();
		t_roma_bids romaBids = null;
		try {
			romaBids = GenericModel.find("roma_status=?", 1).first();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			error.msg = "出错!";
			e.printStackTrace();
			creatRomaBid(-1);
		}

		EntityManager em = JPA.em();
		String sql3 = "update t_roma_bids set roma_status = ? where id = ?";
		Query querys = em.createQuery(sql3).setParameter(1, 0)
				.setParameter(2, romaBids.id);
		int rows = 0;
		try {
			rows = querys.executeUpdate();

		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
		}
		if (rows == 0) {
			flash.error("更改状态失败!");
			creatRomaBid(-1);
		}

		t_roma_bids rom = new t_roma_bids();
		rom.roma_status = 1;
		rom.roma_title = romaBid.roma_title;
		rom.roma_apr = romaBid.roma_apr;
		rom.roma_audit_filename = romaBid.roma_audit_filename;
		rom.roma_description = romaBid.roma_description;
		rom.roma_image_filename = romaBid.roma_image_filename;
		rom.roma_period = romaBid.roma_period;
		rom.roma_loan_purpose_id = romaBid.roma_loan_purpose_id;
		rom.roma_repaymentType = romaBid.roma_repaymentType;
		Date data = new Date();
		SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");
		rom.roma_bid_no = "J" + dateformat.format(new Date());
		rom.roma_mark = "D" + romaBid.roma_period
				+ dateformat.format(new Date()) + romaBid.roma_apr;
		rom.roma_time = data;
		try {
			rom.save();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			JPA.setRollbackOnly();
			e.printStackTrace();
			creatRomaBid(-1);
		}

		/**
		 * 生成二维码代码
		 */
		String uuid = UUID.randomUUID().toString();
		Qrcode code = new Qrcode();
		Blob blob = new Blob();
		String str = Constants.BASE_URL + "front/roma_invest/detailsRomaBid";

		try {
			Qrcode.create(str, BarcodeFormat.QR_CODE, 100, 100,
					new File(Blob.getStore(), uuid).getAbsolutePath(), "png");
		} catch (Exception e) {
			Logger.info("标->创建二维码图片失败" + e.getMessage());
			error.code = -28;
			error.msg = "创建二维码图片失败!";
			JPA.setRollbackOnly();
			creatRomaBid(-1);
		}

		/* 保存二维码标识 */
		Query query = JPA.em().createQuery(
				"update t_roma_bids set roma_qc_code = ? where id = ?");
		query.setParameter(1, uuid);
		query.setParameter(2, rom.id);

		try {
			error.code = query.executeUpdate();
		} catch (Exception e) {
			Logger.info("标->保存二维码标识" + e.getMessage());
			error.code = -29;
			error.msg = "创建二维码图片失败!";
			JPA.setRollbackOnly();
			creatRomaBid(-1);
		}

		if (error.code < 1) {
			error.code = -30;
			error.msg = "创建二维码图片失败!";
			JPA.setRollbackOnly();
			flash.error(error.msg);
			creatRomaBid(-1);
		}
		creatRomaBid(2);

	}

	// 查看今日需要放款的标

	public static void todayLoan() {

		t_roma_bids trb = null;
		try {
			trb = GenericModel.find("roma_status=?", 1).first();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long priod = trb.roma_period;
		List<t_roma_invests> tri = null;
		// 查询 某个日前前状态为0的数据
		String sql = "to_days(now())-to_days(roma_time)>= ? and roma_invests_state=?";

		try {
			tri = GenericModel.find(sql, priod, 0).fetch();
			// System.out.print(tri.get(0).id);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			flash.error("无");
			e.printStackTrace();
		}

		/**
		 * 组装分页器
		 * 
		 * **/
		PageBean<t_roma_invests> pageBean = new PageBean<t_roma_invests>();
		pageBean.totalCount = 20;
		render(tri, pageBean);
	}

	public static void ajaxLoan() {

		ErrorInfo error = new ErrorInfo();

		// 找出来今天要处理的标
		t_roma_bids trb = null;
		try {
			trb = GenericModel.find("roma_status=?", 1).first();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			error.msg = "查询时出错";
			e.printStackTrace();
		}
		long priod = trb.roma_period;
		List<t_roma_invests> tris = null;
		// 查询 某个日前前状态为0的数据
		String sql = "to_days(now())-to_days(roma_time)>= ? and roma_invests_state=?";

		try {
			tris = GenericModel.find(sql, priod, 0).fetch();
			// System.out.print(tri.get(0).id);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			error.msg = "查询时出错";
			e.printStackTrace();
		}

		for (t_roma_invests tri : tris) {

			// 查询账户防止误差
			Map<String, Double> forDetail = DealDetail.queryUserFund(
					tri.user_id, error);
			if (error.code < 0) {
				error.code = -1;
			}

			DataSafety data = new DataSafety();

			if (Constants.CHECK_CODE) {
				data.id = tri.user_id;
				if (!data.signCheck(error)) {
					JPA.setRollbackOnly();
					flash.error(error.msg);
					todayLoan();
				}
			}
			double balance = forDetail.get("user_amount");
			double freeze = forDetail.get("freeze");
			double receiveAmount = forDetail.get("receive_amount");
			balance = balance + tri.roma_loan_amount;
			DealDetail.updateUserBalance(tri.user_id, balance, freeze, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.msg = "对不起，更新账户出错!";
			}

			DealDetail detail = new DealDetail(tri.user_id,
					DealType.RECHARGE_HAND, tri.roma_loan_amount, 1, balance,
					freeze, receiveAmount, "新手标回款");

			detail.addDealDetail(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

			}
			// 添加充值记录
			User.sequence(tri.user_id, 0, tri.roma_loan_amount,
					Constants.ORDINARY_NEWUSER, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.msg = "对不起，出错!";
			}

			data.id = tri.user_id;
			data.updateSign(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

			}

			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.msg = "对不起，出错!";
			}
			EntityManager em = JPA.em();
			String sqls = "update t_roma_invests set roma_invests_state =? where id=?";
			Query query = em.createQuery(sqls).setParameter(1, 1)
					.setParameter(2, tri.id);
			int rows = 0;
			try {
				rows = query.executeUpdate();
			} catch (Exception e) {
				JPA.setRollbackOnly();
				e.printStackTrace();
				Logger.info("修改密码时时,更新保存用户密码时：" + e.getMessage());
				error.msg = "对不起，由于平台出现故障，此次密码修改保存失败！";
			}
			if (rows != 0) {
				error.code = -1;
				error.msg = "成功";
			}

			String date = DateUtil.dateToString(new Date());
			// 发送站内信 [date]财务人员给您充值了￥[money]元，备注：[remark]
			TemplateStation station = new TemplateStation();
			station.id = Templets.S_HAND_RECHARGE;

			if (station.status) {
				String mContent = date + "|新手标回款|" + tri.roma_loan_amount
						+ "|东邦易贷";

				TemplateStation.addMessageTask(tri.user_id, station.title,
						mContent);
			}
			flash.error(error.msg);

		}

		todayLoan();

	}
}
