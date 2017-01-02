package business;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;

import com.shove.security.Encrypt;

import net.sf.json.JSONObject;
import constants.Constants;
import constants.DealType;
import constants.IPSConstants;
import constants.Templets;
import constants.UserEvent;
import constants.IPSConstants.IPSOperation;
import play.Logger;
import play.cache.Cache;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import play.libs.WS;
import utils.Arith;
import utils.DateUtil;
import utils.ErrorInfo;
import utils.NumberUtil;
import utils.PageBean;
import utils.Security;
import models.t_bids;
import models.t_bills;
import models.t_product_audit_items;
import models.t_invests;
import models.t_products;
import models.t_user_attention_bids;
import models.t_user_audit_items;
import models.t_user_automatic_bid;
import models.t_user_automatic_invest_options;
import models.t_users;
import models.v_bill_board;
import models.v_confirm_autoinvest_bids;
import models.v_debt_auction_records;
import models.v_front_all_bids;
import models.v_front_user_attention_bids;
import models.v_invest_records;
import models.v_receiving_invest_bids;
import models.v_user_success_invest_bids;
import models.v_user_waiting_full_invest_bids;

/**
 * 投资业务实体类
 * 
 * @author lwh
 * @version 6.0
 * @created 2014-3-27 下午03:31:06
 */
public class Invest implements Serializable {
	private long _id;
	public long id;
	public String merBillNo;
	public long userId;
	public String userIdSign; // 加密ID
	public Date time;
	public long bidId;
	public double amount;
	public int transferStatus;
	public String status;
	public long transfersId;
	public boolean isAutomaticInvest;

	public User user;
	public Bid bid;

	/**
	 * 获取加密投资者ID
	 * 
	 * @return
	 */
	public String getUserIdSign() {
		return Security.addSign(this.userId, Constants.USER_ID_SIGN);
	}

	public void setUserId(long userId) {
		this.userId = userId;
		this.user = new User();
		this.user.id = userId;
	}

	public void setBidId(long bidId) {
		this.bidId = bidId;
		this.bid = new Bid();
		this.bid.id = bidId;
	}

	public long getId() {
		return _id;
	}

	public void setId(long id) {

		t_invests invests = null;
		try {
			invests = GenericModel.findById(id);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info(e.getMessage());
		}

		if (null == invests) {
			this._id = -1;

			return;
		}
		this._id = invests.id;
		this.userId = invests.user_id;
		this.time = invests.time;
		this.bidId = invests.bid_id;
		this.amount = invests.amount;
		this.transferStatus = invests.transfer_status;

		if (invests.transfer_status == 0) {
			this.status = "正常";
		}

		if (invests.transfer_status == -1) {
			this.status = "已转让出";
		}

		if (invests.transfer_status == 0) {
			this.status = "转让中";
		}

		this.transfersId = invests.transfers_id;
		this.isAutomaticInvest = invests.is_automatic_invest;
	}

	public Invest() {

	}

