package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Transient;

import constants.Constants;

import play.db.jpa.Model;
import utils.Security;

@Entity
public class v_user_cps_users extends Model{

	public long recommend_user_id;
	public Date time;
	public int year;
	public int month;
	public String name;
	public boolean is_active;
	public double bid_amount;
	public double invest_amount;
	public double cps_award;
	
	@Transient
	public String sign;//加密ID
	
	public String getSign() {
		return Security.addSign(this.id, Constants.USER_ID_SIGN);
	}
}
