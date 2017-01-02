package controllers.front.bid;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

import com.shove.Convert;
import models.t_content_news;
import models.t_dict_ad_citys;
import models.t_dict_ad_provinces;
import models.t_dict_cars;
import models.t_dict_educations;
import models.t_dict_houses;
import models.t_dict_maritals;
import net.sf.json.JSONObject;
import constants.Constants;
import constants.IPSConstants;
import constants.OptionKeys;
import constants.IPSConstants.IpsCheckStatus;
import controllers.BaseController;
import controllers.front.account.CheckAction;
import business.Ads;
import business.BackstageSet;
import business.Bid;
import business.News;
import business.Payment;
import business.Product;
import business.User;
import business.Bid.Purpose;
import play.cache.Cache;
import play.mvc.Before;
import utils.CaptchaUtil;
import utils.ErrorInfo;
import utils.PageBean;
import utils.Security;

/**
 * 标 Action
 * 
 * @author bsr
 * @version 6.0
 * @created 2014-4-22 上午09:47:28
 */
public class BidAction extends BaseController {

	/**
	 * 我要借款首页
	 */
	public static void index(long productId, int code, int status) {
		ErrorInfo error = new ErrorInfo();
		/* 根据排序得到所有的非合作机构产品列表 */
		List<Product> products = Product.queryProduct(error);
		/* 最新投资资讯 */
		List<Bid> bids = Bid.queryAdvertisement(error);
		/* 借款须知 */
		PageBean<t_content_news> pageBean = News.queryNewsByTypeId("14", "1",
				"5", "", error);
		/* 小广告 */
		Ads ads = new Ads();
		ads.id = 13;

		renderArgs.put("products", products);
		renderArgs.put("bids", bids);
		renderArgs.put("pageBean", pageBean);
		renderArgs.put("ads", ads);
		renderArgs.put("code", code);
		renderArgs.put("productId", productId);
		renderArgs.put("status", status);

		User user = User.currUser();

		/* 未邮箱激活 */
		if (code == Constants.NOT_EMAILVERIFIED) {
			if (null == user)
				render(Constants.ERROR_PAGE_PATH_FRONT);

			renderArgs.put("userName", user.name);
			renderArgs.put("email", user.email);

			render();
		}

		/* 未完成基本资料 */
		if (code == Constants.NOT_ADDBASEINFO) {
			addBaseInfo();

			render(user);
		}

		render();
	}

	/**
	 * 详情
	 */
	public static void detail(long productId, int code, int status) {
		ErrorInfo error = new ErrorInfo();

		Product product = new Product();
		product.id = productId;

		if (product.id < 1)
			render(Constants.ERROR_PAGE_PATH_FRONT);

		/* 非合作机构,PC/PC+APP产品列表 */
		List<Product> products = Product.queryProduct(error);

		/* 手续费常量值 */
		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();
		double strfee = backstageSet.borrowFee;
		double borrowFeeMonth = backstageSet.borrowFeeMonth;
		double borrowFeeRate = backstageSet.borrowFeeRate;

		renderArgs.put("product", product);
		renderArgs.put("productId", productId);
		renderArgs.put("products", products);
		renderArgs.put("strfee", strfee);
		renderArgs.put("borrowFeeMonth", borrowFeeMonth);
		renderArgs.put("borrowFeeRate", borrowFeeRate);
		renderArgs.put("code", code);
		renderArgs.put("status", status);

		/* 未邮箱激活 */
		if (code == Constants.NOT_EMAILVERIFIED) {
			User user = User.currUser();

			if (null == user)
				render(Constants.ERROR_PAGE_PATH_FRONT);

			renderArgs.put("userName", user.name);
			renderArgs.put("email", user.email);

			render();
		}

		/* 未完成基本资料 */
		if (code == Constants.NOT_ADDBASEINFO) {
			addBaseInfo();

			render();
		}

		render();
	}

