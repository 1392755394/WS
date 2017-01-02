package controllers.front.invest;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import constants.Constants;
import constants.IPSConstants;
import constants.IPSConstants.IPSOperation;
import constants.IPSConstants.IpsCheckStatus;
import controllers.BaseController;
import controllers.front.account.CheckAction;
import net.sf.json.JSONObject;
import models.v_front_all_bids;
import models.v_front_user_attention_bids;
import models.v_invest_records;
import business.Bid;
import business.BidQuestions;
import business.CreditLevel;
import business.Invest;
import business.Payment;
import business.Product;
import business.User;
import business.UserAuditItem;
import play.cache.Cache;
import utils.ErrorInfo;
import utils.NumberUtil;
import utils.PageBean;
import utils.Security;

/**
 * 
 * @author liuwenhui
 * 
 */
public class InvestAction extends BaseController {

	/**
	 * 我要理财首页
	 */

	public static void investHome() {

		ErrorInfo error = new ErrorInfo();
		List<Product> list = Product.queryProductNames(true, error);
		Long totalCount = Invest.getBidCount(error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		List<Product> products = Product.queryProductNames(true, error);

		List<CreditLevel> creditLevels = CreditLevel
				.queryAllCreditLevels(error);
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		String currPageStr = params.get("currPage");
		String pageSizeStr = params.get("pageSize");

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		String apr = params.get("apr");
		String amount = params.get("amount");
		String loanSchedule = params.get("loanSchedule");
		String startDate = params.get("startDate");
		String endDate = params.get("endDate");
		String loanType = params.get("loanType");
		String minLevel = params.get("minLevel");
		String maxLevel = params.get("maxLevel");
		String orderType = params.get("orderType");
		String keywords = params.get("keywords");

		PageBean<v_front_all_bids> pageBean = new PageBean<v_front_all_bids>();
		pageBean = Invest.queryAllBids(currPage, pageSize, apr, amount,
				loanSchedule, startDate, endDate, loanType, minLevel, maxLevel,
				orderType, keywords, error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		render(list, totalCount, creditLevels, products, pageBean);
	}

	/**
	 * 前台投资首页借款标分页
	 * 
	 * @param pageNum
	 */
	public static void homeBids(int pageNum, int pageSize, String apr,
			String amount, String loanSchedule, String startDate,
			String endDate, String loanType, String minLevel, String maxLevel,
			String orderType, String keywords) {

		ErrorInfo error = new ErrorInfo();
		int currPage = pageNum;

		if (params.get("currPage") != null) {
			currPage = Integer.parseInt(params.get("currPage"));
		}

		PageBean<v_front_all_bids> pageBean = new PageBean<v_front_all_bids>();
		pageBean = Invest.queryAllBids(currPage, pageSize, apr, amount,
				loanSchedule, startDate, endDate, loanType, minLevel, maxLevel,
				orderType, keywords, error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}
		render(pageBean);
	}

	/**
	 * 用户查看自己所有的收藏标
	 */
	public static void queryUserCollectBids(int pageNum, int pageSize) {

		ErrorInfo error = new ErrorInfo();
		int currPage = pageNum;

		if (params.get("currPage") != null) {
			currPage = Integer.parseInt(params.get("currPage"));
		}
		User user = User.currUser();
		PageBean<v_front_user_attention_bids> pageBean = Invest
				.queryAllCollectBids(user.id, currPage, pageSize, error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}
		render(pageBean);

	}

	/**
	 * 向借款人提问
	 * 
	 * @param bidId
	 */
	public static void questionToBorrower(String toUserIdSign,
			String bidIdSign, String content, String code, String inputCode) {

		ErrorInfo error = new ErrorInfo();
		User user = User.currUser();
		JSONObject json = new JSONObject();

		long bidId = Security.checkSign(bidIdSign, Constants.BID_ID_SIGN,
				Constants.VALID_TIME, error);

		if (error.code < 0) {
			error.msg = "对不起！非法请求！";
			json.put("error", error);
			renderJSON(json);
		}

		long toUserId = Security.checkSign(toUserIdSign,
				Constants.USER_ID_SIGN, Constants.VALID_TIME, error);

		if (error.code < 0) {
			error.msg = "对不起！非法请求！";
			json.put("error", error);
			renderJSON(json);
		}

		BidQuestions question = new BidQuestions();
		question.bidId = bidId;
		question.userId = user.id;
		question.time = new Date();
		question.content = content;
		question.questionedUserId = toUserId;
		String codes = (String) Cache.get(code);

		if (!codes.equalsIgnoreCase(inputCode)) {
			error.msg = "对不起！验证码错误！";
			json.put("error", error);
			renderJSON(json);
		}

		question.addQuestion(user.id, error);

		if (error.code < 0) {
			json.put("content", content);
		}
		json.put("error", error);
		renderJSON(json);
	}

	/**
	 * 进入投标页面
	 * 
	 * @param bidId
	 * @param investAmount
	 */
	public static void invest(long bidId, double result) {

		ErrorInfo error = new ErrorInfo();
		Bid bid = new Bid();
		bid.id = bidId;

		/* 进入详情页面增加浏览次数 */
		Invest.updateReadCount(bidId, error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		Map<String, String> historySituationMap = User.historySituation(
				bid.userId, error);// 借款者历史记录情况
		List<UserAuditItem> uItems = UserAuditItem.queryUserAllAuditItem(
				bid.userId, bid.mark); // 用户正对产品上传的资料集合

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		User user = User.currUser();

		boolean ipsEnable = Constants.IPS_ENABLE;
	
		render(bid, result, historySituationMap, uItems, user, ipsEnable);
	}

	/**
	 * 投标记录分页ajax方法
	 * 
	 * @param pageNum
	 * @param pageSize
	 * @param bidId
	 */
	public static void viewBidInvestRecords(int pageNum, int pageSize,
			String bidIdSign) {

		ErrorInfo error = new ErrorInfo();
		int currPage = pageNum;

		if (params.get("currPage") != null) {
			currPage = Integer.parseInt(params.get("currPage"));
		}

		long bidId = Security.checkSign(bidIdSign, Constants.BID_ID_SIGN,
				Constants.VALID_TIME, error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		PageBean<v_invest_records> pageBean = new PageBean<v_invest_records>();
		pageBean = Invest.queryBidInvestRecords(currPage, pageSize, bidId,
				error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}
		render(pageBean);

	}

	/**
	 * 查询借款标的所有提问记录ajax分页方法
	 * 
	 * @param pageNum
	 * @param pageSize
	 * @param bidId
	 */
	public static void viewBidAllQuestion(int pageNum, int pageSize,
			String bidIdSign) {

		ErrorInfo error = new ErrorInfo();

		long bidId = Security.checkSign(bidIdSign, Constants.BID_ID_SIGN,
				Constants.VALID_TIME, error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		PageBean<BidQuestions> page = BidQuestions.queryQuestion(pageNum,
				pageSize, bidId, Constants.SEARCH_ALL, -1, error);

		if (null == page) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		render(page);
	}

	/**
	 * 确认投标
	 * 
	 * @param bidId
	 */
	public static void confirmInvest(long bidId) {

		if (Constants.IPS_ENABLE
				&& (User.currUser().getIpsStatus() != IpsCheckStatus.IPS)) {
			CheckAction.approve();
		}

		String investAmountStr = params.get("investAmount");

		String dealpwd = params.get("dealpwd");

		User user = User.currUser();
		ErrorInfo error = new ErrorInfo();
		if (StringUtils.isBlank(investAmountStr)) {
			error.msg = "投标金额不能为空！";
			flash.error(error.msg);
			invest(bidId, -1);
		}

		if (StringUtils.isBlank(dealpwd)) {
			error.msg = "支付密码不能为空！";
			flash.error(error.msg);
			invest(bidId, -1);
		}

		boolean b = investAmountStr.matches("^[1-9][0-9]*$");
		if (!b) {
			error.msg = "对不起！只能输入正整数!";
			flash.error(error.msg);
			invest(bidId, -1);
		}

		int investAmount = Integer.parseInt(investAmountStr);
		Invest.invest(user.id, bidId, investAmount, dealpwd, false, false,
				null, error);

		if (Constants.IPS_ENABLE) {
			if (error.code < 0) {
				flash.error(error.msg);
				invest(bidId, -1);
			}

			String pMerBillNo = Payment.createBillNo(user.id,
					IPSOperation.REGISTER_CREDITOR);

			Map<String, Object> map = new HashMap<String, Object>();
			map.put("userId", user.id);
			map.put("bidId", bidId);
			map.put("investAmount", investAmount);
			Cache.set(pMerBillNo, map, IPSConstants.CACHE_TIME);

			Map<String, String> args = Payment.registerCreditor(pMerBillNo,
					user.id, bidId, 1, investAmount, error);

			render("@front.account.PaymentAction.registerCreditor", args);
		}		
		if (error.code > 0) {
			invest(bidId, investAmount);
		} else {
			flash.error(error.msg);
			invest(bidId, -1);
		}

	}

	/**
	 * 确认投标(页面底部投标按钮)
	 * 
	 * @param bidId
	 */
	public static void confirmInvestBottom(long bidId) {
		if (Constants.IPS_ENABLE
				&& (User.currUser().getIpsStatus() != IpsCheckStatus.IPS)) {
			CheckAction.approve();
		}

		String investAmountStr = params.get("investAmountBottom");
		String dealpwd = params.get("dealpwdBottom");
		User user = User.currUser();
		ErrorInfo error = new ErrorInfo();

		if (StringUtils.isBlank(investAmountStr)) {
			error.msg = "投标金额不能为空！";
			flash.error(error.msg);
			invest(bidId, -1);
		}

		if (StringUtils.isBlank(dealpwd)) {
			error.msg = "支付密码不能为空！";
			flash.error(error.msg);
			invest(bidId, -1);
		}

		boolean b = investAmountStr.matches("^[1-9][0-9]*$");
		if (!b) {
			error.msg = "对不起！只能输入正整数!";
			flash.error(error.msg);
			invest(bidId, -1);
		}

		int investAmount = Integer.parseInt(investAmountStr);
		Invest.invest(user.id, bidId, investAmount, dealpwd, false, false,
				null, error);

		if (Constants.IPS_ENABLE) {
			if (error.code < 0) {
				flash.error(error.msg);
				invest(bidId, -1);
			}

			String pMerBillNo = Payment.createBillNo(user.id,
					IPSOperation.REGISTER_CREDITOR);

			Map<String, Object> map = new HashMap<String, Object>();
			map.put("userId", user.id);
			map.put("bidId", bidId);
			map.put("investAmount", investAmount);
			Cache.set(pMerBillNo, map, IPSConstants.CACHE_TIME);

			Map<String, String> args = Payment.registerCreditor(pMerBillNo,
					user.id, bidId, 1, investAmount, error);

			render("@front.account.PaymentAction.registerCreditor", args);
		}

		if (error.code > 0) {
			invest(bidId, investAmount);
		} else {
			flash.error(error.msg);
			invest(bidId, -1);
		}

	}

	/**
	 * 收藏借款标
	 * 
	 * @param bidId
	 */
	public static void collectBid(long bidId) {

		ErrorInfo error = new ErrorInfo();
		User user = User.currUser();

		Bid.collectBid(user.id, bidId, error);

		JSONObject json = new JSONObject();
		json.put("error", error);
		renderJSON(json);
	}

	/**
	 * 查看用户当前的登录状态(异步)
	 */
	public static void checkUserStatus() {
		User user = User.currUser();

		JSONObject json = new JSONObject();

		/* 是否登录 */
		if (null == user) {
			json.put("status", Constants.NOT_LOGIN);

			renderJSON(json);
		}

		/* 是否激活 */
		if (!user.isEmailVerified) {
			json.put("userName", user.name);
			json.put("email", user.email);
			json.put("status", Constants.NOT_EMAILVERIFIED);

			renderJSON(json);
		}

		/* 是否完善基本资料 */
		if (!user.isAddBaseInfo) {
			json.put("status", Constants.NOT_ADDBASEINFO);

			renderJSON(json);
		}

		json.put("status", Constants.SUCCESS_STATUS);
		renderJSON(json);
	}

	/**
	 * 查看(异步)
	 */
	public static void showitem(String mark, String signUserId) {
		/* 解密userId */
		ErrorInfo error = new ErrorInfo();
		long userId = Security.checkSign(signUserId, Constants.USER_ID_SIGN,
				Constants.VALID_TIME, error);

		if (userId < 1) {
			renderText(error.msg);
		}

		UserAuditItem item = new UserAuditItem();
		item.lazy = true;
		item.userId = userId;
		item.mark = mark;

		render(item);
	}

	/**
	 * 取消关注借款标
	 * 
	 * @param attentionId
	 */
	public static void cancleBidAttention(long attentionId) {

		ErrorInfo error = new ErrorInfo();
		Invest.canaleBid(attentionId, error);

		JSONObject json = new JSONObject();

		json.put("error", error);
		renderJSON(json);
	}
}
