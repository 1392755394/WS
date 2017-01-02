package business;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;
import org.apache.commons.lang.StringUtils;

import constants.Constants;
import constants.DealType;
import constants.OptionKeys;
import constants.SupervisorEvent;
import constants.Templets;
import constants.UserEvent;
import play.Logger;
import play.db.DB;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import utils.DateUtil;
import utils.ErrorInfo;
import utils.NumberUtil;
import utils.PageBean;
import utils.Security;
import models.t_dict_audit_items;
import models.t_user_audit_items;
import models.v_user_audit_item_stats;
import models.v_user_audit_items;

/**
 * 用户上传的审核资料
 * 
 * @author lwh
 * @version 6.0
 * @created 2014年5月9日 下午4:35:38
 */
public class UserAuditItem  implements Serializable{
	
	public long id;
	private long _id;
	public boolean lazy;
	public String no; // 编号
	public long userId; //用户ID
	public String signUserId;
	
	public Date time;  //上传时间
	//public double size; // 大小
	public int status; //状态
	public String strStatus; //状态
	public String imageFileName;
	public String imageFileNames; // json name数组
	public Date expireTime; //到期时间
	public long auditSupervisorId;  //审核人
	public Date auditTime; //审核时间
	public boolean isVisible; // 是否可见
	public String suggestion; // 审核意见
	public String mark; // 唯一标示
	private String _mark;
	public String userName; // 用户名
	
	public long auditItemId; //资料ID
	public AuditItem auditItem; // 资料库
	
	public boolean isOverBorrw; // 是否是超额借款
	public long overBorrowId; // 超额借款ID
	
	public List<String> items; // 资料集合
	public String strItems;
	public List<String> productNames; // 产品名称集合
	
	public String getStrItems() {
		if(null == this.strItems) {
			List<String> items = this.items;
			
			if(null == items || items.size() == 0)
				return null;
			
			this.strItems = StringUtils.join(items, ":") + ":";
		}
		
		return this.strItems;
	}

	/**
	 * 获取ID
	 */
	public long getId(){
		return _id;
	}

	/**
	 * 加密用户ID
	 */
	public String getSignUserId() {
		if(null == this.signUserId)
			this.signUserId = Security.addSign(this.userId, Constants.USER_ID_SIGN);
		
		return this.signUserId;
	}

	/**
	 * 获取mark
	 */
	public String getMark() {
		return _mark;
	}

	/**
	 * 字符串状态
	 */
	public String getStrStatus() {
		if(null == this.strStatus) {
			switch (this.status) {
				case Constants.UNCOMMITTED: this.strStatus = "未提交"; break;
				case Constants.AUDITING: this.strStatus = "审核中"; break;
				case Constants.AUDITED: this.strStatus = "通过审核"; break;
				case Constants.EXPIRED: this.strStatus = "过期失效"; break;
				case Constants.NOT_PASS: this.strStatus = "未通过审核"; break;
			}
		}
		
		return this.strStatus;
	}
	
	/**
	 * 获取状态
	 */
	public int queryStatus(long id) {
		Integer status = null;
		
		try {
			status = GenericModel.find("select status from t_user_audit_items where id = ?", id).first();
		} catch (Exception e) {
			Logger.error("用户资料->获取状态:" + e.getMessage());
			
			return -999;
		}
		
		return null == status ? -999 : status;
	}

	/**
	 * 用户名
	 */
	public String getUserName() {
		if(null == this.userName)
			this.userName = User.queryUserNameById(this.userId, new ErrorInfo());
		
		return this.userName;
	}

	/**
	 *  获取资料集合
	 */
	public List<String> getItems() {
		if(null == this.items) {
			String hql = "select image_file_name from v_user_audit_items where user_id = ? and audit_item_id = ? order by type";
			
			try {
				this.items = GenericModel.find(hql, this.userId, this.auditItemId).fetch();
			} catch (Exception e) {
				Logger.error("用户资料->查询用户同一资料集合:" + e.getMessage());
				
				return null;
			}
		}
		
		return this.items;
	}
	
	/**
	 * 关联借款标
	 */
	public List<String> getProductNames() {
		if(null == this.productNames)
			this.productNames = ProductAuditItem.queryProductName(this.auditItemId);
	
		return this.productNames;
	}

	/**
	 * 填充自己
	 */
	public void setId(long id){
		t_user_audit_items userItem = null;
		
		try {
			userItem = GenericModel.findById(id);
		} catch (Exception e) {
			Logger.error("用户资料->填充自己:" + e.getMessage());
			this._id = -1;
			
			return;
		}
		
		if (userItem == null) {
			this._id = -1;
			
			return;
		}
		
		if(this.lazy) {
			this.mark = userItem.mark;
			
			return;
		}
		
		this._id = id;
		this.time = userItem.time;
		this.imageFileName = userItem.image_file_name;
		this.expireTime = userItem.expire_time;
		this.auditTime = userItem.audit_time;
		this.suggestion = userItem.suggestion;
		this.userId = userItem.user_id;
		this.auditItemId = userItem.audit_item_id;
		this.status = userItem.status;
		this.auditSupervisorId = userItem.audit_supervisor_id == null ? 0 :userItem.audit_supervisor_id;
		this.isVisible = userItem.is_visible == null ? false : userItem.is_visible;
		this.overBorrowId = userItem.over_borrow_id  == null ? 0 : userItem.over_borrow_id;
		this.isOverBorrw = userItem.is_over_borrow  == null ? false : userItem.is_over_borrow;
	}
	
