package models;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Transient;

import business.CreditLevel;
import play.db.jpa.Model;
import utils.ErrorInfo;

/**
 * 坏账借款标列表
 * 
 * @author bsr
 * @version 6.0
 * @created 2014-4-21 上午10:47:08
 */
@Entity
public class v_bid_bad extends Model {
	public Date time;
	public String bid_no;
	public String title;
	public Long user_id;
	public String user_name;
	public Integer credit_level_id;
	public Double amount;
	public Integer product_id;
	public String small_image_filename;
	public String product_name;
	public Integer period_unit;
	public Double apr;
	public Integer period;
	public Integer status;
	public Date audit_time;
	public Date last_repay_time;
	public Integer repayment_count;
	public Integer overdue_count;
	public Date mark_bad_time;
	public Double overdue_length;
	public Long manage_supervisor_id;
	public String supervisor_name;
	
	@Transient
	public CreditLevel creditLevel;
	
	/**
	 * 信用积分
	 */
	public CreditLevel getCreditLevel() {
		return CreditLevel.queryUserCreditLevel(this.user_id, new ErrorInfo());
	}
}