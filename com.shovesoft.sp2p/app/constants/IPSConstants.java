package constants;

import com.shove.security.Encrypt;

import play.Play;

public class IPSConstants {
	public static final String MER_CODE = "808801";						//平台商户号
	public static final String MER_NAME = "环迅";						//平台商户名
	public static final String MER_IDENT_NO = "410621198406155011";		//平台证件号码
	
	public static final String PLATFORM = "2";		//所在资金托管的平台id

	public static final String ACTION = Play.configuration.getProperty("pay.action");	//资金托管url
	public static final String CALLBACK_URL = Play.configuration.getProperty("pay.callback.url");		//回调
	public static final String DOMAIN = Encrypt.encrypt3DES(Play.configuration.getProperty("pay.domain"), Constants.ENCRYPTION_KEY);//p2p平台域名
	
	public static final String CACHE_TIME = "1h";	//标，投资等信息缓存时间
	
	public static final String INDENT_TYPE = "1";		//证件类型：1#身份证
	public static final String ACCT_TYPE = "1";		//账户类型：1#个人
	public static final String CHANNEL_TYPE = "1";	//充值渠道类型：1#网银充值
	public static final String IPS_FEE_TYPE = "1";	//谁付ips手续费：1#平台支付，2#用户支付
	public static final String VALID_TYPE = "N";	//自动还款有限期类型：N#长期有效
	public static final String VALID_DATE = "0";	//自动还款有效期：0
	public static final String OUT_TYPE = "1";	//提现模式：1#普通提现，2#定向提现<暂不开放>
	public static final String DAY_CYCLE_TYPE = "1";	//借款周期类型：1#天，3#月
	public static final String MONTH_CYCLE_TYPE = "3";	//借款周期类型：1#天，3#月
	public static final String PAID_MONTH_EQUAL_PRINCIPAL_INTEREST = "1"; // 按月还款、等额本息
	public static final String PAID_MONTH_ONCE_REPAYMENT = "2"; // 按月付息、一次还款
	public static final String ONCE_REPAYMENT = "3"; // 等额本金
	public static final String OTHER_REPAYMENT = "99"; // 还款方式：其他
	public static final String OPERATION_TYPE_1 = "1"; // 标的操作类型：1#新增，2#结束
	public static final String OPERATION_TYPE_2 = "2"; // 标的操作类型：1#新增，2#结束
	public static final String REPAYMENT_TYPE = "1"; // 还款类型：1#手动还款，2#自动还款
	public static final String BID_FLOW = "flow"; // 流标标示
	
	public static final String BID_CREATE = "create"; //发标
	public static final String BID_CANCEL = "cancel"; //审核中->撤销
	public static final String BID_CANCEL_B = "cancelB"; //提前借款->借款中不通过
	public static final String BID_CANCEL_S = "cancelS"; //审核中->审核不通过
	public static final String BID_CANCEL_I = "cancelI"; //筹款中->借款中不通过
	public static final String BID_CANCEL_M = "cancelM"; //满标->放款不通过
	public static final String BID_CANCEL_F = "cancelF"; //提前借款->撤销
	public static final String BID_CANCEL_N = "cancelN"; //筹款中->撤销
	public static final String BID_ADVANCE_LOAN = "flowA"; //提前借款->流标
	public static final String BID_FUNDRAISE = "flowI"; //筹款中->流标
	
	public static double MIN_AMOUNT = 1.0;						//标的借款额度限额最小值
	
	public class IPSOperation {
		public static final int CREATE_IPS_ACCT = 1;			//开户
		public static final int REGISTER_SUBJECT = 2;			//标的登记
		public static final int REGISTER_CREDITOR = 3;			//登记债权人接口
		public static final int REGISTER_CRETANSFER = 5;		//登记债权转让接口
		public static final int AUTO_NEW_SIGNING = 6;			//自动投标签约
		public static final int REPAYMENT_SIGNING = 7;			//自动还款签约
		public static final int DO_DP_TRADE = 8;				//充值
		public static final int TRANSFER = 9;					//转账(WS)
		public static final int REPAYMENT_NEW_TRADE = 10;		//还款
		public static final int GUARANTEE_UNFREEZE = 11;		//解冻保证金
		public static final int DO_DW_TRADE = 13;				//提现
		public static final int QUERY_FOR_ACC_BALANCE = 14;		//账户余额查询(WS)
		public static final int GET_BANK_LIST = 15;				//获取银行列表查询(WS)
		public static final int QUERY_MER_USER_INFO = 16;		//账户信息查询(WS)
		public static final int QUERY_TRADE = 17;				//交易查询(WS)
		