	/**
	 * 填充自己
	 */
	public void setMark(String mark) {
		t_user_audit_items userItem = null;

		try {
			userItem = GenericModel.find(" mark = ? and user_id = ?", mark, this.userId).first();
		} catch (Exception e) {
			Logger.error("用户资料->填充自己:" + e.getMessage());
			this._id = -1;
			
			return;
		}
		
		if (userItem == null) {
			this._id = -1;
			
			return;
		}
		
		this._id = userItem.id;;
		this._mark = userItem.mark;
		this.userId = userItem.user_id;
		this.time = userItem.time;
		this.imageFileName = userItem.image_file_name;
		this.expireTime = userItem.expire_time;
		this.auditTime = userItem.audit_time;
		this.suggestion = userItem.suggestion;
		this.status = userItem.status;
		this.isVisible = userItem.is_visible == null ? false : userItem.is_visible;
		this.isVisible = userItem.is_visible == null ? false : userItem.is_visible;
		this.overBorrowId = userItem.over_borrow_id  == null ? 0 : userItem.over_borrow_id;

		this.auditItemId = userItem.audit_item_id;
		this.auditItem = new AuditItem();
		this.auditItem.lazy = this.lazy;
		this.auditItem.mark = mark;
	}

	/**
	 * 查询用户针对产品(mark标示)通过/未通过的资料数
	 * (原生态调用了函数，和发送sql语句等效，可自行修改)
	 * @param userId 用户ID
	 * @param mark 唯一标示
	 * @param status 状态
	 * @return 
	 */
	public static int queryUserItemScale(long userId, String mark, int status) {
		String sql = "select f_user_audit_item(?, ?, ?)";
		Connection conn = DB.getConnection();
		CallableStatement cstmt = null;
		ResultSet rs = null;
		int result = 0;

		try {
			cstmt = conn.prepareCall(sql);
			cstmt.setLong(1, userId);
			cstmt.setString(2, mark);
			cstmt.setInt(3, status);
			cstmt.execute();
			rs = cstmt.getResultSet();

			if (rs.next()) 
				result = rs.getInt(1);
		} catch (SQLException e) {
			Logger.error("用户资料-> 用户资料正对产品通过率:" + e.getMessage());

			return 0;
		} finally {
			try {
				rs.close();
				cstmt.close();
				conn.close();
			} catch (SQLException e) {
				Logger.error("用户资料-> 用户资料正对产品通过率,关闭JDBC对象:" + e.getMessage());
			}
		}
		
		return result;
	}
	
//	/**
//	 * 获得特定用户上传的某种资料的最新记录
//	 * @param userId
//	 * @param auditItemId
//	 * @return
//	 */
//	public static UserAuditItem queryLatestRecord(long userId, long auditItemId) {
//
//		int count = 0;
//		String mark = null;
//		String hql = null;
//		v_user_audit_items item = null;
//		UserAuditItem userItem = new UserAuditItem();
//
//		try {
//			count = (int) v_user_audit_items.count("user_id = ? and audit_item_id = ?", userId, auditItemId);
//		} catch (Exception e) {
//			Logger.error("用户资料->获得特定用户上传的某种资料的最新记录：" + e.getMessage());
//
//			return null;
//		}
//
//		if (count < 1)
//			return userItem;
//
//		if (count == 1) {
//			hql = "select mark from v_user_audit_items where user_id = ? and audit_item_id = ?";
//
//			try {
//				mark = v_user_audit_items.find(hql, userId, auditItemId).first();
//			} catch (Exception e) {
//				Logger.error("用户资料->获得特定用户上传的某种资料的最新记录：" + e.getMessage());
//
//				return null;
//			}
//		} else if (count > 1) {
//			try {
//				item = v_user_audit_items.find("user_id = ? and audit_item_id = ? order by  audit_time ",userId, auditItemId).first();
//			} catch (Exception e) {
//				Logger.error("用户资料->获得特定用户上传的某种资料的最新记录：" + e.getMessage());
//
//				return null;
//			}
//
//			if (null == item)
//				return userItem;
//
//			/* 说明该资料在审核中的，找出之前相同资料的状态 */
//			if (item.audit_time == null) {
//				hql = "select mark from v_user_audit_items where user_id = ? amd audit_item_id = ? order by  audit_time desc";
//				
//				try {
//					mark = v_user_audit_items.find(
//							"user_id = ? and audit_item_id = ?  order by  audit_time desc ",
//							userId, auditItemId
//							).first();
//				} catch (Exception e) {
//					Logger.error("用户资料->获得特定用户上传的某种资料的最新记录：" + e.getMessage());
//
//					return null;
//				}
//			}else{
//				mark = item.mark;
//			}
//		}
//
//		userItem.mark = mark;
//		
//		return userItem;
//	}
//
//	/**
//	 * 得到用户所有上传资料的最新记录
//	 * @param userId
//	 * @return
//	 */
//	public static List<UserAuditItem> queryUserAllAuditItem(long userId){
//		List<Long> idList = t_dict_audit_items.find("select id from t_dict_audit_items").fetch();
//		List<UserAuditItem> userAuditItemList = new ArrayList<UserAuditItem>();
//
//		for (Long i : idList) {
//			userAuditItemList.add(UserAuditItem.queryLatestRecord(userId, i));
//		}
//		
//		return userAuditItemList;
//	}
	
