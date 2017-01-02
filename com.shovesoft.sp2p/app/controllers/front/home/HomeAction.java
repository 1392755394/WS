package controllers.front.home;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import models.t_content_advertisements;
import models.t_content_news;
import models.t_roma_bids;
import models.t_view_count;
import models.t_view_index;
import models.v_bill_board;
import models.v_front_all_bids;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;

import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import utils.Arith;
import utils.ErrorInfo;
import utils.PageBean;
import utils.ServiceFee;
import business.Ads;
import business.AdsPartner;
import business.AuditItem;
import business.Bid.Repayment;
import business.Bill;
import business.CreditLevel;
import business.Invest;
import business.News;
import business.NewsType;
import business.Product;
import business.User;
import constants.Constants;
import constants.OptionKeys;
import controllers.BaseController;

/**
 * 
 * @author liuwenhui
 * 
 */
public class HomeAction extends BaseController {

	/* 网站首页 */
	public static void home() {

		ErrorInfo error = new ErrorInfo();

		List<t_content_advertisements> homeAds = Ads.queryAdsByLocation(
				Constants.HOME_PAGE, error); // 广告条

		List<v_front_all_bids> bidList = Invest.carBids();// 首页最新五个借款标
		List<v_front_all_bids> roomList = Invest.roomBids();


		List<t_content_news> news = News.queryNewForFront(7l, 5, error);// 首页官方公告


		List<AdsPartner> adsPartner = AdsPartner.qureyPartnerForFront(error);// 合作伙伴


        
		List<t_roma_bids> newbids = HomeAction.homeFindRomaBid();//新手标
		

		render(homeAds, bidList, roomList, news, adsPartner,newbids);

	}

	public static void banner() {
		ErrorInfo error = new ErrorInfo();
		List<t_content_advertisements> homeAds = Ads.queryAdsByLocation(
				Constants.HOME_PAGE, error); // 广告条

		renderJSON(homeAds);
	}
	
