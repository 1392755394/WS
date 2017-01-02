package controllers.front.red;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import net.sf.json.JSONObject;

import com.google.gson.Gson;

import models.t_reds;
import models.t_reds_use;
import models.t_roma_invests;
import models.t_user_env;
import models.t_user_envs;
import models.t_users;
import play.Logger;
import play.db.helper.JpaHelper;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import utils.ErrorInfo;
import business.DataSafety;
import business.DealDetail;
import business.TemplateStation;
import business.User;
import constants.Constants;
import constants.DealType;
import controllers.BaseController;
import controllers.front.account.FundsManage;
import controllers.front.account.LoginAndRegisterAction;

public class RedAction extends BaseController {

	/**
	 * 砸蛋中奖计算
	 * 
	 * **/
	public static void dropEggCount() {

		ErrorInfo error = new ErrorInfo();
		Gson gson = new Gson();
		String out = null;
		JSONObject json = new JSONObject();
		User user = User.currUser();
		String msg = null;
		String zhizhen = null;
		double enmoney = 0;
		json.put("msg", "");
		json.put("anniu", "");
		if (user == null) {
			msg = "未登录";
			json.put("msg", msg);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;
		}
		if (user.isAddBaseInfo == false) {
			msg = "请先去账户中心认证资料!";
			json.put("msg", msg);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;

		}
		// 判断是不是不能摇奖了

		t_user_envs isun = null;
		try {
			isun = GenericModel.find("user_id=? order by time desc", user.id)
					.first();
		} catch (Exception e1) { // TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (isun != null) {

			msg = "只能砸一下!你已经砸过了!";
			json.put("msg", msg);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;

		}

		// 有资格的设置中奖概率
		int mathVal = (int) (Math.random() * 100 + 1);

		if (0 <= mathVal && mathVal <= 80) {
			zhizhen = "1块钱已经冲入账户!";
			enmoney = 1.0;
		}
		if (81 < mathVal && mathVal <= 87) {
			zhizhen = "1块钱已经冲入账户!";
			enmoney = 1.0;
		}
		if (87 < mathVal && mathVal <= 92) {
			zhizhen = "1块钱已经冲入账户!";
			enmoney = 1.0;
		}
		if (92 < mathVal && mathVal <= 95) {
			zhizhen = "1块钱已经冲入账户!";
			enmoney = 1.0;
		}
		if (95 < mathVal && mathVal <= 97) {
			zhizhen = "1块钱已经冲入账户!";
			enmoney = 1.0;
		}
		if (97 < mathVal && mathVal <= 99) {
			zhizhen = "5块钱已经冲入账户!";
			enmoney = 5.0;
		}
		if (99 < mathVal && mathVal <= 101) {
			zhizhen = "5块钱已经冲入账户!";
			enmoney = 5.0;
		}
		// 开始充钱
		double blances = user.balance + enmoney;
		DataSafety data = new DataSafety();// 数据防篡改(针对当前投标会员)
		if (Constants.CHECK_CODE) {
			data.id = user.id;
			if (!data.signCheck(error)) {
				JPA.setRollbackOnly();
				msg = "资金异常";
				json.put("msg", msg);
				out = gson.toJson(json);
				renderText(request.params.get("callback") + "(" + out + ")");
				return;
			}
		}

		int rows = 0;
		String hql = "update t_users set  balance=? where id=?";
		Query query = JPA.em().createQuery(hql);
		query.setParameter(1, blances);
		query.setParameter(2, user.id);
		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			Logger.error("摇奖时:" + e.getMessage());
			msg = "不知道哪里错了1!";
			json.put("msg", msg);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;
		}
		if (rows < -1) {
			msg = "更新出错!";
			json.put("msg", msg);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;
		}
		// 开始
		Map<String, Double> forDetail = DealDetail
				.queryUserFund(user.id, error);
		if (error.code < 0) {
			error.code = -1;
		}
		double balance = forDetail.get("user_amount");
		double freeze = forDetail.get("freeze");
		double receiveAmount = forDetail.get("receive_amount");

		if (error.code < 0) {
			JPA.setRollbackOnly();
			error.msg = "对不起，更新账户出错!";
		}

		DealDetail detail = new DealDetail(user.id, DealType.RECHARGE_HAND,
				enmoney, 1, balance, freeze, receiveAmount, "砸蛋中奖!");

		detail.addDealDetail(error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

		}
		// 添加充值记录
		User.sequence(user.id, 0, enmoney, Constants.ORDINARY_NEWUSER, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			msg = "添加交易记录错了!";
			json.put("msg", msg);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;
		}

