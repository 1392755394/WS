package reports;

import java.util.Calendar;
import java.util.List;

import javax.persistence.Query;

import com.shove.Convert;

import models.t_dict_audit_items;
import models.t_statistic_audit_items;
import models.t_users;
import play.Logger;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import utils.Arith;
import utils.ErrorInfo;
import constants.OptionKeys;

/**
 * 审核科目库统计分析表
 * @author lzp
 * @version 6.0
 * @created 2014-7-16
 */
public class StatisticAuditItems {
	/**
	 * 周期性执行
	 * @param error
	 * @return
	 */
	public static int executeUpdate(ErrorInfo error) {
		error.clear();
		
		List<t_dict_audit_items> items = null;
		
		try {
			items = GenericModel.findAll();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return error.code;
		}
		
		if (items == null) {
			return error.code;
		}
		
		for (int i = 0; i < items.size(); i++) {
			t_dict_audit_items item = null;
			
			try {
				item = items.get(i);
			} catch (Exception e) {
				Logger.error(e.getMessage());
				continue;
			}
			
			int itemId = item.id.intValue();
			boolean isAdd = isAdd(itemId, error);
			
			if (error.code < 0) {
				return error.code;
			}
			
			if (isAdd) {
				update(item, error);
			} else {
				add(item, error);
			}
			
			if (error.code < 0) {
				return error.code;
			}
		}
		
		error.code = 0;
		
		return error.code;
	}
	
	/**
	 * 添加本月统计数据
	 * @param error
	 * @return
	 */
	private static int add(t_dict_audit_items item, ErrorInfo error) {
		error.clear();
		
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int itemId = item.id.intValue();
		
		t_statistic_audit_items entity = new t_statistic_audit_items();
		entity.audit_item_id = itemId;
		entity.year = year;
		entity.month = month;
		entity.no = OptionKeys.getvalue(OptionKeys.AUDIT_ITEM_NUMBER, new ErrorInfo())+itemId;
		entity.name = item.name;
		entity.credit_score = item.credit_score;
		entity.audit_fee = item.audit_fee;
		entity.borrow_user_num = queryBorrowUserNum(error);
		entity.submit_user_num = querySubmitUserNum(itemId, error);
		entity.submit_per = entity.borrow_user_num==0 ? 0 : Arith.div(entity.submit_user_num, entity.borrow_user_num, 2);
		entity.audit_pass_num = queryAuditPassNum(itemId, error);
		entity.pass_per = entity.submit_user_num==0 ? 0 : Arith.div(entity.audit_pass_num, entity.submit_user_num, 2);
		entity.relate_product_num = queryRelateProductNum(itemId, error);
		entity.relate_overdue_bid_num = queryRelateProductNum(itemId, error);
		entity.relate_bad_bid_num = queryRelateBadBidNum(itemId, error);
		entity.risk_control_ranking = queryRiskControlRanking(itemId, error);

		try {
			entity.save();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return error.code;
		}
		
		error.code = 0;

		return error.code;
	}
	
	/**
	 * 更新本月统计数据
	 * @param error
	 * @return
	 */
	private static int update(t_dict_audit_items item, ErrorInfo error) {
		error.clear();
		
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int itemId = item.id.intValue();
		
		t_statistic_audit_items entity = null;
		
		try {
			entity = GenericModel.find("audit_item_id = ? and year = ? and month = ?", itemId, year, month).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return error.code;
		}
		
		if (entity == null) {
			error.code = -1;
			error.msg = "本月借款资料统计不存在";
			
			return error.code;
		}
		
		entity.audit_item_id = itemId;
		entity.year = cal.get(Calendar.YEAR);
		entity.month = cal.get(Calendar.MONTH) + 1;
		entity.no = OptionKeys.getvalue(OptionKeys.AUDIT_ITEM_NUMBER, new ErrorInfo())+itemId;
		entity.name = item.name;
		entity.credit_score = item.credit_score;
		entity.audit_fee = item.audit_fee;
		entity.borrow_user_num = queryBorrowUserNum(error);
		entity.submit_user_num = querySubmitUserNum(itemId, error);
		entity.submit_per = entity.borrow_user_num==0 ? 0 : Arith.div(entity.submit_user_num, entity.borrow_user_num, 2);
		entity.audit_pass_num = queryAuditPassNum(itemId, error);
		entity.pass_per = entity.submit_user_num==0 ? 0 : Arith.div(entity.audit_pass_num, entity.submit_user_num, 2);
		entity.relate_product_num = queryRelateProductNum(itemId, error);
		entity.relate_overdue_bid_num = queryRelateProductNum(itemId, error);
		entity.relate_bad_bid_num = queryRelateBadBidNum(itemId, error);
		entity.risk_control_ranking = queryRiskControlRanking(itemId, error);

		try {
			entity.save();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return error.code;
		}
		
		error.code = 0;

		return error.code;
	}
	
	/**
	 * 是否添加了本月数据
	 * @return
	 */
	private static boolean isAdd(int itemId, ErrorInfo error) {
		error.clear();
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int count = 0;
		
		try {
			count = (int)GenericModel.count("audit_item_id = ? and year = ? and month = ?", itemId, year, month);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return false;
		}
		
		error.code = 0;
		
		return (count > 0);
	}
	
	/**
	 * 查询借款会员数
	 * @param error
	 * @return
	 */
	public static int queryBorrowUserNum(ErrorInfo error) {
		error.clear();
		int count = 0;
		
		try {
			count = (int) GenericModel.count("master_identity = 1");
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询借款会员数时："+e.getMessage());
			error.code = -1;
			error.msg = "查询借款会员数出现异常！";
			
			return 0;
		}
		
		error.code = 0;
		
		return count;
	}
	
