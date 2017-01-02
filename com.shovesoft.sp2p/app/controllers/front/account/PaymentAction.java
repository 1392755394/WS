package controllers.front.account;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.cache.Cache;
import utils.ErrorInfo;

import com.shove.Convert;
import com.shove.security.Encrypt;

import business.Bid;
import business.Payment;
import business.User;
import constants.Constants;
import constants.IPSConstants;
import constants.OptionKeys;
import controllers.BaseController;
import controllers.front.bid.BidAction;
import controllers.front.invest.InvestAction;
import controllers.supervisor.financeManager.LoanManager;

/**
 * 资金托管
 * @author lzp
 * @version 6.0
 * @created 2014-9-16
 */
public class PaymentAction extends BaseController {
	/**
	 * 开户
	 */
	public static void createAcct() {
		Map<String, String> args = Payment.createAcct();

		render(args);
	}
	
	/**
	 * 开户回调
	 */
	public static void createAcctCB(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		ErrorInfo error = new ErrorInfo();
		
		Payment pay = new Payment();
		pay.pMerCode = pMerCode;
		pay.pErrCode = pErrCode;
		pay.pErrMsg = pErrMsg;
		pay.p3DesXmlPara = p3DesXmlPara;
		pay.pSign = pSign;
		pay.createAcctCB(error);
		
		flash.error(error.msg);
		
		CheckAction.approve();
	}
	
	/**
	 * 标的登记
	 */
	public static void registerSubject() {
		renderText("");
	}
	
	/**
	 * 标的登记回调
	 */
	public static void registerSubjectCB(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		if (IPSConstants.IS_REPAIR_TEST) {
			flash.error("模拟发标掉单");
			
			AccountHome.home();
		}
		
		ErrorInfo error = new ErrorInfo();
		
		Payment pay = new Payment();
		pay.pMerCode = pMerCode;
		pay.pErrCode = pErrCode;
		pay.pErrMsg = pErrMsg;
		pay.p3DesXmlPara = p3DesXmlPara;
		pay.pSign = pSign;
		pay.registerSubjectCB(error);
		
		flash.put("msg", error.msg);
		String operation = pay.jsonPara.getString("pMemo3");
		String bidNo = pay.jsonPara.getString("pBidNo");
		Bid bid = (Bid) Cache.get("bid_"+operation+"_"+bidNo);
		
		if(bid.id > 0){
			flash.put("no", OptionKeys.getvalue(OptionKeys.LOAN_NUMBER, error) + bid.id);
			flash.put("title", bid.title);
			flash.put("amount", bid.amount);
			flash.put("status", bid.status);
		}
		
		Cache.delete("bid_"+operation+"_"+bidNo);
		
		BidAction.applyNow(bid.productId, error.code==0?1:error.code, 1);
	}
	
	/**
	 * 登记债权人
	 */
	public static void registerCreditor() {
		renderText("登记债权人");
	}
	
	/**
	 * 登记债权人回调
	 */
	public static void registerCreditorCB(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		if (IPSConstants.IS_REPAIR_TEST) {
			flash.error("模拟投标掉单");
			
			AccountHome.home();
		}
		
		ErrorInfo error = new ErrorInfo();
		
		Payment pay = new Payment();
		pay.pMerCode = pMerCode;
		pay.pErrCode = pErrCode;
		pay.pErrMsg = pErrMsg;
		pay.p3DesXmlPara = p3DesXmlPara;
		pay.pSign = pSign;
		pay.registerCreditorCB(error);
		
		String pMerBillNo = pay.jsonPara.getString("pMerBillNo");
		Map<String, Object> map = (Map<String, Object>) Cache.get(pMerBillNo);
		long bidId = Convert.strToLong(map.get("bidId") + "", -1);
		int investAmount = Convert.strToInt(map.get("investAmount") + "", -1);
		flash.error(error.msg);
		InvestAction.invest(bidId, investAmount);
	}
	
	/**
	 * 登记债权转让
	 */
	public static void registerCretansfer() {
		renderText("");
	}
	
	/**
	 * 登记债权转让回调
	 */
	public static void registerCretansferCB(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		if (IPSConstants.IS_REPAIR_TEST) {
			flash.error("模拟债权转让掉单");
			
			AccountHome.home();
		}
		
		ErrorInfo error = new ErrorInfo();
		
		Payment pay = new Payment();
		pay.pMerCode = pMerCode;
		pay.pErrCode = pErrCode;
		pay.pErrMsg = pErrMsg;
		pay.p3DesXmlPara = p3DesXmlPara;
		pay.pSign = pSign;
		pay.registerCretansferCB(error);
		
		InvestAccount.myDebts(error.code, error.msg);
	}
	
	/**
	 * 自动投标签约
	 */
	public static void autoNewSigning() {
		renderText("");
	}
	
	/**
	 * 自动投标签约回调
	 */
	public static void autoNewSigningCB(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		ErrorInfo error = new ErrorInfo();
		
		Payment pay = new Payment();
		pay.pMerCode = pMerCode;
		pay.pErrCode = pErrCode;
		pay.pErrMsg = pErrMsg;
		pay.p3DesXmlPara = p3DesXmlPara;
		pay.pSign = pSign;
		pay.autoNewSigningCB(error);
		
		if (StringUtils.isNotBlank(p3DesXmlPara)) {
			p3DesXmlPara = Encrypt.decrypt3DES(p3DesXmlPara, Constants.ENCRYPTION_KEY);
		}
		
		Logger.info("pErrMsg:\n"+pErrMsg+"\np3DesXmlPara:\n"+p3DesXmlPara+"\nerror.msg:\n"+error.msg);
		
		if (error.code < 0) {
			InvestAccount.auditmaticInvest(error.code, error.msg);
		}
		
		InvestAccount.auditmaticInvest(1, "开启投标机器人成功");
	}
	
