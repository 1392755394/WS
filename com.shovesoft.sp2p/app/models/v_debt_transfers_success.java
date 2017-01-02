package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Transient;

import constants.Constants;
import business.CreditLevel;
import play.db.jpa.GenericModel;
import play.db.jpa.Model;
import utils.ErrorInfo;
import utils.Security;

/**
 * 成功转让的债权标
* @author lwh
* @version 6.0
* @created 2014年4月21日 下午3:59:08
 */

@Entity
public class v_debt_transfers_success extends Model {
	public String name;
	public Integer credit_level_id;
	public Integer credit_order;
	public Integer period;
	public Integer period_unit;
	public Double invest_amount;
	public Double bid_amount;
	public Long user_id;
	public Long bid_id;
	public Double apr;
	public Integer type;
	public Integer status;
	public Double transfer_price;
	public Double debt_amount;
	public Date time;
	public String no;
	public Long has_payback_period;//已还账单
	public Long overdue_payback_period;//逾期账单
	public Date end_time;
	public Integer join_times;
	public String receive_user_name;
	public Date transaction_time;
	
	@Transient
	public Double receiving_amount_success;//应收本息
	
	@Transient
	public Double remain_received_corpus_success;//剩余应收本金
	
	
	
	public Double getReceiving_amount_success() {
		Double temp = 0.0;
		Long investId = 0l;
		try {
			investId = GenericModel.find("select id from t_invests where transfers_id = ? ", this.id).first();
		} catch (Exception e) {
			e.printStackTrace();
			investId = 0l;
		}
		
		if(investId != null && investId > 0){
			try {
				temp = GenericModel.find(" select sum(receive_corpus + receive_interest + overdue_fine) from t_bill_invests where invest_id = ? and status in (-1,-2,-5,-6)", investId).first();
			} catch (Exception e) {
				e.printStackTrace();
				temp = 0.0;
			}
			
		}
		 
		if(temp == null){
			temp = 0.0;
		}
		
		return temp;
	}



	public Double getRemain_received_corpus_success() {
		Double temp = 0.0;
		Long investId = 0l;
		try {
			investId = GenericModel.find("select id from t_invests where transfers_id = ? ", this.id).first();
		} catch (Exception e) {
			e.printStackTrace();
			investId = 0l;
		}
		
		if(investId != null && investId > 0){
			try {
				temp = GenericModel.find(" select sum(receive_corpus ) from t_bill_invests where invest_id = ? and status in (-1,-2,-5,-6)", investId).first();
			} catch (Exception e) {
				e.printStackTrace();
				temp = 0.0;
			}
			
		}
		 
		if(temp == null){
			temp = 0.0;
		}
		
		return temp;
	}

	@Transient
	public CreditLevel creditLevel;
	
	public CreditLevel getCreditLevel() {
		
		ErrorInfo error = new ErrorInfo();
		
		return CreditLevel.queryUserCreditLevel(this.user_id, error);
	}
	
	@Transient
	public String sign;

	public String getSign() {
		return Security.addSign(this.id,Constants.BID_ID_SIGN);
	}
	
	
}

	