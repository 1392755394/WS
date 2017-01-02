package controllers.front.welfare;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import models.t_dict_payment_gateways;
import models.t_user_recharge_details;
import models.t_users;
import utils.ErrorInfo;
import business.Payment;
import business.User;
import constants.Constants;
import constants.Constants.RechargeType;
import controllers.BaseController;
import controllers.front.account.LoginAndRegisterAction;

public class WelfareAction extends BaseController {

	// 跳转到公益页面
	public static void welfareJump() {

		List<Object> welLists = WelfareAction.welfareAccount();

		render( welLists);
	}

	// 跳转到捐款页面
	public static void welfareDonat() {
		ErrorInfo error = new ErrorInfo();

		User user = User.currUser();
		if (user == null) {			
			LoginAndRegisterAction.login();
		}

		if (Constants.IPS_ENABLE) {
			List<Map<String, Object>> bankList = Payment.getBankList(error);

			render("@front.account.FundsManage.rechargeIps", user, bankList);
		}

		List<t_dict_payment_gateways> payType = User.gatewayForUse(error);

		render(user, payType);
	}

	// 跳转到查询捐款名单列表页面

	public static void welfarePeople() {

		List<Object> welList = WelfareAction.welfareFind();
		render(welList);

	}

	// 查询所有捐过钱的客户

	public static List<Object> welfareFind() {

		List<t_user_recharge_details> welfarelist = null;
		ArrayList<Object> welList = new ArrayList<Object>();
		welfarelist = GenericModel.find(
				"is_completed=? and type=? ", true, RechargeType.welfare)
				.fetch();
		Iterator<t_user_recharge_details> welll = welfarelist.iterator();	
		int i=0;
		while (welll.hasNext()) {
			t_user_recharge_details user_details = welll.next();
            
            i=i+1;
			long user_id = user_details.user_id;
			Date time = user_details.time;
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			double amount = user_details.amount;
			long id = user_details.id;
			t_users user = null;
			user = GenericModel.findById(user_id);
			String name = user.reality_name;
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("name", name);
			map.put("time",df.format(time));
			map.put("amount", amount);
			map.put("order",i);
			welList.add(map);
		}

		return welList;
	}

	// 统计钱数和客户总数

	public static ArrayList<Object> welfareAccount() {
		List<t_user_recharge_details> welfarelist = null;
		ArrayList<Object> welLists = new ArrayList<Object>();
		welfarelist = GenericModel.find(
				"is_completed=? and type=? ", true, RechargeType.welfare)
				.fetch();
		Iterator<t_user_recharge_details> welll = welfarelist.iterator();
		double j = 0;
		while (welll.hasNext()) {
			t_user_recharge_details user_details = welll.next();
			double amount = user_details.amount;
			j = amount + j;
		}
		DecimalFormat df = new DecimalFormat("######0.00");

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("amount", df.format(j));
		map.put("amountPeople", welfarelist.size());
		welLists.add(map);
		return welLists;
	}
	
	//跳转到公益规章
	public static  void welfareUbar(){
		
		
		render();
	}
	
	//跳转到东邦公益雅安专栏
	
	public static void welfareYaan(){
		
		
		render();
	}
	
	
	//跳转到扶住对象
	
	
	public static void welfareObject(){
		
		
		
		render();
	}
	
	//跳转到活动
	
	public static void welfareActivty(){
		
		render();
		
	}
	
	
	

}