		data.id = user.id;
		data.updateSign(error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			msg = "更新字段错了!";
			json.put("msg", msg);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;
		}

		if (error.code < 0) {
			JPA.setRollbackOnly();
			error.msg = "对不起，出错!";
		}

		// 添加摇奖记录
		t_user_envs tuens = new t_user_envs();
		tuens.time = new Date();
		tuens.user_id = user.id;
		tuens.user_name = user.name;
		tuens.money = enmoney;
		try {
			tuens.save();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		json.put("anniu", zhizhen);
		out = gson.toJson(json);
		renderText(request.params.get("callback") + "(" + out + ")");

	}

	/**
	 * 摇奖页面
	 * 
	 * **/
	public static void ernie() {
		render();
	}

	/**
	 * 摇奖计算
	 * 
	 * **/

	public static void ernieCount() {

		ErrorInfo error = new ErrorInfo();
		Gson gson = new Gson();
		String out = null;
		JSONObject json = new JSONObject();
		User user = User.currUser();
		String msg = null;
		String zhizhen = null;
		json.put("msg", "");
		if (user == null) {
			msg = "未登录";
			json.put("msg", msg);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;
		}

		// 判断今天是不是已经摇过奖了

		t_user_env isun = null;
		try {
			isun = GenericModel.find("user_id=? order by time desc", user.id)
					.first();
		} catch (Exception e1) { // TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (isun != null) {
			Date date = new Date();
			long interval = (date.getTime() - isun.time.getTime()) / 1000 / 60 / 60;

			if (interval < 24) {
				msg = "距离上次摇奖不到24小时!稍后再试";
				json.put("msg", msg);
				out = gson.toJson(json);
				renderText(request.params.get("callback") + "(" + out + ")");
				return;
			}
		}

		// 判断是否可以中奖,投资低于100不能中奖
		int subtractionScore = user.score - 100;
		double enmoney = 0;
		if (subtractionScore < 0) {

			// 添加摇奖记录
			t_user_env tuen = new t_user_env();
			tuen.time = new Date();
			tuen.user_id = user.id;
			tuen.user_name = user.name;
			tuen.money = 0.0;

			try {
				tuen.save();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}

			zhizhen = "4";
			json.put("anniu", zhizhen);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;
		}

		// 有资格的设置中奖概率
		int mathVal = (int) (Math.random() * 100 + 1);
		if (0 <= mathVal && mathVal <= 80) {

			// 添加摇奖记录
			t_user_env tuen = new t_user_env();
			tuen.time = new Date();
			tuen.user_id = user.id;
			tuen.user_name = user.name;
			tuen.money = 0.0;
			try {
				tuen.save();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}

			zhizhen = "4";
			json.put("anniu", zhizhen);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");

			return;
		}
		if (81 < mathVal && mathVal <= 87) {
			zhizhen = "1";
			enmoney = 1.0;
		}
		if (87 < mathVal && mathVal <= 92) {
			zhizhen = "2";
			enmoney = 5.0;
		}
		if (92 < mathVal && mathVal <= 95) {
			zhizhen = "3";
			enmoney = 10.0;
		}
		if (95 < mathVal && mathVal <= 97) {
			zhizhen = "5";
			enmoney = 20.0;
		}
		if (97 < mathVal && mathVal <= 99) {
			zhizhen = "7";
			enmoney = 30.0;
		}
		if (99 < mathVal && mathVal <= 101) {
			zhizhen = "6";
			enmoney = 50.0;
		}

		// 开始充钱
		double blances = user.balance + enmoney;
		DataSafety data = new DataSafety();// 数据防篡改(针对当前投标会员)
		if (Constants.CHECK_CODE) {
			data.id = user.id;
			if (!data.signCheck(error)) {
				JPA.setRollbackOnly();
				msg = "资金异常";
				json.put("msg", msg);
				out = gson.toJson(json);
				renderText(request.params.get("callback") + "(" + out + ")");
				return;
			}
		}

		int rows = 0;
		String hql = "update t_users set  balance=? where id=?";
		Query query = JPA.em().createQuery(hql);
		query.setParameter(1, blances);
		query.setParameter(2, user.id);
		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			Logger.error("摇奖时:" + e.getMessage());
			msg = "不知道哪里错了1!";
			json.put("msg", msg);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;
		}
		if (rows < -1) {
			msg = "更新出错!";
			json.put("msg", msg);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;
		}
		// 开始
		Map<String, Double> forDetail = DealDetail
				.queryUserFund(user.id, error);
		if (error.code < 0) {
			error.code = -1;
		}
		double balance = forDetail.get("user_amount");
		double freeze = forDetail.get("freeze");
		double receiveAmount = forDetail.get("receive_amount");

