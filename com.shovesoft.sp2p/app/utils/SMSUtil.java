package utils;

import java.util.Random;

import business.BackstageSet;

import controllers.DbMsm;

import org.apache.commons.lang.StringUtils;

public class SMSUtil {

	/**
	 * 发送短信
	 * @param mobile
	 * @param content
	 * @param error
	 */
	public static void sendSMS(String mobile,String content, ErrorInfo error) {
		if(StringUtils.isBlank(content)) {
			error.code = -1;
			error.msg = "请输入短信内容";
			
			return;
		}
		
		BackstageSet backstageSet  = BackstageSet.getCurrentBackstageSet();
		/*String balance = EimsSMS.getBalance(backstageSet.smsAccount, backstageSet.smsPassword);
		double balance_long = Convert.strToDouble(balance, 0);
		
		if(balance_long <= 0.0){
			error.code = -2;
			error.msg = "短信平台已欠费,请联系管理员!";
			
			return;
		}*/
		
		//EimsSMS.send(backstageSet.smsAccount, backstageSet.smsPassword, content, mobile);
		
	    DbMsm.send(backstageSet.smsAccount, backstageSet.smsPassword, content, mobile);
		error.msg = "短信发送成功";

	}	
	/**
	 * 发送校验码
	 * @param mobile
	 * @param error
	 */
	public static void sendCode(String mobile, ErrorInfo error) {
		error.clear();
		
		BackstageSet backstageSet  = BackstageSet.getCurrentBackstageSet();
		/*String balance = EimsSMS.getBalance(backstageSet.smsAccount, backstageSet.smsPassword);
		double balance_long = Convert.strToDouble(balance, 0);
		if(balance_long <= 0.0){
			error.code = -2;
			error.msg = "短信平台已欠费,请联系管理员!";			
			return;
		}*/
		int randomCode = (new Random()).nextInt(8999) + 1000;// 最大值位9999
		
		String content = "尊敬的客户您好,欢迎使用"+backstageSet.platformName+",您的验证码是:" + randomCode;
		//EimsSMS.send(backstageSet.smsAccount, backstageSet.smsPassword, content, mobile);
	    DbMsm.send(backstageSet.smsAccount, backstageSet.smsPassword, content, mobile);
		play.cache.Cache.set(mobile, randomCode, "2min");
	
			error.msg = "短信发送成功";

	}
}