		public static final int TRANSFER_ONE = 91;				//转账-1放款(WS)
		public static final int TRANSFER_FOUR = 94;				//转账-4债权转让(WS)
	}
	
	public static class IPSWebUrl {
		public static final String CREATE_IPS_ACCT = CALLBACK_URL + "createAcctCB";					//开户
		public static final String REGISTER_SUBJECT = CALLBACK_URL + "registerSubjectCB";			//标的登记
		public static final String REGISTER_CREDITOR = CALLBACK_URL + "registerCreditorCB";			//登记债权人接口
		public static final String REGISTER_CRETANSFER = CALLBACK_URL + "registerCretansferCB";		//登记债权转让接口
		public static final String AUTO_NEW_SIGNING = CALLBACK_URL + "autoNewSigningCB";			//自动投标签约
		public static final String REPAYMENT_SIGNING = CALLBACK_URL + "repaymentSigningCB";			//自动还款签约
		public static final String DO_DP_TRADE = CALLBACK_URL + "doDpTradeCB";						//充值
		public static final String TRANSFER = CALLBACK_URL + "transferCB";							//转账(WS)
		public static final String REPAYMENT_NEW_TRADE = CALLBACK_URL + "repaymentNewTradeCB";		//还款
		public static final String GUARANTEE_UNFREEZE = CALLBACK_URL + "guaranteeUnfreezeCB";		//解冻保证金
		public static final String DO_DW_TRADE = CALLBACK_URL + "doDwTradeCB";						//提现
		public static final String QUERY_FOR_ACC_BALANCE = CALLBACK_URL + "queryForAccBalanceCB";	//账户余额查询(WS)
		public static final String GET_BANK_LIST = CALLBACK_URL + "getBankListCB";					//获取银行列表查询(WS)
		public static final String QUERY_MER_USER_INFO = CALLBACK_URL + "queryMerUserInfoCB";		//账户信息查询(WS)
	}
	
	public static class IPSS2SUrl {
		public static final String CREATE_IPS_ACCT = CALLBACK_URL + "createAcctCB";					//开户
		public static final String REGISTER_SUBJECT = CALLBACK_URL + "registerSubjectCB";			//标的登记
		public static final String REGISTER_CREDITOR = CALLBACK_URL + "registerCreditorCB";			//登记债权人接口
		public static final String REGISTER_CRETANSFER = CALLBACK_URL + "registerCretansferCB";		//登记债权转让接口
		public static final String AUTO_NEW_SIGNING = CALLBACK_URL + "autoNewSigningCB";			//自动投标签约
		public static final String REPAYMENT_SIGNING = CALLBACK_URL + "repaymentSigningCB";			//自动还款签约
		public static final String DO_DP_TRADE = CALLBACK_URL + "doDpTradeCB";						//充值
		public static final String TRANSFER = CALLBACK_URL + "transferCB";							//转账(WS)
		public static final String REPAYMENT_NEW_TRADE = CALLBACK_URL + "repaymentNewTradeCB";		//还款
		public static final String GUARANTEE_UNFREEZE = CALLBACK_URL + "guaranteeUnfreezeCB";		//解冻保证金
		public static final String DO_DW_TRADE = CALLBACK_URL + "doDwTradeCB";						//提现
		public static final String QUERY_FOR_ACC_BALANCE = CALLBACK_URL + "queryForAccBalanceCB";	//账户余额查询(WS)
		public static final String GET_BANK_LIST = CALLBACK_URL + "getBankListCB";					//获取银行列表查询(WS)
		public static final String QUERY_MER_USER_INFO = CALLBACK_URL + "queryMerUserInfoCB";		//账户信息查询(WS)
	}
	
	public class IpsCheckStatus {
		public static final int NONE = 0;
		public static final int EMAIL = 1;
		public static final int REAL_NAME = 2;
		public static final int MOBILE = 3;
		public static final int IPS = 4;
	}
	
	public class TransferType {
		public static final int INVEST = 1;			//投资(放款)
		public static final int CRETRANSFER = 4;	//债权转让
	}
	
	public class Status {
		public static final int SUCCESS = 1;		//成功
		public static final int FAIL = 2;			//失败
		public static final int HANDLING = 3;		//处理中
		public static final int NONE = 4;			//未查询到交易
	}
	
	public static boolean IS_REPAIR_TEST = false; 
}
