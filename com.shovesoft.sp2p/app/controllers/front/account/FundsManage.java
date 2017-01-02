package controllers.front.account;

import java.io.File;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import com.shove.Convert;

import constants.Constants;
import constants.Constants.RechargeType;
import constants.OptionKeys;
import controllers.BaseController;
import controllers.front.red.RedAction;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import models.t_content_news;
import models.t_dict_audit_items;
import models.t_dict_payment_gateways;
import models.t_reds;
import models.t_reds_use;
import models.t_roma_invests;
import models.t_user_over_borrows;
import models.t_users;
import models.v_credit_levels;
import models.v_user_account_info;
import models.v_user_account_statistics;
import models.v_user_audit_items;
import models.v_user_detail_credit_score_audit_items;
import models.v_user_detail_credit_score_invest;
import models.v_user_detail_credit_score_loan;
import models.v_user_detail_credit_score_normal_repayment;
import models.v_user_detail_credit_score_overdue;
import models.v_user_detail_score;
import models.v_user_details;
import models.v_user_withdrawals;
import business.AuditItem;
import business.BackstageSet;
import business.CreditLevel;
import business.News;
import business.OverBorrow;
import business.Payment;
import business.User;
import business.UserAuditItem;
import business.UserBankAccounts;
import business.Vip;
import play.Logger;
import play.cache.Cache;
import play.db.jpa.GenericModel;
import play.mvc.With;
import utils.ErrorInfo;
import utils.ExcelUtils;
import utils.GopayUtils;
import utils.JsonDateValueProcessor;
import utils.PageBean;

@With(CheckAction.class)
public class FundsManage extends BaseController {

