package models;

import javax.persistence.Entity;
import javax.persistence.Transient;

import business.CreditLevel;

import play.db.jpa.Model;
import utils.ErrorInfo;

/**
 * 用户关注的借款标列表
 * 
 * @author bsr
 * @version 6.0
 * @created 2014-4-21 上午10:47:08
 */
@Entity
public class v_bid_attention extends Model {
	public String bid_no;
	public String title;
	public Integer product_id;
	public String small_image_filename;
	public Long user_id;
	public Long bid_id;
	public String user_name;
	public Double amount;
	public Double apr;
	public Integer status;
	public String mark;
	public double capital_interest_sum;
	public Integer product_item_count;
	public Integer user_item_count_true;
	public Integer period;
	public Integer period_unit;
	
	@Transient
	public CreditLevel creditLevel;
	
	public CreditLevel getCreditLevel() {
		
		ErrorInfo error = new ErrorInfo();
		
		return CreditLevel.queryUserCreditLevel(this.user_id, error);
	}
	
	/*@Transient
	public Object user_item_count_true; // 用户通过资料数
	
	public Object getUser_item_count_true() {
		String hql = "SELECT count(uai2.id) AS user_item_count_true FROM("
				+ "select uai.id,  uai.audit_item_id from t_user_audit_items uai where status = ? and user_id = ? GROUP BY uai.audit_item_id) uai2 "
				+ "where uai2.audit_item_id IN( "
				+ "SELECT  pail.audit_item_id  FROM t_product_audit_items_log pail WHERE pail.mark = ? and type = 1)";

		Query query = JPA.em().createNativeQuery(hql);
		query.setParameter(1, Constants.AUDITED);
		query.setParameter(2, this.user_id);
		query.setParameter(3, this.mark);
		
		try {
			return query.getResultList().get(0);
		} catch (Exception e) {
			
			return 0;
		}
	}*/
}