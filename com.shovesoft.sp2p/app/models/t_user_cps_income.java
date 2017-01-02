package models;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 会员CPS推广收入
 * @author lwh
 *
 */

@Entity
public class t_user_cps_income extends Model{
	public int year ;
	public int month ;
	public long user_id;
	public long spread_user_account;
	public long effective_user_account;
	public long invalid_user_account;
	public double cps_reward;
}