 public static List<t_roma_bids> homeFindRomaBid(){
	 
	 List<t_roma_bids> bids =null;
	 
	 String hql="from t_roma_bids where roma_status = 1";
	 
	 try {
		bids =GenericModel.find(hql).fetch();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	 return bids;
 }
	

	/**
	 * 前端遮罩层里的数字
	 * 
	 * **/

	public static List<t_view_index> mask() {
		List<t_view_index> bids = null;
		String hql = " from t_view_index order by id desc";

		try {
			bids = GenericModel.find(hql).fetch(1);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return bids;
	}

	/**
	 * 前端显示的人数和钱数（舍弃不用）
	 * 
	 * @return
	 * 
	 * */
	public static List<t_view_count> count() {

		// 当前时间
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
		String nowtime = dt.format(new Date());
		// 随机数
		Random r = new Random();
		int x = r.nextInt(100) + 1;
		// 定值
		Long dz = 1l;

		t_view_count cx = null;

		try {
			cx = GenericModel.findById(1l);
			Long re = (dt.parse(nowtime).getTime() - cx.time.getTime()) / 3600000;

			if ((re < dz)) {

				return findCount();

			} else {

				t_view_count tv = new t_view_count();
				tv.count_money = cx.count_money + x * 100;
				tv.count_people = cx.count_people + x + 10;

				// 总收益
				tv.count_avliage_money = (int) Math
						.floor((cx.count_money + x * 100) * 0.15);
				// 待收
				tv.count_today_money = (int) Math
						.floor((cx.count_money + x * 100) * 0.06);

				java.util.Date date = new java.util.Date();
				java.sql.Date data1 = new java.sql.Date(date.getTime());
				tv.time = data1;

				// tv.save();
				EntityManager em = JPA.em();
				String sql = "update t_view_count set count_people = ? ,count_money=?,count_avliage_money=?,count_today_money=?,time=? where id = ?";
				Query query = em.createQuery(sql)
						.setParameter(1, tv.count_people)
						.setParameter(2, tv.count_money)
						.setParameter(3, tv.count_avliage_money)
						.setParameter(4, tv.count_today_money)
						.setParameter(5, tv.time).setParameter(6, 1l);

				int rows = 0;

				rows = query.executeUpdate();

				return findCount();
			}

		} catch (Exception e) {

			e.printStackTrace();
		}
		return findCount();
	}

	/**
	 * 前端查询人数和钱数
	 * 
	 * @return
	 * */

	public static List<t_view_count> findCount() {

		List<t_view_count> bids = null;

		try {

			bids = GenericModel.find("id=1").fetch();

		} catch (Exception e) {

			e.printStackTrace();
		}

		return bids;

	}

	/**
	 * 财富工具箱
	 */
	public static void wealthToolkit(int key) {
		ErrorInfo error = new ErrorInfo();
		List<Product> products = Product.queryProductNames(true, error);

		List<CreditLevel> creditLevels = CreditLevel
				.queryAllCreditLevels(error);

		render(key, products, creditLevels);
	}

	/**
	 * 信用计算器
	 */
	public static void wealthToolkitCreditCalculator() {
		ErrorInfo error = new ErrorInfo();

		List<AuditItem> auditItems = AuditItem.queryAuditItems(error);

		String value = OptionKeys.getvalue(OptionKeys.CREDIT_LIMIT, error); // 得到积分对应的借款额度值
		double amountKey = StringUtils.isBlank(value) ? 0 : Double
				.parseDouble(value);

		render(auditItems, amountKey);
	}

	/**
	 * 还款计算器
	 */
	public static void wealthToolkitRepaymentCalculator() {
		List<Repayment> rtypes = Repayment.queryRepaymentType(null); // 还款类型

		render(rtypes);
	}

	/**
	 * 还款明细(异步)
	 */
	public static void repaymentCalculate(double amount, double apr,
			int period, int periodUnit, int repaymentType) {
		List<Map<String, Object>> payList = null;

		payList = Bill.repaymentCalculate(amount, apr, period, periodUnit,
				repaymentType);

		render(payList);
	}

	/**
	 * 净值计算器
	 */
	public static void wealthToolkitNetValueCalculator() {
		ErrorInfo error = new ErrorInfo();

		double bailScale = Product.queryNetValueBailScale(error); // 得到净值产品的保证金比例

		render(bailScale);
	}

	/**
	 * 利率计算器
	 */
	public static void wealthToolkitAPRCalculator() {
		ErrorInfo error = new ErrorInfo();

		List<Repayment> rtypes = Repayment.queryRepaymentType(null); // 还款类型

		String value = OptionKeys.getvalue(OptionKeys.CREDIT_LIMIT, error); // 得到积分对应的借款额度值
		double serviceFee = StringUtils.isBlank(value) ? 0 : Double
				.parseDouble(value);

		render(rtypes, serviceFee);
	}

	/**
	 * 利率计算器,计算年华收益、总利益(异步)
	 */
	public static void aprCalculator(double amount, double apr,
			int repaymentType, double award, int rperiod) {
		ErrorInfo error = new ErrorInfo();
		DecimalFormat df = new DecimalFormat("#.00");

		double serviceFee = ServiceFee.investServiceFee(amount, apr, error); // 服务费
		double earning = 0;

		if (repaymentType == 1) {/* 按月还款、等额本息 */
			double monRate = apr / 12;// 月利率
			int monTime = rperiod;
			double val1 = amount * monRate * Math.pow((1 + monRate), monTime);
			double val2 = Math.pow((1 + monRate), monTime) - 1;
			double monRepay = val1 / val2;// 每月偿还金额

			/**
			 * 年化收益
			 */
			earning = Arith.excelRate((amount - award),
					Double.parseDouble(df.format(monRepay)), monTime, 200, 15) * 12 * 100;
			earning = Double.parseDouble(df.format(earning) + "");
		}

		if (repaymentType == 2 || repaymentType == 3) { /* 按月付息、一次还款 */
			double monRate = apr / 12;// 月利率
			int monTime = rperiod;// * 12;借款期限填月
			double borrowSum = Double.parseDouble(df.format(amount));
			double monRepay = Double
					.parseDouble(df.format(borrowSum * monRate));// 每月偿还金额
			double allSum = Double.parseDouble(df.format((monRepay * monTime)))
					+ borrowSum;// 还款本息总额
			earning = Arith.rateTotal(allSum, (borrowSum - award), monTime) * 100;
			earning = Double.parseDouble(df.format(earning) + "");
		}

		JSONObject obj = new JSONObject();
		obj.put("serviceFee", serviceFee < 0 ? 0 : serviceFee);
		obj.put("earning", earning);

		renderJSON(obj);
	}

	/**
	 * 服务手续费
	 */
	public static void wealthToolkitServiceFee() {
		ErrorInfo error = new ErrorInfo();
		String content = News.queryContent(-1011L, error);
		flash.error(error.msg);

		renderText(content);
	}

	/**
	 * 超额借款
	 */
	public static void wealthToolkitOverLoad() {
		ErrorInfo error = new ErrorInfo();

		List<AuditItem> auditItems = AuditItem.queryAuditItems(error);

		String value = OptionKeys.getvalue(OptionKeys.CREDIT_LIMIT, error); // 得到积分对应的借款额度值
		double amountKey = StringUtils.isBlank(value) ? 0 : Double
				.parseDouble(value);

		render(auditItems, amountKey);
	}

	/**
	 * 新手入门
	 */
	public static void getStart(int id) {
		ErrorInfo error = new ErrorInfo();

		String content = News.queryContent(id, error);

		List<Product> products = Product.queryProductNames(true, error);

		List<CreditLevel> creditLevels = CreditLevel
				.queryAllCreditLevels(error);

		render(content, products, creditLevels, id);
	}

	/**
	 * 关于我们
	 */
	public static void aboutUs(int id) {
		ErrorInfo error = new ErrorInfo();
		Object [] investData =  News.queryInvestDataSum();
		List<t_view_count> countList = HomeAction.count();
		List<NewsType> types = NewsType.queryChildTypes(1L, error);
		List<Product> products = Product.queryProductNames(true, error);
		List<CreditLevel> creditLevels = CreditLevel
				.queryAllCreditLevels(error);
		String content = News.queryContent(id, error);
		String contentByTypeId = News.queryContentByTypeId(id, error);
		List<t_content_news> hire = News.hire(id, error);
		List<NewsType> bottomLinks = NewsType.queryChildTypes(3L, error);

		render(content, countList, products, creditLevels, creditLevels, hire,
				bottomLinks, contentByTypeId, types,investData);
	}

	/**
	 * 理财风云榜（更多）
	 */
	public static void moreInvest(int currPage) {
		ErrorInfo error = new ErrorInfo();
		PageBean<v_bill_board> page = Invest.investBillboards(currPage, error);

		if (error.code < 0) {
			render(Constants.ERROR_PAGE_PATH_FRONT);
		}

		render(page);
	}

	/**
	 * 招贤纳士
	 */
	public static void careers() {

	}

	/**
	 * 管理团队
	 */
	public static void managementTeam() {

	}

	/**
	 * 专家顾问
	 */
	public static void expertAdvisor() {

	}
	
	
	/**
	 * 
	 * 需要迁移到FundsManage ,,,,,,,,注意with登录拦截。。。。，就这样
	 * 
	 * @param MemberID
	 * @param TerminalID
	 * @param TransID
	 * @param Result
	 * @param ResultDesc
	 * @param FactMoney
	 * @param AdditionalInfo
	 * @param SuccTime
	 * @param Md5Sign
	 */
	
	
	//宝付异步回调
	public static void callBfbackReturnUrl(String MemberID, String TerminalID,
				String TransID, String Result, String ResultDesc, String FactMoney,
				String AdditionalInfo, String SuccTime, String Md5Sign){
			ErrorInfo error = new ErrorInfo();
			String Md5key = "8qwnf558b36uucdr";
			String MARK = "~|~";
			String md5 = "MemberID=" + MemberID + MARK + "TerminalID=" + TerminalID
					+ MARK + "TransID=" + TransID + MARK + "Result=" + Result
					+ MARK + "ResultDesc=" + ResultDesc + MARK + "FactMoney="
					+ FactMoney + MARK + "AdditionalInfo=" + AdditionalInfo + MARK
					+ "SuccTime=" + SuccTime + MARK + "Md5Sign=" + Md5key;

			User vality = new User();
	        System.out.print("--------------------");
	      
			String WaitSign = vality.getMD5ofStr(md5);

			if (WaitSign.compareTo(Md5Sign) == 0) {  
				User.bfRecharge(TransID, Double.parseDouble(FactMoney), error);
				System.out.println("callBfbackReturnUrl sucess");
			} else {	
				System.out.println("callBfbackReturnUrl error");
			}	
			
		}
	
	
	
	
	
	// 汇潮异步回调

	public static void callHcback(String BillNo, String Amount,
			String tradeOrder, String Succeed, String Result, String SignMD5info) {

		ErrorInfo error = new ErrorInfo();
		String info = "";
		String MD5key = "HidGbB]o"; // MD5key值

		controllers.front.account.MD5 md5 = new controllers.front.account.MD5();

		String md5src = BillNo + "&" + Amount + "&" + Succeed + "&" + MD5key;
		String md5sign; // MD5加密后的字符串
		md5sign = md5.getMD5ofStr(md5src);

		if (SignMD5info.equals(md5sign)) {
			
			User.recharge(BillNo, Double.parseDouble(Amount), error);
			
			System.out.println("huichaozhifuchenggong");
		} else {
	        System.out.println("huichaozhifushibai");
		}



	}
	
	
	
	
}
