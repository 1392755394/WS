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
 * 后台—>推广员列表统计
 * @author cp
 * @version 6.0
 * @created 2014年5月22日 下午5:35:11
 */
@Entity
public class v_user_cps_user_info extends Model{
	
	public String name;
	public Date time;
	public long register_length;
	public long recommend_count;
	public long recharge_count;
	public double active_rate;
	public double bid_amount;
	public double invest_amount;
	public double commission_amount;
	
	@Transient
	public CreditLevel creditLevel;
	
	public CreditLevel getCreditLevel() {
		
		ErrorInfo error = new ErrorInfo();
		
		return CreditLevel.queryUserCreditLevel(this.id, error);
	}
	
	@Transient
	public String sign;//加密ID

	public String getSign() {
		return Security.addSign(this.id, Constants.USER_ID_SIGN);
	}
}
