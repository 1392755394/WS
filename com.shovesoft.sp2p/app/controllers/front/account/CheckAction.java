package controllers.front.account;

import org.apache.commons.lang.StringUtils;

import play.mvc.Before;

import business.TemplateEmail;
import business.User;
import utils.EmailUtil;
import utils.ErrorInfo;
import utils.RegexUtils;
import utils.SMSUtil;
import constants.Constants;
import constants.IPSConstants.IpsCheckStatus;
import controllers.interceptor.FInterceptor;

public class CheckAction extends FInterceptor {
	@Before(only = {"front.account.FundsManage.recharge",
			"front.account.InvestAccount.auditmaticInvest",
			"front.account.FundsManage.withdrawal"
			})
	public static void checkIpsAcct(){
		if(Constants.IPS_ENABLE && (User.currUser().getIpsStatus() != IpsCheckStatus.IPS)){
			CheckAction.approve();
		}
	}
	
	/**
	 * ips认证
	 */
	public static void approve() {
		render();
	}
	
	/**
	 * ips认证(弹框)
	 */
	public static void check() {
		int status = User.currUser().getIpsStatus();
		
		switch (status) {
		case IpsCheckStatus.NONE:
			checkEmail();
			break;
		case IpsCheckStatus.EMAIL:
			checkEmailSuccess();
			break;
		case IpsCheckStatus.REAL_NAME:
			checkMobile();
			break;
		case IpsCheckStatus.MOBILE:
			createIpsAcct();
			break;
		case IpsCheckStatus.IPS:
			checkSuccess();
			break;
		default:
			break;
		}
	}
	
	/**
	 * 邮箱认证
	 */
	public static void checkEmail() {
		if (User.currUser().getIpsStatus() != IpsCheckStatus.NONE) {
			check();
		}
		
		ErrorInfo error = new ErrorInfo();
		User user = User.currUser();
		TemplateEmail.activeEmail(user, error);
		String email = user.email;
		String emailUrl = EmailUtil.emailUrl(email);
		
		render(email, emailUrl);
	}
	
	/**
	 * 邮箱认证成功
	 */
	public static void checkEmailSuccess() {
		if (User.currUser().getIpsStatus() != IpsCheckStatus.EMAIL) {
			check();
		}
		
		render();
	}
	
	/**
	 * 实名认证页面
	 */
	public static void checkRealName() {
		if (User.currUser().getIpsStatus() != IpsCheckStatus.EMAIL) {
			check();
		}
		
		render();
	}
	
	/**
	 * 实名认证
	 */
	public static void doCheckRealName(String realName, String idNumber) {
		User user = User.currUser();
		if (user.getIpsStatus() != IpsCheckStatus.EMAIL) {
			check();
		}
		
		flash.put("realName", realName);
		flash.put("idNumber", idNumber);
		
		if (StringUtils.isBlank(realName)) {
			flash.error("真实姓名不能为空");
			
			checkRealName();
		}
		
		if (StringUtils.isBlank(idNumber)) {
			flash.error("身份证不能为空");
			
			checkRealName();
		}
		
		ErrorInfo error = new ErrorInfo();
		user.checkRealName(realName, idNumber, error);
		
		if (error.code < 0) {
			flash.error(error.msg);
			
			checkRealName();
		}
		
		checkMobile();
	}
	
	/**
	 * 手机认证页面
	 */
	public static void checkMobile() {
		if (User.currUser().getIpsStatus() != IpsCheckStatus.REAL_NAME) {
			check();
		}
		
		render();
	}
	
	/**
	 * 发送短信验证码
	 * @param mobile
	 */
	public static void sendCode(String mobile) {
		ErrorInfo error = new ErrorInfo();
		flash.put("mobile", mobile);
		
		if(StringUtils.isBlank(mobile) ) {
			flash.error("手机号码不能为空");
		}
		
		if(!RegexUtils.isMobileNum(mobile)) {
			flash.error("请输入正确的手机号码");
		}
		
		SMSUtil.sendCode(mobile, error);
		
		if (error.code < 0) {
			flash.error(error.msg);
		}
		
		flash.put("isSending", true);
		
		checkMobile();
	}
	
	/**
	 * 手机认证
	 * @param mobile
	 * @param code
	 */
	public static void doCheckMobile(String mobile, String code) {
		User user = User.currUser();
		if (user.getIpsStatus() != IpsCheckStatus.REAL_NAME) {
			check();
		}
		
		flash.put("mobile", mobile);
		flash.put("code", code);
		
		if (StringUtils.isBlank(mobile)) {
			flash.error("手机号不能为空");
			
			checkMobile();
		}
		
		if (StringUtils.isBlank(code)) {
			flash.error("验证码不能为空");
			
			checkMobile();
		}
		
		ErrorInfo error = new ErrorInfo();
		user.checkMoible(mobile, code, error);
		
		if (error.code < 0) {
			flash.error(error.msg);
			
			checkMobile();
		}
		
		createIpsAcct();
	}
	
	/**
	 * 资金托管开户页面
	 */
	public static void createIpsAcct() {
		if (User.currUser().getIpsStatus() != IpsCheckStatus.MOBILE) {
			check();
		}
		
		render();
	}
	
	/**
	 * 认证成功
	 */
	public static void checkSuccess() {
		if (User.currUser().getIpsStatus() != IpsCheckStatus.IPS) {
			check();
		}
		
		render();
	}
}