		if (error.code < 0) {
			JPA.setRollbackOnly();
			error.msg = "对不起，更新账户出错!";
		}

		DealDetail detail = new DealDetail(user.id, DealType.RECHARGE_HAND,
				enmoney, 1, balance, freeze, receiveAmount, "摇奖中奖!");

		detail.addDealDetail(error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

		}
		// 添加充值记录
		User.sequence(user.id, 0, enmoney, Constants.ORDINARY_NEWUSER, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			msg = "添加交易记录错了!";
			json.put("msg", msg);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;
		}

		data.id = user.id;
		data.updateSign(error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			msg = "更新字段错了!";
			json.put("msg", msg);
			out = gson.toJson(json);
			renderText(request.params.get("callback") + "(" + out + ")");
			return;
		}

		if (error.code < 0) {
			JPA.setRollbackOnly();
			error.msg = "对不起，出错!";
		}

		// 添加摇奖记录
		t_user_env tuen = new t_user_env();
		tuen.time = new Date();
		tuen.user_id = user.id;
		tuen.user_name = user.name;
		tuen.money = enmoney;
		try {
			tuen.save();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		json.put("anniu", zhizhen);
		out = gson.toJson(json);
		renderText(request.params.get("callback") + "(" + out + ")");

	}

	// 判断是否有红包可以使用
	public static t_reds redHaveOrNot(long id, ErrorInfo error) {

		RedAction redAction = new RedAction();
		redAction.redTimeout(id, error);

		// 跳转到登陆页面
		if (id == 0) {
			redJump();
		}
		t_reds_use bidsHave;
		ArrayList<Object> redList = new ArrayList<Object>();
		String sql = "Red_Use_UserId= ? and Red_Use_State= ?";

		bidsHave = GenericModel.find(sql, id, 1).first();

		if (bidsHave == null) {
			// flash.error("没有可以使用的红包,无法投新手标");
			return null;
		}

		t_reds bidsHaves;
		String sql1 = "id=?";
		bidsHaves = GenericModel.find(sql1, bidsHave.Red_Use_RedId).first();
		return bidsHaves;
	}

	// 检查红包使用是否合法

	public void t_reds_check(long userId, ErrorInfo error) {

		t_reds_use bidsHave;
		String sql = "Red_Use_UserId= ? and Red_Use_State= ?";
		bidsHave = GenericModel.find(sql, userId, 1).first();

		if (bidsHave == null) {

			error.msg = "您没有红包可以使用，红包提交有误！";
			flash.error(error.msg);
			error.code = -10020;
			return;
		}
		error.code = 10020;

	}

	// 当id为空 默认为用户没有登陆，所以
	public static void redJump() {
		LoginAndRegisterAction.login();

	}

	// 超时更改状态

