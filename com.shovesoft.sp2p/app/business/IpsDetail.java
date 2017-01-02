package business;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;

import com.shove.Convert;

import constants.IPSConstants;
import constants.IPSConstants.IPSOperation;
import constants.IPSConstants.Status;
import models.t_bids;
import models.t_invest_transfers;
import models.t_ips_details;
import models.t_user_recharge_details;
import models.t_user_withdrawals;
import play.Logger;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import utils.ErrorInfo;
import utils.PageBean;

/**
 * 资金托管交易
 * @author lzp
 * @version 6.0
 * @created 2014-10-23
 */
public class IpsDetail {
	public long id;
	public long _id;
	public String merBillNo;
	public String userName;
	public Date time;
	public int type;
	public int status;
	public String memo;
	
	public void setId(long id) {
		t_ips_details detail = null;

		try {
			detail = GenericModel.findById(id);
		} catch (Exception e) {
			this._id = -1;
			Logger.error(e.getMessage());

			return;
		}

		if (null == detail) {
			this._id = -1;

			return;
		}

		setInfomation(detail);
	}

	public long getId() {
		return _id;
	}
	
	/**
	 * 填充数据
	 * @param detail
	 */
	private void setInfomation(t_ips_details detail) {
		if (null == detail) {
			this._id = -1;

			return;
		}
		
		this._id = detail.id;
		this.merBillNo = detail.mer_bill_no;
		this.userName = detail.user_name;
		this.time = detail.time;
		this.type = detail.type;
		this.status = detail.status;
		this.memo = detail.memo;
	}
	
