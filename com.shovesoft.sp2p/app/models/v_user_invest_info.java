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
public class v_user_invest_info extends Model {

	public Date register_time;
	public int num;
	public String name;
	public String email;
	public String mobile;
	public String mobile1;
	public String mobile2;
	public boolean is_allow_login;
	public double credit_score;	
	public double user_amount;
	public int master_identity;
	public double recharge_amount;
	public long invest_count;
	public double invest_amount;
	public double invest_receive_count;
	public double receive_amount;
	public long transfer_count;
	
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