	/**
	 * 查询提交会员数
	 * @param auditItemId
	 * @param error
	 * @return
	 */
	public static int querySubmitUserNum(int auditItemId, ErrorInfo error) {
		error.clear();
		String sql = "select count(distinct user_id) from t_user_audit_items where audit_item_id = ? and status <> 0 and date_format(time,'%Y%m') = date_format(curdate(),'%Y%m')";
		Query query = JPA.em().createNativeQuery(sql);
		query.setParameter(1, auditItemId);
		Object obj = null;
		
		try {
			obj = query.getResultList().get(0);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询提交会员数时："+e.getMessage());
			error.code = -1;
			error.msg = "查询提交会员数出现异常！";
			
			return 0;
		}
		
		error.code = 0;
		
		return Convert.strToInt(obj+"", 0);
	}
	
	/**
	 * 查询审核通过数
	 * @param auditItemId
	 * @param error
	 * @return
	 */
	public static int queryAuditPassNum(int auditItemId, ErrorInfo error) {
		error.clear();
		String sql = "select count(distinct user_id) from t_user_audit_items where audit_item_id = ? and status = 2 and date_format(time,'%Y%m') = date_format(curdate(),'%Y%m')";
		Query query = JPA.em().createNativeQuery(sql);
		query.setParameter(1, auditItemId);
		Object obj = null;
		
		try {
			obj = query.getResultList().get(0);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询审核通过数时："+e.getMessage());
			error.code = -1;
			error.msg = "查询审核通过数出现异常！";
			
			return 0;
		}
		
		error.code = 0;
		
		return Convert.strToInt(obj+"", 0);
	}
	
	/**
	 * 查询关联借款标产品数量
	 * @param auditItemId
	 * @param error
	 * @return
	 */
	public static int queryRelateProductNum(int auditItemId, ErrorInfo error) {
		error.clear();
		String sql = "select count(distinct product_id) from t_product_audit_items where audit_item_id = ? and date_format(time,'%Y%m') = date_format(curdate(),'%Y%m')";
		Query query = JPA.em().createNativeQuery(sql);
		query.setParameter(1, auditItemId);
		Object obj = null;
		
		try {
			obj = query.getResultList().get(0);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询关联借款标产品数量时："+e.getMessage());
			error.code = -1;
			error.msg = "查询关联借款标产品数量出现异常！";
			
			return 0;
		}
		
		error.code = 0;
		
		return Convert.strToInt(obj+"", 0);
	}
	
	/**
	 * 查询关联逾期借款标数量
	 * @param auditItemId
	 * @param error
	 * @return
	 */
	public static int queryRelateOverdueBidNum(int auditItemId, ErrorInfo error) {
		error.clear();
		String sql = "select count(*) from t_bids where id in (select bid_id from t_bills where overdue_mark <> 0) and ? in (select audit_item_id from t_product_audit_items where product_id = t_bids.product_id) and date_format(time,'%Y%m') = date_format(curdate(),'%Y%m')";
		Query query = JPA.em().createNativeQuery(sql);
		query.setParameter(1, auditItemId);
		Object obj = null;
		
		try {
			obj = query.getResultList().get(0);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询关联逾期借款标数量时："+e.getMessage());
			error.code = -1;
			error.msg = "查询关联逾期借款标数量出现异常！";
			
			return 0;
		}
		
		error.code = 0;
		
		return Convert.strToInt(obj+"", 0);
	}
	
	/**
	 * 查询关联坏账借款标数量
	 * @param auditItemId
	 * @param error
	 * @return
	 */
	public static int queryRelateBadBidNum(int auditItemId, ErrorInfo error) {
		error.clear();
		String sql = "select count(*) from t_bids where id in (select bid_id from t_bills where overdue_mark = -3) and ? in (select audit_item_id from t_product_audit_items where product_id = t_bids.product_id) and date_format(time,'%Y%m') = date_format(curdate(),'%Y%m')";
		Query query = JPA.em().createNativeQuery(sql);
		query.setParameter(1, auditItemId);
		Object obj = null;
		
		try {
			obj = query.getResultList().get(0);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询关联坏账借款标数量数量时："+e.getMessage());
			error.code = -1;
			error.msg = "查询关联坏账借款标数量出现异常！";
			
			return 0;
		}
		
		error.code = 0;
		
		return Convert.strToInt(obj+"", 0);
	}
	
	/**
	 * 查询风控有效性排名
	 * @param auditItemId
	 * @param error
	 * @return
	 */
	public static int queryRiskControlRanking(int auditItemId, ErrorInfo error) {
		error.clear();
		String sql = "select count(*)+1 from t_dict_audit_items where (select count(*) from t_bids where id in (select bid_id from t_bills where overdue_mark <> 0) and id in (select audit_item_id from t_product_audit_items where product_id = t_bids.product_id) and date_format(time,'%Y%m') = date_format(curdate(),'%Y%m')) < (select count(*) from t_bids where id in (select bid_id from t_bills where overdue_mark <> 0) and ? in (select audit_item_id from t_product_audit_items where product_id = t_bids.product_id) and date_format(time,'%Y%m') = date_format(curdate(),'%Y%m'))";
		Query query = JPA.em().createNativeQuery(sql);
		query.setParameter(1, auditItemId);
		Object obj = null;
		
		try {
			obj = query.getResultList().get(0);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询关联坏账借款标数量数量时："+e.getMessage());
			error.code = -1;
			error.msg = "查询关联坏账借款标数量出现异常！";
			
			return 0;
		}
		
		error.code = 0;
		
		return Convert.strToInt(obj+"", 0);
	}
}
