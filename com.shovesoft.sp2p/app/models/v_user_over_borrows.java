package models;

import java.util.Date;
import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 超额借款
 * @author lzp
 * @version 6.0
 * @created 2014-6-18
 */
@Entity
public class v_user_over_borrows extends Model {
	public double amount;
	public double pass_amount;
	public Date time;
	public double credit_line;
	public String status;
	public long user_id;
	public String reason;
	public String user_name;
	public String user_email;
	public String user_mobile;
	public int user_credit_line;
	public String filename;
	public int passed_items_count;
	public int unpassed_items_count;
	public int auditing_items_count;
	public int appended_items_count;
}