	/**
	 * 针对某个标的投标记录
	 * 
	 * @return
	 */
	public static PageBean<v_invest_records> queryBidInvestRecords(
			int currPage, int pageSize, long bidId, ErrorInfo error) {

		PageBean<v_invest_records> pageBean = new PageBean<v_invest_records>();
		List<v_invest_records> list = new ArrayList<v_invest_records>();
		pageBean.currPage = currPage;
		pageBean.pageSize = pageSize;

		try {
			pageBean.totalCount = (int) GenericModel.count("bid_id = ?",
					bidId);
			list = GenericModel.find("bid_id = ? ", bidId).fetch(currPage,
					pageBean.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info(e.getMessage());
			error.code = -1;

			return pageBean;
		}

		pageBean.page = list;
		error.code = 1;
		return pageBean;

	}

	/**
	 * 前台借款标条件分页查询
	 * 
	 * @param currPage
	 * @param pageSize
	 * @param _apr
	 * @param _amount
	 * @param _loanSchedule
	 * @param _startDate
	 * @param _endDate
	 * @param _loanType
	 * @param _creditLevel
	 * @return
	 */
	public static PageBean<v_front_all_bids> queryAllBids(int currPage,
			int pageSize, String _apr, String _amount, String _loanSchedule,
			String _startDate, String _endDate, String _loanType,
			String minLevelStr, String maxLevelStr, String _orderType,
			String _keywords, ErrorInfo error) {

		int apr = 0;
		int amount = 0;
		int loan_schedule = 0;
		int orderType = 0;
		int product_id = 0;
		int minLevel = 0;
		int maxLevel = 0;

		List<v_front_all_bids> bidList = new ArrayList<v_front_all_bids>();
		PageBean<v_front_all_bids> page = new PageBean<v_front_all_bids>();

		page.pageSize = pageSize;
		page.currPage = currPage;

		if (StringUtils.isBlank(_apr) && StringUtils.isBlank(_amount)
				&& StringUtils.isBlank(_loanSchedule)
				&& StringUtils.isBlank(_startDate)
				&& StringUtils.isBlank(_endDate)
				&& StringUtils.isBlank(_loanType)
				&& StringUtils.isBlank(minLevelStr)
				&& StringUtils.isBlank(maxLevelStr)
				&& StringUtils.isBlank(_orderType)
				&& StringUtils.isBlank(_keywords)) {

			try {
				page.totalCount = (int) GenericModel.count();
				bidList = GenericModel.find("").fetch(currPage,
						page.pageSize);

			} catch (Exception e) {
				e.printStackTrace();
				error.msg = "系统异常，给您带来的不便敬请谅解！";
				error.code = -1;
			}
			page.page = bidList;
			error.code = 1;
			error.msg = "查询成功";
			return page;
		}

		StringBuffer conditions = new StringBuffer(" 1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (NumberUtil.isNumericInt(_apr)) {
			apr = Integer.parseInt(_apr);
		}

		if (apr < 0 || apr > 4) {
			conditions.append(Constants.BID_APR_CONDITION[0]);// 全部范围
		} else {
			conditions.append(Constants.BID_APR_CONDITION[apr]);
		}

		if (NumberUtil.isNumericInt(_amount)) {
			amount = Integer.parseInt(_amount);
		}

		if (!StringUtils.isBlank(_keywords)) {
			conditions.append(" and title like ? or no like ? ");
			values.add("%" + _keywords + "%");
			values.add("%" + _keywords + "%");
		}

		if (amount < 0 || amount > 5) {
			conditions.append(Constants.BID_AMOUNT_CONDITION[0]);// 全部范围
		} else {
			conditions.append(Constants.BID_AMOUNT_CONDITION[amount]);
		}

		if (NumberUtil.isNumericInt(_loanSchedule)) {
			loan_schedule = Integer.parseInt(_loanSchedule);
		}

		if (loan_schedule < 0 || loan_schedule > 4) {
			conditions.append(Constants.BID_LOAN_SCHEDULE_CONDITION[0]);// 全部范围
		} else {
			conditions
					.append(Constants.BID_LOAN_SCHEDULE_CONDITION[loan_schedule]);
		}

		if (NumberUtil.isNumericInt(_loanType)) {
			product_id = Integer.parseInt(_loanType);
			if (product_id > 0) {
				conditions.append(" and product_id = ? ");
				values.add(product_id);
			}

		}

		if (NumberUtil.isNumericInt(minLevelStr)) {
			minLevel = Integer.parseInt(minLevelStr);
			if (minLevel > 0) {
				conditions.append(" and num <= ? ");
				values.add(minLevel);
			}

		}

		if (NumberUtil.isNumericInt(maxLevelStr)) {
			maxLevel = Integer.parseInt(maxLevelStr);
			if (maxLevel > 0) {
				conditions.append(" and num >= ? ");
				values.add(maxLevel);
			}

		}

		if (!StringUtils.isBlank(_startDate) && !StringUtils.isBlank(_endDate)) {
			conditions
					.append(" and repayment_time >= ? and  repayment_time <= ? ");
			values.add(DateUtil.strToYYMMDDDate(_startDate));
			values.add(DateUtil.strToYYMMDDDate(_endDate));
		}

		if (NumberUtil.isNumericInt(_orderType)) {
			orderType = Integer.parseInt(_orderType);
		}

		if (orderType < 0 || orderType > 4) {
			conditions.append(Constants.BID_ORDER_CONDITION[0]);
		} else {
			conditions.append(Constants.BID_ORDER_CONDITION[orderType]);
		}

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			bidList = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "系统异常，给您带来的不便敬请谅解！";
			error.code = -2;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("apr", apr);
		conditionMap.put("amount", amount);
		conditionMap.put("loanSchedule", loan_schedule);
		conditionMap.put("startDate", _startDate);
		conditionMap.put("endDate", _endDate);
		conditionMap.put("minLevel", minLevel);
		conditionMap.put("maxLevel", maxLevel);
		conditionMap.put("orderType", orderType);
		conditionMap.put("keywords", _keywords);
		conditionMap.put("loanType", product_id);

		error.code = 1;
		error.msg = "查询成功";
		page.page = bidList;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 理财首页用户收藏的所有借款标
	 */
	public static PageBean<v_front_user_attention_bids> queryAllCollectBids(
			long userId, int currPage, int pageSize, ErrorInfo error) {

		List<v_front_user_attention_bids> bidList = new ArrayList<v_front_user_attention_bids>();
		PageBean<v_front_user_attention_bids> page = new PageBean<v_front_user_attention_bids>();
		page.pageSize = pageSize;
		page.currPage = currPage;

		try {
			bidList = GenericModel.find("user_id = ?", userId)
					.fetch(currPage, page.pageSize);
			page.totalCount = (int) GenericModel.count(
					"user_id = ?", userId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info(e.getMessage());
			error.code = -1;

			return page;
		}
		error.code = 1;
		page.page = bidList;

		return page;
	}

	/**
	 * 查询用户所有投资记录
	 * 
	 * @param userId
	 * @return
	 */
	public static PageBean<v_invest_records> queryUserInvestRecords(
			long userId, int currPage, int pageSize, String typeStr,
			String paramter, ErrorInfo error) {

		int type = 0;
		String[] typeCondition = { " and  no like ? or  name like ? ",
				" and  no like ? ", " and name like ? " };

		List<v_invest_records> investRecords = new ArrayList<v_invest_records>();
		PageBean<v_invest_records> page = new PageBean<v_invest_records>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		StringBuffer conditions = new StringBuffer(" 1=1 and  user_id=?  ");
		List<Object> values = new ArrayList<Object>();
		values.add(userId);

		if (typeStr == null && paramter == null) {

			try {
				investRecords = GenericModel.find(conditions.toString(),
						values.toArray()).fetch(currPage, page.pageSize);
				page.totalCount = (int) GenericModel.count(
						conditions.toString(), values.toArray());
			} catch (Exception e) {
				e.printStackTrace();

				return page;
			}
			page.page = investRecords;
			error.code = 1;
			return page;
		}

		if (StringUtils.isNotBlank(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		if (type == 0) {
			conditions.append(typeCondition[0]);
			values.add("%" + paramter + "%");
			values.add("%" + paramter + "%");
		} else {
			conditions.append(typeCondition[type]);
			values.add("%" + paramter + "%");
		}

		try {
			investRecords = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return page;
		}
		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keyWords", paramter);
		conditionMap.put("type", type);

		page.conditions = conditionMap;
		page.page = investRecords;
		error.code = 1;
		return page;
	}

	/**
	 * 查询用户所有投资记录(AJAX)
	 * 
	 * @param userId
	 * @return
	 */
	public static PageBean<v_invest_records> queryInvestRecords(long userId,
			String currPageStr, String pageSizeStr, ErrorInfo error) {
		error.clear();

		int currPage = Constants.ONE;
		int pageSize = Constants.TWO;

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		PageBean<v_invest_records> page = new PageBean<v_invest_records>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		List<v_invest_records> recordDetails = new ArrayList<v_invest_records>();

		try {
			page.totalCount = (int) GenericModel.count("user_id = ?",
					userId);
			recordDetails = GenericModel.find(
					"user_id = ? order by time desc", userId).fetch(currPage,
					page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询查询用户所有投资记录时：" + e.getMessage());
			error.code = -1;
			error.msg = "用户所有投资记录查询失败";
		}

		page.page = recordDetails;
		error.code = 1;
		return page;
	}

	/**
	 * 查询理财交易总数
	 * 
	 * @param error
	 * @return
	 */
	public static long queryTotalInvestCount(ErrorInfo error) {
		error.clear();
		long count = 0;

		try {
			count = GenericModel.count();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("数据库异常");
			error.code = -1;
			error.msg = "查询理财交易总数失败";

			return -1;
		}
		error.code = 1;
		return count;
	}

	/**
	 * 查询理财交易总金额
	 * 
	 * @param error
	 * @return
	 */
	public static double queryTotalDealAmount(ErrorInfo error) {
		error.clear();
		Double count = null;

		try {
			count = GenericModel.find("select SUM(amount) from t_invests").first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("数据库异常");
			error.code = -1;
			error.msg = "查询理财交易总金额失败";

			return -1;
		}
		error.code = 1;
		return null == count ? 0 : count;
	}

	/**
	 * 关闭投标机器人
	 * 
	 * @param robotId
	 * @param error
	 * @return
	 */
	public static int closeRobot(long userId, long robotId, ErrorInfo error) {

		EntityManager em = JPA.em();
		// User user = User.currUser();

		try {
			int rows = em
					.createQuery(
							" update t_user_automatic_invest_options set status = 0 where id = ?")
					.setParameter(1, robotId).executeUpdate();

			if (rows == 0) {
				JPA.setRollbackOnly();
				error.msg = "关闭投标机器人失败！";
				error.code = -1;

				return error.code;
			}
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "关闭投标机器人失败！";
			error.code = -1;

			return error.code;
		}

		DealDetail.userEvent(userId, UserEvent.CLOSE_ROBOT, "关闭投标机器人", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		error.msg = "关闭投标机器人成功！";
		error.code = 1;

		return error.code;

	}

	/**
	 * 设置或修改自动投标机器人
	 * 
	 * @param userId
	 * @param bidAmount
	 * @param rateStart
	 * @param rateEnd
	 * @param deadlineStart
	 * @param deadlineEnd
	 * @param creditStart
	 * @param creditEnd
	 * @param remandAmount
	 * @param borrowWay
	 */
	public static int saveOrUpdateRobot(long userId, int validType,
			int validDate, double minAmount, double maxAmount,
			String bidAmount, String rateStart, String rateEnd,
			String deadlineStart, String deadlineEnd, String creditStart,
			String creditEnd, String remandAmount, String borrowWay,
			ErrorInfo error) {

		error.clear();
		String sql = "select balance from t_users where id = ?";
		Double balance = 0.0;

		t_user_automatic_invest_options robot = null;

		try {
			balance = GenericModel.find(sql, userId).first();

			/* 查询用户是否设置过自动投标 */
			robot = GenericModel.find(" user_id = ? ",
					userId).first();
		} catch (Exception e) {
			error.msg = "对不起！系统异常!";
			error.code = -1;

			return error.code;
		}

		if (validType != 0 && validType != 1) {
			error.msg = "非法参数";
			error.code = -1;

			return error.code;
		}

		if (validDate <= 0) {
			error.msg = "请选择有效期";
			error.code = -1;

			return error.code;
		}

		if (minAmount < IPSConstants.MIN_AMOUNT) {
			error.msg = "借款额度必须大于" + IPSConstants.MIN_AMOUNT;
			error.code = -1;

			return error.code;
		}

		if (minAmount > maxAmount) {
			error.msg = "最高借款额度不能小于最低借款额度";
			error.code = -1;

			return error.code;
		}

		if (robot == null) {
			t_user_automatic_invest_options robotNew = new t_user_automatic_invest_options();
			robotNew.user_id = userId;
			robotNew.min_interest_rate = Double.parseDouble(rateStart);
			robotNew.max_interest_rate = Double.parseDouble(rateEnd);

			if (Double.parseDouble(rateEnd) < Double.parseDouble(rateStart)) {
				error.msg = "对不起！您设置的利率上限不能小于利率下限！";
				error.code = -1;

				return error.code;
			}

			if (null != deadlineStart) {
				robotNew.min_period = Integer.parseInt(deadlineStart);
			}

			if (null != deadlineEnd) {
				robotNew.max_period = Integer.parseInt(deadlineEnd);

				if (Integer.parseInt(deadlineEnd) < Integer
						.parseInt(deadlineStart)) {
					error.msg = "对不起！您设置的借款期限上限不能小于借款期限下限！";
					error.code = -2;

					return error.code;
				}
			}

			if (null != creditStart) {
				robotNew.min_credit_level_id = Integer.parseInt(creditStart);
			}

			if (null != creditEnd) {
				robotNew.max_credit_level_id = Integer.parseInt(creditEnd);

				if (Integer.parseInt(creditEnd) >= Integer
						.parseInt(creditStart)) {
					error.msg = "对不起！您设置的最高信用等级不能低于最低信用等级！";
					error.code = -3;
				}
			}

			if (balance < Double.parseDouble(remandAmount)) {
				error.msg = "对不起！您预留金额不能大于您的可用余额！";
				error.code = -4;
				return error.code;
			}

			if (Double.parseDouble(bidAmount) > balance) {
				error.msg = "对不起！您设置的投标金额不能大于您的可用余额！";
				error.code = -5;
				return error.code;
			}

			if (Double.parseDouble(bidAmount)
					+ Double.parseDouble(remandAmount) > balance) {
				error.msg = "对不起！您设置的投标金额和投标金额总和不能大于您的可用余额！";
				error.code = -5;
				return error.code;
			}

			if (null == remandAmount) {
				error.msg = "对不起！您预留金额不能为空！";
				error.code = -6;
				return error.code;
			}

			if (null == bidAmount) {
				error.msg = "对不起！每次投标金额不能为空！";
				error.code = -7;
				return error.code;
			}

			if (null == borrowWay) {
				error.msg = "对不起！借款类型不能为空！";
				error.code = -8;

				return error.code;
			}

			if (Double.parseDouble(bidAmount) < 0) {
				error.msg = "对不起！您设置的投标金额应该大于0！";
				error.code = -9;
				return error.code;
			}

			if (0 > Double.parseDouble(remandAmount)) {
				error.msg = "对不起！您预留金额不能小于0！";
				error.code = -10;
				return error.code;
			}

			robotNew.retention_amout = Double.parseDouble(remandAmount);
			robotNew.amount = Double.parseDouble(bidAmount);

			robotNew.status = Constants.IPS_ENABLE ? false : true;
			robotNew.loan_type = borrowWay;
			robotNew.time = new Date();
			robotNew.valid_type = validType;
			robotNew.valid_date = validDate;
			robotNew.min_amount = minAmount;
			robotNew.max_amount = maxAmount;

			try {
				robotNew.save();
			} catch (Exception e) {
				error.msg = "对不起！本次设置投标机器人失败！请您重试！";
				error.code = -9;
				return error.code;
			}

		} else {

			robot.user_id = userId;
			robot.min_interest_rate = Double.parseDouble(rateStart);
			robot.max_interest_rate = Double.parseDouble(rateEnd);

			if (Double.parseDouble(rateEnd) < Double.parseDouble(rateStart)) {
				error.msg = "对不起！您设置的利率上限不能小于利率下限！";
				error.code = -1;

				return error.code;
			}

			if (null != deadlineStart) {
				robot.min_period = Integer.parseInt(deadlineStart);
			}

			if (null != deadlineEnd) {
				robot.max_period = Integer.parseInt(deadlineEnd);

				if (Integer.parseInt(deadlineEnd) < Integer
						.parseInt(deadlineStart)) {
					error.msg = "对不起！您设置的借款期限上限不能小于借款期限下限！";
					error.code = -2;

					return error.code;
				}
			}

			if (null != creditStart) {
				robot.min_credit_level_id = Integer.parseInt(creditStart);
			}

			if (null != creditEnd) {
				robot.max_credit_level_id = Integer.parseInt(creditEnd);

				if (Integer.parseInt(creditEnd) >= Integer
						.parseInt(creditStart)) {
					error.msg = "对不起！最高信用等级不能低于最低信用等级！";
					error.code = -3;
					return error.code;
				}
			}

			if (balance < Double.parseDouble(remandAmount)) {
				error.msg = "对不起！您预留金额不能大于您的可用余额！";
				error.code = -4;
				return error.code;
			}

			if (Double.parseDouble(bidAmount) > balance) {
				error.msg = "对不起！您设置的投标金额不能大于您的可用余额！";
				error.code = -5;
				return error.code;
			}

			if (null == remandAmount) {
				error.msg = "对不起！您预留金额不能为空！";
				error.code = -6;
				return error.code;
			}

			if (null == bidAmount) {
				error.msg = "对不起！每次投标金额不能为空！";
				error.code = -7;
				return error.code;
			}

			if (null == borrowWay) {
				error.msg = "对不起！借款类型不能为空！";
				error.code = -8;

				return error.code;
			}

			if (Double.parseDouble(bidAmount) < 0) {
				error.msg = "对不起！您设置的投标金额应该大于0！";
				error.code = -9;
				return error.code;
			}

			if (0 > Double.parseDouble(remandAmount)) {
				error.msg = "对不起！您预留金额不能小于0！";
				error.code = -10;
				return error.code;
			}

			robot.retention_amout = Double.parseDouble(remandAmount);
			robot.amount = Double.parseDouble(bidAmount);

			robot.status = Constants.IPS_ENABLE ? false : true;
			robot.loan_type = borrowWay;
			robot.time = new Date();
			robot.valid_type = validType;
			robot.valid_date = validDate;
			robot.min_amount = minAmount;
			robot.max_amount = maxAmount;

			try {
				robot.save();
			} catch (Exception e) {
				error.msg = "对不起！本次设置投标机器人失败！请您重试！";
				error.code = -9;
				return error.code;
			}

		}

		DealDetail.userEvent(userId, UserEvent.OPEN_ROBOT, "开启投标机器人", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		error.msg = "设置成功";
		error.code = 1;
		return 1;

	}

	/**
	 * 获取用户投标机器人
	 * 
	 * @param userId
	 * @return
	 */
	public static t_user_automatic_invest_options getUserRobot(long userId,
			ErrorInfo error) {

		t_user_automatic_invest_options robot = null;

		try {
			robot = GenericModel.find(" user_id = ? ",
					userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return null;
		}

		error.code = 1;
		return robot;
	}

	public static double getUserBalance(long userId) {

		double balance = 0;

		try {
			balance = GenericModel.find(
					" select balance from t_users where id = ? ", userId)
					.first();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return balance;
	}

	/**
	 * 检查贷款用户是否开启了投标机器人，贷款用户在获得贷款时会自动关闭自动投标，以避免借款被用作自动投标资金
	 * 
	 * @param bidId
	 */
	public static int closeUserBidRobot(long userId) {

		if (0 == userId)
			return -1;

		ErrorInfo error = new ErrorInfo();
		Boolean status = null;
		String hql1 = "select status from t_user_automatic_invest_options  where user_id = ?";

		try {
			status = GenericModel.find(hql1, userId).first();
		} catch (Exception e) {
			Logger.error("理财->查询开启自动投标状态:" + e.getMessage());

			return -2;
		}

		if (null == status)
			return 1;

		/* 表示投标机器人开启,关闭投标机器人 */
		if (status) {
			String hql2 = "update t_user_automatic_invest_options set status = ? where user_id = ?";

			Query query = JPA.em().createQuery(hql2);
			query.setParameter(1, Constants.NOT_ENABLE);
			query.setParameter(2, userId);

			try {
				return query.executeUpdate();
			} catch (Exception e) {
				Logger.error(e.getMessage());

				return -3;
			}
		}

		DealDetail.userEvent(userId, UserEvent.CLOSE_ROBOT, "关闭投标机器人", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		return 1;
	}

	public long investUserId;
	public double investAmount;

	/**
	 * 查询对应标的的所有投资者以及投资金额
	 * 
	 * @param bidId
	 * @return
	 */
	public static List<Invest> queryAllInvestUser(long bidId) {
		List<Map<Long, Object>> tamounts = null;
		List<Invest> amounts = new ArrayList<Invest>();

		String hql = "select new Map(i.user_id as userId, i.amount as amount, i.mer_bill_no as mer_bill_no) from t_invests i where i.bid_id=?  order by time";

		try {
			tamounts = GenericModel.find(hql, bidId).fetch();
		} catch (Exception e) {
			Logger.error("查询对应标的的所有投资者以及投资金额:" + e.getMessage());

			return null;
		}

		if (null == tamounts)
			return null;

		if (tamounts.size() == 0) {
			return amounts;
		}

		Invest invest = null;

		for (Map<Long, Object> map : tamounts) {
			invest = new Invest();

			invest.investUserId = Long.parseLong(map.get("userId") + "");
			invest.investAmount = Double.parseDouble(map.get("amount") + "");
			invest.merBillNo = (String) map.get("mer_bill_no");

			amounts.add(invest);
		}

		return amounts;
	}

	/**
	 * 更新标的浏览次数
	 * 
	 * @param bidId
	 */
	public static void updateReadCount(long bidId, ErrorInfo error) {
		EntityManager em = JPA.em();
		/* 增加该借款标浏览次数 */
		int rows = em
				.createQuery(
						"update t_bids set read_count = read_count + 1 where id = ?")
				.setParameter(1, bidId).executeUpdate();

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
		}

		error.code = 1;
	}

	/**
	 * 等待满标的理财标
	 * 
	 * @param userId
	 * @param type
	 *            1:全部 2：标题 3：借款标编号
	 * @param params
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_waiting_full_invest_bids> queryUserWaitFullBids(
			long userId, String typeStr, String params, int currPage,
			int pageSize, ErrorInfo error) {

		PageBean<v_user_waiting_full_invest_bids> page = new PageBean<v_user_waiting_full_invest_bids>();
		List<v_user_waiting_full_invest_bids> bidList = new ArrayList<v_user_waiting_full_invest_bids>();

		StringBuffer conditions = new StringBuffer(" 1=1 and  user_id = ?  ");
		List<Object> values = new ArrayList<Object>();
		values.add(userId);

		page.pageSize = pageSize;
		page.currPage = currPage;

		if (typeStr == null && params == null) {
			try {
				page.totalCount = (int) GenericModel.count(
						conditions.toString(), values.toArray());
				bidList = GenericModel.find(
						conditions.toString(), values.toArray()).fetch(
						currPage, page.pageSize);
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return page;
			}

			page.page = bidList;
			error.code = 1;
			return page;
		}

		int type = 0;
		String[] typeCondition = { " ", " and  title like ? ",
				" and no like ? " };

		if (StringUtils.isNotBlank(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		if (type == 0) {
			conditions.append(typeCondition[0]);
		} else {
			conditions.append(typeCondition[type]);
			values.add("%" + params + "%");
		}

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			bidList = GenericModel.find(
					conditions.toString(), values.toArray()).fetch(currPage,
					page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			return page;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keyWords", params);
		conditionMap.put("type", type);

		page.page = bidList;
		page.conditions = conditionMap;
		error.code = 1;
		return page;
	}

	/**
	 * 查询用户所有投资成功的借款标
	 * 
	 * @param userId
	 * @param type
	 * @param params
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_success_invest_bids> queryUserSuccessInvestBids(
			long userId, String typeStr, String params, int currPage,
			int pageSize, ErrorInfo error) {

		int type = 0;
		String[] typeCondition = { " ", " and title like  ? ",
				"  and no like ?  " };

		PageBean<v_user_success_invest_bids> page = new PageBean<v_user_success_invest_bids>();
		List<v_user_success_invest_bids> list = new ArrayList<v_user_success_invest_bids>();

		page.pageSize = pageSize;
		page.currPage = currPage;

		StringBuffer conditions = new StringBuffer(" 1=1 and  user_id=?  ");
		List<Object> values = new ArrayList<Object>();
		values.add(userId);

		if (typeStr == null && params == null) {

			try {
				page.totalCount = (int) GenericModel.count(
						" user_id=? ", userId);
				list = GenericModel.find(" user_id=?   ", userId)
						.fetch(currPage, page.pageSize);
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return page;
			}

			page.page = list;
			error.code = 1;
			return page;
		}

		if (StringUtils.isNotBlank(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		if (type == 0) {
			conditions.append(typeCondition[0]);
		} else {
			conditions.append(typeCondition[type]);
			values.add("%" + params + "%");
		}

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			list = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -2;
			return page;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keyWords", params);
		conditionMap.put("type", type);

		page.conditions = conditionMap;
		page.page = list;
		error.code = 1;
		return page;
	}

	/**
	 * 查询用户收款中的理财标
	 * 
	 * @param userId
	 * @param type
	 *            1:全部 2：标题 3：借款标编号
	 * @param params
	 * @param currPage
	 * @return
	 */

	public static PageBean<v_receiving_invest_bids> queryUserAllReceivingInvestBids(
			long userId, String typeStr, String params, int currPage,
			int pageSize, ErrorInfo error) {

		int type = 0;
		String[] typeCondition = { " ", " and title like  ? ",
				"  and no like ?  " };

		PageBean<v_receiving_invest_bids> page = new PageBean<v_receiving_invest_bids>();
		List<v_receiving_invest_bids> bidList = new ArrayList<v_receiving_invest_bids>();

		page.pageSize = pageSize;
		page.currPage = currPage;

		StringBuffer conditions = new StringBuffer(" 1=1 and  user_id = ?  ");
		List<Object> values = new ArrayList<Object>();
		values.add(userId);

		if (typeStr == null && params == null) {
			try {
				page.totalCount = (int) GenericModel.count(
						conditions.toString(), values.toArray());
				bidList = GenericModel.find(" user_id=?   ", userId)
						.fetch(currPage, page.pageSize);
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
				return page;
			}
			page.page = bidList;
			error.code = 1;
			return page;
		}

		if (StringUtils.isNotBlank(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		if (type == 0) {
			conditions.append(typeCondition[0]);
		} else {
			conditions.append(typeCondition[type]);
			values.add("%" + params + "%");
		}

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			bidList = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -2;
			return page;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keyWords", params);
		conditionMap.put("type", type);

		page.conditions = conditionMap;
		page.page = bidList;
		error.code = 1;
		return page;
	}

	/**
	 * 更新t_bids表Version字段
	 * 
	 * @param bidId
	 * @param error
	 * @return
	 */
	public static int updatevVersion(long bidId, int version, ErrorInfo error) {

		EntityManager em = JPA.em();
		String sql = "update t_bids set version = version + 1 where id = ? and version = ?";

		try {
			int rows = em.createQuery(sql).setParameter(1, bidId)
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
	 * 获取t_bids表特定标版本号
	 * 
	 * @param bidId
	 * @param error
	 * @return
	 */
	public static int getBidVersion(long bidId, ErrorInfo error) {

		int version = 0;
		String sql = "select version from t_bids where id = ?";

		try {
			version = GenericModel.find(sql, bidId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常！请您联系平台管理员！";
			error.code = -1;
			return -1;
		}
		error.code = 1;
		return version;
	}

	/**
	 * 已投总额增加,投标进度增加
	 * 
	 * @param bidId
	 * @param amount
	 * @param schedule
	 * @param error
	 * @return
	 */
	public static int updateBidschedule(long bidId, double amount,
			double schedule, ErrorInfo error) {

		EntityManager em = JPA.em();

		try {
			int rows = em
					.createQuery(
							"update t_bids set loan_schedule=?,has_invested_amount=? where id=?")
					.setParameter(1, schedule).setParameter(2, amount)
					.setParameter(3, bidId).executeUpdate();

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
	 * 更新借款标满标时间
	 * 
	 * @param bidId
	 * @param error
	 * @return
	 */
	public static int updateBidExpiretime(long bidId, ErrorInfo error) {
		EntityManager em = JPA.em();
		try {
			int rows = em
					.createQuery(
							"update t_bids set real_invest_expire_time = ? where id=?")
					.setParameter(1, new Date()).setParameter(2, bidId)
					.executeUpdate();

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
	 * 根据投资ID获取对应bidId,userId
	 * 
	 * @param investId
	 * @param error
	 * @return
	 */
	public static t_invests queryUserAndBid(long investId) {
		t_invests invest = null;
		try {
			invest = GenericModel
					.find("select new t_invests(user_id,bid_id) from t_invests where id = ?",
							investId).first();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return invest;
	}

	/**
	 * 即时查询借款标对象
	 * 
	 * @param bidId
	 * @return
	 */
	public static Map<String, Object> bidMap(long bidId, ErrorInfo error) {

		Map<String, Object> map = new HashMap<String, Object>();
		String sql = "select new Map(id as id,title as title,min_invest_amount as min_invest_amount,average_invest_amount as average_invest_amount,amount as amount, status as status,"
				+ "invest_expire_time as invest_expire_time,has_invested_amount as has_invested_amount,user_id as user_id,product_id as product_id) from t_bids  where id = ? ";

		try {
			map = GenericModel.find(sql, bidId).first();

		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
			error.code = -11;

		}
		error.code = 1;
		return map;
	}

	/**
	 * 投标操作
	 * 
	 * @param userId
	 * @param bidId
	 * @param investTotal
	 * @param dealpwdStr
	 * @param isAuto
	 *            是否自动投标
	 * @param isRepair
	 *            是否补单
	 * @param error
	 */
	public static void invest(long userId, long bidId, int investTotal,
			String dealpwdStr, boolean isAuto, boolean isRepair,
			String repairBillNo, ErrorInfo error) {
		error.clear();
		t_users user1 = null;
		t_bids bidOne = null;
		try {
			bidOne = GenericModel.findById(bidId);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}

		try {
			user1 = GenericModel.findById(userId);

		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常！请您联系平台管理员！";
			error.code = -2;

			return;
		}

		if (null == user1) {
			error.msg = "对不起！系统异常！请您联系平台管理员！";
			error.code = -2;

			return;
		}

		if (user1.balance <= 0) {

			error.msg = "对不起！您余额不足，请及时充值！";
			error.code = -2;

			return;

		}

		double balance = user1.balance;
		boolean black = user1.is_blacklist;
		String dealpwd = user1.pay_password;

		Map<String, Object> bid = bidMap(bidId, error);

		if (error.code < 0) {
			error.msg = "对不起！系统异常！请您联系平台管理员！";
			error.code = -2;

			return;
		}

		double minInvestAmount = Double.parseDouble(bid
				.get("min_invest_amount") + "");
		double averageInvestAmount = Double.parseDouble(bid
				.get("average_invest_amount") + "");
		double amount = Double.parseDouble(bid.get("amount") + "");
		int status = Integer.parseInt(bid.get("status") + "");
		Date invest_expire_time = DateUtil.strToDate(bid
				.get("invest_expire_time") + "");

		double hasInvestedAmount = Double.parseDouble(bid
				.get("has_invested_amount") + "");
		long bidUserId = Long.parseLong(bid.get("user_id") + "");// 借款者
		long product_id = Long.parseLong(bid.get("product_id") + "");

		DataSafety data = new DataSafety();// 数据防篡改(针对当前投标会员)
		data.setId(userId);
		boolean sign = data.signCheck(error);

		if (new Date().getTime() > invest_expire_time.getTime()) {
			error.msg = "对不起！此借款标已经不处于招标状态，请投资其他借款标！谢谢！";
			error.code = -2;
			JPA.setRollbackOnly();

			return;
		}
		if (error.code < 0) {
			error.msg = "对不起！尊敬的用户，你的账户资金出现异常变动，请速联系管理员！";
			error.code = -2;
			JPA.setRollbackOnly();

			return;
		}

		if (!sign) {// 数据被异常改动
			error.msg = "对不起！尊敬的用户，你的账户资金出现异常变动，请速联系管理员！";
			error.code = -2;
			JPA.setRollbackOnly();

			return;
		}

		t_products product = GenericModel.findById(product_id);
		boolean is_deal_password = product.is_deal_password;

		if (investTotal <= 0) {
			error.msg = "对不起！请输入正确格式的数字!";
			error.code = -10;

			return;
		}

		if (userId == bidUserId) {
			error.msg = "对不起！您不能投自己的借款标!";
			error.code = -10;

			return;

		}

		if (black) {
			error.msg = "对不起！您已经被平台管理员限制操作！请您与平台管理员联系！";
			error.code = -1;

			return;
		}

		if (User.isInMyBlacklist(bidUserId, userId, error) < 0) {
			error.msg = "对不起！您已经被对方拉入黑名单，您被限制投资此借款标！";
			error.code = -2;

			return;
		}

		if (status != Constants.BID_ADVANCE_LOAN
				&& status != Constants.BID_FUNDRAISE) {
			error.msg = "对不起！此借款标已经不处于招标状态，请投资其他借款标！谢谢！";
			error.code = -2;

			return;
		}

		if (amount == hasInvestedAmount) {
			error.msg = "对不起！此借款标已经不处于招标状态，请投资其他借款标！谢谢！";
			error.code = -2;

			return;
		}

		if (amount <= hasInvestedAmount) {
			error.msg = "对不起！该借款标已经投满!";
			error.code = -13;

			return;
		}

		if (is_deal_password == true && !isRepair) {
			if (StringUtils.isBlank(dealpwdStr)) {
				error.msg = "对不起！请输入交易密码!";
				error.code = -12;

				return;
			}

			if (!Encrypt.MD5(dealpwdStr + Constants.ENCRYPTION_KEY).equals(
					dealpwd)) {
				error.msg = "对不起！交易密码错误!";
				error.code = -13;
				return;
			}
		}

		/* 普通模式 */
		if (averageInvestAmount == 0) {

			if (amount - hasInvestedAmount >= minInvestAmount) {

				if (investTotal < minInvestAmount) {
					error.msg = "对不起！您最少要投" + minInvestAmount + "元";
					error.code = -3;

					return;
				}
			} else {

				if (investTotal < amount - hasInvestedAmount) {
					double money = amount - hasInvestedAmount;
					error.msg = "对不起！您最少要投" + money + "元";
					error.code = -4;

					return;
				}
			}

			if (balance < investTotal && bidOne.product_id != 9) {
				error.msg = "对不起！您可用余额不足！根据您的余额您最多只能投" + balance + "元";
				error.code = -5;

				return;
			}

			if (investTotal > (amount - hasInvestedAmount)) {
				double money = amount - hasInvestedAmount;
				error.msg = "对不起！您的投资金额超过了该标的剩余金额,您最多只能投" + money + "元！";
				error.code = -6;

				return;
			}
		}

		/* 认购模式 */
		if (minInvestAmount == 0) {
			if (investTotal <= 0) {
				error.msg = "对不起！您至少应该买一份！";
				error.code = -7;

				return;
			}
			if (investTotal > ((amount - hasInvestedAmount) / averageInvestAmount)) {
				error.msg = "对不起！您最多只能购买" + (amount - hasInvestedAmount)
						/ averageInvestAmount + "份！";
				error.code = -8;

				return;
			}

			investTotal = (int) (investTotal * averageInvestAmount);

			if (balance < investTotal && bidOne.product_id != 9) {
				error.msg = "对不起！您余额不足！您最多只能购买"
						+ (int) (balance / averageInvestAmount) + "份！";
				error.code = -8;

				return;
			}

		}

		if (error.code < 0) {
			error.msg = "对不起！系统异常！请您联系平台管理员！";
			error.code = -2;

			return;
		}

		String pMerBillNo = null;

		if (Constants.IPS_ENABLE && !isRepair) {
			if (!isAuto) {
				return;
			}

			// 自动投标
			pMerBillNo = Payment.createBillNo(userId,
					IPSOperation.REGISTER_CREDITOR);

			Map<String, Object> map = new HashMap<String, Object>();
			map.put("userId", userId);
			map.put("bidId", bidId);
			map.put("investAmount", investTotal);
			Cache.set(pMerBillNo, map, IPSConstants.CACHE_TIME);

			Map<String, String> args = Payment.registerCreditor(pMerBillNo,
					userId, bidId, 2, investTotal, error);
			String str = WS.url(IPSConstants.ACTION).setParameters(args).post()
					.getString();
			Logger.info("自动投标接口输出参数\n" + str);

			JSONObject jsonObj = JSONObject.fromObject(str);
			String pMerCode = jsonObj.getString("pMerCode");
			String pErrCode = jsonObj.getString("pErrCode");
			String pErrMsg = jsonObj.getString("pErrMsg");
			String p3DesXmlPara = jsonObj.getString("p3DesXmlPara");
			String pSign = jsonObj.getString("pSign");

			if (!Payment.checkSign(
					pMerCode + pErrCode + pErrMsg + p3DesXmlPara, pSign)) {
				error.code = -1;
				error.msg = "sign校验失败";

				return;
			}

			if (!"MG00000F".equals(pErrCode)) {
				error.code = -1;
				error.msg = pErrMsg;

				return;
			}
		} else if (Constants.IPS_ENABLE && isRepair) {
			pMerBillNo = repairBillNo;
		}

		doInvest(user1, bid, investTotal, pMerBillNo, error);
	}

	/**
	 * 投标操作(写入数据库)
	 * 
	 * @param userId
	 * @param bidId
	 * @param parameter
	 *            接收的参数
	 * @param info
	 * @return
	 */
	public static void doInvest(t_users user1, Map<String, Object> bid,
			int investTotal, String pMerBillNo, ErrorInfo error) {
		error.clear();
		long userId = user1.id;
		long bidId = Long.parseLong(bid.get("id") + "");
		int version = getBidVersion(bidId, error);// 获取t_bids version（版本）字段
		double amount = Double.parseDouble(bid.get("amount") + "");
		double hasInvestedAmount = Double.parseDouble(bid
				.get("has_invested_amount") + "");
		long bidUserId = Long.parseLong(bid.get("user_id") + "");// 借款者

		double schedule = Arith.div(hasInvestedAmount + investTotal, amount, 4) * 100;//

		/* 已投总额增加,投标进度增加 */
		int result = updateBidschedule(bidId, hasInvestedAmount + investTotal,
				schedule, error);

		if (result < 0) {
			error.msg = "对不起！系统异常！对此造成的不便敬请谅解！";
			error.code = -8;
			JPA.setRollbackOnly();

			return;
		}

		/* 满标 */
		if (amount == (hasInvestedAmount + investTotal)) {

			// 更新满标时间
			int resulta = updateBidExpiretime(bidId, error);

			if (resulta < 0) {
				error.msg = "对不起！系统异常！对此造成的不便敬请谅解！";
				error.code = -8;
				JPA.setRollbackOnly();

				return;
			}

			// 成功满标，增加信用积分
			DealDetail.addCreditScore(bidUserId, 3, 1, bidId, "成功满标，借款人添加信用积分",
					error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return;
			}
		}

		t_invests invest = new t_invests();
		invest.user_id = userId;
		invest.time = new Date();
		invest.bid_id = bidId;
		/* 0 正常(转让入的也是0) */
		invest.transfer_status = 0;
		invest.amount = investTotal;
		invest.mer_bill_no = pMerBillNo;

		// 更新t_bids版本字段
		updatevVersion(bidId, version, error);

		if (error.code < 0) {
			error.msg = "对不起！系统异常！对此造成的不便敬请谅解！";
			error.code = -8;
			JPA.setRollbackOnly();

			return;
		}

		int result5 = DealDetail.freezeFund(userId, investTotal);

		if (result5 <= 0) {
			error.msg = "对不起！系统异常！请您重试或联系平台管理员！";
			error.code = -9;
			JPA.setRollbackOnly();

			return;

		}
		// 更新会员性质
		User.updateMasterIdentity(userId, Constants.INVEST_USER, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		Map<String, Double> funds = DealDetail.queryUserFund(userId, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		double user_amount = funds.get("user_amount");
		double freeze = funds.get("freeze");
		double receive_amount = funds.get("receive_amount");

		/* 伪构记录 */

		DealDetail dealDetail = new DealDetail(userId,
				DealType.PAY_FREEZE_FUND, investTotal, bidId, user_amount,
				freeze - investTotal, receive_amount, "投标成功，支出投标冻结金额");

		dealDetail.addDealDetail(error);

		if (error.code < 0) {
			error.code = -25;
			error.msg = "添加交易记录失败!";
			JPA.setRollbackOnly();

			return;
		}

		// 添加交易记录
		dealDetail = new DealDetail(userId, 205, investTotal, bidId,
				user_amount, freeze, receive_amount, "投标成功，冻结投标金额"
						+ investTotal + "元");

		dealDetail.addDealDetail(error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;

		}
		// 投标用户增加系统积分 

		DealDetail
				.addScore(userId, 1, investTotal, bidId, "投标成功，添加系统积分", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;

		}
		// 投标一次增加信用积分

		DealDetail.addCreditScore(userId, 4, 1, bidId, "成功投标一次，投资人添加信用积分",
				error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;

		}
		DealDetail.userEvent(userId, UserEvent.INVEST, "成功投标", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		try {
			invest.save();
		} catch (Exception e) {
			error.msg = "对不起！您此次投资失败！请您重试或联系平台管理员！";
			error.code = -10;
			JPA.setRollbackOnly();

			return;
		}

		Map<String, Double> v = DealDetail.queryUserFund(userId, error);

		if (v.get("user_amount") < 0) {
			error.msg = "对不起！您账户余额不足，请及时充值！";
			error.code = -10;
			JPA.setRollbackOnly();

			return;
		}

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		DataSafety data = new DataSafety();
		data.setId(userId);// 更新数据防篡改字段
		data.updateSign(error);

		if (error.code < 0) {
			error.msg = "对不起！系统异常！请您重试或联系平台管理员！";
			error.code = -9;
			JPA.setRollbackOnly();
			return;
		}

		// 发送消息
		String username = user1.name;
		String title = bid.get("title") + "";

		TemplateEmail email = new TemplateEmail();// 发送邮件
		email.id = Templets.E_INVEST;

		if (email.status) {
			String econtent = email.content;
			econtent = econtent.replace("date",
					DateUtil.dateToString((new Date())));
			econtent = econtent.replace("userName", username);
			econtent = econtent.replace("title", title);
			econtent = econtent.replace("investAmount", investTotal + "");

			TemplateEmail.addEmailTask(user1.email, email.title, econtent);
		}

		TemplateStation station = new TemplateStation();// 发送站内信
		station.id = Templets.M_INVEST;

		if (station.status) {
			String stationContent = station.content;
			stationContent = stationContent.replace("date",
					DateUtil.dateToString((new Date())));
			stationContent = stationContent.replace("userName", username);
			stationContent = stationContent.replace("title", title);
			stationContent = stationContent.replace("investAmount", investTotal
					+ "");

			TemplateStation.addMessageTask(userId, station.title, stationContent);
		}

		TemplateSms sms = new TemplateSms();// 发送短信
		sms.id = Templets.S_INVEST;

		if (sms.status) {
			String smscontent = sms.content;
			smscontent = smscontent.replace("date",
					DateUtil.dateToString((new Date())));
			smscontent = smscontent.replace("userName", username);
			smscontent = smscontent.replace("title", title);
			smscontent = smscontent.replace("investAmount", investTotal + "");
			TemplateSms.addSmsTask(user1.mobile, smscontent);
		}

		error.msg = "投资成功！";
		error.code = 1;
	}

	/**
	 * 获取用户减掉预留金额后的可用金额
	 * 
	 * @param userId
	 * @param remandAmount
	 * @return
	 */
	public static double queryAutoUserBalance(long userId, double remandAmount) {

		double balance = 0;
		String sql = "select (balance-" + remandAmount
				+ ") as balance from t_users where id=?";

		try {
			balance = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();

		}

		return balance;
	}

	/**
	 * 按时间倒序排序查出所有开启了投标机器人的用户ID
	 * 
	 * @return
	 */
	public static List<Object> queryAllAutoUser() {

		List<Object> list = null;

		try {
			list = GenericModel
					.find("select user_id from t_user_automatic_invest_options where status = 1 order by time desc")
					.fetch();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	/**
	 * 将用户排到自动投标队尾
	 * 
	 * @param user_id
	 */
	public static void updateUserAutoBidTime(long user_id) {

		EntityManager em = JPA.em();
		em.createQuery(
				"update t_user_automatic_invest_options set time = ? where user_id = ?")
				.setParameter(1, new Date()).setParameter(2, user_id)
				.executeUpdate();
	}

	/**
	 * 查询符合用户设置条件的标的ID
	 * 
	 * @param autoOptions
	 * @param unit
	 * @param bidId
	 * @return
	 */
	public static Map<String, Object> queryBiderByParam(
			t_user_automatic_invest_options autoOptions, int unit, long bidId) {
		int min_period = 0;
		int max_period = 0;

		if (unit == -1) {// 单位为年
			min_period = autoOptions.min_period * 12;
			max_period = autoOptions.max_period * 12;
		}

		if (unit == 0) {
			min_period = autoOptions.min_period;
			max_period = autoOptions.max_period;
		}

		StringBuffer condition = new StringBuffer();
		condition
				.append("select new Map(id as id) from v_confirm_autoinvest_bids where apr >= "
						+ autoOptions.min_interest_rate
						+ " and apr <= "
						+ autoOptions.max_interest_rate
						+ " and min_invest_amount <= " + autoOptions.amount);

		if (autoOptions.min_period > 0 && autoOptions.max_period > 0) {
			condition.append(" and period >=" + min_period + " and period <="
					+ max_period);
		}

		if (autoOptions.min_credit_level_id > 0
				&& autoOptions.max_credit_level_id > 0) {
			condition.append(" and num >= " + autoOptions.max_credit_level_id
					+ "  and num <= " + autoOptions.min_credit_level_id);
		}

		condition.append(" and  loan_type in ( " + autoOptions.loan_type
				+ " )  and id=?");
		Map<String, Object> map = GenericModel.find(
				condition.toString(), bidId).first();
		return map;
	}

	/**
	 * 扣除保留金额后，计算最后投标金额
	 * 
	 * @param bidAmount
	 * @param schedule
	 * @param amount
	 * @param hasInvestedAmount
	 * @return
	 */
	public static int calculateBidAmount(double bidAmount, double schedule,
			double amount, double hasInvestedAmount) {

		int maxBidAmount = (int) (amount * 0.2);
		int invesAmount = 0;

		if (schedule < 95) {
			while (bidAmount > maxBidAmount) {
				bidAmount = bidAmount - 50;
			}

			do {
				invesAmount = (int) (hasInvestedAmount + bidAmount);
				schedule = invesAmount / amount;
				if (schedule > 95) {
					bidAmount = bidAmount - 50;
				}
			} while (schedule > 95);
		}

		return (int) bidAmount;
	}

	/**
	 * 增加用户自动投标记录
	 * 
	 * @param userId
	 * @param bidId
	 */
	public static void addAutoBidRecord(long userId, long bidId) {

		t_user_automatic_bid bid = new t_user_automatic_bid();

		bid.bid_id = bidId;
		bid.time = new Date();
		bid.user_id = userId;

		bid.save();
	}

	/**
	 * 判断用户是否已经自动投过当前标
	 * 
	 * @param bidId
	 * @param userId
	 * @return
	 */
	public static boolean hasAutoInvestTheBid(long bidId, long userId) {

		boolean flag = false;
		t_user_automatic_bid bid = GenericModel.find(
				"user_id=? and bid_id =?", userId, bidId).first();
		if (bid != null) {
			flag = true;
		}
		return flag;
	}

	public Map<String, Object> queryParamByBidId(long bidId) {
		String sql = "select new Map(user_id as user_id,amount as amount,min_invest_amount as min_invest_amount,average_invest_amount as average_invest_amount,"
				+ "has_invested_amount as has_invested_amount) from t_bids where id=?";
		return GenericModel.find(sql, bidId).first();
	}

	/**
	 * 查询所有投标进度小于且进入招标中十五分钟后的所有标的
	 * 
	 * @return
	 * @throws ParseException
	 */
	public static List<Map<String, Object>> queryAllBider() {

		List<Map<String, Object>> mapList = null;
		// String dateTime = "";
		// try {
		// dateTime = DateUtil.getDateMinusMinutes(15);
		// } catch (ParseException e1) {
		// e1.printStackTrace();
		// }//当前时间减去15分钟的时间
		//
		// Date date = DateUtil.strToDate(dateTime);
		String sql = "select new Map(id as bid_id,user_id as user_id,period_unit as period_unit,amount as amount,min_invest_amount as min_invest_amount,"
				+ "has_invested_amount as has_invested_amount,average_invest_amount as average_invest_amount ,loan_schedule as loan_schedule) "
				+ "from v_confirm_autoinvest_bids  ";

		try {
			mapList = GenericModel.find(sql).fetch();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return mapList;
	}

	/**
	 * 判断该借款标是否超过95%
	 * 
	 * @param bidId
	 * @return
	 */
	public static boolean judgeSchedule(long bidId) {

		boolean flag = false;
		Double schedule = 0.0;

		String sql = "select loan_schedule from t_bids where id = ? ";

		try {
			schedule = GenericModel.find(sql, bidId).first();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (schedule == null) {
			schedule = 0.0;
		}

		if (schedule >= 95) {
			flag = true;
		}

		return flag;
	}

	/**
	 * 资金托管模式下自动投标时额外条件判断
	 * 
	 * @param userIdStr
	 * @param bidIdStr
	 * @return
	 */
	public static boolean additionalJudgment(String userIdStr, String bidIdStr) {

		boolean flag = false;

		long userId = Long.parseLong(userIdStr);
		long bidId = Long.parseLong(bidIdStr);

		t_user_automatic_invest_options robot = null;
		double bidAmount = 0;

		try {
			robot = GenericModel.find(" user_id = ? ",
					userId).first();
			bidAmount = GenericModel.find(" select amount from t_bids where id = ? ",
					bidId).first();
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}

		if (null != robot && bidAmount > 0) {
			int dateType = robot.valid_type;
			int date = robot.valid_date;
			double minAmount = robot.min_amount;
			double maxAmount = robot.max_amount;
			Date time = robot.time;
			Date overTime = null;
			if (dateType == 0) {
				overTime = DateUtil.dateAddDay(time, date);
			} else {
				overTime = DateUtil.dateAddMonth(time, date);
			}

			boolean isOverTime = overTime.getTime() >= new Date().getTime() ? true
					: false;
			boolean isOverAmount = false;
			if (bidAmount >= minAmount && bidAmount <= maxAmount) {
				isOverAmount = true;
			}
			if (isOverTime && isOverAmount) {
				flag = true;
			}

		}

		return flag;
	}

	/**
	 * 自动投标
	 * 
	 * @throws ParseException
	 */
	public static void automaticInvest() {

		int unit = -2;// 标产品期限单位 -1：年 0：月 1：天
		long userId = -1;
		long bidId = -1;
		t_user_automatic_invest_options userParam = null;
		List<Map<String, Object>> biderList = Invest.queryAllBider();// 查出所有符合自动投标条件的标的
		ErrorInfo error = new ErrorInfo();

		if (null != biderList && biderList.size() > 0) {

			OK:

			if (null != Invest.queryAllAutoUser()
					&& Invest.queryAllAutoUser().size() > 0) {

				List<Object> userIds = Invest.queryAllAutoUser();
				// 遍历所有的符合条件进度低于95%的招标中的借款
				for (Map<String, Object> map : biderList) {

					// 遍历所有设置了投标机器人用户ID
					for (Object o : userIds) {

						boolean over = judgeSchedule(Long.parseLong(map
								.get("bid_id") + ""));

						if (over) {
							break OK;
						}

						if (!over) {// 借款标投标进度没有超过95%
							userId = Long.parseLong(o.toString());
							// 资金托管模式下的额外判断
							boolean overTime = additionalJudgment(userId + "",
									map.get("bid_id").toString());
							if (map.get("user_id").toString()
									.equals(userId + "")
									|| !overTime) { // 如果该借款是发布者的标,则发布者不能投标,用户自动排队到后面
								Invest.updateUserAutoBidTime(userId);// 将该用户排到队尾
							} else {
								// 获取用户设置的投标机器人参数
								userParam = GenericModel
										.find("from t_user_automatic_invest_options where user_id=?",
												userId).first();

								if (null != userParam) {
									bidId = Long.parseLong(map.get("bid_id")
											.toString());
									boolean flag = hasAutoInvestTheBid(bidId,
											userId);

									if (flag) {// 该用户已经投过该标的
										Invest.updateUserAutoBidTime(userId);// 将该用户排到队尾
									} else {
										unit = Integer.parseInt(map.get(
												"period_unit").toString());
										Map m = Invest.queryBiderByParam(
												userParam, unit, bidId);// 查询符合用户条件的标的

										if (null == m) {// 没有找到符合条件的标的
											Invest.updateUserAutoBidTime(userId);// 将该用户排到队尾
										} else {// 找到了符合条件的标的，现在开始计算投资额
											double amount = Double
													.parseDouble(map.get(
															"amount")
															.toString());// 标的借款总额
											double has_invested_amount = Double
													.parseDouble(map
															.get("has_invested_amount")
															.toString());// 标的已投金额
											double balance = Invest
													.queryAutoUserBalance(
															userId,
															userParam.retention_amout);// 减去用户设置的保留余额后的用户可用余额
											double setAmount = userParam.amount;// 用户设置的每次投标金额
											double loan_schedule = Double
													.parseDouble(map.get(
															"loan_schedule")
															.toString());

											int bidAmount = Invest
													.calculateBidAmount(
															setAmount,
															loan_schedule,
															amount,
															has_invested_amount);// 计算出投标金额

											if (balance < bidAmount) {// 用户余额不足
												Invest.updateUserAutoBidTime(userId);// 排到队尾
											} else {

												invest(userId, bidId,
														bidAmount, "", true,
														false, null, error);
												Invest.addAutoBidRecord(userId,
														bidId);// 添加自动投标记录
												Invest.updateUserAutoBidTime(userId);// 排到队尾
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 前台显示的机构借款标
	 * 
	 * @return
	 */
	public static List<v_front_all_bids> queryAgencyBids() {

		List<v_front_all_bids> agencyBids = null;

		try {
			agencyBids = GenericModel.find(" is_agency = 1 ").fetch(
					Constants.HOME_BID_COUNT);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return agencyBids;
	}

	/**
	 * 前台显示的普通借款标 / 按时间排序
	 * 
	 * @return
	 */
	public static List<v_front_all_bids> queryBids() {

		List<v_front_all_bids> bids = null;

		try {
			bids = GenericModel.find(" is_agency = 0 and status in(?, ?)",
					Constants.BID_ADVANCE_LOAN, Constants.BID_FUNDRAISE).fetch(
					Constants.HOME_BID_COUNT);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return bids;
	}

	/**
	 * 
	 * 增加的车贷
	 * **/

	public static List<v_front_all_bids> carBids() {

		List<v_front_all_bids> bids = null;
		try {
			bids = GenericModel.find("product_id = 7").fetch(2);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return bids;
	}

	/**
	 * 
	 * 增加的房贷
	 * **/
	public static List<v_front_all_bids> roomBids() {

		List<v_front_all_bids> bids = null;

		try {
			bids = GenericModel.find("product_id = 8").fetch(1);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return bids;
	}

	/**
	 * 
	 * 增加的红木抵押标
	 * **/
	public static List<v_front_all_bids> mahoganyBids() {

		List<v_front_all_bids> bids = null;

		try {
			bids = GenericModel.find("product_id = 10").fetch(1);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return bids;
	}

	/**
	 * 投资记录
	 * 
	 * @param pageBean
	 *            分页对象
	 * @param bidId
	 *            标ID
	 */
	public static List<v_invest_records> bidInvestRecord(
			PageBean<v_invest_records> pageBean, long bidId, ErrorInfo error) {
		error.clear();

		int count = -1;

		try {
			count = (int) GenericModel.count("bid_id = ?", bidId);
		} catch (Exception e) {
			Logger.error("理财->标投资记录,查询总记录数:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载投资记录失败!";

			return null;
		}

		if (count < 1)
			return new ArrayList<v_invest_records>();

		pageBean.totalCount = count;

		try {
			return GenericModel.find("bid_id = ? ", bidId).fetch(
					pageBean.currPage, pageBean.pageSize);
		} catch (Exception e) {
			Logger.error("理财->标投资记录:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载投资记录失败!";

			return null;
		}
	}

	/**
	 * 理财风云榜
	 * 
	 * @return
	 */
	public static List<v_bill_board> investBillboard() {

		List<v_bill_board> investBillboard = new ArrayList<v_bill_board>();

		try {
			investBillboard = GenericModel.find("").fetch(5);
		} catch (Exception e) {
			e.printStackTrace();

			return investBillboard;
		}

		return investBillboard;
	}

	/**
	 * 理财风云榜(更多)
	 * 
	 * @return
	 */
	public static PageBean<v_bill_board> investBillboards(int currPage,
			ErrorInfo error) {
		error.clear();

		if (currPage < 1) {
			currPage = 1;
		}

		if (currPage > 5) {
			currPage = 5;
		}

		List<v_bill_board> investBillboard = new ArrayList<v_bill_board>();

		int count = 0;

		try {
			count = (int) GenericModel.count();
			investBillboard = GenericModel.find("").fetch(currPage, 10);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询Top50投资金额排行时：" + e.getMessage());

			error.code = 0;
			error.msg = "查询Top50投资金额排行失败";

			return null;
		}

		count = count > 50 ? 50 : count;

		PageBean<v_bill_board> page = new PageBean<v_bill_board>();

		page.pageSize = 10;
		page.currPage = currPage;
		page.totalCount = count;

		page.page = investBillboard;

		error.code = 0;

		return page;
	}

	/**
	 * 根据标产品资料ID查出用户提交的对应资料
	 * 
	 * @param itemId
	 * @return
	 */
	public static UserAuditItem getAuditItem(long itemId, long userId) {

		String hql = "select audit_item_id from t_product_audit_items where id = ?";
		String sql = "select id from t_user_audit_items where user_id = ? and audit_item_id = ?";
		Long userItemId = 0l;
		Long productItemId = 0l;

		try {
			productItemId = GenericModel.find(hql, itemId).first();
			userItemId = GenericModel.find(sql, userId, productItemId)
					.first();
		} catch (Exception e) {
			e.printStackTrace();
		}

		UserAuditItem item = new UserAuditItem();
		if (null != userItemId) {
			item.id = userItemId;
		}
		return item;
	}

	/**
	 * 根据投资ID查询账单
	 * 
	 * @param investId
	 * @param error
	 * @return
	 */
	public static Long queryBillByInvestId(long investId, ErrorInfo error) {

		Long billId = 0l;

		try {
			billId = GenericModel.find(
					"select id from t_bill_invests where invest_id = ? ",
					investId).first();
		} catch (Exception e) {
			e.printStackTrace();
		}

		error.code = 1;
		return billId;
	}

	/**
	 * ajax分页查询债权竞拍记录
	 * 
	 * @param debtId
	 */
	public static PageBean<v_debt_auction_records> viewAuctionRecords(
			int pageNum, int pageSize, long debtId, ErrorInfo error) {

		PageBean<v_debt_auction_records> page = new PageBean<v_debt_auction_records>();

		int currPage = pageNum;

		page.currPage = currPage;
		page.pageSize = pageSize;
		page.totalCount = (int) GenericModel.count(" transfer_id=?",
				debtId);

		List<v_debt_auction_records> list = new ArrayList<v_debt_auction_records>();

		try {

			list = GenericModel.find(" transfer_id=?", debtId).fetch(
					currPage, page.pageSize);
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
	 * 获取理财首页所有借款标数目
	 * 
	 * @param error
	 * @return
	 */
	public static Long getBidCount(ErrorInfo error) {

		Long count = 0l;

		try {
			count = GenericModel.count();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (null == count) {
			count = 0l;
		}

		error.code = 1;
		return count;
	}

	/**
	 * 取消债权关注
	 * 
	 * @param debtId
	 * @param error
	 */
	public static void canaleBid(long attentionId, ErrorInfo error) {

		t_user_attention_bids attentionBid = null;

		try {
			attentionBid = GenericModel.findById(attentionId);
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			error.msg = "查询关注的借款标异常";

			return;
		}

		if (null != attentionBid) {
			attentionBid.delete();
			error.code = 1;
			error.msg = "取消关注借款标成功";

			return;
		}

		error.code = -2;
		error.msg = "查询关注的借款标异常";

		return;
	}

	/**
	 * 根据标的id和用户的id查找用户投资金额，用户合同
	 * ***/

	public static List<t_invests> queryLoanAgreeAmount(long bidId, long userId,
			ErrorInfo error) {

		error.clear();

		List<t_invests> investDetails = new ArrayList<t_invests>();

		try {
			investDetails = GenericModel.find("user_id=? and bid_id =?", userId,
					bidId).fetch();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询我的理财账单收款情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询我的理财账单收款情况失败";

			return null;
		}

		// System.out.print(investDetails);
		return investDetails;
	}
		

}