	/**
	 * 立即申请
	 */
	public static void applyNow(long productId, int code, int status) {
		productId = Convert.strToLong(productId + "", 0);
		status = Convert.strToInt(status + "", 0);

		ErrorInfo error = new ErrorInfo();

		/* 借款用途 */
		List<Purpose> purpose = Purpose.queryLoanPurpose(error, true);

		if (null == purpose) {
			flash.error("借款用途有误!");

			render();
		}

		Product product = new Product();
		product.createBid = true;
		product.id = productId;

		if (product.id < 1)
			render(Constants.ERROR_PAGE_PATH_FRONT);

		String key = "bid_" + session.getId();
		Bid loanBid = (Bid) Cache.get(key); // 获取用户输入的临时数据
		Cache.delete(key); // 删除缓存中的bid对象
		String uuid = CaptchaUtil.getUUID(); // 防重复提交UUID

		render(purpose, product, code, uuid, loanBid, status);
	}

	/**
	 * 发布借款
	 */
	public static void createBid(Bid bid, String signProductId, String uuid,
			int status) {
		checkAuthenticity();

		ErrorInfo error = new ErrorInfo();
		long productId = Security.checkSign(signProductId,
				Constants.PRODUCT_ID_SIGN, Constants.VALID_TIME, error);

		if (productId < 1) {
			flash.error(error.msg);

			applyNow(productId, -100, status);
		}

		/* 确定用户的状态 */
		// User user = User.currUser();
		// if(null == user) index(productId, Constants.NOT_LOGIN, status);
		// if(!user.isEmailVerified) index(productId,
		// Constants.NOT_EMAILVERIFIED, status);
		// if(!user.isAddBaseInfo) index(productId, Constants.NOT_ADDBASEINFO,
		// status);

		/* 防重复提交 */
		if (!CaptchaUtil.checkUUID(uuid)) {
			flash.error("请求已提交或请求超时!");

			applyNow(productId, -100, status);
		}

		bid.createBid = true; // 优化加载
		bid.productId = productId; // 填充产品对象
		bid.userId = User.currUser().id; // 填充用户对象

		/* 非友好提示 */
		if (null == bid || null == bid.product || !bid.product.isUse
				|| bid.product.isAgency || bid.user.id < 1) {

			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		Cache.set("bid_" + session.getId(), bid); // 缓存用户输入的临时数据

		/* 检查数据 */
		if (checkBid(bid))
			applyNow(productId, -100, status);

		/* 发布借款 */
		bid.createBid(error);

		if (Constants.IPS_ENABLE && error.code >= 0) {
			Map<String, String> args = Payment.registerSubject(
					IPSConstants.BID_CREATE, bid);

			render("@front.account.PaymentAction.registerSubject", args);
		}

		flash.put("msg", error.msg);

		/* 页面需要的返回数据 */
		if (bid.id > 0) {
			flash.put("no", OptionKeys.getvalue(OptionKeys.LOAN_NUMBER, error)
					+ bid.id);
			flash.put("title", bid.title);
			flash.put("amount", bid.amount);
			flash.put("status", bid.status);
		}

		applyNow(productId, error.code, status);
	}

	/**
	 * 检查数据
	 */
	private static boolean checkBid(Bid bid) {
		if (StringUtils.isBlank(bid.title) || bid.title.length() > 24) {
			flash.error("借款标题有误!");
			return true;
		}

		int _amount = (int) bid.amount;

		if (bid.amount <= 0 || bid.amount != _amount
				|| bid.amount < bid.product.minAmount
				|| bid.amount > bid.product.maxAmount) {
			flash.error("借款金额有误!");

			return true;
		}

		if (bid.apr <= 0 || bid.apr > 100
				|| bid.apr < bid.product.minInterestRate
				|| bid.apr > bid.product.maxInterestRate) {
			flash.error("年利率有误!");

			return true;
		}

		if (bid.product.loanImageType == Constants.USER_UPLOAD
				&& (StringUtils.isBlank(bid.imageFilename) || Constants.DEFAULT_IMAGE
						.equals(bid.imageFilename))) {
			flash.error("借款图片有误!");

			return true;
		}

		if (bid.purpose.id < 0) {
			flash.error("借款用途有误!");

			return true;
		}

		if (bid.repayment.id < 0) {
			flash.error("借款类型有误!");

			return true;
		}

		if (bid.period <= 0) {
			flash.error("借款期限有误!");

			return true;
		}

		switch (bid.periodUnit) {
		case Constants.YEAR:

			if (bid.period > Constants.YEAR_PERIOD_LIMIT) {
				flash.error("借款期限超过了" + Constants.YEAR_PERIOD_LIMIT + "年");

				return true;
			}

			break;
		case Constants.MONTH:

			if (bid.period > Constants.YEAR_PERIOD_LIMIT * 12) {
				flash.error("借款期限超过了" + Constants.YEAR_PERIOD_LIMIT + "年");

				return true;
			}

			break;
		case Constants.DAY:

			if (bid.period > Constants.YEAR_PERIOD_LIMIT * 12 * 30) {
				flash.error("借款期限超过了" + Constants.YEAR_PERIOD_LIMIT + "年");

				return true;
			}

			if (bid.investPeriod > bid.period) {
				flash.error("天标满标期限不能大于借款期限 !");

				return true;
			}

			break;
		default:
			flash.error("借款期限单位有误!");

			return true;
		}

		if ((bid.minInvestAmount > 0 && bid.averageInvestAmount > 0)
				|| (bid.minInvestAmount <= 0 && bid.averageInvestAmount <= 0)) {
			flash.error("最低投标金额和平均招标金额有误!");

			return true;
		}

		if (bid.averageInvestAmount > 0
				&& bid.amount % bid.averageInvestAmount != 0) {
			flash.error("平均招标金额有误!");

			return true;
		}

		if (bid.investPeriod <= 0) {
			flash.error("投标期限有误!");

			return true;
		}

		if (StringUtils.isBlank(bid.description)
				|| bid.description.length() > 300) {
			flash.error("借款描述有误!");

			return true;
		}

		if (bid.bonusType == Constants.FIXED_AMOUNT_REWARD
				&& (bid.bonus < 0 || bid.bonus > bid.amount)) {
			flash.error("固定奖励大于了借款金额");

			return true;
		}

		if (bid.bonusType == Constants.PROPORTIONATELY_REWARD
				&& (bid.awardScale < 0 || bid.awardScale > 100)) {
			flash.error("借款奖励比例有误!");

			return true;
		}

		if (bid.minInvestAmount > 0
				&& (bid.minInvestAmount < bid.product.minInvestAmount)) {
			flash.error("最低投标金额不能小于产品最低投标金额!");

			return true;
		}

		if (bid.averageInvestAmount > 0
				&& (bid.amount / bid.averageInvestAmount > bid.product.maxCopies)) {
			flash.error("平均投标份数不能大于产品的最大份数限制 !");

			return true;
		}

		if (bid.product.loanType == Constants.S_REPAYMENT_BID
				&& bid.periodUnit != Constants.DAY) {
			flash.error("秒还标借款期限需为天[标]!");

			return true;
		}

		return false;
	}

	/**
	 * 查看用户当前的登录状态(异步)
	 */
	public static void checkUserStatus() {
		User user = User.currUser();

		JSONObject json = new JSONObject();

		/* 是否登录 */
		if (null == user) {
			json.put("status", Constants.NOT_LOGIN);

			renderJSON(json);
		}

		/* 是否激活 */
		if (!user.isEmailVerified) {
			json.put("userName", user.name);
			json.put("email", user.email);
			json.put("status", Constants.NOT_EMAILVERIFIED);

			renderJSON(json);
		}

		/* 是否完善基本资料 */
		if (!user.isAddBaseInfo) {
			json.put("status", Constants.NOT_ADDBASEINFO);

			renderJSON(json);
		}

		json.put("status", Constants.SUCCESS_STATUS);
		renderJSON(json);
	}

	/**
	 * 弹框登录(异步)
	 */
	public static void logining(String name, String password, String code,
			String randomID) {

		business.BackstageSet currBackstageSet = business.BackstageSet
				.getCurrentBackstageSet();
		Map<String, java.util.List<business.BottomLinks>> bottomLinks = business.BottomLinks
				.currentBottomlinks();

		if (null != currBackstageSet) {
			Cache.delete("backstageSet");// 清除系统设置缓存
		}

		if (null != bottomLinks) {
			Cache.delete("bottomlinks");// 清除底部连接缓存
		}

		ErrorInfo error = new ErrorInfo();

		if (StringUtils.isBlank(name))
			renderText("请输入用户名!");

		if (StringUtils.isBlank(password))
			renderText("请输入密码!");

		if (StringUtils.isBlank(code))
			renderText("请输入验证码");

		if (StringUtils.isBlank(randomID))
			renderText("请刷新验证码");

		if (!code.equalsIgnoreCase(CaptchaUtil.getCode(randomID)))
			renderText("验证码错误");

		User user = new User();
		user.name = name;

		if (user.id < 0)
			renderText("该用户名不存在");

		if (user.login(password, false, error) < 0)
			renderText(error.msg);

	}

	/**
	 * 完善基本资料
	 */
	private static void addBaseInfo() {
		List<t_dict_cars> cars = (List<t_dict_cars>) Cache.get("cars"); // 车子
		List<t_dict_ad_provinces> provinces = (List<t_dict_ad_provinces>) Cache
				.get("provinces"); // 省
		List<t_dict_educations> educations = (List<t_dict_educations>) Cache
				.get("educations"); // 教育
		List<t_dict_houses> houses = (List<t_dict_houses>) Cache.get("houses"); // 房子
		List<t_dict_maritals> maritals = (List<t_dict_maritals>) Cache
				.get("maritals"); // 婚姻

		String key = "province" + session.getId();
		Object obj = Cache.get(key);
		Cache.delete(key);
		int province = obj == null ? 1 : Integer.parseInt(obj.toString());
		List<t_dict_ad_citys> cityList = User.queryCity(province); // 市

		renderArgs.put("cars", cars);
		renderArgs.put("provinces", provinces);
		renderArgs.put("educations", educations);
		renderArgs.put("houses", houses);
		renderArgs.put("maritals", maritals);
		renderArgs.put("cityList", cityList);
	}

	/**
	 * 保存基本信息
	 */
	public static void saveInformation(String realityName, int sex, int age,
			int city, int province, String idNumber, int education,
			int marital, int car, int house, String mobile1, String code1,
			long productId, int status) {

		User user = User.currUser();

		if (null == user) {
			if (Constants.APPLY_NOW_INDEX == status)
				index(productId, Constants.NOT_LOGIN, status);
			else
				detail(productId, Constants.NOT_LOGIN, status);
		}

		user.id = User.currUser().id; // 及时在抓取一次

		if (!user.isEmailVerified)
			if (Constants.APPLY_NOW_INDEX == status)
				index(productId, Constants.NOT_EMAILVERIFIED, status);
			else
				detail(productId, Constants.NOT_EMAILVERIFIED, status);

		if (user.isAddBaseInfo)
			render(Constants.ERROR_PAGE_PATH_FRONT);

		flash.put("realityName", realityName);
		flash.put("sex", sex);
		flash.put("age", age);
		flash.put("city", city);
		flash.put("province", province);
		Cache.set("province" + session.getId(), province);
		flash.put("idNumber", idNumber);
		flash.put("education", education);
		flash.put("marital", marital);
		flash.put("car", car);
		flash.put("house", house);
		flash.put("mobile1", mobile1);

		if (Constants.CHECK_CODE) {
			Object cCode1 = Cache.get(mobile1);

			if (cCode1 == null) {
				flash.error("验证码已失效，请重新点击发送验证码");

				if (Constants.APPLY_NOW_INDEX == status)
					index(productId, Constants.NOT_ADDBASEINFO, status);
				else
					detail(productId, Constants.NOT_ADDBASEINFO, status);
			}

			if (!cCode1.toString().equals(code1)) {
				flash.error("手机验证错误");

				if (Constants.APPLY_NOW_INDEX == status)
					index(productId, Constants.NOT_ADDBASEINFO, status);
				else
					detail(productId, Constants.NOT_ADDBASEINFO, status);
			}

		}

		User newUser = new User();
		newUser.id = user.id;

		newUser.realityName = realityName;
		newUser.setSex(sex);
		newUser.age = age;
		newUser.cityId = city;
		newUser.educationId = education;
		newUser.maritalId = marital;
		newUser.carId = car;
		newUser.idNumber = idNumber;
		newUser.houseId = house;
		newUser.mobile1 = mobile1;
		ErrorInfo error = new ErrorInfo();
		newUser.edit(user, error);

		if (error.code < 0) {
			flash.error(error.msg);
			if (Constants.APPLY_NOW_INDEX == status)
				index(productId, Constants.NOT_ADDBASEINFO, status);
			else
				detail(productId, Constants.NOT_ADDBASEINFO, status);
		}

		applyNow(productId, 0, status);
	}

	/**
	 * 最新满标
	 */
	public static void fullBid(int nowPage) {
		ErrorInfo error = new ErrorInfo();

		PageBean<Bid> pageBean = new PageBean<Bid>();
		pageBean.currPage = nowPage;
		pageBean.pageSize = Constants.FULL_BID_COUNT;
		pageBean.page = Bid.queryFullBid(pageBean, error);

		render(pageBean);
	}

	/* 拦截 createBid, applyNow方法,防止非法数据提交 */
	@Before(only = { "applyNow" })
	static void checkValid() {
		String _status = params.get("status");
		String _productId = params.get("productId");

		if (StringUtils.isBlank(_productId) || StringUtils.isBlank(_status))
			render(Constants.ERROR_PAGE_PATH_FRONT);

		long productId = Long.parseLong(_productId);
		int status = Integer.parseInt(_status);
		User user = User.currUser();

		switch (status) {
		/* 首页申请 */
		case Constants.APPLY_NOW_INDEX:
			if (null == user)
				index(productId, Constants.NOT_LOGIN, status);

			user.id = User.currUser().id; // 及时在抓取一次

			if (Constants.IPS_ENABLE
					&& (User.currUser().getIpsStatus() != IpsCheckStatus.IPS)) {
				CheckAction.approve();
			}

			if (!user.isEmailVerified)
				index(productId, Constants.NOT_EMAILVERIFIED, status);

			if (!user.isAddBaseInfo) {

				if (!user.isAddBaseInfo)
					index(productId, Constants.NOT_ADDBASEINFO, status);
			}

			break;
		/* 详情申请 */
		case Constants.APPLY_NOW_DETAIL:
			if (null == user)
				detail(productId, Constants.NOT_LOGIN, status);

			user.id = User.currUser().id; // 及时在抓取一次

			if (Constants.IPS_ENABLE
					&& (User.currUser().getIpsStatus() != IpsCheckStatus.IPS)) {
				CheckAction.approve();
			}

			if (!user.isEmailVerified)
				detail(productId, Constants.NOT_EMAILVERIFIED, status);

			if (!user.isAddBaseInfo) {

				if (!user.isAddBaseInfo)
					detail(productId, Constants.NOT_ADDBASEINFO, status);
			}

			break;

		default:
			index(productId, 0, 1);

			break;
		}

	}

//	public static void bidsBase() throws Exception {

		//2147480000
		
//		for (int i = 0; i < 1000; i++) {
//			generate("www.dbp2p.com" + i);
//		}

//		renderJSON("");
//	}

//	public static Class generate(String name) throws Exception {
//		ClassPool pool = ClassPool.getDefault();
//		return pool.makeClass(name).toClass();
//	}

}
