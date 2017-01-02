package business;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.apache.commons.lang.StringUtils;
import constants.Constants;
import constants.DealType;
import constants.IPSConstants.IPSOperation;
import constants.SupervisorEvent;
import constants.Templets;
import constants.UserEvent;
import play.Logger;
import play.cache.Cache;
import play.db.helper.JpaHelper;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import utils.Arith;
import utils.DateUtil;
import utils.ErrorInfo;
import utils.NumberUtil;
import utils.PageBean;
import models.t_bids;
import models.t_bill_invests;
import models.t_bills;
import models.t_products;
import models.t_system_options;
import models.t_users;
import models.v_bill_department_haspayed;
import models.v_bill_department_month_maturity;
import models.v_bill_department_overdue;
import models.v_bill_detail;
import models.v_bill_detail_for_collection;
import models.v_bill_detail_for_mark_overdue;
import models.v_bill_has_received;
import models.v_bill_haspayed;
import models.v_bill_invests_overdue_unpaid;
import models.v_bill_invests_paid;
import models.v_bill_invests_payables_statistics;
import models.v_bill_invests_pending_payment;
import models.v_bill_invests_principal_advances;
import models.v_bill_loan;
import models.v_bill_month_maturity;
import models.v_bill_overdue;
import models.v_bill_receiving;
import models.v_bill_receiving_overdue;
import models.v_bill_recently_pending;
import models.v_bill_receviable_statistical;
import models.v_user_detail_credit_score_normal_repayment;
import models.v_user_detail_credit_score_overdue;
import models.v_bill_repayment_record;

/**
 * 账单
 * 
 * @author zhs
 * @version 6.0
 * @created 2014年3月21日 下午2:19:20
 */

public class Bill implements Serializable {
	public long id;
	private long _id;

	public long bidId;
	public String title;
	public Date repaymentTime;
	public double repaymentCorpus;
	public double repaymentInterest;
	public int status;
	public int periods;
	public Date realRepaymentTime;
	public double realRepaymentCorpus;
	public double realRepaymentInterest;
	public int overdueMark;
	public Date markOverdueTime;
	public double overdueFine;
	public Date markBadTime;
	public int noticeCountMessage;
	public int noticeCountMail;
	public int noticeCountTelphone;

	public static Date date = new Date();
	public User user;
	public Bid bid;

	public boolean isRepair;// 是否补单

	public void setId(long id) {
		ErrorInfo error = new ErrorInfo();
		t_bills bills = null;
		try {
			bills = GenericModel.findById(id);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询账单详情时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，导致账单详情失败！";
		}

		if (bills.id < 0 || bills == null) {
			this._id = -1;
			return;
		}

		this._id = bills.id;
		this.bidId = bills.bid_id;
		this.title = bills.title;
		this.repaymentTime = bills.repayment_time;
		this.repaymentCorpus = bills.repayment_corpus;
		this.repaymentInterest = bills.repayment_interest;
		this.status = bills.status;
		this.periods = bills.periods;
		this.realRepaymentTime = bills.real_repayment_time;
		this.realRepaymentCorpus = bills.real_repayment_corpus;
		this.realRepaymentInterest = bills.real_repayment_interest;
		this.overdueMark = bills.overdue_mark;
		this.markOverdueTime = bills.mark_overdue_time;
		this.overdueFine = bills.overdue_fine;
		this.markBadTime = bills.mark_bad_time;
		this.noticeCountMessage = bills.notice_count_message;
		this.noticeCountMail = bills.notice_count_mail;
		this.noticeCountTelphone = bills.notice_count_telphone;

		bid = new Bid();
		bid.id = bills.bid_id;
	}

	public long getiId() {
		return _id;
	}

	/**
	 * 日期累加
	 * 
	 * @param date
	 * @param type
	 * @param value
	 * @return
	 */
	public static Date add(Date date, int type, int value) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(type, value);

