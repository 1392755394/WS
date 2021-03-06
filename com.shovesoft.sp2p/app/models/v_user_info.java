package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Transient;

import constants.Constants;
import business.CreditLevel;
import play.db.jpa.Model;
import utils.ErrorInfo;
import utils.Security;


@Entity
public class v_user_info extends Model {
	
	public String name;
	public int num;
	public Date register_time;
	public double score;
	public String email;
	public String mobile;
	public String mobile1;
	public String mobile2;
	public boolean is_activation;
	public boolean is_blacklist;
	public double user_amount;
	public Date last_login_time;
	public boolean is_allow_login;
	public long invest_count;
	public double invest_amount;
	public long bid_count;
	public double bid_amount;
	public long recharge_amount;
	public long audit_item_count;
	
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