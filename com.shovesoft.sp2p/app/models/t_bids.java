package models;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Transient;

import constants.Constants;
import play.db.jpa.Model;
import utils.Security;

/**
 * 标
 * 
 * @author bsr
 * @version 6.0
 * @created 2014-4-4 下午03:32:02
 */
@Entity
public class t_bids extends Model {
	public long user_id;
	public Date time;
	public String bid_no;
	public String mer_bill_no;
	public long product_id;
	public String title;
	public long loan_purpose_id;
	public long repayment_type_id;
	public double amount;
	public int period;
	public double min_invest_amount;
	public double average_invest_amount;
	public int invest_period;
	public Date invest_expire_time;
	public Date real_invest_expire_time;
	public double apr;
	public long bank_account_id;
	public int bonus_type;
	public double bonus;
	public double award_scale;
	public String image_filename;

	public String audit_filename;

	public boolean is_quality;
	public boolean is_hot;
	public String description;
	public double bail;
	public double service_fees;
	public boolean is_agency;
	public int agency_id;
	public boolean is_show_agency_name;
	public int status;
	public double loan_schedule;
	public double has_invested_amount;
	public int read_count;
	public long allocation_supervisor_id;
	public long manage_supervisor_id;
	public Date audit_time;
	public String audit_suggest;
	public Date last_repay_time;
	public boolean is_auditmatic_invest_bid;

	public int period_unit;
	public boolean is_sec_bid;
	public boolean is_deal_password;
	public int show_type;
	public String mark;
	public int version;
	public String qr_code;// 二维码标识

	public t_bids() {

	}

	/**
	 * 我要借款,时间最新的未满借款标
	 * 
	 * @param user_id
	 *            用户ID
	 * @param name
	 *            用户名称
	 * @param id
	 *            标ID
	 * @param time
	 *            时间
	 * @param amount
	 *            金额
	 * @param apr
	 *            利率
	 */
	public t_bids(long user_id, long id, Date time, double amount, double apr) {
		this.user_id = user_id;
		this.id = id;
		this.time = time;
		this.amount = amount;
		this.apr = apr;
	}

	/**
	 * 我要借款,最新5个满标
	 * 
	 * @param id
	 *            标ID
	 * @param image_filename
	 *            借款图片
	 * @param amount
	 *            金额
	 */
	public t_bids(long id, String image_filename, double amount) {
		this.id = id;
		this.image_filename = image_filename;
		this.amount = amount;
	}

	/**
	 * 放款审核后续处理,部分查询项
	 * 
	 * @param service_fees
	 *            服务费
	 * @param amount
	 *            金额
	 */
	public t_bids(double service_fees, double amount) {
		this.service_fees = service_fees;
		this.amount = amount;
		// this.bail = bail;
	}

	/**
	 * 放款审核后续处理,部分查询项
	 * 
	 * @param bonus_type
	 *            奖励方式
	 * @param bonus
	 *            固定奖金
	 * @param award_scale
	 *            奖励百分比
	 */
	public t_bids(int bonus_type, double bonus, double award_scale) {
		this.bonus_type = bonus_type;
		this.bonus = bonus;
		this.award_scale = award_scale;
	}

	/**
	 * 账户满标倒计时提醒
	 * 
	 * @param id
	 *            标ID
	 * @param amount
	 *            金额
	 * @param time
	 *            时间
	 * @param invest_expire_time
	 *            满标时间
	 */
	public t_bids(long id, double amount, Date time, Date invest_expire_time) {
		this.id = id;
		this.amount = amount;
		this.time = time;
		this.invest_expire_time = invest_expire_time;
	}

	/**
	 * 财务放款
	 * 
	 * @param amount
	 *            金额
	 * @param apr
	 *            年利率
	 * @param period
	 *            期限
	 * @param repayment_type_id
	 *            还款方式
	 * @param service_fees
	 *            服务费
	 * @param bonus_type
	 *            奖励方式
	 * @param bonus
	 *            固定奖励
	 * @param award_scale
	 *            比例奖励
	 */
	public t_bids(double amount, double apr, int period,
			long repayment_type_id, double service_fees, int bonus_type,
			double bonus, double award_scale) {
		this.amount = amount;
		this.apr = apr;
		this.period = period;
		this.repayment_type_id = repayment_type_id;
		this.service_fees = service_fees;
		this.bonus_type = bonus_type;
		this.bonus = bonus;
		this.award_scale = award_scale;
	}

	/**
	 * 解除秒还标操作
	 * 
	 * @param amount
	 *            金额
	 * @param apr
	 *            年利率
	 * @param period
	 *            期限
	 * @param service_fees
	 *            服务费
	 * @param bail
	 *            保证金
	 */
	public t_bids(double amount, double apr, int period, double service_fees,
			double bail) {
		this.amount = amount;
		this.apr = apr;
		this.period = period;
		this.service_fees = service_fees;
		this.bail = bail;
	}

	/* 辉哥 */
	@Transient
	public String no;

	/**
	 * 我的会员账单-借款会员管理
	 * 
	 * @param id
	 * @param no
	 * @param title
	 * @param amount
	 * @param status
	 */
	public t_bids(long id, String no, String title, double amount, int status) {
		this.id = id;
		this.no = no;
		this.title = title;
		this.amount = amount;
		this.status = status;
	}

	/**
	 * 会员管理，根据用户ID查询数据
	 * 
	 * @param id
	 *            ID
	 * @param title
	 *            标题
	 * @param amount
	 *            金额
	 * @param status
	 *            状态
	 */
	public t_bids(long id, String title, double amount, int status) {
		this.id = id;
		this.title = title;
		this.amount = amount;
		this.status = status;
	}

	@Transient
	public String sign;

	/**
	 * 获取加密ID
	 */
	public String getSign() {
		return Security.addSign(this.id, Constants.BILL_ID_SIGN);
	}
}
