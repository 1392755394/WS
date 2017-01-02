package controllers.front.debt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import net.sf.json.JSONObject;
import constants.Constants;
import constants.IPSConstants.IpsCheckStatus;
import controllers.BaseController;
import controllers.front.account.CheckAction;
import controllers.front.account.InvestAccount;
import models.t_invests;
import models.v_debt_auction_records;
import models.v_front_all_debts;
import business.CreditLevel;
import business.Debt;
import business.Invest;
import business.Product;
import business.User;
import business.UserAuditItem;
import utils.ErrorInfo;
import utils.NumberUtil;
import utils.PageBean;
import utils.Security;

/**
 * 
 * @author liuwenhui
 * 
 */
public class DebtAction extends BaseController {
	/* 前台债权转让首页 */
	public static void debtHome() {
		
		ErrorInfo error = new ErrorInfo();
		Long totle = Debt.getAllDebtCount(error);
		
		if(error.code < 0){
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}
		
		List<Product> products = Product.queryProductNames(true, error);
		List<CreditLevel> creditLevels = CreditLevel.queryAllCreditLevels(error);
		
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;
		
		String currPageStr = params.get("currPage");
		String pageSizeStr = params.get("pageSize");
		
		if(NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
 		}
		
 		if(NumberUtil.isNumericInt(pageSizeStr)) {
 			pageSize = Integer.parseInt(pageSizeStr);
 		}
		
 		String apr = params.get("apr");
 		String loanType = params.get("loanType");
 		String debtAmount = params.get("debtAmount");
 		String orderType = params.get("orderType");
 		String keywords = params.get("keywords");
 		
 		 PageBean<v_front_all_debts>  page= Debt.queryAllDebtTransfers( currPage,pageSize,loanType, debtAmount, apr, orderType,keywords,error);
         
         if(error.code < 0){
         	render(Constants.ERROR_PAGE_PATH_FRONT);
         }


		render(totle,products, creditLevels,page);
	}

	
	
	/**
	 * 前台债权列表分页
	 * @param pageNum
	 */
	public static void debtHomeDebtList(int pageNum,int pageSize,String loanType,String debtAmount,String apr,String orderType,String keywords){
		
		ErrorInfo error = new ErrorInfo();
        int currPage = pageNum;
		
		if(params.get("currPage")!=null) {
			currPage = Integer.parseInt(params.get("currPage"));
		}
		
        PageBean<v_front_all_debts>  page= Debt.queryAllDebtTransfers( currPage,pageSize,loanType, debtAmount, apr, orderType,keywords,error);
         
        if(error.code < 0){
        	render(Constants.ERROR_PAGE_PATH_FRONT);
        }
         render(page);
		
	}
	
	
	/* 前台债权转让详情页面 */
	public static void debtTransferDetails() {
		render();
	}

	/**
	 * 前台债权详情页面
	 * @param debtId
	 * @param success
	 */
	public static void debtDetails(long debtId,  int success,String descrption) {
		
		ErrorInfo error = new ErrorInfo();
		Debt debt = new Debt();
		debt.id = debtId;
		
		Long bidUserId = Debt.getBidUserId(debtId, error);
		
		if(error.code < 0){
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}
		
		Map<String,String> historySituationMap = User.historySituation(bidUserId,error);//借款者历史记录情况
		
		if(error.code < 0){
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}
		
		Long investUserId = Debt.getInvestUserId(debtId, error);
		
		if(error.code < 0){
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}
		
		Map<String,String> debtUserhistorySituationMap = new HashMap<String, String>();
		
		debtUserhistorySituationMap = User.debtUserhistorySituation(investUserId,error);//债权者历史记录情况
		
		if(error.code < 0){
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}
		
		List<UserAuditItem> uItems = UserAuditItem.queryUserAllAuditItem(bidUserId, debt.invest.bid.mark); // 用户正对产品上传的资料集合
		
		render(debt,historySituationMap,debtUserhistorySituationMap, success,descrption,uItems);
	}
	
	
	
	
	
