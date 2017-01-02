package controllers.front.invest;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.t_reds;
import models.t_roma_bids;
import models.t_roma_invests;
import org.apache.commons.lang.StringUtils;

import com.shove.security.Encrypt;

import play.Logger;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import utils.ErrorInfo;
import utils.PageBean;
import business.DataSafety;
import business.User;
import constants.Constants;
import controllers.BaseController;
import controllers.front.account.LoginAndRegisterAction;
import controllers.front.red.RedAction;

public class Roma_InvestAction extends BaseController {

	// 标的详情
	public static void detailsRomaBid(double result) {

		ErrorInfo error = new ErrorInfo();
		User user = User.currUser();
		if (user == null) {
			flash.error("请先登录!");
			LoginAndRegisterAction.login();
		}

		t_roma_bids romaBid = null;

		try {
			romaBid = GenericModel.find("roma_status=?", 1).first();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			error.msg = "出错!";
			e.printStackTrace();
		}

		t_reds newCum = RedAction.redHaveOrNot(user.id, error);

		render(romaBid, result, newCum);
	}

	/**
	 * 投标记录分页ajax方法
	 * 
	 * @param pageNum
	 * @param pageSize
	 * @param bidId
	 */
	public static void viewBidInvestRecords(int pageNum, int pageSize,
			String bidsId) {

		ErrorInfo error = new ErrorInfo();
		int currPage = pageNum;

		if (params.get("currPage") != null) {
			currPage = Integer.parseInt(params.get("currPage"));
		}

		long bidId = Long.parseLong(bidsId);
		
		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		PageBean<t_roma_invests> pageBean = new PageBean<t_roma_invests>();
		pageBean = queryBidInvestRecords(currPage, pageSize, bidId, error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}
		render(pageBean);

	}

	/**
	 * 针对某个标的投标记录
	 * 
	 * @return
	 */
	public static PageBean<t_roma_invests> queryBidInvestRecords(int currPage,
			int pageSize, long bidId, ErrorInfo error) {

		PageBean<t_roma_invests> pageBean = new PageBean<t_roma_invests>();
		List<t_roma_invests> list = new ArrayList<t_roma_invests>();
		pageBean.currPage = currPage;
		pageBean.pageSize = pageSize;

		try {
			pageBean.totalCount = (int) GenericModel.count("roma_bid_id = ?",
					bidId);
			list = GenericModel.find("roma_bid_id = ? ", bidId).fetch(
					currPage, pageBean.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info(e.getMessage());
			error.code = -1;

			return pageBean;
		}
		//System.out.print(pageBean);
		pageBean.page = list;
		error.code = 1;
		return pageBean;

	}

	// 新手表列表
	public static void listRomaBid() {
	}

	// 投标
	public static void investRomaBid(long bidId) {
		String investAmountStr = params.get("investAmountBottom");
		String dealpwd = params.get("dealpwd");

		User user = User.currUser();
		ErrorInfo error = new ErrorInfo();
		if (user == null) {
			flash.error("用户不能为空！");
			detailsRomaBid(-1);
		}

		if (StringUtils.isBlank(dealpwd)) {
			error.msg = "支付密码不能为空！";
			flash.error(error.msg);
			detailsRomaBid(-1);
		}
		String dealpwds = user.getPayPassword();

		if (!Encrypt.MD5(dealpwd + Constants.ENCRYPTION_KEY).equals(dealpwds)) {
			error.msg = "对不起！交易密码错误!";
			error.code = -13;
			flash.error(error.msg);
			detailsRomaBid(-1);
		}

		if (StringUtils.isBlank(investAmountStr)) {
			flash.error("投资金额不能为空！");
			detailsRomaBid(-1);
		}
		boolean b = investAmountStr.matches("^[1-9][0-9]*$");
		if (!b) {
			flash.error("只能输入正整数！！");
			detailsRomaBid(-1);
		}

		int investAmounts = Integer.parseInt(investAmountStr);
		double investAmount = investAmounts;
		// 改掉红包状态
		RedAction redAction = new RedAction();
		redAction.redSetState(user.id);

		if (error.code == -1) {
			flash.error("网络连接出错");
			detailsRomaBid(-1);
		}

		DataSafety data = new DataSafety();// 数据防篡改(针对当前投标会员)
		data.setId(user.id);
		boolean sign = data.signCheck(error);
		if (!sign) {// 数据被异常改动
			flash.error("资金异常");
			JPA.setRollbackOnly();
			detailsRomaBid(-1);
		}

		t_roma_bids trb = null;

		try {
			trb = GenericModel.find("id=?", bidId).first();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			error.msg = "出错1!";
			e.printStackTrace();
			detailsRomaBid(-1);
		}

		double getApr = trb.roma_apr;
		// 计算每天的利息，格式化，保留6位小数
		DecimalFormat df = new DecimalFormat("#.######");
		double apr = ((getApr / 12) / 30) / 100;
		double setApr = Double.parseDouble(df.format(apr));

		t_roma_invests tri = new t_roma_invests();
		tri.user_id = user.id;
		tri.user_name=user.name;
		tri.roma_bid_id = bidId;
		tri.roma_amount = investAmount;
		tri.roma_time = new Date();
		tri.roma_loan_amount = investAmount * setApr * trb.roma_period;

		try {
			tri.save();
			error.code = 2;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			JPA.setRollbackOnly();
			error.msg = "出错2";
			e.printStackTrace();
		}

		if (error.code > 0) {
			detailsRomaBid(1);
		} else {
			flash.error(error.msg);
			detailsRomaBid(-1);
		}

	}

}
