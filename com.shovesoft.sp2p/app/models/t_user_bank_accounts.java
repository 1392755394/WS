package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 
 * @author lzp
 * @version 6.0
 * @created 2014-4-4 下午3:41:24
 */

@Entity
public class t_user_bank_accounts extends Model {

	public long user_id;
	public Date time;
	public String bank_name;
	public String account;
	public String account_name;
	public boolean verified;
	public Date verify_time;
	public long verify_supervisor_id;


}
