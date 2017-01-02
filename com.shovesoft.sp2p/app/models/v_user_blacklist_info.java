package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Transient;

import constants.Constants;
import business.CreditLevel;
import play.db.jpa.Model;
import utils.ErrorInfo;
import utils.Security;

/**
 * 黑名单会员
 * @author cp
 * @version 6.0
 * @created 2014年5月17日 上午10:23:25
 */
@Entity
public class v_user_blacklist_info extends Model {
	public Date register_time;
	public int num;
	public String name;
	public boolean is_allow_login;
	public double credit_score;
	public Date join_time;
	public double user_amount;
	public double recharge_amount;
	public long invest_count;
	public double invest_amount;
	public long bid_count;
	public double bid_amount;
	public int overdue_bill_count;
	public int reported_count;
	
	@Transient
	public CreditLevel creditLevel;
	
	public CreditLevel getCreditLevel() {
		
		ErrorInfo error = new ErrorInfo();
		
		return CreditLevel.queryUserCreditLevel(this.id, error);
	}
	
	@Transient
	public String sign;//加密ID

	public String getSign() {
		return Security.addSign(this.id, Constants.USER_ID_SIGN);
	}
}
