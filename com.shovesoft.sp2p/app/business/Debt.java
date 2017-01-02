package business;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;

import com.google.zxing.BarcodeFormat;
import com.shove.Convert;
import com.shove.code.Qrcode;
import constants.Constants;
import constants.DealType;
import constants.OptionKeys;
import constants.SupervisorEvent;
import constants.Templets;
import constants.UserEvent;
import play.Logger;
import play.db.jpa.Blob;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import utils.Arith;
import utils.DateUtil;
import utils.ErrorInfo;
import utils.NumberUtil;
import utils.PageBean;
import utils.Security;
import models.t_bids;
import models.t_bill_invests;
import models.t_invest_transfer_details;
import models.t_invest_transfers;
import models.t_invests;
import models.t_user_attention_bids;
import models.t_user_attention_invest_transfers;
import models.t_users;
import models.v_debt_auction_records;
import models.v_debt_auditing_transfers;
import models.v_debt_no_pass_transfers;
import models.v_debt_transfer_failure;
import models.v_debt_transfering;
import models.v_debt_transfers_success;
import models.v_debt_user_receive_transfers_management;
import models.v_debt_user_transfer_management;
import models.v_front_all_debts;
import models.v_user_attention_invest_transfers;

/**
 * 债权转让
 * 
 * @author lwh
 * @version 6.0
 * @created 2014年3月21日 下午2:01:38
 */
public class Debt implements Serializable {
	private long _id;
	public long id;
	public String sign;
	public String no;
	public long investId;
	public Date time;
	public String title;
	public String transerReason;
	public double debtAmount;
	public double transferPrice;
	public int type;
	public long specifiedUserId;
	public int period;
	public int status;
	public String noThroughReason;
	public boolean isQualityDebt;
	public long auditSupervisorId;
	public Date startTime;
	public Date endTime;
	public String lastTime;
	public int joinTimes;
	public long transactionUserId;
	public Date transactionTime;
	public double transactionPrice;
	public double maxOfferPrice;
	public String qr_code;

	public Invest invest;
	public Map<String, Object> map;
	public User transactionUser;// 竞拍成功会员
	public User specifiedUser;// 转让指定会员

	public String getSpreadLink() {
		return Constants.BASE_URL + "front/debt/debtDetails?debtId=" + this.id;
	}

	public String getSign() {
		return Security.addSign(this._id, Constants.BID_ID_SIGN);
	}

	public void setSpecifiedUserId(long specifiedUserId) {
		this.specifiedUserId = specifiedUserId;
		this.specifiedUser = new User();
		this.specifiedUser.id = specifiedUserId;
	}

	public void setTransactionUserId(long transactionUserId) {
		this.transactionUserId = transactionUserId;
		this.transactionUser = new User();
		this.transactionUser.id = transactionUserId;
	}

	public void setInvestId(long investId) {
		this.investId = investId;
		this.invest = new Invest();
		this.invest.id = investId;
	}

	public Debt() {

	}

	public long getId() {
		return _id;
	}

	public void setId(long id) {
		/* t_invest_transfers 债权转让数据实体 */
		t_invest_transfers investTransfers = null;
		ErrorInfo error = new ErrorInfo();

		try {
			investTransfers = GenericModel.findById(id);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			return;
		}

		if (investTransfers == null) {
			this._id = -1;
			throw new NullPointerException("数据实体对象不存在!");
		}

		this._id = investTransfers.id;
		this.investId = investTransfers.invest_id;
		this.no = OptionKeys.getvalue(OptionKeys.TRANFER_NUMBER, error).concat(
				id + "");
		this.time = investTransfers.time;
		this.title = investTransfers.title;
		this.transerReason = investTransfers.transer_reason;
		this.debtAmount = investTransfers.debt_amount;
		this.transferPrice = investTransfers.transfer_price;
		this.type = investTransfers.type;
		this.qr_code = investTransfers.qr_code;

		if (investTransfers.specified_user_id > 0) {
			this.specifiedUserId = investTransfers.specified_user_id;
		}

		this.period = investTransfers.period;
		this.status = investTransfers.status;
		this.noThroughReason = investTransfers.no_through_reason;
		this.isQualityDebt = investTransfers.is_quality_debt;
		this.auditSupervisorId = investTransfers.audit_supervisor_id;
		this.startTime = investTransfers.start_time;
		this.endTime = investTransfers.end_time;
		if (null != investTransfers.end_time) {
			this.lastTime = DateUtil.dateToString2(investTransfers.end_time);// 格式化时间，方便时间倒计时计算
		}

		this.joinTimes = investTransfers.join_times;

		if (investTransfers.transaction_user_id > 0) {
			this.transactionUserId = investTransfers.transaction_user_id;
		}
		if (null != investTransfers.transaction_time) {
			this.transactionTime = investTransfers.transaction_time;
		}
		if (investTransfers.transaction_price > 0) {
			this.transactionPrice = investTransfers.transaction_price;
		}
		this.maxOfferPrice = Debt.queryMaxPrice(id, error);

		if (null != this.queryInvestBill(id)) {
			this.map = this.queryInvestBill(id);
		}
	}

