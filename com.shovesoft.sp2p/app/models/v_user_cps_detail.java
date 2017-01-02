package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Transient;

import business.CreditLevel;
import play.db.jpa.Model;
import utils.ErrorInfo;

/**
 * 后台—>推广员列表—>详情
 * @author cp
 * @version 6.0
 * @created 2014年5月22日 下午5:35:11
 */
@Entity
public class v_user_cps_detail extends Model{

	public String name;
	public Date time;
	public long recommend_user_id;
	public long register_length;
	public double recharge_amount;
	public double bid_amount;
	public double repayment_amount;
	public double commission_amount;
	
	@Transient
	public CreditLevel creditLevel;
	
	public CreditLevel getCreditLevel() {
		
		ErrorInfo error = new ErrorInfo();
		
		return CreditLevel.queryUserCreditLevel(this.id, error);
	}
}
