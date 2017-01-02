package controllers.interceptor;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import business.BackstageSet;
import business.Supervisor;
import constants.Constants;
import constants.OptionKeys;
import controllers.BaseController;
import controllers.supervisor.account.AccountAction;
import controllers.supervisor.login.LoginAction;
import play.cache.Cache;
import play.mvc.Before;
import utils.DateUtil;
import utils.ErrorInfo;

/**
 * 后台拦截器
 * @author lzp
 * @version 6.0
 * @created 2014-6-5
 */
public class SupervisorInterceptor extends BaseController{
	
	/**
	 * 对后台每一个操作都进行云盾状态的验证
	 */
//	@Before
	public static void checkUkey() {
		String path = request.path;
		String flag = (String)Cache.get("yunflag");
		
		if(flag == null){
			LoginAction.checkUkeyInIt(path);
		}
		
		if(flag.equalsIgnoreCase(Constants.CLOUD_SHIELD_NOT_EXIST)){
			flash.error("未插入安全云盾，不能进行操作");
			Cache.delete("yunflag");
			LoginAction.toBlank();
			return;
		}
		
		if(flag.equalsIgnoreCase(Constants.CLOUD_SHIELD_SUPERVISOR)){
			flash.error("插入的安全云盾不属于当前管理员");
			Cache.delete("yunflag");
			LoginAction.toBlank();
			return;
		}
		
		Cache.delete("yunflag");
	}
	
	/**
	 * 登录拦截
	 */
	@Before(unless = {"supervisor.login.LoginAction.login",
			"supervisor.managementHome.HomeAction.showHome",
			"supervisor.systemSettings.SoftwareLicensAction.notRegister",
			"supervisor.systemSettings.SoftwareLicensAction.saveSoftwareLicens",
			"supervisor.financeManager.PlatformAccountManager.ipsOffSingleDeal"
			})
	public static void checkLogin() {
		//try{
		//	License.update(BackstageSet.getCurrentBackstageSet().registerCode);
		//	if(!(License.getDomainNameAllow() && License.getAdminPagesAllow())) {
		//		flash.put("error", "此版本非正版授权，请联系晓风软件购买正版授权！");
		//		SoftwareLicensAction.notRegister();
		//	}
		//}catch (Exception e) {
		//	e.printStackTrace();
		//	Logger.info("进行正版校验时：" + e.getMessage());
		//	flash.put("error", "此版本非正版授权，请联系晓风软件购买正版授权！");
		//	SoftwareLicensAction.notRegister();
		//}
		
		if (Supervisor.isLogin()) {
			return;
		}
		
		LoginAction.loginInit();
	}
	
	/**
	 * 管理员对象放入renderArgs里边
	 */
	@Before
	public static void putSupervisor() {
		if (!Supervisor.isLogin()) {
			return;
		}
		
		renderArgs.put("supervisor", Supervisor.currSupervisor());
		renderArgs.put("systemOptions", BackstageSet.getCurrentBackstageSet());
	}
	
	/**
	 * 权限拦截
	 */
	@Before(unless = {
				"supervisor.account.AccountAction.home", 
				"supervisor.account.AccountAction.editSupervisor",
				"supervisor.financeManager.PlatformAccountManager.ipsOffSingleDeal"
			})
	public static void checkRight() {
		String action = request.action;
		
		Supervisor currSupervisor = Supervisor.currSupervisor();
		
		if (null == currSupervisor) {
			LoginAction.loginInit();
			
			return;
		}
		
		if (!currSupervisor.haveRight(action)) {
			renderTemplate("Application/insufficientRight.html");
		}
	}
	
	/**
	 * 未设密码拦截
	 */
	@Before(unless = {
				"supervisor.account.AccountAction.home", 
				"supervisor.account.AccountAction.editSupervisor",
				"supervisor.systemSettings.SoftwareLicensAction.notRegister",
				"supervisor.systemSettings.SoftwareLicensAction.saveSoftwareLicens",
				"supervisor.financeManager.PlatformAccountManager.ipsOffSingleDeal"
			})
	public static void goAccountHome() {
		Supervisor supervisor = Supervisor.currSupervisor();
		
		if (StringUtils.isBlank(supervisor.password)) {
			OptionKeys.siteValue(OptionKeys.PLATFORM_STARTUP_TIME, DateUtil.dateToString(new Date()), new ErrorInfo());
			
			AccountAction.home();
		}
	}
	
}