	/**
	 * 得到针对产品上传资料
	 * @param userId 用户ID
	 * @param mark 唯一标示
	 * @return List<UserAuditItem>
	 */
	public static List<UserAuditItem> queryUserAllAuditItem(long userId, String mark){
		List<UserAuditItem> items = new ArrayList<UserAuditItem>();
		List<t_user_audit_items> tuItems = null;
		String hql = "select new t_user_audit_items(audit_item_id, mark) from t_user_audit_items where user_id = ? group by audit_item_id";
		
		try {
			tuItems = GenericModel.find(hql, userId).fetch();
		} catch (Exception e) {
			Logger.error("用户资料->得到针对产品上传资料,查询用户的资料ID和mark："+e.getMessage());
			
			return null;
		}
		
		if(null == tuItems || tuItems.size() == 0)
			return items;
		
		List<ProductAuditItem> pitems = ProductAuditItem.queryAuditByProductMark(mark, false, true);
		
		if(null == pitems || pitems.size() == 0)
			return items;
		
		UserAuditItem item = null;
		/* 为了比对历史数据，需要抓取用户的mark，数据量虽然不大，但是还是值得优化 */
		for (ProductAuditItem pitem : pitems) {
			item = new UserAuditItem();
			
			for (t_user_audit_items tuItem : tuItems) {
				
				if(tuItem.audit_item_id == pitem.auditItemId){
					item.lazy = true;
					item.userId = userId;
					item.mark = tuItem.mark;
					
					items.add(item);
				}
			}
		}
		
		return items;
	}
	
	/**---------------------------------------------------查询(联合资料库)------------------------------------------------------*/
	
	/**
	 * 查询用户已上传通过/未提交的资料
	 * @param userId 用户Id
	 * @param isPass true : 通过; false : 其它状态
	 * @return List<Long>
	 */
	public static List<Long> queryUserAuditItem(long userId, boolean isPass) {
		String hql = "select audit_item_id from t_user_audit_items where user_id = ?";
		
		if(isPass) 
			hql += " and status = 2";
		
		hql += "group by audit_item_id";
		
		try {
			return GenericModel.find(hql, userId).fetch();
		}catch(Exception e) {
			Logger.error("用户资料->查询有效资料时："+e.getMessage());
			
			return null;
		}
	}
	
