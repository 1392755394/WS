package business;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.cache.Cache;
import play.db.jpa.GenericModel;
import play.libs.WS;
import models.t_bids;
import models.t_users;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import utils.Arith;
import utils.Converter;
import utils.DateUtil;
import utils.ErrorInfo;
import com.shove.Convert;
import com.shove.security.Encrypt;

import constants.Constants;
import constants.IPSConstants;
import constants.IPSConstants.IPSOperation;
import constants.IPSConstants.IPSS2SUrl;
import constants.IPSConstants.IPSWebUrl;
import constants.IPSConstants.Status;
import constants.IPSConstants.TransferType;

/**
 * 资金托管
 * @author lzp
 * @version 6.0
 * @created 2014-9-16
 */
public class Payment implements Serializable {
	public String pMerCode;
	public String pErrCode;
	public String pErrMsg;
	public String p3DesXmlPara;
	public JSONObject jsonPara;
	public String pSign;
	
	public JSONObject getJsonPara() {
		String xmlPara = Encrypt.decrypt3DES(this.p3DesXmlPara, Constants.ENCRYPTION_KEY);
		return (JSONObject)Converter.xmlToObj(xmlPara);
	}
	
	public boolean checkSign() {
		if(StringUtils.isBlank(pMerCode) || StringUtils.isBlank(pSign)) {
			return false;
		}
		
		if(StringUtils.isBlank(p3DesXmlPara)) {
			return pSign.equals(Encrypt.MD5(pMerCode+Constants.ENCRYPTION_KEY));
		}
		
		return pSign.equals(Encrypt.MD5(pMerCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
	}
	
	public static boolean checkSign(String src, String sign) {
		if (StringUtils.isBlank(src) || StringUtils.isBlank(sign)) {
			return false;
		}
		
		return sign.equals(Encrypt.MD5(src+Constants.ENCRYPTION_KEY));
	}
	
	/**
	 * 生成流水号(最长30位)
	 * @param userId (不能为负，系统行为：0)
	 * @param operation
	 * @return
	 */
	public static String createBillNo(long userId, int operation) {
		return userId + "x" + operation + "x" + new Date().getTime();
	}
	
	/**
	 * 开户
	 * @return
	 */
	public static Map<String, String> createAcct() {
		User user = User.currUser();
		
		String pMerBillNo = createBillNo(user.id, IPSOperation.CREATE_IPS_ACCT);
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", pMerBillNo);
		
		jsonObj.put("pIdentType", IPSConstants.INDENT_TYPE);
		jsonObj.put("pIdentNo", user.idNumber);
		jsonObj.put("pRealName", user.realityName);
		jsonObj.put("pMobileNo", user.mobile);
		jsonObj.put("pEmail", user.email);
		jsonObj.put("pSmDate", DateUtil.simple(new Date()));
		
		jsonObj.put("pWebUrl", IPSWebUrl.CREATE_IPS_ACCT);
		jsonObj.put("pS2SUrl", IPSS2SUrl.CREATE_IPS_ACCT);
		jsonObj.put("pMemo1", "pMemo1");
		jsonObj.put("pMemo2", "pMemo2");
		jsonObj.put("pMemo3", "pMemo3");

		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		System.out.println(strXml);
		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("action", IPSConstants.ACTION);
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("type", IPSOperation.CREATE_IPS_ACCT+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("memberId", user.id+"");
		map.put("memberName", user.name);
		map.put("argSign", argSign);

		return map;
	}
	
	/**
	 * 开户回调
	 * @param userId
	 * @param error
	 */
	public void createAcctCB(ErrorInfo error) {
		error.clear();
		
		if(!Payment.checkSign(this.pMerCode + this.pErrCode + this.pErrMsg + this.p3DesXmlPara, this.pSign)) {
			error.code = -1;
			error.msg = "sign校验失败";
			
			return;
		}
		
		if(!"10".equals(this.jsonPara.getString("pStatus"))) {
			error.code = -1;
			error.msg = this.pErrMsg;
			
			return;
		}
		
		String pIpsAcctNo = this.jsonPara.getString("pIpsAcctNo");
		
		User user = new User();
		user.ipsAcctNo = pIpsAcctNo;
		user.updateIpsAcctNo(this.jsonPara.getLong("pMemo1"), error);
	}
	
	/**
	 * 标的登记
	 * @return
	 */
	public static Map<String, String> registerSubject(String operation, Bid bid) {
		User user = User.currUser();
		
		String pMerBillNo = bid.merBillNo;
		
		JSONObject jsonObj = new JSONObject();
		
		String cycleType = IPSConstants.MONTH_CYCLE_TYPE;
		int cycleValue = bid.period;
		String paymentType = IPSConstants.PAID_MONTH_EQUAL_PRINCIPAL_INTEREST;
		
		if(bid.periodUnit == Constants.YEAR) {
			cycleValue *=  12;
		}
		
		if(bid.periodUnit == Constants.DAY) {
			cycleType = IPSConstants.DAY_CYCLE_TYPE;
		}
		
		if(bid.repayment.id == Constants.PAID_MONTH_ONCE_REPAYMENT) {
			paymentType = IPSConstants.PAID_MONTH_ONCE_REPAYMENT;
		}else {
			paymentType = IPSConstants.OTHER_REPAYMENT;
		}
		
		jsonObj.put("pMerBillNo", pMerBillNo);
		jsonObj.put("pBidNo", bid.bidNo);
		jsonObj.put("pRegDate", DateUtil.simple(new Date()));
		jsonObj.put("pLendAmt", String.format("%.2f", bid.amount));
		jsonObj.put("pGuaranteesAmt", String.format("%.2f", bid.bail));
		jsonObj.put("pTrdLendRate", String.format("%.2f", bid.apr));
		jsonObj.put("pTrdCycleType", cycleType);
		jsonObj.put("pTrdCycleValue", cycleValue);
		jsonObj.put("pLendPurpose", bid.purpose.id);
		jsonObj.put("pRepayMode", paymentType);
		jsonObj.put("pOperationType", operation.equals(IPSConstants.BID_CREATE) ? IPSConstants.OPERATION_TYPE_1 : IPSConstants.OPERATION_TYPE_2);
		jsonObj.put("pLendFee", String.format("%.2f", bid.serviceFees));
		
		jsonObj.put("pAcctType", IPSConstants.INDENT_TYPE);
		jsonObj.put("pIdentNo", user.idNumber);
		jsonObj.put("pRealName", user.realityName);
		jsonObj.put("pIpsAcctNo", user.ipsAcctNo);
		
		jsonObj.put("pWebUrl", IPSWebUrl.REGISTER_SUBJECT);
		jsonObj.put("pS2SUrl", IPSS2SUrl.REGISTER_SUBJECT);
		jsonObj.put("pMemo1", "pMemo1");
		jsonObj.put("pMemo2", "pMemo2");
		jsonObj.put("pMemo3", operation);

		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		System.out.println(strXml);
		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("action", IPSConstants.ACTION);
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("memberId", user.id+"");
		map.put("type", IPSOperation.REGISTER_SUBJECT+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("argSign", argSign);

		return map;
	}
	
	/**
	 * 流标
	 * @param operation
	 * @param bid
	 * @param userId
	 * @param error
	 */
	public static boolean flow(String operation, Bid bid, long userId, ErrorInfo error) {
		t_users user = User.queryUser2ByUserId(userId, error);
		
		String pMerBillNo = bid.merBillNo;
		
		JSONObject jsonObj = new JSONObject();
		
		String cycleType = IPSConstants.MONTH_CYCLE_TYPE;
		int cycleValue = bid.period;
		String paymentType = IPSConstants.PAID_MONTH_EQUAL_PRINCIPAL_INTEREST;
		
		if(bid.periodUnit == Constants.YEAR) {
			cycleValue *=  12;
		}
		
		if(bid.periodUnit == Constants.DAY) {
			cycleType = IPSConstants.DAY_CYCLE_TYPE;
		}
		
		if(bid.repayment.id == Constants.PAID_MONTH_ONCE_REPAYMENT) {
			paymentType = IPSConstants.PAID_MONTH_ONCE_REPAYMENT;
		}else {
			paymentType = IPSConstants.OTHER_REPAYMENT;
		}
		
		jsonObj.put("pMerBillNo", pMerBillNo);
		jsonObj.put("pBidNo", bid.bidNo);
		jsonObj.put("pRegDate", DateUtil.simple(new Date()));
		jsonObj.put("pLendAmt", String.format("%.2f", bid.amount));
		jsonObj.put("pGuaranteesAmt", String.format("%.2f", bid.bail));
		jsonObj.put("pTrdLendRate", String.format("%.2f", bid.apr));
		jsonObj.put("pTrdCycleType", cycleType);
		jsonObj.put("pTrdCycleValue", cycleValue);
		jsonObj.put("pLendPurpose", bid.purpose.id);
		jsonObj.put("pRepayMode", paymentType);
		jsonObj.put("pOperationType", operation.equals(IPSConstants.BID_CREATE) ? IPSConstants.OPERATION_TYPE_1 : IPSConstants.OPERATION_TYPE_2);
		jsonObj.put("pLendFee", String.format("%.2f", bid.serviceFees));
		
		jsonObj.put("pAcctType", IPSConstants.INDENT_TYPE);
		jsonObj.put("pIdentNo", user.id_number);
		jsonObj.put("pRealName", user.reality_name);
		jsonObj.put("pIpsAcctNo", user.ips_acct_no);
		
		jsonObj.put("pWebUrl", IPSWebUrl.REGISTER_SUBJECT);
		jsonObj.put("pS2SUrl", IPSS2SUrl.REGISTER_SUBJECT);
		jsonObj.put("pMemo1", "pMemo1");
		jsonObj.put("pMemo2", "pMemo2");
		jsonObj.put("pMemo3", operation);

		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		System.out.println(strXml);
		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r\n", "");
		
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("action", IPSConstants.ACTION);
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("memberId", user.id+"");
		map.put("type", IPSOperation.REGISTER_SUBJECT+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("flow", IPSConstants.BID_FLOW);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("argSign", argSign);
		
		
		String strJson = WS.url(IPSConstants.ACTION).setParameters(map).get().getString();
		
		Logger.info(strJson);
		
		JSONObject result = JSONObject.fromObject(strJson);
		Logger.info("--------------流标校验开始---------------------");
		Logger.info(result.get("p3DesXmlPara").toString());
		if(!Payment.checkSign(result.get("pMerCode").toString() + result.get("pErrCode") + result.get("pErrMsg") + result.get("p3DesXmlPara"), result.get("pSign").toString())) {
			error.code = -1;
			error.msg = "sign校验失败";
			Logger.info("--------------流标校验失败---------------------");
			return false;
		}
		Logger.info("--------------流标校验成功---------------------");
		if(!("MG02503F".equals(result.get("pErrCode")) || "MG02505F".equals(result.get("pErrCode")))) {
			return false;
		}
		
		return true;
		
	}
	
	/**
	 * 标的登记回调
	 * @param userId
	 * @param error
	 */
	public void registerSubjectCB(ErrorInfo error) {
		error.clear();
		
		if(!Payment.checkSign(this.pMerCode + this.pErrCode + this.pErrMsg + this.p3DesXmlPara, this.pSign)) {
			error.code = -1;
			error.msg = "sign校验失败";
			
			return;
		}
		
		String operationType = this.jsonPara.getString("pOperationType");
		
		String operation = this.jsonPara.getString("pMemo3");
		String bidNo = this.jsonPara.getString("pBidNo");
		Bid bid = (Bid) Cache.get("bid_"+operation+"_"+bidNo);
		
		if(IPSConstants.BID_CREATE.equals(operation)) {
			if(!("1".equals(operationType) && (pErrCode.equals("MG02500F") || pErrCode.equals("MG00000F")))) {
				error.code = -1;
				error.msg = this.pErrMsg;
				
				return;
			}
			
			t_bids tbid = null;
			
			try {
				tbid = GenericModel.findById(bid.id);
			} catch (Exception e) {
				Logger.error(e.getMessage());
				error.code = -1;
				error.msg = "数据库异常";
				
				return;
			}
			
			bid.afterCreateBid(tbid, bidNo, error);
			
			if (error.code < 0) {
				return;
			}
			
			IpsDetail.updateStatus(tbid.mer_bill_no, Status.SUCCESS, error);
		}else if(IPSConstants.BID_CANCEL.equals(operation)) {
			if(this.pErrCode.equals("MG02503F") || this.pErrCode.equals("MG02505F")) {
				bid.auditToRepealBC(error);
			}
		}else if(IPSConstants.BID_CANCEL_B.equals(operation)) {
			if(this.pErrCode.equals("MG02503F") || this.pErrCode.equals("MG02505F")) {
				bid.advanceLoanToPeviewNotThroughBC(error);
			}
		}else if(IPSConstants.BID_CANCEL_S.equals(operation)) {
			if(this.pErrCode.equals("MG02503F") || this.pErrCode.equals("MG02505F")) {
				bid.auditToNotThroughBC(error);
			}
		}else if(IPSConstants.BID_CANCEL_I.equals(operation)) {
			if(this.pErrCode.equals("MG02503F") || this.pErrCode.equals("MG02505F")) {
				bid.fundraiseToPeviewNotThroughBC(error);
			}
		}else if(IPSConstants.BID_CANCEL_M.equals(operation)) {
			if(this.pErrCode.equals("MG02503F") || this.pErrCode.equals("MG02505F")) {
				bid.fundraiseToLoanNotThroughBC(error);
			}
		}else if(IPSConstants.BID_CANCEL_F.equals(operation)) {
			if(this.pErrCode.equals("MG02503F") || this.pErrCode.equals("MG02505F")) {
				bid.advanceLoanToRepealBC(error);
			}
		}else if(IPSConstants.BID_CANCEL_N.equals(operation)) {
			if(this.pErrCode.equals("MG02503F") || this.pErrCode.equals("MG02505F")) {
				bid.fundraiseToRepealBC(error);
			}
		}else if(IPSConstants.BID_ADVANCE_LOAN.equals(operation)) {
			if(this.pErrCode.equals("MG02503F") || this.pErrCode.equals("MG02505F")) {
				bid.advanceLoanToFlowBC(error);
			}
		}else if(IPSConstants.BID_FUNDRAISE.equals(operation)) {
			if(this.pErrCode.equals("MG02503F") || this.pErrCode.equals("MG02505F")) {
				bid.fundraiseToFlowBC(error);
			}
		}
	}
	
	/**
	 * 登记债权人接口
	 * @return
	 */
	public static Map<String, String> registerCreditor(String pMerBillNo, long userId, long bidId, int pRegType, double pTrdAmt, ErrorInfo error) {
		error.clear();
		
		JSONObject memo = new JSONObject();
		memo.put("userId", userId);
		memo.put("bidId", bidId);
		memo.put("pTrdAmt", pTrdAmt);
		
		IpsDetail detail = new IpsDetail();
		detail.merBillNo = pMerBillNo;
		detail.userName = User.queryUserNameById(userId, error);
		detail.time = new Date();
		detail.type = IPSOperation.REGISTER_CREDITOR;
		detail.status = Status.FAIL;
		detail.memo = memo.toString();
		detail.create(error);
		
		if (error.code < 0) {
			return null;
		}
		
		User user = new User();
		user.id = userId;
		
		Bid bid = new Bid();
		bid.ips = true;
		bid.id = bidId;
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", pMerBillNo);
		jsonObj.put("pMerDate", DateUtil.simple(new Date()));
		jsonObj.put("pBidNo", bid.bidNo);
		jsonObj.put("pContractNo", "pContractNo");
		jsonObj.put("pRegType", pRegType);
		jsonObj.put("pAuthNo", pRegType==1 ? "" : user.ipsBidAuthNo);
		jsonObj.put("pAuthAmt", String.format("%.2f", pTrdAmt));
		jsonObj.put("pTrdAmt", String.format("%.2f", pTrdAmt));
		jsonObj.put("pFee", 0);
		jsonObj.put("pAcctType", IPSConstants.ACCT_TYPE);
		jsonObj.put("pIdentNo", user.idNumber);
		jsonObj.put("pRealName", user.realityName);
		jsonObj.put("pAccount", user.ipsAcctNo);
		jsonObj.put("pUse", bid.purpose.name);
		jsonObj.put("pWebUrl", IPSWebUrl.REGISTER_CREDITOR);
		jsonObj.put("pS2SUrl", IPSS2SUrl.REGISTER_CREDITOR);
		jsonObj.put("pMemo1", "pMemo1");
		jsonObj.put("pMemo2", "pMemo2");
		jsonObj.put("pMemo3", "pMemo3");

		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		System.out.println(strXml);
		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("action", IPSConstants.ACTION);
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("memberId", user.id+"");
		map.put("type", IPSOperation.REGISTER_CREDITOR+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("autoInvest", pRegType==1 ? "" : "autoInvest");
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("argSign", argSign);

		return map;
	}
	
	/**
	 * 登记债权人回调
	 */
	public void registerCreditorCB(ErrorInfo error) {
		error.clear();
		
		if(!Payment.checkSign(this.pMerCode + this.pErrCode + this.pErrMsg + this.p3DesXmlPara, this.pSign)) {
			error.code = -1;
			error.msg = "sign校验失败";
			
			return;
		}
		
		if(!pErrCode.equals("MG00000F")) {
			error.code = -1;
			error.msg = this.pErrMsg;
			
			return;
		}
		
		JSONObject jsonObj = this.jsonPara;
		String pMerBillNo = jsonObj.getString("pMerBillNo");
		Map<String, Object> map = (Map<String, Object>) Cache.get(pMerBillNo);
		
		if (map == null) {
			error.code = -1;
			error.msg = "投标失败";
			
			return;
		}
		
		long userId = Convert.strToLong(map.get("userId") + "", -1);
		long bidId = Convert.strToLong(map.get("bidId") + "", -1);
		int investAmount = Convert.strToInt(map.get("investAmount") + "", -1);
		Map<String, Object> bid = Invest.bidMap(bidId, error);
		
		t_users user1 = null;

		try {
			user1 = GenericModel.findById(userId);
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起！系统异常！请您联系平台管理员！";
			error.code = -2;

			return;
		}
		
		Invest.doInvest(user1, bid, investAmount, pMerBillNo, error);
		
		if (error.code < 0) {
			return;
		}
		
		IpsDetail.updateStatus(pMerBillNo, Status.SUCCESS, error);
	}
	
	/**
	 * 登记债权转让接口
	 * @return
	 */
	public static Map<String, Object> registerCretansfer(String pMerBillNo, long fromUserId, long toUserId, String bidNo, String pCreMerBillNo, double pCretAmt, double pPayAmt, double pFromFee) {
		User fromUser = new User();
		fromUser.id = fromUserId;
		
		User toUser = new User();
		toUser.id = toUserId;
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", pMerBillNo);
		jsonObj.put("pMerDate", DateUtil.simple(new Date()));
		jsonObj.put("pBidNo", bidNo);
		jsonObj.put("pContractNo", "pContractNo");
		
		jsonObj.put("pFromAccountType", IPSConstants.ACCT_TYPE);
		jsonObj.put("pFromName", fromUser.realityName);
		jsonObj.put("pFromAccount", fromUser.ipsAcctNo);
		jsonObj.put("pFromIdentType", IPSConstants.INDENT_TYPE);
		jsonObj.put("pFromIdentNo", fromUser.idNumber);
		
		jsonObj.put("pToAccountType", IPSConstants.ACCT_TYPE);
		jsonObj.put("pToAccountName", toUser.realityName);
		jsonObj.put("pToAccount", toUser.ipsAcctNo);
		jsonObj.put("pToIdentType", IPSConstants.INDENT_TYPE);
		jsonObj.put("pToIdentNo", toUser.idNumber);
		
		jsonObj.put("pCreMerBillNo", pCreMerBillNo);
		jsonObj.put("pCretAmt", String.format("%.2f", pCretAmt));
		jsonObj.put("pPayAmt", String.format("%.2f", pPayAmt));
		jsonObj.put("pFromFee", String.format("%.2f", pFromFee));
		jsonObj.put("pToFee", 0);
		jsonObj.put("pCretType", 1);
		
		jsonObj.put("pWebUrl", IPSWebUrl.REGISTER_CRETANSFER);
		jsonObj.put("pS2SUrl", IPSS2SUrl.REGISTER_CRETANSFER);
		jsonObj.put("pMemo1", "pMemo1");
		jsonObj.put("pMemo2", "pMemo2");
		jsonObj.put("pMemo3", "pMemo3");

		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		
		System.out.println(strXml);
		
		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("action", IPSConstants.ACTION);
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("type", IPSOperation.REGISTER_CRETANSFER);
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("argSign", argSign);

		return map;
	}
	
	/**
	 * 登记债权转让回调
	 */
	public void registerCretansferCB(ErrorInfo error) {
		error.clear();
		
		if(!Payment.checkSign(this.pMerCode + this.pErrCode + this.pErrMsg + this.p3DesXmlPara, this.pSign)) {
			error.code = -1;
			error.msg = "sign校验失败";
			
			return;
		}
		
		if(!"MG00000F".equals(this.pErrCode)) {
			error.code = -1;
			error.msg = this.pErrMsg;
			
			return;
		}
		
		String pMerBillNo = this.jsonPara.getString("pMerBillNo");
		IpsDetail.updateStatus(pMerBillNo, Status.SUCCESS, error);
		
		if (error.code < 0) {
			return;
		}

		long debtId = (Long) Cache.get(pMerBillNo);
		String paymentMerBillNo = Payment.transferForCretransfer(pMerBillNo, debtId, error);
		
		if (error.code < 0) {
			return;
		}
		
		Debt.dealDebtTransfer(paymentMerBillNo, debtId, error);
	}
	
	/**
	 * 自动投标签约
	 * @return
	 */
	public static Map<String, String> autoNewSigning(String pValidType, int pValidDate, String pTrdCycleType, int pSTrdCycleValue,
			int pETrdCycleValue, double pSAmtQuota, double pEAmtQuota, double pSIRQuota,double pEIRQuota) {
		User user = User.currUser();
		
		String pMerBillNo = createBillNo(user.id, IPSOperation.AUTO_NEW_SIGNING);
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", pMerBillNo);
		jsonObj.put("pSigningDate", DateUtil.simple(new Date()));
		jsonObj.put("pIdentNo", user.idNumber);
		jsonObj.put("pRealName", user.realityName);
		jsonObj.put("pIpsAcctNo", user.ipsAcctNo);
		
		jsonObj.put("pValidType", pValidType);
		jsonObj.put("pValidDate", pValidDate);
		jsonObj.put("pTrdCycleType", pTrdCycleType);
		jsonObj.put("pSTrdCycleValue", pSTrdCycleValue);
		jsonObj.put("pETrdCycleValue", pETrdCycleValue);
		jsonObj.put("pSAmtQuota", String.format("%.2f", pSAmtQuota));
		jsonObj.put("pEAmtQuota", String.format("%.2f", pEAmtQuota));
		jsonObj.put("pSIRQuota", String.format("%.2f", pSIRQuota));
		jsonObj.put("pEIRQuota", String.format("%.2f", pEIRQuota));
		
		jsonObj.put("pWebUrl", IPSWebUrl.AUTO_NEW_SIGNING);
		jsonObj.put("pS2SUrl", IPSS2SUrl.AUTO_NEW_SIGNING);
		jsonObj.put("pMemo1", "pMemo1");
		jsonObj.put("pMemo2", "pMemo2");
		jsonObj.put("pMemo3", "pMemo3");

		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		
		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("action", IPSConstants.ACTION);
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("type", IPSOperation.AUTO_NEW_SIGNING+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("memberId", user.id+"");
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("argSign", argSign);

		return map;
	}
	
	/**
	 * 自动投标签约回调
	 * @param userId
	 * @param error
	 */
	public void autoNewSigningCB(ErrorInfo error) {
		error.clear();
		
		if(!Payment.checkSign(this.pMerCode + this.pErrCode + this.pErrMsg + this.p3DesXmlPara, this.pSign)) {
			error.code = -1;
			error.msg = "sign校验失败";
			
			return;
		}
		
		if(!"MG00000F".equals(this.pErrCode)) {
			error.code = -1;
			error.msg = this.pErrMsg;
			
			return;
		}
		
		String pIpsAuthNo = this.jsonPara.getString("pIpsAuthNo");
		
		User user = new User();
		user.ipsBidAuthNo = pIpsAuthNo;
		user.updateIpsBidAuthNo(this.jsonPara.getLong("pMemo1"), error);
	}
	
	/**
	 * 自动还款签约
	 * @return
	 */
	public static Map<String, String> repaymentSigning() {
		User user = User.currUser();
		
		String pMerBillNo = createBillNo(user.id, IPSOperation.REPAYMENT_SIGNING);
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", pMerBillNo);
		jsonObj.put("pSigningDate", DateUtil.simple(new Date()));
		jsonObj.put("pIdentType", IPSConstants.INDENT_TYPE);
		jsonObj.put("pIdentNo", user.idNumber);
		jsonObj.put("pRealName", user.realityName);
		jsonObj.put("pIpsAcctNo", user.ipsAcctNo);
		
		jsonObj.put("pValidType", IPSConstants.VALID_TYPE);
		jsonObj.put("pValidDate", IPSConstants.VALID_DATE);
		
		jsonObj.put("pWebUrl", IPSWebUrl.REPAYMENT_SIGNING);
		jsonObj.put("pS2SUrl", IPSS2SUrl.REPAYMENT_SIGNING);
		jsonObj.put("pMemo1", "pMemo1");
		jsonObj.put("pMemo2", "pMemo2");
		jsonObj.put("pMemo3", "pMemo3");

		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		
		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("action", IPSConstants.ACTION);
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("memberId", user.id+"");
		map.put("type", IPSOperation.REPAYMENT_SIGNING+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("argSign", argSign);

		return map;
	}
	
	/**
	 * 自动还款签约回调
	 * @param userId
	 * @param error
	 */
	public void repaymentSigningCB(ErrorInfo error) {
		error.clear();
		
		if(!Payment.checkSign(this.pMerCode + this.pErrCode + this.pErrMsg + this.p3DesXmlPara, this.pSign)) {
			error.code = -1;
			error.msg = "sign校验失败";
			
			return;
		}
		
		if(!"MG00000F".equals(this.pErrCode)) {
			error.code = -1;
			error.msg = this.pErrMsg;
			
			return;
		}
		
		String pIpsAuthNo = this.jsonPara.getString("pIpsAuthNo");
		
		User user = new User();
		user.ipsRepayAuthNo = pIpsAuthNo;
		user.updateIpsRepayAuthNo(this.jsonPara.getLong("pMemo1"), error);
	}
	
	/**
	 * 充值
	 * @return
	 */
	public static Map<String, String> doDpTrade(double pTrdAmt, String pTrdBnkCode, ErrorInfo error) {
		User user = User.currUser();
		
		String pMerBillNo = createBillNo(user.id, IPSOperation.DO_DP_TRADE);
		
		IpsDetail detail = new IpsDetail();
		detail.merBillNo = pMerBillNo;
		detail.userName = user.name;
		detail.time = new Date();
		detail.type = IPSOperation.DO_DP_TRADE;
		detail.status = Status.FAIL;
		detail.create(error);
		
		if (error.code < 0) {
			return null;
		}
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", pMerBillNo);
		
		jsonObj.put("pAcctType", IPSConstants.ACCT_TYPE);
		jsonObj.put("pIdentNo", user.idNumber);
		jsonObj.put("pRealName", user.realityName);
		jsonObj.put("pIpsAcctNo", user.ipsAcctNo);
		
		jsonObj.put("pTrdDate", DateUtil.simple(new Date()));
		jsonObj.put("pTrdAmt", String.format("%.2f", pTrdAmt));
		jsonObj.put("pChannelType", IPSConstants.CHANNEL_TYPE);
		jsonObj.put("pTrdBnkCode", pTrdBnkCode);
		jsonObj.put("pMerFee", "0");
		jsonObj.put("pIpsFeeType", IPSConstants.IPS_FEE_TYPE);
		
		jsonObj.put("pWebUrl", IPSWebUrl.DO_DP_TRADE);
		jsonObj.put("pS2SUrl", IPSS2SUrl.DO_DP_TRADE);
		jsonObj.put("pMemo1", "pMemo1");
		jsonObj.put("pMemo2", "pMemo2");
		jsonObj.put("pMemo3", "pMemo3");

		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		
		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("action", IPSConstants.ACTION);
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("memberId", user.id+"");
		map.put("type", IPSOperation.DO_DP_TRADE+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("argSign", argSign);
		
		User.sequence(0, pMerBillNo, pTrdAmt, Constants.ENTRUST_RECHARGE, error);

		return map;
	}
	
	/**
	 * 充值回调
	 * @param userId
	 * @param error
	 */
	public void doDpTradeCB(ErrorInfo error) {
		error.clear();
		
		if(!Payment.checkSign(this.pMerCode + this.pErrCode + this.pErrMsg + this.p3DesXmlPara, this.pSign)) {
			error.code = -1;
			error.msg = "sign校验失败";
			
			return;
		}
		
		if(!"MG00000F".equals(this.pErrCode)) {
			error.code = -1;
			error.msg = this.pErrMsg;
			
			return;
		}
		
		String pMerBillNo = this.jsonPara.getString("pMerBillNo");
		String amount = this.jsonPara.getString("pTrdAmt");
		
		User.recharge(pMerBillNo, Double.parseDouble(amount), error);
		
		if (error.code < 0) {
			return;
		}
		
		IpsDetail.updateStatus(pMerBillNo, Status.SUCCESS, error);
	}
	
	/**
	 * 转账 (放款)
	 * @param pMerBillNo
	 * @param bidId
	 * @param error
	 */
	public static void transfer(String pMerBillNo, long bidId, ErrorInfo error) {
		error.clear();
		
		Bid bid = new Bid();
		bid.auditBid = true;
		bid.id = bidId;
		
		double tFee = Arith.round(bid.serviceFees, 2); // 借款人手续费
		double sum = Arith.round(bid.amount, 2);
		double sumFee = 0;
		t_users tUser = null;
		
		try {
			tUser = GenericModel.findById(bid.userId);
		} catch (Exception e) {
			Logger.info(e.getMessage());
			error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
			error.code = -111;
			
			return;
		}
		
		IpsDetail detail = new IpsDetail();
		detail.merBillNo = pMerBillNo;
		detail.userName = tUser.name;
		detail.time = new Date();
		detail.type = IPSOperation.TRANSFER_ONE;
		detail.status = Status.FAIL;
		detail.memo = ""+bidId;
		detail.create(error);
		
		if (error.code < 0) {
			return;
		}
		
		List<Invest> invests = Invest.queryAllInvestUser(bid.id);
		JSONArray pDetails = new JSONArray();
		
		for (int i = 0; i < invests.size(); i++) {
			Invest invest = invests.get(i);
			String pOriMerBillNo = invest.merBillNo;
			double pTrdAmt = invest.investAmount;
			t_users fUser = null;
			
			try {
				fUser = GenericModel.findById(invest.investUserId);
			} catch (Exception e) {
				Logger.info(e.getMessage());
				error.msg = "对不起！系统异常，给您造成的不便敬请谅解！";
				error.code = -111;
				
				return;
			}
			
			double pTTrdFee = 0;
			double per = pTrdAmt / sum;

			if (i == invests.size()-1) {
				pTTrdFee =  Arith.round(tFee - sumFee, 2);
			} else {
				pTTrdFee = Arith.round(tFee * per, 2);
				sumFee += pTTrdFee;
			}
			Logger.info("pOriMerBillNo>>>", pOriMerBillNo);
			JSONObject pRow = new JSONObject();
			pRow.put("pOriMerBillNo", pOriMerBillNo);
			pRow.put("pTrdAmt", String.format("%.2f", pTrdAmt));
			pRow.put("pFAcctType", IPSConstants.ACCT_TYPE);
			pRow.put("pFIpsAcctNo", fUser.ips_acct_no);
			pRow.put("pFTrdFee", 0);
			pRow.put("pTAcctType", IPSConstants.ACCT_TYPE);
			pRow.put("pTIpsAcctNo", tUser.ips_acct_no);
			pRow.put("pTTrdFee", String.format("%.2f", pTTrdFee));
			pDetails.add(pRow);
		}
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", pMerBillNo);
		jsonObj.put("pBidNo", bid.bidNo);
		jsonObj.put("pDate", DateUtil.simple(new Date()));
		jsonObj.put("pTransferType", TransferType.INVEST);
		jsonObj.put("pTransferMode", 1);
		jsonObj.put("pS2SUrl", IPSS2SUrl.TRANSFER);
		jsonObj.put("pDetails", pDetails);
		jsonObj.put("pMemo1", "pMemo1");
		jsonObj.put("pMemo2", "pMemo2");
		jsonObj.put("pMemo3", "pMemo3");

		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null);
		
		System.out.println(strXml);
		
		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("action", IPSConstants.ACTION);
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("type", IPSOperation.TRANSFER+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("argSign", argSign);
		
		Logger.info("arg3DesXmlPara:%s",strXml);
		
		String strJson = WS.url(IPSConstants.ACTION).setParameters(map).get().getString();
		
		if (IPSConstants.IS_REPAIR_TEST) {
			error.code = -1;
			error.msg = "模拟转账 (放款)掉单";
			
			return;
		}
		
		Logger.info("转账回调原始数据\n%s", strJson);
		
		if (strJson == null) {
			error.code = -1;
			error.msg = "转账失败";
			
			return;
		}
		
		JSONObject cbJsonObj = JSONObject.fromObject(strJson);
		
		Payment pay = new Payment();
		pay.pMerCode = cbJsonObj.getString("pMerCode");
		pay.pErrCode = cbJsonObj.getString("pErrCode");
		pay.pErrMsg = cbJsonObj.getString("pErrMsg");
		pay.p3DesXmlPara = cbJsonObj.getString("p3DesXmlPara");
		pay.pSign = cbJsonObj.getString("pSign");
		pay.transferCB(error);
		
		if (error.code < 0) {
			return;
		}
		
		IpsDetail.updateStatus(pMerBillNo, Status.SUCCESS, error);
	}
	
	/**
	 * 转账 (债权转让)
	 * @param pCreMerBillNo
	 * @param debtId
	 * @param error
	 * @return
	 */
	public static String transferForCretransfer(String pCreMerBillNo, long debtId, ErrorInfo error) {
		error.clear();
		Map<String, Object> debtInfo = Debt.queryTransferInfo(debtId, error);
		
		if (error.code < 0) {
			return null;
		}
		
		long fromUserId = (Long) debtInfo.get("toUserId"); 
		long toUserId = (Long) debtInfo.get("fromUserId");
		String bidNo = (String) debtInfo.get("bidNo"); 
		double pPayAmt = (Double) debtInfo.get("pPayAmt");
		double pTTrdFee = (Double) debtInfo.get("managefee");
		
		t_users fUser = null;//money转出方
		t_users tUser = null;//money转入方
		
		try {
			fUser = GenericModel.findById(fromUserId);
			tUser = GenericModel.findById(toUserId);
		} catch (Exception e) {
			Logger.info(e.getMessage());
			error.msg = "数据库异常";
			error.code = -1;
			
			return null;
		}
		
		String pMerBillNo = createBillNo(0, IPSOperation.TRANSFER);
		JSONArray pDetails = new JSONArray();
		
		JSONObject pRow = new JSONObject();
		pRow.put("pOriMerBillNo", pCreMerBillNo);
		pRow.put("pTrdAmt", String.format("%.2f", pPayAmt));
		pRow.put("pFAcctType", IPSConstants.ACCT_TYPE);
		pRow.put("pFIpsAcctNo", fUser.ips_acct_no);
		pRow.put("pFTrdFee", 0);
		pRow.put("pTAcctType", IPSConstants.ACCT_TYPE);
		pRow.put("pTIpsAcctNo", tUser.ips_acct_no);
		pRow.put("pTTrdFee", String.format("%.2f", pTTrdFee));
		pDetails.add(pRow);
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", pMerBillNo);
		jsonObj.put("pBidNo", bidNo);
		jsonObj.put("pDate", DateUtil.simple(new Date()));
		jsonObj.put("pTransferType", TransferType.CRETRANSFER);
		jsonObj.put("pTransferMode", 1);
		jsonObj.put("pS2SUrl", IPSS2SUrl.TRANSFER);
		jsonObj.put("pDetails", pDetails);
		jsonObj.put("pMemo1", "pMemo1");
		jsonObj.put("pMemo2", "pMemo2");
		jsonObj.put("pMemo3", "pMemo3");

		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null);
		
		Logger.info("转账(债权转让)接口输入参数\n"+strXml);
		
		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("action", IPSConstants.ACTION);
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("type", IPSOperation.TRANSFER+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("argSign", argSign);
		
		IpsDetail detail = new IpsDetail();
		detail.merBillNo = pMerBillNo;
		detail.userName = fUser.name;
		detail.time = new Date();
		detail.type = IPSOperation.TRANSFER_FOUR;
		detail.status = Status.FAIL;
		detail.memo = ""+debtId;
		detail.create(error);
		
		if (error.code < 0) {
			return null;
		}
		
		String strJson = WS.url(IPSConstants.ACTION).setParameters(map).get().getString();
		Logger.info("转账(债权转让)接口输出参数\n"+strJson);
		
		if (IPSConstants.IS_REPAIR_TEST) {
			error.code = -1;
			error.msg = "模拟转账 (债权转让)掉单";
			
			return null;
		}
		
		if (StringUtils.isBlank(strJson)) {
			error.code = -1;
			error.msg = "转账失败";
			
			return null;
		}
		
		JSONObject cbJsonObj = JSONObject.fromObject(strJson);
		
		Payment pay = new Payment();
		pay.pMerCode = cbJsonObj.getString("pMerCode");
		pay.pErrCode = cbJsonObj.getString("pErrCode");
		pay.pErrMsg = cbJsonObj.getString("pErrMsg");
		pay.p3DesXmlPara = cbJsonObj.getString("p3DesXmlPara");
		pay.pSign = cbJsonObj.getString("pSign");
		pay.transferCB(error);
		
		if (error.code < 0) {
			return null;
		}
		
		IpsDetail.updateStatus(jsonObj.getString("pMerBillNo"), Status.SUCCESS, error);
		
		if (error.code < 0) {
			return null;
		}
		
		return jsonObj.getString("pMerBillNo");
	}
	
	/**
	 * 转账回调
	 */
	public void transferCB(ErrorInfo error) {
		error.clear();
		
		if(!Payment.checkSign(this.pMerCode + this.pErrCode + this.pErrMsg + this.p3DesXmlPara, this.pSign)) {
			error.code = -1;
			error.msg = "sign校验失败";
			
			return;
		}
		
		if(pErrCode==null || !(pErrCode.equals("MG00000F") || pErrCode.equals("MG00008F"))) {
			error.code = -1;
			error.msg = this.pErrMsg;
			
			return;
		}
		
		String xmlPara = Encrypt.decrypt3DES(this.p3DesXmlPara, Constants.ENCRYPTION_KEY);
		
		if (xmlPara.substring(0, 1).equals("?")) {
			xmlPara = xmlPara.substring(1);
		}
		
		JSONObject jsonObj = (JSONObject)Converter.xmlToObj(xmlPara);
		String pMerBillNo = jsonObj.getString("pMerBillNo");
		int pTransferType = jsonObj.getInt("pTransferType");
		
		/**
		 * 投资(放款)
		 */
		if (pTransferType == TransferType.INVEST) {
			Bid bid = (Bid) Cache.get(pMerBillNo);
			
			if (bid == null) {
				error.code = -1;
				error.msg = "放款失败";
				
				return;
			}
			
			bid.doEaitLoanToRepayment(error);
			
			return;
		}
		
		/**
		 * 债权转让
		 */
		if (pTransferType == TransferType.CRETRANSFER) {
			error.code = 0;
			error.msg = "转账(债权转让)成功";
			
			return;
		}
		
		error.code = -1;
		error.msg = "转账方式未知";
	}
	
	/**
	 * 还款
	 * @return
	 */
	public static Map<String, String> repaymentNewTrade(Map<String, List<Map<String, Object>>> mapList, long billId, ErrorInfo error) {
		User user = User.currUser();
		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();
		
		String pMerBillNo = createBillNo(user.id, IPSOperation.REPAYMENT_NEW_TRADE);
		
		IpsDetail detail = new IpsDetail();
		detail.merBillNo = pMerBillNo;
		detail.userName = user.name;
		detail.time = new Date();
		detail.type = IPSOperation.REPAYMENT_NEW_TRADE;
		detail.status = Status.FAIL;
		detail.memo = ""+billId;
		detail.create(error);
		
		if (error.code < 0) {
			return null;
		}
		
		JSONArray pDetails = new JSONArray();
		
		List<Map<String, Object>> investRepayment = mapList.get("investRepayment");
		List<Map<String, Object>> bidRepayment = mapList.get("bidRepayment");
		Map<String, Object> repaymentMap = bidRepayment.get(0);
		Map<String, Object> perBillNo = bidRepayment.get(1);
		
		JSONObject pRow = null;
		double receiveCorpus = 0;
		double receiveInterest = 0;
		double manageFee = 0;
		double receive  = 0;
		
//		int mark = (Integer) repaymentMap.get("overdue_mark");
		
		for(Map<String, Object> param : investRepayment) {
			pRow = new JSONObject();
			
			receiveCorpus = (Double) param.get("receive_corpus");//投资本金
			receiveInterest = (Double) param.get("receive_interest");//投资利息
			manageFee = Arith.round(Arith.mul(receiveInterest, backstageSet.investmentFee)/100, 2);// 投资管理费
			receive = receiveCorpus + receiveInterest;//计算投资人将获得的收益
			Logger.info(param.get("merBillNo").toString().toString());
			pRow.put("pCreMerBillNo", param.get("merBillNo").toString());
			pRow.put("pInAcctNo", param.get("ipsAcctNo").toString());
			pRow.put("pInFee", String.format("%.2f", manageFee));
			pRow.put("pOutInfoFee", "0");
			pRow.put("pInAmt", String.format("%.2f", receive));
			
			pDetails.add(pRow);
		}
		
		JSONObject jsonObj = new JSONObject();
		
		jsonObj.put("pBidNo", repaymentMap.get("bid_no"));
		jsonObj.put("pRepaymentDate", DateUtil.simple(new Date()));
		jsonObj.put("pMerBillNo", pMerBillNo);
		
		jsonObj.put("pRepayType", IPSConstants.REPAYMENT_TYPE);
		jsonObj.put("pIpsAuthNo", user.ipsRepayAuthNo);
		jsonObj.put("pOutAcctNo", repaymentMap.get("ips_acct_no"));//double payment = repaymentCorpus + repaymentInterest + repayOverdueFine
		jsonObj.put("pOutAmt", String.format("%.2f", Arith.add((Double) repaymentMap.get("repayment_corpus"), (Double) repaymentMap.get("repayment_interest")+ (Double)repaymentMap.get("overdue_fine"))));
		jsonObj.put("pOutFee", "0");
		
		jsonObj.put("pWebUrl", IPSWebUrl.REPAYMENT_NEW_TRADE);
		jsonObj.put("pS2SUrl", IPSS2SUrl.REPAYMENT_NEW_TRADE);
		jsonObj.put("pDetails", pDetails);
		jsonObj.put("pMemo1", "pMemo1");
		jsonObj.put("pMemo2", "pMemo2");
		jsonObj.put("pMemo3", perBillNo.get("bidRepayment"));
		Logger.info("还款:"+jsonObj.toString());
		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null);
		Logger.info(strXml);
		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("action", IPSConstants.ACTION);
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("memberId", user.id+"");
		map.put("type", IPSOperation.REPAYMENT_NEW_TRADE+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("argSign", argSign);

		return map;
	}
	
	/**
	 * 还款回调
	 * @param userId
	 * @param error
	 */
	public void repaymentNewTradeCB(ErrorInfo error) {
		error.clear();
		
		if(!Payment.checkSign(this.pMerCode + this.pErrCode + this.pErrMsg + this.p3DesXmlPara, this.pSign)) {
			error.code = -1;
			error.msg = "sign校验失败";
			
			return;
		}
		
		if(!("MG00000F".equals(this.pErrCode) || "MG00008F".equals(this.pErrCode))) {
			error.code = -1;
			error.msg = this.pErrMsg;
			
			return;
		}
		
		String pepaymentCache = this.jsonPara.getString("pMemo3");
		
		Map<String, List<Map<String, Object>>> mapList = (Map<String, List<Map<String, Object>>>) Cache.get("pepaymentCache_"+pepaymentCache);
		
		List<Map<String, Object>> bidRepayment = mapList.get("bidRepayment");
		List<Map<String, Object>> investRepayment = mapList.get("investRepayment");
		
		Map<String, Object> perBillNoMap = bidRepayment.get(1);
		
		long userId = (Long) perBillNoMap.get("userId");
		long billId = (Long) perBillNoMap.get("billId");
		long bidId = (Long) perBillNoMap.get("bidId");
		double repaymentCorpus = (Double) perBillNoMap.get("repaymentCorpus");
		double repaymentInterest = (Double) perBillNoMap.get("repaymentInterest");
		double repayOverdueFine = (Double) perBillNoMap.get("repayOverdueFine");
		double balance = (Double) perBillNoMap.get("balance");
		int period = (Integer) perBillNoMap.get("period");
		int mark = (Integer) perBillNoMap.get("mark");
		int status = (Integer) perBillNoMap.get("status");
		
		Bill bill = new Bill();
		bill.id = billId;
		
		if (mark == Constants.BILL_NO_OVERDUE) {
			bill.normalPayment(bidId, userId, repaymentCorpus, repaymentInterest, balance, investRepayment, period, error);
		}else if(mark == Constants.BILL_NO_OVERDUE) {
			bill.overduePayment(bidId, userId, repaymentCorpus, repaymentInterest, balance, investRepayment, status, repayOverdueFine, period, error);
		}
		
		if(error.code >= 0) {
			Cache.delete("pepaymentCache_"+pepaymentCache);
			IpsDetail.updateStatus(this.jsonPara.getString("pMerBillNo"), Status.SUCCESS, error);
		}
	}
	
	/**
	 * 解冻保证金
	 * @return
	 */
//	public static Map<String, String> guaranteeUnfreeze(long bidId, double pUnfreezeAmt, int pUnfreezenType, int pAcctType) {
//		String pMerBillNo = createBillNo(0, IPSOperation.GUARANTEE_UNFREEZE);
//		
//		String pIdentNo = null;
//		String pRealName = null;
//		String pIpsAcctNo = null;
//		
//		if (pAcctType == 1) {
//			Bid bid = new Bid();
//			bid.id = bidId;
//			
//			User user = User.currUser();
//			
//			pIdentNo = user.idNumber;
//			pRealName = user.realityName;
//			pIpsAcctNo = user.ipsAcctNo;
//		} else {
//			pIdentNo = IPSConstants.MER_IDENT_NO;
//			pRealName = IPSConstants.MER_NAME;
//			pIpsAcctNo = IPSConstants.MER_CODE;
//		}
//		
//		JSONObject jsonObj = new JSONObject();
//		jsonObj.put("pMerBillNo", pMerBillNo);
//		
//		jsonObj.put("pBidNo", Bid.currBid().id);
//		jsonObj.put("pUnfreezeDate", DateUtil.simple(new Date()));
//		jsonObj.put("pUnfreezeAmt", String.format("%.2f", pUnfreezeAmt));
//		jsonObj.put("pUnfreezenType", pUnfreezenType);
//		
//		jsonObj.put("pAcctType", pAcctType);
//		jsonObj.put("pIdentNo", pIdentNo);
//		jsonObj.put("pRealName", pRealName);
//		jsonObj.put("pIpsAcctNo", pIpsAcctNo);
//		
//		jsonObj.put("pS2SUrl", IPSS2SUrl.GUARANTEE_UNFREEZE);
//		jsonObj.put("pMemo1", "pMemo1");
//		jsonObj.put("pMemo2", "pMemo2");
//		jsonObj.put("pMemo3", "pMemo3");
//
//		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
//		System.out.println(strXml);
//		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
//		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
//		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
//		
//		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);
//
//		Map<String, String> map = new HashMap<String, String>();
//		map.put("action", IPSConstants.ACTION);
//		map.put("platform", IPSConstants.PLATFORM);
//		map.put("type", IPSOperation.GUARANTEE_UNFREEZE+"");
//		map.put("argMerCode", IPSConstants.MER_CODE);
//		map.put("arg3DesXmlPara", arg3DesXmlPara);
//		map.put("argSign", argSign);
//
//		return map;
//	}
	
	/**
	 * 提现
	 * @return
	 */
	public static Map<String, String> doDwTrade(long withdrawalId, double pTrdAmt, ErrorInfo error) {
		User user = User.currUser();
		String pMerBillNo = createBillNo(user.id, IPSOperation.DO_DW_TRADE);
		
		IpsDetail detail = new IpsDetail();
		detail.merBillNo = pMerBillNo;
		detail.userName = user.name;
		detail.time = new Date();
		detail.type = IPSOperation.DO_DW_TRADE;
		detail.status = Status.FAIL;
		detail.memo = ""+withdrawalId;
		detail.create(error);
		
		if (error.code < 0) {
			return null;
		}
		 
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", pMerBillNo);
		jsonObj.put("pAcctType", IPSConstants.ACCT_TYPE);
		jsonObj.put("pOutType", IPSConstants.OUT_TYPE);
		jsonObj.put("pBidNo", "");
		jsonObj.put("pContractNo", "");
		jsonObj.put("pDwTo", "");
		jsonObj.put("pIdentNo", user.idNumber);
		jsonObj.put("pRealName", user.realityName);
		jsonObj.put("pIpsAcctNo", user.ipsAcctNo);
		jsonObj.put("pDwDate", DateUtil.simple(new Date()));
		jsonObj.put("pTrdAmt", String.format("%.2f", pTrdAmt));
		jsonObj.put("pMerFee", String.format("%.2f", withdrawalFee(pTrdAmt)));
		jsonObj.put("pIpsFeeType", IPSConstants.IPS_FEE_TYPE);
		jsonObj.put("pWebUrl", IPSWebUrl.DO_DW_TRADE);
		jsonObj.put("pS2SUrl", IPSS2SUrl.DO_DW_TRADE);
		jsonObj.put("pMemo1", "pMemo1");
		jsonObj.put("pMemo2", "pMemo2");
		jsonObj.put("pMemo3", withdrawalId+"");

		String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		
		String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("action", IPSConstants.ACTION);
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("memberId", user.id+"");
		map.put("type", IPSOperation.DO_DW_TRADE+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("argSign", argSign);
		
		return map;
	}
	
	/**
	 * 提现回调
	 * @param userId
	 * @param error
	 */
	public void doDwTradeCB(ErrorInfo error) {
		error.clear();
		
		if(!Payment.checkSign(this.pMerCode + this.pErrCode + this.pErrMsg + this.p3DesXmlPara, this.pSign)) {
			error.code = -1;
			error.msg = "sign校验失败";
			
			return;
		}
		
		if(!"MG00000F".equals(this.pErrCode)) {
			error.code = -1;
			error.msg = this.pErrMsg;
			
			return;
		}
		
		String pMerBillNo = this.jsonPara.getString("pMerBillNo");
		long withdrawalId = this.jsonPara.getLong("pMemo3");
		
		User.withdrawalNotice(this.jsonPara.getLong("pMemo1"), withdrawalId, 1, true, error);
		
		if (error.code < 0) {
			return;
		}
		
		IpsDetail.updateStatus(pMerBillNo, Status.SUCCESS, error);
	}
	
	/**
	 * 账户余额查询
	 * @return
	 */
	public static JSONObject queryForAccBalance(String argIpsAccount, ErrorInfo error) {
		error.clear();
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + argIpsAccount + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("memberId", User.currUser().id+"");
		map.put("type", IPSOperation.QUERY_FOR_ACC_BALANCE+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("argIpsAccount", argIpsAccount);
		map.put("argSign", argSign);
		
		String strJson = WS.url(IPSConstants.ACTION).setParameters(map).get().getString();
		
		if (strJson == null) {
			error.code = -1;
			error.msg = "账户余额查询失败";
			
			return null;
		}
		
		JSONObject jsonObj = JSONObject.fromObject(strJson);
		
//		String src = "<pMerCode>" + jsonObj.getString("pMerCode") + "</pMerCode>" + 
//		"<pErrCode>" + jsonObj.getString("pErrCode") + "</pErrCode>" + 
//		"<pErrMsg>" + jsonObj.getString("pErrMsg") + "</pErrMsg>" + 
//		"<pIpsAcctNo>" + jsonObj.getString("pIpsAcctNo") + "</pIpsAcctNo>" +
//		"<pBalance>" + jsonObj.getString("pBalance") + "</pBalance>" +
//		"<pLock>" + jsonObj.getString("pLock") + "</pLock>" +
//		"<pNeedstl>" + jsonObj.getString("pNeedstl") + "</pNeedstl>";
//		
//		String pSign = jsonObj.getString("pSign");
//		
//		if (!checkSign(src, pSign)) {
//			error.code = -1;
//			error.msg = "签名失败";
//			
//			return null;
//		}
		
		return jsonObj;
	}
	
	/**
	 * 商户端获取银行列表查询
	 * @return
	 */
	public static List<Map<String, Object>> getBankList(ErrorInfo error) {
		error.clear();
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("memberId", User.currUser().id+"");
		map.put("type", IPSOperation.GET_BANK_LIST+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("argSign", argSign);
		
		String strJson = WS.url(IPSConstants.ACTION).setParameters(map).get().getString();
		Logger.info(strJson);
		if (strJson == null) {
			error.code = -1;
			error.msg = "商户端获取银行列表查询失败";
			
			return null;
		}
		
		JSONObject jsonObj = JSONObject.fromObject(strJson);
		
//		String src = "<pMerCode>" + jsonObj.getString("pMerCode") + "</pMerCode>" + 
//		"<pErrCode>" + jsonObj.getString("pErrCode") + "</pErrCode>" + 
//		"<pErrMsg>" + jsonObj.getString("pErrMsg") + "</pErrMsg>" + 
//		"<pBankList>" + jsonObj.getString("pBankList") + "</pBankList>";
//		
//		String pSign = jsonObj.getString("pSign");
//		
//		if (!checkSign(src, pSign)) {
//			error.code = -1;
//			error.msg = "签名失败";
//			
//			return null;
//		}
		
		//<pBankList>银行名称|银行卡别名|银行卡编号#银行名称|银行卡别名|银行卡编号</pBankList>
		String pBankList = jsonObj.getString("pBankList");
		System.out.println(pBankList);
		if (pBankList == null) {
			error.code = -1;
			error.msg = "商户银行列表失败";
			
			return null;
		}
		
		String[] banks = pBankList.split("#");
		List<Map<String, Object>> bankList = new ArrayList<Map<String,Object>>();
		
		for (String strBank : banks) {
			String[] bankParams = strBank.split("\\|");
			
			if (bankParams.length < 3) {
				continue;
			}
			
			Map<String, Object> bank = new HashMap<String, Object>();
			bank.put("name", bankParams[0]);
			bank.put("alias", bankParams[1]);
			bank.put("code", bankParams[2]);
			
			bankList.add(bank);
		}
		
		return bankList;
	}
	
	/**
	 * 账户信息查询
	 * @return
	 */
	public static JSONObject queryMerUserInfo(String argIpsAccount, ErrorInfo error) {
		String argSign = Encrypt.MD5(IPSConstants.MER_CODE + argIpsAccount + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("memberId", User.currUser().id+"");
		map.put("type", IPSOperation.QUERY_MER_USER_INFO+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("argIpsAccount", argIpsAccount);
		map.put("argSign", argSign);
		map.put("argMemo", "argMemo");

		String strJson = WS.url(IPSConstants.ACTION).setParameters(map).get().getString();
		
		if (strJson == null) {
			error.code = -1;
			error.msg = "账户信息查询失败";
			
			return null;
		}
		
		JSONObject jsonObj = JSONObject.fromObject(strJson);
		
//		String src = "<pMerCode>" + jsonObj.getString("pMerCode") + "</pMerCode>" + 
//		"<pIpsAcctNo>" + jsonObj.getString("pIpsAcctNo") + "</pIpsAcctNo>" + 
//		"<pEmail>" + jsonObj.getString("pEmail") + "</pEmail>" + 
//		"<pStatus>" + jsonObj.getString("pStatus") + "</pStatus>" +
//		"<pUCardStatus>" + jsonObj.getString("pUCardStatus") + "</pUCardStatus>" +
//		"<pBankName>" + jsonObj.getString("pBankName") + "</pBankName>" +
//		"<pBankCard>" + jsonObj.getString("pBankCard") + "</pBankCard>" +
//		"<pBCardStatus>" + jsonObj.getString("pBCardStatus") + "</pBCardStatus>" +
//		"<pSignStatus>" + jsonObj.getString("pSignStatus") + "</pSignStatus>";
//		
//		String pSign = jsonObj.getString("pSign");
//		
//		if (!checkSign(src, pSign)) {
//			error.code = -1;
//			error.msg = "签名失败";
//			
//			return null;
//		}
		
		return jsonObj;
	}
	
	/**
	 * 交易查询
	 * @param pMerBillNo
	 * @param pTradeType
	 * @param error
	 * @return 1#成功、2#失败、3#处理中、4#未查询到交易
	 */
	public static int queryIpsStatus(String pMerBillNo, int pTradeType, ErrorInfo error) {
		error.clear();
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", pMerBillNo);
		jsonObj.put("pTradeType", new DecimalFormat("00").format(pTradeType));

		String arg3DesXmlPara = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		arg3DesXmlPara = Encrypt.encrypt3DES(arg3DesXmlPara, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");

		String pSign = Encrypt.MD5(IPSConstants.MER_CODE + arg3DesXmlPara + Constants.ENCRYPTION_KEY);

		Map<String, String> map = new HashMap<String, String>();
		map.put("domain", IPSConstants.DOMAIN);
		map.put("platform", IPSConstants.PLATFORM);
		map.put("type", IPSOperation.QUERY_TRADE+"");
		map.put("version", BackstageSet.getCurrentBackstageSet().entrustVersion);
		map.put("argMerCode", IPSConstants.MER_CODE);
		map.put("arg3DesXmlPara", arg3DesXmlPara);
		map.put("argSign", pSign);

		String strJson = WS.url(IPSConstants.ACTION).setParameters(map).get().getString();
		
		if (StringUtils.isBlank(strJson)) {
			error.code = -1;
			error.msg = "交易查询失败";
			
			return error.code;
		}
		
		jsonObj = JSONObject.fromObject(strJson);
		String pErrCode = jsonObj.getString("pErrCode");
		String pErrMsg = jsonObj.getString("pErrMsg");
		
		if(!pErrCode.equals("MG00000F")) {
			error.code = -1;
			error.msg = pErrMsg;
			
			return error.code;
		}
		
		String p3DesXmlPara = Encrypt.decrypt3DES(jsonObj.getString("p3DesXmlPara"), Constants.ENCRYPTION_KEY);
		jsonObj = (JSONObject) Converter.xmlToObj(p3DesXmlPara);
		
		error.code = 0;
		
		return jsonObj.getInt("pTradeStatue");
	}
	
	/**
	 * 提现服务费
	 * @param pTrdAmt
	 * @return
	 */
	public static double withdrawalFee(double pTrdAmt) {
		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();
		
		return (pTrdAmt - backstageSet.withdrawFee) > 0 ? Arith.round(((pTrdAmt - backstageSet.withdrawFee) * backstageSet.withdrawRate) / 100, 2) : 0;
	}
}
