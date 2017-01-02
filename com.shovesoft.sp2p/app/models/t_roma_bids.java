package models;

import javax.persistence.Entity;
import java.util.Date;

import play.db.jpa.Model;

@Entity
public class t_roma_bids extends Model {

	public int roma_admin_id;
	public Date roma_time;
	public String roma_bid_no;
	public String roma_title;
	public int roma_loan_purpose_id;
	public int roma_period;
	public double roma_apr;
	public String roma_image_filename;
	public String roma_description;
	public boolean roma_is_deal_password;
	public double roma_bail;
	public boolean roma_is_hot;
	public int roma_status;
	public double roma_has_invested_amount;
	public int roma_read_count;
	public String roma_mark;
	public String roma_audit_filename;
	public String roma_repaymentType;
	public String roma_qc_code;

}