	/**
	 * 自动还款签约
	 */
	public static void repaymentSigning() {
		Map<String, String> args = Payment.repaymentSigning();

		render(args);
	}
	
	/**
	 * 自动还款签约回调
	 */
	public static void repaymentSigningCB(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		ErrorInfo error = new ErrorInfo();
		
		Payment pay = new Payment();
		pay.pMerCode = pMerCode;
		pay.pErrCode = pErrCode;
		pay.pErrMsg = pErrMsg;
		pay.p3DesXmlPara = p3DesXmlPara;
		pay.pSign = pSign;
		pay.repaymentSigningCB(error);
		
		if (StringUtils.isNotBlank(p3DesXmlPara)) {
			p3DesXmlPara = Encrypt.decrypt3DES(p3DesXmlPara, Constants.ENCRYPTION_KEY);
		}
		
		renderText("pErrMsg:\n"+pErrMsg+"\np3DesXmlPara:\n"+p3DesXmlPara+"\nerror.msg:\n"+error.msg);
	}
	
	/**
	 * 充值
	 */
	public static void doDpTrade() {
		renderText("");
	}
	
	/**
	 * 充值回调
	 */
	public static void doDpTradeCB(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		if (IPSConstants.IS_REPAIR_TEST) {
			flash.error("模拟充值掉单");
			
			AccountHome.home();
		}
		
		ErrorInfo error = new ErrorInfo();
		
		Payment pay = new Payment();
		pay.pMerCode = pMerCode;
		pay.pErrCode = pErrCode;
		pay.pErrMsg = pErrMsg;
		pay.p3DesXmlPara = p3DesXmlPara;
		pay.pSign = pSign;
		pay.doDpTradeCB(error);
		
		if (error.code < 0) {
			flash.error(error.msg);
		} else {
			flash.error("充值成功");
		}
		
		FundsManage.recharge();
	}
	
	/**
	 * 转账
	 */
	public static void transfer() {
		renderText("");
	}
		
	/**
	 * 转账回调
	 */
	public static void transferCB(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		ErrorInfo error = new ErrorInfo();
		
		Payment pay = new Payment();
		pay.pMerCode = pMerCode;
		pay.pErrCode = pErrCode;
		pay.pErrMsg = pErrMsg;
		pay.p3DesXmlPara = p3DesXmlPara;
		pay.pSign = pSign;
		pay.transferCB(error);
		
		flash.error(error.msg);
		
		LoanManager.readyReleaseList();
	}
	
	/**
	 * 还款
	 */
	public static void repaymentNewTrade() {
		renderText("");
	}
	
	/**
	 * 还款回调
	 */
	public static void repaymentNewTradeCB(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		if (IPSConstants.IS_REPAIR_TEST) {
			flash.error("模拟还款掉单");
			
			AccountHome.home();
		}
		
		ErrorInfo error = new ErrorInfo();
		
		Payment pay = new Payment();
		pay.pMerCode = pMerCode;
		pay.pErrCode = pErrCode;
		pay.pErrMsg = pErrMsg;
		pay.p3DesXmlPara = p3DesXmlPara;
		pay.pSign = pSign;
		pay.repaymentNewTradeCB(error);
		
		flash.error(error.msg);
		
		AccountHome.myLoanBills(0, 0, 0, null, 0);
	}
	
	/**
	 * 解冻保证金
	 */
	public static void guaranteeUnfreeze() {
		renderText("");
	}

	/**
	 * 解冻保证金回调
	 */
	public static void guaranteeUnfreezeCB() {
		
	}
	
	/**
	 * 提现
	 */
	public static void doDwTrade() {
		renderText("");
	}
	
	/**
	 * 提现回调
	 */
	public static void doDwTradeCB(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		if (IPSConstants.IS_REPAIR_TEST) {
			flash.error("模拟提现掉单");
			
			AccountHome.home();
		}
		
		ErrorInfo error = new ErrorInfo();
		
		Payment pay = new Payment();
		pay.pMerCode = pMerCode;
		pay.pErrCode = pErrCode;
		pay.pErrMsg = pErrMsg;
		pay.p3DesXmlPara = p3DesXmlPara;
		pay.pSign = pSign;
		pay.doDwTradeCB(error);
		
		if (error.code < 0) {
			flash.error(error.msg);
		} else {
			flash.error("提现成功");
		}
		
		FundsManage.withdrawal();
	}
	
	/**
	 * 账户余额查询
	 */
	public static void queryForAccBalance() {
		ErrorInfo error = new ErrorInfo();
		renderText(Payment.queryForAccBalance(User.currUser().ipsAcctNo, error).toString());
	}
	
	/**
	 * 商户端获取银行列表查询
	 */
	public static void getBankList() {
		ErrorInfo error = new ErrorInfo();
		renderText(Payment.getBankList(error).toString());
	}
	
	/**
	 * 账户信息查询
	 */
	public static void queryMerUserInfo() {
		ErrorInfo error = new ErrorInfo();
		renderText(Payment.queryMerUserInfo(User.currUser().ipsAcctNo, error).toString());
	}
}