	/**
	 * 会员借款资料审核管理
	 * @param pageBean 分页对象
	 * @param error 信息值
	 * @return List<v_audit_item_user>
	 */
	public static List<v_user_audit_item_stats> queryUserAuditItemInAdmin(PageBean<v_user_audit_item_stats> pageBean, ErrorInfo error, String ... str){
		error.clear();
		
		int count = -1;
		StringBuffer conditions = new StringBuffer(" 1 = 1"); 
		List<Object> values = new ArrayList<Object>(); 
		Map<String, Object> conditionmap = new HashMap<String, Object>(); 
		
		/* 条件 */
		if (NumberUtil.isNumericInt(str[0])) {
			
			switch (Integer.parseInt(str[0])) {
			/* 名称搜索 */
			case Constants.ITEM_SEARCH_NAME:
				conditions.append(" AND user_name LIKE ?");
				values.add("%" + str[1] + "%");

				break;

			/* 邮箱搜索 */
			case Constants.ITEM_SEARCH_EMAIL:
				conditions.append(" AND email LIKE ?");
				values.add("%" + str[1] + "%");

				break;
			
			/* 全部搜索 */
			case Constants.SEARCH_ALL:

				if (StringUtils.isBlank(str[1]))
					break;

				conditions.append(" AND (user_name LIKE ? OR email LIKE ?)");
				values.add("%" + str[1] + "%");
				values.add("%" + str[1] + "%");

				break;
			}
			
			conditionmap.put("condition", str[0]);
			conditionmap.put("keyword", str[1]);
		}
		
		/* 排序 */
		if (NumberUtil.isNumericInt(str[2])) {
			int _order = Integer.parseInt(str[2]);
			conditions.append(Constants.ITEMS_SEARCH_ORDER[_order]);
			
			conditionmap.put("orderIndex", _order);
			
			/* 升降序 */
			if(NumberUtil.isNumericInt(str[3]) && _order > 0){
				
				if(Integer.parseInt(str[3]) == 1)
					conditions.append("ASC");
				else
					conditions.append("DESC");
				
				/* 保存当前索引值 + 升降值 */
				conditionmap.put("orderStatus", str[3]);
			}
		}else{
			conditions.append(Constants.ITEMS_SEARCH_ORDER[0]);
		}
		
		pageBean.conditions = conditionmap;
		
		try {
			count = (int)GenericModel.count(conditions.toString(), values.toArray());
		} catch (Exception e) {
			Logger.error("用户资料->会员借款资料审核管理,查询总记录数:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载会员借款资料列表失败!";
		}
		
		if(count < 1)
			return new ArrayList<v_user_audit_item_stats>();
			
		pageBean.totalCount = count;
		
		try {
			return GenericModel.find(conditions.toString(), values.toArray()).fetch(pageBean.currPage, pageBean.pageSize);
		} catch (Exception e) {
			Logger.error("用户资料->会员借款资料审核管理:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载会员借款资料列表失败!";
			
			return null;
		}
	}
	
	/**
	 * 借款资料审核认证
	 * @param pageBean 分页对象
	 * @param error 信息值
	 */
	public static PageBean<v_user_audit_items> queryUserAuditItem(String currPage, String pageSize, long userId, ErrorInfo error, String ... str){
		error.clear();
		
		if(0 == userId) return null;
		
		PageBean<v_user_audit_items> pageBean = new PageBean<v_user_audit_items>();
		pageBean.currPage = NumberUtil.isNumericInt(currPage)? Integer.parseInt(currPage): 1;
		pageBean.pageSize = NumberUtil.isNumericInt(pageSize)? Integer.parseInt(pageSize): 30;
		
		Map<String, Object> conditionmap = new HashMap<String, Object>();
		StringBuffer conditions = new StringBuffer(" user_id = ?"); 
		List<Object> values = new ArrayList<Object>(); 
		values.add(userId);

		/* 存在状态搜索条件 */
		if(NumberUtil.isNumericInt(str[0])) {
			int status = Integer.parseInt(str[0]);
			
			/* 状态不为全部  */
			if(status != Constants.SEARCH_ALL_TEN) {
				conditions.append(" and status = ?");
				values.add(status);
				conditionmap.put("status", status);
			}
		}
		
		/* 开始时间 */
		if(StringUtils.isNotBlank(str[1])){
			conditions.append(" and time >= ?");
			values.add(DateUtil.strToYYMMDDDate(str[1]));
			conditionmap.put("startDate", str[1]);
		}
		
		/* 结束时间 */
		if(StringUtils.isNotBlank(str[2])){
			conditions.append(" and time <= ?");
			values.add(DateUtil.strToYYMMDDDate(str[2]));
			conditionmap.put("endDate", str[2]);
		}
		
		/* 关联借款标 */
		if(StringUtils.isNotBlank(str[3])){
			long productId = Long.parseLong(str[3]);

			if (productId != Constants.SEARCH_ALL) {
				conditions.append(" and audit_item_id in (select audit_item_id from t_product_audit_items_log where product_id = ?");
				values.add(productId);
				conditionmap.put("productId", productId);
			
				/* 是否必选 */
				if(StringUtils.isNotBlank(str[4])){
					int type = Integer.parseInt(str[4]);
				
					if(type != Constants.SEARCH_ALL_TEN){
						conditions.append(" and type = ? ");
						values.add(type == 1 ? true : false);
						conditionmap.put("type", type);
					}
				}
			
				conditions.append(")");
			}
		}
		
		/* 分组去重复  */
		conditions.append(" group by audit_item_id");
		pageBean.conditions = conditionmap;
		
		Long count = -1l;
		List<v_user_audit_items> items = null;
		String hql = "select count(id) from v_user_audit_items where " + conditions.toString();
		
		try {
			count = GenericModel.find(hql, values.toArray()).first();
		} catch (Exception e) {
			Logger.error("用户资料->借款资料审核认证,查询总记录:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载会员借款资料列表失败!";
		}
		
		if(null == count || count <= 0) 
			return pageBean;
		
		pageBean.totalCount = Integer.parseInt(count.toString()); // 分组不支持count函数
		
		try {
			items = GenericModel.find(conditions.toString(), values.toArray()).fetch(pageBean.currPage, pageBean.pageSize);
		} catch (Exception e) {
			Logger.error("用户资料->借款资料审核认证:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载会员借款资料列表失败!";

			return null;
		}
		
		pageBean.page = items;
		
		return pageBean;
	}
	
	/**
	 * 用户、产品、资料列表
	 * @param userId
	 * @param productId
	 * @param error
	 * @return
	 */
	public static List<v_user_audit_items> queryUserAuditItem(long userId, long productId, ErrorInfo error){
		error.clear();
		
		String hql = "user_id = ? and audit_item_id in (select audit_item_id from t_product_audit_items_log where product_id = ?) group by audit_item_id";
		List<v_user_audit_items> items = null;
		
		try {
			items = GenericModel.find(hql, userId, productId).fetch();
		} catch (Exception e) {
			Logger.error("用户资料->借款资料审核认证:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载会员借款资料列表失败!";

			return null;
		}
		
		return items;
	}
	
	/**
	 * 审核明细统计
	 * @param userId 用户ID
	 * @param error 信息值
	 * @return Map<String, Integer> 
	 */
	public static Map<String, Integer> auditItemsStatistics(long userId, ErrorInfo error){
		
		if(0 == userId) return null;
		
		int sumCount = -1; // 合计资料
		int uncommitted = -1; //未提交
		int auditing = -1; //审核中
		int audited = -1; //通过审核
		int expired = -1; //过期失效 
		int notPass = -1; //未通过
		
		try {
			sumCount = (int)GenericModel.count(" user_id = ?", userId);
			uncommitted = (int)GenericModel.count(" user_id = ? and status = ?", userId, Constants.UNCOMMITTED);
			auditing = (int)GenericModel.count(" user_id = ? and status = ?", userId, Constants.AUDITING);
			audited = (int)GenericModel.count(" user_id = ? and status = ?", userId, Constants.AUDITED);
			expired = (int)GenericModel.count(" user_id = ? and status = ?", userId, Constants.EXPIRED);
			notPass = (int)GenericModel.count(" user_id = ? and status = ?", userId, Constants.NOT_PASS);
		} catch (Exception e) {
			Logger.error("用户资料->审核明细统计:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "资料统计失败!";
			
			return null;
		}
		
		Map<String, Integer> statistics = new HashMap<String, Integer>();
		
		statistics.put("sumCount", sumCount);
		statistics.put("uncommitted", uncommitted);
		statistics.put("auditing", auditing);
		statistics.put("audited", audited);
		statistics.put("expired", expired);
		statistics.put("notPass", notPass);
		
		return statistics;
	}
	
	/**
	 * 查询资料
	 * @param userid 用户ID
	 * @param itemid 资料ID
	 * @param error 信息值
	 * @return List<UserAuditItem> 
	 */
	public static List<v_user_audit_items> querySameAuditItem(long userId, long auditItemId, ErrorInfo error) {
		error.clear();

		try {
			return GenericModel.find("user_id = ? and audit_item_id = ? order by time", userId, auditItemId).fetch();
		} catch (Exception e) {
			Logger.error("用户资料->查询相同的审核资料:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载用户资料失败!";

			return null;
		}
	}
	
	/**
	 * 查询超额借款提交的审核资料
	 * @param overBorrowId
	 * @param error
	 * @return
	 */
	public static List<t_user_audit_items> queryAuditItemsByOverBorrowId(long overBorrowId, ErrorInfo error) {
		error.clear();
		
		String sqlCondition = "id in (select id from t_user_audit_items where over_borrow_id = ?)";
		List<t_user_audit_items> items = null;
		
		try {
			items = GenericModel.find(sqlCondition, overBorrowId).fetch();
		}catch(Exception e) {
			Logger.error("查询超额借款可提交的审核资料时："+e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return null;
		}
		
		error.msg = "查询超额借款审核资料成功";
		
		return items;
	}
	
	/**
	 * 查询超额借款可提交的审核资料
	 * @param userId
	 * @param error
	 * @return
	 */
	public static List<AuditItem> queryAuditItemsOfOverBorrow(long userId, ErrorInfo error) {
		error.clear();
		
		String sqlCondition = "id not in (select distinct audit_item_id from t_user_audit_items where user_id = ? and status = ?)";
		List<t_dict_audit_items> tItems = null;
		
		try {
			tItems = GenericModel.find(sqlCondition, userId, Constants.AUDITED).fetch();
		}catch(Exception e) {
			Logger.error("查询超额借款可提交的审核资料时："+e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			return null;
		}
		
		String noPrefix = OptionKeys.getvalue(OptionKeys.AUDIT_ITEM_NUMBER, error);

		if (null == noPrefix) {
			noPrefix = "ZL";
		}
		
		List<AuditItem> auditItems = new ArrayList<AuditItem>();
		AuditItem auditItem = null;

		for (t_dict_audit_items item : tItems) {
			auditItem = new AuditItem();
			auditItem.id = item.id;
			
			auditItems.add(auditItem);
		}
		
		error.msg = "查询可提交的超额借款审核资料成功";
		
		return auditItems;
	}
	
	/**--------------------------------------------------标关联审核资料-----------------------------------------------------------------*/

	/**
	 * 添加标对应的审核资料(及用户上传的审核资料)
	 * @param productItem 产品必审资料集合
	 * @param userItem 用户ID集合
	 * @param info 错误信息
	 * @return ? > 0 : success; ? < 0 : fail
	 */
	public static int addBidAuditItem(long userId, String mark, ErrorInfo error) {
		error.clear();
		
		/* 得到用户已经提交的资料ID集合 */
		List<Long> userItem = UserAuditItem.queryUserAuditItem(userId, false);
		/* 产品所有需要提交的资料 */
		List<ProductAuditItem> productItem = ProductAuditItem.queryAuditByProductMark(mark, true, true);
		/* 得到没有提交的资料 */
		List<Long> itemId = UserAuditItem.getNotUploadItemId(productItem, userItem);

		if(itemId.size() == 0)
			return 1;
		
		t_user_audit_items item = null;

		for (long id : itemId) {
			item = new t_user_audit_items();

			item.user_id = userId; //用户ID
			item.time = new Date();
			item.audit_item_id = id;
			item.is_over_borrow = Constants.NOT_ENABLE; // 不关联超额借款
			item.over_borrow_id = 0l; // 默认为0
			item.audit_supervisor_id = 0l; // 审核人默认为0
			item.status = Constants.UNCOMMITTED; // 未提交
			item.is_visible = Constants.NOT_ENABLE; // 不可见
			item.mark = AuditItem.queryMark(id); // 唯一标示
			
			try {
				item.save();
			} catch (Exception e) {
				Logger.error("资料->添加标对应的审核资料:" + e.getMessage());
				error.msg = "添加标对应的审核资料失败!";
				
				return -1;
			}
		}

		DealDetail.userEvent(userId, UserEvent.ADD_USER_AUDIT_ITEM, "添加用户需上传的审核资料", error);
			
		if(error.code < 0) 
			return -2;
		
		return 1;
	}
	
	/**
	 * 得到用户针对标没有上传的审计资料ID集合
	 * @param productItems
	 * @param userItems
	 * @return List<Long>
	 */
	public static List<Long> getNotUploadItemId(List<ProductAuditItem> productItems, List<Long> userItems) {
		if(null == productItems || userItems == null)
			return null;
		
		long id = -1;
		List<Long> notUploadItemId = new ArrayList<Long>(); // 申明没有出现的审核资料ID
		
		/* 如果用户暂无提交资料 */
		if(userItems.size() == 0){
			
			for (ProductAuditItem item : productItems) {
				notUploadItemId.add(item.auditItemId); // 得到没有出现的审核资料ID
			}
		}else if(productItems.size() > 0){
			/* 比较 */
			for (ProductAuditItem item : productItems) {
				id = item.auditItemId;
				
				if (userItems.contains(id)) {
					continue;  //出现则跳转
				}

				notUploadItemId.add(id); // 得到没有出现的审核资料ID
			}
		}
		
		return notUploadItemId;
	}
	
	/**---------------------------------------------------改变状态------------------------------------------------------*/
	
	/**
	 * 检查资料是否过期
	 */
	public static void checkAuditItemIsExpired(){
		List<Long> ids = null;
		String hql = "select id from t_user_audit_items where status = ? and NOW() > expire_time";
		
		try {
			ids = GenericModel.find(hql, Constants.AUDITED).fetch();
		} catch (Exception e) {
			Logger.error("资料->检查资料是否过期:" + e.getMessage());
			
			return;
		}
		
		if(null == ids || ids.size() == 0) return;
		
		StringBuffer buffer = new StringBuffer();
		
		for (Long id : ids) {
			buffer.append(id).append(",");
		}
		
		String strId = buffer.toString();
		strId = strId.substring(0, strId.length() - 1);

		hql = "update t_user_audit_items set status = ? where id in (" + strId + ")";
		Query query = JPA.em().createQuery(hql);
		query.setParameter(1, Constants.EXPIRED);	
		
		try {
			query.executeUpdate();
		} catch (Exception e) {
			Logger.error("资料->检查资料是否过期，修改过期状态:" + e.getMessage());
			
			return;
		}
	}
	
	/**
	 * 提交资料
	 * @param useritemId 用户资料ID
	 * @param filename 资源路径
	 * @param error 信息值
	 * @return ? > 0 : success; ? < 0 : fail 
	 */
	public void createUserAuditItem(ErrorInfo error) {
		error.code = -1;
		
		/* 通过的资料不给予提交 */
		if(this.status == Constants.AUDITED){
			error.code = -1;
			error.msg = "资料已通过,无需再提交!";
			
			return;
		}
		
		/* 不为审核中 */
		if(this.status != Constants.AUDITING){
			/* 1.删除资料 */
			String hql = "delete from t_user_audit_items where audit_item_id = ? and user_id = ? and status <> 1";
			
			Query query = JPA.em().createQuery(hql);
			query.setParameter(1, this.auditItemId);
			query.setParameter(2, this.userId);
			
			try {
				error.code = query.executeUpdate();
			} catch (Exception e) {
				Logger.error("用户资料->提交资料,删除以前作废的资料:" + e.getMessage());
				error.msg = "提交失败!";
				
				return;
			}
			
			if(error.code < 1){
				error.msg = "提交失败!";
				
				return;
			}
		}
		
		/* 2.添加资料 */
		Date date = new Date();
		String [] names = null;
		
		try {
			names = this.imageFileNames.split(",");
		} catch (Exception e) {
			error.msg = "提交失败,请您不要禁用浏览器脚本语言!";
			
			return;
		}
		
		t_user_audit_items item = null;
		
		for (String image : names) {
			item = new t_user_audit_items();
			item.status = Constants.AUDITING;
			item.user_id = this.userId;
			item.time = date;
			item.audit_item_id = this.auditItemId;
			item.image_file_name = image;
			item.is_over_borrow = this.isOverBorrw;
			item.over_borrow_id = this.overBorrowId;
			item.is_visible = Constants.NOT_ENABLE; // 默认为可见
			item.mark = this.mark;
			
			try {
				item.save();
			} catch (Exception e) {
				Logger.error("用户资料->提交资料,删除以前作废的资料:" + e.getMessage());
				error.msg = "提交失败!";
				JPA.setRollbackOnly();
				
				return;
			}
			
			if(item.id < 1){
				error.msg = "提交失败!";
				
				return;
			}
			
			/* 添加事件 */
			DealDetail.userEvent(this.userId, UserEvent.SUMBIT_AUDIT_ITEM, "用户提资料", error);

			if(error.code < 0){
				error.msg = "添加事件失败!";
				JPA.setRollbackOnly();
				
				return;
			}
		}
		
		/* 回调数据 */
		this.status = item.status;
		this.time = item.time;
		
		error.code = 1;
	}
	
//	public t_user_audit_items queryUserAuditItems(long auditItemId, long userId) {
//		/* 查询是否关联超额借款 */
//		String hql = "select new t_user_audit_items(is_over_borrow, over_borrow_id, mark) from t_user_audit_items where audit_item_id = ? and user_id = ?";
//		
//		try {
//			return t_user_audit_items.find(hql, auditItemId, userId).first();
//		} catch (Exception e) {
//			Logger.error("用户资料-> 查询是否关联超额借款:" + e.getMessage());
//			
//			return null;
//		}
//	}
	
	/**
	 * 审核用户资料
	 * @param mark 标示
	 * @param status 状态
	 * @param isVisible 是否可见
	 * @param supervisorId 审核人
	 * @param suggestion 审核意见
	 * @param error 信息值
	 */
	public void audit(long supervisorId, int auditStatus, boolean isVisible, String suggestion, ErrorInfo error){
		error.clear();
		
		DataSafety data = new DataSafety();
		data.setId(this.userId);
		data.signCheck(error);
		
		if(error.code < 0){
			error.msg = "审核失败!";

			return;
		}
		
		if( auditStatus == Constants.AUDITED && this.status == Constants.AUDITED ||
			auditStatus == Constants.NOT_PASS && this.status == Constants.NOT_PASS
			){
			error.msg = "请确定是否已经审核过!";
			
			return;
		}
		
		Map<String, Double> detail = DealDetail.queryUserFund(this.userId, error);
		
		if(null == detail){
			error.code = -1;
			error.msg = "审核失败!";
			
			return;
		}
		
		double user_amount = detail.get("user_amount");
		double user_amount2 = detail.get("user_amount2");
		
		if (Constants.IPS_ENABLE) {
			if (this.auditItem.auditFee > user_amount2) {
				error.code = -2;
				error.msg = "用户可用余额不足,无法扣除审核费用!";
				
				return;
			}
		} else if (this.auditItem.auditFee > user_amount){
			error.code = -2;
			error.msg = "用户可用余额不足,无法扣除审核费用!";
			
			return;
		}
		
		Date date = new Date();  // 审核时间
		/* 修改其审核的一些添加条件 */
		String hql = "update t_user_audit_items set status = ?, is_visible = ?, audit_supervisor_id = ?,"
				   + "suggestion = ?, audit_time = ? where user_id =? and audit_item_id = ? and status = ?";
		
		Query query = JPA.em().createQuery(hql);
		query.setParameter(1, auditStatus);
		query.setParameter(2, isVisible);
		query.setParameter(3, supervisorId);
		query.setParameter(4, suggestion.trim());
		query.setParameter(5, date);
		query.setParameter(6, this.userId);
		query.setParameter(7, this.auditItemId);
		query.setParameter(8, Constants.AUDITING);
		
		try {
			error.code = query.executeUpdate();
		} catch (Exception e) {
			Logger.error("用户资料->审核用户资料:" + e.getMessage());
			error.code = -3;
			error.msg = error.FRIEND_INFO + "审核用户资料失败!";
			
			return;
		}
		
		if(error.code < 1) {
			error.code = -4;
			error.msg = error.FRIEND_INFO + "审核用户资料失败!";
			
			return;
		}
		
		User user = new User();
		user.createBid = true;
		user.id = this.userId;
		
		String descrption = null;
		String content = null;
		TemplateStation station = new TemplateStation();
		TemplateEmail email = new TemplateEmail();
		TemplateSms sms = new TemplateSms();
		
		switch (auditStatus) {
		/*  审核通过 */
		case Constants.AUDITED:
			/* 更新资料的过期时间  */
			String updatetime = "update t_user_audit_items set expire_time = ? where user_id =? and audit_item_id = ?";
			
			query = JPA.em().createQuery(updatetime);
			query.setParameter(1, DateUtil.dateAddMonth(date, this.auditItem.period));
			query.setParameter(2, this.userId);
			query.setParameter(3, this.auditItemId);
			
			try {
				error.code = query.executeUpdate();
			} catch (Exception e) {
				Logger.error("用户资料->更新资料的过期时间 :" + e.getMessage());
				error.code = -5;
				error.msg = error.FRIEND_INFO + "审核用户资料失败!";
				JPA.setRollbackOnly();
				
				return;
			}
			
			/* 扣除审核费用 */
			if (Constants.IPS_ENABLE) {
				error.code = DealDetail.minusUserFund2(this.userId, this.auditItem.auditFee);
			} else {
				error.code = DealDetail.minusUserFund(this.userId, this.auditItem.auditFee);
			}

			if(error.code < 1){
				JPA.setRollbackOnly();
				error.msg = "审核失败!";
				
				return;
			}
			
			detail = DealDetail.queryUserFund(this.userId, error);
			
			DealDetail dealDetail = new DealDetail(this.userId,
					DealType.CHARGE_AUDIT_ITEM, this.auditItem.auditFee,
					this.id, detail.get("user_amount"), detail.get("freeze"),
					detail.get("receive_amount"), "审核用户资料扣除审核费用");

			dealDetail.addDealDetail(error);
			
			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.msg = "审核失败!";

				return;
			}
			
			/* 添加风险金 */
			DealDetail.addPlatformDetail(DealType.LOAN_FEE, this.id, this.userId, -1,
					DealType.ACCOUNT, this.auditItem.auditFee, 1,
					"审核用户资料，扣除审核资料费", error);
			
			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.msg = "审核失败!";

				return;
			}
			
			data.setId(this.userId);
			data.updateSign(error);
			
			if(error.code < 0){
				JPA.setRollbackOnly();
				error.msg = "审核失败!";

				return;
			}
			
			/* 添加用户的信用积分 */
			DealDetail.addCreditScore(this.userId, 1, this.auditItem.creditScore, this.id, "审核资料通过,添加用户的信用积分", error);
			
			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.msg = "审核失败!";

				return;
			}
			
			this.status = Constants.AUDITED;
			descrption = "审核用户资料通过";
			
			station.setId(Templets.M_AUDIT_ITEM_SUCCESS);
			email.setId(Templets.E_AUDIT_ITEM_SUCCESS);
			sms.setId(Templets.S_AUDIT_ITEM_SUCCESS);
			
			if(station.status){
				content = this.auditItemPassNotice(station.content, user.name, suggestion);
				TemplateStation.addMessageTask(this.userId, station.title, content);
			}
			
			if(email.status){
				content = this.auditItemPassNotice(email.content, user.name, suggestion);
				TemplateEmail.addEmailTask(user.email, email.title, content);
			}
			
			if(sms.status){
				content = this.auditItemPassNotice(sms.content, user.name, suggestion);
				TemplateSms.addSmsTask(user.mobile, content); 
			}
			
			break;
			
		/* 审核不通过 */
		case Constants.NOT_PASS:
			this.status = Constants.AUDITED;
			descrption = "审核用户资料不通过";
			
			station.setId(Templets.M_AUDIT_ITEM_FAIL);
			email.setId(Templets.E_AUDIT_ITEM_FAIL);
			sms.setId(Templets.S_AUDIT_ITEM_FAIL);
			
			if(station.status){
				content = this.auditItemNotPassNotice(station.content, user.name, suggestion);
				TemplateStation.addMessageTask(this.userId, station.title, content);
			}
			
			if(email.status){
				content = this.auditItemNotPassNotice(email.content, user.name, suggestion);
				TemplateEmail.addEmailTask(user.email, email.title, content);
			}
			
			if(sms.status){
				content = this.auditItemNotPassNotice(sms.content, user.name, suggestion);
				TemplateSms.addSmsTask(user.mobile, content); 
			}
			
			break;
			
		default:
			error.code = -11;
			error.msg = "非法审核!";
			JPA.setRollbackOnly();
			
			return;
		}
		
		this.time = date;
		
		/* 添加事件 */
		DealDetail.supervisorEvent(Supervisor.currSupervisor().id, SupervisorEvent.AUDIT_USER_ITEM, descrption, error);
		
		if(error.code < 0){
			JPA.setRollbackOnly();
			error.msg = "审核失败!";
			
			return;
		}
	}
	
	/**
	 * 审核资料通过
	 */
	private String auditItemPassNotice(String content, String userName, String suggestion){
		
		content = content.replace("userName", userName);
		content = content.replace("date", DateUtil.dateToString((new Date())));
		content = content.replace("itemName", this.auditItem.name); 
		content = content.replace("auditFee", this.auditItem.auditFee + ""); 
		content = content.replace("creditScore", this.auditItem.creditScore + ""); 
	 
		return content;  
	}
	
	/**
	 * 审核资料不通过
	 */
	private String auditItemNotPassNotice(String content, String userName, String suggestion){
		content = content.replace("userName", userName);
		content = content.replace("date", DateUtil.dateToString((new Date())));
		content = content.replace("itemName", this.auditItem.name); 
		content = content.replace("content", suggestion); 
	 
		return content;                                                     
	}
	
	/**
	 * 删除用户资料 
	 * @param id 用户资料ID
	 * @param error 信息值
	 */
	public static void deleteAuditItem(long id, ErrorInfo error){
		error.clear();

		String hql = "delete from t_user_audit_items where id=? ";
		
		Query query = JPA.em().createQuery(hql);
		query.setParameter(1, id);
		
		try {
			error.code = query.executeUpdate();
		} catch (Exception e) {
			Logger.error("用户资料->删除用户资料:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "删除用户资料失败!";

			return;
		}

		if(error.code < 1){
			error.msg = error.FRIEND_INFO + "删除失败!";
			
			return;
		}
		
		/* 添加事件 */
		DealDetail.userEvent(User.currUser().id, UserEvent.DELETE_AUDIT_ITEM, "用户删除资料", error);
		
		if(error.code < 0){
			JPA.setRollbackOnly();
			error.msg = error.FRIEND_INFO + "删除失败!";
			
			return;
		}
	}
	
	/**--------------------------------------------上一个、下一个-------------------------------------------------*/
	
	public String upId; // 上个ID
	public String nextId; // 下个ID 
	public int countUpId; 
	public int countNextId; 
	
	/**
	 * 获取上个Id
	 */
	public String getUpId() {
		Long upId = null;
		String errorKey = Security.addSign(this.userId, Constants.USER_ID_SIGN);
		
		try {
			upId = GenericModel.find("select MAX(id) from v_user_audit_item_stats where id < ?", this.userId).first();
		} catch (Exception e) {
			return errorKey;
		}
		
		if(null == upId){
			return errorKey;
		}
		
		return Security.addSign(upId.longValue(), Constants.USER_ID_SIGN);
	}
	
	/**
	 * 下个Id
	 */
	public String getNextId() {
		Long nextId = null;
		String errorKey = Security.addSign(this.userId, Constants.USER_ID_SIGN);
		
		try {
			nextId = GenericModel.find("select MIN(id) from v_user_audit_item_stats where id > ?", this.userId).first();
		} catch (Exception e) {
			return errorKey;
		}
		
		if(null == nextId)
			return errorKey;
		
		return Security.addSign(nextId.longValue(), Constants.USER_ID_SIGN);
	}

	/**
	 * 上一个ID统计
	 */
	public int getCountUpId() {
		try {
			return (int)GenericModel.count("id < ?", this.userId);
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * 下一个ID统计
	 */
	public int getCountNextId() {
		try {
			return (int)GenericModel.count("id > ?", this.userId);
		} catch (Exception e) {
			return -1;
		}
	}
}
