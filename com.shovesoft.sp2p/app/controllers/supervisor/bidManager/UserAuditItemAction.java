package controllers.supervisor.bidManager;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import constants.Constants;
import business.Product;
import controllers.supervisor.SupervisorController;
import business.Supervisor;
import business.UserAuditItem;
import models.v_user_audit_item_stats;
import models.v_user_audit_items;
import utils.ErrorInfo;
import utils.NumberUtil;
import utils.PageBean;
import utils.Security;

/**
 * 会员借款资料审核管理
 * 
 * @author bsr
 * @version 6.0
 * @created 2014-5-16 下午09:25:51
 */
public class UserAuditItemAction extends SupervisorController {
	
	/**
	 * 会员借款资料审核管理
	 */
	public static void userAuditItemList() {
		String currPage = params.get("currPage"); // 当前页
		String pageSize = params.get("pageSize"); // 分页行数
		String condition = params.get("condition"); // 条件
		String keyword = params.get("keyword"); // 关键词
		String orderIndex = params.get("orderIndex"); // 排序索引
		String orderStatus = params.get("orderStatus"); // 升降标示
		
		ErrorInfo error = new ErrorInfo();
		PageBean<v_user_audit_item_stats> pageBean = new PageBean<v_user_audit_item_stats>();
		pageBean.currPage = NumberUtil.isNumericInt(currPage)? Integer.parseInt(currPage): 1;
		pageBean.pageSize = NumberUtil.isNumericInt(pageSize)? Integer.parseInt(pageSize): 10;
		pageBean.page = UserAuditItem.queryUserAuditItemInAdmin(pageBean, error, condition, keyword, orderIndex, orderStatus);

		render(pageBean);
	}
	
	/**
	 * 审核明细
	 */
	public static void auditDetail(String signUserId){
		/* 解密userId */
		ErrorInfo error = new ErrorInfo();
		long userId = Security.checkSign(signUserId, Constants.USER_ID_SIGN, Constants.VALID_TIME, error);
		
		if(userId < 1){
			flash.error(error.msg);
			
			userAuditItemList();
		}

		String currPage = params.get("currPage");
		String pageSize = params.get("pageSize");
		String status = params.get("status");
		String startDate = params.get("startDate");
		String endDate = params.get("endDate");
		String productId = params.get("productId");
		String productType = params.get("productType");
		
		/* 产品名称列表 */
		List<Product> products = Product.queryProductNames(Constants.NOT_ENABLE, error);
		PageBean<v_user_audit_items> pageBean = UserAuditItem.queryUserAuditItem(currPage, pageSize, userId, error, status, startDate, endDate, productId, productType);
		/* 当前审核明细统计 */
		Map<String, Integer> auditStatistics = UserAuditItem.auditItemsStatistics(userId, error);
		/* 上一个,下一个 */
		UserAuditItem item = new UserAuditItem();
		item.userName = pageBean.page == null ? "---" : pageBean.page.get(0).user_name;
		item.userId = userId;
		
		render(pageBean, products, auditStatistics, signUserId, item);
	}
	
	/**
	 * 查看(异步)
	 */
	public static void showitem(String mark, String signUserId){
		/* 解密userId */
		ErrorInfo error = new ErrorInfo();
		long userId = Security.checkSign(signUserId, Constants.USER_ID_SIGN, Constants.VALID_TIME, error);
		
		if(userId < 1){
			renderText(error.msg);
		}
		
		UserAuditItem item = new UserAuditItem();
		item.lazy = true;
		item.userId = userId;
		item.mark = mark;
		
		render(item);
	}
	
	/**
	 * 审核页面(异步)
	 */
	public static void audititem(String mark, String signUserId){
		/* 解密userId */
		ErrorInfo error = new ErrorInfo();
		long userId = Security.checkSign(signUserId, Constants.USER_ID_SIGN, Constants.VALID_TIME, error);
		
		if(userId < 1){
			renderText(error.msg);
		}

		UserAuditItem item = new UserAuditItem();
		item.lazy = true;
		item.userId = userId;
		item.mark = mark;
		
		render(item);
	}
	
	/**
	 * 审核(异步)
	 */
	public static void audit(String signUserId, String mark, int status, boolean isVisible, String suggestion) {
		/* 解密userId */
		ErrorInfo error = new ErrorInfo();
		long userId = Security.checkSign(signUserId, Constants.USER_ID_SIGN, Constants.VALID_TIME, error);
		
		if(userId < 1){
			renderText(error.msg);
		}
		
		if( StringUtils.isBlank(mark) || 
			(status != Constants.AUDITED && status != Constants.NOT_PASS) || 
			StringUtils.isBlank(suggestion)   
		){
			renderText("数据有误!");
		}
		
		UserAuditItem item = new UserAuditItem();
		item.lazy = true;
		item.userId = userId;
		item.mark = mark;
		item.audit(Supervisor.currSupervisor().id, status, isVisible, suggestion, error);
		
//		JSONObject json = new JSONObject();
//		json.put("id", item.auditItemId);
//		json.put("time", item.time);
//		json.put("status", item.strStatus);
//		json.put("msg", error.msg);
		
		renderText(error.msg);
	}
}