	/**
	 * 确认转让债权
	 * @param investId
	 */
	public static void confirmTransfer(long investId){
		
		ErrorInfo error = new ErrorInfo();
		
		String specifiedUserName = "";
		
		t_invests invest = Invest.queryUserAndBid(investId);
		
		if(null == invest){
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}
		
		String transferTitle = params.get("transferTitle");
		String period = params.get("period");
		String transferReason = params.get("transferReason");
		String price = params.get("transferPrice");
		String transerType = params.get("type");
		
		User user = User.currUser();
		
		double debtAmount = Debt.getDebtAmount(investId,error);
		
		boolean b = price.matches("^[1-9][0-9]*$");
    	if(!b){
    		error.msg = "对不起！转让底价只能输入正整数!";
			error.code = -10;
			InvestAccount.repayingInvestBid(error.code,error.msg);
    	} 
    	
    	if(StringUtils.isBlank(transferTitle) || StringUtils.isBlank(period) || StringUtils.isBlank(transferReason) || StringUtils.isBlank(price) ||
    			StringUtils.isBlank(transerType)){
    		error.msg = "对不起！请正确设置各种参数!";
			error.code = -10;
			InvestAccount.repayingInvestBid(error.code,error.msg);
    	}
    	
    	int periods = Integer.parseInt(period);
		int type = Integer.parseInt(transerType);
		double transferPrice = Double.parseDouble(price);
		
		if(type == Constants.DIRECTIONAL_MODE){//定向转让
			 specifiedUserName = params.get("specifiedUserName");
		}
		
		Debt.transferDebt(user.id,investId, transferTitle, transferReason, periods, debtAmount, transferPrice, type, specifiedUserName, error);
		
		InvestAccount.repayingInvestBid(error.code,error.msg);
		
		
	}

	
	
	
	/**
	 * 竞拍债权
	 * @param debtId
	 */
	public static void auction(long debtId) {
		
		User user = User.currUser();
		
		if(Constants.IPS_ENABLE && (User.currUser().getIpsStatus() != IpsCheckStatus.IPS)){
			CheckAction.approve();
		}

		String offerPriceStr =  params.get("offerPrice");
		ErrorInfo error = new ErrorInfo();
		
		if(StringUtils.isBlank(offerPriceStr)){
			error.msg = "对不起！竞拍价格不能为空！";
			error.code = -1;
			debtDetails(debtId,  error.code,error.msg);
		}
		
		boolean b=offerPriceStr.matches("^[1-9][0-9]*$");
    	if(!b){
    		error.msg = "对不起！竞拍价格只能是正整数！";
			error.code = -1;
			debtDetails(debtId,  error.code,error.msg);
    	} 
		
		int offerPrice = Integer.parseInt(offerPriceStr);
		Debt.auctionDebt(user.id, offerPrice, debtId, error);
		
		debtDetails(debtId,  error.code,error.msg);
		
	}
	
	/**
	 * 收藏债权
	 * @param bidId
	 */
	public static void collectDebt(long debtId){
		
		ErrorInfo error = new ErrorInfo();
		User user = User.currUser();
		
		Debt.collectDebt(user.id, debtId, error);
		
		JSONObject json = new JSONObject();
		json.put("error", error);
		renderJSON(json);
	}
	
	/**
	 * ajax分页查询债权竞拍记录
	 * @param debtId
	 */
	public static void viewDebtAllAuctionRecords(int pageNum, int pageSize,long debtId){
		
		ErrorInfo error = new ErrorInfo();
		int currPage = pageNum;
		
		if (params.get("currPage") != null) {
			currPage = Integer.parseInt(params.get("currPage"));
		}
		PageBean<v_debt_auction_records> page = Debt.queryDebtAllAuctionRecords( currPage,  pageSize, debtId,error);
		
		if(error.code < 0){
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}
		
		
		render(page);
		
	}
	
	
	/**
	 * 举报用户
	 * @param userName
	 * @param reason
	 * @param bidId
	 * @param investTransferId
	 */
	public static void reportUser(String userIdSign, String reason, String bidIdSign, long investTransferId ){
		
		User user = User.currUser();
		ErrorInfo error = new ErrorInfo();
		JSONObject json = new JSONObject();
		long bidId = 0;
		
		if(!StringUtils.isBlank(bidIdSign)){
			
			 bidId = Security.checkSign(bidIdSign, Constants.BID_ID_SIGN, Constants.VALID_TIME, error);
			
			if(error.code < 0){
				json.put("msg", error.msg); 
				renderJSON(json);
			}
		}
		
		
		long userId = Security.checkSign(userIdSign, Constants.USER_ID_SIGN, Constants.VALID_TIME, error);
		
		if(error.code < 0){
			json.put("msg", error.msg); 
			renderJSON(json);
		}
		
		if(userId == user.id){
			error.msg = "对不起！您不能举报自己";
			json.put("msg", error.msg); 
			renderJSON(json);
		}
		
		String userName = User.queryUserNameById( userId,error);
		user.addReportAUser(userName, reason, bidId, investTransferId, error);
		
		
		json.put("msg", error.msg);
		
		renderJSON(json);
	}
	
	
	public static void judgeUserNameExist(String userName){
		
		ErrorInfo error = new ErrorInfo();
		int result = User.isNameExist(userName, error);
		
		JSONObject json = new JSONObject();
		json.put("result", result);
		
		renderJSON(json);
	}
	
	/**
	 * 查看资料图片
	 * @param itemId
	 */
	public static void showitem(long itemId,String signUserId){
		ErrorInfo error = new ErrorInfo();
		long userId = Security.checkSign(signUserId, Constants.USER_ID_SIGN, Constants.VALID_TIME, error);
		
		if(error.code < 0){
			renderJSON(error);
		}
		
		UserAuditItem item = Invest.getAuditItem(itemId, userId);
		
		render(item);
	}
	
}
