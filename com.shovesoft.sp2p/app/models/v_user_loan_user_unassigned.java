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
 * 部门账单管理-待分配借款会员列表
 */

@Entity
public  class v_user_loan_user_unassigned extends Model {
	
	public String bid_no;
	public long user_id;
	public String type;
	public String name;
	public Date register_time;
	public boolean vip_status;
	public int age;
	public int sex;
	public int product_id;
	public String product_name;
	public Date audit_time;
	public double amount;
	public String city_name;
	public String province_name;
	
	@Transient
	public String sign;
	/**
	 * 获取加密ID
	 */
	public String getSign() {
		return Security.addSign(this.id, Constants.BID_ID_SIGN);
	}
	
	@Transient
	public CreditLevel creditLevel;
	
	public CreditLevel getCreditLevel() {
		
		ErrorInfo error = new ErrorInfo();
		
		return CreditLevel.queryUserCreditLevel(this.user_id, error);
	}
}