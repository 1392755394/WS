package controllers.supervisor.systemSettings;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import models.t_reds;
import controllers.supervisor.SupervisorController;

public class RedManagerAction extends SupervisorController {

	// 跳转
	public static void redSetJump() {

		List<t_reds> redScan = RedManagerAction.redScan();

		render(redScan);
	}

	// 跳转到参数设置

	public static void redSetJumpSet() {

		render();

	}

	// 参数设置
	public static void redSet(Date Red_StartTime, int Red_Amount,
			int Red_Range_Time, String Red_Details) {

		List<t_reds> ltres = null;
		String sql5 = "Red_State=?";

		ltres = GenericModel.find(sql5, 1).fetch();

		if (ltres == null) {

			flash.error("服务器出错");
		}

		Iterator<t_reds> terds = ltres.iterator();
		while (terds.hasNext()) {
			t_reds ts = terds.next();
			long Red_States = ts.id;
			EntityManager em = JPA.em();
			String sql3 = "update t_reds set Red_State = ? where id = ?";
			Query query = em.createQuery(sql3).setParameter(1, 0)
					.setParameter(2, Red_States);
			int rows = 0;
			try {
				rows = query.executeUpdate();

			} catch (Exception e) {
				JPA.setRollbackOnly();
				e.printStackTrace();
			}

		}

		flash.put("Red_StartTime", Red_StartTime);
		flash.put("Red_Amount", Red_Amount);
		flash.put("Red_Range_Time", Red_Range_Time);
		flash.put("Red_Details", Red_Details);
		

		if (Red_StartTime == null) {

			flash.error("请输入开始时间");
		}

		if (Red_Amount + "" == null) {

			flash.error("请输入金额");
		}
		if (Red_Range_Time + "" == null) {

			flash.error("请输入时间范围");
		}
		if (Red_Details == null) {

			flash.error("请输入详情");
		}
		

		t_reds trs = new t_reds();
		trs.Red_Amount = Red_Amount;
		trs.Red_Details = Red_Details;
		trs.Red_StartTime = Red_StartTime;
	    //转换成毫秒
		trs.Red_Range_Time = Red_Range_Time*86400000l;
		trs.RedTime = new Date();
		trs.Red_State =1;
		trs.save();
		redSetJump();
	}

	// 查看历史红包
	public static List<t_reds> redScan() {
		List<t_reds> strs = null;

		try {
			strs = GenericModel.findAll();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return strs;
	}

	// 查看用户红包领取情况
	public static void redUserGet() {

		render();
	}

}