	public void redTimeout(long id, ErrorInfo error) {

		// 根据红包状态获取当前使用的红包
		String sql = "Red_State=?";
		t_reds red;
		red = GenericModel.find(sql, 1).first();

		if (red == null) {
			error.msg = "当前系统没有红包优惠可以使用";
			flash.error(error.msg);
			return;
		}

		// 根据用户的id得到用户当前的红包
		String sql1 = "Red_Use_UserId=?";
		t_reds_use red_userGetTime;
		red_userGetTime = GenericModel.find(sql1, id).first();

		if (red_userGetTime == null) {

			return;
		}

		Date date = new Date();
		double Red_Range_Time = red.Red_Range_Time;
		Date Red_UseGetTime = red_userGetTime.Red_UseGetTime;

		// 格式化当前时间
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

		double dateOne = 0;
		try {
			dateOne = df.parse(df.format(date)).getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		double Red_UseGetTimeOne = 0;

		try {
			Red_UseGetTimeOne = df.parse(df.format(Red_UseGetTime)).getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if ((dateOne - Red_UseGetTimeOne) > Red_Range_Time) {

			RedAction redAction = new RedAction();
			redAction.redSetState(id);
			error.code = 1000;

			// 发送站内信
			User user = User.currUser();
			TemplateStation station = new TemplateStation();
			String stationContent = "您的新手红包超时失效";
			String title = "新手红包超时失效";
			TemplateStation.addMessageTask(user.id, title, stationContent);

		}

	}

	// 红包使用完或者超时更改红包状态

	public void redSetState(long id) {

		ErrorInfo error = new ErrorInfo();
		String sql = "update t_reds_use set Red_Use_State = ? ,Red_UseTime=? where Red_Use_UserId = ?";
		Query query = JpaHelper.execute(sql, 0, new Date(), id);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			JPA.setRollbackOnly();
			error.code = -1;
		}

	}

	// 根据用户的id查询所拥有的红包

	public static List<t_reds_use> redById() {

		User user = User.currUser();

		if (user == null) {
			redJump();
		}

		List<t_reds_use> trub = null;

		String sql = "Red_Use_UserId=?";

		trub = GenericModel.find(sql, user.id).fetch();

		return trub;
	}

	// 根据用户的注册时间查询用户是否有可以领的红包

	public static List<t_reds> redByRt() {

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		User user = User.currUser();
		if (user == null) {

			redJump();

		}

		// 判断用户是否领过新手红包
		List<t_reds_use> trus = null;
		String sql1 = "Red_Use_UserId=? ";
		trus = GenericModel.find(sql1, user.id).fetch();

		if (trus.isEmpty()) {
			List<t_users> rlu = null;
			String sql2 = "id=?";
			rlu = GenericModel.find(sql2, user.id).fetch();

			Iterator<t_users> iter = rlu.iterator();
			while (iter.hasNext()) {
				t_users tus = iter.next();
				Date rt = tus.time;
				long c = 0;
				try {
					c = df.parse(df.format(rt)).getTime();
				} catch (ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				List<t_reds> truss = null;
				String sql3 = "Red_State=?";

				truss = GenericModel.find(sql3, 1).fetch(1);

				Iterator<t_reds> iters = truss.iterator();
				while (iters.hasNext()) {

					t_reds tredss = iters.next();

					Date tredT = tredss.Red_StartTime;
					long tredTl = 0;
					try {

						tredTl = df.parse(df.format(tredT)).getTime();

					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					long tredR = tredss.Red_Range_Time;

					if (tredTl < c && c < (tredTl + tredR)) {

						return truss;

					} else {

						return null;

					}

				}

			}

		}

		return null;
	}

	// 查询用户历史红包

	public static List<t_reds_use> redHistory() {

		ErrorInfo error = new ErrorInfo();
		User user = User.currUser();

		if (user == null) {
			redJump();
		}
		List ee = new ArrayList<Object>();

		List<t_reds_use> ltres = null;

		ltres = GenericModel.find("Red_Use_UserId=?", user.id).fetch();

		Iterator<t_reds_use> a = ltres.iterator();
		while (a.hasNext()) {

			t_reds_use b = a.next();

			long c = b.Red_Use_RedId;

			int m = b.Red_Use_State;

			Date n = b.Red_UseGetTime;

			List<t_reds> d = null;

			d = GenericModel.find("id=?", c).fetch(1);
			Iterator<t_reds> e = d.iterator();
			while (e.hasNext()) {

				t_reds f = e.next();

				String g = f.Red_Details;
				int j = f.Red_Amount;

				Map<String, Object> h = new HashMap<String, Object>();
				h.put("Red_Details", g);
				h.put("Red_Amount", j);
				h.put("Red_Use_State", m);
				h.put("Red_UseGetTime", n);
				ee.add(h);
			}
		}

		return ee;

	}

	// 增加红包 懒得改，就这么样吧

	public static void redAdd() {

		User user = User.currUser();

		if (user == null) {
			redJump();
		}

		List<t_reds> truss = null;
		String sql3 = "Red_State=?";
		truss = GenericModel.find(sql3, 1).fetch();
		Iterator<t_reds> iters = truss.iterator();
		while (iters.hasNext()) {

			t_reds tredss = iters.next();

			long Red_Use_RedId = tredss.id;

			List<t_reds_use> trus = null;
			String sql1 = "Red_Use_UserId=?";
			trus = GenericModel.find(sql1, user.id).fetch();
			if (trus.isEmpty()) {

				t_reds_use f = new t_reds_use();
				f.Red_Use_RedId = Red_Use_RedId;
				f.Red_Use_State = 1;
				f.Red_Use_UserId = user.id;
				f.Red_UseGetTime = new Date();
				f.save();
				// render("@front.account.FundsManage.redPage");
				FundsManage.redPage();

			} else {

				flash.error("领取红包失败");
			}

		}

	}

	/**
	 * 查询用户红包投资记录
	 * 
	 */

	public static List<t_roma_invests> redInvest() {

		User user = User.currUser();

		if (user == null) {
			redJump();
		}
		List<t_roma_invests> inves = null;

		String sql3 = "user_id=?";
		inves = GenericModel.find(sql3, user.id).fetch();

		return inves;

	}

}
