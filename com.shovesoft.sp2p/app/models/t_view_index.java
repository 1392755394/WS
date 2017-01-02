package models;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class t_view_index extends Model{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public double  mask_percent;
	public int mask_multiple;
	public int mask_small_multiple;
	public int mask_give_money;
	
	
	

}