	// -------------------------------资金管理-------------------------
	/**
	 * 账户信息
	 */
	public static void accountInformation() {

		User user = User.currUser();
		long userId = user.id;
		ErrorInfo error = new ErrorInfo();
		// 根据ID查询真实姓名
		t_users users = User.queryUser2ByUserId(userId, error);
		v_user_account_statistics accountStatistics = User
				.queryAccountStatistics(userId, error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		v_user_account_info accountInfo = User.queryUserAccountInfo(userId,
				error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		List<v_user_details> userDetails = User.queryUserDetail(userId, error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		List<UserBankAccounts> userBanks = UserBankAccounts
				.queryUserAllBankAccount(userId);
		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();
		String content = News.queryContent(Constants.NewsTypeId.VIP_AGREEMENT,
				error);

		List<t_content_news> news = News.queryNewForFront(
				Constants.NewsTypeId.MONEY_TIPS, 3, error);

		boolean isIps = Constants.IPS_ENABLE;

		render(user, users, accountStatistics, accountInfo, userDetails,
				userBanks, backstageSet, content, news, isIps);
	}

	/**
	 * 添加银行账号
	 */
	public static void addBank(String addBankName, String addAccount,
			String addAccountName) {
		User user = User.currUser();

		ErrorInfo error = new ErrorInfo();

		UserBankAccounts bankUser = new UserBankAccounts();

		bankUser.userId = user.id;
		bankUser.bankName = addBankName;
		bankUser.account = addAccount;
		bankUser.accountName = addAccountName;

		bankUser.addUserBankAccount(error);

		JSONObject json = new JSONObject();
		json.put("error", error);

		renderJSON(json);
	}

	// 保存银行账号
	public static void saveBank() {
		render();
	}

	/**
	 * 编辑银行账号
	 */

	public static void editBank(long editAccountId, String editBankName,
			String editAccount, String editAccountName) {

		ErrorInfo error = new ErrorInfo();

		User user = User.currUser();
		UserBankAccounts userAccount = new UserBankAccounts();

		userAccount.bankName = editBankName;
		userAccount.account = editAccount;
		userAccount.accountName = editAccountName;

		userAccount.editUserBankAccount(editAccountId, user.id, error);

		JSONObject json = new JSONObject();
		json.put("error", error);

		renderJSON(json);
	}

	/**
	 * 删除银行账号
	 */
	public static void deleteBank(long accountId) {
		ErrorInfo error = new ErrorInfo();

		UserBankAccounts.deleteUserBankAccount(accountId, error);

		JSONObject json = new JSONObject();
		json.put("error", error);

		renderJSON(json);
	}

	/**
	 * 我的信用等级
	 */
	public static void myCredit() {
		User user = User.currUser();
		ErrorInfo error = new ErrorInfo();

		v_user_detail_score creditScore = User.queryCreditScore(user.id);

		List<t_user_over_borrows> overBorrows = OverBorrow
				.queryUserOverBorrows(user.id, error);

		if (error.code < 0) {
			render(user, Constants.ERROR_PAGE_PATH_FRONT);
		}

		// double creditInitialAmount = BackstageSet.queryCreditInitialAmount();
		double creditInitialAmount = BackstageSet.getCurrentBackstageSet().initialAmount;

		render(user, creditScore, overBorrows, creditInitialAmount);
	}

	/**
	 * 信用积分明细(成功借款)
	 */
	public static void creditDetailLoan(String key, int currPage) {
		User user = User.currUser();

		PageBean<v_user_detail_credit_score_loan> page = User
				.queryCreditDetailLoan(user.id, currPage, 0, key);

		render(page);
	}

	/**
	 * 信用积分明细(审核资料)
	 */
	public static void creditDetailAuditItem(String key, int currPage) {
		ErrorInfo error = new ErrorInfo();

		User user = User.currUser();

		PageBean<v_user_detail_credit_score_audit_items> page = User
				.queryCreditDetailAuditItem(user.id, currPage, 0, key, error);

		// if(error.code < 0){
		// renderJSON(error);
		// }

		render(page);
	}

	/**
	 * 信用积分明细(成功投标)
	 */
	public static void creditDetailInvest(String key, int currPage) {
		User user = User.currUser();

		PageBean<v_user_detail_credit_score_invest> page = User
				.queryCreditDetailInvest(user.id, currPage, 0, key);

		render(page);
	}

	/**
	 * 信用积分明细(正常还款)
	 * 
	 * @param key
	 */
	public static void creditDetailRepayment(String key, int currPage) {
		User user = User.currUser();

		PageBean<v_user_detail_credit_score_normal_repayment> page = User
				.queryCreditDetailRepayment(user.id, currPage, 0, key);

		render(page);
	}

	/**
	 * 信用积分明细(逾期扣分)
	 * 
	 * @param key
	 */
	public static void creditDetailOverdue(String key, int currPage) {
		User user = User.currUser();

		PageBean<v_user_detail_credit_score_overdue> page = User
				.queryCreditDetailOverdue(user.id, currPage, 0, key);

		render(page);
	}

	/**
	 * 查看信用等级规则
	 */
	public static void viewCreditRule() {
		ErrorInfo error = new ErrorInfo();
		List<v_credit_levels> CreditLevels = CreditLevel
				.queryCreditLevelList(error);

		render(CreditLevels);
	}

	/**
	 * 查看信用积分规则
	 */
	public static void creditintegral() {
		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();

		long auditItemCount = AuditItem.auditItemCount();

		ErrorInfo error = new ErrorInfo();

		String value = OptionKeys.getvalue(OptionKeys.CREDIT_LIMIT, error);
		double amountKey = StringUtils.isBlank(value) ? 0 : Double
				.parseDouble(value); // 积分对应额度

		render(backstageSet, auditItemCount, amountKey);
	}

	/**
	 * 查看科目积分规则
	 */
	public static void creditItem(String key, int currPage) {
		ErrorInfo error = new ErrorInfo();

		PageBean<t_dict_audit_items> page = AuditItem.queryEnableAuditItems(
				key, currPage, 0, error); // 审核资料

		String value = OptionKeys.getvalue(OptionKeys.CREDIT_LIMIT, error);
		double amountKey = StringUtils.isBlank(value) ? 0 : Double
				.parseDouble(value); // 积分对应额度

		render(page, amountKey);
	}

	/**
	 * 审核资料
	 */

	/**
	 * 审核资料积分明细（信用积分规则弹窗）
	 */
	public static void auditItemScore(String keyword, String currPage,
			String pageSize) {
		ErrorInfo error = new ErrorInfo();
		PageBean<AuditItem> page = AuditItem.queryAuditItems(currPage,
				pageSize, keyword, true, error);

		render(page, error);
	}

	// 申请超额借款
	public static void applyOverBorrow() {
		render();
	}

	// 提交申请
	public static void submitApply() {
		render();
	}

	/**
	 * 查看超额申请详情
	 */
	public static void viewOverBorrow(long overBorrowId) {
		ErrorInfo error = new ErrorInfo();
		List<v_user_audit_items> auditItems = OverBorrow.queryAuditItems(
				overBorrowId, error);
		t_user_over_borrows overBorrows = OverBorrow.queryOverBorrowById(
				overBorrowId, error);
		render(overBorrows, auditItems);
	}

	/**
	 * 提交资料
	 */
	public static void userAuditItem(long overBorrowId, long useritemId,
			long auditItemId, String filename) {

		ErrorInfo error = new ErrorInfo();

		UserAuditItem item = new UserAuditItem();
		item.lazy = true;
		item.userId = User.currUser().id;
		item.id = useritemId;
		item.auditItemId = auditItemId;
		item.imageFileName = filename;
		item.overBorrowId = overBorrowId;
		item.createUserAuditItem(error);

		JSONObject json = new JSONObject();

		json.put("error", error);
		renderJSON(json);
	}

	
	
	/**
	 * 充值
	 */
	public static void recharge() {
		ErrorInfo error = new ErrorInfo();

		User user = User.currUser();

		t_users users = User.queryUser2ByUserId(user.id, error);

		//if (users.reality_name == null || users.reality_name.length() == 0) {

		//	flash.error("为了资金安全,请先编写个人基本信息在充值！");
        //
		//	BasicInformation.basicInformation();
			
	    //	}
	 //	if(user.isMobileVerified){
			
	   //		flash.error("请先认证手机在充值");
			
	 //		BasicInformation.modifyMobile();
			
	 //	}
	 //	System.out.println(user.isMobileVerified);
		if (Constants.IPS_ENABLE) {
			List<Map<String, Object>> bankList = Payment.getBankList(error);

			render("@front.account.FundsManage.rechargeIps", user, bankList);
		}

		List<t_dict_payment_gateways> payType = User.gatewayForUse(error);

		render(user, payType);
	}

	/**
	 * 支付vip，资料审核等服务费
	 */
	public static void rechargePay() {
		ErrorInfo error = new ErrorInfo();
		User user = User.currUser();
		List<t_dict_payment_gateways> payType = User.gatewayForUse(error);

		Map<String, Object> map = (Map<String, Object>) Cache.get("rechargePay"
				+ user.id);
		double fee = (Double) map.get("fee");

		render(user, payType, fee);
	}

	/**
	 * 确认充值
	 */
	public static void submitRecharge(int type, double money, int bankType) {
		ErrorInfo error = new ErrorInfo();

		if (Constants.IPS_ENABLE) {
			String bankCode = params.get("bankCode");

			if (money <= 0) {
				flash.error("请输入正确的充值金额");
				recharge();
			}

			if (StringUtils.isBlank(bankCode) || bankCode.equals("0")) {
				flash.error("请选择充值银行");
				recharge();
			}

			Map<String, String> args = Payment
					.doDpTrade(money, bankCode, error);

			render("@front.account.PaymentAction.doDpTrade", args);
		}

		flash.put("type", type);
		flash.put("money", money);
		flash.put("bankType", bankType);

		if (bankType < 1 || bankType > 7) {
			flash.error("请选择正确的充值方式");
			recharge();
		}

		if (money == 0) {
			flash.error("请输入正确的充值金额");
			recharge();
		}

		BigDecimal moneyDecimal = new BigDecimal(money);

		if (moneyDecimal.compareTo(new BigDecimal("0.02")) < 0) {
			flash.error("请输入正确的充值金额");
			recharge();
		}

		if (bankType == 2) {
			Map<String, String> args = User.ipay(moneyDecimal, bankType, type,
					error);

			if (error.code < 0) {
				flash.error(error.msg);
				recharge();
			}

			render(args);
		}

		if (bankType == 1) {
			Map<String, String> args = User.gpay(moneyDecimal, bankType, type,
					error);

			if (error.code != 0) {
				flash.error(error.msg);
				recharge();
			}

			render("@front.account.FundsManage.submitRecharge2", args);
		}

		if (bankType == 3) {

			Map<String, String> args = User.cbpay(moneyDecimal, bankType, type,
					error);
			if (error.code != 0) {
				flash.error(error.msg);
				recharge();
			}

			render("@front.account.FundsManage.submitRecharge3", args);

		}

		if (bankType == 4) {

			Map<String, String> args = User.baopay(moneyDecimal, bankType,
					type, error);

			if (error.code != 0) {
				flash.error(error.msg);
				recharge();
			}

			render("@front.account.FundsManage.submitRecharge4", args);

		}

		if (bankType == 5) {

			Map<String, String> args = User.huipay(moneyDecimal, bankType,
					type, error);

			if (error.code != 0) {
				flash.error(error.msg);
				recharge();
			}

			render("@front.account.FundsManage.submitRecharge5", args);
		}

	}

	/**
	 * 确认支付
	 */
	public static void submitRechargePay(int type, int bankType, boolean isUse) {
		ErrorInfo error = new ErrorInfo();
		flash.put("type", type);
		flash.put("bankType", bankType);

		if (type < 1 || type > 2) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		User user = User.currUser();
		Map<String, Object> map = (Map<String, Object>) Cache.get("rechargePay"
				+ user.id);
		double fee = (Double) map.get("fee");
		int rechargeType = (Integer) map.get("rechargeType");
		double balance2 = user.balance2;
		double money = isUse ? (fee - balance2) : fee;

		if (money <= 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		BigDecimal moneyDecimal = new BigDecimal(money);

		if (moneyDecimal.compareTo(new BigDecimal("0.02")) < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		if (type == 2) {
			Map<String, String> args = User.ipay(moneyDecimal, bankType,
					rechargeType, error);

			if (error.code < 0) {
				render(Constants.ERROR_PAGE_PATH_FRONT);
			}

			render("@front.account.FundsManage.submitRecharge", args);
		}

		if (type == 1) {
			Map<String, String> args = User.gpay(moneyDecimal, bankType,
					rechargeType, error);

			if (error.code != 0) {
				flash.error(error.msg);
				rechargePay();
			}

			render("@front.account.FundsManage.submitRecharge2", args);
		}

	}

	// 网银在线回调

	public static void callCbpay(String v_oid, String v_pmode,
			String v_pstatus, String v_pstring, String v_amount,
			String v_moneytype, String v_md5str, String remark1, String remark2) {

		ErrorInfo error = new ErrorInfo();
		String info = "";

		String key = "duxingyao@751115*8006*9";

		String text = v_oid + v_pstatus + v_amount + v_moneytype + key;

		beartool.MD5 md5 = new beartool.MD5();

		String v_md5 = md5.getMD5ofStr(text).toUpperCase();

		if (v_md5str.equals(v_md5)) {

			User.recharge(v_oid, Double.parseDouble(v_amount), error);

		} else {

			info = "交易失败";
			render(info);

		}

		if (error.code < 0) {
			flash.error(error.msg);
			render(error);
		}

		info = "交易成功";
		render(info);

	}

	// 宝付回调PageUrl
	public static void callBfback(String MemberID, String TerminalID,
			String TransID, String Result, String ResultDesc, String FactMoney,
			String AdditionalInfo, String SuccTime, String Md5Sign) {

		// 商户号 = request.getParameter("MemberID");
		// 商户终端号 = request.getParameter("TerminalID");
		// 商户流水号 = request.getParameter("TransID");
		// 支付结果 = request.getParameter("Result");
		// 支付结果描述 = request.getParameter("ResultDesc");
		// 实际成功金额 = request.getParameter("FactMoney");
		// 订单附加消息 = request.getParameter("AdditionalInfo");
		// 支付完成时间 = request.getParameter("SuccTime");
		// MD5签名 = request.getParameter("Md5Sign");

		ErrorInfo error = new ErrorInfo();
		String info = "";
		String Md5key = "8qwnf558b36uucdr";
		String MARK = "~|~";
		String md5 = "MemberID=" + MemberID + MARK + "TerminalID=" + TerminalID
				+ MARK + "TransID=" + TransID + MARK + "Result=" + Result
				+ MARK + "ResultDesc=" + ResultDesc + MARK + "FactMoney="
				+ FactMoney + MARK + "AdditionalInfo=" + AdditionalInfo + MARK
				+ "SuccTime=" + SuccTime + MARK + "Md5Sign=" + Md5key;

		User vality = new User();

		String WaitSign = vality.getMD5ofStr(md5);

		if (WaitSign.compareTo(Md5Sign) == 0) {
			info = "交易成功！";
		} else {
			info = "交易失败";

		}

		if (error.code < 0) {
			flash.error(error.msg);
			render(error);
		}

		render(info);

	}

	public static void callHcback(String BillNo, String Amount,
			String tradeOrder, String Succeed, String Result, String SignMD5info) {

		ErrorInfo error = new ErrorInfo();
		String info = "";
		String MD5key = "HidGbB]o"; // MD5key值

		controllers.front.account.MD5 md5 = new controllers.front.account.MD5();

		String md5src = BillNo + "&" + Amount + "&" + Succeed + "&" + MD5key;
		String md5sign; // MD5加密后的字符串
		md5sign = md5.getMD5ofStr(md5src);
		if (SignMD5info.equals(md5sign)) {
			info = "交易成功";

		} else {
			info = "交易失败";

		}
		render(info);

	}

	/**
	 * 环迅回调
	 */
	public static void callback(String billno, String mercode,
			String Currency_type, String amount, String date, String succ,
			String msg, String attach, String ipsbillno, String retencodetype,
			String signature) {
		ErrorInfo error = new ErrorInfo();

		// 返回订单加密的明文:billno+【订单编号】+currencytype+【币种】+amount+【订单金额】+date+【订单日期】+succ+【成功标志】+ipsbillno+【IPS订单编号】+retencodetype
		// +【交易返回签名方式】+【商户内部证书】
		String content = "billno" + billno + "currencytype" + Currency_type
				+ "amount" + amount + "date" + date + "succ" + succ
				+ "ipsbillno" + ipsbillno + "retencodetype" + retencodetype; // 明文：订单编号+订单金额+订单日期+成功标志+IPS订单编号+币种

		boolean verify = false;

		// 验证方式：16-md5withRSA 17-md5
		if (retencodetype.equals("16")) {
			cryptix.jce.provider.MD5WithRSA a = new cryptix.jce.provider.MD5WithRSA();
			a.verifysignature(content, signature, "D:\\software\\publickey.txt");

			// Md5withRSA验证返回代码含义
			// -99 未处理
			// -1 公钥路径错
			// -2 公钥路径为空
			// -3 读取公钥失败
			// -4 验证失败，格式错误
			// 1： 验证失败
			// 0: 成功
			if (a.getresult() == 0) {
				verify = true;
			}
		} else if (retencodetype.equals("17")) {
			User.validSign(content, signature, error);

			if (error.code == 0) {
				verify = true;
			}
		}
		String info = "";
		if (!verify) {
			info = "验证失败";
			render(info);
		}

		if (succ == null) {
			info = "交易失败";
			render(info);
		}

		if (!succ.equalsIgnoreCase("Y")) {
			info = "交易失败";
			render(info);
		}

		User.recharge(billno, Double.parseDouble(amount), error);

		if (Constants.IPS_ENABLE) {
			if (error.code < 0) {
				flash.error(error.msg);
				render(Constants.ERROR_PAGE_PATH_FRONT);
			}

			int rechargeType = Convert.strToInt(billno.split("type")[0],
					RechargeType.Normal);

			if (rechargeType == RechargeType.VIP) {
				User user = User.currUser();
				Map<String, Object> map = (Map<String, Object>) Cache
						.get("rechargePay" + user.id);
				int serviceTime = (Integer) map.get("serviceTime");
				Vip vip = new Vip();
				vip.serviceTime = serviceTime;
				vip.renewal(user, error);

				if (error.code < 0) {
					flash.error(error.msg);
				} else {
					flash.success("支付vip费用成功");
				}

				Cache.delete("rechargePay" + user.id);

				AccountHome.home();
			}

			if (rechargeType == RechargeType.ItemAudit) {
				User user = User.currUser();
				Map<String, Object> map = (Map<String, Object>) Cache
						.get("rechargePay" + user.id);
				long itemId = (Long) map.get("itemId");
				String items = (String) map.get("items");
				long userItemId = (Long) map.get("userItemId");

				UserAuditItem item = new UserAuditItem();
				item.lazy = true;
				item.userId = User.currUser().id;
				item.id = userItemId;
				item.auditItemId = itemId;
				item.imageFileNames = items;
				item.createUserAuditItem(error);

				if (error.code < 0) {
					flash.error(error.msg);
				} else {
					flash.success("支付资料审核费成功");
				}

				Cache.delete("rechargePay" + user.id);

				AccountHome.auditMaterials(null, null, null, null, null, null,
						null);
			}
		}

		if (error.code < 0) {
			flash.error(error.msg);
			render(error);
		}

		info = "交易成功";
		render(info);
	}

	/**
	 * 国付宝回调
	 */
	public static void gCallback(String version, String charset,
			String language, String signType, String tranCode,
			String merchantID, String merOrderNum, String tranAmt,
			String feeAmt, String frontMerUrl, String backgroundMerUrl,
			String tranDateTime, String tranIP, String respCode, String msgExt,
			String orderId, String gopayOutOrderId, String bankCode,
			String tranFinishTime, String merRemark1, String merRemark2,
			String signValue) {
		ErrorInfo error = new ErrorInfo();
		String info = "";

		t_dict_payment_gateways gateway = User.gateway(Constants.GO_GATEWAY,
				error);

		if (GopayUtils.validateSign(version, tranCode, merchantID, merOrderNum,
				tranAmt, feeAmt, tranDateTime, frontMerUrl, backgroundMerUrl,
				orderId, gopayOutOrderId, tranIP, respCode, gateway._key,
				signValue)) {

			info = "验证失败，支付失败！";
			render(info);
		}

		Logger.info("respCode:" + respCode);

		if (!"0000".equals(respCode) && !"9999".equals(respCode)) {
			info = "支付失败！";
			render(info);
		}

		if ("9999".equals(respCode)) {
			info = "订单处理中，请耐心等待！";
			// render(info);
		}

		User.recharge(merOrderNum, Double.parseDouble(tranAmt), error);

		if (Constants.IPS_ENABLE) {
			if (error.code < 0) {
				flash.error(error.msg);
				render(Constants.ERROR_PAGE_PATH_FRONT);
			}

			int rechargeType = Convert.strToInt(merOrderNum.split("type")[0],
					RechargeType.Normal);

			if (rechargeType == RechargeType.VIP) {
				User user = User.currUser();
				Map<String, Object> map = (Map<String, Object>) Cache
						.get("rechargePay" + user.id);
				int serviceTime = (Integer) map.get("serviceTime");
				Vip vip = new Vip();
				vip.serviceTime = serviceTime;
				vip.renewal(user, error);

				if (error.code < 0) {
					flash.error(error.msg);
				} else {
					flash.success("支付vip费用成功");
				}

				Cache.delete("rechargePay" + user.id);

				AccountHome.home();
			}

			if (rechargeType == RechargeType.ItemAudit) {
				User user = User.currUser();
				Map<String, Object> map = (Map<String, Object>) Cache
						.get("rechargePay" + user.id);
				long itemId = (Long) map.get("itemId");
				String items = (String) map.get("items");
				long userItemId = (Long) map.get("userItemId");

				UserAuditItem item = new UserAuditItem();
				item.lazy = true;
				item.userId = User.currUser().id;
				item.id = userItemId;
				item.auditItemId = itemId;
				item.imageFileNames = items;
				item.createUserAuditItem(error);

				if (error.code < 0) {
					flash.error(error.msg);
				} else {
					flash.success("支付资料审核费成功");
				}

				Cache.delete("rechargePay" + user.id);

				AccountHome.auditMaterials(null, null, null, null, null, null,
						null);
			}
		}

		if (error.code < 0) {
			flash.error(error.msg);
			render(error);
		}

		info = "交易成功";
		render(info);
	}

	/**
	 * 提现
	 */
	public static void withdrawal() {
		User user = new User();
		user.id = User.currUser().id;
		ErrorInfo error = new ErrorInfo();
		t_users users = User.queryUser2ByUserId(user.id, error);
		String type = params.get("type");
		String currPage = params.get("currPage");
		String pageSize = params.get("pageSize");
		String beginTime = params.get("startDate");
		String endTime = params.get("endDate");

		double amount = User.queryRechargeIn(user.id, error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		double withdrawalAmount = user.balance - amount;

		if (withdrawalAmount < 0) {
			withdrawalAmount = 0;
		}

		List<UserBankAccounts> banks = UserBankAccounts
				.queryUserAllBankAccount(user.id);

		PageBean<v_user_withdrawals> page = User.queryWithdrawalRecord(user.id,
				type, beginTime, endTime, currPage, pageSize, error);
		boolean ipsEnable = Constants.IPS_ENABLE;

		render(user, users, withdrawalAmount, banks, page, ipsEnable);
	}

	/**
	 * 根据选择的银行卡id查询其信息
	 */
	public static void QueryBankInfo(long id) {
		JSONObject json = new JSONObject();

		UserBankAccounts bank = new UserBankAccounts();
		bank.setId(id);

		json.put("bank", bank);

		renderJSON(json);
	}

	// /**
	// * 提现记录
	// */
	// public static void withdrawalRecord() {
	// User user = User.currUser();
	//
	// String type = params.get("type");
	// String currPage = params.get("currPage");
	// String pageSize = params.get("pageSize");
	// String beginTime = params.get("startDate");
	// String endTime = params.get("endDate");
	//
	// ErrorInfo error = new ErrorInfo();
	// PageBean<v_user_withdrawals> page = User.queryWithdrawalRecord(user.id,
	// type,
	// beginTime, endTime, currPage, pageSize, error);
	//
	// render(page);
	// }

	// 申请提现
	public static void applyWithdrawal() {
		render();
	}

	/**
	 * 确认提现
	 */
	public static void submitWithdrawal(double amount, long bankId,
			String payPassword, int type, String ipsSelect) {
		ErrorInfo error = new ErrorInfo();
		boolean flag = false;

		if (StringUtils.isNotBlank(ipsSelect) && ipsSelect.equals("1")) {
			flag = true;
		}
		if (amount <= 0) {
			flash.error("请输入提现金额");

			withdrawal();
		}

		if (!(Constants.IPS_ENABLE && flag)) {
			if (StringUtils.isBlank(payPassword)) {
				flash.error("请输入交易密码");

				withdrawal();
			}

			if (type != 1 && type != 2) {
				flash.error("传入参数有误");

				withdrawal();
			}

			if (bankId <= 0) {
				flash.error("请选择提现银行");

				withdrawal();
			}
		}

		User user = new User();
		user.id = User.currUser().id;

		long withdrawalId = user.withdrawal(amount, bankId, payPassword, type,
				flag, error);

		if (Constants.IPS_ENABLE && flag) {
			if (error.code < 0) {
				flash.error(error.msg);

				withdrawal();
			}

			Map<String, String> args = Payment.doDwTrade(withdrawalId, amount,
					error);

			if (error.code < 0) {
				flash.error(error.msg);

				withdrawal();
			}

			render("@front.account.PaymentAction.doDwTrade", args);
		}

		flash.error(error.msg);

		withdrawal();
	}

	// 转账
	public static void transfer() {
		render();
	}

	// 确认转账
	public static void submitTransfer() {
		render();
	}

	/**
	 * 交易记录
	 */
	public static void dealRecord(int type, Date beginTime, Date endTime,
			int currPage, int pageSize) {

		User user = User.currUser();
		PageBean<v_user_details> page = User.queryUserDetails(user.id, type,
				beginTime, endTime, currPage, pageSize);

		render(page);
	}

	// 交易详情
	public static void dealDetails() {
		render();
	}

	/**
	 * 导出交易记录
	 */
	public static void exportDealRecords() {
		ErrorInfo error = new ErrorInfo();

		List<v_user_details> details = User.queryAllDetails(error);

		if (error.code < 0) {
			renderText("下载数据失败");
		}

		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.registerJsonValueProcessor(Date.class,
				new JsonDateValueProcessor("yyyy-MM-dd"));
		JSONArray arrDetails = JSONArray.fromObject(details, jsonConfig);

		for (Object obj : arrDetails) {
			JSONObject detail = (JSONObject) obj;
			int type = detail.getInt("type");
			double amount = detail.getDouble("amount");

			switch (type) {
			case 1:
				detail.put("inAmount", amount);
				detail.put("outAmount", "");
				break;
			case 2:
				detail.put("inAmount", "");
				detail.put("outAmount", amount);
				break;
			default:
				detail.put("inAmount", "");
				detail.put("outAmount", "");
				break;
			}
		}

		File file = ExcelUtils.export("交易记录", arrDetails, new String[] { "时间",
				"收入", "支出", "账户总额", "可用余额", "冻结金额", "科目", "明细" }, new String[] {
				"time", "inAmount", "outAmount", "user_balance", "balance",
				"freeze", "name", "summary" });

		renderBinary(file, "交易记录.xls");
	}

	/**
	 * 资金托管测试页面
	 */
	public static void payment() {
		render();
	}

	/**
	 * 我的支付账号
	 */
	public static void payAccount() {
		render();
	}

	/**
	 * 红包页面
	 * ***/
	public static void redPage() {

		User user = User.currUser();
		if (user == null) {

			RedAction.redJump();

		}
		List<t_users> rlu = null;
		String sql2 = "id=? and is_mobile_verified=?";
		rlu = GenericModel.find(sql2, user.id, true).fetch();
		if (rlu.isEmpty()) {
			flash.error("请先认证手机");
			BasicInformation.modifyMobile();
		}
		// 检查红包有没有超时
		ErrorInfo error = new ErrorInfo();
		RedAction redAction = new RedAction();
		redAction.redTimeout(user.id, error);

		int tag = 0 ;
		int haha = 0;
		List<t_reds_use> ee = RedAction.redHistory();
        if(ee==null){
        	
         tag =1;
        }
		List<t_reds> ff = RedAction.redByRt();
		
		if(ff==null){
			
			tag=2;
		}
		
		List<t_roma_invests> sansan = RedAction.redInvest();
		
		if(sansan==null||sansan.isEmpty()){
			
			haha=1;
		}
		
		
		render(ee, ff,tag,haha,sansan);
	}

}