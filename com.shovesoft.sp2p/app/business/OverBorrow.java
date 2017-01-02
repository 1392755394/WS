package business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import constants.Constants;
import constants.SupervisorEvent;
import constants.UserEvent;
import play.Logger;
import play.db.helper.JpaHelper;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import utils.ErrorInfo;
import utils.PageBean;
import models.t_user_audit_items;
import models.t_user_over_borrows;
import models.v_user_audit_items;
import models.v_user_over_borrows;



/**
 * 超额借款
 * @author cp
 * @version 6.0
 * @created 2014年3月25日 上午11:28:56
 */
public class OverBorrow implements Serializable{

	public long id;
	public long _id = -1;
	
	public long userId;
	public Date time;
	public double amount;
	public String reason;
	public int status;
    public double passAmount;
	public long auditSupervisorId;
	public Date auditTime;
	public String auditOpinion;
	
	public void setId(long id){
        t_user_over_borrows overBorrow = null;

		try {
			overBorrow = GenericModel.findById(id);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			this._id = -1;

			return;
		}

		if (null == overBorrow) {
			this._id = -1;

			return;
		}
        
        this._id = overBorrow.id;
        this.userId = overBorrow.user_id;
        this.time = overBorrow.time;
        this.amount = overBorrow.amount;
        this.reason = overBorrow.reason;
        this.status = overBorrow.status;
        this.passAmount = overBorrow.pass_amount;
        this.auditSupervisorId = overBorrow.audit_supervisor_id;
        this.auditTime = overBorrow.audit_time;
        this.auditOpinion = overBorrow.audit_opinion;
	}
	
	public long getId() {
 		return _id;
 	}
	
