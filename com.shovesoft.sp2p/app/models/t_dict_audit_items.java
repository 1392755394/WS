package models;

import java.util.Date;

import javax.persistence.Entity;
import play.db.jpa.Model;

/**
 * 审计项目
 * 
 * @author bsr
 * @version 6.0
 * @created 2014-4-4 下午04:23:25
 */
@Entity
public class t_dict_audit_items extends Model {
	public String name;
	public Date time;
	public int type;
	public int period;
	public String description;
	public int credit_score;
	public int audit_cycle;
	public double audit_fee;
	public boolean is_use;
	public double pass_rate;
	public String mark;
	
	public t_dict_audit_items(){
		
	}
	
	/**
	 * 查询ID和name
	 * @param id ID
	 * @param name 名称
	 */
	public t_dict_audit_items(long id, String name, int credit_score, int period){
		this.id = id;
		this.name = name;
		this.credit_score = credit_score;
		this.period = period;
	}
}
