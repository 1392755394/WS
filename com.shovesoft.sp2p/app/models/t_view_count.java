package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 前台显示的投资人和钱数
 * */

@Entity
public class t_view_count extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int count_people;
	public int count_money;
	public int count_avliage_money;
	public int count_today_money;
	public Date time;

	public t_view_count() {

	}

}
