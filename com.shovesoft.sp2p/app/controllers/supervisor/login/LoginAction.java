package controllers.supervisor.login;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.gson.JsonObject;
import constants.Constants;
import controllers.BaseController;
import controllers.supervisor.managementHome.HomeAction;
import business.BackstageSet;
import business.Supervisor;
import play.cache.Cache;
import play.libs.Codec;
import play.libs.WS;
import utils.DataUtil;
import utils.DateUtil;
import utils.ErrorInfo;

/**
 * 登录
 * @author lzp
 * @version 6.0
 * @created 2014-5-29
 */
public class LoginAction extends BaseController {
	
	/**
	 * 登录界面
	 */
	public static void loginInit() {
		String randomID = Codec.UUID();
		String companyName = BackstageSet.getCurrentBackstageSet().companyName;
		render(randomID, companyName);
	}
	
	/**
	 * ip定位
	 */
	public static void ipLocation() {
		JsonObject json = WS.url(Constants.URL_IP_LOCATION + "&ip=" + DataUtil.getIp()).get().getJson().getAsJsonObject();
		String province = (json.get("province") == null ? "" : json.get("province").getAsString());
		String city = (json.get("city") == null ? "" : json.get("city").getAsString());
		
		if (province.equals(city)) {
			renderText(province);
		}
		
		renderText(province + city);
	}
	
	/**
	 * 云盾登录
	 * @param userName
	 * @param password
	 * @param sign
	 * @throws UnsupportedEncodingException
	 */
	public static void ukeyCheck(String userName, String password, String sign, String time) throws UnsupportedEncodingException{
		ErrorInfo error = new ErrorInfo();
		
		String result = Supervisor.checkUkey(userName, password, sign, time, error);
		ByteArrayInputStream is = new ByteArrayInputStream(result.getBytes("ISO-8859-1"));
		
		renderBinary(is);
	}
	
	/**
	 * 登录
	 * @param name
	 * @param password
	 * @param captcha
	 * @param randomCode
	 */
	public static void login(String name, String password, String captcha, String randomID, String city, String flag) {
		
	   business.BackstageSet  currBackstageSet = business.BackstageSet.getCurrentBackstageSet();
	   Map<String,java.util.List<business.BottomLinks>> bottomLinks = business.BottomLinks.currentBottomlinks();
	   
	   if(null != currBackstageSet){
		   Cache.delete("backstageSet");//清除系统设置缓存
	   }
	   
	   if(null != bottomLinks){
		   Cache.delete("bottomlinks");//清除底部连接缓存
	   }
		
		ErrorInfo error = new ErrorInfo();
		
		flash.put("name", name);
		flash.put("password", password);
		
		if (StringUtils.isBlank(captcha)) {
			flash.error("请输入验证码");
			
			loginInit();
		}

		if (StringUtils.isBlank(randomID)) {
			flash.error("请刷新验证码");
			
			loginInit();
		}

		String random = (String) Cache.get(randomID);
		// Logger.info("supervisor_[id:%s][random:%s]", randomID,random);
		Cache.delete(randomID);
		if (!captcha.equalsIgnoreCase(random)) {
			flash.error("验证码错误");
			
			loginInit();
		}

		Supervisor supervisor = new Supervisor();
		supervisor.name = name;
		supervisor.loginIp = DataUtil.getIp();
		supervisor.loginCity = city;
		
		long adminId = Supervisor.queryAdminId(name, com.shove.security.Encrypt.MD5(password + Constants.ENCRYPTION_KEY), error);
		String time = Long.toString(new DateUtil().getHours());
		String flag2 = com.shove.security.Encrypt.MD5(Long.toString(adminId) + time);

//		//正式发布环境需开通验证功能
//		if(!flag2.equals(flag)){
//			flash.error("未检测到有效的云盾");
//			loginInit();
//		}
		
		supervisor.login(password, error);
		
		if (error.code < 0) {
			flash.error(error.msg);
			loginInit();
		}

		HomeAction.showHome();
	}
	
	public static void logout() {
		ErrorInfo error = new ErrorInfo();
		
		Supervisor supervisor = Supervisor.currSupervisor();
		
		if (null != supervisor) {
			supervisor.logout(error);
		}
		
		Supervisor.deleteCurrSupervisor();//请除缓存
		redirect("/supervisor");
	}

	/**
	 * 跳转到警告页面
	 */
	public static void loginAlert() {
		render();
	}
	
	/**
	 * 跳转到检查云盾状态页面
	 */
	public static void checkUkeyInIt(String url) {
		//System.out.println(url);
		
		render(url);
	}
	
	/**
	 * 通过页面传过来的数据验证云盾状态
	 */
	public static void checkUkeySign(String sign, String url, String hostPath) {
		String sign2 = Supervisor.encryptAdminId();
		String path = hostPath + url;
		String flag = null;
		
		if(sign.equalsIgnoreCase("noKey")){
			flag = Constants.CLOUD_SHIELD_NOT_EXIST;
			//设置缓存
			Cache.set("yunflag", flag);
			redirect(path);
			
		}
		
		if(!sign.equalsIgnoreCase(sign2)){
			flag = Constants.CLOUD_SHIELD_SUPERVISOR;
			//设置缓存
			Cache.set("yunflag", flag);
			redirect(path);
		}
		
		//设置缓存
		flag = "";
		Cache.set("yunflag", flag);
		redirect(path);
	}

	/**跳转到空白页面
	 */
	public static void toBlank() {
		render();
	}
}