	/**
	 * 查询交易记录
	 * @param currPage
	 * @param pageSize
	 * @param mer_bill_no
	 * @param userName
	 * @param type
	 * @param beginTime
	 * @param endTime
	 * @param status
	 * @param error
	 * @return
	 */
	public static PageBean<t_ips_details> queryDetails(int currPage, int pageSize, String merBillNo, String userName, int type, 
			Date beginTime, Date endTime, int status, ErrorInfo error) {
		error.clear();
		
		if (currPage < 1) {
			currPage = 1;
		}

		if (pageSize < 1) {
			pageSize = 10;
		}

		StringBuffer condition = new StringBuffer("1=1");
		Map<String, Object> conditions = new HashMap<String, Object>();
		List<Object> params = new ArrayList<Object>();

		if (StringUtils.isNotBlank(merBillNo)) {
			condition.append(" and mer_bill_no = ?");
			conditions.put("merBillNo", merBillNo);
			params.add(merBillNo);
		}
		
		if (StringUtils.isNotBlank(userName)) {
			condition.append(" and user_name = ?");
			conditions.put("userName", userName);
			params.add(userName);
		}
		
		if (type != 0) {
			condition.append(" and type = ?");
			conditions.put("type", type);
			params.add(type);
		}
		
		if (beginTime != null) {
			condition.append(" and time >= ?");
			conditions.put("beginTime", beginTime);
			params.add(beginTime);
		}
		
		if (endTime != null) {
			condition.append(" and time <= ?");
			conditions.put("endTime", endTime);
			params.add(endTime);
		}

		if (status != 0) {
			condition.append(" and status = ?");
			conditions.put("status", status);
			params.add(status);
		}

		int count = 0;
		List<t_ips_details> page = null;

		try {
			count = (int) GenericModel.count(condition.toString(), params.toArray());
			page = GenericModel.find(condition.append(" order by time desc").toString(), params.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";

			return null;
		}

		PageBean<t_ips_details> bean = new PageBean<t_ips_details>();
		bean.pageSize = pageSize;
		bean.currPage = currPage;
		bean.totalCount = count;
		bean.page = page;
		bean.conditions = conditions;
		
		error.code = 0;

		return bean;
	}
	
	/**
	 * 新增交易
	 * @param error
	 */
	public void create(ErrorInfo error) {
		error.clear();
		
		if (StringUtils.isBlank(this.merBillNo)) {
			error.code = -1;
			error.msg = "订单号不能为空";
			
			return;
		}
		
		if (this.time == null) {
			error.code = -1;
			error.msg = "交易时间不能为空";
			
			return;
		}
		
		if (this.type != IPSOperation.REGISTER_SUBJECT && this.type != IPSOperation.REGISTER_CREDITOR && 
			this.type != IPSOperation.REGISTER_CRETANSFER && this.type != IPSOperation.DO_DP_TRADE && 
			this.type != IPSOperation.REPAYMENT_NEW_TRADE && this.type != IPSOperation.DO_DW_TRADE && 
			this.type != IPSOperation.TRANSFER_ONE && this.type != IPSOperation.TRANSFER_FOUR) {
			error.code = -1;
			error.msg = "交易类型有误";
			
			return;
		}
		
		if (status != 1 && status != 2) {
			error.code = -1;
			error.msg = "交易状态有误";
		}
		
		t_ips_details detail = new t_ips_details();
		detail.mer_bill_no = this.merBillNo;
		detail.user_name = this.userName;
		detail.time = this.time;
		detail.type = this.type;
		detail.status = this.status;
		detail.memo = this.memo;
		
		try {
			detail.save();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			Logger.error(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";

			return;
		}
		
		error.code = 0;
		error.msg = "交易添加成功";
	}
	
	/**
	 * 更新状态
	 * @param merBillNo
	 * @param status
	 */
	public static void updateStatus(String merBillNo, int status, ErrorInfo error) {
		error.clear();
		
		String sql = "update t_ips_details set status = ? where mer_bill_no = ?";
		EntityManager em = JPA.em();
		Query query = em.createQuery(sql).setParameter(1, status).setParameter(2, merBillNo);
		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			Logger.info(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return;
		}
		
		if (rows == 0) {
			error.code = -1;
			error.msg = "数据未更新";
			
			return;
		}
		
		error.code = 0;
	}
	
	/**
	 * 更新状态和备注
	 * @param merBillNo
	 * @param status
	 * @param memo
	 * @param error
	 */
	public static void updateStatusAndMemo(String merBillNo, int status, String memo, ErrorInfo error) {
		error.clear();
		
		String sql = "update t_ips_details set status = ?, memo = ? where mer_bill_no = ?";
		EntityManager em = JPA.em();
		Query query = em.createQuery(sql).setParameter(1, status).setParameter(2, memo).setParameter(3, merBillNo);
		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			Logger.info(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return;
		}
		
		if (rows == 0) {
			error.code = -1;
			error.msg = "数据未更新";
			
			return;
		}
		
		error.code = 0;
		error.msg = "更新状态和备注成功";
	}
	
	/**
	 * 补单
	 * @param merBillNo
	 * @param type
	 * @param error
	 */
	public void repair(ErrorInfo error) {
		error.clear();
		
		if (StringUtils.isBlank(this.merBillNo)) {
			error.code = -1;
			error.msg = "定单号不能为空";
			
			return;
		}
		
		Integer status = null;
		
		try {
			status = GenericModel.find("select status from t_ips_details where mer_bill_no = ?", this.merBillNo).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return;
		}
		
		if (status == null) {
			error.code = -1;
			error.msg = "交易不存在";
			
			return;
		}
		
		if (status == Status.SUCCESS) {
			error.code = -1;
			error.msg = "交易已成功";
			
			return;
		}
		
		int pTradeType = (this.type==IPSOperation.TRANSFER_ONE || this.type == IPSOperation.TRANSFER_FOUR) ? IPSOperation.TRANSFER : this.type;
		int ipsStatus = Payment.queryIpsStatus(this.merBillNo, pTradeType, error);
		
		if (error.code < 0) {
			return;
		}
		
		if (ipsStatus != Status.SUCCESS && ipsStatus != Status.HANDLING && !IPSConstants.IS_REPAIR_TEST) {
			error.code = -1;
			error.msg = "交易在资金托管方就是失败的，不能补单";
			
			return;
		}
		
		String memo = "";
		
		switch (this.type) {
		case IPSOperation.REGISTER_SUBJECT:
			this.registerSubject(error);
			memo = "发标补单";
			break;
		case IPSOperation.REGISTER_CREDITOR:
			this.registerCreditor(error);
			memo = "投标补单";
			break;
		case IPSOperation.REGISTER_CRETANSFER:
			this.registerCretansfer(error);
			memo = "债权转让补单";
			break;
		case IPSOperation.DO_DP_TRADE:
			this.doDpTrade(error);
			memo = "充值补单";
			break;
		case IPSOperation.TRANSFER_ONE:
			this.transfer(error);
			memo = "转账(放款)补单";
			break;
		case IPSOperation.TRANSFER_FOUR:
			this.transferForCretransfer(error);
			memo = "转账(债权转让)补单";
			break;
		case IPSOperation.REPAYMENT_NEW_TRADE:
			this.repaymentNewTrade(error);
			memo = "还款补单";
			break;
		case IPSOperation.DO_DW_TRADE:
			this.doDwTrade(error);
			memo = "提现补单";
			break;
		default:
			break;
		}
		
		if (error.code < 0) {
			return;
		}
		
		updateStatusAndMemo(this.merBillNo, Status.SUCCESS, memo, error);
		
		if (error.code < 0) {
			JPA.setRollbackOnly();
			
			return;
		}
		
		error.code = 0;
		error.msg = "补单成功";
	}
	
	/**
	 * 发标补单
	 * @param error
	 */
	private void registerSubject(ErrorInfo error) {
		error.clear();
		t_bids tbid = null;
		
		try {
			tbid = GenericModel.find("mer_bill_no = ?", this.merBillNo).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return;
		}
		
		if (tbid == null) {
			error.code = -1;
			error.msg = "交易不存在";
			
			return;
		}
		
		Bid bid = new Bid();
		bid.createBid = true;
		bid.id = tbid.id;
		bid.afterCreateBid(tbid, tbid.bid_no, error);
	}
	
	/**
	 * 投标补单
	 * @param error
	 */
	private void registerCreditor(ErrorInfo error) {
		error.clear();
		String memo = null;
		
		try {
			memo = GenericModel.find("select memo from t_ips_details where mer_bill_no = ?", this.merBillNo).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return;
		}
		
		if (memo == null) {
			error.code = -1;
			error.msg = "交易不存在";
			
			return;
		}
		
		JSONObject jsonObj = JSONObject.fromObject(memo);
		long userId = jsonObj.getLong("userId");
		long bidId = jsonObj.getLong("bidId");
		double pTrdAmt = jsonObj.getDouble("pTrdAmt");
		
		Invest.invest(userId, bidId, (int)pTrdAmt, null, false, true, this.merBillNo, error);
	}
	
	/**
	 * 债权转让补单
	 * @param error
	 */
	private void registerCretansfer(ErrorInfo error) {
		error.clear();
		t_invest_transfers transfer = null;
		
		try {
			transfer = GenericModel.find("mer_bill_no = ?", this.merBillNo).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return;
		}
		
		if (transfer == null) {
			error.code = -1;
			error.msg = "交易不存在";
			
			return;
		}
		
		updateStatusAndMemo(this.merBillNo, Status.SUCCESS, "债权转让补单", error);
		
		if (error.code < 0) {
			return;
		}
		
		String paymentMerBillNo = Payment.transferForCretransfer(this.merBillNo, transfer.id, error);
		
		if (error.code < 0) {
			return;
		}
		
		Debt.dealDebtTransfer(paymentMerBillNo, transfer.id, error);
	}
	
	/**
	 * 充值补单
	 * @param error
	 */
	private void doDpTrade(ErrorInfo error) {
		error.clear();
		t_user_recharge_details detail = null;
		
		try {
			detail = GenericModel.find("pay_number = ?", this.merBillNo).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return;
		}
		
		if (detail == null) {
			error.code = -1;
			error.msg = "交易不存在";
			
			return;
		}
		
		User.recharge(this.merBillNo, detail.amount, error);
	}
	
	/**
	 * 提现补单
	 * @param error
	 */
	private void doDwTrade(ErrorInfo error) {
		error.clear();
		String wId = null;
		
		try {
			wId = GenericModel.find("select memo from t_ips_details where mer_bill_no = ?", this.merBillNo).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return;
		}
		
		if (wId == null) {
			error.code = -1;
			error.msg = "交易不存在";
			
			return;
		}
		
		long withdrawId = Convert.strToLong(wId, -1);
		t_user_withdrawals detail = null;
		
		try {
			detail = GenericModel.findById(withdrawId);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return;
		}
		
		if (detail == null) {
			error.code = -1;
			error.msg = "交易不存在";
			
			return;
		}
		
		User.withdrawalNotice(detail.user_id, detail.id, 1, true, error);
	}
	
	/**
	 * 还款补单
	 * @param error
	 */
	private void repaymentNewTrade(ErrorInfo error) {
		error.clear();
		String bId = null;
		
		try {
			bId = GenericModel.find("select memo from t_ips_details where mer_bill_no = ?", this.merBillNo).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return;
		}
		
		if (bId == null) {
			error.code = -1;
			error.msg = "交易不存在";
			
			return;
		}
		
		long billId = Convert.strToLong(bId, -1);
		
		Bill bill = new Bill();
		bill.isRepair = true;
		bill.id = billId;
		bill.repayment(bill.bid.userId, error);
	}
	
	/**
	 * 转账(放款)补单
	 * @param error
	 */
	private void transfer(ErrorInfo error) {
		error.clear();
		String bId = null;
		
		try {
			bId = GenericModel.find("select memo from t_ips_details where mer_bill_no = ?", this.merBillNo).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return;
		}
		
		if (bId == null) {
			error.code = -1;
			error.msg = "交易不存在";
			
			return;
		}
		
		long bidId = Convert.strToLong(bId, -1);
		
		Bid bid = new Bid();
		bid.auditBid = true;
		bid.isRepair = true;
		bid.id = bidId;
		bid.allocationSupervisorId = Supervisor.currSupervisor().id; // 审核人
		bid.eaitLoanToRepayment(error);
	}
	
	/**
	 * 转账(债权转让)补单
	 * @param error
	 */
	private void transferForCretransfer(ErrorInfo error) {
		error.clear();
		String dId = null;
		
		try {
			dId = GenericModel.find("select memo from t_ips_details where mer_bill_no = ?", this.merBillNo).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return;
		}
		
		if (dId == null) {
			error.code = -1;
			error.msg = "交易不存在";
			
			return;
		}
		
		long debtId = Convert.strToLong(dId, -1);
		
		Debt.dealDebtTransfer(this.merBillNo, debtId, error);
	}
}