	/**
	 * 查询某个人投资某个借款标的投资账单
	 * 
	 * @param id
	 * @return
	 */
	private Map<String, Object> queryInvestBill(long id) {

		java.text.DecimalFormat df = new java.text.DecimalFormat("0.00");// 保留2位小数
		t_invest_transfers investTransfers = null;
		t_invests invest = null;
		Map<String, Object> map = new HashMap<String, Object>();
		Date time = null;
		Double receive_money = 0.0;
		Double has_receive_money = 0.0;
		Double receive_corpus = 0.0;

		try {
			investTransfers = GenericModel.findById(id);
			invest = GenericModel.findById(investTransfers.invest_id);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
		}

		if (invest.transfer_status == -1) {
			invest = GenericModel.find("transfers_id = ? ", investTransfers.id)
					.first();
		}

		if (null != invest) {
			String hql = "select receive_time from t_bill_invests   where  invest_id = ? and status = -1 order by receive_time";

			time = GenericModel.find(hql, invest.id).first();

			String strTime = "";
			if (time != null) {
				strTime = DateUtil.dateToString1(time);
			}

			map.put("receive_time", strTime);

			String s = "select sum(receive_corpus + receive_interest + overdue_fine) from t_bill_invests where  invest_id = ? and status <> -7";
			String r = "select sum(receive_corpus + receive_interest + overdue_fine) from t_bill_invests where  invest_id = ? and status in (-3,-4,0)";
			String c = "select sum(receive_corpus - real_receive_corpus) from t_bill_invests where  invest_id = ? and status in (-1,-2,-5,-6)";

			try {

				receive_money = GenericModel.find(s, invest.id).first();
				has_receive_money = GenericModel.find(r, invest.id).first();
				receive_corpus = GenericModel.find(c, invest.id).first();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (null == receive_money || receive_money == 0) {
			receive_money = 0.0;
		}

		if (null == has_receive_money || has_receive_money == 0) {
			has_receive_money = 0.0;
		}

		if (null == receive_corpus || receive_corpus == 0) {
			receive_corpus = 0.0;
		}

		map.put("receive_money", df.format(receive_money));
		map.put("has_receive_money", df.format(has_receive_money));
		map.put("remain_receive_money",
				df.format(receive_money - has_receive_money));
		map.put("receive_corpus", df.format(receive_corpus));

		return map;
	}

	/**
	 * 查询最大债权竞拍出价
	 * 
	 * @param transfer_id
	 * @return
	 */
	public static Double queryMaxPrice(long transfer_id, ErrorInfo error) {

		List<t_invest_transfer_details> list = null;
		Double maxOfferPrice = 0.0;

		try {
			list = GenericModel.find(
					"from t_invest_transfer_details where transfer_id=?",
					transfer_id).fetch();
		} catch (Exception e) {
			Logger.info(e.getMessage());
			e.printStackTrace();

			error.code = -1;
		}

		if (list.size() > 0 && null != list) {

			try {
				maxOfferPrice = GenericModel
						.find("select max(offer_price) from t_invest_transfer_details where transfer_id=?",
								transfer_id).first();
			} catch (Exception e) {
				Logger.info(e.getMessage());
				e.printStackTrace();

				error.code = -2;
			}

			if (null == maxOfferPrice || maxOfferPrice == 0) {
				maxOfferPrice = 0.0;
			}

		}

		error.code = 1;
		return maxOfferPrice;
	}

	/**
	 * 查询最新的4个债券转让
	 * ***/
	public static List<v_front_all_debts> debtsBids() {

		List<v_front_all_debts> debtsBids = null;

		try {
			debtsBids = GenericModel.find("").fetch(4);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return debtsBids;
	}

	/**
	 * 查询所有在前台显示的债权转让标
	 * 
	 * @return
	 */
	public static PageBean<v_front_all_debts> queryAllDebtTransfers(
			int currPage, int pageSize, String loanType, String debtAmount,
			String apr, String orderType, String _keywords, ErrorInfo error) {

		int debtAmountType = 0;
		int aprtype = 0;
		int order = 0;
		int loanTypes = 0;

		List<v_front_all_debts> debtList = new ArrayList<v_front_all_debts>();
		PageBean<v_front_all_debts> page = new PageBean<v_front_all_debts>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		StringBuffer conditions = new StringBuffer(" 1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (StringUtils.isBlank(loanType) && StringUtils.isBlank(debtAmount)
				&& StringUtils.isBlank(apr) && StringUtils.isBlank(orderType)
				&& StringUtils.isBlank(_keywords)) {

			try {
				page.totalCount = (int) GenericModel.count();
				debtList = GenericModel.find("").fetch(currPage,
						page.pageSize);

			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
			}

			error.code = 1;
			page.page = debtList;

			return page;
		}

		if (NumberUtil.isNumericInt(loanType)) {
			loanTypes = Convert.strToInt(loanType, 0);
			conditions.append(" and product_id = ?");
			values.add(loanTypes);
		}

		if (NumberUtil.isNumericInt(debtAmount)) {
			debtAmountType = Integer.parseInt(debtAmount);
		}

		if (!StringUtils.isBlank(_keywords)) {
			conditions.append(" and title like ? ");
			values.add("%" + _keywords + "%");
		}
		if (debtAmountType < 0 || debtAmountType > 5) {
			conditions.append(Constants.DEBT_AMOUNT_CONDITION[0]);
		} else {
			conditions.append(Constants.DEBT_AMOUNT_CONDITION[debtAmountType]);
		}

		if (NumberUtil.isNumericInt(apr)) {
			aprtype = Integer.parseInt(apr);
		}

		if (aprtype < 0 || aprtype > 4) {
			conditions.append(Constants.BID_APR_CONDITION[0]);
		} else {
			conditions.append(Constants.BID_APR_CONDITION[aprtype]);
		}

		if (NumberUtil.isNumericInt(orderType)) {
			order = Integer.parseInt(orderType);
		}

		if (order < 0 || order > 4) {
			conditions.append(Constants.DEBT_ORDER_CONITION[0]);
		} else {
			conditions.append(Constants.DEBT_ORDER_CONITION[order]);
		}

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			debtList = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -2;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("apr", aprtype);
		conditionMap.put("debtAmount", debtAmountType);
		conditionMap.put("order", order);
		conditionMap.put("keywords", _keywords);
		conditionMap.put("loanType", loanTypes);

		error.code = 1;
		page.page = debtList;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 查询在竞拍状态的优质债权转让标(前两条)
	 * 
	 * @return
	 */
	public static List<v_front_all_debts> queryQualityDebtTransfers(
			ErrorInfo error) {
		error.clear();

		List<v_front_all_debts> debtList = new ArrayList<v_front_all_debts>();

		try {
			debtList = GenericModel.find(
					"  status = 1 and is_quality_debt = 1 order by time desc ")
					.fetch(2);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info(e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，导致优质债权转让标查询失败";

			return debtList;
		}

		error.code = 1;
		return debtList;
	}

	/**
	 * 根据投资ID判断所投借款标是否是天标
	 * 
	 * @param investId
	 * @param error
	 * @return
	 */
	public static boolean judgeIsDayBid(long investId, ErrorInfo error) {

		String sql = "select bid_id from t_invests where id = ?";
		Long bidId = 0l;
		t_bids bid = null;
		try {
			bidId = GenericModel.find(sql, investId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (bidId > 0) {
			try {
				bid = GenericModel.findById(bidId);
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
			}

			if (null != bid) {
				if (bid.period_unit == 1) {// 天标单位

					error.code = 1;
					return true;
				}
			}

		}

		error.code = 1;
		return false;
	}

	/**
	 * 根据投资ID判断所投借款标是否是秒还标
	 * 
	 * @param investId
	 * @param error
	 * @return
	 */
	public static boolean judgeIsSecBid(long investId, ErrorInfo error) {

		String sql = "select bid_id from t_invests where id = ?";
		Long bidId = 0l;
		t_bids bid = null;
		try {
			bidId = GenericModel.find(sql, investId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (bidId > 0) {
			try {
				bid = GenericModel.findById(bidId);
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
			}

			if (null != bid) {
				error.code = 1;
				return bid.is_sec_bid;
			}

		}

		error.code = 1;
		return false;
	}

	/**
	 * 获取债权总额
	 * 
	 * @param investId
	 * @param error
	 * @return
	 */
	public static double getDebtAmount(long investId, ErrorInfo error) {

		Double hasReceivedAmount = 0.0;

		try {
			hasReceivedAmount = GenericModel
					.find("select sum(receive_corpus + receive_interest + overdue_fine) from t_bill_invests where status in (-1,-2,-5,-6) and invest_id = ? ",
							investId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (null == hasReceivedAmount) {
			hasReceivedAmount = 0.0;
		}

		error.code = 1;
		return hasReceivedAmount;
	}

	/**
	 * 获取债权待收本金
	 * 
	 * @param investId
	 * @param error
	 * @return
	 */
	public static double getReceiveCorpus(long userId, long bidId,
			ErrorInfo error) {
		error.clear();
		Double amount = 0.0;

		try {
			amount = GenericModel
					.find("select sum(receive_corpus) from t_bill_invests where status in (-1,-2) and user_id = ? and bid_id = ? ",
							userId, bidId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (null == amount) {
			amount = 0.0;
		}

		error.code = 1;

		return amount;
	}

	/**
	 * 判断是否被管理员拉黑
	 * 
	 * @param userId
	 * @param error
	 * @return
	 */
	public static boolean isBlack(long userId, ErrorInfo error) {

		boolean flag = false;
		String sql = "select is_blacklist from t_users where id = ?";

		try {
			flag = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}
		error.code = 1;
		return flag;
	}

	/**
	 * 处理该债权之前未通过的同样债权
	 * 
	 * @param investId
	 * @param error
	 * @return
	 */
	public static long hasNopassDebt(long investId, ErrorInfo error) {

		long result = -1;
		t_invest_transfers debt = null;

		try {
			debt = GenericModel.find(" invest_id = ? and status = -1 ",
					investId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (null != debt) {
			result = debt.id;
		}

		return result;
	}

	/**
	 * 转让债权
	 * 
	 * @param investId
	 *            投资ID
	 * @param transferTitle
	 *            债权转让标题
	 * @param transferReason
	 *            债权转让原因
	 * @param specifiedUserId
	 *            指定转让会员
	 * @param period
	 *            转让天数
	 * @param debtAmount
	 *            债权金额
	 * @param transferPrice
	 *            转让定价
	 * @param type
	 *            类型 1定向转让： 2:竞价转让
	 * @param error
	 * @return 返回-1代表转让债权失败，返回 1 代表成功
	 */
	public static int transferDebt(long userId, long investId,
			String transferTitle, String transferReason, int period,
			double debtAmount, double transferPrice, int type,
			String specifiedUserName, ErrorInfo error) {

		error.clear();
		EntityManager em = JPA.em();
		t_invests invest = new t_invests();

		try {
			invest = GenericModel.findById(investId);
		} catch (Exception e) {
			e.printStackTrace();

			error.msg = "查询对应投资数据实体对象异常";
			error.code = -1;

			return error.code;
		}

		if (userId != invest.user_id) {
			error.msg = "非法请求！";
			error.code = -2;

			return error.code;
		}

		if (invest.transfer_status != Constants.TRANSFER_STATUS) {// 0
																	// 正常(转让入的也是0)
																	// -1 已转让出 1
																	// 转让中

			error.msg = "对不起！该债权不能进行转让操作！";
			error.code = -1;

			return error.code;
		}

		boolean black = isBlack(invest.user_id, error);

		if (error.code < 0) {
			error.msg = "对不起！系统异常，请联系平台管理员！";
			error.code = -2;

			return error.code;
		}

		if (black) {
			error.msg = "对不起！您被管理员限制操作，请联系平台管理员！";
			error.code = -2;

			return error.code;
		}

		boolean isDSecBid = judgeIsSecBid(investId, error);// 判断是否是天标

		if (error.code < 0) {
			error.msg = "对不起！您此次债权转让操作失败！请您重试或联系平台管理员！";
			error.code = -2;

			return error.code;
		}

		if (isDSecBid) {
			error.msg = "对不起！您投资的是秒还标，不能进行债权转让！";
			error.code = -3;

			return error.code;
		}

		if (debtAmount < transferPrice) {
			error.msg = "对不起！您设置的竞拍底价不能高于债权总额！";
			error.code = -2;

			return error.code;
		}

		if (debtAmount > transferPrice * 2) {
			error.msg = "对不起！您设置的竞拍底价应该高于债权总额的一半！";
			error.code = -2;

			return error.code;
		}

		t_users user = null;
		if (!StringUtils.isBlank(specifiedUserName)) {
			try {
				user = GenericModel.find(" name = ? ", specifiedUserName).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.msg = "对不起！您此次债权转让操作失败！请您重试或联系平台管理员！";
				error.code = -1;

				return error.code;
			}

			if (null == user) {
				error.msg = "对不起！您指定的用户名不存在！";
				error.code = -1;

				return error.code;
			}

			if (userId == user.id) {
				error.msg = "对不起！您指定的用户不能是自己！";
				error.code = -1;

				return error.code;
			}
			long uId = getUserIdByInvestId(investId, error);// 债权借款用户

			if (uId == user.id) {
				error.msg = "借款持有用户禁止参与该债权交易！";
				error.code = -1;

				return error.code;
			}
		}

		t_invest_transfers investTransfer = new t_invest_transfers();
		investTransfer.invest_id = investId;
		investTransfer.title = transferTitle;

		/* 转让状态(0审核中，1为竞拍中，2等待认购 3成功 -1审核不通过,-2流标 */
		investTransfer.status = 0;
		investTransfer.time = new Date();
		investTransfer.transer_reason = transferReason;
		investTransfer.period = period;
		investTransfer.debt_amount = debtAmount;
		investTransfer.transfer_price = transferPrice;
		investTransfer.type = type;

		boolean flag = false;

		/* DIRECTIONAL_MODE =1; 定向模式 */
		if (Constants.DIRECTIONAL_MODE == type) {
			investTransfer.specified_user_id = user.id;
			flag = true;
		}

		try {
			/* 添加债权转让记录 */
			investTransfer.save();
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！您此次债权转让操作失败！请您重试或联系平台管理员！";
			error.code = -1;

			return error.code;
		}

		String uuid = UUID.randomUUID().toString();
		Qrcode code = new Qrcode();

		String str = Constants.BASE_URL + "front/debt/debtDetails?debtId = "
				+ investTransfer.id;
		try {
			Blob blob = new Blob();
			Qrcode.create(str, BarcodeFormat.QR_CODE, 100, 100,
					new File(Blob.getStore(), uuid).getAbsolutePath(), "png");
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("创建二维码图片失败" + e.getMessage());
			error.code = -5;
			error.msg = "对不起！您此次债权转让操作失败！请您重试或联系平台管理员！";
			JPA.setRollbackOnly();
			return error.code;
		}

		// 保存二维码标识
		try {
			int a = em
					.createQuery(
							"update t_invest_transfers set qr_code = ? where id = ?")
					.setParameter(1, uuid).setParameter(2, investTransfer.id)
					.executeUpdate();

			if (a == 0) {
				error.code = -5;
				error.msg = "对不起！您此次债权转让操作失败！请您重试或联系平台管理员！";
				JPA.setRollbackOnly();
				return error.code;
			}
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -5;
			error.msg = "对不起！您此次债权转让操作失败！请您重试或联系平台管理员！";
			JPA.setRollbackOnly();
			return error.code;
		}

		/* 在债权竞拍记录表插入一条记录 */
		if (flag) {
			t_invest_transfer_details details = new t_invest_transfer_details();
			details.transfer_id = investTransfer.id;
			details.time = new Date();
			details.user_id = user.id;
			details.offer_price = transferPrice;

			/* 状态：0 正常；1 成交；2 待接受；-1 失败 */
			details.status = 2;

			// 添加事件
			DealDetail.userEvent(invest.user_id, UserEvent.DIRECTIONAL_MODE,
					"申请定向模式债权转让", error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			try {
				details.save();
			} catch (Exception e) {
				e.printStackTrace();
				error.msg = "对不起！您此次债权转让操作失败！请您重试或联系平台管理员！";
				error.code = -2;

				return error.code;
			}

		}

		try {
			/* 改变投资表对应记录的状态值为 “转让中” */
			int rows = em
					.createQuery(
							"update t_invests set transfer_status = 1 where id=? and transfer_status = 0")
					.setParameter(1, investId).executeUpdate();

			if (rows == 0) {
				JPA.setRollbackOnly();
				error.msg = "对不起！您此次债权转让操作失败！请您重试或联系平台管理员！";
				error.code = -3;

				return error.code;
			}

		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！您此次债权转让操作失败！请您重试或联系平台管理员！";
			error.code = -3;

			return error.code;
		}

		// 添加事件
		DealDetail.userEvent(invest.user_id, UserEvent.AUCTION_MODE,
				"申请竞拍模式债权转让", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		// 判断该债权之前是否有审核未通过记录，有的话直接置为流拍
		long result = hasNopassDebt(investId, error);

		if (error.code < 0) {
			error.msg = "对不起！您此次债权转让操作失败！请您重试或联系平台管理员！";
			error.code = -3;

			return error.code;
		}

		if (result > 0) {
			changeDebtStatus(result, Constants.DEBT_FLOW);
		}

		error.msg = "债权转让申请成功！请您耐心等待平台审核！谢谢！";
		error.code = 1;

		return error.code;

	}

	/**
	 * 债权审核通过操作
	 * 
	 * @param investTransferId
	 * @param type
	 *            AUCTION_MODE: 竞价模式 DIRECTIONAL_MODE：定向模式
	 * @param qualityStatus
	 * @param auditSupervisorId
	 * @param info
	 * @return 返回 -1 代表失败 返回 1 代表成功
	 */
	public static int auditDebtTransferPass(long investTransferId, int type,
			int qualityStatus, long auditSupervisorId, ErrorInfo error) {

		error.clear();
		EntityManager em = JPA.em();
		t_invest_transfers transfer = new t_invest_transfers();
		t_invests invest = new t_invests();
		t_users user = new t_users();

		try {
			transfer = GenericModel.findById(investTransferId);
			invest = GenericModel.findById(transfer.invest_id);
			user = GenericModel.findById(invest.user_id);
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常！";
			error.code = -1;

			return error.code;
		}

		if (transfer.status != Constants.PENDING_AUDIT_STATUS
				&& transfer.status != Constants.DEBT_NOPASS) {
			error.msg = "对不起！该债权已经得到处理！";
			error.code = -1;

			return error.code;
		}

		/* AUCTION_MODE: 竞价模式 */
		if (Constants.AUCTION_MODE == type) {
			/* 0审核中，1为竞拍中，2等待认购 3成功-1审核不通过,-2流标，-3对方未接受，-4主动撤销，-5还款自动撤销,-6系统撤销 */
			transfer.status = 1;

		} else {
			transfer.status = 2;
		}

		transfer.audit_supervisor_id = auditSupervisorId;

		if (qualityStatus == 1) {
			transfer.is_quality_debt = true;
			// 添加事件
			DealDetail.supervisorEvent(auditSupervisorId,
					SupervisorEvent.MARK_QUALITY_DEBT, "设置优质债权转让", error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}
		} else {
			transfer.is_quality_debt = false;
		}

		transfer.start_time = new Date();
		transfer.end_time = DateUtil.dateAddDay(new Date(), transfer.period);

		try {
			/* 0 正常(转让入的也是0) -1 已转让出 1 转让中 */
			int rows = em
					.createQuery(
							"update t_invests set transfer_status=1 where id = ? and transfer_status in(0,1)")
					.setParameter(1, transfer.invest_id).executeUpdate();

			if (rows == 0) {
				JPA.setRollbackOnly();
				error.msg = "对不起，系统异常，审核失败！";
				error.code = -1;

				return error.code;
			}

			transfer.save();
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起，系统异常，审核失败！";
			error.code = -1;

			return error.code;
		}

		// 发送邮件
		String name = user.name;

		TemplateEmail email = new TemplateEmail();
		email.id = Templets.E_DEBT_PASS;

		if (email.status) {
			String econtent = email.content;
			econtent = econtent.replace("userName", name);
			econtent = econtent.replace("date", transfer.time + "");
			econtent = econtent.replace("title", transfer.title);
			TemplateEmail.addEmailTask(user.email, email.title, econtent);
		}

		TemplateStation station = new TemplateStation();// 发送站内信
		station.id = Templets.M_DEBT_PASS;

		if (station.status) {
			String sContent = station.content;
			sContent = sContent.replace("userName", name);
			sContent = sContent.replace("date", transfer.time + "");
			sContent = sContent.replace("title", transfer.title);
			TemplateStation.addMessageTask(user.id, station.title, sContent);
		}

		TemplateSms sms = new TemplateSms();// 发送短信
		sms.id = Templets.S_DEBT_PASS;

		if (sms.status) {
			String smscontent = sms.content;
			smscontent = smscontent.replace("userName", name);
			smscontent = smscontent.replace("date", transfer.time + "");
			smscontent = smscontent.replace("title", transfer.title);
			TemplateSms.addSmsTask(user.mobile, smscontent);
		}

		// 添加事件
		DealDetail.supervisorEvent(auditSupervisorId,
				SupervisorEvent.AUDIT_DEBT_TRANSFER, "审核转让债权通过", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		error.code = 1;
		error.msg = "债权审核通过成功！";

		return error.code;
	}

	/**
	 * 债权审核不通过操作
	 * 
	 * @param investTransferId
	 * @param auditSupervisorId
	 * @param noThroughReason
	 * @param qualityStatus
	 * @param info
	 * @return 返回 -1 代表操作失败 返回 1 代表操作成功
	 */
	public static int auditDebtTransferNoPass(long investTransferId,
			long auditSupervisorId, String noThroughReason, ErrorInfo error) {

		error.clear();
		EntityManager em = JPA.em();
		t_invest_transfers transfer = new t_invest_transfers();
		t_invests invest = new t_invests();
		t_users user = new t_users();

		try {
			transfer = GenericModel.findById(investTransferId);
			invest = GenericModel.findById(transfer.invest_id);
			user = GenericModel.findById(invest.user_id);
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常！";
			error.code = -1;

			return error.code;
		}

		if (transfer.status != 0 && transfer.status != -1) {
			error.msg = "对不起！该债权已经得到处理！";
			error.code = -1;

			return error.code;
		}

		/* 0审核中，1为竞拍中，2等待认购 3成功 -1审核不通过,-2流标，-3对方未接受，-4主动撤销，-5还款自动撤销,-6系统撤销 */
		transfer.status = -1;
		transfer.audit_supervisor_id = auditSupervisorId;
		transfer.start_time = new Date();
		transfer.no_through_reason = noThroughReason;
		transfer.end_time = DateUtil.dateAddDay(new Date(), transfer.period);
		transfer.is_quality_debt = false;

		try {
			/* 0 正常(转让入的也是0) -1 已转让出 1 转让中 */
			int rows = em
					.createQuery(
							"update t_invests set transfer_status=0 where id=? and transfer_status = 1")
					.setParameter(1, transfer.invest_id).executeUpdate();

			if (rows == 0) {
				JPA.setRollbackOnly();
				error.msg = "审核失败！";
				error.code = -1;

				return error.code;
			}
			transfer.save();

		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "审核失败！";
			error.code = -1;

			return error.code;
		}

		/* 发送站内信通知操作 */
		String name = user.name;

		TemplateEmail email = new TemplateEmail();
		email.id = Templets.E_DEBT_NO_PASS;

		if (email.status) {
			String econtent = email.content;
			econtent = econtent.replace("userName", name);
			econtent = econtent.replace("date", transfer.time + "");
			econtent = econtent.replace("title", transfer.title);
			TemplateEmail.addEmailTask(user.email, email.title, econtent);
		}

		TemplateStation station = new TemplateStation();// 发送站内信
		station.id = Templets.M_DEBT_NO_PASS;

		if (station.status) {
			String sContent = station.content;
			sContent = sContent.replace("userName", name);
			sContent = sContent.replace("date", transfer.time + "");
			sContent = sContent.replace("title", transfer.title);
			TemplateStation.addMessageTask(user.id, station.title, sContent);
		}

		TemplateSms sms = new TemplateSms();// 发送短信
		sms.id = Templets.S_DEBT_NO_PASS;

		if (sms.status) {
			String smscontent = sms.content;
			smscontent = smscontent.replace("userName", name);
			smscontent = smscontent.replace("date", transfer.time + "");
			smscontent = smscontent.replace("title", transfer.title);
			TemplateSms.addSmsTask(user.mobile, smscontent);
		}

		// 添加事件
		DealDetail.supervisorEvent(auditSupervisorId,
				SupervisorEvent.AUDIT_DEBT_TRANSFER, "审核转让债权不通过", error);

		if (error.code < 0) {
			error.msg = "对不起！系统异常！请您重试或联系平台管理员！";
			error.code = -9;
			JPA.setRollbackOnly();

			return error.code;
		}

		error.code = 1;
		error.msg = "债权审核不通过操作成功！";

		return error.code;
	}

	/**
	 * 拒绝接收债权转让
	 * 
	 * @param investTransferId
	 * @param info
	 * @return 返回 1 代表操作成功，-1 代表失败
	 */
	public static int refuseAccept(long investTransferId, ErrorInfo error) {

		error.clear();
		EntityManager em = JPA.em();
		t_invest_transfers transfer = new t_invest_transfers();
		t_invest_transfer_details detail = new t_invest_transfer_details();

		try {
			transfer = GenericModel.findById(investTransferId);
			detail = GenericModel.find(" transfer_id = ?",
					investTransferId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常！";
			error.code = -1;

			return error.code;
		}

		if (transfer.status != Constants.DEBT_ACCEPT) {
			error.msg = "对不起！该债权您已经处理过了，请不要重复操作！";
			error.code = -1;

			return error.code;
		}

		long investId = transfer.invest_id;

		try {
			/* 对方未接受 */
			int a = em
					.createQuery(
							"update t_invest_transfers set status=-3 ,failure_time =? where id = ? and status = 2")
					.setParameter(1, new Date())
					.setParameter(2, investTransferId).executeUpdate();
			int b = em
					.createQuery(
							"update t_invests set transfer_status=0  where id=? and transfer_status = 1")
					.setParameter(1, investId).executeUpdate();

			/* 失败 */
			int c = em
					.createQuery(
							"update t_invest_transfer_details set status=-1   where transfer_id=? and status = 2")
					.setParameter(1, investTransferId).executeUpdate();

			if (a == 0 || b == 0 || c == 0) {
				JPA.setRollbackOnly();
				error.msg = "拒绝操作失败！";
				error.code = -1;

				return error.code;
			}

		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "拒绝操作失败！";
			error.code = -1;

			return error.code;
		}

		t_invests invest = new t_invests();
		t_users user = new t_users();

		try {
			invest = GenericModel.findById(investId);
			user = GenericModel.findById(invest.user_id);
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常！";
			error.code = -1;

			return error.code;
		}

		// 发送消息
		TemplateEmail email = new TemplateEmail();// 发送邮件
		email.id = Templets.E_DEBT_TRANSFER_FAILURE;

		if (email.status) {
			String econtent = email.content;
			econtent = econtent.replace("userName", user.name);
			econtent = econtent.replace("date", transfer.time + "");
			econtent = econtent.replace("describe", transfer.title);
			econtent = econtent.replace("money", transfer.debt_amount + "");
			TemplateEmail.addEmailTask(user.email, email.title, econtent);
		}

		TemplateStation station = new TemplateStation();// 发送站内信
		station.id = Templets.M_DEBT_TRANSFER_FAILURE;

		if (station.status) {
			String sContent = station.content;
			sContent = sContent.replace("userName", user.name);
			sContent = sContent.replace("date", transfer.time + "");
			sContent = sContent.replace("describe", transfer.title);
			sContent = sContent.replace("money", transfer.debt_amount + "");
			TemplateStation.addMessageTask(user.id, station.title, sContent);
		}

		TemplateSms sms = new TemplateSms();// 发送短信
		sms.id = Templets.S_DEBT_TRANSFER_FAILURE;

		if (sms.status) {
			String scontent = sms.content;
			scontent = scontent.replace("userName", user.name);
			scontent = scontent.replace("date", transfer.time + "");
			scontent = scontent.replace("describe", transfer.title);
			scontent = scontent.replace("money", transfer.debt_amount + "");
			TemplateSms.addSmsTask(user.mobile, scontent);
		}

		// 添加事件
		DealDetail.userEvent(detail.user_id, UserEvent.REFUSE_DEBT,
				"拒绝受让定向转让债权", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		error.code = 1;
		error.msg = "拒绝操作成功";
		return 1;
	}

	/**
	 * 收藏债权
	 * 
	 * @param userId
	 * @param investTransferId
	 */
	public static int collectDebt(long userId, long investTransferId,
			ErrorInfo error) {

		error.clear();

		long investUserId = getInvestUserId(investTransferId, error);

		if (error.code < 0) {
			error.msg = "对不起！您此次收藏该债权失败！";
			error.code = -2;

			return error.code;
		}

		if (investUserId == userId) {
			error.msg = "对不起！您不能收藏自己的债权！";
			error.code = -2;

			return error.code;
		}

		t_user_attention_invest_transfers userAttentionInvestTransfer = GenericModel
				.find("from t_user_attention_invest_transfers where user_id=? and invest_transfer_id=?",
						userId, investTransferId).first();

		if (null == userAttentionInvestTransfer) {
			t_user_attention_invest_transfers userAttentionInvestTransfers = new t_user_attention_invest_transfers();

			userAttentionInvestTransfers.user_id = userId;
			userAttentionInvestTransfers.time = new Date();
			userAttentionInvestTransfers.invest_transfer_id = investTransferId;

			try {
				userAttentionInvestTransfers.save();
			} catch (Exception e) {
				e.printStackTrace();
				error.msg = "对不起！您此次收藏该债权失败！请您重试或联系平台管理员！";
				error.code = -1;

				return error.code;
			}

			// 添加事件
			DealDetail.userEvent(userId, UserEvent.COLLECT_DEBT, "收藏转让债权",
					error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			error.msg = "收藏债权成功！";
			error.code = 1;

			return error.code;
		}
		error.msg = "对不起！您已经收藏过该债权！";
		error.code = -2;

		return error.code;
	}

	/**
	 * 删除收藏的债权
	 * 
	 * @param userId
	 * @param investTransferId
	 */
	public int deleteAttentionInvestTransfer(long userId,
			long investTransferId, ErrorInfo error) {

		t_user_attention_invest_transfers userAttentionInvestTransfers = null;

		try {
			userAttentionInvestTransfers = GenericModel
					.find(" user_id=? and invest_transfer_id=?", userId,
							investTransferId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info(e.getMessage());

			error.msg = "对不起！系统异常，在此给你造成的不便敬请谅解！";
			error.code = -1;

			return error.code;

		}

		if (null == userAttentionInvestTransfers) {
			error.msg = "对不起！该债权已经不在你的收藏列表！";
			error.code = -2;

			return error.code;
		}

		userAttentionInvestTransfers.delete();
		error.msg = "操作成功！";
		error.code = 1;

		return error.code;
	}

	/**
	 * 查询债权转让信息
	 * 
	 * @param debtId
	 * @return
	 */
	public static Map<String, Object> queryTransferInfo(long debtId,
			ErrorInfo error) {
		error.clear();

		long fromUserId = -1;
		long toUserId = -1;

		String bidNo = null;
		String pCreMerBillNo = null;// 登记债权人时提交的订单号

		double pCretAmt = 0;
		double pPayAmt = 0;

		t_invest_transfers debt = null;
		t_invests invest = null;
		t_bids bid = null;

		try {
			debt = GenericModel.findById(debtId);
			invest = GenericModel.findById(debt.invest_id);
			bid = GenericModel.findById(invest.bid_id);
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
			error.code = -111;

			return null;
		}

		if (debt == null || invest == null || bid == null) {
			return null;
		}

		fromUserId = invest.user_id;
		bidNo = bid.bid_no;
		pCreMerBillNo = invest.mer_bill_no;
		// pCretAmt = debt.debt_amount;
		pCretAmt = Debt.getReceiveCorpus(invest.user_id, invest.bid_id, error);
		pPayAmt = queryMaxPrice(debtId, error);

		double percent = BackstageSet.getCurrentBackstageSet().debtTransferFee;// 债权转让管理费费率
		t_invest_transfer_details auctionDebtRecord = GenericModel
				.find(" offer_price=? and transfer_id=? ", pPayAmt, debtId)
				.first();

		double managefee = Double.parseDouble(Debt.debtBidReceiveSituation(
				invest.id).get("remainReceivedInterest"))
				* percent / 100;
		toUserId = auctionDebtRecord.user_id;

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("fromUserId", fromUserId);
		map.put("toUserId", toUserId);
		map.put("bidNo", bidNo);
		map.put("pCreMerBillNo", pCreMerBillNo);
		map.put("pCretAmt", pCretAmt);
		map.put("pPayAmt", pPayAmt);
		map.put("managefee", managefee);

		return map;
	}

	/**
	 * 更新订单号
	 * 
	 * @param id
	 * @param merBillNo
	 * @param error
	 */
	public static void updateMerBillNo(long id, String merBillNo,
			ErrorInfo error) {
		error.clear();

		String sql = "update t_invest_transfers set mer_bill_no = ? where id = ?";
		EntityManager em = JPA.em();
		Query query = em.createQuery(sql).setParameter(1, merBillNo)
				.setParameter(2, id);
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
		error.msg = "更新订单号成功";
	}

	/**
	 * 确认债权成交，记录对应竞拍者的资金转移
	 * 
	 * @param investTransferId
	 *            转让债权ID
	 * @return 返回 1 代表操作成功
	 */
	public static int dealDebtTransfer(String paymentMerBillNo,
			long investTransferId, ErrorInfo error) {

		EntityManager em = JPA.em();

		Double maxOfferPrice = 0.0;
		t_invest_transfer_details auctionDebtRecord = new t_invest_transfer_details();
		t_invest_transfers debt = new t_invest_transfers();
		t_invests invest = new t_invests();
		t_users assignUser = new t_users();
		List<Map<String, Object>> userIdMap = null;
		t_invest_transfer_details transferDetails = new t_invest_transfer_details();
		t_invest_transfers transfers = new t_invest_transfers();
		DealDetail detail = null;
		Map<String, Double> funds = new HashMap<String, Double>();
		BackstageSet set = BackstageSet.getCurrentBackstageSet();
		double percent = set.debtTransferFee;// 债权转让管理费费率

		try {
			debt = GenericModel.findById(investTransferId);
			invest = GenericModel.findById(debt.invest_id);
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
			error.code = -111;
			JPA.setRollbackOnly();

			return error.code;
		}

		if (debt.status == Constants.TRANSFER_STATUS) {
			error.msg = "对不起！该债权还没有审核，请耐心等待！";
			error.code = -1;

			return error.code;
		}

		if (debt.status != Constants.DIRECTIONAL_MODE
				&& debt.status != Constants.AUCTION_MODE
				&& debt.status != Constants.WAIT_CONFIRM) {
			error.msg = "对不起！该债权已经不处于待成交状态！";
			error.code = -1;

			return error.code;

		}

		maxOfferPrice = queryMaxPrice(investTransferId, error);

		if (error.code < 0) {
			error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
			error.code = -111;
			JPA.setRollbackOnly();

			return error.code;
		}

		if (maxOfferPrice == 0) {// 说明没有竞拍记录，不能执行成交操作

			error.msg = "对不起！该债权暂时没有用户参与竞拍，暂时不能成交！";
			error.code = -111;
			JPA.setRollbackOnly();

			return error.code;
		}

		if (debt.specified_user_id <= 0) {// 没有指定用户说明是竞价模式

			/* 修改债权相关状态字段 */
			try {
				auctionDebtRecord = GenericModel.find(
						" offer_price=? and transfer_id=? ", maxOfferPrice,
						investTransferId).first();
				debt = GenericModel
						.findById(auctionDebtRecord.transfer_id);
			} catch (Exception e) {
				e.printStackTrace();
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				JPA.setRollbackOnly();

				return error.code;
			}

			debt.transaction_price = maxOfferPrice;
			debt.transaction_time = new Date();
			debt.transaction_user_id = auctionDebtRecord.user_id;

			/* 3 成功 */
			debt.status = Constants.DEBT_SUCCESS;

			try {

				debt.save();
				/* 已转让出,transfer_status=-1 */
				int a = em
						.createQuery(
								"update t_invests set transfer_status = -1 where id = ? and transfer_status = 1")
						.setParameter(1, debt.invest_id).executeUpdate();

				if (a == 0) {
					error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
					error.code = -111;
					JPA.setRollbackOnly();

					return error.code;
				}

			} catch (Exception e) {
				e.printStackTrace();
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				JPA.setRollbackOnly();

				return error.code;
			}

			// 更新竞拍记录状态
			int rows = changeAuctionStatus(investTransferId,
					auctionDebtRecord.user_id, Constants.AUCTION_DETAIL_DEAL);

			if (rows == 0) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				JPA.setRollbackOnly();

				return error.code;
			}

			/* 原始投资人 */
			long originalInvestUserId = invest.user_id;

			DataSafety data1 = new DataSafety();
			data1.setId(originalInvestUserId);// 数据防篡改（针对债权所有者）
			boolean flag1 = data1.signCheck(error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -11;

				return error.code;
			}

			if (!flag1) {
				error.msg = "对不起！尊敬的用户，债权所有者账户资金出现异常变动，暂时不能成交该债权，请速联系管理员！";
				error.code = -11;

				return error.code;
			}

			t_invests tInvest = new t_invests();// 债权竞拍成功在投资表里面插入新纪录

			tInvest.user_id = auctionDebtRecord.user_id;
			tInvest.time = new Date();
			tInvest.bid_id = invest.bid_id;
			tInvest.amount = invest.amount;
			tInvest.correct_amount = invest.correct_amount;
			tInvest.correct_interest = invest.correct_interest;
			tInvest.transfer_status = 0;
			tInvest.transfers_id = investTransferId;

			tInvest.save();

			/* 现在投资人 */
			long presentInvestUserId = auctionDebtRecord.user_id;

			DataSafety data2 = new DataSafety();
			data2.setId(presentInvestUserId);// 数据防篡改（针对债权竞拍者）
			boolean flag2 = data2.signCheck(error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -11;

				return error.code;
			}

			if (!flag2) {
				error.msg = "对不起！尊敬的用户，您的账户资金出现异常变动，暂时不能成交该债权，请速联系管理员！";
				error.code = -11;

				return error.code;
			}

			/* 标ID */
			long bidId = invest.bid_id;

			// 计算债权转让管理费
			double managefee = Double.parseDouble(Debt.debtBidReceiveSituation(
					invest.id).get("remainReceivedInterest"))
					* percent / 100;

			/* 更新账单表 */
			Bill.investBillsTransfer(bidId, originalInvestUserId, invest.id,
					error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			Bill.debtTransfer(paymentMerBillNo, bidId, originalInvestUserId,
					presentInvestUserId, invest.id, tInvest.id, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			/* 出价最高用户冻结资金减少 */
			int result = DealDetail.minusUserFreezeFund(presentInvestUserId,
					maxOfferPrice);

			if (result <= 0) {
				JPA.setRollbackOnly();
				return error.code;
			}

			// 添加交易记录
			funds = DealDetail.queryUserFund(presentInvestUserId, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();
				return error.code;
			}

			detail = new DealDetail(presentInvestUserId,
					DealType.CHARGE_FREEZE_AUCTIONAMOUNT, maxOfferPrice,
					investTransferId, funds.get("user_amount"),
					funds.get("freeze"), funds.get("receive_amount"),
					"债权竞拍成功，冻结金额减少");
			detail.addDealDetail(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			data2.setId(presentInvestUserId);
			data2.updateSign(error);// 债权竞拍用户更新防数据篡改字段

			if (error.code < 0) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -11;
				JPA.setRollbackOnly();
				return error.code;
			}

			/* 债权转让人可用余额增加 */

			int result2 = DealDetail.addUserFund(originalInvestUserId,
					maxOfferPrice);

			if (result2 <= 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			// 添加交易记录
			funds = DealDetail.queryUserFund(originalInvestUserId, error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常！请您重试或联系平台管理员！";
				JPA.setRollbackOnly();

				return error.code;
			}

			detail = new DealDetail(originalInvestUserId,
					DealType.AUCTION_DEBT_SUCCESS, maxOfferPrice,
					investTransferId, funds.get("user_amount"),
					funds.get("freeze"), funds.get("receive_amount"),
					"竞拍模式债权转让成功，增加可用余额");
			detail.addDealDetail(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}
			// 更新防数据篡改字段
			data1.setId(originalInvestUserId);
			data1.updateSign(error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常，请联系平台管理员！";
				error.code = -1;
				JPA.setRollbackOnly();
				return error.code;
			}

			// 扣除债权人转让管理费

			int result3 = DealDetail.minusUserFund(originalInvestUserId,
					managefee);

			if (result3 <= 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			funds = DealDetail.queryUserFund(originalInvestUserId, error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常！请您重试或联系平台管理员！";
				JPA.setRollbackOnly();

				return error.code;
			}

			// 添加扣除管理费交易记录
			detail = new DealDetail(originalInvestUserId,
					DealType.CHARGE_DEBT_TRANSFER_MANAGEFEE, managefee,
					investTransferId, funds.get("user_amount"),
					funds.get("freeze"), funds.get("receive_amount"),
					"竞拍模式债权转让成功，扣除转让管理费");
			detail.addDealDetail(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			// -------------------发送消息-----------------------
			t_users debtUser = new t_users();
			t_users auctionUser = new t_users();
			// 发送通知
			try {
				debtUser = GenericModel.findById(originalInvestUserId);// 债权所有者
				auctionUser = GenericModel.findById(originalInvestUserId);// 债权竞拍用户
			} catch (Exception e) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -11;
				JPA.setRollbackOnly();
				return error.code;
			}

			if (error.code < 0) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -11;
				JPA.setRollbackOnly();
				return error.code;
			}

			// 发送消息(竞拍成功)
			TemplateEmail email = new TemplateEmail();// 发送邮件
			email.id = Templets.E_AUCTION_SUCCESS;

			if (email.status) {
				String content = email.content;
				content = content.replace("userName", auctionUser.name);
				content = content.replace("date", debt.time + "");
				content = content.replace("describe", debt.title);
				content = content.replace("money", debt.transaction_price + "");
				TemplateEmail.addEmailTask(auctionUser.email, email.title, content);
			}

			TemplateStation station = new TemplateStation();// 发送站内信
			station.id = Templets.M_AUCTION_SUCCESS;

			if (station.status) {
				String sContent = station.content;
				sContent = sContent.replace("userName", auctionUser.name);
				sContent = sContent.replace("date", debt.time + "");
				sContent = sContent.replace("describe", debt.title);
				sContent = sContent.replace("money", debt.transaction_price
						+ "");
				TemplateStation.addMessageTask(auctionUser.id, station.title, sContent);
			}

			TemplateSms sms = new TemplateSms();// 发送短信
			sms.id = Templets.S_AUCTION_SUCCESS;

			if (sms.status) {
				String smscontent = sms.content;
				smscontent = smscontent.replace("userName", auctionUser.name);
				smscontent = smscontent.replace("date", debt.time + "");
				smscontent = smscontent.replace("describe", debt.title);
				smscontent = smscontent.replace("money", debt.transaction_price
						+ "");
				TemplateSms.addSmsTask(auctionUser.mobile, smscontent);
			}

			// 发送消息（债权转让成功）
			email.id = Templets.E_DEBT_TRANSFER_SUCCESS;

			if (email.status) {
				String content1 = email.content;
				content1 = content1.replace("date", debt.time + "");
				content1 = content1.replace("userName", debtUser.name);
				content1 = content1.replace("describe", debt.title);
				content1 = content1.replace("money", debt.transaction_price
						+ "");
				content1 = content1.replace("debtReward", managefee + "");
				TemplateEmail.addEmailTask(debtUser.email, email.title, content1);
			}

			station = new TemplateStation();// 发送站内信
			station.id = Templets.M_DEBT_TRANSFER_SUCCESS;

			if (station.status) {
				String stationContent1 = station.content;
				stationContent1 = stationContent1.replace("date", debt.time
						+ "");
				stationContent1 = stationContent1.replace("userName",
						debtUser.name);
				stationContent1 = stationContent1.replace("describe",
						debt.title);
				stationContent1 = stationContent1.replace("money",
						debt.transaction_price + "");
				stationContent1 = stationContent1.replace("debtReward",
						managefee + "");
				TemplateStation.addMessageTask(debtUser.id, station.title,
						stationContent1);
			}

			sms = new TemplateSms();// 发送短信
			sms.id = Templets.S_DEBT_TRANSFER_SUCCESS;

			if (sms.status) {
				String smscontent1 = sms.content;
				smscontent1 = smscontent1.replace("date", debt.time + "");
				smscontent1 = smscontent1.replace("userName", debtUser.name);
				smscontent1 = smscontent1.replace("describe", debt.title);
				smscontent1 = smscontent1.replace("money",
						debt.transaction_price + "");
				smscontent1 = smscontent1.replace("debtReward", managefee + "");
				TemplateSms.addSmsTask(debtUser.mobile, smscontent1);
			}

			// --------------------------------------------------------------

			// 更新防数据篡改字段
			data1.setId(originalInvestUserId);
			data1.updateSign(error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常，请联系平台管理员！";
				error.code = -1;
				JPA.setRollbackOnly();
				return error.code;
			}

			// 添加平台交易记录
			DealDetail.addPlatformDetail(DealType.TRANSFER_FEE,
					investTransferId, originalInvestUserId, -1,
					DealType.ACCOUNT, managefee, 1, "扣除债权转让管理费", error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			/* 解冻竞拍失败者冻结资金 */
			String sql = "select new Map(user_id as userId,offer_price as offerPrice ) from t_invest_transfer_details  where transfer_id=? and offer_price < ?";

			try {
				userIdMap = GenericModel.find(sql,
						investTransferId, maxOfferPrice).fetch();

			} catch (Exception e) {
				e.printStackTrace();
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				JPA.setRollbackOnly();

				return error.code;
			}

			DataSafety d = new DataSafety();
			if (null != userIdMap) {

				for (Map<String, Object> map : userIdMap) {

					long userId = Long.parseLong(map.get("userId").toString());
					double offerPrice = Double.parseDouble(map
							.get("offerPrice").toString());

					// 解冻竞拍资金
					int resulta = DealDetail.relieveFreezeFund(userId,
							offerPrice);

					if (resulta <= 0) {
						JPA.setRollbackOnly();
						return error.code;
					}

					// 添加交易记录
					funds = DealDetail.queryUserFund(userId, error);

					if (error.code < 0) {
						JPA.setRollbackOnly();

						return error.code;
					}

					double user_amount = funds.get("user_amount");
					double freeze = funds.get("freeze");
					double receive_amount = funds.get("receive_amount");

					detail = new DealDetail(userId,
							DealType.THAW_FREEZE_AUCTIONAMOUNT, offerPrice,
							investTransferId, user_amount - offerPrice, freeze,
							receive_amount, "竞拍债权失败，解冻竞拍金额");
					detail.addDealDetail(error);

					if (error.code < 0) {
						JPA.setRollbackOnly();

						return error.code;
					}

					detail = new DealDetail(userId,
							DealType.REVENUE_FREEZE_FUND, offerPrice,
							investTransferId, user_amount, freeze,
							receive_amount, "竞拍债权失败，返还竞拍冻结金额");

					detail.addDealDetail(error);

					if (error.code < 0) {
						error.code = -25;
						error.msg = "添加交易记录失败!";
						JPA.setRollbackOnly();

						return error.code;
					}

					t_users user = new t_users();

					try {
						user = GenericModel.findById(userId);
					} catch (Exception e) {
						e.printStackTrace();
						error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
						error.code = -111;
						JPA.setRollbackOnly();

						return error.code;
					}

					// //发送消息(竞拍失败)
					email = new TemplateEmail();// 发送邮件
					email.id = Templets.E_AUCTION_FAILURE;

					if (email.status) {
						String content2 = email.content;
						content2 = content2.replace("userName", user.name);
						content2 = content2.replace("date", debt.time + "");
						content2 = content2.replace("describe", debt.title);
						content2 = content2.replace("money", offerPrice + "");
						TemplateEmail.addEmailTask(user.email, email.title, content2);
					}

					station = new TemplateStation();// 发送站内信
					station.id = Templets.M_AUCTION_FAILURE;

					if (station.status) {
						String stationContent2 = station.content;
						stationContent2 = stationContent2.replace("userName",
								user.name);
						stationContent2 = stationContent2.replace("date",
								debt.time + "");
						stationContent2 = stationContent2.replace("describe",
								debt.title);
						stationContent2 = stationContent2.replace("money",
								offerPrice + "");
						TemplateStation.addMessageTask(user.id, station.title,
								stationContent2);
					}

					sms = new TemplateSms();// 发送短信
					sms.id = Templets.S_AUCTION_FAILURE;

					if (sms.status) {
						String smscontent2 = sms.content;
						smscontent2 = smscontent2
								.replace("userName", user.name);
						smscontent2 = smscontent2.replace("date", debt.time
								+ "");
						smscontent2 = smscontent2.replace("describe",
								debt.title);
						smscontent2 = smscontent2.replace("money", offerPrice
								+ "");
						TemplateSms.addSmsTask(user.mobile, smscontent2);
					}

					// 更新数据防篡改字段
					d.setId(userId);
					d.updateSign(error);

					if (error.code < 0) {
						JPA.setRollbackOnly();
						return error.code;
					}

					/* 修改竞拍记录表状态值 */

					int resultc = changeAuctionStatus(investTransferId, userId,
							Constants.AUCTION_DETAIL_FAILURE);
					if (resultc < 0) {
						JPA.setRollbackOnly();
						return -1;
					}
				}
			}

			// 添加事件
			DealDetail.userEvent(originalInvestUserId,
					UserEvent.ACCEPT_DEBT_TRANSFER, "债权成交操作成功", error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			// 更新缓存
			Map<String, Double> v = DealDetail.queryUserFund(
					originalInvestUserId, error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			// 奖励债权竞拍用户推广用户CPS奖励
			User.rewardCPS(presentInvestUserId, managefee, investTransferId,
					error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			User user = User.currUser();
			user.balance = v.get("user_amount");
			user.freeze = v.get("freeze");
			User.setCurrUser(user);

			error.msg = "债权成交操作成功！";
			error.code = 1;
			return error.code;

		} else {

			/* 定向转让模式 */
			try {
				transferDetails = GenericModel.find(
						" transfer_id=?", investTransferId).first();
				transfers = GenericModel
						.findById(transferDetails.transfer_id);

				/* 债权转让指定人 */
				assignUser = GenericModel.findById(transferDetails.user_id);

				/* 原始投资记录 */
				invest = GenericModel.findById(transfers.invest_id);
			} catch (Exception e) {
				e.printStackTrace();
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				JPA.setRollbackOnly();

				return error.code;
			}

			/* 原始投资人 */
			long originalInvestUserId = invest.user_id;

			DataSafety data1 = new DataSafety();
			data1.setId(originalInvestUserId);// 数据防篡改（针对债权所有者）
			boolean flag1 = data1.signCheck(error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -11;

				return error.code;
			}

			if (!flag1) {
				error.msg = "对不起！尊敬的用户，债权所有者账户资金出现异常变动，暂时不能成交该债权，请速联系管理员！";
				error.code = -11;

				return error.code;
			}

			/* 现在投资人 */
			long presentInvestUserId = assignUser.id;

			DataSafety data2 = new DataSafety();
			data2.setId(presentInvestUserId);// 数据防篡改（针对债权所有者）
			boolean flag2 = data1.signCheck(error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -11;

				return error.code;
			}

			if (!flag2) {
				error.msg = "对不起！尊敬的用户，债权所有者账户资金出现异常变动，暂时不能成交该债权，请速联系管理员！";
				error.code = -11;

				return error.code;
			}

			/* 投资标ID */
			long bidId = invest.bid_id;

			if (assignUser.balance < transferDetails.offer_price) {
				error.msg = "对不起！您可用余额不足，请及时充值";
				error.code = -1;

				return error.code;
			}

			/* 3 成功 */
			transfers.status = 3;
			transfers.transaction_price = transferDetails.offer_price;
			transfers.transaction_time = new Date();
			transfers.transaction_user_id = transferDetails.user_id;

			try {
				transfers.save();

			} catch (Exception e) {
				e.printStackTrace();
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				JPA.setRollbackOnly();

				return error.code;
			}

			// 计算债权转让管理费
			double managefee = Arith.round(
					Double.parseDouble(Debt.debtBidReceiveSituation(invest.id)
							.get("remainReceivedInterest")) * percent / 100, 2);

			/* 更新账单表 */
			Bill.investBillsTransfer(bidId, originalInvestUserId, invest.id,
					error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			// 冻结指定用户可用余额
			int row = DealDetail.freezeFund(assignUser.id,
					transferDetails.offer_price);

			if (row < 1) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -11;
				JPA.setRollbackOnly();

				return error.code;
			}

			funds = DealDetail.queryUserFund(assignUser.id, error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -11;
				JPA.setRollbackOnly();

				return error.code;
			}

			double user_amount = funds.get("user_amount");
			double freeze = funds.get("freeze");
			double receive_amount = funds.get("receive_amount");

			/* 伪构记录 */
			detail = new DealDetail(assignUser.id, DealType.PAY_FREEZE_FUND,
					transferDetails.offer_price, investTransferId, user_amount,
					freeze - transferDetails.offer_price, receive_amount,
					"债权竞拍成功，支出竞拍冻结金额");

			detail.addDealDetail(error);

			if (error.code < 0) {
				error.code = -12;
				error.msg = "添加交易记录失败!";
				JPA.setRollbackOnly();

				return error.code;
			}

			// 添加冻结可用余额交易记录
			detail = new DealDetail(assignUser.id, DealType.FREEZE_DEBT,
					transferDetails.offer_price, investTransferId, user_amount,
					freeze, receive_amount, "债权竞拍成功，冻结竞拍金额"
							+ transferDetails.offer_price + "元");
			detail.addDealDetail(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -11;

				return error.code;
			}

			data2.setId(presentInvestUserId);// 数据防篡改（针对债权竞拍者）
			data2.updateSign(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -11;

				return error.code;
			}

			/* 更改投资表状态 已转让出 */
			int result = changeInvestStatus(transfers.invest_id, -1);
			if (result < 0) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				JPA.setRollbackOnly();

				return error.code;
			}

			/* 债权竞拍成功在投资表里面插入新纪录 */
			t_invests tInvest = new t_invests();

			tInvest.user_id = transferDetails.user_id;
			tInvest.time = new Date();
			tInvest.bid_id = invest.bid_id;
			tInvest.amount = invest.amount;
			tInvest.correct_amount = invest.correct_amount;
			tInvest.correct_interest = invest.correct_interest;
			tInvest.transfer_status = 0;
			tInvest.transfers_id = investTransferId;

			try {
				tInvest.save();
			} catch (Exception e) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				JPA.setRollbackOnly();

				return error.code;
			}

			Bill.debtTransfer(paymentMerBillNo, bidId, originalInvestUserId,
					presentInvestUserId, invest.id, tInvest.id, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			/* 竞拍者冻结金额减少 */
			int resulta = DealDetail.minusUserFreezeFund(assignUser.id,
					transferDetails.offer_price);

			if (resulta < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			data2.setId(presentInvestUserId);// 数据防篡改（针对债权竞拍者）
			data2.updateSign(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			// 添加交易记录
			funds = DealDetail.queryUserFund(assignUser.id, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			user_amount = funds.get("user_amount");
			detail = new DealDetail(assignUser.id,
					DealType.CHARGE_FREEZE_AUCTIONAMOUNT,
					transferDetails.offer_price, investTransferId, user_amount,
					funds.get("freeze"), funds.get("receive_amount"),
					"债权竞拍成功，冻结金额减少" + transferDetails.offer_price + "元");
			detail.addDealDetail(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			data2.setId(presentInvestUserId);// 数据防篡改（针对债权竞拍者）
			data2.updateSign(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			/* 债权转让人可用余额增加 */
			int resultd = DealDetail.addUserFund(invest.user_id,
					transferDetails.offer_price);

			if (resultd < 0) {
				JPA.setRollbackOnly();
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				return error.code;
			}

			data1.setId(originalInvestUserId);// 数据防篡改（针对债权所有者）
			data1.updateSign(error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				return error.code;
			}

			// 添加交易记录
			funds = DealDetail.queryUserFund(invest.user_id, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			detail = new DealDetail(invest.user_id,
					DealType.DIRECT_DEBT_SUCCESS, transferDetails.offer_price,
					investTransferId, funds.get("user_amount"),
					funds.get("freeze"), funds.get("receive_amount"),
					"定向债权转让成功，增加可用余额");
			detail.addDealDetail(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			// 扣除债权人转让管理费

			int result3 = DealDetail.minusUserFund(invest.user_id, managefee);

			if (result3 <= 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			data1.setId(originalInvestUserId);// 数据防篡改（针对债权所有者）
			data1.updateSign(error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				return error.code;
			}

			funds = DealDetail.queryUserFund(invest.user_id, error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常！请您重试或联系平台管理员！";
				JPA.setRollbackOnly();

				return error.code;
			}

			// 添加扣除管理费交易记录
			detail = new DealDetail(invest.user_id,
					DealType.CHARGE_DEBT_TRANSFER_MANAGEFEE, managefee,
					investTransferId, funds.get("user_amount"),
					funds.get("freeze"), funds.get("receive_amount"),
					"定向模式债权转让成功，扣除转让管理费");
			detail.addDealDetail(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			data1.setId(originalInvestUserId);// 数据防篡改（针对债权所有者）
			data1.updateSign(error);

			/* 竞拍记录状态改变 状态：0 正常；1 成交；2 待接受；-1 失败 */
			try {
				int rows = em
						.createQuery(
								"update  t_invest_transfer_details set status=1 where id=? and status = 2")
						.setParameter(1, transferDetails.id).executeUpdate();

				if (rows == 0) {
					JPA.setRollbackOnly();
					error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
					error.code = -111;
					return error.code;
				}
			} catch (Exception e) {
				e.printStackTrace();
				JPA.setRollbackOnly();
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				return error.code;
			}

			// 添加事件
			DealDetail.userEvent(invest.user_id, UserEvent.DEAL_DEBT_TRANSFER,
					"债权成交操作成功", error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			// 更新缓存
			Map<String, Double> v = DealDetail.queryUserFund(invest.user_id,
					error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			// 奖励债权竞拍用户推广用户CPS奖励
			User.rewardCPS(presentInvestUserId, managefee, investTransferId,
					error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			User user = User.currUser();
			user.balance = v.get("user_amount");
			user.freeze = v.get("freeze");
			User.setCurrUser(user);

			error.msg = "债权成交操作成功！";
			error.code = 1;

			if (v.get("user_amount") < 0 || v.get("freeze") < 0) {
				error.msg = "对不起！资金出现负数，不能进行操作。给您造成的不便敬请谅解！";
				error.code = -1;
				JPA.setRollbackOnly();

				return error.code;
			}

			return error.code;

		}

	}

	/**
	 * 获取债权表特定标版本号
	 * 
	 * @param bidId
	 * @param error
	 * @return
	 */
	public static int getDebtVersion(long transfer_id, ErrorInfo error) {

		int version = 0;
		String sql = "select version from t_invest_transfers where id = ?";

		try {
			version = GenericModel.find(sql, transfer_id).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常！请您联系平台管理员！";
			error.code = -1;
		}

		error.code = 1;
		return version;
	}

	/**
	 * 更新债权表Version字段
	 * 
	 * @param bidId
	 * @param error
	 * @return
	 */
	public static int updatevVersion(long transfer_id, int version,
			ErrorInfo error) {

		EntityManager em = JPA.em();
		String sql = "update t_invest_transfers set version = version + 1 where id = ? and version = ?";

		try {
			int rows = em.createQuery(sql).setParameter(1, transfer_id)
					.setParameter(2, version).executeUpdate();

			if (rows == 0) {
				JPA.setRollbackOnly();
				error.msg = "对不起！系统异常！请您联系平台管理员！";
				error.code = -1;
			}

		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常！请您联系平台管理员！";
			error.code = -1;
		}

		error.code = 1;
		return error.code;
	}

	/**
	 * 竞拍同一债权的时候进行更新
	 * 
	 * @param transferDetailId
	 * @param offerPrice
	 * @param error
	 */
	public static void updateAuctionDetail(long transferDetailId,
			double offerPrice, ErrorInfo error) {

		EntityManager em = JPA.em();

		try {
			int rows = em
					.createQuery(
							"update t_invest_transfer_details set time = ? , offer_price = ? where id = ?")
					.setParameter(1, new Date()).setParameter(2, offerPrice)
					.setParameter(3, transferDetailId).executeUpdate();

			if (rows == 0) {
				JPA.setRollbackOnly();
				error.code = -1;
				return;
			}

		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			error.code = -1;
			return;
		}

		error.code = 1;
	}

	/**
	 * 定时判断债权流拍
	 */
	public static void judgeDebtFlow() {

		String sql = "select id from t_invest_transfers where status in (-1,1,2,4) and end_time < ? ";
		List<Long> transfers = new ArrayList<Long>();

		try {
			transfers = GenericModel.find(sql, new Date()).fetch();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (transfers.size() > 0) {

			for (int i = 0; i < transfers.size(); i++) {
				debtTransferFailure(transfers.get(i));
			}

		}

	}

	/**
	 * 竞拍债权
	 * 
	 * @param userId
	 * @param offerPrice
	 * @param transfer_id
	 * @param error
	 * @return 返回 -1 代表竞拍失败 ，返回 1 代表竞拍成功
	 */
	public static int auctionDebt(long userId, int offerPrice,
			long transfer_id, ErrorInfo error) {

		error.clear();
		Double maxOfferPrice = 0.0;
		t_users users = new t_users();
		t_invest_transfers transfer = new t_invest_transfers();
		t_invests invest = new t_invests();
		List<t_invest_transfer_details> list = null;
		DealDetail dealDetail = null;

		DataSafety data = new DataSafety();
		data.setId(userId);// 数据防篡改（针对竞拍会员）
		boolean sign = data.signCheck(error);

		if (error.code < 0) {
			error.msg = "对不起！尊敬的用户，你的账户资金出现异常变动，请速联系管理员！";
			error.code = -1;
			return error.code;
		}

		if (!sign) {
			error.msg = "对不起！尊敬的用户，你的账户资金出现异常变动，请速联系管理员！";
			error.code = -1;
			return error.code;
		}

		try {
			users = GenericModel.findById(userId);

			transfer = GenericModel.findById(transfer_id);

			invest = GenericModel.findById(transfer.invest_id);
		} catch (Exception e) {
			error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
			error.code = -111;
			JPA.setRollbackOnly();// 发生异常，事务回滚

			return error.code;
		}

		long bidUserId = getBidUserId(transfer_id, error);// 获取债权的借款用户

		if (error.code < 0) {
			error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
			error.code = -8;

			return error.code;
		}

		if (userId == bidUserId) {
			error.msg = "对不起！借款持有用户禁止参与该债权交易！";
			error.code = -9;

			return error.code;
		}

		if (transfer.status != Constants.AUCTION_STATUS) {
			error.msg = "对不起！该债权不处于竞拍状态！";
			error.code = -8;

			return error.code;
		}

		/* 投资者Id */
		long investUserId = invest.user_id;

		if (userId == investUserId) {
			error.msg = "对不起！您不能竞拍自己转让的债权";
			error.code = -4;

			return error.code;
		}

		if (User.isInMyBlacklist(investUserId, userId, error) < 0) {
			error.msg = "对不起！您已经被债权所有者拉黑,不能竞拍该债权！";
			error.code = -5;

			return error.code;
		}

		/* 判断竞拍会员是否被管理员拉黑 */
		if (users.is_blacklist) {
			error.msg = "对不起！您已经被平台管理员限制操作！请您与平台管理员联系！";
			error.code = -6;

			return error.code;
		}

		Date endTime = transfer.end_time;

		if (endTime.getTime() < new Date().getTime()) {

			error.msg = "对不起！该债权已经过期！";
			error.code = -8;

			return error.code;
		}

		if (offerPrice < transfer.transfer_price) {
			error.msg = "对不起！您的出价应该高于债权底价！";
			error.code = -8;

			return error.code;
		}

		if (offerPrice > transfer.debt_amount) {
			error.msg = "对不起！您的出价不能高于债权总额！";
			error.code = -3;

			return error.code;
		}

		try {
			list = GenericModel.find(
					"from t_invest_transfer_details where transfer_id=?",
					transfer_id).fetch();

		} catch (Exception e) {
			error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
			error.code = -111;
			JPA.setRollbackOnly();// 发生异常，事务回滚

			return error.code;
		}

		if (null != list && list.size() > 0) {

			/* 说明已经有人竞拍,找出最大出价 */
			maxOfferPrice = queryMaxPrice(transfer_id, error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				JPA.setRollbackOnly();// 发生异常，事务回滚

				return error.code;
			}

			if (offerPrice <= maxOfferPrice) {
				error.msg = "对不起！您出价应该高于" + maxOfferPrice + "元！";
				error.code = -1;

				return error.code;
			}
		}
		double transferPrice = transfer.transfer_price;

		if (users.balance < transferPrice) {
			error.msg = "对不起！您可用余额不足";
			error.code = -2;
			return error.code;
		}

		// 获取verison
		int version = getDebtVersion(transfer_id, error);

		if (error.code < 0) {
			error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
			error.code = -111;
			JPA.setRollbackOnly();

			return error.code;

		}

		t_invest_transfer_details investTransferDetails = new t_invest_transfer_details();

		investTransferDetails.transfer_id = transfer_id;
		investTransferDetails.time = new Date();
		investTransferDetails.user_id = userId;
		investTransferDetails.offer_price = offerPrice;
		investTransferDetails.status = 0;

		/* 查询用户之前有没有竞拍该债权 */
		t_invest_transfer_details detail = GenericModel.find(
				" user_id = ? and transfer_id = ?", userId, transfer_id)
				.first();

		if (detail != null) {

			updateAuctionDetail(detail.id, offerPrice, error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常，请联系平台管理员！";
				error.code = -2;
				return error.code;
			}

			/* 说明该会员之前竞拍过该债权，继续冻结比上次出价高出的价格 */
			int result = DealDetail.freezeFund(userId, offerPrice
					- detail.offer_price);

			if (result < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			Map<String, Double> funds = DealDetail.queryUserFund(userId, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			double user_amount = funds.get("user_amount");
			double freeze = funds.get("freeze");
			double receive_amount = funds.get("receive_amount");

			/* 伪构记录 */
			dealDetail = new DealDetail(userId, DealType.PAY_FREEZE_FUND,
					offerPrice - detail.offer_price, transfer_id, user_amount,
					freeze - (offerPrice - detail.offer_price), receive_amount,
					"竞拍相同债权成功，继续支出竞拍冻结金额");

			dealDetail.addDealDetail(error);

			if (error.code < 0) {
				error.code = -25;
				error.msg = "添加交易记录失败!";
				JPA.setRollbackOnly();

				return error.code;
			}

			// 添加交易记录
			dealDetail = new DealDetail(userId, DealType.FREEZE_DEBT,
					offerPrice - detail.offer_price, transfer_id, user_amount,
					freeze, receive_amount, "竞拍相同债权成功，继续冻结对应可用余额"
							+ (offerPrice - detail.offer_price) + "元");

			try {
				dealDetail.addDealDetail(error);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {

				Map<String, Double> details = DealDetail.queryUserFund(userId,
						error);

				if (error.code < 0) {
					error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
					error.code = -111;
					JPA.setRollbackOnly();

					return error.code;
				}

				if (details.get("user_amount") < 0) {
					error.msg = "对不起！您的可用余额不足，请及时充值！";
					error.code = -111;
					JPA.setRollbackOnly();

					return error.code;
				}
			}

			data.setId(userId);// 更新数据防篡改字段
			data.updateSign(error);

			if (error.code < 0) {
				error.msg = "对不起！系统异常，请联系平台管理员！";
				JPA.setRollbackOnly();

				return error.code;
			}

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

		} else {

			/* 冻结竞拍者资金 可用余额减少 */
			int result = DealDetail.freezeFund(userId, offerPrice);

			if (result < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			Map<String, Double> funds = DealDetail.queryUserFund(userId, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			double user_amount = funds.get("user_amount");
			double freeze = funds.get("freeze");
			double receive_amount = funds.get("receive_amount");

			/* 伪构记录 */
			dealDetail = new DealDetail(userId, DealType.PAY_FREEZE_FUND,
					offerPrice, transfer_id, user_amount, freeze - offerPrice,
					receive_amount, "竞拍债权成功，支出竞拍冻结金额");

			dealDetail.addDealDetail(error);

			if (error.code < 0) {
				error.code = -25;
				error.msg = "添加交易记录失败!";
				JPA.setRollbackOnly();

				return error.code;
			}

			// 添加交易记录
			dealDetail = new DealDetail(userId, DealType.FREEZE_DEBT,
					offerPrice, transfer_id, user_amount, freeze,
					receive_amount, "竞拍债权成功，冻结对应可用余额" + offerPrice + "元");
			dealDetail.addDealDetail(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return error.code;
			}

			data.setId(userId);// 更新数据防篡改字段
			data.updateSign(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.msg = "对不起！系统异常，请联系平台管理员！";
				return error.code;
			}

			try {

				/* 添加债权竞拍记录 */
				investTransferDetails.save();

			} catch (Exception e) {
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				JPA.setRollbackOnly();

				return error.code;
			} finally {
				Map<String, Double> details = DealDetail.queryUserFund(userId,
						error);

				if (error.code < 0) {
					error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
					error.code = -111;
					JPA.setRollbackOnly();

					return error.code;
				}

				if (details.get("user_amount") < 0) {
					error.msg = "对不起！您的可用余额不足，请及时充值！";
					error.code = -111;
					JPA.setRollbackOnly();

					return error.code;
				}
			}

		}

		/* 更新该债权竞拍次数 */
		int result = updateDebtJoinTimes(transfer_id, error);

		if (result < 0) {
			error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
			error.code = -111;
			JPA.setRollbackOnly();

			return error.code;
		}

		// 更新会员性质
		User.updateMasterIdentity(userId, Constants.INVEST_USER, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		DealDetail.userEvent(userId, UserEvent.AUCTION, "竞拍债权成功", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		// 更新verson
		updatevVersion(transfer_id, version, error);

		if (error.code < 0) {
			error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
			error.code = -111;
			JPA.setRollbackOnly();

			return error.code;

		}

		// 刷新缓存
		Map<String, Double> details = DealDetail.queryUserFund(userId, error);
		if (details.get("user_amount") < 0) {
			JPA.setRollbackOnly();
			error.msg = "对不起！您的余额不足请及时充值！";
			error.code = -111;
			return error.code;
		}

		User user = User.currUser();
		user.balance = details.get("user_amount");
		user.freeze = details.get("freeze");
		User.setCurrUser(user);

		// 发送消息（冻结竞拍金额）
		String username = users.name;
		String title = transfer.title;

		TemplateEmail email = new TemplateEmail();// 发送邮件
		email.id = Templets.E_AUCTION_FREEZE;

		if (email.status) {
			String content = email.content;
			content = content.replace("date",
					DateUtil.dateToString((new Date())));
			content = content.replace("userName", username);
			content = content.replace("describe", title);
			content = content.replace("money", offerPrice + "");
			TemplateEmail.addEmailTask(users.email, email.title, content);
		}

		TemplateStation station = new TemplateStation();// 发送站内信
		station.id = Templets.M_AUCTION_FREEZE;

		if (station.status) {
			String stationContent = station.content;
			stationContent = stationContent.replace("date",
					DateUtil.dateToString((new Date())));
			stationContent = stationContent.replace("userName", username);
			stationContent = stationContent.replace("describe", title);
			stationContent = stationContent.replace("money", offerPrice + "");
			TemplateStation.addMessageTask(userId, station.title, stationContent);
		}

		TemplateSms sms = new TemplateSms();// 发送短信
		sms.id = Templets.S_AUCTION_FREEZE;

		if (sms.status) {
			String smscontent = sms.content;
			smscontent = smscontent.replace("date",
					DateUtil.dateToString((new Date())));
			smscontent = smscontent.replace("userName", username);
			smscontent = smscontent.replace("describe", title);
			smscontent = smscontent.replace("money", offerPrice + "");
			TemplateSms.addSmsTask(users.mobile, smscontent);
		}

		error.code = 1;
		error.msg = "竞拍成功！";
		return error.code;
	}

	/**
	 * 更新债权竞拍次数
	 * 
	 * @param transfer_id
	 * @param error
	 * @return
	 */
	public static int updateDebtJoinTimes(long transfer_id, ErrorInfo error) {

		EntityManager em = JPA.em();

		try {
			int rows = em
					.createQuery(
							"update t_invest_transfers set join_times = join_times + 1  where id = ? ")
					.setParameter(1, transfer_id).executeUpdate();

			if (rows == 0) {
				JPA.setRollbackOnly();
				error.code = -1;
				return error.code;
			}
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return error.code;
		}

		error.code = 1;
		return 1;
	}

	/**
	 * 根据条件查询用户所有的转让债权
	 * 
	 * @param userId
	 * @param type
	 *            1:全部 2：借款标标题 3：借款标编号
	 * @param title
	 *            借款标标题
	 * @param bidNo
	 *            借款标编号
	 * @param status
	 *            状态
	 * @return
	 */
	public static PageBean<v_debt_user_transfer_management> queryUserAllDebtTransfersByConditions(
			long userId, String typeStr, String params, String statusStr,
			int currPage, int pageSize) {

		int status = 0;
		int type = 0;
		StringBuffer conditions = new StringBuffer(" 1=1 and user_id = ?");
		List<Object> values = new ArrayList<Object>();
		values.add(userId);

		PageBean<v_debt_user_transfer_management> page = new PageBean<v_debt_user_transfer_management>();
		List<v_debt_user_transfer_management> transferList = new ArrayList<v_debt_user_transfer_management>();
		page.pageSize = pageSize;

		if (0 == currPage) {
			page.currPage = 1;
		} else {
			page.currPage = currPage;

		}

		if (typeStr == null && params == null && statusStr == null) {

			try {
				page.totalCount = (int) GenericModel.count(
						conditions.toString(), values.toArray());
				transferList = GenericModel.find(
						conditions.toString(), values.toArray()).fetch(
						page.currPage, page.pageSize);

			} catch (Exception e) {
				e.printStackTrace();

				return null;
			}
			page.page = transferList;
			return page;
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(statusStr)) {
			status = Integer.parseInt(statusStr);
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		if (status < 0 || status > 5) {
			status = 0;
		}

		if (type == 0) {
			conditions
					.append(Constants.TRANSFER_MANAGEMENT_TYPE_CONDITION[type]);
			conditions
					.append(Constants.TRANSFER_MANAGEMENT_STATUS_CONDITION[status]);
		} else {
			conditions
					.append(Constants.TRANSFER_MANAGEMENT_TYPE_CONDITION[type]);
			conditions
					.append(Constants.TRANSFER_MANAGEMENT_STATUS_CONDITION[status]);
			values.add("%" + params + "%");
		}

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			transferList = GenericModel.find(
					conditions.toString(), values.toArray()).fetch(currPage,
					page.pageSize);

		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}
		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keyWords", params);
		conditionMap.put("type", type);
		conditionMap.put("status", status);

		page.conditions = conditionMap;
		page.page = transferList;
		return page;
	}

	/**
	 * 根据条件查询用户所有的受让债权
	 * 
	 * @param userId
	 * @param type
	 *            1:全部 2：借款标标题 3：借款标编号
	 * @param param
	 *            借款标标题 借款标编号
	 * @return
	 */
	public static PageBean<v_debt_user_receive_transfers_management> queryUserAllReceivedDebtTransfersByConditions(
			long userId, String typeStr, String params, int currPage,
			int pageSize) {

		String[] condition = { "   ", "  and bid_no like ? ",
				"  and title like ? " };
		int type = 0;
		StringBuffer conditions = new StringBuffer(
				" 1=1 and  user_id=?  and status in (0,1,2,-1,3)");
		List<Object> values = new ArrayList<Object>();
		values.add(userId);

		List<v_debt_user_receive_transfers_management> transferList = new ArrayList<v_debt_user_receive_transfers_management>();
		PageBean<v_debt_user_receive_transfers_management> page = new PageBean<v_debt_user_receive_transfers_management>();
		page.pageSize = pageSize;
		page.currPage = currPage;

		if (typeStr == null && params == null) {

			try {
				page.totalCount = (int) GenericModel
						.count(conditions.toString(), values.toArray());
				transferList = GenericModel.find(
						conditions.toString(), values.toArray()).fetch(
						currPage, page.pageSize);
			} catch (Exception e) {
				e.printStackTrace();

				return page;
			}

			page.page = transferList;

			return page;
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		if (type == 0) {
			conditions.append(condition[type]);
		} else {
			conditions.append(condition[type]);
			values.add("%" + params + "%");
		}

		try {
			page.totalCount = (int) GenericModel
					.count(conditions.toString(), values.toArray());
			transferList = GenericModel.find(
					conditions.toString(), values.toArray()).fetch(currPage,
					page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keyWords", params);
		conditionMap.put("type", type);

		page.conditions = conditionMap;
		page.page = transferList;

		return page;
	}

	/**
	 * 用户收藏的债权
	 * 
	 * @param type
	 *            1:全部 2：借款标标题 3：转让标题 4:债权编号 5：转让用户名 6：借款用户名
	 * @param userId
	 * @param params
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_attention_invest_transfers> queryUserAttentionDebtTransfers(
			long userId, int currPage, String typeStr, String paramter,
			int pageSize, ErrorInfo error) {

		String[] condition = { "   ", "  and transfer_title  like ? ",
				" and debt_transfer_no like ? ", "  and bid_title like ? ",
				" and transfer_user_name like ? ", " and bid_user_name like ? " };
		int type = 0;
		StringBuffer conditions = new StringBuffer(" 1=1 and  user_id = ?  ");
		List<Object> values = new ArrayList<Object>();
		values.add(userId);

		List<v_user_attention_invest_transfers> transferList = new ArrayList<v_user_attention_invest_transfers>();
		PageBean<v_user_attention_invest_transfers> page = new PageBean<v_user_attention_invest_transfers>();

		page.pageSize = pageSize;
		page.currPage = currPage;

		if (typeStr == null && paramter == null) {

			try {
				page.totalCount = (int) GenericModel
						.count(conditions.toString(), values.toArray());
				transferList = GenericModel.find(
						conditions.toString(), values.toArray()).fetch(
						currPage, page.pageSize);

				page.page = transferList;
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return page;
			}

			error.code = 1;
			return page;
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (type < 0 || type > 5) {
			type = 0;
		}

		if (type == 0) {
			conditions.append(condition[type]);
		} else {
			conditions.append(condition[type]);
			values.add("%" + paramter + "%");
		}

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			transferList = GenericModel.find(
					conditions.toString(), values.toArray()).fetch(currPage,
					page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -2;
			return page;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keyWords", paramter);
		conditionMap.put("type", type);

		page.conditions = conditionMap;
		page.page = transferList;
		error.code = 1;
		return page;
	}

	/**
	 * 判断还款的借款标有没有债权在转让，如果有，将其状态回归原态,并解冻竞拍者资金
	 * 
	 * @param bidId
	 * @param error
	 * @return
	 */
	public static int judgeHasBidTransfer(long bidId) {

		String sql = "select id from t_invests where transfer_status = 1 and bid_id = ?";
		List<Long> investIdList = new ArrayList<Long>();

		try {
			investIdList = GenericModel.find(sql, bidId).fetch();
		} catch (Exception e) {
			e.printStackTrace();
			JPA.setRollbackOnly();
			return -1;
		}

		if (investIdList.size() > 0) {

			for (long id : investIdList) {
				long debtTrasferId = 0;
				try {
					debtTrasferId = GenericModel
							.find("select id from t_invest_transfers where invest_id = ? and status in (1,2,4)",
									id).first();
				} catch (Exception e) {
					e.printStackTrace();
					JPA.setRollbackOnly();
					return -1;
				}
				int result = debtTransferFailure(debtTrasferId);
				if (result < 0) {
					JPA.setRollbackOnly();
					return -1;
				}
			}
		}

		return 1;
	}

	/**
	 * 债权转让流标时，改变相关状态以及资金解冻
	 * 
	 * @param investransferId
	 */
	public static int debtTransferFailure(long investransferId) {

		ErrorInfo error = new ErrorInfo();
		DealDetail dealDetail = null;
		List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();

		/* 更新债权转让状态 */
		int resulta = changeDebtStatus(investransferId, Constants.DEBT_FLOW);

		if (resulta < 0) {
			JPA.setRollbackOnly();
			return -1;
		}

		t_invest_transfers investTransfer = GenericModel
				.findById(investransferId);

		/* 更新投资表状态 */
		int resultb = changeInvestStatus(investTransfer.invest_id,
				Constants.INVEST_NORMAL);

		if (resultb < 0) {
			JPA.setRollbackOnly();
			return -1;
		}

		t_invests invest = new t_invests();
		t_users user = new t_users();

		try {
			invest = GenericModel.findById(investTransfer.invest_id);
			user = GenericModel.findById(invest.user_id);
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常！";
			error.code = -1;

			return error.code;
		}

		// 发送消息(债权转让失败)
		TemplateEmail email = new TemplateEmail();// 发送邮件
		email.id = Templets.E_DEBT_TRANSFER_FAILURE;

		if (email.status) {
			String content1 = email.content;
			content1 = content1.replace("userName", user.name);
			content1 = content1.replace("date", investTransfer.time + "");
			content1 = content1.replace("describe", investTransfer.title);
			content1 = content1.replace("money", investTransfer.debt_amount
					+ "");
			TemplateEmail.addEmailTask(user.email, email.title, content1);
		}

		TemplateStation station = new TemplateStation();// 发送站内信
		station.id = Templets.M_DEBT_TRANSFER_FAILURE;

		if (station.status) {
			String scontent = station.content;
			scontent = scontent.replace("userName", user.name);
			scontent = scontent.replace("date", investTransfer.time + "");
			scontent = scontent.replace("describe", investTransfer.title);
			scontent = scontent.replace("money", investTransfer.debt_amount
					+ "");
			TemplateStation.addMessageTask(user.id, station.title, scontent);
		}

		TemplateSms sms = new TemplateSms();// 发送短信
		sms.id = Templets.S_DEBT_TRANSFER_FAILURE;

		if (sms.status) {
			String scontent = sms.content;
			scontent = scontent.replace("userName", user.name);
			scontent = scontent.replace("date", investTransfer.time + "");
			scontent = scontent.replace("describe", investTransfer.title);
			scontent = scontent.replace("money", investTransfer.debt_amount
					+ "");
			TemplateSms.addSmsTask(user.mobile, scontent);
		}

		if (investTransfer.type == Constants.DIRECTIONAL_MODE) {// 定向模式不需要解冻冻结金额
			return 1;
		}

		/* 如果有竞拍记录，修改状态值和资金退回 */
		String sql = "select new Map(user_id as userId,offer_price as offerPrice) from t_invest_transfer_details where transfer_id=?";

		listMap = GenericModel.find(sql, investransferId).fetch();

		DataSafety data = new DataSafety();
		if (null != listMap && listMap.size() > 0) {

			for (Map<String, Object> map : listMap) {

				long userId = Long.parseLong(map.get("userId").toString());
				double offerPrice = Double.parseDouble(map.get("offerPrice")
						.toString());

				// 解冻竞拍金额
				int result = DealDetail.relieveFreezeFund(userId, offerPrice);

				if (result < 0) {
					JPA.setRollbackOnly();
					return -1;
				}

				// 添加交易记录(解冻竞拍金额)
				Map<String, Double> funds = DealDetail.queryUserFund(userId,
						error);

				dealDetail = new DealDetail(userId,
						DealType.THAW_FREEZE_AUCTIONAMOUNT, offerPrice,
						investransferId, funds.get("user_amount"),
						funds.get("freeze"), funds.get("receive_amount"),
						"债权流拍，解冻对用竞拍金额");
				dealDetail.addDealDetail(error);

				if (error.code < 0) {
					JPA.setRollbackOnly();

					return -1;
				}

				String hql = "select new Map(name as name,mobile as moile,email as email) from t_users where id = ? ";
				Map<String, String> msgmap = new HashMap<String, String>();

				try {
					msgmap = GenericModel.find(hql, userId).first();
				} catch (Exception e) {
					e.printStackTrace();
					error.code = -1;
					error.msg = "对不起！系统异常，对您造成的不便敬请谅解";
					return error.code;
				}

				// //发送消息(竞拍失败)
				email = new TemplateEmail();// 发送邮件
				email.id = Templets.E_AUCTION_FAILURE;

				if (email.status) {
					String content = email.content;
					content = content.replace("userName", msgmap.get("name")
							+ "");
					content = content.replace("date", investTransfer.time + "");
					content = content.replace("describe", investTransfer.title);
					content = content.replace("money", offerPrice + "");
					TemplateEmail.addEmailTask(msgmap.get("email"), email.title,
							content);
				}

				station = new TemplateStation();// 发送站内信
				station.id = Templets.M_AUCTION_FAILURE;

				if (station.status) {
					String scontent = station.content;
					scontent = scontent.replace("userName", msgmap.get("name")
							+ "");
					scontent = scontent.replace("date", investTransfer.time
							+ "");
					scontent = scontent.replace("describe",
							investTransfer.title);
					scontent = scontent.replace("money", offerPrice + "");
					TemplateStation.addMessageTask(userId, station.title, scontent);
				}

				sms = new TemplateSms();// 发送短信
				sms.id = Templets.S_AUCTION_FAILURE;

				if (sms.status) {
					String smscontent = sms.content;
					smscontent = smscontent.replace("userName",
							msgmap.get("name") + "");
					smscontent = smscontent.replace("date", investTransfer.time
							+ "");
					smscontent = smscontent.replace("describe",
							investTransfer.title);
					smscontent = smscontent.replace("money", offerPrice + "");
					TemplateSms.addSmsTask(msgmap.get("mobile"), smscontent);
				}

				// 数据防篡改字段
				data.setId(userId);
				data.updateSign(error);

				if (error.code < 0) {
					JPA.setRollbackOnly();

					return -1;
				}

				int resultc = changeAuctionStatus(investransferId, userId,
						Constants.AUCTION_DETAIL_FAILURE);
				if (resultc < 0) {
					JPA.setRollbackOnly();
					return -1;
				}
			}

		}
		return 1;
	}

	/**
	 * 更新债权标状态
	 * 
	 * @param debtId
	 * @param status
	 * @param error
	 */
	public static int changeDebtStatus(long debtId, int status) {

		EntityManager em = JPA.em();
		try {
			int rows = em
					.createQuery(
							" update t_invest_transfers set status = ? ,failure_time = ? where id = ? and status in (1,2,-1,4)")
					.setParameter(1, status).setParameter(2, new Date())
					.setParameter(3, debtId).executeUpdate();

			if (rows == 0) {
				JPA.setRollbackOnly();
				return -1;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 1;
	}

	/**
	 * 更新投资记录状态
	 * 
	 * @param investId
	 * @param status
	 * @param error
	 */
	public static int changeInvestStatus(long investId, int status) {

		EntityManager em = JPA.em();
		try {
			int rows = em
					.createQuery(
							" update t_invests set transfer_status = ? where id = ? and transfer_status = 1")
					.setParameter(1, status).setParameter(2, investId)
					.executeUpdate();

			if (rows == 0) {
				JPA.setRollbackOnly();
				return -1;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 1;
	}

	/**
	 * 债权流拍时更新竞拍记录状态
	 * 
	 * @param investId
	 * @param status
	 * @param error
	 */
	public static int changeAuctionStatus(long transferId, long userId,
			int status) {

		EntityManager em = JPA.em();
		try {
			int rows = em
					.createQuery(
							" update t_invest_transfer_details set status = ? where transfer_id = ?  and user_id = ? ")
					.setParameter(1, status).setParameter(2, transferId)
					.setParameter(3, userId).executeUpdate();

			if (rows == 0) {
				JPA.setRollbackOnly();
				return -1;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 1;
	}

	/**
	 * 债权成交时，更新竞拍记录状态
	 * 
	 * @param detailId
	 * @param status
	 * @return
	 */
	public static int changeAuctionDetailStatus(double maxOfferPrice,
			long investTransferId) {

		EntityManager em = JPA.em();
		try {

			int a = em
					.createQuery(
							"update t_invest_transfer_details  set status=1"
									+ " where offer_price = ? and transfer_id=? and status = 0 )")
					.setParameter(1, maxOfferPrice)
					.setParameter(2, investTransferId).executeUpdate();

			int b = em
					.createQuery(
							"update t_invest_transfer_details  set status=-1"
									+ " where offer_price < ? and transfer_id=? and status = 0)")
					.setParameter(1, maxOfferPrice)
					.setParameter(2, investTransferId).executeUpdate();

			if (a == 0 || b == 0) {
				JPA.setRollbackOnly();
				return -1;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 1;
	}

	/**
	 * 债权转让借款标收款情况
	 * 
	 * @param userId
	 * @param bidId
	 * @return
	 */
	public static Map<String, String> debtBidReceiveSituation(long investId) {

		Map<String, String> map = new HashMap<String, String>();
		Double receivingAmount = 0.0;
		Double hasReceivedAmount = 0.0;
		Double remainReceivedCorpus = 0.0;
		Double remainReceivedInterest = 0.0;

		try {
			receivingAmount = GenericModel
					.find("select sum(receive_corpus + receive_interest + overdue_fine) from t_bill_invests where invest_id = ?  ",
							investId).first();
			hasReceivedAmount = GenericModel
					.find("select sum(receive_corpus + receive_interest + overdue_fine) from t_bill_invests where status in (-3,-4,0) and invest_id = ? ",
							investId).first();
			remainReceivedCorpus = GenericModel
					.find("select sum(receive_corpus - real_receive_corpus ) from t_bill_invests where status in (-1,-2,-5,-6) and invest_id = ? ",
							investId).first();
			remainReceivedInterest = GenericModel
					.find("select sum(receive_interest - real_receive_interest ) from t_bill_invests where status in (-1,-2,-5,-6) and invest_id = ? ",
							investId).first();

		} catch (Exception e) {
			e.printStackTrace();

			return map;
		}

		if (null == receivingAmount || receivingAmount == 0) {
			receivingAmount = 0.0;
		}
		if (null == hasReceivedAmount || hasReceivedAmount == 0) {
			hasReceivedAmount = 0.0;
		}
		if (null == remainReceivedCorpus || remainReceivedCorpus == 0) {
			remainReceivedCorpus = 0.0;
		}
		if (null == remainReceivedInterest || remainReceivedInterest == 0) {
			remainReceivedInterest = 0.0;
		}

		java.text.DecimalFormat df = new java.text.DecimalFormat("0.00");// 保留2位小数

		map.put("remainReceivedCorpus", df.format(remainReceivedCorpus));// 剩余应收本金
		map.put("remainReceivedInterest", df.format(remainReceivedInterest));// 剩余应收利息
		map.put("receivingAmount", df.format(receivingAmount));// 本息合计应收
		map.put("hasReceivedAmount", df.format(hasReceivedAmount));// 已收金额
		map.put("remainReceivedAmount",
				df.format(receivingAmount - hasReceivedAmount));// 剩余应收金额

		return map;
	}

	/**
	 * 所有审核中的债权转让标
	 * 
	 * @param userId
	 * @param type
	 *            1:全部 2：编号 3：债权人
	 * @param params
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_debt_auditing_transfers> queryAllAuditingTransfers(
			String typeStr, String startDateStr, String endDateStr,
			String keyWords, String orderTypeStr, String currPageStr,
			String pageSizeStr) {

		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;
		int type = 0;
		int orderType = 0;
		String key = new String();

		String[] typeCondition = { " and (no like ? or name like ?)",
				" and no like ? ", " and name like ?" };
		String[] orderCondition = { " ", " order by time ",
				" order by time desc ", " order by bid_amount ",
				" order by bid_amount desc ", " order by credit_order desc",
				" order by credit_order  ",
				" order by overdue_payback_period ",
				" order by overdue_payback_period desc " };

		StringBuffer conditions = new StringBuffer(" 1=1  ");
		List<Object> values = new ArrayList<Object>();

		List<v_debt_auditing_transfers> transfersList = new ArrayList<v_debt_auditing_transfers>();
		PageBean<v_debt_auditing_transfers> page = new PageBean<v_debt_auditing_transfers>();

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		if (orderType < 0 || orderType > 8) {
			orderType = 0;
		}

		if (!StringUtils.isBlank(startDateStr)
				&& !StringUtils.isBlank(endDateStr)) {
			Date startDate = DateUtil.strToYYMMDDDate(startDateStr);
			Date endDate = DateUtil.strToYYMMDDDate(endDateStr);

			conditions.append(" and time > ? and time < ?");
			values.add(startDate);
			values.add(endDate);
		}

		if (StringUtils.isNotBlank(keyWords)) {
			key = keyWords;
		}

		if (type == 0) {
			conditions.append(typeCondition[0]);
			values.add("%" + key + "%");
			values.add("%" + key + "%");
		} else {
			conditions.append(typeCondition[type]);
			values.add("%" + key + "%");
		}
		conditions.append(orderCondition[orderType]);

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keyWords", keyWords);
		conditionMap.put("orderType", orderType);
		conditionMap.put("type", type);
		conditionMap.put("startDateStr", startDateStr);
		conditionMap.put("endDateStr", endDateStr);

		page.pageSize = pageSize;
		page.currPage = currPage;

		try {

			transfersList = GenericModel.find(
					conditions.toString(), values.toArray()).fetch(currPage,
					page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();

			return page;
		}
		if (transfersList.size() > 0) {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
		}

		page.page = transfersList;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 所有转让中的债权转让标
	 * 
	 * @param userId
	 * @param type
	 *            1:全部 2：编号 3：债权人
	 * @param params
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_debt_transfering> queryAllTransferingDebts(
			String typeStr, String startDateStr, String endDateStr,
			String keyWords, String orderTypeStr, String currPageStr,
			String pageSizeStr) {

		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;
		int type = 0;
		int orderType = 0;
		String key = new String();

		String[] typeCondition = { " and (no like ? or name like ?)",
				" and no like ? ", " and name like ?" };
		String[] orderCondition = { " ", " order by time ",
				" order by time desc ", " order by bid_amount ",
				" order by bid_amount desc ", " order by credit_order desc ",
				" order by credit_order  ",
				" order by overdue_payback_period ",
				" order by overdue_payback_period desc ",
				" order by end_time ", " order by end_time desc " };

		StringBuffer conditions = new StringBuffer(" 1=1  ");
		List<Object> values = new ArrayList<Object>();

		List<v_debt_transfering> transfersList = new ArrayList<v_debt_transfering>();
		PageBean<v_debt_transfering> page = new PageBean<v_debt_transfering>();

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		if (orderType < 0 || orderType > 10) {
			orderType = 0;
		}

		if (!StringUtils.isBlank(startDateStr)
				&& !StringUtils.isBlank(endDateStr)) {
			Date startDate = DateUtil.strToYYMMDDDate(startDateStr);
			Date endDate = DateUtil.strToYYMMDDDate(endDateStr);

			conditions.append(" and time > ? and time < ?");
			values.add(startDate);
			values.add(endDate);
		}

		if (StringUtils.isNotBlank(keyWords)) {
			key = keyWords;
		}

		if (type == 0) {
			conditions.append(typeCondition[0]);
			values.add("%" + key + "%");
			values.add("%" + key + "%");
		} else {
			conditions.append(typeCondition[type]);
			values.add("%" + key + "%");
		}
		conditions.append(orderCondition[orderType]);

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keyWords", keyWords);
		conditionMap.put("orderType", orderType);
		conditionMap.put("type", type);
		conditionMap.put("startDateStr", startDateStr);
		conditionMap.put("endDateStr", endDateStr);

		page.pageSize = pageSize;
		page.currPage = currPage;

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			transfersList = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();

			return page;
		}

		page.page = transfersList;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 所有转让成功的债权转让标
	 * 
	 * @param userId
	 * @param type
	 *            1:全部 2：编号 3：债权人
	 * @param params
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_debt_transfers_success> queryAllSuccessedDebts(
			String typeStr, String startDateStr, String endDateStr,
			String keyWords, String orderTypeStr, String currPageStr,
			String pageSizeStr) {

		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;
		int type = 0;
		int orderType = 0;
		String key = new String();

		String[] typeCondition = { " and (no like ? or name like ?)",
				" and no like ? ", " and name like ?" };
		String[] orderCondition = { " ", " order by time ",
				" order by time desc ", " order by bid_amount ",
				" order by bid_amount desc ", " order by credit_order desc ",
				" order by credit_order  ",
				" order by overdue_payback_period ",
				" order by overdue_payback_period desc ",
				" order by transaction_time ",
				" order by transaction_time desc " };

		StringBuffer conditions = new StringBuffer(" 1=1  ");
		List<Object> values = new ArrayList<Object>();

		List<v_debt_transfers_success> transfersList = new ArrayList<v_debt_transfers_success>();
		PageBean<v_debt_transfers_success> page = new PageBean<v_debt_transfers_success>();

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		if (orderType < 0 || orderType > 10) {
			orderType = 0;
		}

		if (!StringUtils.isBlank(startDateStr)
				&& !StringUtils.isBlank(endDateStr)) {
			Date startDate = DateUtil.strToYYMMDDDate(startDateStr);
			Date endDate = DateUtil.strToYYMMDDDate(endDateStr);

			conditions.append(" and time > ? and time < ?");
			values.add(startDate);
			values.add(endDate);
		}

		if (StringUtils.isNotBlank(keyWords)) {
			key = keyWords;
		}

		if (type == 0) {
			conditions.append(typeCondition[0]);
			values.add("%" + key + "%");
			values.add("%" + key + "%");
		} else {
			conditions.append(typeCondition[type]);
			values.add("%" + key + "%");
		}
		conditions.append(orderCondition[orderType]);

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keyWords", keyWords);
		conditionMap.put("orderType", orderType);
		conditionMap.put("type", type);
		conditionMap.put("startDateStr", startDateStr);
		conditionMap.put("endDateStr", endDateStr);

		page.pageSize = pageSize;
		page.currPage = currPage;

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			transfersList = GenericModel.find(
					conditions.toString(), values.toArray()).fetch(currPage,
					page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();

			return page;
		}

		page.page = transfersList;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 所有未审核通过的债权转让标
	 * 
	 * @param userId
	 * @param type
	 *            1:全部 2：编号 3：债权人
	 * @param params
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_debt_no_pass_transfers> queryAllNopassDebts(
			String typeStr, String startDateStr, String endDateStr,
			String keyWords, String orderTypeStr, String currPageStr,
			String pageSizeStr) {

		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;
		int type = 0;
		int orderType = 0;
		String key = new String();

		String[] typeCondition = { " and (no like ? or name like ?)",
				" and no like ? ", " and name like ?" };
		String[] orderCondition = { " ", " order by time ",
				" order by time desc ", " order by bid_amount ",
				" order by bid_amount desc ", " order by credit_order desc ",
				" order by credit_order  ",
				" order by overdue_payback_period ",
				" order by overdue_payback_period desc " };

		StringBuffer conditions = new StringBuffer(" 1=1 ");
		List<Object> values = new ArrayList<Object>();

		List<v_debt_no_pass_transfers> transfersList = new ArrayList<v_debt_no_pass_transfers>();
		PageBean<v_debt_no_pass_transfers> page = new PageBean<v_debt_no_pass_transfers>();

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		if (orderType < 0 || orderType > 8) {
			orderType = 0;
		}

		if (!StringUtils.isBlank(startDateStr)
				&& !StringUtils.isBlank(endDateStr)) {
			Date startDate = DateUtil.strToYYMMDDDate(startDateStr);
			Date endDate = DateUtil.strToYYMMDDDate(endDateStr);

			conditions.append(" and time > ? and time < ?");
			values.add(startDate);
			values.add(endDate);
		}

		if (StringUtils.isNotBlank(keyWords)) {
			key = keyWords;
		}

		if (type == 0) {
			conditions.append(typeCondition[0]);
			values.add("%" + key + "%");
			values.add("%" + key + "%");
		} else {
			conditions.append(typeCondition[type]);
			values.add("%" + key + "%");
		}
		conditions.append(orderCondition[orderType]);

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keyWords", keyWords);
		conditionMap.put("orderType", orderType);
		conditionMap.put("type", type);
		conditionMap.put("startDateStr", startDateStr);
		conditionMap.put("endDateStr", endDateStr);

		page.pageSize = pageSize;
		page.currPage = currPage;

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			transfersList = GenericModel.find(
					conditions.toString(), values.toArray()).fetch(currPage,
					page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();

			return page;
		}

		page.page = transfersList;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 所有失败的债权转让标
	 * 
	 * @param userId
	 * @param type
	 *            1:全部 2：编号 3：债权人
	 * @param params
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_debt_transfer_failure> queryAllFailureDebts(
			String typeStr, String startDateStr, String endDateStr,
			String keyWords, String orderTypeStr, String currPageStr,
			String pageSizeStr) {

		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;
		int type = 0;
		int orderType = 0;
		String key = new String();

		String[] typeCondition = { " and (no like ? or name like ?)",
				" and no like ? ", " and name like ?" };
		String[] orderCondition = { " ", " order by time ",
				" order by time desc ", " order by bid_amount ",
				" order by bid_amount desc ", " order by credit_order desc ",
				" order by credit_order  ",
				" order by overdue_payback_period ",
				" order by overdue_payback_period desc " };

		StringBuffer conditions = new StringBuffer(" 1=1   ");
		List<Object> values = new ArrayList<Object>();

		List<v_debt_transfer_failure> transfersList = new ArrayList<v_debt_transfer_failure>();
		PageBean<v_debt_transfer_failure> page = new PageBean<v_debt_transfer_failure>();

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		if (orderType < 0 || orderType > 8) {
			orderType = 0;
		}

		if (!StringUtils.isBlank(startDateStr)
				&& !StringUtils.isBlank(endDateStr)) {
			Date startDate = DateUtil.strToYYMMDDDate(startDateStr);
			Date endDate = DateUtil.strToYYMMDDDate(endDateStr);

			conditions.append(" and time > ? and time < ?");
			values.add(startDate);
			values.add(endDate);
		}

		if (StringUtils.isNotBlank(keyWords)) {
			key = keyWords;
		}

		if (type == 0) {
			conditions.append(typeCondition[0]);
			values.add("%" + key + "%");
			values.add("%" + key + "%");
		} else {
			conditions.append(typeCondition[type]);
			values.add("%" + key + "%");
		}
		conditions.append(orderCondition[orderType]);

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keyWords", keyWords);
		conditionMap.put("orderType", orderType);
		conditionMap.put("type", type);
		conditionMap.put("startDateStr", startDateStr);
		conditionMap.put("endDateStr", endDateStr);

		page.pageSize = pageSize;
		page.currPage = currPage;

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			transfersList = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();

			return page;
		}

		page.page = transfersList;
		page.conditions = conditionMap;

		return page;

	}

	/**
	 * 受让债权管理竞价成功债权详情页面
	 * 
	 * @param debtId
	 */
	public static v_debt_user_receive_transfers_management details(long debtId) {

		v_debt_user_receive_transfers_management debt = null;
		try {
			debt = GenericModel.findById(debtId);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return debt;
	}

	/**
	 * 转让债权管理竞价成功债权详情页面
	 * 
	 * @param debtId
	 */
	public static v_debt_user_transfer_management transferDetails(long debtId,
			ErrorInfo error) {

		v_debt_user_transfer_management debt = null;

		try {
			debt = GenericModel.findById(debtId);
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return null;
		}

		error.code = 1;
		return debt;

	}

	/**
	 * 查询用户竞拍某个债权的最高价
	 * 
	 * @param transferId
	 * @param userId
	 * @param error
	 * @return
	 */
	public static Double getMyAuctionPrice(long transferId, long userId,
			ErrorInfo error) {

		Double auctionPrice = 0.0;
		try {
			auctionPrice = GenericModel
					.find("select max(offer_price) from t_invest_transfer_details where transfer_id = ? and user_id = ?",
							transferId, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (null == auctionPrice || auctionPrice == 0) {
			auctionPrice = 0.0;
		}

		error.code = 1;
		return auctionPrice;
	}

	/**
	 * 统计所有的债权转让标
	 * 
	 * @param error
	 * @return
	 */
	public static Long getAllDebtCount(ErrorInfo error) {

		Long count = 0l;

		try {
			count = GenericModel.count("");
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (null == count || count == 0) {
			count = 0l;
		}

		error.code = 1;
		return count;
	}

	/**
	 * 根据债权ID查询借款用户ID
	 * 
	 * @param debtId
	 * @return
	 */
	public static long getBidUserId(long debtId, ErrorInfo error) {

		Long userId = 0l;
		Long investId = 0l;
		Long bidId = 0l;

		try {
			investId = GenericModel.find(
					"select invest_id from t_invest_transfers where id = ?",
					debtId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (null == investId) {
			error.code = -2;
		} else {
			try {
				bidId = GenericModel.find(
						"select bid_id from t_invests where id = ?", investId)
						.first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -3;
			}

			if (null == bidId) {
				error.code = -4;
			} else {
				try {
					userId = GenericModel.find(
							"select user_id from t_bids where id = ?", bidId)
							.first();
				} catch (Exception e) {
					e.printStackTrace();
					error.code = -3;
				}
				if (null == userId) {
					error.code = -4;
				}
			}
		}

		error.code = 1;
		return userId;
	}

	/**
	 * 根据债权ID查询投资用户ID
	 * 
	 * @param debtId
	 * @return
	 */
	public static long getInvestUserId(long debtId, ErrorInfo error) {

		Long userId = 0l;
		Long investId = 0l;

		try {
			investId = GenericModel.find(
					"select invest_id from t_invest_transfers where id = ?",
					debtId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (null == investId) {
			error.code = -2;
		} else {
			try {
				userId = GenericModel.find(
						"select user_id from t_invests where id = ?", investId)
						.first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -3;
			}

		}

		error.code = 1;
		return userId;
	}

	/**
	 * 根据投资ID查询借款用户ID
	 * 
	 * @param debtId
	 * @return
	 */
	public static long getUserIdByInvestId(long investId, ErrorInfo error) {

		Long userId = 0l;
		Long bidId = 0l;

		try {
			bidId = GenericModel.find("select bid_id from t_invests where id = ?",
					investId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (null == bidId) {
			error.code = -2;
		} else {
			try {
				userId = GenericModel.find("select user_id from t_bids where id = ?",
						bidId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -3;
			}

		}

		error.code = 1;
		return userId;
	}

	/**
	 * ajax分页查询债权竞拍记录
	 * 
	 * @param debtId
	 */
	public static PageBean<v_debt_auction_records> queryDebtAllAuctionRecords(
			int pageNum, int pageSize, long debtId, ErrorInfo error) {

		PageBean<v_debt_auction_records> page = new PageBean<v_debt_auction_records>();
		List<v_debt_auction_records> list = new ArrayList<v_debt_auction_records>();

		page.pageSize = pageSize;
		page.currPage = pageNum;

		try {
			page.totalCount = (int) GenericModel.count(
					" transfer_id=?", debtId);
			list = GenericModel.find(" transfer_id=?", debtId).fetch(
					page.currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info(e.getMessage());

			error.code = -1;
		}

		error.code = 1;
		page.page = list;

		return page;

	}

	/**
	 * 后台债权转让管理详情页面前一条记录(审核中)
	 * 
	 * @param debtId
	 * @param status
	 * @param error
	 * @return
	 */
	public static long auditingAhead(long debtId, int status, ErrorInfo error) {

		long temp = -1;
		long minId = -1;
		// 审核中

		try {
			minId = GenericModel.find(
					"select min(id) from v_debt_auditing_transfers").first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return error.code;
		}

		if (debtId == minId || minId == -1) {
			temp = debtId;
		} else {
			String sql = "select id from v_debt_auditing_transfers where id = (select max(id) from v_debt_auditing_transfers where id < ?) ";

			try {
				temp = GenericModel.find(sql, debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return error.code;
			}
		}

		error.code = 1;
		return temp;
	}

	/**
	 * 后台债权转让管理详情页面前一条记录(竞拍中)
	 * 
	 * @param debtId
	 * @param status
	 * @param error
	 * @return
	 */
	public static long auctioningAhead(long debtId, int status, ErrorInfo error) {

		long temp = -1;
		long minId = -1;

		// 竞拍转让中
		try {
			minId = GenericModel.find(
					"select min(id) from v_debt_transfering").first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return error.code;
		}

		if (debtId == minId || minId == -1) {
			temp = debtId;
		} else {
			String sql = "select id from v_debt_transfering where id = (select max(id) from v_debt_transfering where id < ?) ";

			try {
				temp = GenericModel.find(sql, debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return error.code;
			}
		}

		error.code = 1;
		return temp;
	}

	/**
	 * 后台债权转让管理详情页面前一条记录(竞拍中)
	 * 
	 * @param debtId
	 * @param status
	 * @param error
	 * @return
	 */
	public static long successAhead(long debtId, int status, ErrorInfo error) {

		long temp = -1;
		long minId = -1;

		try {
			minId = GenericModel.find(
					"select min(id) from v_debt_transfers_success").first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return error.code;
		}

		if (debtId == minId || minId == -1) {
			temp = debtId;
		} else {
			String sql = "select id from v_debt_transfers_success where id = (select max(id) from v_debt_transfers_success where id < ?) ";

			try {
				temp = GenericModel.find(sql, debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return error.code;
			}
		}
		error.code = 1;
		return temp;
	}

	/**
	 * 后台债权转让管理详情页面前一条记录(未通过)
	 * 
	 * @param debtId
	 * @param status
	 * @param error
	 * @return
	 */
	public static long nopassAhead(long debtId, int status, ErrorInfo error) {

		long temp = -1;
		long minId = -1;

		try {
			minId = GenericModel.find(
					"select min(id) from v_debt_no_pass_transfers").first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return error.code;
		}

		if (debtId == minId || minId == -1) {
			temp = debtId;
		} else {
			String sql = "select id from v_debt_no_pass_transfers where id = (select max(id) from v_debt_no_pass_transfers where id < ?) ";

			try {
				temp = GenericModel.find(sql, debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return error.code;
			}
		}
		error.code = 1;
		return temp;
	}

	/**
	 * 后台债权转让管理详情页面前一条记录(失败的)
	 * 
	 * @param debtId
	 * @param status
	 * @param error
	 * @return
	 */
	public static long failureAhead(long debtId, int status, ErrorInfo error) {

		long temp = -1;
		long minId = -1;

		try {
			minId = GenericModel.find(
					"select min(id) from v_debt_transfer_failure").first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return error.code;
		}

		if (debtId == minId || minId == -1) {
			temp = debtId;
		} else {
			String sql = "select id from v_debt_transfer_failure where id = (select max(id) from v_debt_transfer_failure where id < ?) ";

			try {
				temp = GenericModel.find(sql, debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return error.code;
			}
		}
		error.code = 1;
		return temp;

	}

	/**
	 * 后台债权转让管理详情页面后一条记录(审核中的)
	 * 
	 * @param debtId
	 * @param status
	 * @param error
	 * @return
	 */
	public static long auditingBack(long debtId, int status, ErrorInfo error) {

		long temp = -1;
		long maxId = -1;

		// 审核中

		try {
			maxId = GenericModel.find(
					"select max(id) from v_debt_auditing_transfers").first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return error.code;
		}

		if (debtId == maxId || maxId == -1) {
			temp = debtId;
		} else {
			String sql = "select id from v_debt_auditing_transfers where id = (select min(id) from v_debt_auditing_transfers where id > ?) ";
			try {
				temp = GenericModel.find(sql, debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return error.code;
			}
		}

		error.code = 1;
		return temp;
	}

	/**
	 * 后台债权转让管理详情页面后一条记录(竞拍中的)
	 * 
	 * @param debtId
	 * @param status
	 * @param error
	 * @return
	 */
	public static long auctioningBack(long debtId, int status, ErrorInfo error) {

		long temp = -1;
		long maxId = -1;

		// 竞拍转让中

		try {
			maxId = GenericModel.find(
					"select max(id) from v_debt_transfering").first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return error.code;
		}

		if (debtId == maxId || maxId == -1) {
			temp = debtId;
		} else {
			String sql = "select id from v_debt_transfering where id = (select min(id) from v_debt_transfering where id > ?) ";
			try {
				temp = GenericModel.find(sql, debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return error.code;
			}

		}
		error.code = 1;
		return temp;
	}

	/**
	 * 后台债权转让管理详情页面后一条记录(已成功的)
	 * 
	 * @param debtId
	 * @param status
	 * @param error
	 * @return
	 */
	public static long successBack(long debtId, int status, ErrorInfo error) {

		long temp = -1;
		long maxId = -1;

		// 已成功

		try {
			maxId = GenericModel.find(
					"select max(id) from v_debt_transfers_success").first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return error.code;
		}

		if (debtId == maxId) {
			temp = debtId;
		} else {
			String sql = "select id from v_debt_transfers_success where id = (select min(id) from v_debt_transfers_success where id > ?) ";

			try {
				temp = GenericModel.find(sql, debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return error.code;
			}
		}

		error.code = 1;
		return temp;
	}

	/**
	 * 后台债权转让管理详情页面后一条记录(未通过的)
	 * 
	 * @param debtId
	 * @param status
	 * @param error
	 * @return
	 */
	public static long nopassBack(long debtId, int status, ErrorInfo error) {

		long temp = -1;
		long maxId = -1;

		// 审核不通过

		try {
			maxId = GenericModel.find(
					"select max(id) from v_debt_no_pass_transfers").first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return error.code;
		}

		if (debtId == maxId || maxId == -1) {
			temp = debtId;
		} else {
			String sql = "select id from v_debt_no_pass_transfers where id = (select min(id) from v_debt_no_pass_transfers where id > ?) ";
			try {
				temp = GenericModel.find(sql, debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return error.code;
			}

		}

		error.code = 1;
		return temp;
	}

	/**
	 * 后台债权转让管理详情页面后一条记录(失败的)
	 * 
	 * @param debtId
	 * @param status
	 * @param error
	 * @return
	 */
	public static long failureBack(long debtId, int status, ErrorInfo error) {

		long temp = -1;
		long maxId = -1;

		// 失败的

		try {
			maxId = GenericModel.find(
					"select max(id) from v_debt_transfer_failure").first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return error.code;
		}

		if (debtId == maxId || maxId == -1) {
			temp = debtId;
		} else {
			String sql = "select id from v_debt_transfer_failure where id = (select min(id) from v_debt_transfer_failure where id > ?) ";
			try {
				temp = GenericModel.find(sql, debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return error.code;
			}
		}

		error.code = 1;
		return temp;
	}

	/**
	 * 
	 * @param debtId
	 * @param status
	 * @param error
	 * @return
	 */
	public static Map countMap(long debtId, int status, ErrorInfo error) {

		Long countFront = 0l;
		Long countAfter = 0l;

		Map<String, Long> map = new HashMap<String, Long>();

		if (status == Constants.DEBT_AUDITING) {// 审核中
			try {
				countFront = GenericModel
						.find("select count(id) from v_debt_auditing_transfers where id > ?",
								debtId).first();
				countAfter = GenericModel
						.find("select count(id) from v_debt_auditing_transfers where id < ?",
								debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
			}

		}

		if (status == Constants.DEBT_NOPASS) {// 审核未通过
			try {
				countFront = GenericModel
						.find("select count(id) from v_debt_no_pass_transfers where id > ?",
								debtId).first();
				countAfter = GenericModel
						.find("select count(id) from v_debt_no_pass_transfers where id < ?",
								debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
			}
		}

		if (status == Constants.DEBT_AUCTIONING
				|| status == Constants.DEBT_ACCEPT) {// 竞拍中

			try {
				countFront = GenericModel
						.find("select count(id) from v_debt_transfering where id > ?",
								debtId).first();
				countAfter = GenericModel
						.find("select count(id) from v_debt_transfering where id < ?",
								debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
			}
		}

		if (status == Constants.DEBT_SUCCESS) {// 已成功的
			try {
				countFront = GenericModel
						.find("select count(id) from v_debt_transfers_success where id > ?",
								debtId).first();
				countAfter = GenericModel
						.find("select count(id) from v_debt_transfers_success where id < ?",
								debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
			}
		}

		if (status == Constants.DEBT_FLOW || status == Constants.DEBT_REFUSE
				|| status == Constants.DEBT_REPAY) {// 失败的
			try {
				countFront = GenericModel
						.find("select count(id) from v_debt_transfer_failure where id > ?",
								debtId).first();
				countAfter = GenericModel
						.find("select count(id) from v_debt_transfer_failure where id < ?",
								debtId).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
			}
		}

		if (null == countFront) {
			countFront = 0l;
		}

		if (null == countAfter) {
			countAfter = 0l;
		}

		map.put("countFront", countFront);
		map.put("countAfter", countAfter);

		error.code = 1;
		return map;
	}

	/**
	 * 查询某个债权的竞拍记录
	 * 
	 * @param pageNum
	 * @param debtId
	 * @param error
	 * @return
	 */
	public static PageBean<v_debt_auction_records> queryAllAuctionRecords(
			int pageNum, long debtId, ErrorInfo error) {

		PageBean<v_debt_auction_records> page = new PageBean<v_debt_auction_records>();

		page.currPage = pageNum;
		page.pageSize = 7;

		List<v_debt_auction_records> list = new ArrayList<v_debt_auction_records>();

		try {
			page.totalCount = (int) GenericModel.count(
					" transfer_id=?", debtId);
			list = GenericModel.find(" transfer_id=?", debtId).fetch(
					pageNum, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info(e.getMessage());
			error.code = -1;
		}

		page.page = list;
		error.code = 1;
		return page;
	}

	/**
	 * 取消债权关注
	 * 
	 * @param debtId
	 * @param error
	 */
	public static void canaleDebt(long attentionId, ErrorInfo error) {

		t_user_attention_invest_transfers attentionDebt = null;

		try {
			attentionDebt = GenericModel
					.findById(attentionId);
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			error.msg = "查询关注的债权异常";

			return;
		}

		if (null != attentionDebt) {
			attentionDebt.delete();
			error.code = 1;
			error.msg = "取消关注债权成功";

			return;
		}

		error.code = -2;
		error.msg = "查询关注的债权异常";

		return;
	}

	/**
	 * 取消关注借款标
	 * 
	 * @param bidId
	 * @param userId
	 * @param error
	 */
	public static void cancleBid(long bidId, long userId, ErrorInfo error) {

		t_user_attention_bids attentionBid = null;

		try {
			attentionBid = GenericModel.find(
					" user_id = ? and bid_id = ?", userId, bidId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			error.msg = "查询关注借款标异常";

			return;
		}

		if (null != attentionBid) {
			attentionBid.delete();
			error.code = 1;
			error.msg = "取消关注借款标成功";

			return;
		}

		error.code = -2;
		error.msg = "查询关注借款标异常";

		return;
	}

	/**
	 * 债权用户初步成交债权，之后等待竞拍方确认成交
	 * 
	 * @param debtId
	 * @param error
	 */
	public static void firstDealDebt(long debtId, ErrorInfo error) {

		t_invest_transfers debt = null;
		t_invest_transfer_details detail = null;
		t_users user = null;

		try {
			debt = GenericModel.findById(debtId);
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			error.msg = "查询债权对象异常";

			return;
		}

		if (debt.status == Constants.TRANSFER_STATUS) {
			error.msg = "对不起！该债权还没有审核，请耐心等待！";
			error.code = -1;

			return;
		}

		if (debt.status != Constants.DIRECTIONAL_MODE
				&& debt.status != Constants.AUCTION_MODE) {
			error.msg = "对不起！该债权已经不处于待成交状态！";
			error.code = -1;

			return;

		}

		Double maxOfferPrice = queryMaxPrice(debtId, error);

		if (error.code < 0) {
			error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
			error.code = -1;
			JPA.setRollbackOnly();

			return;
		}

		if (maxOfferPrice == 0 || null == maxOfferPrice) {// 说明没有竞拍记录，不能执行成交操作

			error.msg = "对不起！该债权暂时没有用户参与竞拍，暂时不能成交！";
			error.code = -1;
			JPA.setRollbackOnly();

			return;
		}

		try {
			detail = GenericModel.find(
					" transfer_id = ? and offer_price = ? ", debtId,
					maxOfferPrice).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -2;
			error.msg = "查询债权竞拍对象异常";

			return;
		}

		if (null != debt && null != detail) {
			debt.status = Constants.WAIT_CONFIRM;
			detail.status = Constants.AUCTION_DETAIL_WAIT_CONFIRM;

			try {
				user = GenericModel.findById(detail.user_id);
				debt.save();
				detail.save();

				if (null != user) {
					TemplateEmail email = new TemplateEmail();// 发送邮件
					email.id = Templets.E_DEAL_DEBT_FIRST;

					if (email.status) {
						String content = email.content;
						content = content.replace("userName", user.name);
						content = content.replace("title", debt.title);
						content = content.replace("money", detail.offer_price
								+ "");
						TemplateEmail.addEmailTask(user.email, email.title, content);
					}

					TemplateStation station = new TemplateStation();// 发送站内信
					station.id = Templets.M_DEAL_DEBT_FIRST;

					if (station.status) {
						String sContent = station.content;
						sContent = sContent.replace("userName", user.name);
						sContent = sContent.replace("title", debt.title);
						sContent = sContent.replace("money", detail.offer_price
								+ "");
						TemplateStation.addMessageTask(user.id, station.title, sContent);
					}

					TemplateSms sms = new TemplateSms();// 发送短信
					sms.id = Templets.S_DEAL_DEBT_FIRST;

					if (sms.status) {
						String smscontent = sms.content;
						smscontent = smscontent.replace("userName", user.name);
						smscontent = smscontent.replace("title", debt.title);
						smscontent = smscontent.replace("money",
								detail.offer_price + "");
						TemplateSms.addSmsTask(user.mobile, smscontent);
					}
				}

				error.code = 1;
				error.msg = "债权成交成功，等待对方确认成交";
				return;
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -2;
				error.msg = "修改债权对象异常";

				return;
			}
		}

		/**
		 * 发送消息、、、、、、
		 */
		error.code = -3;
		error.msg = "修改债权对象异常";

		return;

	}

}
