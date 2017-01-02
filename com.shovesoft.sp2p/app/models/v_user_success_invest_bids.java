package models;

import javax.persistence.Entity;
import javax.persistence.Transient;

import constants.Constants;

import play.db.jpa.Model;
import utils.Security;


@Entity
public class v_user_success_invest_bids extends Model{
	public Long invest_id;
	public Long user_id;
	public Long bid_id;
	public Double invest_amount;
	public String name;
	public String title;
	public Double bid_amount;
	public Integer overdue_payback_period;
	public String no;
	public Double receiving_amount;
	public Double apr;
	
	@Transient
	public String sign;

	public String getSign() {
		return Security.addSign(this.invest_id, Constants.BID_ID_SIGN);
	}
	
	

}