	/**
	 * 是否有未通过审核的超额借款
	 * @return
	 */
	public static boolean haveAuditingOverBorrow(long userId, ErrorInfo error) {
		int count = 0;
		
		try {
			count = (int) GenericModel.count("user_id = ? and status = 0", userId);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return true;
		}
		
		error.code = 0;
		
		if (count > 0) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * 申请超额借款
	 * @param amount
	 * @param reason
	 * @param auditItems
	 * @param error
	 * @return
	 */
	public static int applyFor(long userId, int amount, String reason, List<Map<String,String>> auditItems, ErrorInfo error) {
		error.clear();
		
		if (amount <= 0) {
			error.code = -1;
			error.msg = "金额必须是正数";
			
			return error.code;
		}
		
		if (amount > 10000000) {
			error.code = -1;
			error.msg = "超额借款最多只能申请10000000万";
			
			return error.code;
		}
		
		if (StringUtils.isBlank(reason)) {
			error.code = -1;
			error.msg = "原因不能为空";
			
			return error.code;
		}
		
		if (null == auditItems || 0 == auditItems.size()) {
			error.code = -1;
			error.msg = "审核资料不能为空";
			
			return error.code;
		}
		
		if (haveAuditingOverBorrow(userId, error) && 0 == error.code) {
			error.code = -1;
			error.msg = "您还有未审核的超额借款申请，不能再次申请";
			
			return error.code;
		}
		
		t_user_over_borrows overBorrow = new t_user_over_borrows(); 
		overBorrow.user_id = userId;
		overBorrow.time = new Date();
		overBorrow.amount = amount;
		overBorrow.reason = reason;
		overBorrow.credit_line = User.queryCreditLineById(userId, error);
		overBorrow.status = 0;
		
		try {
			overBorrow.save();
		}catch (Exception e) {
			e.printStackTrace();
			Logger.info("申请超额借款时："+e.getMessage());
			error.code = -1;
			error.msg = "申请超额借款失败";
			JPA.setRollbackOnly();
			
			return error.code;
		}
		
		long overBorrowId = overBorrow.id;
		
		/**
		 * 添加超额借款审核资料
		 */
		for (Map<String, String> item : auditItems) {
			long itemId = Long.parseLong(item.get("id"));
			String filename = item.get("filename");
			int status = StringUtils.isBlank(filename) ? Constants.UNCOMMITTED : Constants.AUDITING;
			
			if (status == Constants.UNCOMMITTED) {
				t_user_audit_items tItem = new t_user_audit_items();
				tItem.user_id = userId;
				tItem.time = new Date();
				tItem.audit_item_id = itemId;
				tItem.image_file_name = filename;
				tItem.status = status;
				tItem.is_over_borrow = true;
				tItem.over_borrow_id = overBorrowId;
				tItem.is_visible = true;
				tItem.mark = AuditItem.queryMark(itemId);
				
				try {
					tItem.save();
				} catch (Exception e) {
					Logger.error("添加超额借款审核资料时:" + e.getMessage());
					error.code = -1;
					error.msg = "申请超额借款失败!";
					JPA.setRollbackOnly();
					
					return error.code; 
				}
				
				continue;
			}
			
			String[] names = filename.split(",");
			
			for (int i = 0; i < names.length; i++) {
				t_user_audit_items tItem = new t_user_audit_items();
				tItem.user_id = userId;
				tItem.time = new Date();
				tItem.audit_item_id = itemId;
				tItem.image_file_name = names[i];
				tItem.status = status;
				tItem.is_over_borrow = true;
				tItem.over_borrow_id = overBorrowId;
				tItem.is_visible = true;
				tItem.mark = AuditItem.queryMark(itemId);
				
				try {
					tItem.save();
				} catch (Exception e) {
					Logger.error("添加超额借款审核资料时:" + e.getMessage());
					error.code = -1;
					error.msg = "申请超额借款失败!";
					JPA.setRollbackOnly();
					
					return error.code; 
				}
			}
		}
		
		DealDetail.userEvent(userId, UserEvent.APPLY_FOR_OVER_BORROW, "申请超额借款", error);
		
		if (error.code < 0) {
			JPA.setRollbackOnly();
			
			return error.code;
		}
		
		error.code = 0;
		error.msg = "您的超额借款申请已提交，请耐心等待审核结果。";
		
		return error.code;
	}
	
	/**
	 * 审核超额借款
	 * @param supervisorId
	 * @param overBorrowId
	 * @param status
	 * @param passAmount
	 * @param auditOpinion
	 * @param error
	 * @return
	 */
	public static int audit(long supervisorId, long overBorrowId, int status, int passAmount, String auditOpinion, ErrorInfo error) {
		error.clear();
		
		/**
		 * 判断审核资料是否全部提交完毕
		 */
		List<v_user_audit_items> items = queryAuditItems(overBorrowId, error);
		
		if (error.code < 0) {
			return error.code;
		}
		
		if(status == 1){
			for (v_user_audit_items item : items) {
				if (item.status != 2) {
					error.code = -1;
					error.msg = "该超额借款还有未审核通过的资料，审核失败";
					
					return error.code;
				}
			}
			
			if (passAmount < 0) {
				error.code = -1;
				error.msg = "通过的额度必须是正数";
			}
		}
		
		if (StringUtils.isBlank(auditOpinion)) {
			error.code = -1;
			error.msg = "审核意见不能为空";
			
			return error.code;
		}
		
		if (status != 1 && status != 2) {
			error.code = -1;
			error.msg = "请选择审核状态";
			
			return error.code;
		}
		
		t_user_over_borrows overBorrow = null;

		try {
			overBorrow = GenericModel.findById(overBorrowId);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return error.code;
		}

		if (null == overBorrow) {
			error.code = -2;
			error.msg = "审核的超额借款不存在";

			return error.code;
		}
		
		if (overBorrow.status != 0) {
			error.code = -3;
			error.msg = "超额借款已审核";

			return error.code;
		}
		
		overBorrow.status = status;
		overBorrow.audit_supervisor_id = supervisorId;
		overBorrow.audit_time = new Date();
		overBorrow.pass_amount = passAmount;
		overBorrow.audit_opinion = auditOpinion;
		
		try {
			overBorrow.save();
		}catch (Exception e) {
			e.printStackTrace();
			Logger.info("update t_user_over_borrows："+e.getMessage());
			error.code = -1;
			error.msg = "审核超额借款失败, 请稍后重试";
			JPA.setRollbackOnly();
			
			return error.code;
		}
		
		/**
		 * 不通过
		 */
		if (status == 2) {
			DealDetail.supervisorEvent(supervisorId, SupervisorEvent.AUDIT_OVER_BORROW, "审核超额借款，不通过", error);
			
			if (error.code < 0) {
				JPA.setRollbackOnly();
				
				return error.code;
			}
			
			error.msg = "审核超额借款成功";
			
			return error.code;
		}
		
		/**
		 * 通过
		 */
		long userId = overBorrow.user_id;
		String sql = "update t_users set credit_line = credit_line + ? where id = ?";
		int rows = 0;
		
		try {
			rows = JpaHelper.execute(sql, (double)passAmount, userId).executeUpdate();
		}catch (Exception e) {
			e.printStackTrace();
			Logger.info("update t_users："+e.getMessage());
			error.code = -1;
			error.msg = "审核超额借款失败, 请稍后重试";
			JPA.setRollbackOnly();
			
			return error.code;
		}
		
		if(rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";
			
			return error.code;
		}
		
		DealDetail.supervisorEvent(supervisorId, SupervisorEvent.AUDIT_OVER_BORROW, "审核超额借款，通过", error);
		
		if (error.code < 0) {
			JPA.setRollbackOnly();
			
			return error.code;
		}
		
		error.code = 0;
		error.msg = "审核超额借款成功";
		
		return error.code;
	}
	
	/**
	 * 超额借款管理列表
	 * @param currPage
	 * @param pageSize
	 * @param keywordType
	 * @param keyword
	 * @param orderType
	 * @param error
	 * @return
	 */
	public static PageBean<v_user_over_borrows> queryOverBorrows(int currPage, int pageSize, 
			int keywordType, String keyword, int orderType, ErrorInfo error) {
		error.clear();
		
		if (currPage < 1) {
			currPage = 1;
		}

		if (pageSize < 1) {
			pageSize = 10;
		}
		
		if (keywordType < 0 || keywordType > 2) {
			keywordType = 0;
		}
		
		if (orderType < 0 || orderType > 9) {
			orderType = 0;
		}

		String searchCondition = "(1 = 1)";
		String orderCondition = Constants.OVER_BORROWS_ORDER_CONDITION[orderType];
		List<Object> params = new ArrayList<Object>();

		if (StringUtils.isNotBlank(keyword)) {
			if (1 == keywordType) {
				searchCondition += " and (user_name like ?)";
				params.add("%" + keyword + "%");
			} else if (2 == keywordType) {
				searchCondition += " and (user_email like ?)";
				params.add("%" + keyword + "%");
			} else {
				searchCondition += " and ((user_name like ?) or (user_email like ?))";
				params.add("%" + keyword + "%");
				params.add("%" + keyword + "%");
			}
		}
		
		String condition = searchCondition + orderCondition;
		
		List<v_user_over_borrows> page = null;
		int count = 0;

		try {
			page = GenericModel.find(condition, params.toArray()).fetch(currPage, pageSize);
			count = (int) GenericModel.count(condition, params.toArray());
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return null;
		}

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("keywordType", keywordType);
		map.put("orderType", orderType);
		
		if (StringUtils.isNotBlank(keyword)) {
			map.put("keyword", keyword);
		}
		
		PageBean<v_user_over_borrows> bean = new PageBean<v_user_over_borrows>();
		bean.pageSize = pageSize;
		bean.currPage = currPage;
		bean.page = page;
		bean.totalCount = count;
		bean.conditions = map;
		
		error.code = 0;

		return bean;
	}
	
	
	
	/**
	 * 查询超额借款补提交的资料
	 * @param overBorrowId
	 * @return
	 */
	public static List<v_user_audit_items> queryAuditItems(long overBorrowId, ErrorInfo error) {
		error.clear();
		List<v_user_audit_items> items = null;
		
		try {
			items = GenericModel.find("over_borrow_id = ? group by audit_item_id", overBorrowId).fetch();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return null;
		}
		
		error.code = 0;
		error.msg = "查询超额借款补提交的资料成功";
		
		return items;
	}
	
	/**
	 * 查询历史申请记录
	 * @param userId
	 * @param error
	 * @return
	 */
	public static List<v_user_over_borrows> queryHistoryOverBorrows(long overBorrowId, ErrorInfo error) {
		error.clear();
		String sql = "user_id = (select user_id from t_user_over_borrows where id = ?) and status != '审核中'";
		List<v_user_over_borrows> overBorrows = null;
		
		try {
			overBorrows = GenericModel.find(sql, overBorrowId).fetch();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return null;
		}
		
		error.code = 0;
		error.msg = "查询历史申请记录成功";
		
		return overBorrows;
	}
	
	/**
	 * 查询审核中的超额借款
	 * @param userId
	 * @param error
	 * @return
	 */
	public static v_user_over_borrows queryAuditingOverBorrow(long overBorrowId, ErrorInfo error) {
		error.clear();
		String sql = "id = ?";
		v_user_over_borrows overBorrow = null;
		
		try {
			overBorrow = GenericModel.find(sql, overBorrowId).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return null;
		}
		
		error.code = 0;
		
		return overBorrow;
	}
	
	/**
	 * 查询上一个超额借款id
	 * @param overBorrowId
	 * @return
	 */
	public static long queryPreOverBorrowId(long overBorrowId) {
		Long id = null;
		
		try {
			id = GenericModel.find("select MAX(id) from t_user_over_borrows where id < ?", overBorrowId).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();

			return -1;
		}
		
		return (null == id) ? -1 : id.longValue();
	}
	
	/**
	 * 查询下一个超额借款id
	 * @param overBorrowId
	 * @return
	 */
	public static long queryNextOverBorrowId(long overBorrowId) {
		Long id = null;
		
		try {
			id = GenericModel.find("select MIN(id) from t_user_over_borrows where id > ?", overBorrowId).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();

			return -1;
		}
		
		return (null == id) ? -1 : id.longValue();
	}
	
	/**
	 * 查询之前的超额借款数
	 * @param overBorrowId
	 * @return
	 */
	public static long queryPreOverBorrowCount(long overBorrowId) {
		long count = 0;
		
		try {
			count = GenericModel.count("id < ?", overBorrowId);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();

			return 0;
		}
		
		return count;
	}
	
	/**
	 * 查询之后的超额借款数
	 * @param overBorrowId
	 * @return
	 */
	public static long queryLaterOverBorrowCount(long overBorrowId) {
		long count = 0;
		
		try {
			count = GenericModel.count("id > ?", overBorrowId);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();

			return 0;
		}
		
		return count;
	}
	
	/**
	 * 查询超额借款补提交的资料通过数
	 * @param overBorrowId
	 * @return
	 */
	public static int queryPassedAuditItemsCount(long overBorrowId, ErrorInfo error) {
		error.clear();
		int count = 0;
		
		try {
			count = (int) GenericModel.count("over_borrow_id = ? and status = 2", overBorrowId);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return 0;
		}
		
		error.code = 0;
		
		return count;
	}
	
	/**
	 * 查询信用积分
	 * @param overBorrowId
	 * @return
	 */
	public static int queryCreditScore(long overBorrowId, ErrorInfo error) {
		error.clear();
		Long sum = null;
		String sql = "select sum(credit_score) from v_user_audit_items where over_borrow_id = ? and status = ?";
		
		try {
			sum = (Long) GenericModel.find(sql, overBorrowId, Constants.AUDITED).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return 0;
		}
		
		error.code = 0;
		
		return (sum == null) ? 0 : sum.intValue();
	}
	
	/**
	 * 查询userId通过overBorrowId
	 * @param overBorrowId
	 * @return
	 */
	public static long queryUserId(long overBorrowId, ErrorInfo error) {
		error.clear();
		Long userId = null;
		String sql = "select user_id from v_user_over_borrows where id = ?";
		
		try {
			userId = (Long) GenericModel.find(sql, overBorrowId).first();
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return 0;
		}
		
		error.code = 0;
		
		return (userId == null) ? 0 : userId.longValue();
	}
	
	/**
	 * 查询超额借款记录
	 * @param userId
	 * @return
	 */
	public static List<t_user_over_borrows> queryUserOverBorrows(long userId, ErrorInfo error){
		error.clear();
		
		List<t_user_over_borrows> overBorrows = new ArrayList<t_user_over_borrows>();
		
		String sql = "select new t_user_over_borrows(id, amount, reason, time, status) from t_user_over_borrows"
				+ " where user_id=?";
		
		try{
			overBorrows = GenericModel.find(sql, userId).fetch();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("查询超额借款申请记录时："+e.getMessage());
			
			error.code = -1;
			error.msg = "查询用户超额借款记录出现异常";
			
			return null;
		}
		
		error.code = 0;
		
		return overBorrows;
	} 
	
	public static t_user_over_borrows queryOverBorrowById(long overBorrowId, ErrorInfo error){
		error.clear();
		
		t_user_over_borrows overBorrows = new t_user_over_borrows();
		
		String sql = "select new t_user_over_borrows(id, amount, reason, time, status) from t_user_over_borrows"
				+ " where id=?";
		
		try{
			overBorrows = GenericModel.find(sql, overBorrowId).first();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("查询超额借款申请记录时："+e.getMessage());
			
			error.code = -1;
			error.msg = "查询用户超额借款记录出现异常";
			
			return null;
		}
		
		error.code = 0;
		
		return overBorrows;
	} 
	
}
