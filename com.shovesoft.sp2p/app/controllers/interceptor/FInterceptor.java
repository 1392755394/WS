package controllers.interceptor;

import business.User;
import controllers.BaseController;
import controllers.front.account.LoginAndRegisterAction;
import play.mvc.Before;

public class FInterceptor extends BaseController{
	
	@Before(unless={"front.account.FundsManage.gCallback",
			"front.account.FundsManage.callback"
	})
	public static void checkLogin(){
		//try{
			//License.update(BackstageSet.getCurrentBackstageSet().registerCode);
			//if(!(License.getDomainNameAllow() && License.getWebPagesAllow())) {
			//	flash.put("error", "此版本非正版授权，请联系晓风软件购买正版授权！");
			//	LoginAndRegisterAction.login();
		//	}
		//}catch (Exception e) {
		//	e.printStackTrace();
		//	Logger.info("进行正版校验时：" + e.getMessage());
		//	flash.put("error", "此版本非正版授权，请联系晓风软件购买正版授权！");
		//	LoginAndRegisterAction.login();
		//}
		
		User user = User.currUser();
		
		if(User.currUser() == null){
			LoginAndRegisterAction.login();
		}
		
		renderArgs.put("user", user);
	}
	
	/**
	 * 模拟登录拦截
	 */
	@Before (only = {
			"front.account.AccountHome.uploadPhoto",
			"front.account.AccountHome.applyForOverBorrow",
			"front.account.AccountHome.vipApply",
			"front.account.AccountHome.setNoteName",
			"front.account.AccountHome.sendMessage",
			"front.account.Message.deleteSystemMsgs",
			"front.account.Message.markMsgsReaded",
			"front.account.Message.markMsgsUnread",
			"front.account.Message.deleteInboxMsgs",
			"front.account.Message.deleteOutboxMsgs",
			"front.account.Message.replyMsg",
			"front.account.Message.createAnswers",
			"front.account.Message.deleteBidQuestion",
			"front.account.AccountHome.attentionUser",
			"front.account.AccountHome.cancelAttentionUser",
			"front.account.AccountHome.vipMoney",
			"front.account.AccountHome.vipApply",
			"front.account.AccountHome.submitRepayment",
			"front.account.AccountHome.repealLoaningBid",
			"front.account.AccountHome.repealAuditingBid",
			"front.account.AccountHome.deleteAuditItem",
			"front.account.AccountHome.createUserAuditItem",
			"front.account.BasicInformation.saveInformation",
			"front.account.BasicInformation.verifySafeQuestion",
			"front.account.BasicInformation.saveSafeQuestion",
			"front.account.BasicInformation.resetSafeQuestion",
			"front.account.BasicInformation.saveSafeQuestionByEmail",
			"front.bid.bidAction.activeEmail",
			"front.account.BasicInformation.saveEmail",
			"front.account.BasicInformation.savePassword",
			"front.account.BasicInformation.editPayPassword",
			"front.account.BasicInformation.editPayPassword",
			"front.account.BasicInformation.savePayPassword",
			"front.account.BasicInformation.resetPayPassword",
			"front.account.BasicInformation.saveMobile",
			"front.account.BasicInformation.bindMobile",
			"front.account.FundsManage.addBank",
			"front.account.FundsManage.editBank",
			"front.account.FundsManage.deleteBank",
			"front.account.FundsManage.userAuditItem",
			"front.account.FundsManage.submitWithdrawal",
			"front.account.FundsManage.exportDealRecords",
			"front.account.InvestAccount.increaseAuction",
			"front.account.InvestAccount.transact",
			"front.account.InvestAccount.acceptDebts",
			"front.account.InvestAccount.notAccept",
			"front.account.InvestAccount.addBlack",
			"front.account.InvestAccount.removeBlacklist",
			"front.account.InvestAccount.closeRobot",
			"front.account.InvestAccount.saveOrUpdateRobot",
			"front.account.LoginAndRegisterAction.saveUsernameByTele",
			"front.account.LoginAndRegisterAction.savePasswordByMobile",
			"front.account.LoginAndRegisterAction.sendResetEmail",
			"front.account.LoginAndRegisterAction.savePasswordByEmail",
			"front.bid.bidAction.createBid",
			"front.bid.bidAction.saveInformation",
			"front.debt.debtAction.confirmTransfer",
			"front.debt.debtAction.auction",
			"front.debt.debtAction.reportUser",
			"front.help.HelpCenterAction.support",
			"front.help.HelpCenterAction.opposition ",
			"front.invest.investAction.confirmInvest",
			"front.invest.investAction.confirmInvestBottom",
			"front.invest.investAction.collectBid",
			"front.account.Message.sendMsg",
			"Application.dlImages"})
	public static void simulateLogin(String encryString) {
        User user = User.currUser();
        
        if(user.simulateLogin != null){
        	if(user.simulateLogin.equalsIgnoreCase(User.encrypt())){
            	//flash.error("模拟登录不能进行该操作");
            	String url = request.headers.get("referer").value();
            	redirect(url);
            }else{
            	//flash.error("模拟登录超时，请重新操作");
            	String url = request.headers.get("referer").value();
            	redirect(url);
            }
        }
	}
}