		return calendar.getTime();
	}

	/**
	 * 查询投资管理费率和逾期费率
	 * 
	 * @param info
	 * @return
	 */
	public static Map<String, Object> checkManagerate(ErrorInfo error) {
		error.clear();
		List<t_system_options> options = null;

		double investmentRates;
		double overdueRates;

		String sql = "_key = ? or _key = ? order by id";
		try {
			options = GenericModel.find(sql, "investment_fee",
					"overdue_fee").fetch();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询投资管理费率时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，导致查询查询投资管理费率失败！";

			return null;
		}
		investmentRates = Double.parseDouble(options.get(0)._value);
		overdueRates = Double.parseDouble(options.get(1)._value);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("investmentRates", investmentRates);
		map.put("overdueRates", overdueRates);

		return map;
	}

	/**
	 * 保存账单
	 * 
	 * @param obj
	 */
	public int addBill(Bid bid, ErrorInfo error) {
		error.clear();

		// 账单时间错误 后期加上的；
		Date dates = new Date();

		double monthRate = 0;// 月利率
		double monPay = 0;// 每个月要还的金额
		double amount = 0;
		double monPayInterest = 0;// 每个月利息（如果是天标的话，就是所有的利息）
		double monPayAmount = 0;// 每个月本金
		double totalAmount = 0;// 总共要还的金额
		double payRemain = 0;// 剩余要还的金额
		double payAmount = 0;// 加起来付了多少钱
		double totalInterest = 0;// 总利息

		t_bills bills;
		int deadline = bid.period; // 借款标期限
		double borrowSum = bid.amount; // 借款金额
		Integer period_unit = bid.periodUnit;// 借款期限
		monthRate = Double.valueOf(bid.apr * 0.01) / 12.0;// 通过年利率得到月利率

		// 秒还还款
		if (bid.isSecBid) {
			bills = new t_bills();
			monPayInterest = Bid.getInterest(bid.period, bid.periodUnit,
					bid.apr, borrowSum);// 要还的利息

			bills.bid_id = bid.id;
			bills.title = bid.title;
			bills.periods = 1;
			bills.repayment_time = add(dates, Calendar.MONTH, 0);
			bills.repayment_corpus = borrowSum;
			bills.repayment_interest = monPayInterest;
			bills.status = Constants.NO_REPAYMENT;

			try {
				bills.save();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("保存投资账单时：" + e.getMessage());
				error.code = -5;
				error.msg = "数据库异常，导致保存投资账单失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			this.id = bills.id;

			this.addInvestBills(bid.id, bid.repayment.id, bid.userId,
					bid.isSecBid, error);

			return error.code;
		}

		// 按月还款、等额本息
		if (bid.repayment.id == Constants.PAID_MONTH_EQUAL_PRINCIPAL_INTEREST) {

			if (period_unit == -1 || period_unit == 0) {// 判断标类(年标，月标，天标)

				if (period_unit == -1) {// 如果为年标，那么传过来的借款期限都乘以12
					deadline = deadline * 12;
				}

				monPay = Double.valueOf(Arith.mul(borrowSum, monthRate)
						* Math.pow((1 + monthRate), deadline))
						/ Double.valueOf(Math.pow((1 + monthRate), deadline) - 1);// 每个月要还的本金和利息
				monPay = Arith.round(monPay, 2);
				amount = borrowSum;
				totalAmount = Arith.mul(monPay, deadline);// 总共要还的金额
				payRemain = Arith.round(totalAmount, 2);

				for (int n = 1; n <= deadline; n++) {
					bills = new t_bills();
					monPayInterest = Arith.round(Arith.mul(amount, monthRate),
							2);// 每个月利息
					monPayAmount = Arith.round(
							Arith.sub(monPay, monPayInterest), 2);// 每个月本金
					amount = Arith.round(Arith.sub(amount, monPayAmount), 2);

					if (n == deadline) {
						monPay = payRemain;
						monPayAmount = borrowSum - payAmount;
						monPayInterest = monPay - monPayAmount;
					}
					payAmount += monPayAmount;
					payRemain = Arith.sub(payRemain, monPay);

					bills.bid_id = bid.id;
					bills.title = bid.title;
					bills.periods = n;
					bills.repayment_time = add(dates, Calendar.MONTH, n);
					bills.repayment_corpus = monPayAmount;
					bills.repayment_interest = monPayInterest;
					bills.status = Constants.NO_REPAYMENT;

					try {
						bills.save();
					} catch (Exception e) {
						e.printStackTrace();
						Logger.info("保存投资账单时：" + e.getMessage());
						error.code = -2;
						error.msg = "数据库异常，导致保存投资账单失败";
						JPA.setRollbackOnly();

						return error.code;
					}
				}

				this.addInvestBills(bid.id, bid.repayment.id, bid.userId,
						bid.isSecBid, error);// 生成投资账单

				return error.code;

			} else {

				this.addDayBills(bid.title, deadline, borrowSum, monthRate,
						bid.id, bid.repayment.id, bid.userId, bid.isSecBid,
						error);// 生成天标借款账单和投资账单

				return error.code;
			}
		}

		// 按月付息、一次还款
		if (bid.repayment.id == Constants.PAID_MONTH_ONCE_REPAYMENT) {
			monPayInterest = Arith.mul(borrowSum, monthRate);

			if (period_unit == -1 || period_unit == 0) {

				if (period_unit == -1) {
					deadline = deadline * 12;
				}
				for (int n = 1; n <= deadline; n++) {
					bills = new t_bills();
					if (n == deadline) {
						monPayAmount = borrowSum;
					} else {
						monPayAmount = 0.00;
					}

					bills.bid_id = bid.id;
					bills.title = bid.title;
					bills.periods = n;
					bills.repayment_time = add(dates, Calendar.MONTH, n);
					bills.repayment_corpus = monPayAmount;
					bills.repayment_interest = monPayInterest;
					bills.status = Constants.NO_REPAYMENT;

					try {
						bills.save();
					} catch (Exception e) {
						e.printStackTrace();
						Logger.info("保存投资账单时：" + e.getMessage());
						error.code = -3;
						error.msg = "数据库异常，导致保存投资账单失败";
						JPA.setRollbackOnly();

						return error.code;
					}
				}

				this.addInvestBills(bid.id, bid.repayment.id, bid.userId,
						bid.isSecBid, error);// 生成投资账单

				return error.code;

			} else {

				this.addDayBills(bid.title, deadline, borrowSum, monthRate,
						bid.id, bid.repayment.id, bid.userId, bid.isSecBid,
						error);// 生成天标借款账单和投资账单

				return error.code;
			}

		}

		// 一次还款
		if (bid.repayment.id == Constants.ONCE_REPAYMENT) {
			if (period_unit == -1 || period_unit == 0) {
				if (period_unit == -1) {
					deadline = deadline * 12;
				}

				bills = new t_bills();
				monPayInterest = Arith.mul(borrowSum, monthRate);
				totalInterest = monPayInterest * deadline;
				totalAmount = borrowSum + totalInterest;

				bills.bid_id = bid.id;
				bills.title = bid.title;
				bills.periods = 1;
				bills.repayment_time = add(dates, Calendar.MONTH, 1);
				bills.repayment_corpus = borrowSum;
				bills.repayment_interest = totalInterest;
				bills.status = Constants.NO_REPAYMENT;

				try {
					bills.save();

				} catch (Exception e) {
					e.printStackTrace();
					Logger.info("保存投资账单时：" + e.getMessage());
					error.code = -4;
					error.msg = "数据库异常，导致保存投资账单失败";
					JPA.setRollbackOnly();

					return error.code;
				}

				this.addInvestBills(bid.id, bid.repayment.id, bid.userId,
						bid.isSecBid, error);// 生成投资账单

				return error.code;

			} else {

				this.addDayBills(bid.title, deadline, borrowSum, monthRate,
						bid.id, bid.repayment.id, bid.userId, bid.isSecBid,
						error);// 生成天标借款账单和投资账单

				return error.code;
			}
		}

		return error.code;
	}

	/**
	 * 生成天标借款账单
	 * 
	 * @param deadline
	 *            借款天标的天数
	 * @param borrowSum
	 *            借款的总额
	 * @param monthRate
	 *            月利率
	 * @param bidId
	 *            标id
	 */
	public int addDayBills(String title, int deadline, double borrowSum,
			double monthRate, long bidId, long repaymentId, long userId,
			boolean isSecBid, ErrorInfo error) {
		error.clear();

		// 账单时间错误 后期加上的；
		Date dates = new Date();

		t_bills bills = new t_bills();
		double monPayInterest = Arith.div(
				Arith.mul(Arith.mul(borrowSum, monthRate), deadline), 30, 2);// 天标的总利息

		// 1.生成借款账单
		bills.bid_id = bidId;
		bills.title = title;
		bills.periods = 1;
		bills.repayment_time = add(dates, Calendar.DAY_OF_MONTH, deadline);
		Logger.info("now date()==" + dates);
		Logger.info("deadline==" + deadline);
		Logger.info("payTime==" + add(dates, Calendar.DAY_OF_MONTH, deadline));
		bills.repayment_corpus = borrowSum;
		bills.repayment_interest = monPayInterest;
		bills.status = Constants.NO_REPAYMENT;

		try {
			bills.save();

		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("生成天标借款账单时：" + e.getMessage());
			error.code = -1;
			error.msg = "数据库异常，导致生成天标借款账单失败";
			JPA.setRollbackOnly();
		}

		this.addInvestBills(bidId, repaymentId, userId, isSecBid, error);// 生成投资账单

		error.code = 0;
		return error.code;
	}

	/**
	 * 生成投资账单
	 * 
	 * @param deadline
	 *            借款期限
	 * @param borrowSum
	 *            借款总额
	 * @param monPayAmount
	 *            月收金额
	 * @param monPayRate
	 *            月利息
	 * @param bidId
	 *            标id
	 */
	public int addInvestBills(long bidId, long repaymentId, long userId,
			boolean isSecBid, ErrorInfo error) {
		error.clear();
		EntityManager em = JPA.em();

		// 1.生成投资账单

		String sql = "insert into t_bill_invests(user_id,invest_id,bid_id,mer_bill_no,periods,title,receive_time,receive_corpus,receive_interest, "
				+ "status, overdue_fine, real_receive_corpus, real_receive_interest) SELECT a.user_id,a.id, a.bid_id,a.mer_bill_no,b.periods,b.title,b.repayment_time,((a.amount * b.repayment_corpus)/ c.amount),"
				+ "((a.amount * b.repayment_interest)/ c.amount), -1, 0.00, 0.00, 0.00 FROM t_bills AS b LEFT JOIN t_invests AS a ON a.bid_id "
				+ "= b.bid_id LEFT JOIN t_bids AS c ON a.bid_id = c.id AND b.bid_id = c.id WHERE b.bid_id IS NOT NULL AND b.status "
				+ "= -1 AND b.bid_id = ?";
		Query query = em.createNativeQuery(sql).setParameter(1, bidId);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("添加投资账单时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致添加投资账单失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (rows == 0) {
			error.code = -1;
			error.msg = "添加投资账单操作数据库没有改变";
			JPA.setRollbackOnly();

			return error.code;
		}

		// 2.初始化纠偏数据(投资记录表)
		String correctStartSql = "update t_invests t1, (select a.invest_id, a.bid_id, a.user_id, sum(a.receive_corpus) receive_corpus, sum(a.receive_interest)"
				+ " receive_interest from t_bill_invests a where a.bid_id = ? group by a.invest_id) t2 set t1.correct_amount = t2.receive_corpus ,"
				+ " t1.correct_interest = t2.receive_interest where t1.bid_id = t2.bid_id and t1.id = t2.invest_id";

		Query correctStart = em.createNativeQuery(correctStartSql)
				.setParameter(1, bidId);

		try {
			rows = correctStart.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("纠偏数据初始化时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致纠纠偏数据初始化失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		// 3.纠偏本金利息给第一个投资人(投资记录表)
		String correctCorIntSql = "update t_invests t1, (select t3.min_id, (t4.repayment_corpus - t3.collect_amount) check_amount, "
				+ "(t4.repayment_interest - t3.collect_interest) check_interest from (select min(a.id) min_id, a.bid_id, sum(a.correct_amount) "
				+ "collect_amount, sum(a.correct_interest) collect_interest from t_invests a where a.bid_id = ? group by a.bid_id) t3 left join "
				+ "(select b.bid_id, sum(b.repayment_corpus) repayment_corpus, sum(b.repayment_interest) repayment_interest from  t_bills b where "
				+ "b.bid_id = ? group by b.bid_id) t4 on t3.bid_id = t4.bid_id) t2 set t1.correct_amount = t1.correct_amount + t2.check_amount, "
				+ "t1.correct_interest = t1.correct_interest + t2.check_interest where t1.id = t2.min_id";

		Query correctCorInt = em.createNativeQuery(correctCorIntSql)
				.setParameter(1, bidId).setParameter(2, bidId);

		try {
			rows = correctCorInt.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("纠偏数据初始化时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致纠偏本金利息失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		// 4.核对纠偏本金(投资记录表)
		String checkCorrectSql = "update t_invests t1,(select a.id, a.user_id, a.bid_id, (a.amount - a.correct_amount) check_corpus from t_invests a "
				+ "where a.bid_id = ?) t2 set t1.correct_amount = t1.correct_amount + t2.check_corpus, t1.correct_interest = "
				+ "t1.correct_interest + t2.check_corpus where t1.bid_id = t2.bid_id and t1.user_id = t2.user_id and t2.id = t1.id";
		// update t_invests t1,(select a.user_id, a.bid_id, (a.amount -
		// a.correct_amount) check_corpus from t_invests a where a.bid_id = 41
		// group by a.user_id) t2 set t1.correct_amount = t1.correct_amount +
		// t2.check_corpus, t1.correct_interest = t1.correct_interest +
		// t2.check_corpus where t1.bid_id = t2.bid_id and t1.user_id =
		// t2.user_id

		Query checkCorrect = em.createNativeQuery(checkCorrectSql)
				.setParameter(1, bidId);

		try {
			rows = checkCorrect.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("纠偏数据初始化时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致纠偏本金失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		// 5.纠偏利息给第一个投资人(投资记录表)
		String correctIntSql = "update t_invests t1, (select t3.min_id, (t4.repayment_corpus - t3.collect_amount) check_amount, "
				+ "(t4.repayment_interest - t3.collect_interest) check_interest from (select min(a.id) min_id, a.bid_id, sum(a.correct_amount) "
				+ "collect_amount, sum(a.correct_interest) collect_interest from t_invests a where a.bid_id = ? group by a.bid_id) t3 left join "
				+ "(select b.bid_id, sum(b.repayment_corpus) repayment_corpus, sum(b.repayment_interest) repayment_interest from  t_bills b where "
				+ "b.bid_id = ? group by b.bid_id) t4 on t3.bid_id = t4.bid_id) t2 set t1.correct_amount = t1.correct_amount + t2.check_amount, "
				+ "t1.correct_interest = t1.correct_interest + t2.check_interest where t1.id = t2.min_id";

		Query correctInt = em.createNativeQuery(correctIntSql)
				.setParameter(1, bidId).setParameter(2, bidId);

		try {
			rows = correctInt.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("纠偏数据初始化时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致利息失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		// 6.纠偏投资应收款明细资金(理财账单表)
		String updateSql = "update t_bill_invests t1,(select c.minId, (a.repayment_corpus-b.recivedPrincipal) check_corpus,"
				+ "(a.repayment_interest-b.recivedInterest) check_interest from (select id, bid_id,periods,"
				+ "repayment_corpus,repayment_interest from t_bills where bid_id = ?) a left join (select a.id ,a.bid_id, a.periods, "
				+ "sum(a.receive_corpus) recivedPrincipal, sum(a.receive_interest) recivedInterest from t_bill_invests a where a.bid_id	= ? "
				+ "group by a.periods) b on a.bid_id = b.bid_id AND a.periods = b.periods  left join (select min(a.id) minId,a.bid_id,"
				+ "a.periods from t_bill_invests a where a.bid_id = ? group by a.periods) c on b.bid_id = c.bid_id AND a.periods = c.periods) "
				+ "t2 set t1.receive_corpus = t1.receive_corpus + t2.check_corpus, t1.receive_interest = t1.receive_interest + "
				+ "t2.check_interest where t1.id = t2.minId";

		Query update = em.createNativeQuery(updateSql).setParameter(1, bidId)
				.setParameter(2, bidId).setParameter(3, bidId);

		try {
			rows = update.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("纠偏投资应收款明细资金时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致纠偏投资应收款明细资金失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		// 7.纠偏待收本金和利息(理财账单表)
		String updateCorIntSql = "update t_bill_invests t1,(select t3.id, t3.min_id, t3.user_id, t3.bid_id, (t4.amount - t3.receive_corpus) check_corpus, "
				+ "(t4.correct_interest - t3.receive_interest) check_interest from (select a.id, min(a.id) as min_id, a.invest_id, a.bid_id, a.user_id, sum(a.receive_corpus) "
				+ "receive_corpus, sum(a.receive_interest) receive_interest from t_bill_invests a where a.bid_id = ? group by a.invest_id) "
				+ "t3 left join (select b.user_id, b.id, b.bid_id, b.amount, b.correct_interest from t_invests b where b.bid_id = ? group by"
				+ " b.id) t4 on t3.bid_id = t4.bid_id and t3.invest_id = t4.id) t2 set t1.receive_corpus = t1.receive_corpus + "
				+ "t2.check_corpus, t1.receive_interest = t1.receive_interest + t2.check_interest where t1.id = t2.min_id";

		Query updateCorInt = em.createNativeQuery(updateCorIntSql)
				.setParameter(1, bidId).setParameter(2, bidId);

		try {
			rows = updateCorInt.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("纠偏投资应收款明细资金时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致纠偏待收本金和利息失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (repaymentId == Constants.PAID_MONTH_ONCE_REPAYMENT) {
			// 8.先息后本账单
			String oprateSql = "update t_bill_invests t1,(select a.receive_corpus, a.id from t_bill_invests a left join t_bids b on a.bid_id = "
					+ "b.id where a.periods < b.period and a.bid_id = ? group by a.id) t2 set t1.receive_corpus = 0.00, t1.receive_interest = "
					+ "t1.receive_interest + t2.receive_corpus where t1.id = t2.id";

			Query oprateInvestBill = em.createNativeQuery(oprateSql)
					.setParameter(1, bidId);

			try {
				rows = oprateInvestBill.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("纠偏投资应收款明细资金时：" + e.getMessage());
				error.code = -3;
				error.msg = "数据库异常，导致纠偏待收本金和利息失败";
				JPA.setRollbackOnly();

				return error.code;
			}
		}

		// 秒还还款
		if (isSecBid) {
			this.repayment(userId, error);
		}

		error.code = 0;
		return error.code;
	}

	/**
	 * 根据userId查询用户可用余额
	 * 
	 * @param error
	 * @return
	 */
	public static double queryBalance(long userId, ErrorInfo error) {
		String sql2 = "select balance from t_users where id = ? ";
		Double balance;
		try {
			balance = GenericModel.find(sql2, userId).first();

		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询投资人金额时：" + e.getMessage());
			error.code = -2;
			error.msg = "数据库异常，导致查询投资人金额失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		return balance = null == balance ? 0 : balance.doubleValue();

	}

	/**
	 * 管理员放款理财账单
	 * 
	 * @param investId
	 * @param error
	 * @return
	 */
	public static int investForPayment(long investId, ErrorInfo error) {
		error.clear();

		EntityManager em = JPA.em();
		int rows = 0;
		Map<String, Object> investMap = new HashMap<String, Object>();
		Bill bill = new Bill();

		// 1.查询理财账单的相关信息
		String sql = "select new Map(status as status, invest_id as investId, user_id as user_id, receive_corpus as receive_corpus,"
				+ " receive_interest as receive_interest, overdue_fine as overdue_fine, periods as period, bid_id as bid_id)"
				+ " from t_bill_invests where id = ? and status in (?,?)";
		try {
			investMap = GenericModel.find(sql, investId, Constants.FOR_PAY,
					Constants.FOR_OVERDUE_PAY).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询理财账单信息时：" + e.getMessage());
			error.code = -1;
			error.msg = "数据出现异常，导致付款失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (null == investMap) {
			error.code = -1;
			error.msg = "数据出现异常，导致付款失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		long investUserId = (Long) investMap.get("user_id");// 投资人id
		long bidId = (Long) investMap.get("bid_id");// 标id
		long investsId = (Long) investMap.get("investId");// 投资记录Id
		int period = (Integer) investMap.get("period");// 期数
		int status = (Integer) investMap.get("status");// 收款状态
		double receiveCorpus = (Double) investMap.get("receive_corpus");// 投资本金
		double receiveInterest = (Double) investMap.get("receive_interest");// 投资利息
		double overDueFine = (Double) investMap.get("overdue_fine");// 逾期罚息

		double investmentRate = (Double) Bill.checkManagerate(error).get(
				"investmentRates");// 投资管理费率

		if (investmentRate != 0) {
			investmentRate = investmentRate / 100;
		}

		double manageFee = Arith.round(
				Arith.mul(receiveInterest, investmentRate), 2);// 投资管理费
		double amount = receiveCorpus + receiveInterest + overDueFine
				- manageFee;// 用户总共获得的金额

		if (status != Constants.FOR_PAY && status != Constants.FOR_OVERDUE_PAY) {
			error.code = -1;
			error.msg = "该理财账单已付款";

			return error.code;
		}

		// 4.改变投资账单的收款状态
		// 逾期付款
		if (status == Constants.FOR_OVERDUE_PAY) {
			String overdueSql = "update t_bill_invests set status = ?, real_receive_time = ?, real_receive_corpus = ?, "
					+ "real_receive_interest = ?, overdue_fine = ? where id =? and status = ? and invest_id = ?";

			Query overdue = em.createQuery(overdueSql)
					.setParameter(1, Constants.OVERDUE_RECEIVABLES)
					.setParameter(2, DateUtil.currentDate())
					.setParameter(3, receiveCorpus)
					.setParameter(4, receiveInterest)
					.setParameter(5, overDueFine).setParameter(6, investId)
					.setParameter(7, Constants.FOR_OVERDUE_PAY)
					.setParameter(8, investsId);

			try {
				rows = overdue.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改理财账单收款情况时：" + e.getMessage());
				error.code = -2;
				error.msg = "数据出现异常，导致付款失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据出现异常，导致付款失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			// 投资人添加本金和利息
			String userBalanceSql = "update t_users set balance = balance + ? where id =?";

			Query userBalance = em.createQuery(userBalanceSql)
					.setParameter(1, receiveCorpus + receiveInterest)
					.setParameter(2, investUserId);

			try {
				rows = userBalance.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改用户可用金额时：" + e.getMessage());
				error.code = -2;
				error.msg = "数据出现异常，导致修改用户可用金额失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据出现异常，导致付款失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			Map<String, Double> investForDetail = DealDetail.queryUserFund(
					investUserId, error);
			if (error.code < 0 || investForDetail == null) {
				JPA.setRollbackOnly();

				return -1;
			}

			double investFreeze = investForDetail.get("freeze");
			double investReceiveAmount = investForDetail.get("receive_amount");

			Double investBalance10 = Bill.queryBalance(investUserId, error);

			DealDetail investDetail = new DealDetail(investUserId,
					DealType.OVER_RECEIVE, receiveCorpus + receiveInterest,
					investId, investBalance10, investFreeze,
					investReceiveAmount, "逾期收款获取第" + investId + "号账单投资金额");

			// 添加逾期收款的交易记录
			investDetail.addDealDetail(error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}

			// 投资人添加罚息
			String userBalanceSql2 = "update t_users set balance = balance + ? where id =?";

			Query userBalance2 = em.createQuery(userBalanceSql2)
					.setParameter(1, overDueFine).setParameter(2, investUserId);

			try {
				rows = userBalance2.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改用户可用金额时：" + e.getMessage());
				error.code = -2;
				error.msg = "数据出现异常，导致修改用户可用金额失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据出现异常，导致付款失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			Double investBalance3 = Bill.queryBalance(investUserId, error);

			DealDetail investOverdueFeeDetail = new DealDetail(investUserId,
					DealType.ADD_OVERDUE_FEE, overDueFine, investId,
					investBalance3, investFreeze, investReceiveAmount, "获取第"
							+ investId + "号账单逾期费");

			// 添加获取逾期费用的交易记录
			investOverdueFeeDetail.addDealDetail(error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}
			// 正常收款
		} else {
			String normalSql = "update t_bill_invests set status = ?, real_receive_time = ?, real_receive_corpus = ?, "
					+ "real_receive_interest = ?, overdue_fine = ? where id =? and status = ? and invest_id = ?";

			Query normal = em.createQuery(normalSql)
					.setParameter(1, Constants.NORMAL_RECEIVABLES)
					.setParameter(2, DateUtil.currentDate())
					.setParameter(3, receiveCorpus)
					.setParameter(4, receiveInterest).setParameter(5, 0.00)
					.setParameter(6, investId)
					.setParameter(7, Constants.FOR_PAY)
					.setParameter(8, investsId);

			try {
				rows = normal.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改理财账单收款情况时：" + e.getMessage());
				error.code = -2;
				error.msg = "数据出现异常，导致付款失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据出现异常，导致付款失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			// 投资人本金和利息
			String userBalanceSql2 = "update t_users set balance = balance + ? where id =?";

			Query userBalance2 = em.createQuery(userBalanceSql2)
					.setParameter(1, receiveCorpus + receiveInterest)
					.setParameter(2, investUserId);

			try {
				rows = userBalance2.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改用户可用金额时：" + e.getMessage());
				error.code = -2;
				error.msg = "数据出现异常，导致修改用户可用金额失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据出现异常，导致付款失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			Map<String, Double> investForDetail3 = DealDetail.queryUserFund(
					investUserId, error);
			if (error.code < 0 || investForDetail3 == null) {
				JPA.setRollbackOnly();

				return -1;
			}

			double investFreeze3 = investForDetail3.get("freeze");
			double investReceiveAmount3 = investForDetail3
					.get("receive_amount");

			Double investBalance9 = Bill.queryBalance(investUserId, error);

			DealDetail investDetail = new DealDetail(investUserId,
					DealType.NOMAL_RECEIVE, receiveCorpus + receiveInterest,
					investId, investBalance9, investFreeze3,
					investReceiveAmount3, "正常收款获取第" + investId + "号账单投资金额");

			// 添加收款的交易记录
			investDetail.addDealDetail(error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}
		}

		// 减去投资人管理费
		String userBalanceSql2 = "update t_users set balance = balance - ? where id =?";

		Query userBalance2 = em.createQuery(userBalanceSql2)
				.setParameter(1, manageFee).setParameter(2, investUserId);

		try {
			rows = userBalance2.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改用户可用金额时：" + e.getMessage());
			error.code = -2;
			error.msg = "数据出现异常，导致修改用户可用金额失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (rows == 0) {
			error.code = -1;
			error.msg = "数据出现异常，导致付款失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		Map<String, Double> investForDetail4 = DealDetail.queryUserFund(
				investUserId, error);
		if (error.code < 0 || investForDetail4 == null) {
			JPA.setRollbackOnly();

			return -1;
		}

		double investFreeze4 = investForDetail4.get("freeze");
		double investReceiveAmount4 = investForDetail4.get("receive_amount");

		Double investBalance2 = Bill.queryBalance(investUserId, error);

		DealDetail investFeeDetail = new DealDetail(investUserId,
				DealType.CHARGE_INVEST_FEE, manageFee, investId,
				investBalance2, investFreeze4, investReceiveAmount4, "扣除第"
						+ investId + "号账单理财管理费");

		// 添加扣除理财管理费的交易记录
		investFeeDetail.addDealDetail(error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		/* 添加CPS推广费 */
		User.rewardCPS(investUserId, manageFee, investId, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		// 添加平台扣除理财管理费交易记录
		DealDetail.addPlatformDetail(DealType.INVEST_FEE, investId,
				investUserId, -1, DealType.ACCOUNT, manageFee, 1, "平台收取第"
						+ investId + "号账单投资管理费", error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		// 如果投资用户账户资金没有遭到非法改动，那么就更新其篡改标志，否则不更新
		DataSafety dataTamperproof = new DataSafety();
		dataTamperproof.id = investUserId;
		dataTamperproof.updateSign(error);

		if (error.code < 0) {
			error.code = -1;
			error.msg = "数据出现异常，导致付款失败";
		}

		Map<String, Object> userMap = new HashMap<String, Object>();

		String userSql = "select new Map(name as name, email as eamil, mobile as mobile) from t_users where id = ? ";

		try {
			userMap = GenericModel.find(userSql, investUserId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据用户id查询时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户名失败";

			return error.code;
		}

		String bidSql = "select title from t_bids where id = ? ";
		String title = null;

		try {
			title = GenericModel.find(bidSql, bidId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据用户id查询时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户名失败";

			return error.code;
		}

		String userName = (String) userMap.get("name");
		String userEamil = (String) userMap.get("eamil");
		String userMobile = (String) userMap.get("mobile");

		// 发送站内信 尊敬的username:\n
		// 您投资的借款title,第repayPeriod期还款已经完成.<br/>paymentModeStr本期应得总额：
		// ￥recivedSum,其中本金部分为：hasP元,利息部分：hasI元,实得逾期罚息：hasLFI元<br/>扣除投资管理费：
		// ￥mFee元<br/>实得总额：￥msFee元
		TemplateStation station = new TemplateStation();
		station.id = Templets.M_INVEST_RECEIVE;

		if (station.status) {

			String mContent = station.content.replace("userName", userName);
			mContent = mContent.replace("title", title);
			mContent = mContent.replace("repayPeriod", period + "");
			mContent = mContent.replace("recivedSum", (receiveCorpus
					+ receiveInterest + overDueFine)
					+ "");
			mContent = mContent.replace("hasP", receiveCorpus + "");
			mContent = mContent.replace("hasI", receiveInterest + "");
			mContent = mContent.replace("hasLFI", overDueFine + "");
			mContent = mContent.replace("mFee", manageFee + "");
			mContent = mContent.replace("msFee", amount + "");
			TemplateStation.addMessageTask(investUserId, station.title,
					mContent);
		}

		// 发送邮件 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
		TemplateEmail email = new TemplateEmail();
		email.id = Templets.E_INVEST_RECEIVE;

		if (email.status) {
			String eContent = email.content.replace("userName", userName);
			eContent = eContent.replace("title", title);
			eContent = eContent.replace("repayPeriod", period + "");
			eContent = eContent.replace("recivedSum", (receiveCorpus
					+ receiveInterest + overDueFine)
					+ "");
			eContent = eContent.replace("hasP", receiveCorpus + "");
			eContent = eContent.replace("hasI", receiveInterest + "");
			eContent = eContent.replace("hasLFI", overDueFine + "");
			eContent = eContent.replace("mFee", manageFee + "");
			eContent = eContent.replace("msFee", amount + "");
			TemplateEmail.addEmailTask(userEamil, email.title, eContent);
		}

		// 发送短信 尊敬的username:\n
		// 您投标的借款title,第repayPeriod期还款已经完成.\npaymentModeStr本期应得总额:
		// ￥recivedSum,其中本金部分为:hasP元,利息部分:hasI元,实得逾期罚息:hasLFI元\n扣除投资管理费:
		// ￥mFee元\n实得总额:￥msFee元
		TemplateSms sms = new TemplateSms();
		if (StringUtils.isNotBlank(userMobile)) {
			sms.id = Templets.S_INVEST_RECEIVE;

			if (sms.status) {
				String sContent = sms.content.replace("userName", userName);
				sContent = sContent.replace("title", title);
				sContent = sContent.replace("repayPeriod", period + "");
				sContent = sContent.replace("recivedSum", (receiveCorpus
						+ receiveInterest + overDueFine)
						+ "");
				sContent = sContent.replace("hasP", receiveCorpus + "");
				sContent = sContent.replace("hasI", receiveInterest + "");
				sContent = sContent.replace("hasLFI", overDueFine + "");
				sContent = sContent.replace("mFee", manageFee + "");
				sContent = sContent.replace("msFee", amount + "");
				TemplateSms.addSmsTask(userMobile, sContent);
			}
		}

		error.code = 0;
		error.msg = "付款成功";

		return error.code;
	}

	/**
	 * 还款
	 * 
	 * @param obj
	 * @return
	 * @throws ParseException
	 */
	public Map<String, List<Map<String, Object>>> repayment(long userId,
			ErrorInfo error) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> mapp = new HashMap<String, Object>();
		error.clear();

		double repaymentCorpus = 0;// 借款账单本期要付的本金
		double repaymentInterest = 0;// 借款账单本期要付的利息
		double repayOverdueFine = 0;
		int mark = 0;// 是否逾期的标记
		int status = 0;// 账单的还款状态

		double balance = queryBalance(userId, error);// 借款用户可用余额
		// double investmentRate =
		// (Double)this.checkManagerate(error).get("investmentRates");//投资管理费率

		// if(investmentRate != 0){
		// investmentRate = investmentRate / 100;
		// }

		// 1.查询本期借款账单的账单数据
		String sql = "select new Map(user.ips_acct_no as ips_acct_no,bid.bid_no as bid_no,bill.overdue_mark as overdue_mark, bill.repayment_corpus as "
				+ "repayment_corpus, bill.repayment_interest as repayment_interest, bill.overdue_fine as overdue_fine,"
				+ " bill.status as status, bill.periods as period) from t_bills as bill,t_bids as bid, t_users as user where bill.bid_id = bid.id and bid.user_id = user.id and bill.id = ?";
		try {
			mapp = GenericModel.find(sql, this._id).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询借款账单信息时：" + e.getMessage());
			error.code = -1;
			error.msg = "数据库异常，导致查询借款账单信息失败";
			JPA.setRollbackOnly();

			return null;
		}

		mark = (Integer) mapp.get("overdue_mark");
		repaymentCorpus = (Double) mapp.get("repayment_corpus");
		repaymentInterest = (Double) mapp.get("repayment_interest");
		status = (Integer) mapp.get("status");
		repayOverdueFine = (Double) mapp.get("overdue_fine");
		int period = (Integer) mapp.get("period");

		// 2.从投资记录表查询有哪些用户投资了这个借款标
		String sql3 = "select new Map(invest.id as id, invest.invest_id as investId, user.ips_acct_no as ipsAcctNo, invest.mer_bill_no as merBillNo, invest.receive_corpus as receive_corpus, invest.receive_interest "
				+ "as receive_interest, invest.user_id as user_id, invest.overdue_fine as overdue_fine) "
				+ "from t_bill_invests as invest, t_users as user where invest.user_id = user.id and invest.bid_id = ? and invest.periods = ? and invest.status not in (?,?,?,?)";
		try {
			list = GenericModel.find(sql3, this.bidId, this.periods,
					Constants.FOR_DEBT_MARK, Constants.NORMAL_RECEIVABLES,
					Constants.ADVANCE_PRINCIIPAL_RECEIVABLES,
					Constants.OVERDUE_RECEIVABLES).fetch();

		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询投资账单信息时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致查询投资账单信息失败";
			JPA.setRollbackOnly();

			return null;
		}

		Map<String, List<Map<String, Object>>> mapList = null;
		String paymentCacheNo = Payment.createBillNo(userId,
				IPSOperation.REPAYMENT_NEW_TRADE);

		if (Constants.IPS_ENABLE && !this.isRepair) {
			mapList = new HashMap<String, List<Map<String, Object>>>();
			List<Map<String, Object>> mappList = new ArrayList<Map<String, Object>>();
			Map<String, Object> perBillNo = new HashMap<String, Object>();
			perBillNo.put("bidRepayment", paymentCacheNo);
			// perBillNo.put("investRepayment", Payment.createBillNo(userId,
			// IPSOperation.REPAYMENT_NEW_TRADE)+"1");
			perBillNo.put("userId", userId);
			perBillNo.put("bidId", this.bidId);
			perBillNo.put("repaymentCorpus", repaymentCorpus);
			perBillNo.put("repaymentInterest", repaymentInterest);
			perBillNo.put("balance", balance);
			perBillNo.put("period", period);
			perBillNo.put("mark", mark);
			perBillNo.put("status", status);
			perBillNo.put("repayOverdueFine", repayOverdueFine);
			perBillNo.put("billId", this._id);

			mappList.add(mapp);
			mappList.add(perBillNo);

			mapList.put("bidRepayment", mappList);
			mapList.put("investRepayment", list);
		}

		if (mark == Constants.BILL_NO_OVERDUE) {// 如果未标记逾期，则判断为正常还款

			double payment = Arith.add(repaymentCorpus, repaymentInterest);
			double lastBalance = Arith.sub(balance, payment);

			// 判断用户账户是否被恶意篡改
			DataSafety dataTamperproof = new DataSafety();
			dataTamperproof.id = userId;
			boolean isChanged = dataTamperproof.signCheck(error);

			if (isChanged == false) {
				JPA.setRollbackOnly();
				return null;
			}

			// 1.判断借款人余额是否足够
			if (lastBalance < 0) {
				error.code = -1;
				error.msg = "余额不足，暂时不能还款，请及时充值";

				return null;
			}

			// 2.判断该借款是否已经还款，防止重复提交
			if (this.status == Constants.NORMAL_REPAYMENT
					|| this.status == Constants.OVERDUE_PATMENT) {
				error.code = -1;
				error.msg = "本期账单已还款";

				return null;
			}

			// 3.判断还款的借款标有没有债权在转让，如果有，将其状态回归原态,并解冻竞拍者资金
			int resulta = Debt.judgeHasBidTransfer(bidId);
			if (resulta < 0) {
				error.code = -7;
				error.msg = "还款出现异常，导致还款失败";

				return null;
			}

			if (null == list) {
				error.code = -4;
				error.msg = "还款出现异常，导致还款失败";

				return null;
			}

			if (Constants.IPS_ENABLE && !this.isRepair) {
				Cache.set("pepaymentCache_" + paymentCacheNo, mapList,
						Constants.CACHE_TIME);

				return mapList;
			}

			this.normalPayment(this.bidId, userId, repaymentCorpus,
					repaymentInterest, balance, list, period, error);

		} else {// 否则判断为逾期还款
			double payment = repaymentCorpus + repaymentInterest
					+ repayOverdueFine;// 总共要还款的金额
			double lastBalance = Arith.sub(balance, payment);

			// 判断用户账户是否被恶意篡改
			DataSafety dataTamperproof = new DataSafety();
			dataTamperproof.id = userId;
			boolean isChanged = dataTamperproof.signCheck(error);

			if (isChanged == false) {
				return null;
			}

			// 1.判断借款人余额是否足够
			if (lastBalance < 0) {
				error.code = -1;
				error.msg = "余额不足，暂时不能还款，请及时充值";

				return null;
			}

			// 2.判断该借款是否已经还款，防止重复提交
			if (this.status == Constants.NORMAL_REPAYMENT
					|| this.status == Constants.OVERDUE_PATMENT) {
				error.code = -1;
				error.msg = "本期账单已还款";

				return null;
			}

			// 3.判断还款的借款标有没有债权在转让，如果有，将其状态回归原态,并解冻竞拍者资金
			int resulta = Debt.judgeHasBidTransfer(bidId);
			if (resulta < 0) {
				error.code = -7;
				error.msg = "还款出现异常，导致还款失败";

				return null;
			}

			if (null == list) {
				error.code = -3;
				error.msg = "还款出现异常，导致还款失败";
				JPA.setRollbackOnly();

				return null;
			}

			if (Constants.IPS_ENABLE && !this.isRepair) {
				Cache.set("pepaymentCache_" + paymentCacheNo, mapList,
						Constants.CACHE_TIME);

				return mapList;
			}

			this.overduePayment(this.bidId, userId, repaymentCorpus,
					repaymentInterest, balance, list, status, repayOverdueFine,
					period, error);

		}

		error.code = 0;
		return null;
	}

	/**
	 * 正常还款
	 * 
	 * @param userId
	 * @param repaymentCorpus
	 * @param repaymentInterest
	 * @param balance
	 * @param managementRate
	 * @param list
	 * @param info
	 * @return
	 */
	public int normalPayment(long bidId, long userId, double repaymentCorpus,
			double repaymentInterest, double balance,
			List<Map<String, Object>> list, int period, ErrorInfo error) {
		error.clear();

		double managementRate = BackstageSet.getCurrentBackstageSet().investmentFee;

		if (managementRate != 0) {
			managementRate = managementRate / 100;
		}

		double payment = Arith.add(repaymentCorpus, repaymentInterest);
		double lastBalance = Arith.sub(balance, payment);
		EntityManager em = JPA.em();
		int rows = 0;

		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();

		// 判断系统应付账单设置（0为自动付款，1为手动付款）
		if (backstageSet.repayType.equalsIgnoreCase(Constants.AUTO_PAYMENT)
				|| Constants.IPS_ENABLE) {

			// 5.遍历本期的投资账单，计算并操作对应的还款金额到投资人账上
			for (Map<String, Object> map : list) {
				long investId = (Long) map.get("investId");// 投资id
				long investBillId = (Long) map.get("id");// 每个投资人的投资账单id
				long investUserId = (Long) map.get("user_id");// 投资人id
				double receiveCorpus = (Double) map.get("receive_corpus");// 投资本金
				double receiveInterest = (Double) map.get("receive_interest");// 投资利息

				// double investerBalance = this.queryBalance(investUserId,
				// error);//查询投资人可用余额
				double manageFee = Arith.round(
						Arith.mul(receiveInterest, managementRate), 2);// 投资管理费
				double receive = receiveCorpus + receiveInterest;// 计算投资人将获得的收益
				// double totalBalance = Arith.sub(Arith.add(investerBalance,
				// receive), manageFee);//计算用户的可用余额

				// 查看投资用户是否受到非法资金改动
				DataSafety investDataTamperproof = new DataSafety();
				investDataTamperproof.id = investUserId;
				boolean investIsChange = investDataTamperproof.signCheck(error);

				// 7. 修改投资账单的收款情况
				String updateSql = "update t_bill_invests set status = ?, real_receive_time = ?, real_receive_corpus = ?,"
						+ " real_receive_interest = ? where user_id = ? and bid_id = ? and periods = ? and status not in(?,?) and invest_id = ?";

				Query query = em.createQuery(updateSql)
						.setParameter(1, Constants.NORMAL_RECEIVABLES)
						.setParameter(2, DateUtil.currentDate())
						.setParameter(3, receiveCorpus)
						.setParameter(4, receiveInterest - manageFee)
						.setParameter(5, investUserId)
						.setParameter(6, this.bidId)
						.setParameter(7, this.periods)
						.setParameter(8, Constants.NORMAL_RECEIVABLES)
						.setParameter(9, Constants.FOR_DEBT_MARK)
						.setParameter(10, investId);

				try {
					rows = query.executeUpdate();
				} catch (Exception e) {
					e.printStackTrace();
					Logger.info("修改投资账单的收款情况时：" + e.getMessage());
					error.code = -6;
					error.msg = "还款出现异常，导致还款失败";
					JPA.setRollbackOnly();

					return error.code;
				}

				if (rows == 0) {
					error.code = -1;
					error.msg = "还款出现异常，导致还款失败";
					JPA.setRollbackOnly();

					return error.code;
				}

				// 6.投资人余额增加获取的投资本金和利息
				String balanceSql2 = "update t_users set balance = balance + ? where id = ?";

				Query BalanceUpdate2 = em.createQuery(balanceSql2)
						.setParameter(1, receiveCorpus + receiveInterest)
						.setParameter(2, investUserId);

				try {
					rows = BalanceUpdate2.executeUpdate();

				} catch (Exception e) {
					e.printStackTrace();
					Logger.info("返回每个投资用户每期获得的投资本金和利息时：" + e.getMessage());
					error.code = -5;
					error.msg = "数据库异常，导致还款失败";
					JPA.setRollbackOnly();

					return error.code;
				}

				if (rows == 0) {
					error.code = -1;
					error.msg = "还款出现异常，导致还款失败";
					JPA.setRollbackOnly();

					return error.code;
				}

				Map<String, Double> investForDetail = DealDetail.queryUserFund(
						investUserId, error);
				if (error.code < 0 || investForDetail == null) {
					JPA.setRollbackOnly();

					return -1;
				}

				double investFreeze = investForDetail.get("freeze");
				double investReceiveAmount = investForDetail
						.get("receive_amount");

				Double investerBalance10 = Bill.queryBalance(investUserId,
						error);

				DealDetail investDetail = new DealDetail(investUserId,
						DealType.NOMAL_RECEIVE, receive - manageFee,
						investBillId, investerBalance10, investFreeze,
						investReceiveAmount, "正常收款获取第" + this._id + "号账单投资金额");

				// 添加正常收款的交易记录
				investDetail.addDealDetail(error);
				if (error.code < 0) {
					JPA.setRollbackOnly();

					return -1;
				}

				// 6.投资人可用余额减去投资管理费
				String balanceSql = "update t_users set balance = balance - ? where id = ?";

				Query BalanceUpdate = em.createQuery(balanceSql)
						.setParameter(1, manageFee)
						.setParameter(2, investUserId);

				try {
					rows = BalanceUpdate.executeUpdate();

				} catch (Exception e) {
					e.printStackTrace();
					Logger.info("扣取投资管理费时：" + e.getMessage());
					error.code = -5;
					error.msg = "数据库异常，导致还款失败";
					JPA.setRollbackOnly();

					return error.code;
				}

				if (rows == 0) {
					error.code = -1;
					error.msg = "还款出现异常，导致还款失败";
					JPA.setRollbackOnly();

					return error.code;
				}

				// 本次查询用户可用余额是获取最新状态值，避免从缓存里拿数据
				Double investerBalance2 = Bill
						.queryBalance(investUserId, error);

				DealDetail investFeeDetail = new DealDetail(investUserId,
						DealType.CHARGE_INVEST_FEE, manageFee, investBillId,
						investerBalance2, investFreeze, investReceiveAmount,
						"扣除第" + this._id + "号账单理财管理费");

				// 添加扣除理财管理费的交易记录
				investFeeDetail.addDealDetail(error);
				if (error.code < 0) {
					JPA.setRollbackOnly();

					return -1;
				}

				/* 添加CPS推广费 */
				User.rewardCPS(investUserId, manageFee, this._id, error);

				if (error.code < 0) {
					JPA.setRollbackOnly();

					return -1;
				}

				Map<String, Object> userMap = new HashMap<String, Object>();

				String userSql = "select new Map(name as name, email as eamil, mobile as mobile) from t_users where id = ? ";

				try {
					userMap = GenericModel.find(userSql, investUserId).first();
				} catch (Exception e) {
					e.printStackTrace();
					Logger.info("根据用户id查询时：" + e.getMessage());
					error.code = -1;
					error.msg = "查询用户名失败";

					return error.code;
				}

				String userName = (String) userMap.get("name");
				String userEamil = (String) userMap.get("eamil");
				String userMobile = (String) userMap.get("mobile");

				// 发送站内信 尊敬的username:\n
				// 您投资的借款title,第repayPeriod期还款已经完成.<br/>paymentModeStr本期应得总额：
				// ￥recivedSum,其中本金部分为：hasP元,利息部分：hasI元,实得逾期罚息：hasLFI元<br/>扣除投资管理费：
				// ￥mFee元<br/>实得总额：￥msFee元
				TemplateStation station = new TemplateStation();
				station.id = Templets.M_INVEST_RECEIVE;

				if (station.status) {
					String mContent = station.content.replace("userName",
							userName);
					mContent = mContent.replace("title", title);
					mContent = mContent.replace("repayPeriod", this.periods
							+ "");
					mContent = mContent.replace("recivedSum",
							(receive - manageFee) + "");
					mContent = mContent.replace("hasP", receiveCorpus + "");
					mContent = mContent.replace("hasI", receiveInterest + "");
					mContent = mContent.replace("hasLFI", 0 + "");
					mContent = mContent.replace("mFee", manageFee + "");
					mContent = mContent.replace("msFee", (receive - manageFee)
							+ "");
					TemplateStation.addMessageTask(investUserId, station.title,
							mContent);
				}

				// 发送邮件 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum
				// 元
				TemplateEmail email = new TemplateEmail();
				email.id = Templets.E_INVEST_RECEIVE;

				if (email.status) {
					String eContent = email.content.replace("userName",
							userName);
					eContent = eContent.replace("title", title);
					eContent = eContent.replace("repayPeriod", this.periods
							+ "");
					eContent = eContent.replace("recivedSum",
							(receive - manageFee) + "");
					eContent = eContent.replace("hasP", receiveCorpus + "");
					eContent = eContent.replace("hasI", receiveInterest + "");
					eContent = eContent.replace("hasLFI", 0 + "");
					eContent = eContent.replace("mFee", manageFee + "");
					eContent = eContent.replace("msFee", (receive - manageFee)
							+ "");
					TemplateEmail.addEmailTask(userEamil, email.title, eContent);
				}

				// 发送短信 尊敬的username:\n
				// 您投标的借款title,第repayPeriod期还款已经完成.\npaymentModeStr本期应得总额:
				// ￥recivedSum,其中本金部分为:hasP元,利息部分:hasI元,实得逾期罚息:hasLFI元\n扣除投资管理费:
				// ￥mFee元\n实得总额:￥msFee元
				TemplateSms sms = new TemplateSms();
				if (StringUtils.isNotBlank(userMobile)) {
					sms.id = Templets.S_INVEST_RECEIVE;

					if (sms.status) {
						String sContent = sms.content.replace("userName",
								userName);
						sContent = sContent.replace("title", title);
						sContent = sContent.replace("repayPeriod", this.periods
								+ "");
						sContent = sContent.replace("recivedSum",
								(receive - manageFee) + "");
						sContent = sContent.replace("hasP", receiveCorpus + "");
						sContent = sContent.replace("hasI", receiveInterest
								+ "");
						sContent = sContent.replace("hasLFI", 0 + "");
						sContent = sContent.replace("mFee", manageFee + "");
						sContent = sContent.replace("msFee",
								(receive - manageFee) + "");
						TemplateSms.addSmsTask(userMobile, sContent);
					}
				}

				// 添加平台扣除理财管理费交易记录
				DealDetail.addPlatformDetail(DealType.INVEST_FEE, investBillId,
						investUserId, -1, DealType.ACCOUNT, manageFee, 1,
						"平台收取" + this._id + "投资管理费", error);
				if (error.code < 0) {
					JPA.setRollbackOnly();

					return -1;
				}

				// 如果投资用户账户资金没有遭到非法改动，那么就更新其篡改标志，否则不更新
				if (investIsChange) {
					investDataTamperproof.id = investUserId;
					investDataTamperproof.updateSign(error);
				}
			}
		}
		// 手动付款模式
		else {
			String updateInvestSql = "update t_bill_invests set status = ? where bid_id = ? and periods = ? and status not in(?,?,?,?,?)";

			Query updateInvest = em.createQuery(updateInvestSql)
					.setParameter(1, Constants.FOR_PAY).setParameter(2, bidId)
					.setParameter(3, period).setParameter(4, Constants.FOR_PAY)
					.setParameter(5, Constants.NORMAL_RECEIVABLES)
					.setParameter(6, Constants.ADVANCE_PRINCIIPAL_RECEIVABLES)
					.setParameter(7, Constants.OVERDUE_RECEIVABLES)
					.setParameter(8, Constants.FOR_DEBT_MARK);

			try {
				rows = updateInvest.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改投资账单状态时：" + e.getMessage());
				error.code = -3;
				error.msg = "数据库异常，导致修改投资账单状态失败";
				JPA.setRollbackOnly();

				return error.code;
			}
		}

		// 10.修改借款账单还款情况
		String updateBillSql = "update t_bills set status = ?, real_repayment_time = ?, real_repayment_corpus = ?, real_repayment_interest = ? where id = ? and status = ? ";

		Query updateBill = em.createQuery(updateBillSql)
				.setParameter(1, Constants.NORMAL_REPAYMENT)
				.setParameter(2, DateUtil.currentDate())
				.setParameter(3, repaymentCorpus)
				.setParameter(4, repaymentInterest).setParameter(5, this._id)
				.setParameter(6, Constants.NO_REPAYMENT);

		try {
			rows = updateBill.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改借款账单数据时：" + e.getMessage());
			error.code = -3;
			error.msg = "还款出现异常，导致还款失败";
			JPA.setRollbackOnly();

			return error.code;
		} finally {
			double Userbalance = queryBalance(userId, error);// 用户可用余额

			if (rows == 0) {
				error.code = -1;
				error.msg = "还款出现异常，导致还款失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			if (Userbalance < 0) {
				error.code = -1;
				error.msg = "资金不足,请充值!";
				JPA.setRollbackOnly();

				return error.code;
			}
		}

		// 扣除借款人还款本期的所需金额
		String userBalanceSql = "update t_users set balance = ? where id = ?";

		Query userBalance = em.createQuery(userBalanceSql)
				.setParameter(1, lastBalance).setParameter(2, userId);

		try {
			rows = userBalance.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改借款人账户金额时：" + e.getMessage());
			error.code = -2;
			error.msg = "还款出现异常，导致还款失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (rows == 0) {
			error.code = -1;
			error.msg = "还款出现异常，导致还款失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		// 9.针对借款账单做交易记录和事件
		Map<String, Double> billForDetail2 = DealDetail.queryUserFund(userId,
				error);
		if (error.code < 0 || billForDetail2 == null) {
			JPA.setRollbackOnly();

			return -1;
		}

		double userFreeze3 = billForDetail2.get("freeze");
		double userReceiveAmount3 = billForDetail2.get("receive_amount");

		Double investerBalance10 = Bill.queryBalance(userId, error);

		DealDetail detail = new DealDetail(userId, DealType.CHARGE_NOMAL_PAY,
				payment, this._id, investerBalance10, userFreeze3,
				userReceiveAmount3, "第" + this._id + "号账单正常还款扣除还款金额");

		// 添加正常还款的交易记录
		detail.addDealDetail(error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		// 正常还款获得信用积分
		int creditScore = DealDetail.addCreditScore(userId, 2, 0, this._id, "第"
				+ this._id + "号账单正常还款获得信用积分", error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		// 添加事件
		DealDetail.userEvent(userId, UserEvent.ADD_NOMAL_PAY, "第" + this._id
				+ "号账单用户正常还款", error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		Map<String, Object> userMap = new HashMap<String, Object>();

		String userSql = "select new Map(name as name, email as eamil, mobile as mobile) from t_users where id = ? ";

		try {
			userMap = GenericModel.find(userSql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据用户id查询时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户名失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		String userName = (String) userMap.get("name");
		String userEamil = (String) userMap.get("eamil");
		String userMobile = (String) userMap.get("mobile");

		// 发送站内信 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
		TemplateStation station = new TemplateStation();
		station.id = Templets.M_SUCCESS_PAY;

		if (station.status) {
			String mContent = station.content.replace("userName", userName);
			mContent = mContent.replace("date",
					DateUtil.dateToString(new Date()));
			mContent = mContent.replace("title", title);
			mContent = mContent.replace("needSum", payment + "");
			mContent = mContent.replace("creditScore", creditScore + "");
			TemplateStation.addMessageTask(userId, station.title, mContent);
		}

		// 发送邮件 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
		TemplateEmail email = new TemplateEmail();
		email.id = Templets.E_SUCCESS_PAY;

		if (email.status) {
			String eContent = email.content.replace("userName", userName);
			eContent = eContent.replace("date",
					DateUtil.dateToString(new Date()));
			eContent = eContent.replace("title", title);
			eContent = eContent.replace("needSum", payment + "");
			eContent = eContent.replace("creditScore", creditScore + "");
			TemplateEmail.addEmailTask(userEamil, email.title, eContent);
		}

		// 发送短信 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
		TemplateSms sms = new TemplateSms();
		if (StringUtils.isNotBlank(userMobile)) {
			sms.id = Templets.S_SUCCESS_PAY;

			if (sms.status) {
				String eContent = sms.content.replace("username", userName);
				eContent = eContent.replace("date",
						DateUtil.dateToString(new Date()));
				eContent = eContent.replace("title", title);
				eContent = eContent.replace("needSum", payment + "");
				eContent = eContent.replace("creditScore", creditScore + "");
				TemplateSms.addSmsTask(userMobile, eContent);
			}
		}

		// 11.判断这个借款标是否已还完款，若还完标记这个借款标为已还款完状态
		int count = this.isEndPayment(this.bidId, error);

		if (count == 0) {
			String bidSql = "update t_bids set status = ?, last_repay_time = ? where id = ? and status not in(?)";

			Query query = em.createQuery(bidSql)
					.setParameter(1, Constants.BID_REPAYMENTS)
					.setParameter(2, DateUtil.currentDate())
					.setParameter(3, bidId)
					.setParameter(4, Constants.BID_REPAYMENTS);

			try {
				rows = query.executeUpdate();
				;

			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改借款标为已还款完状态时：" + e.getMessage());
				error.code = -7;
				error.msg = "还款出现异常，导致还款失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "还款出现异常，导致还款失败";
				JPA.setRollbackOnly();

			}
		}

		// 12.更改当前用户在缓存变动的数据
		User user = User.currUser();

		if (user != null) {
			user.balance = lastBalance;
			user.creditScore = user.creditScore + creditScore;
			User.setCurrUser(user);
		}

		// 13.还款完毕后重新生成新的数据库防篡改标志
		DataSafety dataTamperproof = new DataSafety();
		dataTamperproof.id = userId;
		dataTamperproof.updateSign(error);

		error.msg = "还款成功";
		error.code = 0;
		return error.code;
	}

	/**
	 * 逾期还款
	 * 
	 * @param userId
	 * @param repaymentCorpus
	 * @param repaymentInterest
	 * @param balance
	 * @param managementRate
	 * @param list
	 * @param info
	 * @return
	 * @throws ParseException
	 */
	public int overduePayment(long bidId, long userId, double repaymentCorpus,
			double repaymentInterest, double balance,
			List<Map<String, Object>> investList, int status,
			double repayOverdueFine, int period, ErrorInfo error) {
		error.clear();

		double managementRate = BackstageSet.getCurrentBackstageSet().investmentFee;

		if (managementRate != 0) {
			managementRate = managementRate / 100;
		}

		double payment = repaymentCorpus + repaymentInterest + repayOverdueFine;// 总共要还款的金额
		double lastBalance = Arith.sub(balance, payment);
		EntityManager em = JPA.em();
		int rows = 0;

		// 5.非本金垫付还款的操作
		if (status == Constants.NO_REPAYMENT) {

			BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();

			// 判断系统应付账单设置（0为自动付款，1为手动付款）
			if (backstageSet.repayType.equalsIgnoreCase(Constants.AUTO_PAYMENT)) {

				// a.遍历本期的投资账单，计算并操作对应的还款金额到投资人账上
				for (Map<String, Object> map : investList) {
					long investId = (Long) map.get("investId");// 投资id
					long investBillId = (Long) map.get("id");// 每个投资人的投资账单id
					long investUserId = (Long) map.get("user_id");// 投资人id
					double receiveCorpus = (Double) map.get("receive_corpus");// 本期的投资本金
					double receiveInterest = (Double) map
							.get("receive_interest");// 本期的投资利息
					double investOverdueFine = (Double) map.get("overdue_fine");// 本期逾期罚款

					// double investerBalance = this.queryBalance(investUserId,
					// error);//查询投资人可用余额
					double manageFee = Arith.round(
							Arith.mul(receiveInterest, managementRate), 2);// 投资管理费

					double receive = receiveCorpus + receiveInterest
							+ investOverdueFine;// 计算投资人总共将获取的金额
					// double totalBalance =
					// Arith.sub(Arith.add(investerBalance, receive),
					// manageFee);//计算除去管理费后投资人可用余额还有多少

					// 查看投资用户是否受到非法资金改动
					DataSafety investDataTamperproof = new DataSafety();
					investDataTamperproof.id = investUserId;
					boolean investIsChange = investDataTamperproof
							.signCheck(error);

					// c.填写投资账单的收款情况
					String updateSql = "update t_bill_invests set status = ?, real_receive_time = ?, real_receive_corpus = ?,"
							+ " real_receive_interest = ?, overdue_fine = ? where user_id = ? and bid_id = ? and periods = ? and status not in(?,?) and invest_id = ?";

					Query query = em.createQuery(updateSql)
							.setParameter(1, Constants.OVERDUE_RECEIVABLES)
							.setParameter(2, DateUtil.currentDate())
							.setParameter(3, receiveCorpus)
							.setParameter(4, receiveInterest - manageFee)
							.setParameter(5, investOverdueFine)
							.setParameter(6, investUserId)
							.setParameter(7, this.bidId)
							.setParameter(8, this.periods)
							.setParameter(9, Constants.OVERDUE_RECEIVABLES)
							.setParameter(10, Constants.FOR_DEBT_MARK)
							.setParameter(11, investId);

					try {
						rows = query.executeUpdate();
					} catch (Exception e) {
						e.printStackTrace();
						Logger.info("填写投资账单的收款情况时：" + e.getMessage());
						error.code = -5;
						error.msg = "还款出现异常，导致还款失败";
						JPA.setRollbackOnly();

						return error.code;
					}

					if (rows == 0) {
						error.code = -1;
						error.msg = "还款出现异常，导致还款失败";
						JPA.setRollbackOnly();

						return error.code;

					}

					// b.重新计算每个投资用户获取本期还款(本金+利息)金额
					String balanceSql = "update t_users set balance = balance + ? where id =?";

					Query BalanceUpdate = em.createQuery(balanceSql)
							.setParameter(1, receiveCorpus + receiveInterest)
							.setParameter(2, investUserId);

					try {
						rows = BalanceUpdate.executeUpdate();

					} catch (Exception e) {
						e.printStackTrace();
						Logger.info("返回每个投资用户每期获得的投资本金和利息以及罚息时："
								+ e.getMessage());
						error.code = -5;
						error.msg = "还款出现异常，导致还款失败";
						JPA.setRollbackOnly();

						return error.code;
					}

					if (rows == 0) {
						error.code = -1;
						error.msg = "还款出现异常，导致还款失败";
						JPA.setRollbackOnly();

						return error.code;
					}

					// d.开始添加投资人交易记录和事件
					Map<String, Double> investForDetail = DealDetail
							.queryUserFund(investUserId, error);
					if (error.code < 0 || investForDetail == null) {
						JPA.setRollbackOnly();

						return -1;
					}

					double investFreeze = investForDetail.get("freeze");
					double investReceiveAmount = investForDetail
							.get("receive_amount");

					Double investBalance10 = Bill.queryBalance(investUserId,
							error);

					DealDetail investDetail = new DealDetail(investUserId,
							DealType.OVER_RECEIVE, receiveCorpus
									+ receiveInterest, investBillId,
							investBalance10, investFreeze, investReceiveAmount,
							"逾期收款获取第" + this._id + "号账单投资金额");

					// 添加逾期收款的交易记录
					investDetail.addDealDetail(error);
					if (error.code < 0) {
						JPA.setRollbackOnly();

						return -1;
					}

					// c.投资人减去管理费
					String balanceSql2 = "update t_users set balance = balance - ? where id =?";

					Query BalanceUpdate2 = em.createQuery(balanceSql2)
							.setParameter(1, manageFee)
							.setParameter(2, investUserId);

					try {
						rows = BalanceUpdate2.executeUpdate();

					} catch (Exception e) {
						e.printStackTrace();
						Logger.info("投资人减去管理费：" + e.getMessage());
						error.code = -5;
						error.msg = "还款出现异常，导致还款失败";
						JPA.setRollbackOnly();

						return error.code;
					}

					if (rows == 0) {
						error.code = -1;
						error.msg = "还款出现异常，导致还款失败";
						JPA.setRollbackOnly();

						return error.code;
					}

					Double investBalance2 = Bill.queryBalance(investUserId,
							error);

					DealDetail investFeeDetail = new DealDetail(investUserId,
							DealType.CHARGE_INVEST_FEE, manageFee,
							investBillId, investBalance2, investFreeze,
							investReceiveAmount, "扣除第" + this._id + "号账单理财管理费");

					// 添加扣除理财管理费的交易记录
					investFeeDetail.addDealDetail(error);
					if (error.code < 0) {
						JPA.setRollbackOnly();

						return -1;
					}

					/* 添加CPS推广费 */
					User.rewardCPS(investUserId, manageFee, this._id, error);

					if (error.code < 0) {
						JPA.setRollbackOnly();

						return -1;
					}

					// d.投资人添加逾期费
					String balanceSql3 = "update t_users set balance = balance + ? where id =?";

					Query BalanceUpdate3 = em.createQuery(balanceSql3)
							.setParameter(1, investOverdueFine)
							.setParameter(2, investUserId);

					try {
						rows = BalanceUpdate3.executeUpdate();

					} catch (Exception e) {
						e.printStackTrace();
						Logger.info("投资人添加逾期费：" + e.getMessage());
						error.code = -5;
						error.msg = "还款出现异常，导致还款失败";
						JPA.setRollbackOnly();

						return error.code;
					}

					if (rows == 0) {
						error.code = -1;
						error.msg = "还款出现异常，导致还款失败";
						JPA.setRollbackOnly();

						return error.code;
					}

					Double investBalance3 = Bill.queryBalance(investUserId,
							error);

					DealDetail investOverdueFeeDetail = new DealDetail(
							investUserId, DealType.ADD_OVERDUE_FEE,
							investOverdueFine, investBillId, investBalance3,
							investFreeze, investReceiveAmount, "获取第" + this._id
									+ "号账单逾期费");

					// 添加获取逾期费用的交易记录
					investOverdueFeeDetail.addDealDetail(error);
					if (error.code < 0) {
						JPA.setRollbackOnly();

						return -1;
					}

					// 添加平台扣除理财管理费交易记录
					DealDetail.addPlatformDetail(DealType.INVEST_FEE,
							investBillId, investUserId, -1, DealType.ACCOUNT,
							manageFee, 1, "平台收取第" + this._id + "号账单投资管理费",
							error);
					if (error.code < 0) {
						JPA.setRollbackOnly();

						return -1;
					}

					Map<String, Object> userMap = new HashMap<String, Object>();

					String userSql = "select new Map(name as name, email as eamil, mobile as mobile) from t_users where id = ? ";

					try {
						userMap = GenericModel.find(userSql, investUserId).first();
					} catch (Exception e) {
						e.printStackTrace();
						Logger.info("根据用户id查询时：" + e.getMessage());
						error.code = -1;
						error.msg = "查询用户名失败";
						JPA.setRollbackOnly();

						return error.code;
					}

					String userName = (String) userMap.get("name");
					String userEamil = (String) userMap.get("eamil");
					String userMobile = (String) userMap.get("mobile");

					// 发送站内信 尊敬的username:\n
					// 您投资的借款title,第repayPeriod期还款已经完成.<br/>paymentModeStr本期应得总额：
					// ￥recivedSum,其中本金部分为：hasP元,利息部分：hasI元,实得逾期罚息：hasLFI元<br/>扣除投资管理费：
					// ￥mFee元<br/>实得总额：￥msFee元
					TemplateStation station = new TemplateStation();
					station.id = Templets.M_INVEST_RECEIVE;

					if (station.status) {
						String mContent = station.content.replace("userName",
								userName);
						mContent = mContent.replace("title", title);
						mContent = mContent.replace("repayPeriod", this.periods
								+ "");
						mContent = mContent.replace("recivedSum", receive + "");
						mContent = mContent.replace("hasP", receiveCorpus + "");
						mContent = mContent.replace("hasI", receiveInterest
								+ "");
						mContent = mContent.replace("hasLFI", investOverdueFine
								+ "");
						mContent = mContent.replace("mFee", manageFee + "");
						mContent = mContent.replace("msFee", receive + "");
						TemplateStation.addMessageTask(investUserId,
								station.title, mContent);
					}

					// 发送邮件 尊敬的username:\n [date]
					// 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
					TemplateEmail email = new TemplateEmail();
					email.id = Templets.E_INVEST_RECEIVE;

					if (email.status) {
						String eContent = email.content.replace("userName",
								userName);
						eContent = eContent.replace("title", title);
						eContent = eContent.replace("repayPeriod", this.periods
								+ "");
						eContent = eContent.replace("recivedSum", receive + "");
						eContent = eContent.replace("hasP", receiveCorpus + "");
						eContent = eContent.replace("hasI", receiveInterest
								+ "");
						eContent = eContent.replace("hasLFI", investOverdueFine
								+ "");
						eContent = eContent.replace("mFee", manageFee + "");
						eContent = eContent.replace("msFee", receive + "");
						TemplateEmail.addEmailTask(userEamil, email.title, eContent);
					}

					// 发送短信 尊敬的username:\n
					// 您投标的借款title,第repayPeriod期还款已经完成.\npaymentModeStr本期应得总额:
					// ￥recivedSum,其中本金部分为:hasP元,利息部分:hasI元,实得逾期罚息:hasLFI元\n扣除投资管理费:
					// ￥mFee元\n实得总额:￥msFee元
					TemplateSms sms = new TemplateSms();
					if (StringUtils.isNotBlank(userMobile)) {
						sms.id = Templets.S_INVEST_RECEIVE;

						if (sms.status) {
							String sContent = sms.content.replace("userName",
									userName);
							sContent = sContent.replace("title", title);
							sContent = sContent.replace("repayPeriod",
									this.periods + "");
							sContent = sContent.replace("recivedSum", receive
									+ "");
							sContent = sContent.replace("hasP", receiveCorpus
									+ "");
							sContent = sContent.replace("hasI", receiveInterest
									+ "");
							sContent = sContent.replace("hasLFI",
									investOverdueFine + "");
							sContent = sContent.replace("mFee", manageFee + "");
							sContent = sContent.replace("msFee", receive + "");
							TemplateSms.addSmsTask(userMobile, sContent);
						}
					}

					// 如果投资用户账户资金没有遭到非法改动，那么就更新其篡改标志，否则不更新
					if (investIsChange) {
						investDataTamperproof.id = investUserId;
						investDataTamperproof.updateSign(error);
					}
				}
			}
			// 手动付款模式
			else {
				String updateInvestSql = "update t_bill_invests set status = ? where bid_id = ? and periods = ? and status not in(?,?,?,?,?,?)";

				Query updateInvest = em
						.createQuery(updateInvestSql)
						.setParameter(1, Constants.FOR_OVERDUE_PAY)
						.setParameter(2, bidId)
						.setParameter(3, period)
						.setParameter(4, Constants.FOR_PAY)
						.setParameter(5, Constants.FOR_OVERDUE_PAY)
						.setParameter(6, Constants.NORMAL_RECEIVABLES)
						.setParameter(7,
								Constants.ADVANCE_PRINCIIPAL_RECEIVABLES)
						.setParameter(8, Constants.OVERDUE_RECEIVABLES)
						.setParameter(9, Constants.FOR_DEBT_MARK);

				try {
					rows = updateInvest.executeUpdate();
				} catch (Exception e) {
					e.printStackTrace();
					Logger.info("修改投资账单状态时：" + e.getMessage());
					error.code = -3;
					error.msg = "数据库异常，导致修改投资账单状态失败";
					JPA.setRollbackOnly();

					return error.code;
				}
			}

		} else {
			// 6.如果为本金垫付，扣完借款人还款金额后直接添加平台收取本金垫付交易记录
			DealDetail.addPlatformDetail(DealType.ADVANCE_FEE, this._id,
					userId, -1, DealType.ACCOUNT, payment, 1, "平台收取第"
							+ this._id + "号账单本金垫付金额", error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}
		}

		// 7.修改借款账单还款情况
		String updateBillSql = "update t_bills set status = ?, real_repayment_time = ?, real_repayment_corpus = ?, real_repayment_interest = ?, overdue_fine = ? where id = ? and status in (?,?)";

		Query updateBill = em.createQuery(updateBillSql)
				.setParameter(1, Constants.OVERDUE_PATMENT)
				.setParameter(2, DateUtil.currentDate())
				.setParameter(3, repaymentCorpus)
				.setParameter(4, repaymentInterest)
				.setParameter(5, repayOverdueFine).setParameter(6, this._id)
				.setParameter(7, Constants.NO_REPAYMENT)
				.setParameter(8, Constants.ADVANCE_PRINCIIPAL_REPAYMENT);

		try {
			rows = updateBill.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改借款账单数据时：" + e.getMessage());
			error.code = -3;
			error.msg = "还款出现异常，导致还款失败";
			JPA.setRollbackOnly();

			return error.code;
		} finally {
			double Userbalance = queryBalance(userId, error);// 用户可用余额

			if (rows == 0) {
				error.code = -1;
				error.msg = "还款出现异常，导致还款失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			if (Userbalance < 0) {
				error.code = -1;
				error.msg = "资金不足,请充值!";
				JPA.setRollbackOnly();

				return error.code;
			}
		}

		// 8.判断这个借款标是否已还完款，如果还完后给这个借款标标记为已还完状态
		int count = this.isEndPayment(this.bidId, error);

		if (count == 0) {
			String bidSql = "update t_bids set status = ?, last_repay_time = ? where id = ? and status not in(?)";

			Query query = em.createQuery(bidSql)
					.setParameter(1, Constants.BID_REPAYMENTS)
					.setParameter(2, DateUtil.currentDate())
					.setParameter(3, bidId)
					.setParameter(4, Constants.BID_REPAYMENTS);

			try {
				rows = query.executeUpdate();
				;

			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改借款标为已还款完状态时：" + e.getMessage());
				error.code = -7;
				error.msg = "还款出现异常，导致还款失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "还款出现异常，导致还款失败";
				JPA.setRollbackOnly();

			}
		}

		// 4.扣除借款人还款本期的本金和利息
		String userBalanceSql = "update t_users set balance = balance - ? where id =?";

		Query userBalanceUpdate = em.createQuery(userBalanceSql)
				.setParameter(1, Arith.add(repaymentCorpus, repaymentInterest))
				.setParameter(2, userId);

		try {
			rows = userBalanceUpdate.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改借款人账户金额时：" + e.getMessage());
			error.code = -2;
			error.msg = "还款出现异常，导致还款失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (rows == 0) {
			error.code = -1;
			error.msg = "还款出现异常，导致还款失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		// 9.添加有关借款账单还款的交易记录和事件
		Map<String, Double> billForDetail = DealDetail.queryUserFund(userId,
				error);
		if (error.code < 0 || billForDetail == null) {
			JPA.setRollbackOnly();

			return -1;
		}

		double userFreeze = billForDetail.get("freeze");
		double userReceiveAmount = billForDetail.get("receive_amount");

		Double investBalance15 = Bill.queryBalance(userId, error);

		DealDetail detail = new DealDetail(userId, DealType.CHARGE_OVER_PAY,
				payment, this._id, investBalance15, userFreeze,
				userReceiveAmount, "第" + this._id + "号账单逾期还款扣除还款金额");

		// 添加逾期还款的交易记录
		detail.addDealDetail(error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		// 4.扣除借款人还款本期的逾期费用
		String userBalanceSql2 = "update t_users set balance = balance - ? where id =?";

		Query userBalanceUpdate2 = em.createQuery(userBalanceSql2)
				.setParameter(1, repayOverdueFine).setParameter(2, userId);

		try {
			rows = userBalanceUpdate2.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改借款人账户金额时：" + e.getMessage());
			error.code = -2;
			error.msg = "还款出现异常，导致还款失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (rows == 0) {
			error.code = -1;
			error.msg = "还款出现异常，导致还款失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		// 本次查询用户可用余额是获取最新状态值，避免从缓存里拿数据
		Double userBalance2 = Bill.queryBalance(userId, error);

		DealDetail overdueDetail = new DealDetail(userId,
				DealType.CHARGE_OVERDUE_FEE, repayOverdueFine, this._id,
				userBalance2, userFreeze, userReceiveAmount, "第" + this._id
						+ "号账单逾期还款扣除逾期费");

		// 添加逾期还款扣除逾期费的交易记录
		overdueDetail.addDealDetail(error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		// 逾期还款减去信用积分
		int creditScore = DealDetail.addCreditScore(userId, -1, 0, this._id, "第"
				+ this._id + "号账单逾期还款扣除信用积分", error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		// 添加事件
		DealDetail.userEvent(userId, UserEvent.ADD_NOMAL_PAY, "第" + this._id
				+ "号账单用户逾期还款", error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		Map<String, Object> userMap = new HashMap<String, Object>();

		String userSql = "select new Map(name as name, email as eamil, mobile as mobile) from t_users where id = ? ";

		try {
			userMap = GenericModel.find(userSql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据用户id查询时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户名失败";

			return error.code;
		}

		String userName = (String) userMap.get("name");
		String userEamil = (String) userMap.get("eamil");
		String userMobile = (String) userMap.get("mobile");

		// 发送站内信 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
		TemplateStation station = new TemplateStation();
		station.id = Templets.M_SUCCESS_PAY;

		if (station.status) {
			String mContent = station.content.replace("userName", userName);
			mContent = mContent.replace("date",
					DateUtil.dateToString(new Date()));
			mContent = mContent.replace("title", title);
			mContent = mContent.replace("needSum", payment + "");
			mContent = mContent.replace("creditScore", -creditScore + "");
			TemplateStation.addMessageTask(userId, station.title, mContent);
		}

		// 发送邮件 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
		TemplateEmail email = new TemplateEmail();
		email.id = Templets.E_SUCCESS_PAY;

		if (email.status) {
			String eContent = email.content.replace("userName", userName);
			eContent = eContent.replace("date",
					DateUtil.dateToString(new Date()));
			eContent = eContent.replace("title", title);
			eContent = eContent.replace("needSum", payment + "");
			eContent = eContent.replace("creditScore", -creditScore + "");
			TemplateEmail.addEmailTask(userEamil, email.title, eContent);
		}

		// 发送短信 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
		TemplateSms sms = new TemplateSms();
		if (StringUtils.isNotBlank(userMobile)) {
			sms.id = Templets.S_SUCCESS_PAY;

			if (sms.status) {
				String eContent = sms.content.replace("userName", userName);
				eContent = eContent.replace("date",
						DateUtil.dateToString(new Date()));
				eContent = eContent.replace("title", title);
				eContent = eContent.replace("needSum", payment + "");
				eContent = eContent.replace("creditScore", -creditScore + "");
				TemplateSms.addSmsTask(userMobile, eContent);
			}
		}

		// 10.更改当前用户在缓存变动的数据
		User user = User.currUser();
		user.balance = lastBalance;
		user.creditScore = user.creditScore + creditScore;
		User.setCurrUser(user);

		// 11.还款完毕后重新生成新的数据库防篡改标志
		DataSafety dataTamperproof = new DataSafety();
		dataTamperproof.id = userId;
		dataTamperproof.updateSign(error);

		error.code = 0;
		error.msg = "还款成功";
		return error.code;
	}

	/**
	 * 判断借款账单是否已还完款
	 * 
	 * @param bidId
	 * @param error
	 * @return
	 */
	public int isEndPayment(long bidId, ErrorInfo error) {
		error.clear();
		int count = 0;

		try {
			count = (int) GenericModel.count("status in(?, ?) and bid_id = ?",
					Constants.NO_REPAYMENT,
					Constants.ADVANCE_PRINCIIPAL_REPAYMENT, bidId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询借款账单是否已还完款时：" + e.getMessage());
			error.code = -1;
			error.msg = "数据库异常，导致查询借款账单是否已还完款失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		return count;
	}

	/**
	 * 标记逾期
	 * 
	 * @param obj
	 */
	public static int markOverdue(long supervisorId, long billIdStr,
			ErrorInfo error) {
		error.clear();

		long billId = billIdStr;

		EntityManager em = JPA.em();
		Query query = em
				.createQuery(
						"update t_bills set overdue_mark = ?, mark_overdue_time = ? where id = ? and overdue_mark not in(?) ")
				.setParameter(1, Constants.BILL_OVERDUE)
				.setParameter(2, new Date()).setParameter(3, billId)
				.setParameter(4, Constants.BILL_OVERDUE);
		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("管理员手动标记借款账单为逾期时：" + e.getMessage());
			error.code = -1;
			error.msg = "数据库异常，导致管理员手动标记借款账单为逾期失败";

			return error.code;
		}

		if (rows == 0) {
			error.code = -1;
			error.msg = "标记逾期操作没有执行";
			JPA.setRollbackOnly();

			return error.code;

		}

		// 添加管理员操作日志
		DealDetail
				.supervisorEvent(supervisorId, SupervisorEvent.MAKE_BILL_OVER,
						"标记" + billId + "号账单为逾期", error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		error.code = 0;
		error.msg = "标记逾期成功！";

		return error.code;

	}

	/**
	 * 系统标记逾期或重新计算逾期费用(每天凌晨00:00系统运行一次该方法)
	 */
	public int systemMakeOverdue(ErrorInfo error) {
		error.clear();
		EntityManager em = JPA.em();

		// 1.修改借款账单逾期生成的数据
		String updateBillSql = "update t_bills t1,(select c.id, round((c.days * c.amount * c._value / 100),2) overdue_fee "
				+ "from (select a.id, b._value, DATEDIFF(now(), a.repayment_time) days, (a.repayment_corpus + "
				+ "a.repayment_interest) amount from t_bills a join t_system_options b where b._key = 'overdue_fee' "
				+ "and a.`status` = -1 and a.overdue_mark= 0 and a.repayment_time < now() group by a.id) c) t2 set "
				+ "t1.mark_overdue_time = now(), t1.overdue_fine = t2.overdue_fee, t1.overdue_mark = -1 where t1.id = t2.id";

		Query updateBill = em.createNativeQuery(updateBillSql);

		try {
			updateBill.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改借款账单逾期生成的数据时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致修改借款账单逾期生成的数据失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		// 2.修改理财账单逾期生成的数据
		String updateInvestBillSql = "update t_bill_invests t1,(select t3.id, round((t4.overdue_fine * ifnull(t6.amount,0) / t5.amount), 2)"
				+ " check_fine, t3.periods, t3.bid_id from (select b.bid_id, b.periods, b.overdue_fine from t_bills b where "
				+ "b.overdue_mark = -1 and date_format(b.mark_overdue_time, '%y%m%d') = date_format(now(), '%y%m%d')) t4 "
				+ "left join (select a.invest_id, a.id, a.periods, a.bid_id, a.user_id from t_bill_invests a) t3 on t3.bid_id = t4.bid_id "
				+ "and t3.periods = t4.periods left join (select c.id, c.amount from t_bids c) t5 on t3.bid_id = t5.id "
				+ "left join (select d.id, d.bid_id, d.user_id, d.amount from t_invests d) t6 on t3.bid_id = t6.bid_id and "
				+ "t3.user_id = t6.user_id and t3.invest_id = t6.id) t2 set t1.overdue_fine = check_fine, t1.status = -2 where t1.id = t2.id";

		Query updateInvestBill = em.createNativeQuery(updateInvestBillSql);

		try {
			updateInvestBill.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改理财账单逾期生成的数据时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致修改理财账单逾期生成的数据失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		error.code = 0;
		return error.code;
	}

	/**
	 * 标记坏账
	 * 
	 * @param obj
	 */
	public static int markBad(long supervisorId, long billId, ErrorInfo error) {
		error.clear();
		EntityManager em = JPA.em();

		String sql = "update t_bills bill set bill.overdue_mark = ?, mark_bad_time = ? where bill.id = ? and bill.overdue_mark not in(?)";
		Query query = em.createQuery(sql)
				.setParameter(1, Constants.BILL_BAD_DEBTS)
				.setParameter(2, DateUtil.currentDate())
				.setParameter(3, billId)
				.setParameter(4, Constants.BILL_BAD_DEBTS);
		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改借款账单数据时：" + e.getMessage());
			error.code = -1;
			error.msg = "数据库异常，导致修改借款账单数据失败";

			return 0;
		}

		if (rows == 0) {
			error.code = -1;
			error.msg = "修改借款账单数据操作没有执行";
			JPA.setRollbackOnly();

			return error.code;

		}

		// 添加管理员操作日志
		DealDetail.supervisorEvent(supervisorId, SupervisorEvent.MAKE_BILL_BAD,
				"标记账单为坏账", error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		error.code = 0;
		error.msg = "标记坏账成功";
		return error.code;
	}

	/**
	 * 用户的待还金额
	 * 
	 * @return
	 */
	public static double forPay(long userId, ErrorInfo error) {
		String sql = "select SUM(a.repayment_corpus + a.repayment_interest + a.overdue_fine) "
				+ "as totalpay from t_bills as a, t_bids as b where a.bid_id = b.id and b.user_id = ? and a.status in (-1,-2)";
		Double pay = 0.0;

		try {
			pay = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			Logger.info("用户的待还金额:" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户待还金额出现异常";
			JPA.setRollbackOnly();

			return -1;
		}

		if (null == pay || pay == 0) {
			pay = 0.0;
		}

		return pay;
	}

	/**
	 * 用户的待收金额
	 * 
	 * @return
	 */
	public static double forReceive(long userId, ErrorInfo error) {
		String sql = "select SUM(a.receive_corpus + a.receive_interest + IFNULL(a.overdue_fine,0.00))"
				+ " as totalreceive from t_bill_invests as a where a.user_id = ? and a.status in (-1,-2,-5,-6)";
		Double receive = 0.0;

		try {
			receive = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			Logger.info("用户的待收金额:" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户待还金额出现异常";
			JPA.setRollbackOnly();

			return -1;
		}

		if (null == receive || receive == 0) {
			receive = 0.0;
		}

		return receive;
	}

	/**
	 * 查询借款账单信息并计算借款人实际的逾期罚款
	 * 
	 * @return
	 * @throws ParseException
	 */
	public Map<String, Object> billOverdueFee(long billId, ErrorInfo error) {
		JPA.setRollbackOnly();
		error.clear();

		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, Object> billMap = new HashMap<String, Object>();

		String billSql = "select new Map(bill.repayment_corpus as repayment_corpus, bill.repayment_interest as"
				+ " repayment_interest, bill.repayment_time as repayment_time) from t_bills as bill where bill.id = ?";

		try {
			billMap = GenericModel.find(billSql, billId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询借款账单信息时：" + e.getMessage());
			error.code = -1;
			error.msg = "数据库异常，导致查询借款账单信息失败";
			JPA.setRollbackOnly();

			return null;
		}

		double payCorpus = (Double) billMap.get("repayment_corpus");
		double payInterest = (Double) billMap.get("repayment_interest");
		Date payTime = (Date) billMap.get("repayment_time");

		double overdueRates = (Double) Bill.checkManagerate(error).get(
				"overdueRates");// 逾期管理费率

		if (overdueRates != 0) {
			overdueRates = overdueRates / 100;
		}

		int days = DateUtil.daysBetween(payTime, DateUtil.currentDate());
		double payOverdueFee = Arith.mul(
				Arith.mul(Arith.add(payCorpus, payInterest), overdueRates),
				days);// 借款人本期应还的逾期罚息

		map.put("repayment_corpus", payCorpus);
		map.put("repayment_interest", payInterest);
		map.put("payOverdueFee", payOverdueFee);

		return map;
	}

	/**
	 * 本金垫付账单
	 * 
	 * @throws ParseException
	 */
	public int principalAdvancePayment(long supervisorId, int status,
			long billId, long bidId, int period, ErrorInfo error) {
		error.clear();
		Bill bill = new Bill();
		Map<String, Object> billMap = new HashMap<String, Object>();
		int rows = 0;

		List<Map<String, Object>> investList = new ArrayList<Map<String, Object>>();
		EntityManager em = JPA.em();

		if (status == Constants.ADVANCE_PRINCIIPAL_REPAYMENT) {
			error.code = -1;
			error.msg = "本期账单已还款";

			return error.code;
		}

		String billSql = "select new Map(bill.repayment_corpus as repayment_corpus, bill.repayment_interest as"
				+ " repayment_interest, bill.repayment_time as repayment_time, bill.overdue_fine as overdue_fine) from t_bills as bill where bill.id = ? and bill.status not in (?,?,?)";

		try {
			billMap = GenericModel.find(billSql, billId, Constants.NORMAL_REPAYMENT,
					Constants.ADVANCE_PRINCIIPAL_REPAYMENT,
					Constants.OVERDUE_PATMENT).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询借款账单信息时：" + e.getMessage());
			error.code = -1;
			error.msg = "数据库异常，导致本金垫付账单失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (null == billMap) {
			error.code = -1;
			error.msg = "数据库异常，导致本金垫付账单失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		double payCorpus = (Double) billMap.get("repayment_corpus");
		double payOverdueFee = (Double) billMap.get("overdue_fine");
		double payInterest = (Double) billMap.get("repayment_interest");

		double investmentRate = (Double) Bill.checkManagerate(error).get(
				"investmentRates");// 投资管理费率

		if (investmentRate != 0) {
			investmentRate = investmentRate / 100;
		}

		String sql = "select new Map(invest.id as id, invest.invest_id as investId, invest.receive_corpus as receive_corpus,invest.receive_interest as "
				+ "receive_interest, invest.overdue_fine as overdue_fine, invest.user_id as user_id, invest.overdue_fine) "
				+ "from t_bill_invests as invest where invest.bid_id = ? and invest.periods = ? and invest.status not in (?,?,?,?)";
		try {
			investList = GenericModel.find(sql, bidId, period,
					Constants.FOR_DEBT_MARK, Constants.NORMAL_RECEIVABLES,
					Constants.ADVANCE_PRINCIIPAL_RECEIVABLES,
					Constants.OVERDUE_RECEIVABLES).fetch();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询投资账单信息时：" + e.getMessage());
			error.code = -1;
			error.msg = "数据库异常，导致本金垫付账单失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (null == investList) {
			error.code = -1;
			error.msg = "数据库异常，导致本金垫付账单失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		for (Map<String, Object> map : investList) {
			long investBillId = (Long) map.get("id");
			long investId = (Long) map.get("investId");
			double receiveInterest = (Double) map.get("receive_interest");// 本期的投资利息
			double receiveCorpus = (Double) map.get("receive_corpus");
			double receiveFees = (Double) map.get("overdue_fine");
			long investUserId = (Long) map.get("user_id");
			String investSql = "select balance from t_users where id = ? ";
			Double investerBalance = null;

			try {
				investerBalance = GenericModel.find(investSql, investUserId).first();// 获取投资用户的余额
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("查询投资用户的可用金额时：" + e.getMessage());
				error.code = -2;
				error.msg = "数据库异常，导致本金垫付账单失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			investerBalance = null == investerBalance ? 0 : investerBalance
					.doubleValue();
			double investManageFee = Arith.round(
					Arith.mul(receiveInterest, investmentRate), 2);// 投资管理费
			// double receiveFees = Arith.div(Arith.mul(receiveCorpus,
			// payOverdueFee), payCorpus, 2);//本期每个投资人获得的罚息
			double receive = receiveCorpus + receiveInterest + receiveFees
					- investManageFee;
			// double totalBalance = Arith.add(investerBalance,
			// receive);//本金垫付后投资用户的总余额

			// 查看投资用户是否受到非法资金改动
			DataSafety investDataTamperproof = new DataSafety();
			investDataTamperproof.id = investUserId;
			boolean investIsChange = investDataTamperproof.signCheck(error);

			// 改变投资账单的收款状态为本金垫付
			String updateSql = "update t_bill_invests set status = ?, real_receive_time = ?, real_receive_corpus = ?,"
					+ " overdue_fine = ?, real_receive_interest = ? where user_id = ? and bid_id = ? and periods = ? and status not in(?,?) and invest_id = ?";

			Query query = em.createQuery(updateSql)
					.setParameter(1, Constants.ADVANCE_PRINCIIPAL_RECEIVABLES)
					.setParameter(2, DateUtil.currentDate())
					.setParameter(3, receiveCorpus)
					.setParameter(4, receiveFees)
					.setParameter(5, receiveInterest - investManageFee)
					.setParameter(6, investUserId).setParameter(7, bidId)
					.setParameter(8, period)
					.setParameter(9, Constants.ADVANCE_PRINCIIPAL_RECEIVABLES)
					.setParameter(10, Constants.FOR_DEBT_MARK)
					.setParameter(11, investId);

			try {
				rows = query.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改投资账单的状态时：" + e.getMessage());
				error.code = -3;
				error.msg = "数据库异常，导致本金垫付账单失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据库异常，导致本金垫付账单失败";
				JPA.setRollbackOnly();

				return error.code;

			}

			// 返回每个投资用户每期获得的投资本金和利息
			String userBalanceSql = "update t_users set balance = balance + ? where id = ?";
			Query userBalance = em.createQuery(userBalanceSql)
					.setParameter(1, receiveInterest + receiveCorpus)
					.setParameter(2, investUserId);

			try {
				rows = userBalance.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改投资用户的金额时：" + e.getMessage());
				error.code = -2;
				error.msg = "数据库异常，导致本金垫付账单失败";

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据库异常，导致本金垫付账单失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			Map<String, Double> billForDetail = DealDetail.queryUserFund(
					investUserId, error);
			if (error.code < 0 || billForDetail == null) {
				JPA.setRollbackOnly();

				return -1;
			}

			double userFreeze = billForDetail.get("freeze");
			double userReceiveAmount = billForDetail.get("receive_amount");
			Double investBalance10 = Bill.queryBalance(investUserId, error);

			DealDetail investDetail = new DealDetail(investUserId,
					DealType.OVER_RECEIVE, receive - investManageFee,
					investBillId, investBalance10, userFreeze,
					userReceiveAmount, "逾期收款获取第" + billId + "号账单投资金额");

			// 添加逾期收款的交易记录
			investDetail.addDealDetail(error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}

			// 返回每个投资用户每期获得的罚息
			String userBalanceSql2 = "update t_users set balance = balance + ? where id = ?";
			Query userBalance2 = em.createQuery(userBalanceSql2)
					.setParameter(1, receiveFees).setParameter(2, investUserId);

			try {
				rows = userBalance2.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改投资用户的金额时：" + e.getMessage());
				error.code = -2;
				error.msg = "数据库异常，导致本金垫付账单失败";

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据库异常，导致本金垫付账单失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			Double investBalance3 = Bill.queryBalance(investUserId, error);
			DealDetail investOverdueFeeDetail = new DealDetail(investUserId,
					DealType.ADD_OVERDUE_FEE, receiveFees, investBillId,
					investBalance3, userFreeze, userReceiveAmount, "获取第"
							+ billId + "号账单逾期费");

			// 添加获取逾期费用的交易记录
			investOverdueFeeDetail.addDealDetail(error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}

			// 减去每个投资用户每期的管理费
			String userBalanceSql3 = "update t_users set balance = balance - ? where id = ?";
			Query userBalance3 = em.createQuery(userBalanceSql3)
					.setParameter(1, investManageFee)
					.setParameter(2, investUserId);

			try {
				rows = userBalance3.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改投资用户的金额时：" + e.getMessage());
				error.code = -2;
				error.msg = "数据库异常，导致本金垫付账单失败";

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据库异常，导致本金垫付账单失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			Double investBalance4 = Bill.queryBalance(investUserId, error);

			DealDetail investFeeDetail = new DealDetail(investUserId,
					DealType.CHARGE_INVEST_FEE, investManageFee, investBillId,
					investBalance4, userFreeze, userReceiveAmount, "扣除"
							+ billId + "号 账单理财管理费");

			// 添加扣除理财管理费的交易记录
			investFeeDetail.addDealDetail(error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}

			/* 添加CPS推广费 */
			User.rewardCPS(investUserId, investManageFee, billId, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}

			// 添加平台扣除理财管理费交易记录
			DealDetail.addPlatformDetail(DealType.INVEST_FEE, investBillId, -1,
					investUserId, DealType.ACCOUNT, investManageFee, 1, "平台收取"
							+ billId + "号 账单投资管理费", error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}

			Map<String, Object> userMap = new HashMap<String, Object>();

			String userSql = "select new Map(name as name, email as eamil, mobile as mobile) from t_users where id = ? ";

			try {
				userMap = GenericModel.find(userSql, investUserId).first();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("根据用户id查询时：" + e.getMessage());
				error.code = -1;
				error.msg = "查询用户名失败";

				return error.code;
			}

			String bidSql = "select title from t_bids where id = ? ";
			String title = null;

			try {
				title = GenericModel.find(bidSql, bidId).first();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("根据用户id查询时：" + e.getMessage());
				error.code = -1;
				error.msg = "查询用户名失败";

				return error.code;
			}

			String userName = (String) userMap.get("name");
			String userEamil = (String) userMap.get("eamil");
			String userMobile = (String) userMap.get("mobile");

			// 发送站内信 尊敬的username:\n
			// 您投资的借款title,第repayPeriod期还款已经完成.<br/>paymentModeStr本期应得总额：
			// ￥recivedSum,其中本金部分为：hasP元,利息部分：hasI元,实得逾期罚息：hasLFI元<br/>扣除投资管理费：
			// ￥mFee元<br/>实得总额：￥msFee元
			TemplateStation station = new TemplateStation();
			station.id = Templets.M_INVEST_RECEIVE;

			if (station.status) {
				String mContent = station.content.replace("userName", userName);
				mContent = mContent.replace("title", title);
				mContent = mContent.replace("repayPeriod", period + "");
				mContent = mContent.replace("recivedSum", receive + "");
				mContent = mContent.replace("hasP", receiveCorpus + "");
				mContent = mContent.replace("hasI", receiveInterest + "");
				mContent = mContent.replace("hasLFI", receiveFees + "");
				mContent = mContent.replace("mFee", investManageFee + "");
				mContent = mContent.replace("msFee", receive + "");
				TemplateStation.addMessageTask(investUserId, station.title,
						mContent);
			}

			// 发送邮件 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
			TemplateEmail email = new TemplateEmail();
			email.id = Templets.E_INVEST_RECEIVE;

			if (email.status) {
				String eContent = email.content.replace("userName", userName);
				eContent = eContent.replace("title", title);
				eContent = eContent.replace("repayPeriod", period + "");
				eContent = eContent.replace("recivedSum", receive + "");
				eContent = eContent.replace("hasP", receiveCorpus + "");
				eContent = eContent.replace("hasI", receiveInterest + "");
				eContent = eContent.replace("hasLFI", receiveFees + "");
				eContent = eContent.replace("mFee", investManageFee + "");
				eContent = eContent.replace("msFee", receive + "");
				TemplateEmail.addEmailTask(userEamil, email.title, eContent);
			}

			// 发送短信 尊敬的username:\n
			// 您投标的借款title,第repayPeriod期还款已经完成.\npaymentModeStr本期应得总额:
			// ￥recivedSum,其中本金部分为:hasP元,利息部分:hasI元,实得逾期罚息:hasLFI元\n扣除投资管理费:
			// ￥mFee元\n实得总额:￥msFee元
			TemplateSms sms = new TemplateSms();
			if (StringUtils.isNotBlank(userMobile)) {
				sms.id = Templets.S_INVEST_RECEIVE;

				if (sms.status) {
					String sContent = sms.content.replace("userName", userName);
					sContent = sContent.replace("title", title);
					sContent = sContent.replace("repayPeriod", period + "");
					sContent = sContent.replace("recivedSum", receive + "");
					sContent = sContent.replace("hasP", receiveCorpus + "");
					sContent = sContent.replace("hasI", receiveInterest + "");
					sContent = sContent.replace("hasLFI", receiveFees + "");
					sContent = sContent.replace("mFee", investManageFee + "");
					sContent = sContent.replace("msFee", receive + "");
					TemplateSms.addSmsTask(userMobile, sContent);
				}
			}

			// 如果投资用户账户资金没有遭到非法改动，那么就更新其篡改标志，否则不更新
			if (investIsChange) {
				investDataTamperproof.id = investUserId;
				investDataTamperproof.updateSign(error);
			}
		}

		// 改变借款账单的状态为本金垫付还款

		String updateBillSql = "update t_bills set status = ?, real_repayment_time = ?, real_repayment_corpus = ?, "
				+ "real_repayment_interest = ? where id = ? and status not in(?) ";

		Query updateBill = em.createQuery(updateBillSql)
				.setParameter(1, Constants.ADVANCE_PRINCIIPAL_REPAYMENT)
				.setParameter(2, DateUtil.currentDate())
				.setParameter(3, payCorpus).setParameter(4, payInterest)
				.setParameter(5, billId)
				.setParameter(6, Constants.ADVANCE_PRINCIIPAL_REPAYMENT);

		try {
			rows = updateBill.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改借款账单的状态时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致本金垫付账单失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (rows == 0) {
			error.code = -1;
			error.msg = "数据库异常，导致本金垫付账单失败";
			JPA.setRollbackOnly();

			return error.code;

		}

		// 添加平台本金垫付支出交易记录
		DealDetail.addPlatformDetail(DealType.ADVANCE_FEE, billId,
				supervisorId, -1, DealType.ACCOUNT, payCorpus + payOverdueFee
						+ payInterest, 2, "平台本金垫付" + billId + "号 账单支出", error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		// 添加管理员操作日志
		DealDetail.supervisorEvent(supervisorId, SupervisorEvent.PRINCIPAL_PAY,
				"管理员对" + billId + "号 账单本金垫付", error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		error.code = 0;
		error.msg = "本金垫付成功";
		return error.code;
	}

	/**
	 * 后台--我的会员账单--查询催收账单
	 * 
	 * @param supervisorId
	 * @param billIdStr
	 * @param error
	 * @return
	 */
	public static v_bill_detail_for_collection queryCollection(
			long supervisorId, long billId, ErrorInfo error) {
		error.clear();

		// if(!NumberUtil.isNumericInt(billIdStr)) {
		// error.code = -1;
		// error.msg = "账单类型有误";
		//
		// return null;
		// }

		v_bill_detail_for_collection collection = null;

		try {
			collection = GenericModel.findById(billId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询催收账单时：" + e.getMessage());
			error.code = -4;
			error.msg = "数据库异常，查询催收账单时失败";

			return null;
		}

		if (collection == null) {
			collection = new v_bill_detail_for_collection();
		}

		return collection;
	}

	/**
	 * 后台--我的会员账单--查询逾期账单
	 * 
	 * @param supervisorId
	 * @param billIdStr
	 * @param error
	 * @return
	 */
	public static v_bill_detail_for_mark_overdue queryOverdue(
			long supervisorId, long billId, ErrorInfo error) {
		error.clear();

		v_bill_detail_for_mark_overdue overdue = null;

		try {
			overdue = GenericModel.findById(billId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询催收账单时：" + e.getMessage());
			error.code = -4;
			error.msg = "数据库异常，查询催收账单时失败";

			return null;
		}

		if (overdue == null) {
			overdue = new v_bill_detail_for_mark_overdue();
		}

		return overdue;
	}

	/**
	 * 账单催收
	 * 
	 * @param supervisorId
	 * @param typeStr
	 *            1 站内信 2 邮件 3 电话
	 * @param billId
	 * @param error
	 * @return
	 */
	public static int updateBillCollection(long supervisorId, String typeStr,
			long billId, ErrorInfo error) {
		error.clear();

		if (!NumberUtil.isNumericInt(typeStr)) {
			error.code = -1;
			error.msg = "类型有误";

			return error.code;
		}

		int type = Integer.parseInt(typeStr);

		if (type < 0 || type > 3) {
			error.code = -2;
			error.msg = "类型范围有误";

			return error.code;
		}

		String sql = "";
		switch (type) {
		case Constants.ONE:
			sql = "update t_bills set notice_count_message = notice_count_message+1 where id = ?";
			break;

		case Constants.TWO:
			sql = "update t_bills set notice_count_mail = notice_count_mail+1 where id = ?";
			break;

		case Constants.THREE:
			sql = "update t_bills set notice_count_telphone = notice_count_telphone+1 where id = ?";
			break;

		default:
			break;
		}

		int rows = 0;

		try {
			rows = JpaHelper.execute(sql, billId).executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改借款账单的催收次数时：" + e.getMessage());
			error.code = -1;
			error.msg = "数据库异常，导致修改借款账单的催收次数失败";

			return error.code;
		}

		if (rows == 0) {
			error.code = -1;
			error.msg = "修改借款账单的催收次数操作没有执行";
			JPA.setRollbackOnly();

			return error.code;

		}

		// 添加管理员操作日志
		if (type == Constants.ONE) {
			DealDetail.supervisorEvent(supervisorId,
					SupervisorEvent.BILL_COLLECTION, "管理员发短信催收" + billId
							+ "号 账单", error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}
		}

		if (type == Constants.TWO) {
			DealDetail.supervisorEvent(supervisorId,
					SupervisorEvent.BILL_COLLECTION, "管理员发邮件催收" + billId
							+ "号 账单", error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}
		}

		DealDetail.supervisorEvent(supervisorId,
				SupervisorEvent.BILL_COLLECTION, "管理员打电话催收" + billId + "号 账单",
				error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		error.msg = "催收账单成功！";
		error.code = 0;
		return error.code;
	}

	/**
	 * 线下收款
	 * 
	 * @param info
	 * @return
	 */
	public int offlineCollection(long supervisorId, ErrorInfo error) {
		error.clear();

		Map<String, Object> billMap = new HashMap<String, Object>();
		List<Map<String, Object>> investList = new ArrayList<Map<String, Object>>();
		EntityManager em = JPA.em();
		int rows = 0;
		int status;
		int overdueMark;
		double investmentRates = (Double) Bill.checkManagerate(error).get(
				"investmentRates");

		if (investmentRates != 0) {
			investmentRates = investmentRates / 100;
		}

		if (this.status != Constants.NO_REPAYMENT
				&& this.status != Constants.ADVANCE_PRINCIIPAL_REPAYMENT) {
			error.code = -1;
			error.msg = "本期账单已还款";

			return error.code;
		}

		String sql = "select new Map(bill.overdue_mark as overdue_mark, bill.repayment_corpus as "
				+ "repayment_corpus, bill.repayment_interest as repayment_interest, bill.overdue_fine as overdue_fine,"
				+ " bill.status as status) from t_bills bill where bill.id = ? and bill.status not in(?,?)";

		try {
			billMap = GenericModel.find(sql, this._id, Constants.NORMAL_REPAYMENT,
					Constants.OVERDUE_PATMENT).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询借款账单状态时：" + e.getMessage());
			error.code = -1;
			error.msg = "数据库异常，导致查询借款账单状态失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (null == billMap) {
			error.code = -1;
			error.msg = "数据库异常，导致查询借款账单状态失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		double repaymentCorpus = (Double) billMap.get("repayment_corpus");
		double repaymentInterest = (Double) billMap.get("repayment_interest");
		double repayOverdueFine = (Double) billMap.get("overdue_fine");
		status = (Integer) billMap.get("status");
		overdueMark = (Integer) billMap.get("overdue_mark");

		String sql3 = "select new Map(invest.id as id, invest.invest_id as investId, invest.receive_corpus as receive_corpus, invest.receive_interest "
				+ "as receive_interest, invest.user_id as user_id, invest.overdue_fine as overdue_fine) "
				+ "from t_bill_invests as invest where invest.bid_id = ? and invest.periods = ? and invest.status not in(?,?,?)";

		try {
			investList = GenericModel.find(sql3, this.bidId, this.periods,
					Constants.NORMAL_RECEIVABLES,
					Constants.OVERDUE_RECEIVABLES, Constants.FOR_DEBT_MARK)
					.fetch();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询投资账单信息时：" + e.getMessage());
			error.code = -2;
			error.msg = "数据库异常，导致查询投资账单信息失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (null == investList) {
			error.code = -1;
			error.msg = "数据库异常，导致查询借款账单状态失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		for (Map<String, Object> map : investList) {
			long investBillId = (Long) map.get("id");
			long investUserId = (Long) map.get("user_id");
			long investId = (Long) map.get("investId");// 投资记录id
			double receiveCorpus = (Double) map.get("receive_corpus");// 本期的投资本金
			double receiveInterest = (Double) map.get("receive_interest");// 本期的投资利息
			double investOverdueFine = (Double) map.get("overdue_fine");// 收取逾期罚款

			double manageFee = Arith.round(
					Arith.mul(receiveInterest, investmentRates), 2);// 投资管理费
			double receive = receiveCorpus + receiveInterest
					+ investOverdueFine;
			double totalBalance = Arith.sub(receive, manageFee);

			// 查看投资用户是否受到非法资金改动
			DataSafety investDataTamperproof = new DataSafety();
			investDataTamperproof.id = investUserId;
			boolean investIsChange = investDataTamperproof.signCheck(error);

			Query query = null;

			// 改变投资账单的收款状态
			String updateSql = "update t_bill_invests set status = ?, real_receive_time = ?, real_receive_corpus = ?,"
					+ " real_receive_interest = ?, overdue_fine = ? where user_id = ? and bid_id = ? and periods = ? and status not in(?) and invest_id = ?";

			if (status == Constants.NO_REPAYMENT
					&& overdueMark == Constants.BILL_NO_OVERDUE) {// 正常收款
				query = em.createQuery(updateSql)
						.setParameter(1, Constants.NORMAL_RECEIVABLES)
						.setParameter(2, DateUtil.currentDate())
						.setParameter(3, receiveCorpus)
						.setParameter(4, receiveInterest - manageFee)
						.setParameter(5, investOverdueFine)
						.setParameter(6, investUserId)
						.setParameter(7, this.bidId)
						.setParameter(8, this.periods)
						.setParameter(9, Constants.NORMAL_RECEIVABLES)
						.setParameter(10, investId);
			} else {
				query = em.createQuery(updateSql)
						.setParameter(1, Constants.OVERDUE_RECEIVABLES)
						.setParameter(2, DateUtil.currentDate())
						.setParameter(3, receiveCorpus)
						.setParameter(4, receiveInterest - manageFee)
						.setParameter(5, investOverdueFine)
						.setParameter(6, investUserId)
						.setParameter(7, this.bidId)
						.setParameter(8, this.periods)
						.setParameter(9, Constants.OVERDUE_RECEIVABLES)
						.setParameter(10, investId);
			}

			try {
				rows = query.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("更新投资账单数据时：" + e.getMessage());
				error.code = -5;
				error.msg = "数据库异常，导致更新投资账单数据失败";
				JPA.setRollbackOnly();
				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "更新投资账单数据操作没有执行";
				JPA.setRollbackOnly();

				return error.code;
			}

			Map<String, Object> userMap = new HashMap<String, Object>();

			String userSql = "select new Map(name as name, email as eamil, mobile as mobile) from t_users where id = ? ";

			try {
				userMap = GenericModel.find(userSql, investUserId).first();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("根据用户id查询时：" + e.getMessage());
				error.code = -1;
				error.msg = "查询用户名失败";

				return error.code;
			}

			String userName = (String) userMap.get("name");
			String userEamil = (String) userMap.get("eamil");
			String userMobile = (String) userMap.get("mobile");

			// 发送站内信 尊敬的username:\n
			// 您投资的借款title,第repayPeriod期还款已经完成.<br/>paymentModeStr本期应得总额：
			// ￥recivedSum,其中本金部分为：hasP元,利息部分：hasI元,实得逾期罚息：hasLFI元<br/>扣除投资管理费：
			// ￥mFee元<br/>实得总额：￥msFee元
			TemplateStation station = new TemplateStation();
			station.id = Templets.M_INVEST_RECEIVE;

			if (station.status) {
				String mContent = station.content.replace("userName", userName);
				mContent = mContent.replace("title", this.bid.title);
				mContent = mContent.replace("repayPeriod", this.periods + "");
				mContent = mContent.replace("recivedSum", totalBalance + "");
				mContent = mContent.replace("hasP", receiveCorpus + "");
				mContent = mContent.replace("hasI", receiveInterest + "");
				mContent = mContent.replace("hasLFI", investOverdueFine + "");
				mContent = mContent.replace("mFee", manageFee + "");
				mContent = mContent.replace("msFee", totalBalance + "");
				TemplateStation.addMessageTask(investUserId, station.title,
						mContent);
			}

			// 发送邮件 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
			TemplateEmail email = new TemplateEmail();
			email.id = Templets.E_INVEST_RECEIVE;

			if (email.status) {
				String eContent = email.content.replace("userName", userName);
				eContent = eContent.replace("title", this.bid.title);
				eContent = eContent.replace("recivedSum", totalBalance + "");
				eContent = eContent.replace("hasP", receiveCorpus + "");
				eContent = eContent.replace("hasI", receiveInterest + "");
				eContent = eContent.replace("hasLFI", investOverdueFine + "");
				eContent = eContent.replace("mFee", manageFee + "");
				eContent = eContent.replace("msFee", totalBalance + "");
				TemplateEmail.addEmailTask(userEamil, email.title, eContent);
			}

			// 发送短信 尊敬的username:\n
			// 您投标的借款title,第repayPeriod期还款已经完成.\npaymentModeStr本期应得总额:
			// ￥recivedSum,其中本金部分为:hasP元,利息部分:hasI元,实得逾期罚息:hasLFI元\n扣除投资管理费:
			// ￥mFee元\n实得总额:￥msFee元
			TemplateSms sms = new TemplateSms();
			if (StringUtils.isNotBlank(userMobile)) {
				sms.id = Templets.S_INVEST_RECEIVE;

				if (sms.status) {
					String sContent = sms.content.replace("userName", userName);
					sContent = sContent.replace("title", this.bid.title);
					sContent = sContent.replace("repayPeriod", this.periods
							+ "");
					sContent = sContent
							.replace("recivedSum", totalBalance + "");
					sContent = sContent.replace("hasP", receiveCorpus + "");
					sContent = sContent.replace("hasI", receiveInterest + "");
					sContent = sContent.replace("hasLFI", investOverdueFine
							+ "");
					sContent = sContent.replace("mFee", manageFee + "");
					sContent = sContent.replace("msFee", totalBalance + "");
					TemplateSms.addSmsTask(userMobile, sContent);
				}
			}

			// 返回每个投资用户每期获得的投资本金和利息
			String userBalanceSql2 = "update t_users set balance = balance + ? where id = ?";
			Query userBalance2 = em.createQuery(userBalanceSql2)
					.setParameter(1, receiveCorpus + receiveInterest)
					.setParameter(2, investUserId);

			try {
				rows = userBalance2.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改投资用户的金额时：" + e.getMessage());
				error.code = -2;
				error.msg = "数据库异常，导致线下收款失败";

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据库异常，导致线下收款失败";
				JPA.setRollbackOnly();

				return error.code;

			}

			Map<String, Double> billForDetail = DealDetail.queryUserFund(
					investUserId, error);
			if (error.code < 0 || billForDetail == null) {
				JPA.setRollbackOnly();

				return -1;
			}

			double userFreeze = billForDetail.get("freeze");
			double userReceiveAmount = billForDetail.get("receive_amount");

			Double investerBalance2 = Bill.queryBalance(investUserId, error);

			DealDetail investDetail = new DealDetail(investUserId,
					DealType.OFFLINE_COLLECTION, receiveCorpus
							+ receiveInterest, investBillId, investerBalance2,
					userFreeze, userReceiveAmount, "第" + this._id + "号账单还款获得金额");

			// 添加线下收款的交易记录
			investDetail.addDealDetail(error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}

			// 减去每个投资用户每期的管理费
			String userBalanceSql3 = "update t_users set balance = balance - ? where id = ?";
			Query userBalance3 = em.createQuery(userBalanceSql3)
					.setParameter(1, manageFee).setParameter(2, investUserId);

			try {
				rows = userBalance3.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改投资用户的金额时：" + e.getMessage());
				error.code = -2;
				error.msg = "数据库异常，导致线下收款失败";

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据库异常，导致线下收款失败";
				JPA.setRollbackOnly();

				return error.code;

			}

			Double investerBalance10 = Bill.queryBalance(investUserId, error);
			DealDetail investFeeDetail = new DealDetail(investUserId,
					DealType.CHARGE_INVEST_FEE, manageFee, investBillId,
					investerBalance10, userFreeze, userReceiveAmount, "扣除第"
							+ this._id + "号账单理财管理费");

			// 添加扣除理财管理费的交易记录
			investFeeDetail.addDealDetail(error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}

			/* 添加CPS推广费 */
			User.rewardCPS(investUserId, manageFee, this._id, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}

			// 添加平台扣除理财管理费交易记录
			DealDetail.addPlatformDetail(DealType.INVEST_FEE, investBillId, -1,
					investUserId, DealType.ACCOUNT, manageFee, 1, "平台收取"
							+ this._id + "号 账单投资管理费", error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}

			if (overdueMark != Constants.BILL_NO_OVERDUE) {
				// 增加每个投资用户每期获得的罚息
				String userBalanceSql4 = "update t_users set balance = balance + ? where id = ?";
				Query userBalance4 = em.createQuery(userBalanceSql4)
						.setParameter(1, investOverdueFine)
						.setParameter(2, investUserId);

				try {
					rows = userBalance4.executeUpdate();
				} catch (Exception e) {
					e.printStackTrace();
					Logger.info("修改投资用户的金额时：" + e.getMessage());
					error.code = -2;
					error.msg = "数据库异常，导致线下收款失败";

					return error.code;
				}

				if (rows == 0) {
					error.code = -1;
					error.msg = "数据库异常，导致线下收款失败";
					JPA.setRollbackOnly();

					return error.code;

				}

				Double investerBalance3 = Bill
						.queryBalance(investUserId, error);
				DealDetail investOverdueFeeDetail = new DealDetail(
						investUserId, DealType.ADD_OVERDUE_FEE,
						investOverdueFine, investBillId, investerBalance3,
						userFreeze, userReceiveAmount, "第" + this._id
								+ "号账单还款获取逾期费");

				// 添加获取逾期费用的交易记录
				investOverdueFeeDetail.addDealDetail(error);
				if (error.code < 0) {
					JPA.setRollbackOnly();

					return -1;
				}
			}

			// 如果投资用户账户资金没有遭到非法改动，那么就更新其篡改标志，否则不更新
			if (investIsChange) {
				investDataTamperproof.id = investUserId;
				investDataTamperproof.updateSign(error);
			}
		}

		// 借款账单没有逾期
		if (status == Constants.NO_REPAYMENT
				&& overdueMark == Constants.BILL_NO_OVERDUE) {

			String updateBillSql = "update t_bills set status = ?, real_repayment_time = ?, real_repayment_corpus = ?, real_repayment_interest = ? where id = ? and status not in(?)";

			Query updateBill = em.createQuery(updateBillSql)
					.setParameter(1, Constants.NORMAL_REPAYMENT)
					.setParameter(2, DateUtil.currentDate())
					.setParameter(3, repaymentCorpus)
					.setParameter(4, repaymentInterest)
					.setParameter(5, this._id)
					.setParameter(6, Constants.NORMAL_REPAYMENT);

			try {
				rows = updateBill.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改借款账单数据时：" + e.getMessage());
				error.code = -3;
				error.msg = "数据库异常，导致线下收款失败";
				JPA.setRollbackOnly();

				return error.code;

			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据库异常，导致线下收款失败";
				JPA.setRollbackOnly();

				return error.code;

			}
		} else {
			// 逾期的借款账单
			String updateBillSql = "update t_bills set status = ?, real_repayment_time = ?, real_repayment_corpus = ?, real_repayment_interest = ?,"
					+ " overdue_fine = ? where id = ? and status not in(?)";

			Query updateBill = em.createQuery(updateBillSql)
					.setParameter(1, Constants.OVERDUE_PATMENT)
					.setParameter(2, DateUtil.currentDate())
					.setParameter(3, repaymentCorpus)
					.setParameter(4, repaymentInterest)
					.setParameter(5, repayOverdueFine)
					.setParameter(6, this._id)
					.setParameter(7, Constants.OVERDUE_PATMENT);

			try {
				rows = updateBill.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("修改借款账单数据时：" + e.getMessage());
				error.code = -3;
				error.msg = "数据库异常，导致线下收款失败";
				JPA.setRollbackOnly();

				return error.code;
			}

			if (rows == 0) {
				error.code = -1;
				error.msg = "数据库异常，导致线下收款失败";
				JPA.setRollbackOnly();

				return error.code;

			}

			// 逾期还款减去信用积分
			DealDetail.addCreditScore(this.bid.userId, -1, 0, this._id, "第"
					+ this._id + "号账单逾期还款扣除信用积分", error);
			if (error.code < 0) {
				JPA.setRollbackOnly();

				return -1;
			}
		}

		// 添加管理员操作日志
		DealDetail.supervisorEvent(supervisorId,
				SupervisorEvent.OFFLINE_COLLECTION, "第" + this._id
						+ "号账单管理员执行线下收款操作 ", error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return -1;
		}

		// Map<String, Double> billUserForDetail =
		// DealDetail.queryUserFund(this.bid.userId, error);
		// if(error.code < 0 || billUserForDetail == null) {
		// JPA.setRollbackOnly();
		//
		// return -1;
		// }
		//
		// double userBillFreeze = billUserForDetail.get("freeze");
		// double userBillReceiveAmount =
		// billUserForDetail.get("receive_amount");
		//
		// Double userBalance = this.queryBalance(this.bid.userId, error);
		// DealDetail billDetail = new DealDetail(this.bid.userId,
		// DealType.OFFLINE_COLLECTION, 0,
		// this._id, userBalance, userBillFreeze, userBillReceiveAmount,
		// "第"+this._id+"号账单线下还款");
		//
		// //添加获取逾期费用的交易记录
		// billDetail.addDealDetail(error);
		// if(error.code < 0) {
		// JPA.setRollbackOnly();
		//
		// return -1;
		// }

		Map<String, Object> userMap = new HashMap<String, Object>();

		String userSql = "select new Map(name as name, email as eamil, mobile as mobile) from t_users where id = ? ";

		try {
			userMap = GenericModel.find(userSql, this.bid.userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据用户id查询时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户名失败";

			return error.code;
		}

		String userName = (String) userMap.get("name");
		String userEamil = (String) userMap.get("eamil");
		String userMobile = (String) userMap.get("mobile");

		// 发送站内信 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
		TemplateStation station = new TemplateStation();
		station.id = Templets.M_SUCCESS_PAY;

		if (station.status) {
			String mContent = station.content.replace("userName", userName);
			mContent = mContent.replace("date", this.bid.time + "");
			mContent = mContent.replace("title", this.bid.title);
			mContent = mContent.replace("needSum", (repaymentCorpus
					+ repaymentInterest + repayOverdueFine)
					+ "");
			TemplateStation.addMessageTask(this.bid.userId, station.title,
					mContent);
		}

		// 发送邮件 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
		TemplateEmail email = new TemplateEmail();
		email.id = Templets.E_SUCCESS_PAY;

		if (email.status) {
			String eContent = email.content.replace("userName", userName);
			eContent = eContent.replace("date", this.bid.time + "");
			eContent = eContent.replace("title", this.bid.title);
			eContent = eContent.replace("needSum", (repaymentCorpus
					+ repaymentInterest + repayOverdueFine)
					+ "");
			TemplateEmail.addEmailTask(userEamil, email.title, eContent);
		}

		// 发送短信 尊敬的username:\n [date] 您在晓风安全网贷系统6.0对标的【title】还款了￥needSum 元
		TemplateSms sms = new TemplateSms();
		if (StringUtils.isNotBlank(userMobile)) {
			sms.id = Templets.S_SUCCESS_PAY;

			if (sms.status) {
				String eContent = sms.content.replace("userName", userName);
				eContent = eContent.replace("date", this.bid.time + "");
				eContent = eContent.replace("title", this.bid.title);
				eContent = eContent.replace("needSum", (repaymentCorpus
						+ repaymentInterest + repayOverdueFine)
						+ "");
				TemplateSms.addSmsTask(userMobile, eContent);
			}
		}

		error.msg = "线下收款操作成功";
		error.code = 0;
		return error.code;
	}

	/**
	 * 债权转让成功后投资账单的转换
	 * 
	 * @param bidId
	 * @param originalInvestUserId
	 * @param presentInvestUserId
	 * @param info
	 *            -1 失败 0成功
	 * @return
	 */
	public static int debtTransfer(String paymentMerBillNo, long bidId,
			long originalInvestUserId, long presentInvestUserId,
			long originalInvestId, long presentInvestId, ErrorInfo error) {
		error.clear();
		EntityManager em = JPA.em();

		String sql = "update t_bill_invests set user_id =? ,invest_id = ? where bid_id = ? and user_id = ? and invest_id = ? and status not in (?,?,?,?)";
		Query query = em.createQuery(sql).setParameter(1, presentInvestUserId)
				.setParameter(2, presentInvestId).setParameter(3, bidId)
				.setParameter(4, originalInvestUserId)
				.setParameter(5, originalInvestId)
				.setParameter(6, Constants.NORMAL_RECEIVABLES)
				.setParameter(7, Constants.ADVANCE_PRINCIIPAL_RECEIVABLES)
				.setParameter(8, Constants.OVERDUE_RECEIVABLES)
				.setParameter(9, Constants.FOR_DEBT_MARK);

		if (Constants.IPS_ENABLE) {
			sql = "update t_bill_invests set user_id =? ,invest_id = ?, mer_bill_no = ? where bid_id = ? and user_id = ? and invest_id = ? and status not in (?,?,?,?)";
			query = em.createQuery(sql).setParameter(1, presentInvestUserId)
					.setParameter(2, presentInvestId)
					.setParameter(3, paymentMerBillNo).setParameter(4, bidId)
					.setParameter(5, originalInvestUserId)
					.setParameter(6, originalInvestId)
					.setParameter(7, Constants.NORMAL_RECEIVABLES)
					.setParameter(8, Constants.ADVANCE_PRINCIIPAL_RECEIVABLES)
					.setParameter(9, Constants.OVERDUE_RECEIVABLES)
					.setParameter(10, Constants.FOR_DEBT_MARK);
		}

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("更新投资账单id时：" + e.getMessage());
			error.code = -1;
			error.msg = "债权转让成功后，数据库异常导致投资账单转换为新债权人失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		if (rows == 0) {
			error.code = -1;
			error.msg = "更新投资账单数据操作没有执行";
			JPA.setRollbackOnly();

			return error.code;

		}

		return 0;
	}

	/**
	 * 在投资账单里复制符合条件的记录
	 * 
	 * @param bidId
	 * @param userId
	 * @param error
	 * @return
	 */
	public static int investBillsTransfer(long bidId, long userId,
			long investId, ErrorInfo error) {
		error.clear();

		String sql = "INSERT INTO t_bill_invests(user_id,invest_id, bid_id,mer_bill_no,periods,title,receive_time,receive_corpus,"
				+ "receive_interest,status,overdue_fine,real_receive_time,real_receive_corpus,real_receive_interest"
				+ ") SELECT user_id,invest_id, bid_id,mer_bill_no,periods,title,receive_time,receive_corpus,receive_interest,-7,"
				+ "overdue_fine,real_receive_time,real_receive_corpus,real_receive_interest FROM t_bill_invests "
				+ "WHERE user_id = ? AND bid_id = ? AND invest_id = ? and status not in (?,?,?)";

		try {
			JPA.em().createNativeQuery(sql).setParameter(1, userId)
					.setParameter(2, bidId).setParameter(3, investId)
					.setParameter(4, Constants.NORMAL_RECEIVABLES)
					.setParameter(5, Constants.ADVANCE_PRINCIIPAL_RECEIVABLES)
					.setParameter(6, Constants.OVERDUE_RECEIVABLES)
					.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("更新投资账单id时：" + e.getMessage());
			error.code = -1;
			error.msg = "债权转让成功后，数据库异常导致投资账单转换为新债权人失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		return 0;
	}

	/**
	 * 每天对逾期的借款账单重新计算逾期罚款并对其投资账单做对应的的修改
	 * 
	 * @param info
	 * @throws ParseException
	 */
	public int upadateOverdueFee(ErrorInfo error) {
		error.clear();
		EntityManager em = JPA.em();

		// 1.修改借款账单逾期生成的数据
		String updateBillSql = "update t_bills t1,(select c.id, round((c.days * c.amount * c._value/100),2) overdue_fee from"
				+ " (select a.id, b._value, DATEDIFF(now(), a.repayment_time) days, (a.repayment_corpus + a.repayment_interest)"
				+ " amount from t_bills a join t_system_options b where b._key = 'overdue_fee' and a.`status` in(-1,-2) and"
				+ " a.overdue_mark in(-1,-2,-3) group by a.id) c) t2 set t1.overdue_fine = t2.overdue_fee where t1.id = t2.id";

		Query updateBill = em.createNativeQuery(updateBillSql);

		try {
			updateBill.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改借款账单逾期生成的数据时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致修改借款账单逾期生成的数据失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		// 2.修改理财账单逾期生成的数据
		String updateInvestBillSql = "update t_bill_invests t1,(select t3.id, round((t4.overdue_fine * ifnull(t6.amount,0) / t5.amount), 2)"
				+ " check_fine, t3.periods, t3.bid_id from (select b.bid_id, b.periods, b.overdue_fine from t_bills b where "
				+ "b.overdue_mark in(-1,-2,-3) and b.status in(-1,-2)) t4 left join (select a.invest_id, a.id, a.periods, a.bid_id, "
				+ "a.user_id from t_bill_invests a) t3 on t3.bid_id = t4.bid_id and t3.periods = t4.periods left join "
				+ "(select c.id, c.amount from t_bids c) t5 on t3.bid_id = t5.id left join (select d.id, d.bid_id, d.user_id, "
				+ "d.amount from t_invests d) t6 on t3.bid_id = t6.bid_id and t3.user_id = t6.user_id and t3.invest_id = t6.id) t2 set "
				+ "t1.overdue_fine = check_fine where t1.id = t2.id";

		Query updateInvestBill = em.createNativeQuery(updateInvestBillSql);

		try {
			updateInvestBill.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改理财账单逾期生成的数据时：" + e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致修改理财账单逾期生成的数据失败";
			JPA.setRollbackOnly();

			return error.code;
		}

		error.code = 0;
		return error.code;
	}

	/**
	 * 查询某个标的所有账单
	 * 
	 * @param pageBean
	 *            分页对象
	 * @param userId
	 *            用户ID
	 * @param bidId
	 *            标ID
	 * @param error
	 *            信息值
	 * @return List<v_bill_loan>
	 */
	public static List<v_bill_loan> queryMyLoanBills(
			PageBean<v_bill_loan> pageBean, long userId, long bidId,
			ErrorInfo error) {
		error.clear();

		int count = -1;
		String condition = null;

		if (userId > 0)
			condition = "id = " + userId;
		else if (bidId > 0)
			condition = "bid_id = " + bidId;
		else
			condition = "id = " + userId + "bid_id = " + bidId;

		try {
			count = (int) GenericModel.count(condition);
		} catch (Exception e) {
			Logger.error("账单->查询我的借款账单,查询总行数:" + e.getMessage());
			error.msg = "加载我的借款账户失败!";

			return null;
		}

		if (count < 1)
			return new ArrayList<v_bill_loan>();

		pageBean.totalCount = count;

		try {
			return GenericModel.find(condition).fetch(pageBean.currPage,
					pageBean.pageSize);
		} catch (Exception e) {
			Logger.error("账单->查询我的借款账单:" + e.getMessage());
			error.msg = "加载我的借款账户失败!";

			return null;
		}
	}

	/**
	 * 查询我所有的借款账单
	 * 
	 * @param error
	 * @return
	 */
	public static List<v_bill_loan> queryMyAllLoanBills(ErrorInfo error) {
		error.clear();

		List<v_bill_loan> bills = null;

		try {
			bills = GenericModel.find("user_id = ?", User.currUser().id).fetch();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询我所有的借款账单:" + e.getMessage());
			error.code = -1;
			error.msg = "查询我所有的借款账单失败!";

			return null;
		}

		return bills;
	}

	/**
	 * 查询用户的所有借款账单
	 * 
	 * @param pageBean
	 *            分页对象
	 * @param error
	 *            信息值
	 * @return List<v_bill_loan>
	 */
	public static PageBean<v_bill_loan> queryMyLoanBills(long userId,
			int payType, int isOverType, int keyType, String keyStr,
			int currPageStr, int pageSizeNum, ErrorInfo error) {
		error.clear();

		int count = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.FIVE;

		if (pageSizeNum != 0) {
			pageSize = pageSizeNum;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		List<v_bill_loan> bills = new ArrayList<v_bill_loan>();
		List<Object> values = new ArrayList<Object>();
		StringBuffer conditions = new StringBuffer("1=1 ");

		if ((payType < 0) || (payType > 2)) {
			payType = 0;
		}

		if ((isOverType < 0) || (isOverType > 2)) {
			isOverType = 0;
		}

		if ((keyType < 0) || (keyType > 3)) {
			keyType = 0;
		}

		if (currPageStr != 0) {
			currPage = currPageStr;
		}

		if (StringUtils.isNotBlank(keyStr)) {
			conditions.append(Constants.LOAN_BILL_ALL[keyType]);
			values.add("%" + keyStr.trim() + "%");
		}

		conditions.append(Constants.LOAN_BILL_REPAYMENT[payType]);
		conditions.append(Constants.LOAN_BILL_OVDUE[isOverType]);
		conditions.append("and user_id = " + userId);

		conditionMap.put("payType", payType);
		conditionMap.put("isOverType", isOverType);
		conditionMap.put("keyType", keyType);
		conditionMap.put("key", keyStr);

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			bills = GenericModel.find(conditions.toString(), values.toArray())
					.fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询我的借款账单:" + e.getMessage());
			error.msg = "加载我的借款账单失败!";

			return null;
		}

		PageBean<v_bill_loan> page = new PageBean<v_bill_loan>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = bills;

		return page;
	}

	/**
	 * 我的账单详情
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static v_bill_detail queryBillDetails(long id, long userId,
			ErrorInfo error) {
		error.clear();

		v_bill_detail billDetail = new v_bill_detail();
		try {
			billDetail = GenericModel.find("id = ? and user_id = ?", id,
					userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询我的借款账单详情时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询我的借款账单详情失败";

			return null;
		}

		if (null == billDetail) {
			error.code = -1;
			error.msg = "由于数据库异常，查询我的借款账单详情失败";

			return null;
		}

		return billDetail;
	}

	/**
	 * 我的账单详情
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static v_bill_detail queryBillDetails(long id, ErrorInfo error) {
		error.clear();

		v_bill_detail billDetail = new v_bill_detail();
		try {
			billDetail = GenericModel.findById(id);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询我的借款账单详情时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询我的借款账单详情失败";

			return null;
		}

		if (null == billDetail) {
			error.code = -1;
			error.msg = "由于数据库异常，查询我的借款账单详情失败";

			return null;
		}

		return billDetail;
	}

	/**
	 * 我的借款账单——-历史还款情况
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_repayment_record> queryBillReceivables(
			long bidid, int currPage, int pageSize, ErrorInfo error) {
		error.clear();

		List<v_bill_repayment_record> bills = new ArrayList<v_bill_repayment_record>();
		PageBean<v_bill_repayment_record> page = new PageBean<v_bill_repayment_record>();
		page.pageSize = Constants.TWO;
		page.currPage = Constants.ONE;

		if (currPage != 0) {
			page.currPage = currPage;
		}

		if (pageSize != 0) {
			page.pageSize = pageSize;
		}

		String sql = "select new v_bill_repayment_record( a.current_pay_amount as current_pay_amount, a.title as title,"
				+ "a.overdue_mark as overdue_mark, a.status as status, "
				+ "a.real_repayment_time as real_repayment_time, a.repayment_time as "
				+ "repayment_time )from v_bill_repayment_record as a where a.bid_id = ?";
		try {
			page.totalCount = (int) GenericModel.count("bid_id = ?",
					bidid);
			bills = GenericModel.find(sql, bidid).fetch(
					page.currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询我的借款账单收款情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询我的借款账单收款情况失败";

			return null;
		}

		page.page = bills;

		return page;
	}

	/**
	 * 我的信用等级——-正常还款积分明细
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_user_detail_credit_score_normal_repayment> queryBillNormalPayment(
			long userId, int currPage, ErrorInfo error) {
		error.clear();

		List<v_user_detail_credit_score_normal_repayment> bills = new ArrayList<v_user_detail_credit_score_normal_repayment>();
		PageBean<v_user_detail_credit_score_normal_repayment> page = new PageBean<v_user_detail_credit_score_normal_repayment>();
		page.pageSize = 2;
		page.currPage = currPage;

		String sql = "select a.bill_id as bill_id, a.title as title,"
				+ "a.period as period, a.score as score, "
				+ "a.audit_time as audit_time"
				+ " from v_bill_normal_repayment_score_details as a where a.id = ?";
		try {
			page.totalCount = (int) GenericModel
					.count(sql, userId);
			bills = GenericModel.find(sql,
					userId).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询我的正常还款积分明细时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询正常还款积分明细失败";

			return null;
		}

		page.page = bills;

		return page;
	}

	/**
	 * 我的信用等级——-逾期账单积分明细
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_user_detail_credit_score_overdue> queryBillOverdueMark(
			long userId, int currPage, ErrorInfo error) {
		error.clear();

		List<v_user_detail_credit_score_overdue> bills = new ArrayList<v_user_detail_credit_score_overdue>();
		PageBean<v_user_detail_credit_score_overdue> page = new PageBean<v_user_detail_credit_score_overdue>();
		page.pageSize = 2;
		page.currPage = currPage;

		String sql = "select a.bill_id as bill_id, a.title as title,"
				+ "a.period as period, a.overdue_marking as overdue_marking, "
				+ "a.audit_time as audit_time"
				+ " from v_bill_overdue_score_details as a where a.id = ?";
		try {
			page.totalCount = (int) GenericModel.count(
					sql, userId);
			bills = GenericModel.find(sql, userId).fetch(
					currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询我的逾期账单积分明细时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询逾期账单积分明细失败";

			return null;
		}

		page.page = bills;

		return page;
	}

	/**
	 * 我的会员账单--本月到期账单
	 * 
	 * @param supervisorId
	 * @param yearStr
	 * @param monthStr
	 * @param typeStr
	 * @param key
	 * @param currPageStr
	 * @param pageSizeStr
	 * @param error
	 * @return
	 */
	public static PageBean<v_bill_month_maturity> queryBillMonthMaturity(
			long supervisorId, String yearStr, String monthStr, String typeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		// if(year < 0 || year > 5) {
		// year = 0;
		// }
		//
		if (month < 0 || month > 12) {
			month = 0;
		}

		if (type < 0 || type > 3) {
			type = 0;
		}

		if (orderType < 0 || orderType > 10) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
			// values.add(Constants.TIME_YEAR[year]);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
			// values.add(Constants.TIME_MONTH[month]);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}
		}

		conditions.append("and supervisor_id = ? ");
		values.add(supervisorId);

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_Maturity[orderType]);
		}

		List<v_bill_month_maturity> bills = new ArrayList<v_bill_month_maturity>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			bills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询本月到期账单情况时：" + e.getMessage());
			error.code = -3;
			error.msg = "";

			return null;
		}

		PageBean<v_bill_month_maturity> page = new PageBean<v_bill_month_maturity>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 查询本月到期账单，用于写入excel
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */

	public static List<v_bill_department_month_maturity> queryBillMaturity() {

		ErrorInfo error = new ErrorInfo();

		List<v_bill_department_month_maturity> bills = new ArrayList<v_bill_department_month_maturity>();

		try {
			bills = GenericModel.findAll();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询部门账单管理--本月到期账单情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询部门账单管理--本月到期账单情况失败";

			return null;
		}

		return bills;
	}

	/**
	 * 部门账单管理--本月到期账单
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_department_month_maturity> queryBillDepartmentMonthMaturity(
			long supervisorId, String yearStr, String monthStr, String typeStr,
			String key, String kefuStr, String orderTypeStr,
			String currPageStr, String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int kefu = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(kefuStr)) {
			kefu = Integer.parseInt(kefuStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		// if(year < 0 || year > 5) {
		// year = 0;
		// }
		//
		if (month < 0 || month > 12) {
			month = 0;
		}

		if (type < 0 || type > 3) {
			type = 0;
		}

		if (orderType < 0 || orderType > 10) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("kefu", kefu);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
			// values.add(Constants.TIME_YEAR[year]);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
			// values.add(Constants.TIME_MONTH[month]);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}
		}

		if (kefu != 0) {
			conditions.append("and supervisor_id = ? ");
			values.add(kefu);
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_Maturity[orderType]);
		}

		List<v_bill_department_month_maturity> bills = new ArrayList<v_bill_department_month_maturity>();
		int count = 0;

		try {
			count = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			bills = GenericModel.find(
					conditions.toString(), values.toArray()).fetch(currPage,
					pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询部门账单管理--本月到期账单情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询部门账单管理--本月到期账单情况失败";

			return null;
		}

		PageBean<v_bill_department_month_maturity> page = new PageBean<v_bill_department_month_maturity>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 我的会员账单--逾期账单
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_overdue> queryBillOverdue(long supervisorId,
			String yearStr, String monthStr, String typeStr, String key,
			String orderTypeStr, String currPageStr, String pageSizeStr,
			ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		// if(year < 0 || year > 5) {
		// year = 0;
		// // error.code = -1;
		// // error.msg = "选择的年份不在范围内";
		// //
		// // return null;
		// }

		if (month < 0 || month > 12) {
			month = 0;
			// error.code = -2;
			// error.msg = "选择的月份不在范围内";
			//
			// return null;
		}

		if (type < 0 || type > 3) {
			type = 0;
			// error.code = -3;
			// error.msg = "选择的查询类别不在范围内";
			//
			// return null;
		}

		if (orderType < 0 || orderType > 6) {
			orderType = 0;
			// error.code = -3;
			// error.msg = "选择的排序类别不在范围内";
			//
			// return null;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
			// values.add(Constants.TIME_YEAR[year]);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
			// values.add(Constants.TIME_MONTH[month]);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}

		}

		conditions.append("and supervisor_id = ? ");
		values.add(supervisorId);

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_Maturity[orderType]);
		}

		List<v_bill_overdue> bills = new ArrayList<v_bill_overdue>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			bills = GenericModel
					.find(conditions.toString(), values.toArray()).fetch(
							currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询我的会员账单--逾期账单情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询我的会员账单--逾期账单情况失败";

			return null;
		}

		PageBean<v_bill_overdue> page = new PageBean<v_bill_overdue>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 部门账单管理-逾期账单
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_department_overdue> queryBillDepartmentOverdue(
			long supervisorId, String yearStr, String monthStr, String typeStr,
			String key, String kefuStr, String orderTypeStr,
			String currPageStr, String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int kefu = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(kefuStr)) {
			kefu = Integer.parseInt(kefuStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		// if(year < 0 || year > 5) {
		// year = 0;
		// }

		if (month < 0 || month > 12) {
			month = 0;
		}

		if (type < 0 || type > 3) {
			type = 0;
		}

		if (orderType < 0 || orderType > 10) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("kefu", kefu);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
			// values.add(Constants.TIME_YEAR[year]);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
			// values.add(Constants.TIME_MONTH[month]);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}
		}

		if (kefu != 0) {
			conditions.append("and supervisor_id = ? ");
			values.add(kefu);
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_Maturity[orderType]);
		}

		List<v_bill_department_overdue> bills = new ArrayList<v_bill_department_overdue>();
		int count = 0;

		try {
			count = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			bills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询部门账单管理-逾期账单情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询部门账单管理-逾期账单情况失败";

			return null;
		}

		PageBean<v_bill_department_overdue> page = new PageBean<v_bill_department_overdue>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 我的会员账单---已还款账单列表
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_haspayed> queryBillHasPayed(
			long supervisorId, String yearStr, String monthStr, String typeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		// if(year < 0 || year > 5) {
		// year = 0;
		// // error.code = -1;
		// // error.msg = "选择的年份不在范围内";
		// //
		// // return null;
		// }

		if (month < 0 || month > 12) {
			month = 0;
			// error.code = -2;
			// error.msg = "选择的月份不在范围内";
			//
			// return null;
		}

		if (type < 0 || type > 3) {
			type = 0;
			// error.code = -3;
			// error.msg = "选择的查询类别不在范围内";
			//
			// return null;
		}

		if (orderType < 0 || orderType > 6) {
			orderType = 0;
			// error.code = -3;
			// error.msg = "选择的排序类别不在范围内";
			//
			// return null;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
			// values.add(Constants.TIME_YEAR[year]);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
			// values.add(Constants.TIME_MONTH[month]);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}

		}

		conditions.append("and supervisor_id = ? ");
		values.add(supervisorId);

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_PAID[orderType]);
		}

		List<v_bill_haspayed> bills = new ArrayList<v_bill_haspayed>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			bills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询我的会员账单---已还款账单列表情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询我的会员账单---已还款账单列表情况失败";

			return null;
		}

		PageBean<v_bill_haspayed> page = new PageBean<v_bill_haspayed>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 部门会员账单---已还款账单列表
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_department_haspayed> queryBillDepartmentHasPayed(
			long supervisorId, String yearStr, String monthStr, String typeStr,
			String key, String kefuStr, String orderTypeStr,
			String currPageStr, String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int kefu = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(kefuStr)) {
			kefu = Integer.parseInt(kefuStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		// if(year < 0 || year > 5) {
		// year = 0;
		// }

		if (month < 0 || month > 12) {
			month = 0;
		}

		if (type < 0 || type > 3) {
			type = 0;
		}

		if (orderType < 0 || orderType > 10) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("kefu", kefu);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
			// values.add(Constants.TIME_YEAR[year]);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
			// values.add(Constants.TIME_MONTH[month]);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}
		}

		if (kefu != 0) {
			conditions.append("and supervisor_id = ? ");
			values.add(kefu);
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_PAID[orderType]);
		}

		List<v_bill_department_haspayed> bills = new ArrayList<v_bill_department_haspayed>();
		int count = 0;

		try {
			count = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			bills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询我的会员账单---已还款账单列表情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询我的会员账单---已还款账单列表情况失败";

			return null;
		}

		PageBean<v_bill_department_haspayed> page = new PageBean<v_bill_department_haspayed>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 应收账单管理--待收款借款账单列表
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_receiving> queryBillReceiving(
			long supervisorId, String yearStr, String monthStr, String typeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (month < 0 || month > 12) {
			month = 0;
		}

		if (type < 0 || type > 3) {
			type = 0;
		}

		if (orderType < 0 || orderType > 6) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_Maturity[orderType]);
		}

		List<v_bill_receiving> bills = new ArrayList<v_bill_receiving>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			bills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询应收账单管理--待收款借款账单列表情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询应收账单管理--待收款借款账单列表情况失败";

			return null;
		}

		PageBean<v_bill_receiving> page = new PageBean<v_bill_receiving>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 应收账单管理--逾期账单列表
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_receiving_overdue> queryBillReceivingOverdue(
			long supervisorId, String yearStr, String monthStr, String typeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (month < 0 || month > 12) {
			month = 0;
		}

		if (type < 0 || type > 3) {
			type = 0;
		}

		if (orderType < 0 || orderType > 10) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_OVERDUE[orderType]);
		}

		List<v_bill_receiving_overdue> bills = new ArrayList<v_bill_receiving_overdue>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			bills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询应收账单管理--逾期账单列表情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询应收账单管理--逾期账单列表情况失败";

			return null;
		}

		PageBean<v_bill_receiving_overdue> page = new PageBean<v_bill_receiving_overdue>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 应收账单管理--已收款借款账单列表
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_has_received> queryBillHasReceived(
			long supervisorId, String yearStr, String monthStr, String typeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (month < 0 || month > 12) {
			month = 0;
		}

		if (type < 0 || type > 3) {
			type = 0;
		}

		if (orderType < 0 || orderType > 10) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_RECEIVE[orderType]);
		}

		List<v_bill_has_received> bills = new ArrayList<v_bill_has_received>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			bills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询应收账单管理--逾期账单列表情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询应收账单管理--逾期账单列表情况失败";

			return null;
		}

		PageBean<v_bill_has_received> page = new PageBean<v_bill_has_received>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 应收账单管理--应收款借款账单统计表
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_receviable_statistical> queryBillReceivedStatical(
			long supervisorId, String yearStr, String monthStr,
			String orderTypeStr, String currPageStr, String pageSizeStr,
			ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (month < 0 || month > 12) {
			month = 0;
		}

		if (orderType < 0 || orderType > 10) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_RECEVIABLE[orderType]);
		}

		List<v_bill_receviable_statistical> bills = new ArrayList<v_bill_receviable_statistical>();
		int count = 0;

		try {
			count = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			bills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询应收账单管理--应收款借款账单统计表情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询应收账单管理--应收款借款账单统计表情况失败";

			return null;
		}

		PageBean<v_bill_receviable_statistical> page = new PageBean<v_bill_receviable_statistical>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 应付账单管理--待付款理财账单列表
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_invests_pending_payment> queryBillInvestPending(
			long supervisorId, String yearStr, String monthStr, String typeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (month < 0 || month > 12) {
			month = 0;
		}

		if (type < 0 || type > 3) {
			type = 0;
		}

		if (orderType < 0 || orderType > 10) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_REPAYMENT[orderType]);
		}

		List<v_bill_invests_pending_payment> bills = new ArrayList<v_bill_invests_pending_payment>();
		int count = 0;

		try {
			count = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			bills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询应收账单管理--已收款借款账单列表情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询应收账单管理--已收款借款账单列表情况失败";

			return null;
		}

		PageBean<v_bill_invests_pending_payment> page = new PageBean<v_bill_invests_pending_payment>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 应付账单管理--逾期未付理财账单列表
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_invests_overdue_unpaid> queryBillOverdueUnpaid(
			long supervisorId, String yearStr, String monthStr, String typeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (month < 0 || month > 12) {
			month = 0;
		}

		if (type < 0 || type > 3) {
			type = 0;
		}

		if (orderType < 0 || orderType > 10) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_REPAYMENT[orderType]);
		}

		List<v_bill_invests_overdue_unpaid> bills = new ArrayList<v_bill_invests_overdue_unpaid>();
		int count = 0;

		try {
			count = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			bills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询应付账单管理--逾期未付理财账单列表情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询应付账单管理--逾期未付理财账单列表情况失败";

			return null;
		}

		PageBean<v_bill_invests_overdue_unpaid> page = new PageBean<v_bill_invests_overdue_unpaid>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 应付账单管理--已付款理财账单列表
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_invests_paid> queryBillInvestPaid(
			long supervisorId, String yearStr, String monthStr, String typeStr,
			String key, String paidTypeStr, String orderTypeStr,
			String currPageStr, String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int paidType = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(paidTypeStr)) {
			paidType = Integer.parseInt(paidTypeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (month < 0 || month > 12) {
			month = 0;
		}

		if (type < 0 || type > 3) {
			type = 0;
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		if (orderType < 0 || orderType > 6) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("paidType", paidType);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}
		}

		if (paidType != 0) {
			conditions.append(Constants.BILL_ORDER_STATUS[paidType]);
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.INVEST_BILL_HAS_PAID[orderType]);
		}

		List<v_bill_invests_paid> bills = new ArrayList<v_bill_invests_paid>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			bills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询应付账单管理--已付款理财账单列表情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询应付账单管理--已付款理财账单列表情况失败";

			return null;
		}

		PageBean<v_bill_invests_paid> page = new PageBean<v_bill_invests_paid>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 应付账单管理--本金垫付理财账单列表
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_invests_principal_advances> queryBillPrincipalAdvances(
			long supervisorId, String yearStr, String monthStr, String typeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int type = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (month < 0 || month > 12) {
			month = 0;
		}

		if (type < 0 || type > 3) {
			type = 0;
		}

		if (orderType < 0 || orderType > 10) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append(Constants.C_TYPE[type]);

			if (type == 0) {
				values.add("%" + key + "%");
				values.add("%" + key + "%");
				values.add("%" + key + "%");
			} else {
				values.add("%" + key + "%");
			}
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_REPAYMENT[orderType]);
		}

		List<v_bill_invests_principal_advances> bills = new ArrayList<v_bill_invests_principal_advances>();
		int count = 0;

		try {
			count = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			bills = GenericModel.find(
					conditions.toString(), values.toArray()).fetch(currPage,
					pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询应付账单管理--本金垫付理财账单列表情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询应付账单管理--本金垫付理财账单列表情况失败";

			return null;
		}

		PageBean<v_bill_invests_principal_advances> page = new PageBean<v_bill_invests_principal_advances>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 应付账单管理--理财情况统计
	 * 
	 * @param id
	 * @param currPage
	 * @param info
	 * @return
	 */
	public static PageBean<v_bill_invests_payables_statistics> queryBillInvestStatistics(
			long supervisorId, String yearStr, String monthStr,
			String orderTypeStr, String currPageStr, String pageSizeStr,
			ErrorInfo error) {
		error.clear();

		int year = 0;
		int month = 0;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (month < 0 || month > 12) {
			month = 0;
		}

		if (orderType < 0 || orderType > 8) {
			orderType = 0;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BILL_ORDER_INVEST[orderType]);
		}

		List<v_bill_invests_payables_statistics> bills = new ArrayList<v_bill_invests_payables_statistics>();
		int count = 0;

		try {
			count = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			bills = GenericModel.find(
					conditions.toString(), values.toArray()).fetch(currPage,
					pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询应收账单管理--应收款借款账单统计表情况时：" + e.getMessage());
			error.code = -1;
			error.msg = "由于数据库异常，查询应收账单管理--应收款借款账单统计表情况失败";

			return null;
		}

		PageBean<v_bill_invests_payables_statistics> page = new PageBean<v_bill_invests_payables_statistics>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 账单提醒--最新的还款账单(借款子账户首页)
	 * 
	 * @param error
	 *            信息值
	 * @return
	 */
	public static List<v_bill_recently_pending> queryRecentlyBills(
			ErrorInfo error) {
		error.clear();

		User user = User.currUser();

		try {
			return GenericModel.find("user_id ", user.id).fetch(
					Constants.RECENTLY_REPAYMENT_BILL_COUNT);
		} catch (Exception e) {
			Logger.error("账单提醒--最新的还款账单:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载最新的还款账单列表失败!";

			return null;
		}
	}

	/**
	 * 还款计算器明细
	 */
	public static List<Map<String, Object>> repaymentCalculate(double amount,
			double apr, int period, int periodUnit, int repaymentType) {
		double monthRate = 0;// 月利率
		double monPay = 0;// 每个月要还的金额
		double monPayInterest = 0;// 每个月利息（如果是天标的话，就是所有的利息）
		double monPayAmount = 0;// 每个月本金
		double totalAmount = 0;// 总共要还的金额
		double payRemain = 0;// 剩余要还的金额
		double payAmount = 0;// 加起来付了多少钱
		double totalInterest = 0;// 总利息
		double payAmountAdd = 0;
		double stillPay = 0;

		int deadline = period; // 借款标期限
		double borrowSum = amount; // 借款金额
		monthRate = Double.valueOf(apr * 0.01) / 12.0;// 通过年利率得到月利率
		Map<String, Object> payMap = null;
		List<Map<String, Object>> payList = null;

		// 按日结算
		if (periodUnit == 1) {

			monPayInterest = Arith.mul(borrowSum, monthRate);
			totalInterest = monPayInterest * deadline / 30;
			totalAmount = borrowSum + totalInterest;
			payList = new ArrayList<Map<String, Object>>();
			payMap = new HashMap<String, Object>();

			payMap.put("period", 1);
			payMap.put("monPay", Arith.round(totalAmount, 2));
			payMap.put("monPayAmount", Arith.round(borrowSum, 2));
			payMap.put("monPayInterest", Arith.round(totalInterest, 2));
			payMap.put("stillPay", Arith.round(stillPay, 2));
			payList.add(payMap);

			return payList;
		} else {
			// 按月还款、等额本息
			if (repaymentType == Constants.PAID_MONTH_EQUAL_PRINCIPAL_INTEREST) {

				if (periodUnit == -1 || periodUnit == 0) {// 判断标类(年标，月标，天标)

					if (periodUnit == -1) {// 如果为年标，那么传过来的借款期限都乘以12
						deadline = deadline * 12;
					}

					monPay = Double.valueOf(Arith.mul(borrowSum, monthRate)
							* Math.pow((1 + monthRate), deadline))
							/ Double.valueOf(Math
									.pow((1 + monthRate), deadline) - 1);// 每个月要还的本金和利息
					monPay = Arith.round(monPay, 2);
					amount = borrowSum;
					totalAmount = Arith.mul(monPay, deadline);// 总共要还的金额
					payRemain = Arith.round(totalAmount, 2);
					payList = new ArrayList<Map<String, Object>>();

					for (int n = 1; n <= deadline; n++) {
						monPayInterest = Arith.round(
								Arith.mul(amount, monthRate), 2);// 每个月利息
						monPayAmount = Arith.round(
								Arith.sub(monPay, monPayInterest), 2);// 每个月本金
						amount = Arith
								.round(Arith.sub(amount, monPayAmount), 2);

						if (n == deadline) {
							monPay = payRemain;
							monPayAmount = borrowSum - payAmount;
							monPayInterest = monPay - monPayAmount;
						}

						payAmount += monPayAmount;
						payRemain = Arith.sub(payRemain, monPay);

						payAmountAdd += monPayAmount + monPayInterest;
						stillPay = totalAmount - payAmountAdd;
						payMap = new HashMap<String, Object>();

						payMap.put("period", n);
						payMap.put("monPay", monPay);
						payMap.put("monPayAmount", Arith.round(monPayAmount, 2));
						payMap.put("monPayInterest",
								Arith.round(monPayInterest, 2));
						payMap.put("stillPay", Arith.round(stillPay, 2));
						payList.add(payMap);
					}
				}
				return payList;
			}

			// 按月付息、一次还款
			if (repaymentType == Constants.PAID_MONTH_ONCE_REPAYMENT) {
				monPayInterest = Arith.mul(borrowSum, monthRate);

				if (periodUnit == -1 || periodUnit == 0) {

					if (periodUnit == -1) {
						deadline = deadline * 12;
					}

					totalInterest = Arith.mul(Arith.mul(borrowSum, monthRate),
							deadline);
					payList = new ArrayList<Map<String, Object>>();

					for (int n = 1; n <= deadline; n++) {

						if (n == deadline) {
							monPayAmount = borrowSum;
						} else {
							monPayAmount = 0.00;
						}

						payAmountAdd += monPayAmount + monPayInterest;
						stillPay = totalInterest + amount - payAmountAdd;
						monPay = monPayAmount + monPayInterest;
						payMap = new HashMap<String, Object>();

						payMap.put("period", n);
						payMap.put("monPay", Arith.round(monPay, 2));
						payMap.put("monPayAmount", Arith.round(monPayAmount, 2));
						payMap.put("monPayInterest",
								Arith.round(monPayInterest, 2));
						payMap.put("stillPay", Arith.round(stillPay, 2));
						payList.add(payMap);
					}
				}

				return payList;
			}

			// 一次还款
			if (repaymentType == Constants.ONCE_REPAYMENT) {
				if (periodUnit == -1 || periodUnit == 0) {
					if (periodUnit == -1) {
						deadline = deadline * 12;
					}

					monPayInterest = Arith.mul(borrowSum, monthRate);
					totalInterest = monPayInterest * deadline;
					totalAmount = borrowSum + totalInterest;
					payList = new ArrayList<Map<String, Object>>();
					payMap = new HashMap<String, Object>();

					payMap.put("period", 1);
					payMap.put("monPay", Arith.round(totalAmount, 2));
					payMap.put("monPayAmount", Arith.round(borrowSum, 2));
					payMap.put("monPayInterest", Arith.round(totalInterest, 2));
					payMap.put("stillPay", Arith.round(stillPay, 2));
					payList.add(payMap);
				}

				return payList;
			}
		}

		return null;
	}
}
