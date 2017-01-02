package business;

import interfaces.UserBase;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import models.t_bids;
import models.t_bill_invests;
import models.t_bills;
import models.t_dict_ad_citys;
import models.t_dict_ad_provinces;
import models.t_dict_cars;
import models.t_dict_educations;
import models.t_dict_houses;
import models.t_dict_maritals;
import models.t_dict_payment_gateways;
import models.t_invests;
import models.t_system_recharge_completed_sequences;
import models.t_user_attention_users;
import models.t_user_blacklist;
import models.t_user_cps_income;
import models.t_user_details;
import models.t_user_events;
import models.t_user_recharge_details;
import models.t_user_report_users;
import models.t_user_withdrawals;
import models.t_users;
import models.v_bid_assigned;
import models.v_bill_invest_statistics;
import models.v_user_account_info;
import models.v_user_account_statistics;
import models.v_user_attention_info;
import models.v_user_blacklist;
import models.v_user_blacklist_info;
import models.v_user_complex_info;
import models.v_user_cps_detail;
import models.v_user_cps_info;
import models.v_user_cps_offer_info;
import models.v_user_cps_user_count;
import models.v_user_cps_user_info;
import models.v_user_cps_users;
import models.v_user_detail_credit_score_audit_items;
import models.v_user_detail_credit_score_invest;
import models.v_user_detail_credit_score_loan;
import models.v_user_detail_credit_score_normal_repayment;
import models.v_user_detail_credit_score_overdue;
import models.v_user_detail_score;
import models.v_user_details;
import models.v_user_for_details;
import models.v_user_for_message;
import models.v_user_for_personal;
import models.v_user_info;
import models.v_user_invest_amount;
import models.v_user_invest_info;
import models.v_user_loan_info;
import models.v_user_loan_info_bad;
import models.v_user_loan_info_bad_d;
import models.v_user_loan_info_bill;
import models.v_user_loan_info_bill_d;
import models.v_user_loan_user_unassigned;
import models.v_user_locked_info;
import models.v_user_report_list;
import models.v_user_reported_info;
import models.v_user_scores;
import models.v_user_unverified_info;
import models.v_user_users;
import models.v_user_vip_info;
import models.v_user_withdrawal_info;
import models.v_user_withdrawals;
import models.v_users;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import beartool.MD5;

import com.google.zxing.BarcodeFormat;
import com.shove.Convert;
import com.shove.code.Qrcode;
import com.shove.security.Encrypt;
import constants.Constants;
import constants.Constants.RechargeType;
import constants.DealType;
import constants.SupervisorEvent;
import constants.Templets;
import constants.UserEvent;
import constants.IPSConstants.IpsCheckStatus;
import play.Logger;
import play.cache.Cache;
import play.db.helper.JpaHelper;
import play.db.jpa.Blob;
import play.db.jpa.JPA;
import play.db.jpa.Model;
import play.libs.WS;
import play.mvc.Http.Cookie;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import utils.Arith;
import utils.CacheManager;
import utils.CharUtil;
import utils.DataUtil;
import utils.DateUtil;
import utils.EmailUtil;
import utils.ErrorInfo;
import utils.GopayUtils;
import utils.IDCardValidate;
import utils.NumberUtil;
import utils.PageBean;
import utils.RegexUtils;
import utils.SMSUtil;
import utils.Security;
import utils.ServiceFee;

/**
 * 用户的业务实体
 * 
 * @author cp
 * @version 6.0
 * @created 2014年3月21日 下午4:25:45
 */
public class User extends UserBase implements Serializable {
	public String simulateLogin = null;

	/**
	 * 缺少的字段 是否是有效会员（充值就是有效会员） 基本信息中的邮箱
	 */
	public long id;
	private long _id;

	public String sign;// 加密ID

	public String getSign() {
		return Security.addSign(this.id, Constants.USER_ID_SIGN);
	}

	public void setId(long id) {

		if (id <= 0) {
			this._id = -1;

			return;
		}

		Model user = null;

		/* 发布借款user实体部分字段填充 */
		if (this.createBid) {
			try {
				user = GenericModel.findById(id);
			} catch (Exception e) {
				Logger.info("用户setId填充时（createBid=true）：" + e.getMessage());
				this._id = -1;

				return;
			}

			t_users tuser = (t_users) user;
			this._id = tuser.id;
			this.name = tuser.name;
			this.vipStatus = tuser.vip_status;
			this.balance = tuser.balance;
			this.creditLine = tuser.credit_line;
			this.isAddBaseInfo = tuser.is_add_base_info;
			this.isEmailVerified = tuser.is_email_verified;
			this.mobile = tuser.mobile;
			this.email = tuser.email;

			return;
		}

		if (this.lazy) {

			try {
				user = GenericModel.findById(id);
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("用户setId填充时（lazy=true）：" + e.getMessage());
				this._id = -1;

				return;
			}
		} else {
			try {
				user = GenericModel.findById(id);
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("用户setId填充时：" + e.getMessage());
				this._id = -1;

				return;
			}
		}

		if (user == null) {
			this._id = -1;
			return;

		}

		if (this.lazy) {
			this.setInformationLazy(user);
		} else {
			this.setInformation(user);
		}

	}

	public long getId() {
		return _id;
	}

	/**
	 * 给所有属性赋值
	 * 
	 * @param obj
	 */
	public void setInformation(Model model) {
		if (!(model instanceof v_users)) {
			return;
		}

		v_users user = (v_users) model;

		this._id = user.id;
		this.time = user.time;
		this._name = user.name;
		this.photo = user.photo;
		this.realityName = user.reality_name;
		this._password = user.password;
		this.passwordContinuousErrors = user.password_continuous_errors;
		this.isPasswordErrorLocked = user.is_password_error_locked;
		this.passwordErrorLockedTime = user.password_error_locked_time;
		this._payPassword = user.pay_password;
		this.payPasswordContinuousErrors = user.pay_password_continuous_errors;
		this.isPayPasswordErrorLocked = user.is_pay_password_error_locked;
		this.payPasswordErrorLockedTime = user.pay_password_error_locked_time;
		this.isSecretSet = user.is_secret_set;
		this.secretSetTime = user.secret_set_time;
		this.isAllowLogin = user.is_allow_login;

		this.secretQuestionId1 = user.secret_question_id1;
		this.secretQuestionId2 = user.secret_question_id2;
		this.secretQuestionId3 = user.secret_question_id3;

		this.answer1 = user.answer1;
		this.answer2 = user.answer2;
		this.answer3 = user.answer3;

		this.questionName1 = user.question_name1;
		this.questionName2 = user.question_name2;
		this.questionName3 = user.question_name3;

		this.loginCount = user.login_count;
		this.lastLoginTime = user.last_login_time;
		this.lastLoginIp = user.last_login_ip;
		this.lastLogoutTime = user.last_logout_time;
		this.email = user.email;
		this.isEmailVerified = user.is_email_verified;
		this.telephone = user.telephone;
		this.mobile = user.mobile;
		this.isMobileVerified = user.is_mobile_verified;

		this.mobile1 = user.mobile1;
		this.mobile2 = user.mobile2;
		this.email2 = user.email2;
		this.idNumber = user.id_number;
		this.address = user.address;
		this.postcode = user.postcode;
		this._sex = user.sex;
		this._birthday = user.birthday;
		this.cityId = user.city_id;
		this.familyAddress = user.family_address;
		this.familyTelephone = user.family_telephone;
		this.company = user.company;
		this.companyAddress = user.company_address;
		this.officeTelephone = user.office_telephone;
		this.faxNumber = user.fax_number;
		this.educationId = user.education_id;
		this.maritalId = user.marital_id;
		this.houseId = user.house_id;
		this.carId = user.car_id;
		this.isAddBaseInfo = user.is_add_base_info;

		/**
		 * 0 = 已擦除状态; 1 = 正常状态;
		 */
		this.isErased = user.is_erased;

		this.recommendUserId = user.recommend_user_id;
		this.recommendRewardType = user.recommend_reward_type;

		this.masterIdentity = user.master_identity;
		this.vipStatus = user.vip_status;
		this.balance = user.balance;
		this.balance2 = user.balance2;
		this.freeze = user.freeze;
		this.creditLine = user.credit_line;
		this.lastCreditLine = user.last_credit_line;
		this.score = user.score;
		this.creditScore = user.credit_score;
		this.creditLevelId = user.credit_level_id;

		// this.isActive = user.isActive; //是否是活跃 会员

		this.isRefusedReceive = user.is_refused_receive;
		this.refusedTime = user.refused_time;
		this.refusedReason = user.refused_reason;

		this.isBlacklist = user.is_blacklist;
		this.joinedTime = user.joined_time;
		this.joinedReason = user.joined_reason;

		this.assignedTime = user.assigned_time;
		this.assignedToSupervisorId = user.assigned_to_supervisor_id;

		this.ipsAcctNo = user.ips_acct_no;
		this.ipsBidAuthNo = user.ips_bid_auth_no;
		this.ipsRepayAuthNo = user.ips_repay_auth_no;

		this.cityName = user.city_name;
		this.provinceId = user.province_Id;
		this.provinceName = user.province_name;
		this.educationName = user.education_name;
		this.maritalName = user.marital_name;
		this.houseName = user.house_name;
		this.carName = user.car_name;
		this.qrcode = user.qr_code;
	}

	/**
	 * lazy 赋值
	 * 
	 * @param user
	 */
	public void setInformationLazy(Model model) {
		if (!(model instanceof v_user_users)) {
			return;
		}

		v_user_users user = (v_user_users) model;

		this._id = user.id;
		this.time = user.time;
		this._name = user.name;
		this.creditScore = user.credit_score;
		this.photo = user.photo;
		this.realityName = user.reality_name;

		this.email = user.email;
		this.isEmailVerified = user.is_email_verified;

		this.mobile = user.mobile;
		this.isMobileVerified = user.is_mobile_verified;

		this.idNumber = user.id_number;

		this._sex = user.sex;
		this._birthday = user.birthday;

		this.cityName = user.city_name;
		this.provinceName = user.province_name;
		this.educationName = user.education_name;
		this.maritalName = user.marital_name;
		this.houseName = user.house_name;
		this.carName = user.car_name;
	}

	public Date time;

	public String name;
	private String _name;

	/**
	 * 根据name，填充数据
	 * 
	 * @param name
	 * @return -1执行失败 0执行成功
	 */
	public void setName(String name) {
		v_users user = null;

		try {
			user = GenericModel.find("name = ? or email = ? or mobile = ?", name, name,name).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("注册时,根据用户名查询用户信息时：" + e.getMessage());
		}

		if (user == null || user.id <= 0) {
			this._name = name;
			this._id = -1;
			return;
		}

		this.setInformation(user);
	}

	public String getName() {
		return this._name;

	}

	public String photo;
	public String realityName;

	public String password;
	private String _password;

	/**
	 * 用户密码加密
	 * 
	 * @param password
	 */
	public void setPassword(String password) {
		this._password = Encrypt.MD5(password + Constants.ENCRYPTION_KEY);
	}

	public String getPassword() {
		return this._password;
	}

	public int passwordContinuousErrors;
	public boolean isPasswordErrorLocked;
	public Date passwordErrorLockedTime;
	public String payPassword;
	private String _payPassword;

	/**
	 * 用户支付密码加密
	 * 
	 * @param password
	 */
	public void setPayPassword(String payPassword) {
		this._payPassword = Encrypt.MD5(payPassword + Constants.ENCRYPTION_KEY);
	}

	public String getPayPassword() {
		return this._payPassword;

	}

	public int payPasswordContinuousErrors;
	public boolean isPayPasswordErrorLocked;
	public Date payPasswordErrorLockedTime;

	public boolean isSecretSet;
	public Date secretSetTime;

	public long secretQuestionId1;
	public long secretQuestionId2;
	public long secretQuestionId3;

	public String questionName1;
	public String questionName2;
	public String questionName3;

	public String answer1;
	public String answer2;
	public String answer3;

	/**
	 * 是否允许登录(管理员是否锁定 true锁定)
	 */
	public boolean isAllowLogin;
	public long loginCount;
	public Date lastLoginTime;
	public String lastLoginIp;
	public String thisLoginIp;
	public Date lastLogoutTime;

	public String email;
	// private String _email;

	/**
	 * 根据email，填充数据
	 * 
	 * @param email
	 * @return -1执行失败 0执行成功
	 */
	// public long setEmail(String email, ErrorInfo info){
	// info.clear();
	//
	// v_users user = null;
	// try {
	// user = v_users.find("byEmail",email).first();
	// }catch (Exception e) {
	// e.printStackTrace();
	// Logger.info("注册时,根据用户邮箱查询用户信息时："+e.getMessage());
	// info.code = -1;
	// info.msg = "用户邮箱不存在";
	// }
	//
	// if(user==null){
	// this._email = email;
	// this._id = -1;
	// return -1;
	// }
	//
	// this.setInformation(user);
	//
	// info.code = 0;
	// return info.code;
	//
	// }

	// public String getEmail() {
	// return _email;
	//
	// }

	public boolean isEmailVerified;

	public String telephone;
	public String mobile;
	public boolean isMobileVerified;

	public String mobile2;
	public String idNumber;
	public String address;
	public String postcode;
	public String sex;
	private int _sex;

	public String getSex() {
		if (Constants.ONE == _sex) {
			return Constants.MAN;
		}

		if (Constants.TWO == _sex) {
			return Constants.WOMAN;
		}

		if (Constants.THREE == _sex) {
			return Constants.UNKNOWN;
		}
		return null;

	}

	public void setSex(int sex) {
		this._sex = sex;
	}

	public Date birthday;
	private Date _birthday;

	public Date getBirthday() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - this._age);
		this.birthday = calendar.getTime();
		return calendar.getTime();
	}

	public void setBirthday(Date birthday) {
		this._birthday = birthday;
	}

	public int age;
	private int _age;

	public int getAge() {

		if (_birthday == null) {
			return -1;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(_birthday);
		Calendar currentDate = Calendar.getInstance();

		return currentDate.get(Calendar.YEAR) - calendar.get(Calendar.YEAR);

	}

	public void setAge(int age) {
		this._age = age;
	}

	public int cityId;
	public String cityName;
	public int provinceId;
	public String provinceName;

	public String familyAddress;
	public String familyTelephone;
	public String company;
	public String companyAddress;
	public String officeTelephone;
	public String faxNumber;
	public int educationId;
	public String educationName;

	public int maritalId;
	public String maritalName;

	public int houseId;
	public String houseName;

	public int carId;
	public String carName;

	public String email2;
	public boolean isAddBaseInfo;
	public boolean isErased;

	public long recommendUserId;
	public String recommendUserName;

	public String spreadLink;

	public String getSpreadLink() {
		return Constants.BASE_URL + "register?un="
				+ Encrypt.encrypt3DES(this.name, Constants.ENCRYPTION_KEY);
	}

	// public void setRecommendUserId(long recommendUserId) {
	// this.recommendUserId = recommendUserId;
	// this.recommendUserName = User.queryUserNameById(recommendUserId, new
	// ErrorInfo());
	// }

	public void setRecommendUserName(String recommendUserName) {
		this.recommendUserName = recommendUserName;
		this.recommendUserId = User.queryIdByUserName(recommendUserName,
				new ErrorInfo());
	}

	public int recommendRewardType;

	/**
	 * 0 未确定 1 借款会员 2 投资会员 3 复合会员
	 */
	public int masterIdentity;
	public boolean vipStatus;
	public double balance;
	public double balance2;
	public double freeze;
	public double creditLine;
	public double lastCreditLine;
	public int score;
	public int creditScore;
	public int creditLevelId;

	public boolean isRefusedReceive;
	public Date refusedTime;
	public String refusedReason;

	public boolean isBlacklist;
	public Date joinedTime;
	public String joinedReason;

	public Date assignedTime;
	public long assignedToSupervisorId;

	public boolean createBid;
	public boolean lazy;

	public String qqKey;
	private String _qqKey;

	public String ipsAcctNo;
	public String ipsBidAuthNo;
	public String ipsRepayAuthNo;

	public String getQqKey() {
		return this._qqKey;
	}

	public void setQqKey(String qqKey) {
		this._qqKey = qqKey;
	}

	public String weiboKey;
	private String _weiboKey;

	public String getWeiboKey() {
		return this._weiboKey;
	}

	public void setWeiboKey(String weiboKey) {
		this._weiboKey = weiboKey;
	}

	public String qrcode;

	/**
	 * 用户已上传的审核资料
	 */
	public List<Long> auditItems;

	public List<Long> getAuditItems() {

		return UserAuditItem.queryUserAuditItem(this.id, true);
	}

	// /**
	// * 用户所有上传资料最新记录
	// */
	// public List<UserAuditItem> userAuditItemList;
	//
	// public List<UserAuditItem> getUserAuditItemList(){
	//
	// return UserAuditItem.queryUserAllAuditItem(this.id);
	// }

	/**
	 * 信用等级
	 */
	public CreditLevel myCredit;

	public CreditLevel getMyCredit() {

		ErrorInfo error = new ErrorInfo();

		return CreditLevel.queryUserCreditLevel(this.id, error);
	}

	/**
	 * 金额
	 */
	public v_user_for_details balanceDetail;

	public v_user_for_details getBalanceDetail() {
		return GenericModel.findById(this.id);
	}

	public v_user_scores userScore;

	public v_user_scores getUserScore() {
		return GenericModel.findById(this.id);
	}

	public User() {

	}

	/**
	 * 注册
	 * 
	 * @return
	 */
	public int register(ErrorInfo error) {
		error.clear();

		t_users user = new t_users();
		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();
		String keyWord = backstageSet.keywords;

		if (StringUtils.isNotBlank(keyWord)) {
			String[] keywords = keyWord.split("，");

			for (String word : keywords) {
				if (this._name.contains(word)) {
					error.code = -1;
					error.msg = "对不起，注册的用户名包含敏感词汇，请重新输入用户名";

					return error.code;
				}
			}
		}

		if (StringUtils.isNotBlank(this.recommendUserName)) {

			if (this.recommendUserId > 0) {
				user.recommend_user_id = this.recommendUserId;
				user.recommend_reward_type = backstageSet.cpsRewardType;
				user.recommend_time = new Date();
			}
		}

		user.name = this._name;
		user.mobile=this.mobile;
		user.is_mobile_verified=true;
		user.time = new Date();
		user.password = this._password;
		user.email = this.email;
		user.credit_line = backstageSet.initialAmount;
		user.last_credit_line = backstageSet.initialAmount;
		user.photo = Constants.DEFAULT_PHOTO;
		user.qq_key = this.qqKey;
		user.weibo_key = this.weiboKey;

		String uuid = UUID.randomUUID().toString();
		Qrcode code = new Qrcode();

		try {
			Blob blob = new Blob();
			Qrcode.create(this.getSpreadLink(), BarcodeFormat.QR_CODE, 100, 100,
					new File(Blob.getStore(), uuid).getAbsolutePath(), "png");
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("创建二维码图片失败" + e.getMessage());
			error.code = -5;
			error.msg = "对不起，由于平台出现故障，此次注册失败！";

			return error.code;
		}

		user.qr_code = uuid;

		try {
			user.save();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("注册时，保存注册信息时：" + e.getMessage());
			error.code = -5;
			error.msg = "对不起，由于平台出现故障，此次注册失败！";

			return error.code;
		}

		this.id = user.id;

		String sign1 = Encrypt.MD5("" + this._id + 0.00 + 0.00
				+ Constants.ENCRYPTION_KEY);
		String sign2 = Encrypt.MD5("" + this._id + 0.00 + 0.00 + 0.00 + 0.00
				+ Constants.ENCRYPTION_KEY);

		String sql = "update t_users set sign1 = ?, sign2 = ? where id = ?";
		Query query = JPA.em().createQuery(sql).setParameter(1, sign1)
				.setParameter(2, sign2).setParameter(3, this.id);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次注册失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次注册失败！";
			return error.code;
		}

		DealDetail.userEvent(this.id, UserEvent.REGISTER, "注册成功", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		// 发送注册站内信
		TemplateStation message = new TemplateStation();
		message.id = Templets.M_REGISTER;
		String mcontent = message.content;
		mcontent = mcontent.replace("userName", this._name);
		if (message.status) {
			TemplateStation.addMessageTask(this.id, message.title, mcontent);
		}

		User.setCurrUser(this);

		error.code = 0;
		error.msg = "恭喜你，注册成功！";

		return error.code;
	}

	/**
	 * 登录
	 * 
	 * @param password
	 * @return
	 */
	public int login(String password, boolean encrypt, ErrorInfo error) {
		error.clear();
		int rows = 0;

		if (this.isAllowLogin) {
			error.code = -1;
			error.msg = "你已经被管理员禁止登录";

			return error.code;
		}

		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();
		String ip = DataUtil.getIp();

		if (backstageSet.isOpenPasswordErrorLimit == Constants.OPEN_LOCK
				&& this.isPasswordErrorLocked) {

			long lockTimes = backstageSet.lockingTime * 60 * 1000;
			long leftTime = lockTimes
					- (System.currentTimeMillis() - this.passwordErrorLockedTime
							.getTime());

			if (leftTime > 0) {
				error.code = -2;
				error.msg = "由于连续输入错误密码达到上限，用户已经被锁定";

				return error.code;
			}

			this.isPasswordErrorLocked = Constants.FALSE;
			this.passwordContinuousErrors = Constants.ZERO;
			this.passwordErrorLockedTime = null;

			DealDetail.userEvent(this.id, UserEvent.LOGIN, "解除锁定", error);

			if (error.code < 0) {
				JPA.setRollbackOnly();
				return error.code;
			}

		}

		EntityManager em = JPA.em();

		if (!encrypt) {
			// 密码传入为明文则进行加密处理
			password = Encrypt.MD5(password + Constants.ENCRYPTION_KEY);
		}

		if (!password.equalsIgnoreCase(this._password)) {
			error.msg = "对不起，您的密码有误!";

			if (backstageSet.isOpenPasswordErrorLimit == Constants.OPEN_LOCK) {
				this.passwordContinuousErrors += 1;

				if (this.passwordContinuousErrors >= backstageSet.passwordErrorCounts) {

					Query query = em
							.createQuery(
									"update t_users set last_login_time = ?, last_login_ip = ?,"
											+ "password_continuous_errors = ?, is_password_error_locked = ?,"
											+ "password_error_locked_time = ? where id = ?")
							.setParameter(1, new Date()).setParameter(2, ip)
							.setParameter(3, this.passwordContinuousErrors)
							.setParameter(4, Constants.TRUE)
							.setParameter(5, new Date())
							.setParameter(6, this.id);

					try {
						rows = query.executeUpdate();
					} catch (Exception e) {
						JPA.setRollbackOnly();
						e.printStackTrace();
						Logger.info("登录密码错误,锁定用户时：" + e.getMessage());
						error.code = -3;
						error.msg = "对不起，由于平台出现故障，此次登录失败！";

						return error.code;
					}

					if (rows == 0) {
						JPA.setRollbackOnly();
						error.code = -1;
						error.msg = "数据未更新";

						return error.code;
					}

					DealDetail.userEvent(this.id, UserEvent.LOGIN, "连续登录错误锁定",
							error);

					if (error.code < 0) {
						JPA.setRollbackOnly();
						return error.code;
					}

					error.code = -3;
					error.msg = "由于连续输入错误密码达到上限，账号已被锁定，请于"
							+ backstageSet.lockingTime + "分钟后登录";

					return error.code;

				}

				Query query = em
						.createQuery(
								"update t_users set last_login_time = ?, last_login_ip = ?,"
										+ "password_continuous_errors = ?, is_password_error_locked = ?,"
										+ "password_error_locked_time = ? where id = ?")
						.setParameter(1, new Date())
						.setParameter(2, "127.0.0.1")
						.setParameter(3, this.passwordContinuousErrors)
						.setParameter(4, this.isPasswordErrorLocked)
						.setParameter(5, null).setParameter(6, this.id);

				try {
					rows = query.executeUpdate();
				} catch (Exception e) {
					JPA.setRollbackOnly();
					e.printStackTrace();
					Logger.info("登录密码错误,更新用户错误次数时：" + e.getMessage());
					error.code = -5;
					error.msg = "对不起，由于平台出现故障，此次登录失败！";

					return error.code;
				}

				if (rows == 0) {
					JPA.setRollbackOnly();
					error.code = -1;
					error.msg = "数据未更新";

					return error.code;
				}

				error.code = -6;

				return error.code;
			}

			error.code = -7;

			return error.code;
		}

		if (backstageSet.isOpenPasswordErrorLimit == Constants.OPEN_LOCK) {
			Query saveUser = em
					.createQuery(
							"update t_users set last_login_time = ?, last_login_ip = ?,"
									+ "login_count = login_count + 1, password_continuous_errors = ?, is_password_error_locked = ?,"
									+ "password_error_locked_time = ? where id = ?")
					.setParameter(1, new Date()).setParameter(2, ip)
					.setParameter(3, Constants.ZERO)
					.setParameter(4, Constants.FALSE).setParameter(5, null)
					.setParameter(6, this.id);

			try {
				rows = saveUser.executeUpdate();
			} catch (Exception e) {
				JPA.setRollbackOnly();
				e.printStackTrace();
				Logger.info("登录时,更新用户登录信息时：" + e.getMessage());
				error.code = -5;
				error.msg = "对不起，由于平台出现故障，此次登录失败！";

				return error.code;
			}

			if (rows == 0) {
				JPA.setRollbackOnly();
				error.code = -1;
				error.msg = "数据未更新";

				return error.code;
			}
		} else {
			Query saveUser = em
					.createQuery(
							"update t_users set last_login_time = ?, last_login_ip = ?"
									+ ",login_count = login_count + 1 where id = ?")
					.setParameter(1, new Date()).setParameter(2, ip)
					.setParameter(3, this.id);

			try {
				rows = saveUser.executeUpdate();
			} catch (Exception e) {
				JPA.setRollbackOnly();
				e.printStackTrace();
				Logger.info("登录时,更新用户登录信息时：" + e.getMessage());
				error.code = -6;
				error.msg = "对不起，由于平台出现故障，此次登录失败！";

				return error.code;
			}

			if (rows == 0) {
				JPA.setRollbackOnly();
				error.code = -1;
				error.msg = "数据未更新";

				return error.code;
			}
		}

		DealDetail.userEvent(this.id, UserEvent.LOGIN, "登录成功", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		setCurrUser(this);

		utils.Cache cache = CacheManager.getCacheInfo("online_user_" + this.id
				+ "");

		if (null == cache) {
			cache = new utils.Cache();
			long timeout = 1800000;// 单位毫秒
			CacheManager.putCacheInfo("online_user_" + this.id, cache, timeout);
		}

		error.code = 0;

		return error.code;
	}

	/**
	 * 编辑信息
	 * 
	 * @return
	 */
	public void edit(User user, ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(this.realityName)) {
			error.code = -1;
			error.msg = "请输入真实姓名";

			return;
		}

		if (this._sex == 0 || this._sex == 3) {
			error.code = -1;
			error.msg = "请选择性别";

			return;
		}

		if (this._age < 10 || this._age > 120) {
			error.code = -1;
			error.msg = "请输入正确的年龄";

			return;
		}

		if (StringUtils.isBlank(this.idNumber)) {
			error.code = -1;
			error.msg = "请输入身份证号码";
		}

		if (this.cityId == 0) {
			error.code = -1;
			error.msg = "请选择户口所在地";

			return;
		}

		if (this.educationId == 0) {
			error.code = -1;
			error.msg = "请选择文化程度";

			return;
		}

		if (this.maritalId == 0) {
			error.code = -1;
			error.msg = "请选择婚姻情况";

			return;
		}

		if (this.carId == 0) {
			error.code = -1;
			error.msg = "请选择购车情况";

			return;
		}

		if (this.houseId == 0) {
			error.code = -1;
			error.msg = "请选择购房情况";

			return;
		}


		if (!"".equals(IDCardValidate.chekIdCard(this._sex, this.idNumber))) {
			error.code = -1;
			error.msg = "请输入正确的身份证号码";

			return;
		}



		if (!this.idNumber.equals(User.currUser().idNumber)) {
			User.isIDNumberExist(this.idNumber, error);

			if (error.code < 0) {
				return;
			}
		}

		EntityManager em = JPA.em();

		if (this.isAddBaseInfo) {
			error.code = -1;
			error.msg = "对不起，基本资料不能进行修改";

			return;
		}
		Query query = em
				.createQuery(
						"update t_users set reality_name=?,sex=?,birthday=?,"
								+ "id_number=?,city_id=?,education_id=?,marital_id=?,car_id=?,house_id=?,"
								+ "is_add_base_info=?  where id = ? and is_add_base_info = 0")
				.setParameter(1, this.realityName).setParameter(2, this._sex)
				.setParameter(3, this.birthday).setParameter(4, this.idNumber)
				.setParameter(5, this.cityId).setParameter(6, this.educationId)
				.setParameter(7, this.maritalId).setParameter(8, this.carId)
				.setParameter(9, this.houseId).setParameter(10, Constants.TRUE).setParameter(11, this.id);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("首次编辑基本信息时,保存用户编辑的信息时：" + e.getMessage());
			error.code = -2;
			error.msg = "对不起，由于平台出现故障，此次编辑信息保存失败！";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		DealDetail.userEvent(this.id, UserEvent.ADD_BASIC_INFORMATION,
				"添加用户资料", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return;
		}

		user.realityName = realityName;
		user._sex = this._sex;
		user._birthday = this.birthday;
		user.cityId = this.cityId;
		user.provinceId = queryProvince(this.cityId);
		user.educationId = this.educationId;
		user.maritalId = this.maritalId;
		user.carId = this.carId;
		user.idNumber = this.idNumber;
		user.houseId = this.houseId;
		user.mobile1 = this.mobile1;
		user.isAddBaseInfo = true;

		error.msg = "保存基本资料成功";
		error.code = 0;
	}

	/**
	 * 安全退出
	 * 
	 * @return
	 */
	public int logout(ErrorInfo error) {
		error.clear();

		EntityManager em = JPA.em();
		Query query = em
				.createQuery(
						"update t_users set last_logout_time = ? where id = ?")
				.setParameter(1, new Date()).setParameter(2, this.id);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("安全退出时,保存安全退出的信息时：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次安全退出信息保存失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.userEvent(this.id, UserEvent.LOGOUT, "安全退出", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		User.removeCurrUser();
		CacheManager.clearByKey("online_user_" + this.id);

		error.code = 0;

		return error.code;
	}

	/**
	 * 更新安全问题
	 * 
	 * @param info
	 * @return
	 */
	public void updateSecretQuestion(boolean flag, ErrorInfo error) {
		error.clear();

		if (this.secretQuestionId1 < 1 || this.secretQuestionId2 < 1
				|| this.secretQuestionId3 < 1) {
			error.code = -1;
			error.msg = "请选择安全问题";

			return;
		}

		if (StringUtils.isBlank(this.answer1)
				|| StringUtils.isBlank(this.answer2)
				|| StringUtils.isBlank(this.answer3)) {
			error.code = -1;
			error.msg = "安全问题答案不能为空";

			return;
		}

		EntityManager em = JPA.em();
		Query query = null;

		query = em
				.createQuery(
						"update t_users set secret_question_id1=?,"
								+ "secret_question_id2=?,secret_question_id3=?,answer1=?,answer2=?,answer3=?,"
								+ "is_secret_set=?,secret_set_time=? where id=?")
				.setParameter(1, this.secretQuestionId1)
				.setParameter(2, this.secretQuestionId2)
				.setParameter(3, this.secretQuestionId3)
				.setParameter(4, this.answer1).setParameter(5, this.answer2)
				.setParameter(6, this.answer3).setParameter(7, Constants.TRUE)
				.setParameter(8, new Date()).setParameter(9, this.id);
		int rows1 = 0;
		int rows2 = 0;

		try {
			rows1 = query.executeUpdate();
			rows2 = JpaHelper.execute(
					"update t_dict_secret_questions set use_count = use_count + 1 where "
							+ "id = ? or id = ? or id = ?",
					this.secretQuestionId1, this.secretQuestionId2,
					this.secretQuestionId2).executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("保存安全问题时：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次保存安全问题失败！";

			return;
		}

		if (rows1 == 0 || rows2 == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		DealDetail.userEvent(this.id, flag ? UserEvent.EDIT_QUESTION
				: UserEvent.RESET_QUESTION, flag ? "修改安全问题" : "重置安全问题", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return;
		}

		this.isSecretSet = true;
		this.questionName1 = SecretQuestion.queryQuestionById(
				this.secretQuestionId1, error);
		this.questionName2 = SecretQuestion.queryQuestionById(
				this.secretQuestionId2, error);
		this.questionName3 = SecretQuestion.queryQuestionById(
				this.secretQuestionId3, error);

		setCurrUser(this);

		error.code = 0;
		error.msg = "安全问题设置成功";

	}

	/**
	 * 安全问题校验
	 * 
	 * @param secretQuestionId1
	 * @param secretQuestionId2
	 * @param secretQuestionId3
	 * @param questionName1
	 * @param questionName2
	 * @param questionName3
	 * @param info
	 * @return
	 */
	public int verifySafeQuestion(String answer1, String answer2,
			String answer3, ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(answer1) || StringUtils.isBlank(answer2)
				|| StringUtils.isBlank(answer3)) {
			error.code = -1;
			error.msg = "请输入安全问题答案";

			return error.code;
		}

		if (!answer1.equals(this.answer1) || !answer2.equals(this.answer2)
				|| !answer3.equals(this.answer3)) {
			error.code = -1;
			error.msg = "对不起，你的安全问题回答错误！";

			return error.code;
		}

		error.code = 0;
		error.msg = "安全问题回答正确";

		return error.code;
	}

	/**
	 * 添加(重置)支付密码
	 * 
	 * @param payPassword1
	 * @param payPassword2
	 * @param info
	 * @return
	 */
	public int addPayPassword(boolean flag, String payPassword1,
			String payPassword2, ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(payPassword1)
				|| StringUtils.isBlank(payPassword2)) {
			error.code = -1;
			error.msg = "请输入交易密码";

			return error.code;
		}
		if (!RegexUtils.isValidPassword(payPassword1)) {
			error.code = -1;
			error.msg = "请设置符合要求的密码";

			return error.code;
		}
		if (!payPassword1.equals(payPassword2)) {
			error.code = -1;
			error.msg = "对不起，两次输入密码不一致！";

			return error.code;
		}

		String payPassword = Encrypt.MD5(payPassword1
				+ Constants.ENCRYPTION_KEY);

		if (this.password.equalsIgnoreCase(payPassword)) {
			error.code = -1;
			error.msg = "对不起，为了账户安全，请勿将交易密码设置和登录密码一致！";

			return error.code;
		}

		String sql = "update t_users set pay_password = ? where id = ?";
		EntityManager em = JPA.em();
		Query query = em.createQuery(sql).setParameter(1, payPassword)
				.setParameter(2, this.id);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("添加交易密码时：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次保存交易密码失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.userEvent(this.id, flag ? UserEvent.ADD_PAY_PASSWORD
				: UserEvent.RESET_PAY_PASSWORD, flag ? "添加交易密码" : "重置交易密码",
				error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		this._payPassword = payPassword;

		setCurrUser(this);

		error.msg = "交易密码添加成功！";

		return 0;
	}

	/**
	 * 修改支付密码
	 * 
	 * @param oldPayPassword
	 *            旧密码
	 * @param newPayPassword1
	 *            新密码1
	 * @param newPayPassword2
	 *            新密码1
	 * @param info
	 * @return
	 */
	public int editPayPassword(String oldPayPassword, String newPayPassword1,
			String newPayPassword2, ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(oldPayPassword)) {
			error.code = -1;
			error.msg = "请输入原交易密码";

			return error.code;
		}

		if (StringUtils.isBlank(newPayPassword1)
				|| StringUtils.isBlank(newPayPassword2)) {
			error.code = -1;
			error.msg = "请输入新交易密码";

			return error.code;
		}

		if (!RegexUtils.isValidPassword(newPayPassword1)) {
			error.code = -1;
			error.msg = "请设置符合要求的密码";

			return error.code;
		}

		if (!newPayPassword1.equals(newPayPassword2)) {
			error.code = -1;
			error.msg = "对不起，两次输入密码不一致！";

			return error.code;
		}

		String oldPassword = Encrypt.MD5(oldPayPassword
				+ Constants.ENCRYPTION_KEY);
		String payPassword = Encrypt.MD5(newPayPassword1
				+ Constants.ENCRYPTION_KEY);

		if (!oldPassword.equals(this._payPassword)) {
			error.code = -2;
			error.msg = "对不起，密码错误！";

			return error.code;
		}

		if (this._password.equalsIgnoreCase(payPassword)) {
			error.code = -1;
			error.msg = "对不起，为了账户安全，请勿将交易密码设置和登录密码一致！";

			return error.code;
		}

		String sql = "update t_users set pay_password = ? where id = ?";
		EntityManager em = JPA.em();
		Query query = em.createQuery(sql).setParameter(1, payPassword)
				.setParameter(2, this.id);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("更新交易密码时：" + e.getMessage());
			error.code = -3;
			error.msg = "对不起，由于平台出现故障，此次更新交易密码失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.userEvent(this.id, UserEvent.EDIT_PAY_PASSWORD, "修改交易密码",
				error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		Logger.info("用户可用金额：" + this.balanceDetail.user_amount);
		Logger.info("用户冻结金额：" + this.balanceDetail.freeze);

		setCurrUser(this);

		error.code = 0;
		error.msg = "交易密码修改成功！";

		return error.code;
	}

	/**
	 * 校验支付密码
	 * 
	 * @param payPassword
	 * @param error
	 * @return
	 */
	public int verifyPayPassword(String payPassword, ErrorInfo error) {
		error.clear();

		if (this._payPassword == null || this._payPassword == "") {
			error.code = -1;
			error.msg = "对不起，你还未设置交易密码，请先设置交易密码！";

			return error.code;

		}

		if (!this._payPassword.equalsIgnoreCase(Encrypt.MD5(payPassword
				+ Constants.ENCRYPTION_KEY))) {
			error.code = -1;
			error.msg = "对不起，你的交易密码不正确！";

			return error.code;
		}

		error.code = 0;

		return error.code;

	}

	/**
	 * 更换头像
	 * 
	 * @param file
	 *            上传的文件
	 * @return
	 */
	public int editPhoto(ErrorInfo error) {
		error.clear();
		int rows = 0;

		try {
			rows = DataUtil.update("t_users", new String[] { "photo" },
					new String[] { "id" },
					new Object[] { this.photo, this._id }, error);
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("更换头像时" + e.getMessage());

			error.code = -1;
			error.msg = "更换头像时出现异常！";

			new User().id = User.currUser().id;

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.userEvent(this.id, UserEvent.EDIT_PHOTO, "更换头像", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		setCurrUser(this);

		error.code = 0;
		error.msg = "更换头像成功！";

		return error.code;
	}

	/**
	 * 修改密码
	 * 
	 * @param oldPassword
	 *            旧密码
	 * @param newPassword1
	 *            新密码1
	 * @param newPassword2
	 *            新密码2
	 * @return
	 */
	public int editPassword(String oldPassword, String newPassword1,
			String newPassword2, ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(oldPassword)) {
			error.code = -1;
			error.msg = "请输入原密码";

			return error.code;
		}

		if (StringUtils.isBlank(newPassword1)
				|| StringUtils.isBlank(newPassword2)) {
			error.code = -1;
			error.msg = "密码不能为空";

			return error.code;
		}

		if (!newPassword1.equals(newPassword2)) {
			error.code = -1;
			error.msg = "对不起，两次密码输入不一致！";

			return error.code;
		}

		if (!RegexUtils.isValidPassword(newPassword1)) {
			error.code = -1;
			error.msg = "请设置符合要求的密码";

			return error.code;
		}

		if (!Encrypt.MD5(oldPassword + Constants.ENCRYPTION_KEY)
				.equalsIgnoreCase(this._password)) {
			error.code = -2;
			error.msg = "密码错误！";

			return error.code;
		}

		EntityManager em = JPA.em();
		String sql = "update t_users set password = ? where id = ?";
		Query query = em
				.createQuery(sql)
				.setParameter(1,
						Encrypt.MD5(newPassword1 + Constants.ENCRYPTION_KEY))
				.setParameter(2, this._id);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("修改密码时时,更新保存用户密码时：" + e.getMessage());
			error.code = -3;
			error.msg = "对不起，由于平台出现故障，此次密码修改保存失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.userEvent(this.id, UserEvent.EDIT_PASSWORD, "修改密码", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		logout(error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		error.code = 0;
		error.msg = "密码修改成功，请重新登录！";

		return error.code;
	}

	/**
	 * 通过手机重置用户密码
	 * 
	 * @param oldPassword
	 * @param newPassword1
	 * @param newPassword2
	 * @param error
	 * @return
	 */
	public static void updatePasswordByMobile(String mobile, String code,
			String password, String confirmPassword, ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(mobile)) {
			error.code = -1;
			error.msg = "请输入手机号码";

			return;
		}

		if (StringUtils.isBlank(code)) {
			error.code = -1;
			error.msg = "请输入验证码";

			return;
		}

		if (StringUtils.isBlank(password)) {
			error.code = -1;
			error.msg = "请输入新密码";

			return;
		}

		if (StringUtils.isBlank(confirmPassword)) {
			error.code = -1;
			error.msg = "请输入确认密码";

			return;
		}

		if (!RegexUtils.isMobileNum(mobile)) {
			error.code = -1;
			error.msg = "请输入正确的手机号码";

			return;
		}

		if (!password.equals(confirmPassword)) {
			error.code = -1;
			error.msg = "两次输入密码不一致";

			return;
		}

		long userId = User.queryIdByMobile(mobile, error);

		if (error.code < 0) {
			error.code = -1;
			error.msg = "该手机号码不存在";

			return;
		}

		if (Constants.CHECK_CODE) {
			String cCode = (Cache.get(mobile)).toString();

			if (cCode == null) {
				error.code = -1;
				error.msg = "验证码已失效，请重新点击发送验证码";

				return;
			}

			if (!code.equals(cCode)) {
				error.code = -1;
				error.msg = "手机验证错误";

				return;
			}
		}

		EntityManager em = JPA.em();
		String sql = "update t_users set password = ? where id = ?";
		Query query = em
				.createQuery(sql)
				.setParameter(1,
						Encrypt.MD5(password + Constants.ENCRYPTION_KEY))
				.setParameter(2, userId);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("修改密码时时,更新保存用户密码时：" + e.getMessage());
			error.code = -3;
			error.msg = "对不起，由于平台出现故障，此次密码修改保存失败！";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		DealDetail.userEvent(userId, UserEvent.RESET_PASSWORD_MOBILE,
				"通过手机重置用户密码", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return;
		}

		error.code = 0;
		error.msg = "密码修改成功，下次登录启用新密码！";
	}

	public static void updatePasswordByMobileApp(String mobile,
			String password, ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(password)) {
			error.code = -1;
			error.msg = "请输入新密码";

			return;
		}

		if (!RegexUtils.isMobileNum(mobile)) {
			error.code = -1;
			error.msg = "请输入正确的手机号码";

			return;
		}

		long userId = User.queryIdByMobile(mobile, error);

		if (error.code < 0) {
			error.code = -1;
			error.msg = "该手机号码不存在";

			return;
		}

		EntityManager em = JPA.em();
		String sql = "update t_users set password = ? where id = ?";
		Query query = em
				.createQuery(sql)
				.setParameter(1,
						Encrypt.MD5(password + Constants.ENCRYPTION_KEY))
				.setParameter(2, userId);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("修改密码时时,更新保存用户密码时：" + e.getMessage());
			error.code = -3;
			error.msg = "对不起，由于平台出现故障，此次密码修改保存失败！";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		DealDetail.userEvent(userId, UserEvent.RESET_PASSWORD_MOBILE,
				"通过手机重置用户密码", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return;
		}

		error.code = 0;
		error.msg = "密码修改成功，下次登录启用新密码！";
	}

	/**
	 * 通过邮箱重置用户密码
	 * 
	 * @param password
	 * @param confirmPassword
	 * @param error
	 */
	public void updatePasswordByEmail(String password, String confirmPassword,
			ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(password)) {
			error.code = -1;
			error.msg = "请输入新密码";

			return;
		}

		if (StringUtils.isBlank(confirmPassword)) {
			error.code = -1;
			error.msg = "请输入确认密码";

			return;
		}

		if (!password.equals(confirmPassword)) {
			error.code = -1;
			error.msg = "两次输入密码不一致";

			return;
		}

		EntityManager em = JPA.em();
		String sql = "update t_users set password = ? where id = ?";
		Query query = em
				.createQuery(sql)
				.setParameter(1,
						Encrypt.MD5(password + Constants.ENCRYPTION_KEY))
				.setParameter(2, this.id);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("修改密码时时,更新保存用户密码时：" + e.getMessage());
			error.code = -3;
			error.msg = "对不起，由于平台出现故障，此次密码修改保存失败！";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		DealDetail.userEvent(this.id, UserEvent.RESET_PASSWORD_EMAIL,
				"通过邮箱重置用户密码", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return;
		}

		if (currUser() != null) {
			logout(error);

			if (error.code < 0) {
				JPA.setRollbackOnly();
				return;
			}
		}

		error.code = 0;
		error.msg = "重置密码成功，下次登录启用新密码！";
	}

	/**
	 * 修改手机号码
	 * 
	 * @param mobile
	 *            修改后的手机号码
	 * @return 0运行成功
	 */
	public int editMobile(String code, ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(this.mobile)) {
			error.code = -1;
			error.msg = "请输入手机号码";

			return error.code;
		}

		if (!RegexUtils.isMobileNum(this.mobile)) {
			error.code = -1;
			error.msg = "请输入正确的手机号码";

			return error.code;
		}

		if (Constants.CHECK_CODE) {
			String cCode = Cache.get(mobile) + "";

			if (cCode == null) {
				error.code = -1;
				error.msg = "验证码已失效，请重新点击发送验证码";

				return error.code;
			}
			if (!code.equals(cCode)) {
				error.code = -1;
				error.msg = "手机验证错误";

				return error.code;
			}
		}

		isMobileExist(this.mobile, error);

		if (error.code < 0) {
			return error.code;
		}

		EntityManager em = JPA.em();
		String sql = "update t_users set mobile = ?,is_mobile_verified = 1 where id = ?";
		Query query = em.createQuery(sql).setParameter(1, this.mobile)
				.setParameter(2, this._id);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("保存手机号码时：" + e.getMessage());
			error.code = -3;
			error.msg = "保存手机号码时出现异常";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.userEvent(this.id, UserEvent.EDIT_MOBILE, "修改绑定手机", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		setCurrUser(this);

		error.code = 0;
		error.msg = "安全手机绑定成功！";

		return 0;

	}

	/**
	 * 激活邮箱
	 * 
	 * @param error
	 */
	public void activeEmail(ErrorInfo error) {
		error.clear();

		if (this.isEmailVerified) {
			error.code = -1;
			error.msg = "你的邮箱已激活，无需再次激活";

			return;
		}

		int rows = 0;

		try {
			rows = JpaHelper
					.execute(
							"update t_users set is_email_verified = ? where id = ? and is_email_verified = 0",
							true, this.id).executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("激活邮箱时：" + e.getMessage());
			error.code = -1;
			error.msg = "激活邮箱时出现异常";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		this.isEmailVerified = true;
		DealDetail.userEvent(this.id, UserEvent.VERIFIED_EMAIL, "激活邮箱", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return;
		}

		// logout(error);

		error.code = 0;
		error.msg = "邮箱已激活，请登录！";
	}

	/**
	 * 管理员激活用户
	 * 
	 * @param supervisorId
	 * @param userId
	 * @param info
	 * @return
	 */
	public static int activeUserBySupervisor(long userId, ErrorInfo error) {
		error.clear();

		int code = DataUtil.update("t_users",
				new String[] { "is_email_verified" }, new String[] { "id" },
				new Object[] { Constants.TRUE, userId }, error);

		if (code < 0) {
			return code;
		}

		DealDetail.supervisorEvent(Supervisor.currSupervisor().id,
				SupervisorEvent.VERIFIED_EMAIL, "手动激活用户", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		error.code = 0;
		error.msg = "用户激活成功！";

		return error.code;
	}

	/**
	 * 修改邮箱
	 * 
	 * @param info
	 * @return
	 */
	public int editEmail(ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(email)) {
			error.code = -1;
			error.msg = "请输入邮箱";

			return error.code;
		}

		if (!RegexUtils.isEmail(email)) {
			error.code = -1;
			error.msg = "请输入正确的邮箱地址";

			return error.code;
		}

		User.isEmailExist(email, error);

		if (error.code < 0) {

			return error.code;
		}

		//if (!this.isEmailVerified) {
		//	error.code = -10; // -10确定用户之前的邮箱未激活
		//	error.msg = "您之前的邮箱未激活，不能进行重新绑定邮箱，是否重新发送激活邮件激活之前的邮箱";
        //
	   //		return error.code;
	   //	}

		String sql = "update t_users set email = ?, is_email_verified = 0 where id = ?";

		int rows = 0;

		try {
			rows = JpaHelper.execute(sql, this.email, this.id).executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("更新用户邮箱时：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次更新邮箱失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.userEvent(this.id, UserEvent.EDIT_EMAIL, "修改邮箱", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		setCurrUser(this);

		error.code = 0;
		error.msg = "安全邮箱绑定成功，系统将使用新的邮箱，请及时激活";

		return error.code;
	}

	/**
	 * 实名认证
	 * 
	 * @param realName
	 * @param idNumber
	 */
	public int checkRealName(String realName, String idNumber, ErrorInfo error) {
		error.clear();

		if (!CharUtil.isChinese(realName)) {
			error.code = -1;
			error.msg = "真实姓名必须是中文";

			return error.code;
		}

		if (!"".equals(IDCardValidate.chekIdCard(0, idNumber))) {
			error.code = -1;
			error.msg = "请输入正确的身份证号";

			return error.code;
		}

		String sql = "update t_users set reality_name = ?, id_number = ? where id = ?";
		int rows = 0;

		try {
			rows = JpaHelper.execute(sql, realName, idNumber, this.id)
					.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			Logger.info("实名认证：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次更新邮箱失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		this.realityName = realName;
		this.idNumber = idNumber;
		setCurrUser(this);

		error.clear();
		error.msg = "实名认证成功";

		return error.code;
	}

	/**
	 * 手机认证
	 * 
	 * @param mobile
	 * @param code
	 * @param error
	 */
	public int checkMoible(String mobile, String code, ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(mobile)) {
			error.code = -1;
			error.msg = "请输入手机号码";

			return error.code;
		}

		if (!RegexUtils.isMobileNum(mobile)) {
			error.code = -1;
			error.msg = "请输入正确的手机号码";

			return error.code;
		}

		if (Constants.CHECK_CODE) {
			String cCode = (String) Cache.get(mobile);

			if (cCode == null) {
				error.code = -1;
				error.msg = "验证码已失效，请重新点击发送验证码";

				return error.code;
			}

			if (!code.equals(cCode)) {
				error.code = -1;
				error.msg = "验证码错误";

				return error.code;
			}
		}

		isMobileExist(mobile, error);

		if (error.code < 0) {
			return error.code;
		}

		String sql = "update t_users set mobile = ?, is_mobile_verified = ? where id = ?";
		int rows = 0;

		try {
			rows = JpaHelper.execute(sql, mobile, true, this.id)
					.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			Logger.info("手机认证：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次更新邮箱失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		this.mobile = mobile;
		this.isMobileVerified = true;
		setCurrUser(this);

		error.clear();
		error.msg = "手机认证成功";

		return error.code;
	}

	/**
	 * 用户名是否存在
	 * 
	 * @param userName
	 * @param info
	 * @return 0不存在
	 */
	public static int isNameExist(String userName, ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(userName)) {
			error.code = -1;
			error.msg = "用户名不能为空";

			return error.code;
		}

		String sql = "select name from t_users where name = ?";
		String name = null;

		try {
			name = GenericModel.find(sql, userName).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("判断用户名是否存在时,根据用户名查询数据时：" + e.getMessage());
			error.code = -10;
			error.msg = "对不起，由于平台出现故障，此次用户名是否存在的判断失败！";

			return error.code;
		}

		if (name != null && name.equals(userName)) {
			error.code = -2;
			error.msg = "该用户名已经存在";

			return -2;
		}

		error.code = 0;

		return error.code;
	}

	/**
	 * 身份证是否存在
	 * 
	 * @param userName
	 * @param info
	 * @return 0不存在
	 */
	public static int isIDNumberExist(String idNumber, ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(idNumber)) {
			error.code = -1;
			error.msg = "身份证号码不能为空";

			return error.code;
		}

		String sql = "select id_number from t_users where id_number = ?";
		String name = null;

		try {
			name = GenericModel.find(sql, idNumber).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("判断身份证是否存在时,根据身份证查询数据时：" + e.getMessage());
			error.code = -10;
			error.msg = "对不起，由于平台出现故障，此次用身份证是否存在的判断失败！";

			return error.code;
		}

		if (name != null && name.equals(name)) {
			error.code = -2;
			error.msg = "对不起，该身份证号码已被使用！";

			return -2;
		}

		error.code = 0;

		return error.code;
	}

	/**
	 * 是否是有效会员
	 * 
	 * @param userName
	 * @param info
	 * @return 0不存在
	 */
	public static int isActiveUser(long userId, ErrorInfo error) {
		error.clear();

		if (userId <= 0) {
			error.code = -1;
			error.msg = "传入参数有误";

			return error.code;
		}

		String sql = "select new t_users(id, is_active) from t_users where id = ?";
		t_users user = null;

		try {
			user = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("判断身份证是否存在时,根据身份证查询数据时：" + e.getMessage());
			error.code = -10;
			error.msg = "对不起，由于平台出现故障，此次用身份证是否存在的判断失败！";

			return error.code;
		}

		if (user == null) {
			error.code = -1;
			error.msg = "该用户不存在";

			return error.code;
		}

		if (user.is_active) {
			error.code = 0;

			return error.code;
		}

		error.code = -1;

		return error.code;
	}

	/**
	 * 判断更新为有效会员
	 * 
	 * @param userId
	 * @param error
	 */
	public static void updateActive(long userId, ErrorInfo error) {
		error.clear();

		if (User.isActiveUser(userId, error) < 0) {
			int rows = 0;
			try {
				rows = JPA
						.em()
						.createQuery(
								"update t_users set is_active = 1 where id = ?")
						.setParameter(1, userId).executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.info("更新用户为有效用户时：" + e.getMessage());

				error.code = -1;
				error.msg = "更新有效用户失败";

				return;
			}

			if (rows < 0) {
				error.msg = "更新有效用户失败";
				JPA.setRollbackOnly();

				return;
			}
		}

		error.code = 0;
	}

	/**
	 * 邮箱是否被注册
	 * 
	 * @param email
	 * @param info
	 * @return
	 */
	public static int isEmailExist(String userEmail, ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(userEmail)) {
			error.code = -1;
			error.msg = "请输入邮箱地址";

			return error.code;
		}

		String sql = "select email from t_users where email = ?";
		String email = null;

		try {
			email = GenericModel.find(sql, userEmail).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("判断邮箱是否被注册时,根据邮箱查询数据时：" + e.getMessage());
			error.code = -10;
			error.msg = "对不起，由于平台出现故障，此次邮箱是否被注册判断失败！";

			return error.code;
		}

		if (email != null && email.equals(userEmail)) {
			error.code = -2;
			error.msg = "该邮箱已经注册 ！ 请到前往您的个人邮箱查看邮件！尽快激活！";

			return error.code;
		}

		error.code = 0;

		return error.code;

	}
	
	
	
	/**
	 * 手机号码1是否已经存在
	 * 
	 * @param email
	 * @return
	 */
	public static int isMobileExist1(String userMobile, ErrorInfo error) {
		error.clear();

		String sql = "select mobile1 from t_users where mobile1 = ?";
		String mobiles = null;

		try {
			mobiles = GenericModel.find(sql, userMobile).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("判手机号码是否已经存在时,根据手机号码查询数据时：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次手机号码是否已经存在判断失败！";

			return error.code;
		}

		if (mobiles != null && mobiles.equals(userMobile)) {
			error.code = -2;
			error.msg = "该手机号码已经存在！";

			return error.code;
		}

		error.code = 0;

		return 0;
	}
	
	


	/**
	 * 手机号码是否已经存在
	 * 
	 * @param email
	 * @return
	 */
	public static int isMobileExist(String userMobile, ErrorInfo error) {
		error.clear();

		String sql = "select mobile from t_users where mobile = ?";
		String mobile = null;

		try {
			mobile = GenericModel.find(sql, userMobile).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("判手机号码是否已经存在时,根据手机号码查询数据时：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次手机号码是否已经存在判断失败！";

			return error.code;
		}

		if (mobile != null && mobile.equals(userMobile)) {
			error.code = -2;
			error.msg = "该手机号码已经存在！";

			return error.code;
		}

		error.code = 0;

		return 0;
	}

	/**
	 * 忘记用户名，根据email获得用户名，然后发送模板邮件至该邮箱
	 * 
	 * @param email
	 * @return
	 */
	public static int forgetName(String email, ErrorInfo error) {

		return EmailUtil.emailFindUserName(email, error);
	}

	/**
	 * 管理员重置用户密码
	 * 
	 * @param supervisorId
	 * @param userId
	 * @param info
	 * @return
	 */
	public static int resetPasswordBySupervisor(long supervisorId, long userId,
			ErrorInfo error) {
		error.msg = "";

		String sql = "select new t_users(name, email) from t_users where id = ?";
		t_users user = null;

		try {
			user = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("管理员重置用户密码时,根据用户ID查询数据时：" + e.getMessage());
			error.msg = "对不起，由于平台出现故障，此次重置用户密码失败！";

			return -1;
		}

		if (user == null) {
			error.msg = "用户不存在，请确认是否操作有误！";

			return -1;
		}

		return EmailUtil.emailFindUserPassword(user.name, user.email, error);
	}

	/**
	 * 管理员将一个用户添加到黑名单
	 * 
	 * @param supervisonId
	 *            管理员id
	 * @param id
	 *            用户id
	 * @param reason
	 *            原因
	 * @param info
	 * @return
	 */
	public static int addBlacklistBySupervisor(long userId, String reason,
			ErrorInfo error) {
		error.clear();

		if (StringUtils.isBlank(reason)) {
			error.code = -1;
			error.msg = "加入黑名单原因不能为空！";

			return error.code;
		}

		if (userId <= 0) {
			error.code = -1;
			error.msg = "传入参数有误！";

			return error.code;
		}

		/*
		 * 下面注释的内容，应该跳转到编写黑名单理由前判断
		 */
		String sql = "select is_blacklist from t_users where id = ?";
		boolean is_blacklist = false;

		try {
			is_blacklist = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("管理员添加黑名单时,根据用户ID查询数据时：" + e.getMessage());
			error.msg = "对不起，由于平台出现故障，此次添加黑名单失败！";

			return -1;
		}
		if (is_blacklist) {
			error.msg = "该用户已在黑名单中";
			return -1;
		}

		EntityManager em = JPA.em();
		String updateSql = "update t_users set is_blacklist=?,joined_time=?,joined_reason=? where id=?";
		Query query = em.createQuery(updateSql).setParameter(1, true)
				.setParameter(2, new Date()).setParameter(3, reason)
				.setParameter(4, userId);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("管理员添加黑名单时,更新用户数据时：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次添加黑名单失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.supervisorEvent(Supervisor.currSupervisor().id,
				SupervisorEvent.ADD_BLACKLIST, "加入黑名单", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		error.code = 0;
		error.msg = "添加黑名单用户成功";

		return 0;
	}

	/**
	 * 管理员将一个用户解除黑名单
	 * 
	 * @param supervisonId
	 *            管理员id
	 * @param id
	 *            用户id
	 * @param info
	 * @return
	 */
	public static long editBlacklist(long userId, ErrorInfo error) {
		error.clear();

		// String sql = "select is_blacklist from t_users where id = ?";
		// boolean is_blacklist = false;
		//
		// try {
		// is_blacklist = t_users.find(sql, userId).first();
		// } catch(Exception e) {
		// e.printStackTrace();
		// Logger.info("管理员解除黑名单时,根据用户ID查询数据时："+e.getMessage());
		// info.msg = "对不起，由于平台出现故障，此次解除黑名单失败！";
		//
		// return -1;
		// }
		// if(！is_blacklist) {
		// info.msg = "该用户不在黑名单中";
		// return -1;
		// }

		EntityManager em = JPA.em();
		String updateSql = "update t_users set is_blacklist=?,joined_time=?,joined_reason=? where id=?";
		Query query = em.createQuery(updateSql).setParameter(1, false)
				.setParameter(2, null).setParameter(3, null)
				.setParameter(4, userId);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("管理员解除黑名单时,更新用户数据时：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次解除黑名单失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.supervisorEvent(Supervisor.currSupervisor().id,
				SupervisorEvent.DELETE_BLACKLIST, "解除黑名单", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		error.code = 0;
		error.msg = "解除黑名单用户成功";

		return 0;
	}

	/**
	 * 管理员分配用户
	 * 
	 * @param supervisonId
	 *            分配的管理员
	 * @param toSupervisonId
	 *            被分配管理员的id
	 * @param userId
	 *            被分配的用户
	 * @param info
	 * @return
	 */
	public static int assignUser(long supervisorId, String typeStr, long bidId,
			ErrorInfo error) {
		error.clear();

		long userId = 0;

		if (!NumberUtil.isNumericInt(typeStr)) {
			error.code = -1;
			error.msg = "传入的类型参数有误";

			return error.code;
		}

		String hql = "select user_id from t_bids where id = ?";

		try {
			userId = GenericModel.find(hql, bidId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.msg = "对不起，由于平台出现故障，此次分配用户失败！";

			return -1;
		}

		if (userId <= 0) {
			error.code = -1;
			error.msg = "传入的类型参数有误";

			return error.code;
		}

		/* 客服可以更改，所以不需要进行此校验 */
		// String sql =
		// "select assigned_to_supervisor_id from t_users where id = ?";
		// long assignedToSupervisorId = -1;
		//
		// try {
		// assignedToSupervisorId = t_users.find(sql, userId).first();
		// } catch(Exception e) {
		// e.printStackTrace();
		// Logger.info("管理员分配用户时,根据用户ID查询数据时："+e.getMessage());
		// error.msg = "对不起，由于平台出现故障，此次分配用户失败！";
		//
		// return -1;
		// }
		//
		// if(assignedToSupervisorId > 0) {
		// error.msg = "该用户已被分配！";
		//
		// return -1;
		// }

		int type = Integer.parseInt(typeStr);

		if (type != 2) {
			error.code = -1;
			error.msg = "对不起！已有借款标单独分配出去，不能进行分配此会员所有标的操作";

			return error.code;
		}

		String sql = "select sum(manage_supervisor_id) from t_bids where user_id = ? ";
		Long temp = 0l;

		try {
			temp = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (temp > 0) {// 说明该会员已经有单个借款标被分配
			error.code = -1;
			error.msg = "对不起！已有借款标单独分配出去，不能进行分配此会员所有标的操作";

			return error.code;
		}

		EntityManager em = JPA.em();
		String updateSql = "update t_users set assigned_time=?, assigned_to_supervisor_id=? where id=?";
		Query query = em.createQuery(updateSql).setParameter(1, new Date())
				.setParameter(2, supervisorId).setParameter(3, userId);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("管理员分配用户时,更新用户数据时：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次分配用户失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.supervisorEvent(Supervisor.currSupervisor().id,
				SupervisorEvent.REASSIGN_USER, "分配会员所有标", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		error.code = 0;
		error.msg = "用户分配成功!";

		return 0;
	}

	/**
	 * 重新分配用户所有的借款标
	 * 
	 * @param supervisorId
	 * @param typeStr
	 * @param toSupervisorIdStr
	 * @param userIdStr
	 * @param error
	 * @return
	 */
	public static int assignUserAgain(long supervisorId, String typeStr,
			String toSupervisorIdStr, String userIdStr, ErrorInfo error) {
		error.clear();

		long userId = 0;
		long toSupervisorId = 0;

		if (!NumberUtil.isNumericInt(userIdStr)) {
			error.code = -1;
			error.msg = "传入的借款会员ID有误";

			return error.code;
		}

		if (!NumberUtil.isNumericInt(toSupervisorIdStr)) {
			error.code = -2;
			error.msg = "传入的管理员参数有误";

			return error.code;
		}

		if (!NumberUtil.isNumericInt(typeStr)) {
			error.code = -1;
			error.msg = "传入的类型参数有误";

			return error.code;
		}

		userId = Long.parseLong(userIdStr);
		toSupervisorId = Long.parseLong(toSupervisorIdStr);
		EntityManager em = JPA.em();
		String updateSql = "update t_users set assigned_time=?, assigned_to_supervisor_id=? where id=?";

		Query query = em.createQuery(updateSql).setParameter(1, new Date())
				.setParameter(2, toSupervisorId).setParameter(3, userId);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("管理员分配用户时,更新用户数据时：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次分配用户失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.supervisorEvent(supervisorId, SupervisorEvent.ASSIGN_USER,
				"分配会员所有标", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		error.code = 0;
		error.msg = "用户分配成功!";

		return 0;
	}

	/**
	 * 模拟登录管理员id加密
	 * 
	 * @param adminId
	 * @return
	 */
	public static String encrypt() {
		Supervisor supervisor = Supervisor.currSupervisor();
		String time = Long.toString(new DateUtil().getHours());
		String id = Long.toString(supervisor.id);
		String all = id + time + Constants.ENCRYPTION_KEY;

		return com.shove.security.Encrypt.MD5(all.trim());
	}

	/**
	 * 管理员锁定用户
	 * 
	 * @param SupervisonId
	 * @param userId
	 * @param info
	 * @return
	 */
	public static int lockUser(long userId, ErrorInfo error) {
		error.clear();

		// String sql = "select is_allow_login from t_users where id = ?";
		// boolean isLocked = false;
		//
		// try {
		// isLocked = t_users.find(sql, userId).first();
		// } catch(Exception e) {
		// e.printStackTrace();
		// Logger.info("管理员锁定用户时,根据用户ID查询数据时："+e.getMessage());
		// info.msg = "对不起，由于平台出现故障，此次锁定用户失败！";
		//
		// return -1;
		// }
		//
		// if(isLocked) {
		// info.msg = "该用户已被锁定！";
		//
		// return -1;
		// }

		EntityManager em = JPA.em();
		String updateSql = "update t_users set is_allow_login=?,lock_time=? where id=?";
		Query query = em.createQuery(updateSql).setParameter(1, Constants.TRUE)
				.setParameter(2, new Date()).setParameter(3, userId);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("管理员锁定用户时,更新用户数据时：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次锁定用户失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.supervisorEvent(Supervisor.currSupervisor().id,
				SupervisorEvent.LOCK_USER, "锁定", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		error.code = 0;
		error.msg = "锁定用户成功!";

		return 0;
	}

	/**
	 * 管理员开启用户
	 * 
	 * @param SupervisonId
	 * @param userId
	 * @param info
	 * @return
	 */
	public static int openUser(long userId, ErrorInfo error) {
		error.clear();

		// String sql = "select is_allow_login from t_users where id = ?";
		// boolean isLocked = false;
		//
		// try {
		// isLocked = t_users.find(sql, userId).first();
		// } catch(Exception e) {
		// e.printStackTrace();
		// Logger.info("管理员开启用户时,根据用户ID查询数据时："+e.getMessage());
		// info.msg = "对不起，由于平台出现故障，此次开启用户失败！";
		//
		// return -1;
		// }
		//
		// if(!isLocked) {
		// info.msg = "该用户未被锁定";
		//
		// return -1;
		// }

		EntityManager em = JPA.em();
		String updateSql = "update t_users set is_allow_login=?,lock_time=? where id=?";
		Query query = em.createQuery(updateSql)
				.setParameter(1, Constants.FALSE).setParameter(2, null)
				.setParameter(3, userId);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("管理员开启用户时,更新用户数据时：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次开启用户失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.supervisorEvent(Supervisor.currSupervisor().id,
				SupervisorEvent.OPEN_USER, "分配会员所有标", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return error.code;
		}

		error.code = 0;
		error.msg = "开启用户成功！";

		return 0;
	}

	/**
	 * 管理员拒收用户的站内信
	 * 
	 * @param SupervisonId
	 *            操作的管理员
	 * @param reason
	 *            原因
	 * @param info
	 * @return
	 */
	public static int refusedMessage(long supervisorId, long userId,
			String reason, ErrorInfo error) {
		error.clear();

		/* 查询的时一个布尔值，如果用户不存在会回出现空指针异常，不能用first,需用fetch */
		// String sql = "select is_refused_receive from t_users where id = ?";
		// boolean isRefused = false;
		//
		// try {
		// isRefused = t_users.find(sql, userId).first();
		// } catch(Exception e) {
		// e.printStackTrace();
		// Logger.info("管理员拒收用户的站内信时,根据用户ID查询数据时："+e.getMessage());
		// info.msg = "对不起，由于平台出现故障，此次拒收用户的站内信失败！";
		//
		// return -1;
		// }
		//
		// if(isRefused) {
		// info.msg = "该用户已被拒收";
		// return -1;
		// }

		EntityManager em = JPA.em();
		String updateSql = "update t_users set is_refused_receive=?,refused_time=?,refused_reason=? where id=?";
		Query query = em.createQuery(updateSql).setParameter(1, true)
				.setParameter(2, new Date()).setParameter(3, reason)
				.setParameter(4, userId);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("管理员拒收用户的站内信时,更新用户数据时：" + e.getMessage());
			error.msg = "对不起，由于平台出现故障，此次拒收用户的站内信失败！";

			return -1;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.supervisorEvent(supervisorId, SupervisorEvent.REFUSE_MSG,
				"拒收会员站内信", error);

		if (error.code < 0) {
			return error.code;
		}

		error.code = 0;
		error.msg = "添加拒收用户成功";

		return 0;
	}

	/**
	 * 管理员接收用户信息
	 * 
	 * @param supervisorId
	 * @param userId
	 * @param info
	 * @return
	 */
	public static int recieverMessage(long supervisorId, long userId,
			ErrorInfo error) {
		error.clear();

		// String sql = "select is_refused_receive from t_users where id = ?";
		// boolean isRefused = false;
		//
		// try {
		// isRefused = t_users.find(sql, userId).first();
		// } catch(Exception e) {
		// e.printStackTrace();
		// info.code = -1;
		// info.msg = "查询用户数据出现错误";
		//
		// return info.code;
		// }
		//
		// if(!isRefused) {
		// info.code = -2;
		// info.msg = "该用户未被拒收";
		//
		// return info.code;
		// }

		EntityManager em = JPA.em();
		String updateSql = "update t_users set is_refused_receive=?,refused_time=?,refused_reason=? where id=?";
		Query query = em.createQuery(updateSql)
				.setParameter(1, Constants.FALSE).setParameter(2, null)
				.setParameter(3, null).setParameter(4, userId);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("管理员接收用户的站内信时,更新用户数据时：" + e.getMessage());
			error.code = -3;
			error.msg = "更新用户数据出现错误";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		DealDetail.supervisorEvent(supervisorId, SupervisorEvent.RECEIVE_MSG,
				"接收会员站内信", error);

		if (error.code < 0) {
			return error.code;
		}

		error.code = 0;
		error.msg = "解除拒收用户成功";

		return 0;
	}

	/**
	 * 充值记录查询
	 * 
	 * @param id
	 *            用户id
	 * @return
	 */
	public static List<t_user_recharge_details> queryRechargeRecord(long id) {
		List<t_user_recharge_details> list = GenericModel.find("",
				id).fetch();
		return list;
	}

	/**
	 * 手工充值
	 */
	public static void rechargeByHand(long supervisorId, String name,
			double amount, ErrorInfo error) {
		error.clear();

		if (amount <= 0) {
			error.code = -1;
			error.msg = "请输入正确的充值金额";

			return;
		}

		if (StringUtils.isBlank(name)) {
			error.code = -1;
			error.msg = "充值对象不能为空";

			return;
		}

		long userId = User.queryIdByUserName(name, error);

		if (userId < 0) {
			error.code = -1;
			error.msg = "该用户名不存在";

			return;
		}

		DataSafety data = new DataSafety();

		if (Constants.CHECK_CODE) {

			data.id = userId;

			if (!data.signCheck(error)) {
				JPA.setRollbackOnly();

				return;
			}
		}

		/*
		 * v_user_for_details forDetail = DealDetail.queryUserBalance(userId,
		 * error);
		 * 
		 * if(error.code < 0) { return ; }
		 */

		Map<String, Double> forDetail = DealDetail.queryUserFund(userId, error);

		if (error.code < 0) {
			return;
		}

		double balance = forDetail.get("user_amount");
		double freeze = forDetail.get("freeze");
		double receiveAmount = forDetail.get("receive_amount");

		balance = balance + amount;

		DealDetail.updateUserBalance(userId, balance, freeze, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		DealDetail detail = new DealDetail(userId, DealType.RECHARGE_HAND,
				amount, supervisorId, balance, freeze, receiveAmount, "管理员手动充值");

		detail.addDealDetail(error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		// 添加充值记录
		User.sequence(userId, 0, amount, Constants.ORDINARY_RECHARGE, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		// 奖励当前充值用户的推荐用户的CPS推广奖励
		User.rewardCPS(userId, 0, userId, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		User.updateActive(userId, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		data.id = userId;
		data.updateSign(error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		DealDetail.supervisorEvent(supervisorId, SupervisorEvent.HAND_RECHARGE,
				"手工充值", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		// String encryString = Security.encryCookie(userId);

		// User user = (User) Cache.get("userId_" + encryString);
		//
		// if(user != null) {
		// user.balance = balance;
		// }

		t_users user = User.queryUserByUserName(name, error);

		if (error.code < 0) {
			return;
		}

		String date = DateUtil.dateToString(new Date());

		// 发送站内信 [date]财务人员给您充值了￥[money]元，备注：[remark]
		TemplateStation station = new TemplateStation();
		station.id = Templets.S_HAND_RECHARGE;

		if (station.status) {
			String mContent = station.content.replace("date", date);
			mContent = mContent.replace("money", amount + "");
			mContent = mContent.replace("remark", "无");
			TemplateStation.addMessageTask(userId, station.title, mContent);
		}

		// 发送邮件 [date] 财务人员给您充值了￥[money]元，备注：[remark]
		TemplateEmail email = new TemplateEmail();
		email.id = Templets.E_HAND_RECHARGE;

		if (email.status) {
			String eContent = email.content.replace("date", date);
			eContent = eContent.replace("money", amount + "");
			eContent = eContent.replace("remark", "无");
			TemplateEmail.addEmailTask(user.email, email.title, eContent);
		}

		// 发送短信 尊敬的[userName]，[date]财务人员给您充值了￥[money]元，备注：[remark]
		TemplateSms sms = new TemplateSms();
		if (StringUtils.isNotBlank(user.mobile)) {
			sms.id = Templets.S_HAND_RECHARGE;

			if (sms.status) {
				String eContent = sms.content.replace("userName", user.name);
				eContent = eContent.replace("date", date);
				eContent = eContent.replace("money", amount + "");
				eContent = eContent.replace("remark", "无");
				TemplateSms.addSmsTask(user.mobile, eContent);
			}
		}

		error.code = 0;
		error.msg = "手动充值成功！";
	}

	/**
	 * 申请提现
	 * @param money  提现金额
	 * @param bankName  银行名称
	 * @param cardNumber  银行账号
	 * @return
	 */
	public long withdrawal(double amount, long bankId, String payPassword, int type, boolean flag, ErrorInfo error){
		error.clear();
		
		if (!(Constants.IPS_ENABLE && flag)) {
			if(StringUtils.isBlank(this.payPassword)) {
				error.code = -1;
				error.msg = "对不起，为了您的账户安全，请先设置交易密码";
				
				return -1;
			}
			
			if(!Encrypt.MD5(payPassword+Constants.ENCRYPTION_KEY).equalsIgnoreCase(this.payPassword)) {
				error.code = -1;
				error.msg = "对不起，交易密码错误";
				
				return -1;
			}
		}
		
		DataSafety data = new DataSafety();
		
		data.id = this.id;
		
		if(!data.signCheck(error)){
			
			return -1;
		}
		
		double money = User.queryRechargeIn(this.id, error);
		
		if(error.code < 0) {
			return -1;
		}
		
		v_user_for_details forDetail = DealDetail.queryUserBalance(this.id, error);
		double balance = 0;
		if(Constants.IPS_ENABLE == true && flag == false) {
			balance = forDetail.user_amount2;
			money = 0;
		}else {
			balance = forDetail.user_amount;
		}
		
		double freeze = forDetail.freeze;
		double receiveAmount = forDetail.receive_amount;
		
		if(balance - money < amount) {
			error.code = -1;
			error.msg = "对不起，已超出最大提现金额";
			
			return -1;
		}
		
		if(!(Constants.IPS_ENABLE && flag)) {
			UserBankAccounts account = new UserBankAccounts();
			account.id = bankId;
			
			if(account.id < 0 || account.userId != this.id) {
				error.code = -1;
				error.msg = "请选择正确的银行账号";
				
				return -1;
			}
		}
		
		t_user_withdrawals withdrawal = new t_user_withdrawals();
		
		withdrawal.time = new Date();
		withdrawal.user_id = this.id;
		withdrawal.amount = amount;
		withdrawal.type = type;
		withdrawal.bank_account_id = bankId;
		withdrawal.status = 0;
		
		try{
			withdrawal.save();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("保存申请提现金额时："+e.getMessage());
			
			error.code = -1;
			error.msg = "请保存申请提现金额时出现异常";
			
			return -1;
		}
		

		
		balance = balance - amount;
		freeze = freeze + amount;
    	DealDetail detail = new DealDetail(this.id, 
				type==1 ? DealType.FREEZE_WITHDRAWAL : DealType.FREEZE_WITHDRAWAL_P,
						amount, withdrawal.id, balance, freeze, receiveAmount, "冻结提现金额");
		 
		if(Constants.IPS_ENABLE && flag == false) {
			detail.addDealDetail2(error);
		}else{
			detail.addDealDetail(error);
		}
		
		if(error.code < 0) {
			JPA.setRollbackOnly();
			
			return -1;
		}
	
		
		if(Constants.IPS_ENABLE == true && flag == false) {
			DealDetail.updateUserBalance2(this.id, balance, freeze, error);
		}else {
			DealDetail.updateUserBalance(this.id, balance, freeze, error);
		}
		
		if(error.code < 0) {
			JPA.setRollbackOnly();
			
			return -1;
		}
		
		data.id = this.id;
		data.updateSign(error);
		
		if(error.code < 0) {
			JPA.setRollbackOnly();
			
			return -1;
		}
		
		DealDetail.userEvent(this.id, UserEvent.APPLY_WITHDRAWALT, "申请提现", error);
		
		if(error.code < 0) {
			JPA.setRollbackOnly();
			
			return -1;
		}
		
		this.balance = balance;
		this.freeze = freeze;
		
		setCurrUser(this);
		
		error.code = 0;
		error.msg = "提现申请成功，请等待管理员的审核";
		
		return withdrawal.id;
	}
	/**
	 * 转账
	 * 
	 * @param money
	 *            转账金额
	 * @param recivedId
	 *            接收方
	 * @return
	 */
	public int transfer(double money, long recivedId) {
		/* 判断转账方可用金额是否足够 */

		/* 根据上面的判断结果，查询接收用户金额 */

		/* 接收方添加金额(总金额，可用金额) */

		/* 保存接收方金额 */

		/* 减少转账方金额（总金额，可用金额） */

		/* 保存接收方金额 */

		/* 添加转账记录 */

		return 0;
	}

	/**
	 * 前台--我的账户--提现（查询提现记录）
	 * 
	 * @param id
	 * @return
	 */
	public static PageBean<v_user_withdrawals> queryWithdrawalRecord(
			long userId, String typeStr, String beginTimeStr,
			String endTimeStr, String currPageStr, String pageSizeStr,
			ErrorInfo error) {
		error.clear();

		int type = 0;
		Date beginTime = null;
		Date endTime = null;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (RegexUtils.isDate(beginTimeStr)) {
			beginTime = DateUtil.strToYYMMDDDate(beginTimeStr);
		}

		if (RegexUtils.isDate(endTimeStr)) {
			endTime = DateUtil.strToYYMMDDDate(endTimeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (type < 0 || type > 4) {
			type = 0;
		}

		StringBuffer conditions = new StringBuffer(
				"select new  v_user_withdrawals(amount, bank_name, account,"
						+ " time, pay_time, status,disagree_reason) from v_user_withdrawals where 1=1 and ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("type", type);
		conditionMap.put("beginTime", beginTimeStr);
		conditionMap.put("endTime", endTimeStr);

		StringBuffer condition = new StringBuffer();

		condition.append("user_id = ? ");
		values.add(userId);

		if (type != 0) {
			condition.append(Constants.WITHDRAWAL_TYPE[type]);
		}

		if (beginTime != null) {
			condition.append("and time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			condition.append("and time < ? ");
			values.add(endTime);
		}

		List<v_user_withdrawals> withdrawals = new ArrayList<v_user_withdrawals>();
		int count = 0;

		try {
			count = (int) GenericModel.count(condition.toString(),
					values.toArray());
			withdrawals = GenericModel.find(
					conditions.append(condition).toString(), values.toArray())
					.fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询提现记录时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询提现记录时出现异常！";

			return null;
		}

		PageBean<v_user_withdrawals> page = new PageBean<v_user_withdrawals>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = withdrawals;

		error.code = 0;
		return page;
	}

	/**
	 * 查询15内充值的金额（15内充值的金额不能提现）
	 * 
	 * @return
	 */
	public static double queryRechargeIn(long userId, ErrorInfo error) {
		error.clear();

		double amount = 0;

		String sql = "select ifnull(sum(detail.amount),0) as amount from t_user_details detail"
				+ " where detail.user_id = ? and (detail.operation = 1 or detail.operation = 2) and "
				+ " detail.time > DATE_ADD(now(),INTERVAL - 1 DAY)";

		try {
			amount = ((BigDecimal) JPA.em().createNativeQuery(sql)
					.setParameter(1, userId).getSingleResult()).doubleValue();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询提现记录时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询提现记录时出现异常！";

			return error.code;
		}
		error.code = 0;

		return amount;
	}

	/**
	 * 后台--提现管理
	 * 
	 * @param id
	 * @return
	 */
	public static PageBean<v_user_withdrawal_info> queryWithdrawalBySupervisor(
			long supervisorId, int status, String beginTimeStr,
			String endTimeStr, String key, String orderTypeStr,
			String currPageStr, String pageSizeStr, ErrorInfo error) {
		error.clear();

		Date beginTime = null;
		Date endTime = null;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;
		int orderType = StringUtils.isBlank(orderTypeStr) ? 0 : Integer
				.parseInt(orderTypeStr);

		if (RegexUtils.isDate(beginTimeStr)) {
			beginTime = DateUtil.strToYYMMDDDate(beginTimeStr);
		}

		if (RegexUtils.isDate(endTimeStr)) {
			endTime = DateUtil.strToYYMMDDDate(endTimeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("beginTime", beginTimeStr);
		conditionMap.put("endTime", endTimeStr);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		if (beginTime != null) {
			conditions.append("and time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and time < ? ");
			values.add(endTime);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append("and name like ? ");
			values.add("%" + key + "%");
		}

		conditions.append("and status = ? ");
		values.add(status);

		conditions.append(Constants.WITHDRAWAL_ORDER_TYPE[orderType]);

		List<v_user_withdrawal_info> withdrawals = new ArrayList<v_user_withdrawal_info>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			withdrawals = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询提现记录时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询提现记录时出现异常！";

			return null;
		}

		PageBean<v_user_withdrawal_info> page = new PageBean<v_user_withdrawal_info>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = withdrawals;

		error.code = 0;
		return page;
	}

	/**
	 * 后台--提现管理--详情
	 */
	public static v_user_withdrawals queryWithdrawalDetailBySupervisor(
			long supervisorId, long withdrawalId, ErrorInfo error) {
		error.clear();

		if (withdrawalId < 0) {
			error.code = -1;
			error.msg = "传入参数有误！";

			return null;
		}

		String sql = "select new  v_user_withdrawals(amount, bank_name, "
				+ "account, account_name) from v_user_withdrawals where id = ? ";

		v_user_withdrawals withdrawal = null;

		try {
			withdrawal = GenericModel.find(sql, withdrawalId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询提现管理详情时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询提现管理详情时出现异常！";

			return null;
		}

		error.code = 0;
		return withdrawal;
	}

	/**
	 * 审核提现通过
	 */
	public static void auditWithdrawalPass(long supervisorId,
			long withdrawalId, ErrorInfo error) {
		error.clear();

		if (withdrawalId <= 0) {
			error.code = -1;
			error.msg = "传入参数有误！";

			return;
		}

		t_user_withdrawals withdrawal = GenericModel
				.findById(withdrawalId);

		if (withdrawal.status != 0) {
			error.code = -1;
			error.msg = "状态已更改，请勿重复提交！";

			return;
		}

		int rows = 0;

		try {
			rows = JpaHelper.execute(
					"update t_user_withdrawals set status = 1, audit_supervisor_id = ?,"
							+ " audit_time = ? where id = ? and status = 0",
					supervisorId, new Date(), withdrawalId).executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.error("审核提现通过时：" + e.getMessage());
			error.code = -1;
			error.msg = "查审核提现通过时出现异常！";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		DealDetail.supervisorEvent(supervisorId,
				SupervisorEvent.AUDIT_WITHDRAWAL, "审核提现通过", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		error.code = 0;
		error.msg = "提现审核通过保存成功！";
	}

	/**
	 * 审核提现不通过
	 */
	public static void auditWithdrawalDispass(long supervisorId,
			long withdrawalId, String reason, ErrorInfo error) {
		error.clear();

		if (withdrawalId <= 0) {
			error.code = -1;
			error.msg = "传入参数有误！";

			return;
		}

		if (StringUtils.isBlank(reason)) {
			error.code = -2;
			error.msg = "不通过原因不能为空！";

			return;
		}

		t_user_withdrawals withdrawal = GenericModel
				.findById(withdrawalId);

		if (!(withdrawal.status == 0 || withdrawal.status == 1)) {
			error.code = -1;
			error.msg = "状态已更改，请勿重复提交！";

			return;
		}

		int rows = 0;

		try {
			rows = JpaHelper
					.execute(
							"update t_user_withdrawals set status = -1, audit_supervisor_id = ?,"
									+ " audit_time = ?, disagree_reason = ? where id = ? and (status = 0 or status = 1)",
							supervisorId, new Date(), reason, withdrawalId)
					.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.error("审核提现通过时：" + e.getMessage());
			error.code = -1;
			error.msg = "查审核提现通过时出现异常！";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		/**
		 * 返还冻结资金
		 * 
		 * ***/
		int row = 0;
		row = DealDetail.relieveFreezeFund(withdrawal.user_id,
				withdrawal.amount);

		if (row < 1) {
			error.msg = "返还冻结资金失败!";
			JPA.setRollbackOnly();
			return;
		}

		DealDetail.supervisorEvent(supervisorId,
				SupervisorEvent.AUDIT_WITHDRAWAL, "审核提现不通过", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		/* 更新自己的防篡改 */
		DataSafety drawReviewData = new DataSafety(); // 借款会员数据防篡改对象

		drawReviewData.id = withdrawal.user_id;
		drawReviewData.updateSign(error);
		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		t_users user = User.queryUserByUserId(withdrawal.user_id, error);
		String date = DateUtil.dateToString(withdrawal.time);
		// 发送站内信 [date]您申请的提现￥[money]元，提现失败，请重新申请
		TemplateStation station = new TemplateStation();
		station.id = Templets.S_RECHARGE_FAIL;

		if (station.status) {
			String mContent = station.content.replace("date", date);
			mContent = mContent.replace("money", withdrawal.amount + "");
			TemplateStation.addMessageTask(withdrawal.user_id, station.title,
					mContent);
		}

		// 发送邮件 [date]您申请的提现￥[money]元，提现失败，请重新申请
		TemplateEmail email = new TemplateEmail();
		email.id = Templets.E_RECHARGE_FAIL;

		if (email.status) {
			String eContent = email.content.replace("date", date);
			eContent = eContent.replace("money", withdrawal.amount + "");
			TemplateEmail.addEmailTask(user.email, email.title, eContent);
		}

		// 发送短信 尊敬的[userName]，[date]您申请的提现￥[money]元，提现失败，请重新申请
		TemplateSms sms = new TemplateSms();
		if (StringUtils.isNotBlank(user.mobile)) {
			sms.id = Templets.S_RECHARGE_FAIL;

			if (sms.status) {
				String eContent = sms.content.replace("userName", user.name);
				eContent = eContent.replace("date", date);
				eContent = eContent.replace("money", withdrawal.amount + "");
				TemplateSms.addSmsTask(user.mobile, eContent);
			}
		}

		error.code = 0;
		error.msg = "提现审核不通过保存成功！";
	}

	/**
	 * 提现管理--付款通知初始化
	 * 
	 * @param supervisorId
	 * @param withdrawalId
	 */
	public static v_user_withdrawals withdrawalDetail(long supervisorId,
			long withdrawalId, ErrorInfo error) {
		error.clear();

		if (withdrawalId <= 0) {
			error.code = -1;
			error.msg = "传入参数有误！";
		}

		String sql = "select new  v_user_withdrawals(amount, bank_name, "
				+ "account, account_name) from v_user_withdrawals where id = ? ";

		v_user_withdrawals withdrawal = null;

		try {
			withdrawal = GenericModel.find(sql, withdrawalId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询提现管理详情时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询提现管理详情时出现异常！";

			return null;
		}

		error.code = 0;
		return withdrawal;
	}

	/**
	 * 提现管理--付款通知（1站内信、2短信、3邮件）
	 */
	public static void withdrawalNotice(long userId, long withdrawalId,
			int type, boolean flag, ErrorInfo error) {
		error.clear();

		if (userId <= 0) {
			error.code = -1;
			error.msg = "传入参数有误！";

			return;
		}

		if (withdrawalId <= 0) {
			error.code = -1;
			error.msg = "传入参数有误！";

			return;
		}

		if (type < 1 && type > 3) {
			error.code = -1;
			error.msg = "传入参数有误！";

			return;
		}

		DataSafety data = new DataSafety();

		data.id = userId;

		if (!data.signCheck(error)) {
			JPA.setRollbackOnly();

			return;
		}

		t_user_withdrawals withdrawal = GenericModel
				.findById(withdrawalId);

		long supervisorId = -1;

		if (!(Constants.IPS_ENABLE && flag)) {

			if (withdrawal.status != 1) {
				error.code = -1;
				error.msg = "已提现或正在审核中";

				return;
			}

			supervisorId = Supervisor.currSupervisor().id;
		}

		Query query = JpaHelper.execute(
				"update t_user_withdrawals set status = 2, audit_supervisor_id = ?,"
						+ " audit_time = ? where id = ? and status = 1",
				supervisorId, new Date(), withdrawalId);

		if (Constants.IPS_ENABLE && flag) {
			query = JpaHelper.execute(
					"update t_user_withdrawals set status = 2, audit_supervisor_id = ?,"
							+ " audit_time = ? where id = ? and status = 0",
					supervisorId, new Date(), withdrawalId);
		}

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("确定提现付款时：" + e.getMessage());

			error.code = -1;
			error.msg = "付款提现失败";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		v_user_for_details forDetail = DealDetail.queryUserBalance(userId,
				error);

		if (error.code < 0) {
			return;
		}

		double balance = forDetail.user_amount;
		if (Constants.IPS_ENABLE && flag == false) {
			balance = forDetail.user_amount2;
		}
		double freeze = forDetail.freeze;
		double receiveAmount = forDetail.receive_amount;
		double amount = withdrawal.amount;

		DealDetail detail = new DealDetail(userId,
				withdrawal.type == 1 ? DealType.CHARGE_LEFT_WITHDRAWALT
						: DealType.CHARGE_AWARD_WITHDRAWALT, amount,
				supervisorId, balance, freeze, receiveAmount, "管理员提现付款");

		if (Constants.IPS_ENABLE && flag == false) {
			detail.addDealDetail(error);
		} else {
			detail.addDealDetail(error);
		}

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		double serviceFee = ServiceFee.withdrawalFee(amount);

		if (serviceFee > 0) {
			DealDetail detail2 = new DealDetail(userId,
					DealType.CHARGE_WITHDRAWALT, serviceFee, withdrawalId,
					balance, freeze, receiveAmount, "提现管理费");
			if (Constants.IPS_ENABLE && flag == false) {
				detail2.addDealDetail2(error);
			} else {
				detail2.addDealDetail(error);
			}

			if (error.code < 0) {
				JPA.setRollbackOnly();

				return;
			}
		}

		DealDetail.addPlatformDetail(DealType.WIDTHDRAWAL_FEE, withdrawalId,
				userId, -1, DealType.ACCOUNT, serviceFee, 1, "提现管理费", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		freeze = freeze - amount;

		if (Constants.IPS_ENABLE && flag == false) {
			DealDetail.updateUserBalance2(userId, balance, freeze, error);
		} else {
			DealDetail.updateUserBalance(userId, balance, freeze, error);
		}

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		data.id = userId;
		data.updateSign(error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		DealDetail.supervisorEvent(supervisorId,
				SupervisorEvent.NOTICE_WITHDRAWAL, "提现通知", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		/* 待确定是否需要 */
		// User user = (User) Cache.get("userId_" +
		// Security.encryCookie(userId));
		//
		// if(user != null) {
		// user.freeze = freeze;
		// }
		//
		// if(user == null) {
		// user = new User();
		// user.id = userId;
		// }

		t_users user = User.queryUserByUserId(withdrawal.user_id, error);
		String date = DateUtil.dateToString(withdrawal.time);

		/*
		 * 调用站内信/短信/邮件的模板
		 */
		switch (type) {
		case 1:
			// 发送站内信 [date]您申请的提现￥[money]元,财务人员已经处理完毕，请您查收
			TemplateStation station = new TemplateStation();
			station.id = Templets.S_RECHARGE_SUCCESS;

			if (station.status) {
				String mContent = station.content.replace("date", date);
				mContent = mContent.replace("money", withdrawal.amount + "");
				TemplateStation.addMessageTask(withdrawal.user_id,
						station.title, mContent);
			}
			break;
		case 2:
			// 发送邮件 [date]您申请的提现￥[money]元,财务人员已经处理完毕，请您查收
			TemplateEmail email = new TemplateEmail();
			email.id = Templets.E_RECHARGE_SUCCESS;

			if (email.status) {
				String eContent = email.content.replace("date", date);
				eContent = eContent.replace("money", withdrawal.amount + "");
				TemplateEmail.addEmailTask(user.email, email.title, eContent);
			}
			break;
		case 3:
			// 发送短信 尊敬的[userName]，[date]您申请的提现￥[money]元，财务人员已经处理完毕，请您查收
			TemplateSms sms = new TemplateSms();
			if (StringUtils.isNotBlank(user.mobile)) {
				sms.id = Templets.S_RECHARGE_SUCCESS;

				if (sms.status) {
					String eContent = sms.content
							.replace("userName", user.name);
					eContent = eContent.replace("date", date);
					eContent = eContent
							.replace("money", withdrawal.amount + "");
					TemplateSms.addSmsTask(user.mobile, eContent);
				}
			}
			break;
		}

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return;
		}

		error.code = 0;
		error.msg = "通知发送成功！";
	}

	/**
	 * 打印付款单
	 * 
	 * @param supervisorId
	 * @param withdrawalId
	 * @param error
	 */
	public static v_user_withdrawals printPayBill(long withdrawalId,
			ErrorInfo error) {
		error.clear();

		if (withdrawalId <= 0) {
			error.code = -1;
			error.msg = "传入参数有误！";
		}

		String sql = "select new  v_user_withdrawals(name, amount, time, audit_time, status, "
				+ "account, account_name, bank_name) from v_user_withdrawals where id = ? ";

		v_user_withdrawals withdrawal = null;

		try {
			withdrawal = GenericModel.find(sql, withdrawalId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询提现管理详情时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询提现管理详情时出现异常！";

			return null;
		}

		DealDetail.supervisorEvent(Supervisor.currSupervisor().id,
				SupervisorEvent.PRINT_PAYMENT, "打印付款单", error);

		error.code = 0;

		return withdrawal;
	}

	/**
	 * 查询提现不通过的原因
	 */
	public static String withdrawalDispassReason(long withdrawalId,
			ErrorInfo error) {
		error.clear();

		if (withdrawalId <= 0) {
			error.code = -1;
			error.msg = "传入参数有误！";
		}

		String reason = null;

		String sql = "select disagree_reason from t_user_withdrawals where id = ? ";

		try {
			reason = GenericModel.find(sql, withdrawalId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询提现管理详情时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询提现管理详情时出现异常！";

			return null;
		}

		return reason;
	}

	/**
	 * 查询用户的黑名单
	 * 
	 * @param info
	 * @return
	 */
	public static PageBean<v_user_blacklist> queryBlacklist(long userId,
			String key, int currPage, int pageSize, ErrorInfo error) {
		error.clear();

		List<v_user_blacklist> myBlacklist = new ArrayList<v_user_blacklist>();

		PageBean<v_user_blacklist> page = new PageBean<v_user_blacklist>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		StringBuffer conditions = new StringBuffer();
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("key", key);

		conditions.append("user_id = ? ");
		values.add(userId);

		if (StringUtils.isNotBlank(key)) {
			conditions.append("and blacklist_name like ? ");
			values.add("%" + key + "%");
		}

		try {
			myBlacklist = GenericModel.find(conditions.toString(),
					values.toArray()).fetch();
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询用户黑名单时，根据用户ID查询黑名单时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户黑名单时出现异常";

			return null;
		}

		page.page = myBlacklist;
		page.conditions = conditionMap;

		error.code = 0;

		return page;
	}

	/**
	 * 前台—>查询用户的推广会员
	 * 
	 * @param info
	 * @return
	 */
	public static PageBean<v_user_cps_users> queryCpsSpreadUsers(long userId,
			String typeStr, String key, String yearStr, String monthStr,
			String currPageStr, String pageSizeStr, ErrorInfo error) {
		error.clear();

		int type = 0;
		int year = -1;
		int month = -1;
		int currPage = 1;
		int pageSize = Constants.PAGE_SIZE;

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
		}

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (type < 0 || type > 2) {
			type = 0;
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("type", type);
		conditionMap.put("key", key);
		conditionMap.put("year", yearStr);
		conditionMap.put("month", monthStr);

		conditions.append("and recommend_user_id = ? ");
		values.add(userId);

		if (type != 0) {
			conditions.append(Constants.MY_CPS[type]);
		}

		if (StringUtils.isNotBlank(key)) {
			conditions.append("and name like ? ");
			values.add("%" + key + "%");
		}

		if (year != -1 && month != -1) {
			conditions.append("and year = ? and month = ? ");
			values.add(year);
			values.add(month);
		}

		List<v_user_cps_users> users = new ArrayList<v_user_cps_users>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			users = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询我成功推广的会员时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询我成功推广的会员时出现异常！";

			return null;
		}

		PageBean<v_user_cps_users> page = new PageBean<v_user_cps_users>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = users;

		error.code = 0;

		return page;
	}

	/**
	 * 前台—>查询用户的推广会员下面的统计
	 * 
	 * @return
	 */
	public static v_user_cps_user_count queryCpsCount(long userId,
			ErrorInfo error) {
		v_user_cps_user_count user = null;
        
		try {				

			user = GenericModel.findById(userId);
			
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询用户的推广会员下面的统计时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户的推广会员下面的统计时出现异常！";

			return null;
		}

		error.code = 0;

		return user;
	}

	/**
	 * 前台—>查询用户的推广收入
	 * 
	 * @param userId
	 * @param info
	 * @param currPage
	 * @return
	 */
	public static PageBean<t_user_cps_income> queryCpsSpreadIncome(long userId,
			String yearStr, String monthStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		int year = -1;
		int month = -1;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;

		if (NumberUtil.isNumericInt(yearStr)) {
			year = Integer.parseInt(yearStr);
		}

		if (NumberUtil.isNumericInt(monthStr)) {
			month = Integer.parseInt(monthStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);

		conditions.append("and user_id = ? ");
		values.add(userId);

		if (year != -1 && month != -1) {
			conditions.append("and year = ? and month = ? ");
			values.add(year);
			values.add(month);
		}

		List<t_user_cps_income> users = new ArrayList<t_user_cps_income>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			users = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询我成功推广的会员时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询我的推广收入时出现异常！";

			return null;
		}

		PageBean<t_user_cps_income> page = new PageBean<t_user_cps_income>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = users;

		error.code = 0;

		return page;
	}

	/**
	 * 后台—>推广会员列表
	 * 
	 * @param supervisorId
	 * @param name
	 * @param orderType
	 * @param currPage
	 * @param pageSize
	 * @return
	 */
	public static PageBean<v_user_cps_user_info> queryCpsUserInfo(String name,
			int orderType, int currPage, int pageSize) {

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("name", name);
		conditionMap.put("orderType", orderType);

		if (name != null && !name.equals("")) {
			conditions.append("and name like ? ");
			values.add("%" + name + "%");
		}

		conditions.append("order by "
				+ Constants.ORDER_TYPE_CPS_DETAIL[orderType]);

		PageBean<v_user_cps_user_info> page = new PageBean<v_user_cps_user_info>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		List<v_user_cps_user_info> details = new ArrayList<v_user_cps_user_info>();

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			details = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询推广会员列表时：" + e.getMessage());

			return null;
		}

		page.page = details;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 后台—>推广会员列表详情
	 * 
	 * @param supervisorId
	 * @param name
	 * @param beginTime
	 * @param endTime
	 * @param currPage
	 * @param pageSize
	 * @return
	 */
	public static PageBean<v_user_cps_detail> queryCpsDetail(long userId,
			String name, Date beginTime, Date endTime, int currPage,
			int pageSize) {

		StringBuffer conditions = new StringBuffer("recommend_user_id = ? ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("name", name);
		conditionMap.put("beginTime", beginTime);
		conditionMap.put("endTime", endTime);

		values.add(userId);

		if (name != null && !name.equals("")) {
			conditions.append("and name like ? ");
			values.add("%" + name + "%");
		}

		if (beginTime != null) {
			conditions.append("and time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and time < ? ");
			values.add(endTime);
		}

		PageBean<v_user_cps_detail> page = new PageBean<v_user_cps_detail>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		List<v_user_cps_detail> details = new ArrayList<v_user_cps_detail>();

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			details = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询查询推广会员列表详情时：" + e.getMessage());

			return null;
		}

		page.page = details;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 后台—>佣金发放明细（operation=4）
	 * 
	 * @param supervisorId
	 * @param name
	 * @param currPage
	 * @param pageSize
	 * @return
	 */
	public static PageBean<t_user_details> queryCpsCommissionDetail(
			long supervisorId, String name, int currPage, int pageSize) {

		StringBuffer conditions = new StringBuffer("operation = 4 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("name", name);

		if (name != null && !name.equals("")) {
			conditions.append("name like ?");
			values.add("%" + name + "%");
		}

		PageBean<t_user_details> page = new PageBean<t_user_details>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		List<t_user_details> details = new ArrayList<t_user_details>();

		try {
			page.totalCount = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			details = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询佣金发放明细时：" + e.getMessage());

			return null;
		}

		page.page = details;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 佣金发放明细表（operation=4）
	 * 
	 * @param supervisorId
	 * @param year
	 * @param month
	 * @param currPage
	 * @param pageSize
	 * @return
	 */
	public static PageBean<v_user_cps_offer_info> queryCpsOfferInfo(
			long supervisorId, int year, int month, int currPage) {

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("year", year);
		conditionMap.put("month", month);

		PageBean<v_user_cps_offer_info> page = new PageBean<v_user_cps_offer_info>();
		page.currPage = Constants.ONE;
		page.pageSize = Constants.FIVE;

		if (currPage != 0) {
			page.currPage = currPage;
		}

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
		}

		List<v_user_cps_offer_info> offerInfo = new ArrayList<v_user_cps_offer_info>();

		try {
			page.totalCount = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			offerInfo = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(page.currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询佣金发放明细表时：" + e.getMessage());

			return null;
		}

		page.page = offerInfo;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 关注用户
	 * 
	 * @param userId
	 * @param error
	 * @return
	 */
	public static int attentionUser(long userId, long attentionUserId,
			ErrorInfo error) {
		error.clear();

		long size = 0;

		try {
			size = GenericModel.count(
					"user_id = ? and attention_user_id = ?", userId,
					attentionUserId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("关注用户时，获取关注的用户时：" + e.getMessage());
			error.code = -1;
			error.msg = "关注用户失败";

			return error.code;
		}

		if (size > 0) {
			error.code = -1;
			error.msg = "你已经关注了该用户！";

			return error.code;
		}

		t_user_attention_users attention = new t_user_attention_users();

		attention.time = new Date();
		attention.user_id = userId;
		attention.attention_user_id = attentionUserId;

		try {
			attention.save();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("关注用户时，保存关注的用户时：" + e.getMessage());
			error.code = -1;
			error.msg = "关注用户失败";

			return error.code;
		}

		DealDetail.userEvent(userId, UserEvent.ATTENTION_USER, "关注用户", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		error.code = 0;
		error.msg = "关注该用户成功";

		return 0;
	}

	/**
	 * 取消关注的用户
	 * 
	 * @param id
	 * @param userId
	 * @param info
	 * @return
	 */
	public static int cancelAttentionUser(long attentionId, ErrorInfo error) {
		error.clear();

		t_user_attention_users attentionUser = null;

		try {
			attentionUser = GenericModel.findById(attentionId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("取消关注用户时，获取关注的用户时：" + e.getMessage());
			error.code = -1;
			error.msg = "取消关注用户失败！";

			return error.code;
		}

		if (attentionUser == null) {
			error.code = -1;
			error.msg = "取消关注用户不存在！";

			return error.code;
		}

		try {
			attentionUser.delete();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("取消关注用户时，保存取消关注的用户时：" + e.getMessage());
			error.code = -1;
			error.msg = "取消关注用户失败";

			return error.code;
		}

		// String sql = "delete t_user_attention_users where id = ?";
		//
		// try {
		// JpaHelper.execute(sql,attentionId).executeUpdate();
		// }catch (Exception e) {
		// e.printStackTrace();
		// Logger.info("取消关注用户时，保存取消关注的用户时："+e.getMessage());
		// info.code = -1;
		// info.msg = "取消关注用户失败";
		//
		// return info.code;
		// }

		DealDetail.userEvent(attentionUser.user_id, UserEvent.CANCEL_ATTENTION,
				"取消关注用户", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		error.code = 0;
		error.msg = "取消关注用户成功";

		return 0;
	}

	/**
	 * 判断是否为关注的人
	 * 
	 * @param userId
	 * @param attentionUserId
	 * @param error
	 * @return
	 */
	public static boolean isAttentionUser(long userId, long attentionUserId,
			ErrorInfo error) {
		error.clear();

		long count = 0;

		try {
			count = GenericModel.count(
					"user_id = ? and attention_user_id =?", userId,
					attentionUserId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("判断是否为关注用户时：" + e.getMessage());
			error.code = -1;
			error.msg = "根据ID查询关注用户失败";

			return false;
		}

		if (count <= 0) {
			error.code = -1;
			error.msg = "对不起，你并未关注此用户，你无权查看该用户信息！";

			return false;
		}

		error.code = 0;

		return true;
	}

	/**
	 * 前台--会员详情
	 * 
	 * @param userId
	 * @param error
	 * @return
	 */
	public static v_user_for_personal queryUserInformation(long userId,
			ErrorInfo error) {
		error.clear();

		v_user_for_personal userInformation = null;

		try {
			userInformation = GenericModel.findById(userId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询会员详情时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询会员详情时出现异常";

			return null;
		}

		error.code = 0;

		return userInformation;
	}

	/**
	 * 修改备注名
	 * 
	 * @param attentionId
	 * @param info
	 * @return
	 */
	public static int updateAttentionUser(long attentionId, String noteName,
			ErrorInfo error) {
		error.clear();

		if (attentionId <= 0) {
			error.code = -1;
			error.msg = "传入参数有误";

			return error.code;
		}

		if (StringUtils.isBlank(noteName)) {
			error.code = -1;
			error.msg = "备注名不能为空";

			return error.code;
		}

		t_user_attention_users attentionUser = null;

		try {
			attentionUser = GenericModel.findById(attentionId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改备注名,根据ID查询关注用户时：" + e.getMessage());
			error.code = -1;
			error.msg = "根据ID查询关注用户失败";

			return error.code;
		}

		if (attentionUser == null) {
			error.code = -2;
			error.msg = "关注的用户不存在";

			return error.code;
		}

		attentionUser.note_name = noteName;

		try {
			attentionUser.save();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查修改备注名，保存备注名时：" + e.getMessage());
			error.code = -2;
			error.msg = "保存备注名失败";

			return error.code;
		}

		DealDetail.userEvent(attentionUser.user_id, UserEvent.EDIT_NOTE_NAME,
				"修改关注用户备注名", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		error.code = 0;
		error.msg = "修改备注名成功！";

		return 0;
	}

	/**
	 * 查询关注的用户
	 * 
	 * @param userId
	 * @param info
	 * @return
	 */
	public static PageBean<v_user_attention_info> queryAttentionUsers(
			long userId, int currPage, int pageSize, ErrorInfo error) {
		error.clear();

		pageSize = Constants.PAGE_SIZE;

		if (currPage == 0) {
			currPage = Constants.ONE;
		}

		if (pageSize == 0) {
			pageSize = Constants.PAGE_SIZE;
		}

		List<v_user_attention_info> attentionUsers = new ArrayList<v_user_attention_info>();
		int count = 0;

		try {
			count = (int) GenericModel.count("user_id = ?", userId);
			attentionUsers = GenericModel.find("user_id = ?", userId)
					.fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("修改备注名,根据ID查询关注用户时：" + e.getMessage());
			error.code = -1;
			error.msg = "根据ID查询关注用户失败";

			return null;
		}

		PageBean<v_user_attention_info> page = new PageBean<v_user_attention_info>();
		page.currPage = currPage;
		page.pageSize = pageSize;
		page.totalCount = count;

		page.page = attentionUsers;

		error.code = 0;

		return page;
	}

	/**
	 * blackUserId是否在id的黑名单中
	 * 
	 * @param id
	 * @param blackUserId
	 * @param info
	 *            错误信息
	 * @return
	 */
	public static int isInMyBlacklist(long id, long blackUserId, ErrorInfo error) {
		error.clear();
		t_user_blacklist myBlack = null;

		try {
			myBlack = GenericModel.find("user_id = ? and black_user_id =?",
					id, blackUserId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("判断是否在黑名单时，根据用户ID查询黑名单时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询黑名单失败";

			return error.code;
		}

		if (myBlack != null) {
			error.code = -2;
			error.msg = "该用户已在你的黑名单中";

			return error.code;
		}

		error.code = 0;

		return 0;
	}

	/**
	 * 用户添加自己的黑名单
	 * 
	 * @param bidId
	 *            关联在标
	 * @param reason
	 *            原因
	 * @return
	 */
	public int addBlacklist(long bidId, String reason, ErrorInfo error) {
		error.clear();

		String sql = "select user_id from t_bids where id = ?";
		List<Long> blackUserIds = null;

		try {
			blackUserIds = GenericModel.find(sql, bidId).fetch();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("用户添加自己的黑名单,根据标ID查询用户ID时" + e.getMessage());
			error.code = -1;
			error.msg = "根据借款标查询用户id失败";

			return error.code;
		}

		if (blackUserIds == null || blackUserIds.size() == 0) {
			error.code = -2;
			error.msg = "关联的标不存在，数据有有误！";

			return error.code;
		}

		if (User.isInMyBlacklist(this.id, blackUserIds.get(0), error) < 0) {

			error.code = -4;
			error.msg = "该用户已在你的黑名单中！";

			return error.code;
		}

		if (reason == "" || reason == null || reason.length() == 0) {
			error.msg = "原因不能为空";
			error.code = -1;

			return error.code;
		}

		t_user_blacklist blacklist = new t_user_blacklist();

		blacklist.time = new Date();
		blacklist.user_id = this.id;
		blacklist.bid_id = bidId;
		blacklist.black_user_id = blackUserIds.get(0);
		blacklist.reason = reason;

		try {
			blacklist.save();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -3;
			error.msg = "添加黑名单失败";

			return error.code;
		}

		DealDetail.userEvent(this.id, UserEvent.ADD_BLACKLIST, "添加黑名单", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		error.code = 0;
		error.msg = "添加黑名单成功";

		return 0;
	}

	/**
	 * 用户删除自己的黑名单
	 * 
	 * @param blacklistId
	 * @param info
	 * @return
	 */
	public int deleteBlacklist(long blacklistId, ErrorInfo error) {
		error.clear();

		if (blacklistId <= 0) {
			error.code = -1;
			error.msg = "传入参数有误";

			return error.code;
		}

		t_user_blacklist blacklist = null;

		try {
			blacklist = GenericModel.findById(blacklistId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("用户删除自己的黑名单,根据黑名单ID查询黑名单时" + e.getMessage());
			error.code = -1;
			error.msg = "根据黑名单ID查询黑名单失败";

			return error.code;
		}

		if (blacklist == null) {
			error.code = -2;
			error.msg = "操作有误，无数据";

			return error.code;
		}

		try {
			blacklist.delete();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("用户删除自己的黑名单,删除黑名单时" + e.getMessage());
			error.code = -3;
			error.msg = "用户删除自己的黑名单失败";

			return error.code;
		}

		DealDetail.userEvent(this.id, UserEvent.DELETE_BLACKLIST, "删除黑名单",
				error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		error.code = 0;
		error.msg = "删除黑名单成功";

		return 0;
	}

	/**
	 * 根据id获得用户名
	 * 
	 * @return
	 */
	public static String queryUserNameById(long id, ErrorInfo error) {
		error.clear();

		String sql = "select name from t_users where id = ?";
		String userName = null;

		try {
			userName = GenericModel.find(sql, id).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据id获得用户名时，根据用户ID查询用户名时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户名失败";

			return null;
		}

		error.code = 0;

		return userName == null ? "*" : userName;
	}

	/**
	 * 根据id获得用户
	 * 
	 * @return
	 */
	public static t_users queryUserByUserId(long userId, ErrorInfo error) {
		error.clear();

		String sql = "select new t_users(name, mobile, email) from t_users where id = ?";
		t_users user = null;

		try {
			user = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据用户id查询用户时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户失败";

			return null;
		}

		error.code = 0;

		return user;
	}

	/**
	 * 根据id获得用户
	 * 
	 * @return
	 */
	public static t_users queryUser2ByUserId(long userId, ErrorInfo error) {
		error.clear();

		String sql = "select new t_users(reality_name, id_number, ips_acct_no, ips_bid_auth_no) from t_users where id = ?";
		t_users user = null;

		try {
			user = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据用户id查询用户时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户失败";

			return null;
		}

		error.code = 0;

		return user;
	}

	/**
	 * 根据用户名获得用户
	 * 
	 * @return
	 */
	public static t_users queryUserByUserName(String name, ErrorInfo error) {
		error.clear();

		String sql = "select new t_users(name, mobile, email) from t_users where name = ?";
		t_users user = null;

		try {
			user = GenericModel.find(sql, name).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据用户名查询用户时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户失败";

			return null;
		}

		error.code = 0;

		return user;
	}

	/**
	 * 根据email获得用户名
	 * 
	 * @return
	 */
	public static t_users queryUserByEmail(String email, ErrorInfo error) {
		error.clear();

		String sql = "select new t_users(id, name) from t_users where email = ?";
		t_users user = null;

		try {
			user = GenericModel.find(sql, email).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据id获得用户名时，根据用户ID查询用户名时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户名失败";

			return null;
		}

		error.code = 0;

		return user;
	}

	/**
	 * 根据mobile获得用户名
	 * 
	 * @return
	 */
	public static t_users queryUserByMobile(String mobile, ErrorInfo error) {
		error.clear();

		String sql = "select new t_users(id, name) from t_users where mobile = ?";
		t_users user = null;

		try {
			user = GenericModel.find(sql, mobile).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据mobile获得用户名时，根据用户mobile查询用户名时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户名失败";

			return null;
		}

		error.code = 0;

		return user;
	}

	/**
	 * 根据用户id获得该用户的信用额度credit_line
	 * 
	 * @return
	 */
	public static double queryCreditLineById(long id, ErrorInfo error) {
		error.clear();

		String sql = "select credit_line from t_users where id = ?";
		Double creditLine = null;

		try {
			creditLine = GenericModel.find(sql, id).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询用户信用额度时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户信用额度失败";

			return 0;
		}

		if (creditLine == null) {
			error.code = 1;
			error.msg = "信用额度为空";

			return 0;
		}

		error.code = 0;

		return (creditLine == null) ? 0 : creditLine.doubleValue();
	}

	/**
	 * 根据用户名查询id
	 * 
	 * @return
	 */
	public static long queryIdByUserName(String userName, ErrorInfo error) {
		error.clear();

		String sql = "select id from t_users where name = ?";
		List<Long> ids = null;

		try {
			ids = GenericModel.find(sql, userName).fetch();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据用户名查询id时，根据用户名查询用户ID时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户id失败";

			return error.code;
		}

		if (ids == null || ids.size() == 0) {
			error.code = -1;
			error.msg = "用户不存在";

			return error.code;
		}

		error.code = 0;

		return ids.get(0);
	}

	/**
	 * 根据手机号码查询id
	 * 
	 * @return
	 */
	public static long queryIdByMobile(String mobile, ErrorInfo error) {
		error.clear();

		String sql = "select id from t_users where mobile = ?";
		List<Long> ids = null;

		try {
			ids = GenericModel.find(sql, mobile).fetch();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("根据用户名查询id时，根据用户名查询用户ID时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询用户id失败";

			return error.code;
		}

		if (ids == null || ids.size() == 0) {
			error.code = -1;
			error.msg = "用户不存在";

			return error.code;
		}

		error.code = 0;

		return ids.get(0);
	}

	/**
	 * 举报用户
	 * 
	 * @param userName
	 * @param reason
	 * @param bidId
	 *            关联的借款标（如果不关联，填0）
	 * @param investTransferId
	 *            关联的债权（如果不关联，填0）
	 * @param info
	 * @return
	 */
	public int addReportAUser(String userName, String reason, long bidId,
			long investTransferId, ErrorInfo error) {
		error.clear();

		long rId = User.queryIdByUserName(userName, error);

		if (rId <= 0) {
			error.code = -1;
			error.msg = "用户不存在";

			return error.code;
		}

		t_user_report_users report = null;

		try {
			report = GenericModel.find(
					"user_id = ? and reported_user_id =?", this.id, rId)
					.first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("举报用户时，判断用户是否已被举报时：" + e.getMessage());
			error.code = -2;
			error.msg = "查询举报名单失败";

			return error.code;
		}

		if (report != null) {
			error.code = -3;
			error.msg = "该用户已在你的举报名单中";

			return error.code;
		}

		t_user_report_users reportUser = new t_user_report_users();

		reportUser.user_id = this.id;
		reportUser.time = new Date();
		reportUser.reported_user_id = rId;
		reportUser.reason = reason;
		reportUser.relation_bid_id = bidId;
		reportUser.relation_invest_transfer_id = investTransferId;

		try {
			reportUser.save();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("举报用户时，保存举报信息时：" + e.getMessage());
			error.code = -4;
			error.msg = "查询举报名单失败";

			return error.code;
		}

		DealDetail.userEvent(this.id, UserEvent.REPORT_USER, "举报用户", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return error.code;
		}

		error.code = 0;
		error.msg = "举报成功！";

		return 0;
	}

	/**
	 * 更新会员类型
	 * 
	 * @param userId
	 * @param type
	 *            1 借款 2 理财
	 * @param info
	 * @return
	 */
	public static int updateMasterIdentity(long userId, int type,
			ErrorInfo error) {
		error.clear();

		if (type != Constants.ONE && type != Constants.TWO) {
			error.code = -1;
			error.msg = "传入数据有误数据";

			return -1;
		}

		String sql = "select master_identity from t_users where id = ?";

		int identity = -1;

		try {
			identity = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("更新会员类型" + e.getMessage());
			error.code = -2;
			error.msg = "查询用户信息失败";

			return error.code;
		}

		switch (identity) {
		case Constants._ONE:
			error.code = -3;
			error.msg = "查询用户不存在";

			return error.code;

		case Constants.THREE:
			error.msg = "用户性质为复合会员";

			return 0;

		case Constants.ZERO:

			switch (type) {
			case Constants.ONE:
				forMasterIdentity(userId, Constants.ONE, error);

				return error.code;

			case Constants.TWO:
				forMasterIdentity(userId, Constants.TWO, error);

				return error.code;
			}

		case Constants.ONE:

			switch (type) {
			case Constants.ONE:
				error.msg = "用户性质为借款会员";
				return 0;

			case Constants.TWO:
				forMasterIdentity(userId, Constants.THREE, error);

				return error.code;
			}

		case Constants.TWO:

			switch (type) {
			case Constants.ONE:
				forMasterIdentity(userId, Constants.THREE, error);

				return error.code;

			case Constants.TWO:
				error.msg = "用户性质为借款会员";

				return 0;

			}

		}

		error.code = -8;
		error.msg = "未知错误";

		return error.code;
	}

	private static void forMasterIdentity(long userId, int masterIdentity,
			ErrorInfo error) {
		EntityManager em = JPA.em();

		String master_time = "";

		if (masterIdentity == Constants.ONE) {
			master_time = "master_time_loan";
		} else if (masterIdentity == Constants.TWO) {
			master_time = "master_time_invest";
		} else {
			master_time = "master_time_complex";
		}

		Query queryTwo = em
				.createQuery(
						"update t_users set master_identity = ?, "
								+ master_time + " = ? where id = ?")
				.setParameter(1, masterIdentity).setParameter(2, new Date())
				.setParameter(3, userId);

		int rows = 0;

		try {
			rows = queryTwo.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("更新会员类型" + e.getMessage());
			error.code = -7;
			error.msg = "更新用户性质失败";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		error.code = 0;
	}

	/**
	 * 财富统计
	 * 
	 * @param id
	 * @param info
	 * @return
	 */
	public v_user_invest_amount getInvestAmount() {
		v_user_invest_amount investAmounts = null;

		try {
			investAmounts = GenericModel.findById(this.id);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询财富统计时：" + e.getMessage());

			return null;
		}

		return investAmounts;
	}

	/********************************************* 性别，房车，婚姻等的查询 ***********************/
	public static void queryBasic() {

		List<t_dict_cars> cars = null;
		List<t_dict_ad_provinces> provinces = null;
		List<t_dict_ad_citys> citys = null;
		List<t_dict_educations> educations = null;
		List<t_dict_houses> houses = null;
		List<t_dict_maritals> maritals = null;

		try {
			cars = GenericModel.findAll();
			provinces = GenericModel.findAll();
			citys = GenericModel.findAll();

			educations = GenericModel.findAll();
			houses = GenericModel.findAll();
			maritals = GenericModel.findAll();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询基本信息时：" + e.getMessage());

			return;
		}

		Cache.set("cars", cars);
		Cache.set("provinces", provinces);
		Cache.set("citys", citys);
		Cache.set("educations", educations);
		Cache.set("houses", houses);
		Cache.set("maritals", maritals);
	}

	/**
	 * 根据provinceId查询所有的市
	 * 
	 * @param citys
	 * @param cityId
	 * @return
	 */
	public static List<t_dict_ad_citys> queryCity(long provinceId) {

		List<t_dict_ad_citys> citys = (List<t_dict_ad_citys>) Cache
				.get("citys");

		if (citys == null) {
			User.queryBasic();
			citys = (List<t_dict_ad_citys>) Cache.get("citys");
		}
		List<t_dict_ad_citys> cityList = new ArrayList<t_dict_ad_citys>();

		for (t_dict_ad_citys city : citys) {

			if (city.province_id == provinceId) {
				cityList.add(city);
			}
		}

		return cityList;

	}

	/**
	 * 根据市查询省
	 * 
	 * @param citys
	 * @param cityId
	 * @return
	 */
	public static int queryProvince(long cityId) {

		t_dict_ad_citys city = GenericModel.findById(cityId);

		if (city == null) {
			return -1;
		}

		return city.province_id;
	}

	/**
	 * 获得当前缓存中的user
	 * 
	 * @return
	 */
	public static User currUser() {
		Cookie cookie = Request.current().cookies.get("sp2p6");

		if (cookie == null) {
			return null;
		}

		String encryString = cookie.value;

		User user = (User) Cache.get("userId_" + encryString);

		if (user == null) {
			ErrorInfo error = new ErrorInfo();
			long userId = Security.decryCookie(encryString, error);

			if (userId < 0) {
				return null;
			}

			user = new User();

			user.id = userId;
		}

		return user;
	}

	/**
	 * 添加cookie和cache
	 * 
	 * @param user
	 */
	public static void setCurrUser(User user) {
		String encryString = Security.encryCookie(user.id);
		Response.current().setCookie("sp2p6", encryString);
		Cache.set("userId_" + encryString, user);
	}

	/**
	 * 退出时清除cookie和缓存
	 */
	public static void removeCurrUser() {
		Cookie cookie = Request.current().cookies.get("sp2p6");
		if (cookie != null) {
			String encryString = cookie.value;
			Cache.delete("userId_" + encryString);
		}

		Response.current().removeCookie("sp2p6");
	}

	/**
	 * 账户总览查询（账户总览--温馨提示）
	 * 
	 * @param userId
	 * @return
	 */
	public static v_user_account_statistics queryAccountStatistics(long userId,
			ErrorInfo error) {
		error.clear();

		v_user_account_statistics accountStatistics = null;

		try {
			// findById：如果id不存在，则得到空
			
			
			accountStatistics = GenericModel.findById(userId);
			
	
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询财富统计(温馨提示)时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询财富统计(温馨提示)时系统异常！";

			return null;
		}

		error.code = 0;

		return accountStatistics;
	}

	/**
	 * 根据用户id查询不同类别获得的积分
	 * 
	 * @param userId
	 * @return
	 */
	public static v_user_detail_score queryCreditScore(long userId) {
		v_user_detail_score creditScore = null;

		try {
			// findById：如果id不存在，则得到空
			creditScore = GenericModel.findById(userId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询不同类别获得的积分时：" + e.getMessage());

			return null;
		}

		return creditScore;
	}

	/**
	 * 查询积分明细（成功借款）
	 * 
	 * @param id
	 * @param type
	 * @param currPage
	 * @param key
	 * @return
	 */
	public static PageBean<v_user_detail_credit_score_loan> queryCreditDetailLoan(
			long id, int currPage, int pageSize, String key) {
		if (currPage == 0) {
			currPage = 1;
		}

		List<v_user_detail_credit_score_loan> creditScore = new ArrayList<v_user_detail_credit_score_loan>();
		PageBean<v_user_detail_credit_score_loan> page = new PageBean<v_user_detail_credit_score_loan>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		if (pageSize == 0) {
			page.pageSize = Constants.FIVE;
		}

		String sql = "";
		Object[] conditions = null;

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("key", key);

		if (StringUtils.isBlank(key)) {
			sql = "user_id = ?";
			conditions = new Object[1];
			conditions[0] = id;
		} else {
			sql = "title like ? and user_id = ?";
			conditions = new Object[2];
			conditions[0] = "%" + key + "%";
			conditions[1] = id;
		}

		try {
			page.totalCount = (int) GenericModel.count(sql,
					conditions);
			creditScore = GenericModel.find(sql, conditions)
					.fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询积分明细（成功借款）时：" + e.getMessage());

			return null;
		}

		page.page = creditScore;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 根据查询积分明细（审核资料）
	 * 
	 * @param id
	 * @param type
	 * @param currPage
	 * @param key
	 * @return
	 */
	public static PageBean<v_user_detail_credit_score_audit_items> queryCreditDetailAuditItem(
			long id, int currPage, int pageSize, String key, ErrorInfo error) {
		error.clear();

		if (currPage == 0) {
			currPage = 1;
		}

		List<v_user_detail_credit_score_audit_items> creditScore = new ArrayList<v_user_detail_credit_score_audit_items>();
		PageBean<v_user_detail_credit_score_audit_items> page = new PageBean<v_user_detail_credit_score_audit_items>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		if (pageSize == 0) {
			page.pageSize = Constants.FIVE;
		}

		String sql = "";
		Object[] conditions = null;

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("key", key);

		if (StringUtils.isBlank(key)) {
			sql = "user_id = ?";
			conditions = new Object[1];
			conditions[0] = id;
		} else {
			sql = "audit_item_name like ? and user_id = ?";
			conditions = new Object[2];
			conditions[0] = "%" + key + "%";
			conditions[1] = id;
		}

		try {
			page.totalCount = (int) GenericModel
					.count(sql, conditions);
			creditScore = GenericModel.find(sql,
					conditions).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询信用积分明细时：" + e.getMessage());
			error.code = -1;
			error.msg = "信用积分明细查询出现错误";
			return null;
		}

		page.page = creditScore;
		page.conditions = conditionMap;

		error.code = 0;

		return page;
	}

	/**
	 * 根据查询积分明细（成功投标）
	 * 
	 * @param id
	 * @param currPage
	 * @param key
	 * @return
	 */
	public static PageBean<v_user_detail_credit_score_invest> queryCreditDetailInvest(
			long id, int currPage, int pageSize, String key) {
		if (currPage == 0) {
			currPage = 1;
		}

		List<v_user_detail_credit_score_invest> creditScore = new ArrayList<v_user_detail_credit_score_invest>();
		PageBean<v_user_detail_credit_score_invest> page = new PageBean<v_user_detail_credit_score_invest>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		if (pageSize == 0) {
			page.pageSize = Constants.FIVE;
		}

		String sql = "";
		Object[] conditions = null;

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("key", key);

		if (StringUtils.isBlank(key)) {
			sql = "user_id = ?";
			conditions = new Object[1];
			conditions[0] = id;
		} else {
			sql = "title like ? and user_id = ?";
			conditions = new Object[2];
			conditions[0] = "%" + key + "%";
			conditions[1] = id;
		}

		try {
			page.totalCount = (int) GenericModel.count(
					sql, conditions);
			creditScore = GenericModel.find(sql,
					conditions).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询积分明细（成功投标）时：" + e.getMessage());

			return null;
		}

		page.page = creditScore;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 根据查询积分明细（正常还款）
	 * 
	 * @param id
	 * @param currPage
	 * @param key
	 * @return
	 */
	public static PageBean<v_user_detail_credit_score_normal_repayment> queryCreditDetailRepayment(
			long id, int currPage, int pageSize, String key) {
		if (currPage == 0) {
			currPage = 1;
		}

		List<v_user_detail_credit_score_normal_repayment> creditScore = new ArrayList<v_user_detail_credit_score_normal_repayment>();
		PageBean<v_user_detail_credit_score_normal_repayment> page = new PageBean<v_user_detail_credit_score_normal_repayment>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		if (pageSize == 0) {
			page.pageSize = Constants.FIVE;
		}

		String sql = "";
		Object[] conditions = null;

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("key", key);

		if (StringUtils.isBlank(key)) {
			sql = "user_id = ?";
			conditions = new Object[1];
			conditions[0] = id;
		} else {
			sql = "title like ? and user_id = ?";
			conditions = new Object[2];
			conditions[0] = "%" + key + "%";
			conditions[1] = id;
		}

		try {
			page.totalCount = (int) GenericModel
					.count(sql, conditions);
			creditScore = GenericModel.find(sql,
					conditions).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询积分明细（正常还款）时：" + e.getMessage());

			return null;
		}

		page.page = creditScore;
		page.conditions = conditionMap;

		return page;
	}

	/**
	 * 根据查询积分明细（逾期扣分）
	 * 
	 * @param id
	 * @param currPage
	 * @param key
	 * @return
	 */
	public static PageBean<v_user_detail_credit_score_overdue> queryCreditDetailOverdue(
			long id, int currPage, int pageSize, String key) {
		if (currPage == 0) {
			currPage = 1;
		}

		List<v_user_detail_credit_score_overdue> creditScore = new ArrayList<v_user_detail_credit_score_overdue>();
		PageBean<v_user_detail_credit_score_overdue> page = new PageBean<v_user_detail_credit_score_overdue>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		if (pageSize == 0) {
			page.pageSize = Constants.FIVE;
		}

		String sql = "";
		Object[] conditions = null;

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("key", key);

		if (StringUtils.isBlank(key)) {
			sql = "user_id = ?";
			conditions = new Object[1];
			conditions[0] = id;
		} else {
			sql = "title like ? and user_id = ?";
			conditions = new Object[2];
			conditions[0] = "%" + key + "%";
			conditions[1] = id;
		}

		try {
			page.totalCount = (int) GenericModel.count(
					sql, conditions);
			creditScore = GenericModel.find(sql,
					conditions).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询积分明细（逾期扣分）时：" + e.getMessage());

			return null;
		}

		page.page = creditScore;
		page.conditions = conditionMap;

		return page;
	}

	public static v_user_users queryUserById(long id, ErrorInfo error) {
		error.clear();
		v_user_users user = null;

		try {
			user = GenericModel.findById(id);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询会员管理详情时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询会员详情失败";

			return null;
		}

		error.code = 0;

		return user;
	}

	/**
	 * 拒收人员名单
	 * 
	 * @param currPage
	 * @param pageSize
	 * @return
	 */
	public static PageBean<t_users> queryRefusedUser(int currPage,
			int pageSize, String keyword, ErrorInfo error) {
		error.clear();

		if (currPage < 1) {
			currPage = 1;
		}

		if (pageSize < 1) {
			pageSize = 10;
		}

		List<t_users> refusedUser = new ArrayList<t_users>();
		String condition = "(is_refused_receive=true)";
		List<Object> params = new ArrayList<Object>();

		if (StringUtils.isNotBlank(keyword)) {
			condition += " and (name like ?)";
			params.add("%" + keyword + "%");
		}

		String sql = "select new t_users(id, name, refused_time, is_refused_receive, refused_reason, is_allow_login) "
				+ "from t_users where " + condition;

		PageBean<t_users> page = new PageBean<t_users>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		try {
			page.totalCount = (int) GenericModel.count(condition, params.toArray());
			refusedUser = GenericModel.find(sql, params.toArray()).fetch(currPage,
					page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询所有的会员时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询所有的会员时：数据库异常";

			return null;
		}

		Map<String, Object> map = new HashMap<String, Object>();

		if (StringUtils.isNotBlank(keyword)) {
			map.put("keyword", keyword);
		}

		page.conditions = map;
		page.page = refusedUser;

		error.code = 0;

		return page;
	}

	/**
	 * 拒收人员详情
	 * 
	 * @param userId
	 * @param error
	 * @return
	 */
	public static t_users queryRefusedUserDetail(long userId, ErrorInfo error) {
		error.clear();
		t_users user = null;
		String sql = "select new t_users(id, name, refused_time, is_refused_receive, refused_reason, is_allow_login) from t_users where (id = ?)";

		try {
			user = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询拒收人员详情时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询拒收人员详情时：数据库异常";

			return null;
		}

		error.code = 0;

		return user;
	}

	/**
	 * 通过名字查询用户
	 * 
	 * @param name
	 * @param error
	 * @return
	 */
	public static t_users queryUserByName(String name, ErrorInfo error) {
		error.clear();
		t_users user = null;

		try {
			user = GenericModel
					.find("select new t_users(id, name, reality_name, email) from t_users where name = ?",
							name).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("数据库异常");
			error.code = -1;
			error.msg = "查询用户失败";

			return null;
		}

		if (user == null) {
			error.code = -2;
			error.msg = "账号为" + name + "的用户不存在";

			return null;
		}

		error.code = 0;
		error.msg = "查询管理员成功";

		return user;
	}

	/**
	 * 查询所有的会员
	 * 
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_info> queryUserBySupervisor(String name,
			String email, String beginTimeStr, String endTimeStr, String key,
			String orderTypeStr, String currPageStr, String pageSizeStr,
			ErrorInfo error) {
		error.clear();

		Date beginTime = null;
		Date endTime = null;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;

		if (RegexUtils.isDate(beginTimeStr)) {
			beginTime = DateUtil.strToYYMMDDDate(beginTimeStr);
		}

		if (RegexUtils.isDate(endTimeStr)) {
			endTime = DateUtil.strToYYMMDDDate(endTimeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (orderType < 0 || orderType > 14) {
			orderType = 0;
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("name", name);
		conditionMap.put("email", email);
		conditionMap.put("beginTime", beginTimeStr);
		conditionMap.put("endTime", endTimeStr);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		if (StringUtils.isNotBlank(key)) {
			conditions.append("and name like ? ");
			values.add("%" + key + "%");
		}

		if (StringUtils.isNotBlank(name)) {
			conditions.append("and name like ? ");
			values.add("%" + name + "%");
		}

		if (StringUtils.isNotBlank(email)) {
			conditions.append("and email like ? ");
			values.add("%" + email + "%");
		}

		if (beginTime != null) {
			conditions.append("and register_time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and register_time < ? ");
			values.add(endTime);
		}

		conditions.append("order by " + Constants.ORDER_TYPE[orderType]);

		List<v_user_info> users = new ArrayList<v_user_info>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			users = GenericModel.find(conditions.toString(), values.toArray())
					.fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询所有的会员时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询全部会员列表时出现异常！";

			return null;
		}

		PageBean<v_user_info> page = new PageBean<v_user_info>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = users;

		error.code = 0;

		return page;
	}

	/**
	 * 查询借款会员
	 * 
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_loan_info> queryLoanUserBySupervisor(
			String statusTypeStr, String name, String email,
			String beginTimeStr, String endTimeStr, String key,
			String orderTypeStr, String currPageStr, String pageSizeStr,
			ErrorInfo error) {
		error.clear();

		int statusType = 0;
		Date beginTime = null;
		Date endTime = null;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;

		if (NumberUtil.isNumericInt(statusTypeStr)) {
			statusType = Integer.parseInt(statusTypeStr);
		}

		if (RegexUtils.isDate(beginTimeStr)) {
			beginTime = DateUtil.strToYYMMDDDate(beginTimeStr);
		}

		if (RegexUtils.isDate(endTimeStr)) {
			endTime = DateUtil.strToYYMMDDDate(endTimeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (statusType < 0 || statusType > 2) {
			statusType = 0;
		}

		if (orderType < 0 || orderType > 8) {
			orderType = 0;
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("statusType", statusType);
		conditionMap.put("name", name);
		conditionMap.put("email", email);
		conditionMap.put("beginTime", beginTimeStr);
		conditionMap.put("endTime", endTimeStr);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		if (statusType != 0) {
			conditions.append(Constants.STATUS_TYPE[statusType]);
		}

		if (StringUtils.isNotBlank(name)) {
			conditions.append("and name like ? ");
			values.add("%" + name + "%");
		}

		if (StringUtils.isNotBlank(email)) {
			conditions.append("and email like ? ");
			values.add("%" + email + "%");
		}

		if (beginTime != null) {
			conditions.append("and register_time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and register_time < ? ");
			values.add(endTime);
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.LOAN_USER_ORDER[orderType]);
		}

		List<v_user_loan_info> users = new ArrayList<v_user_loan_info>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			users = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询所有的会员时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询借款会员列表时出现异常！";

			return null;
		}

		PageBean<v_user_loan_info> page = new PageBean<v_user_loan_info>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = users;

		error.code = 0;

		return page;
	}

	/**
	 * 查询理财会员
	 * 
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_invest_info> queryInvestUserBySupervisor(
			String name, String email, String beginTimeStr, String endTimeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		Date beginTime = null;
		Date endTime = null;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;

		if (RegexUtils.isDate(beginTimeStr)) {
			beginTime = DateUtil.strToYYMMDDDate(beginTimeStr);
		}

		if (RegexUtils.isDate(endTimeStr)) {
			endTime = DateUtil.strToYYMMDDDate(endTimeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (orderType < 0 || orderType > 8) {
			orderType = 0;
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("name", name);
		conditionMap.put("email", email);
		conditionMap.put("beginTime", beginTimeStr);
		conditionMap.put("endTime", endTimeStr);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		if (StringUtils.isNotBlank(name)) {
			conditions.append("and name like ? ");
			values.add("%" + name + "%");
		}

		if (StringUtils.isNotBlank(email)) {
			conditions.append("and email like ? ");
			values.add("%" + email + "%");
		}

		if (beginTime != null) {
			conditions.append("and register_time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and register_time < ? ");
			values.add(endTime);
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.INVEST_USER_ORDER[orderType]);
		}

		List<v_user_invest_info> users = new ArrayList<v_user_invest_info>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			users = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询所有的会员时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询理财会员列表时出现异常！";

			return null;
		}

		PageBean<v_user_invest_info> page = new PageBean<v_user_invest_info>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = users;

		error.code = 0;

		return page;
	}

	/**
	 * 查询复合会员
	 * 
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_complex_info> queryComplexUserBySupervisor(
			String name, String email, String beginTimeStr, String endTimeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		Date beginTime = null;
		Date endTime = null;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;

		if (RegexUtils.isDate(beginTimeStr)) {
			beginTime = DateUtil.strToYYMMDDDate(beginTimeStr);
		}

		if (RegexUtils.isDate(endTimeStr)) {
			endTime = DateUtil.strToYYMMDDDate(endTimeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (orderType < 0 || orderType > 12) {
			orderType = 0;
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("name", name);
		conditionMap.put("email", email);
		conditionMap.put("beginTime", beginTimeStr);
		conditionMap.put("endTime", endTimeStr);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		if (StringUtils.isNotBlank(name)) {
			conditions.append("and name like ? ");
			values.add("%" + name + "%");
		}

		if (StringUtils.isNotBlank(email)) {
			conditions.append("and email like ? ");
			values.add("%" + email + "%");
		}

		if (beginTime != null) {
			conditions.append("and register_time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and register_time < ? ");
			values.add(endTime);
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.COMPLEX_USER_ORDER[orderType]);
		}

		List<v_user_complex_info> users = new ArrayList<v_user_complex_info>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			users = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询复合会员列表时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询复合会员列表时出现异常！";

			return null;
		}

		PageBean<v_user_complex_info> page = new PageBean<v_user_complex_info>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = users;

		error.code = 0;

		return page;
	}

	/**
	 * 查询vip会员
	 * 
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_vip_info> queryVipUserBySupervisor(
			String name, String email, String beginTimeStr, String endTimeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();
		Date beginTime = null;
		Date endTime = null;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;

		if (RegexUtils.isDate(beginTimeStr)) {
			beginTime = DateUtil.strToYYMMDDDate(beginTimeStr);
		}

		if (RegexUtils.isDate(endTimeStr)) {
			endTime = DateUtil.strToYYMMDDDate(endTimeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (orderType < 0 || orderType > 12) {
			orderType = 0;
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("name", name);
		conditionMap.put("email", email);
		conditionMap.put("beginTime", beginTimeStr);
		conditionMap.put("endTime", endTimeStr);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		if (StringUtils.isNotBlank(name)) {
			conditions.append("and name like ? ");
			values.add("%" + name + "%");
		}

		if (StringUtils.isNotBlank(email)) {
			conditions.append("and email like ? ");
			values.add("%" + email + "%");
		}

		if (beginTime != null) {
			conditions.append("and register_time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and register_time < ? ");
			values.add(endTime);
		}

		if (orderType != 0) {
			conditions
					.append("order by " + Constants.VIP_USER_ORDER[orderType]);
		}

		List<v_user_vip_info> users = new ArrayList<v_user_vip_info>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			users = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询vip会员列表时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询vip会员列表时出现异常！";

			return null;
		}

		PageBean<v_user_vip_info> page = new PageBean<v_user_vip_info>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = users;

		error.code = 0;

		return page;
	}

	/**
	 * 查询cps会员
	 * 
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_cps_info> queryCpsUserBySupervisor(
			String name, String email, String beginTimeStr, String endTimeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		Date beginTime = null;
		Date endTime = null;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;

		if (RegexUtils.isDate(beginTimeStr)) {
			beginTime = DateUtil.strToYYMMDDDate(beginTimeStr);
		}

		if (RegexUtils.isDate(endTimeStr)) {
			endTime = DateUtil.strToYYMMDDDate(endTimeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (orderType < 0 || orderType > 8) {
			orderType = 0;
		}

		StringBuffer conditions = new StringBuffer("1=1 ");

		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("name", name);
		conditionMap.put("email", email);
		conditionMap.put("beginTime", beginTimeStr);
		conditionMap.put("endTime", endTimeStr);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		if (StringUtils.isNotBlank(name)) {
			conditions.append("and name like ? ");
			values.add("%" + name + "%");
		}

		if (StringUtils.isNotBlank(email)) {
			conditions.append("and email like ? ");
			values.add("%" + email + "%");
		}

		if (beginTime != null) {
			conditions.append("and register_time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and register_time < ? ");
			values.add(endTime);
		}

		if (orderType != 0) {
			conditions
					.append("order by " + Constants.CPS_USER_ORDER[orderType]);
		}

		List<v_user_cps_info> users = new ArrayList<v_user_cps_info>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			users = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询cps会员列表时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询cps会员列表时出现异常！";

			return null;
		}

		PageBean<v_user_cps_info> page = new PageBean<v_user_cps_info>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = users;

		error.code = 0;

		return page;
	}

	/**
	 * 查询未激活会员
	 * 
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_unverified_info> queryUnverifiedUserBySupervisor(
			String name, String email, String beginTimeStr, String endTimeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		Date beginTime = null;
		Date endTime = null;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;

		if (RegexUtils.isDate(beginTimeStr)) {
			beginTime = DateUtil.strToYYMMDDDate(beginTimeStr);
		}

		if (RegexUtils.isDate(endTimeStr)) {
			endTime = DateUtil.strToYYMMDDDate(endTimeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (orderType < 0 || orderType > 2) {
			orderType = 0;
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("name", name);
		conditionMap.put("email", email);
		conditionMap.put("beginTime", beginTimeStr);
		conditionMap.put("endTime", endTimeStr);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		if (StringUtils.isNotBlank(name)) {
			conditions.append("and name like ? ");
			values.add("%" + name + "%");
		}

		if (StringUtils.isNotBlank(email)) {
			conditions.append("and email like ? ");
			values.add("%" + email + "%");
		}

		if (beginTime != null) {
			conditions.append("and register_time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and register_time < ? ");
			values.add(endTime);
		}

		// if(orderType != 0) {
		// conditions.append("order by "+Constants.UNVERIFIED_USER_ORDER[orderType]);
		// }

		conditions.append("order by id desc ");
		List<v_user_unverified_info> users = new ArrayList<v_user_unverified_info>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			users = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询未激活会员列表时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询未激活会员列表时出现异常！";

			return null;
		}

		PageBean<v_user_unverified_info> page = new PageBean<v_user_unverified_info>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = users;

		error.code = 0;

		return page;
	}

	/**
	 * 查询锁定会员
	 * 
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_locked_info> queryLockedUserBySupervisor(
			String name, String email, String beginTimeStr, String endTimeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		Date beginTime = null;
		Date endTime = null;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;

		if (RegexUtils.isDate(beginTimeStr)) {
			beginTime = DateUtil.strToYYMMDDDate(beginTimeStr);
		}

		if (RegexUtils.isDate(endTimeStr)) {
			endTime = DateUtil.strToYYMMDDDate(endTimeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (orderType < 0 || orderType > 6) {
			orderType = 0;
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("name", name);
		conditionMap.put("email", email);
		conditionMap.put("beginTime", beginTimeStr);
		conditionMap.put("endTime", endTimeStr);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		if (StringUtils.isNotBlank(name)) {
			conditions.append("and name like ? ");
			values.add("%" + name + "%");
		}

		if (StringUtils.isNotBlank(email)) {
			conditions.append("and email like ? ");
			values.add("%" + email + "%");
		}

		if (beginTime != null) {
			conditions.append("and register_time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and register_time < ? ");
			values.add(endTime);
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.LOCKED_USER_ORDER[orderType]);
		}

		List<v_user_locked_info> users = new ArrayList<v_user_locked_info>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			users = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询已锁定会员会员列表时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询已锁定会员列表时出现异常！";

			return null;
		}

		PageBean<v_user_locked_info> page = new PageBean<v_user_locked_info>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = users;

		error.code = 0;

		return page;
	}

	/**
	 * 查询被举报会员列表
	 * 
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_reported_info> queryReportedUserBySupervisor(
			String name, String email, String beginTimeStr, String endTimeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		Date beginTime = null;
		Date endTime = null;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;

		if (RegexUtils.isDate(beginTimeStr)) {
			beginTime = DateUtil.strToYYMMDDDate(beginTimeStr);
		}

		if (RegexUtils.isDate(endTimeStr)) {
			endTime = DateUtil.strToYYMMDDDate(endTimeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (orderType < 0 || orderType > 4) {
			orderType = 0;
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("name", name);
		conditionMap.put("email", email);
		conditionMap.put("beginTime", beginTimeStr);
		conditionMap.put("endTime", endTimeStr);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		if (StringUtils.isNotBlank(name)) {
			conditions.append("and name like ? ");
			values.add("%" + name + "%");
		}

		if (StringUtils.isNotBlank(email)) {
			conditions.append("and email like ? ");
			values.add("%" + email + "%");
		}

		if (beginTime != null) {
			conditions.append("and register_time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and register_time < ? ");
			values.add(endTime);
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.REPORTED_USER_ORDER[orderType]);
		}

		List<v_user_reported_info> users = new ArrayList<v_user_reported_info>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			users = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询被举报会员列表时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询被举报会员列表时出现异常！";

			return null;
		}

		PageBean<v_user_reported_info> page = new PageBean<v_user_reported_info>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = users;

		error.code = 0;

		return page;
	}

	/**
	 * 后台--被举报会员列表--举报会员列表
	 * 
	 * @param supervisorId
	 * @param reportedUserId
	 * @param currPage
	 * @param pageSize
	 */
	public static PageBean<v_user_report_list> queryReportUserBySupervisor(
			long supervisorId, long reportedUserId, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		int currPage = Constants.ONE;
		int pageSize = Constants.TWO;

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}
		List<v_user_report_list> reportUsers = new ArrayList<v_user_report_list>();

		PageBean<v_user_report_list> page = new PageBean<v_user_report_list>();
		page.currPage = currPage;
		page.pageSize = pageSize;

		String sql = "reported_user_id = ?";

		try {
			page.totalCount = (int) GenericModel.count(sql,
					reportedUserId);
			reportUsers = GenericModel.find(sql, reportedUserId).fetch(
					currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询举报会员列表时：" + e.getMessage());
			error.code = -2;
			error.msg = "查询举报会员列表时出现异常！";

			return null;
		}

		page.page = reportUsers;

		error.code = 0;

		return page;
	}

	/**
	 * 举报某个用户记录列表
	 * 
	 * @param pageBean
	 *            分页对象
	 * @param userId
	 *            用户ID
	 * @param error
	 *            信息值
	 * @return List<t_user_report_users>
	 */
	public static List<t_user_report_users> queryBidRecordByUser(
			PageBean<t_user_report_users> pageBean, long userId, ErrorInfo error) {
		error.clear();

		int count = -1;
		String condition = "from t_user_report_users uru,t_users u where uru.user_id = u.id and uru.reported_user_id = ?";

		try {
			count = (int) GenericModel.count(condition, userId);
		} catch (Exception e) {
			Logger.error("用户->举报某个用户记录列表,查询总记录数:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载举报记录列表失败!";

			return null;
		}

		if (count < 1)
			return new ArrayList<t_user_report_users>();

		pageBean.totalCount = count;

		String hql = "select new t_user_report_users(u.name, uru.reason, uru.time, uru.situation) "
				+ condition;
		List<t_user_report_users> reportUsers = null;

		try {
			reportUsers = GenericModel.find(hql, userId).fetch(
					pageBean.currPage, pageBean.pageSize);
		} catch (Exception e) {
			Logger.error("用户->举报某个用户记录列表,查询总记录数:" + e.getMessage());
			error.msg = error.FRIEND_INFO + "加载举报记录列表失败!";

			return null;
		}

		error.code = 0;

		return reportUsers;
	}

	/**
	 * 查询黑名单会员列表
	 * 
	 * @param currPage
	 * @return
	 */
	public static PageBean<v_user_blacklist_info> queryBlacklistUserBySupervisor(
			String name, String email, String beginTimeStr, String endTimeStr,
			String key, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		Date beginTime = null;
		Date endTime = null;
		int orderType = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.PAGE_SIZE;

		if (RegexUtils.isDate(beginTimeStr)) {
			beginTime = DateUtil.strToYYMMDDDate(beginTimeStr);
		}

		if (RegexUtils.isDate(endTimeStr)) {
			endTime = DateUtil.strToYYMMDDDate(endTimeStr);
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			orderType = Integer.parseInt(orderTypeStr);
		}

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (orderType < 0 || orderType > 6) {
			orderType = 0;
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("name", name);
		conditionMap.put("email", email);
		conditionMap.put("beginTime", beginTimeStr);
		conditionMap.put("endTime", endTimeStr);
		conditionMap.put("key", key);
		conditionMap.put("orderType", orderType);

		if (StringUtils.isNotBlank(name)) {
			conditions.append("and name like ? ");
			values.add("%" + name + "%");
		}

		if (StringUtils.isNotBlank(email)) {
			conditions.append("and email like ? ");
			values.add("%" + email + "%");
		}

		if (beginTime != null) {
			conditions.append("and register_time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and register_time < ? ");
			values.add(endTime);
		}

		if (orderType != 0) {
			conditions.append("order by "
					+ Constants.BLACK_USER_ORDER[orderType]);
		}

		List<v_user_blacklist_info> users = new ArrayList<v_user_blacklist_info>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			users = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询黑名单会员列表时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询黑名单会员列表时出现异常！";

			return null;
		}

		PageBean<v_user_blacklist_info> page = new PageBean<v_user_blacklist_info>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;

		page.page = users;

		error.code = 0;

		return page;
	}

	/**
	 * 查询不同类别会员的个数(用于后台站内信群发)
	 * 
	 * @return
	 */
	public static Map<String, String> queryUserType(ErrorInfo error) {

		String sql = "select count(*) as all_user_count,count( ( case when (t_users.master_identity = 1)"
				+ " then t_users.id end)) as loan_user_count, count( ( case when (t_users.master_identity = 2)"
				+ " then t_users.id end)) as invest_user_count, count( ( case when (t_users.master_identity = 3)"
				+ " then t_users.id end)) as complex_user_count, count( ( case when (t_users.is_email_verified = 0)"
				+ " then t_users.id end)) as unverified_user_count, count( ( case when (t_users.is_blacklist = 1)"
				+ " then t_users.id end)) as black_user_count, count( ( case when ((now() - INTERVAL 7 DAY)< t_users.time)"
				+ " then t_users.id end)) as new_user_count from t_users";

		Object[] obj = null;
		try {
			obj = (Object[]) JPA.em().createNativeQuery(sql).getSingleResult();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询资金管理—>账户信息时：" + e.getMessage());

			error.code = -1;
			error.msg = "查询资金管理—>账户信息时出现异常";

			return null;
		}

		if (obj == null) {
			error.code = -1;
			error.msg = "查询资金管理—>账户信息时出现异常";

			return null;
		}

		Map<String, String> userMap = new HashMap<String, String>();
		userMap.put("all_user_count", obj[0].toString());
		userMap.put("loan_user_count", obj[1].toString());
		userMap.put("invest_user_count", obj[2].toString());
		userMap.put("complex_user_count", obj[3].toString());
		userMap.put("unverified_user_count", obj[4].toString());
		userMap.put("black_user_count", obj[5].toString());
		userMap.put("new_user_count", obj[6].toString());

		error.code = 0;

		return userMap;
	}

	/**
	 * 用于 资金管理—>账户信息
	 * 
	 * @param userId
	 * @return
	 */
	public static List<v_user_details> queryUserDetail(long userId,
			ErrorInfo error) {

		List<v_user_details> userDetails = new ArrayList<v_user_details>();

		try {
			userDetails = GenericModel.find("user_id = ? order by time desc",
					userId).fetch(1, 4);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询资金管理—>账户信息时：" + e.getMessage());

			error.code = -1;
			error.msg = "查询资金管理—>账户信息时出现异常";

			return null;
		}

		error.code = 0;

		return userDetails;
	}

	/**
	 * 我的会员账单--借款会员管理
	 * 
	 * @param userId
	 * @return
	 */
	public static PageBean<v_user_loan_info_bill> queryUserInfoBill(
			long supervisor, String typeStr, String startTimeStr,
			String endTimeStr, String name, String orderTypeStr,
			String currPageStr, String pageSizeStr, ErrorInfo error) {
		error.clear();

		int type = 0;
		int order = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		String[] typeCondition = { " ",
				" and overdue_bill_count <= 0 and bad_bid_count<= 0 ",
				" and overdue_bill_count > 0 and bad_bid_count<= 0 ",
				" and overdue_bill_count <= 0 and bad_bid_count > 0 " };
		String[] orderCondition = { " ", " order by bid_count asc ",
				" order by bid_count  desc ", " order by invest_count asc ",
				" order by invest_count desc ",
				" order by overdue_bill_count asc ",
				" order by overdue_bill_count  desc ",
				" order by bad_bid_count asc ", " order by bad_bid_count desc " };

		StringBuffer conditions = new StringBuffer(
				" 1=1 and  supervisor_id = ?  ");
		List<Object> values = new ArrayList<Object>();
		values.add(supervisor);

		PageBean<v_user_loan_info_bill> page = new PageBean<v_user_loan_info_bill>();
		List<v_user_loan_info_bill> userBills = new ArrayList<v_user_loan_info_bill>();

		int count = 0;

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
			conditions.append(typeCondition[type]);
		}

		if (StringUtils.isNotBlank(startTimeStr)
				&& StringUtils.isNotBlank(endTimeStr)) {
			Date start = DateUtil.strToYYMMDDDate(startTimeStr);
			Date end = DateUtil.strToYYMMDDDate(endTimeStr);
			conditions.append(" and  register_time > ? and register_time < ? ");
			values.add(start);
			values.add(end);
		}

		if (StringUtils.isNotBlank(name)) {
			conditions.append(" and name like ?");
			values.add("%" + name + "%");
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			order = Integer.parseInt(orderTypeStr);
			conditions.append(orderCondition[order]);
		}

		page.pageSize = pageSize;
		page.currPage = currPage;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			userBills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询本月到期账单情况时：" + e.getMessage());
			error.code = -3;
			error.msg = "";

			return page;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keywords", name);
		conditionMap.put("type", type);
		conditionMap.put("startDate", startTimeStr);
		conditionMap.put("endDate", endTimeStr);
		conditionMap.put("order", order);

		page.page = userBills;
		page.totalCount = count;
		page.conditions = conditionMap;

		error.code = 0;

		return page;
	}

	/**
	 * 我的会员账单--坏账会员管理
	 * 
	 * @param userId
	 * @return
	 */
	public static PageBean<v_user_loan_info_bad> queryUserInfoBad(
			long supervisor, String typeStr, String startTimeStr,
			String endTimeStr, String name, String orderTypeStr,
			String currPageStr, String pageSizeStr, ErrorInfo error) {
		error.clear();

		int type = 0;
		int order = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		String[] typeCondition = { " ",
				" and overdue_bill_count <= 0 and bad_bid_count<= 0 ",
				" and overdue_bill_count > 0 and bad_bid_count<= 0 ",
				" and overdue_bill_count <= 0 and bad_bid_count > 0 " };
		String[] orderCondition = { " ", " order by bid_count asc ",
				" order by bid_count  desc ", " order by invest_count asc ",
				" order by invest_count desc ",
				" order by overdue_bill_count asc ",
				" order by overdue_bill_count  desc ",
				" order by bad_bid_count asc ", " order by bad_bid_count desc " };

		StringBuffer conditions = new StringBuffer(
				" 1=1 and  supervisor_id = ?  ");
		List<Object> values = new ArrayList<Object>();
		values.add(supervisor);

		PageBean<v_user_loan_info_bad> page = new PageBean<v_user_loan_info_bad>();
		List<v_user_loan_info_bad> userBills = new ArrayList<v_user_loan_info_bad>();

		int count = 0;

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
			conditions.append(typeCondition[type]);
		}

		if (StringUtils.isNotBlank(startTimeStr)
				&& StringUtils.isNotBlank(endTimeStr)) {
			Date start = DateUtil.strToYYMMDDDate(startTimeStr);
			Date end = DateUtil.strToYYMMDDDate(endTimeStr);
			conditions.append(" and  register_time > ? and register_time < ? ");
			values.add(start);
			values.add(end);
		}

		if (StringUtils.isNotBlank(name)) {
			conditions.append(" and name like ?");
			values.add("%" + name + "%");
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			order = Integer.parseInt(orderTypeStr);
			conditions.append(orderCondition[order]);
		}

		page.pageSize = pageSize;
		page.currPage = currPage;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			userBills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询本月到期账单情况时：" + e.getMessage());
			error.code = -3;
			error.msg = "";

			return page;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keywords", name);
		conditionMap.put("type", type);
		conditionMap.put("startDate", startTimeStr);
		conditionMap.put("endDate", endTimeStr);
		conditionMap.put("order", order);

		page.page = userBills;
		page.totalCount = count;
		page.conditions = conditionMap;

		error.code = 0;

		return page;
	}

	/**
	 * 部门账单管理--借款会员管理
	 * 
	 * @param userId
	 * @return
	 */
	public static PageBean<v_user_loan_info_bill_d> queryUserInfoBillD(
			String typeStr, String startDateStr, String endDateStr,
			String name, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		int type = 0;
		int order = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		String[] typeCondition = { " ",
				" and overdue_bill_count <= 0 and bad_bid_count<= 0 ",
				" and overdue_bill_count > 0 and bad_bid_count<= 0 ",
				" and overdue_bill_count <= 0 and bad_bid_count > 0 " };
		String[] orderCondition = { " ", " order by bid_count asc ",
				" order by bid_count  desc ", " order by invest_count asc ",
				" order by invest_count desc ",
				" order by overdue_bill_count asc ",
				" order by overdue_bill_count  desc ",
				" order by bad_bid_count asc ", " order by bad_bid_count desc " };

		StringBuffer conditions = new StringBuffer(" 1=1   ");
		List<Object> values = new ArrayList<Object>();

		PageBean<v_user_loan_info_bill_d> page = new PageBean<v_user_loan_info_bill_d>();
		List<v_user_loan_info_bill_d> userBills = new ArrayList<v_user_loan_info_bill_d>();

		int count = 0;

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
			conditions.append(typeCondition[type]);
		}

		if (StringUtils.isNotBlank(startDateStr)
				&& StringUtils.isNotBlank(endDateStr)) {
			Date start = DateUtil.strToYYMMDDDate(startDateStr);
			Date end = DateUtil.strToYYMMDDDate(endDateStr);
			conditions.append(" and  register_time > ? and register_time < ? ");
			values.add(start);
			values.add(end);
		}

		if (StringUtils.isNotBlank(name)) {
			conditions.append(" and name like ?");
			values.add("%" + name + "%");
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			order = Integer.parseInt(orderTypeStr);
			conditions.append(orderCondition[order]);
		}

		page.pageSize = pageSize;
		page.currPage = currPage;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			userBills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询本月到期账单情况时：" + e.getMessage());
			error.code = -3;
			error.msg = "";

			return page;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keywords", name);
		conditionMap.put("type", type);
		conditionMap.put("startDate", startDateStr);
		conditionMap.put("endDate", endDateStr);
		conditionMap.put("order", order);

		page.page = userBills;
		page.totalCount = count;
		page.conditions = conditionMap;

		error.code = 0;

		return page;
	}

	/**
	 * 部门账单管理--借款会员管理
	 * 
	 * @param userId
	 * @return
	 */
	public static PageBean<v_bid_assigned> queryBidInfoBillD(String typeStr,
			String startDateStr, String endDateStr, String name,
			String orderTypeStr, String currPageStr, String pageSizeStr,
			ErrorInfo error) {
		error.clear();

		// int type = 0;
		int order = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		// String [] typeCondition =
		// {" "," and overdue_bill_count <= 0 and bad_bid_count<= 0 ",
		// " and overdue_bill_count > 0 and bad_bid_count<= 0 "," and overdue_bill_count <= 0 and bad_bid_count > 0 "};
		String[] orderCondition = { " ", " order by amount asc ",
				" order by amount desc ", " order by apr asc ",
				" order by apr desc " };

		StringBuffer conditions = new StringBuffer(" 1=1   ");
		List<Object> values = new ArrayList<Object>();

		PageBean<v_bid_assigned> page = new PageBean<v_bid_assigned>();
		List<v_bid_assigned> userBills = new ArrayList<v_bid_assigned>();

		int count = 0;

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		// if(StringUtils.isNotBlank(startDateStr) &&
		// StringUtils.isNotBlank(endDateStr)){
		// Date start = DateUtil.strToYYMMDDDate(startDateStr);
		// Date end = DateUtil.strToYYMMDDDate(endDateStr);
		// conditions.append(" and  register_time > ? and register_time < ? ");
		// values.add(start);
		// values.add(end);
		// }

		if (StringUtils.isNotBlank(name)) {
			conditions.append(" and user_name like ?");
			values.add("%" + name + "%");
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			order = Integer.parseInt(orderTypeStr);
			conditions.append(orderCondition[order]);
		}

		page.pageSize = pageSize;
		page.currPage = currPage;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			userBills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询本月到期账单情况时：" + e.getMessage());
			error.code = -3;
			error.msg = "";

			return page;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keywords", name);
		// conditionMap.put("type", type);
		conditionMap.put("startDate", startDateStr);
		conditionMap.put("endDate", endDateStr);
		conditionMap.put("order", order);

		page.page = userBills;
		page.totalCount = count;
		page.conditions = conditionMap;

		error.code = 0;

		return page;
	}

	/**
	 * 部门账单管理--坏账会员管理
	 * 
	 * @param userId
	 * @return
	 */
	public static PageBean<v_user_loan_info_bad_d> queryUserInfoBadD(
			String typeStr, String startDateStr, String endDateStr,
			String name, String orderTypeStr, String currPageStr,
			String pageSizeStr, ErrorInfo error) {
		error.clear();

		int type = 0;
		int order = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		String[] typeCondition = { " ",
				" and overdue_bill_count <= 0 and bad_bid_count<= 0 ",
				" and overdue_bill_count > 0 and bad_bid_count<= 0 ",
				" and overdue_bill_count <= 0 and bad_bid_count > 0 " };
		String[] orderCondition = { " ", " order by bid_count asc ",
				" order by bid_count  desc ", " order by invest_count asc ",
				" order by invest_count desc ",
				" order by overdue_bill_count asc ",
				" order by overdue_bill_count  desc ",
				" order by bad_bid_count asc ", " order by bad_bid_count desc " };

		StringBuffer conditions = new StringBuffer(" 1=1   ");
		List<Object> values = new ArrayList<Object>();

		PageBean<v_user_loan_info_bad_d> page = new PageBean<v_user_loan_info_bad_d>();
		List<v_user_loan_info_bad_d> userBills = new ArrayList<v_user_loan_info_bad_d>();

		int count = 0;

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (NumberUtil.isNumericInt(typeStr)) {
			type = Integer.parseInt(typeStr);
			conditions.append(typeCondition[type]);
		}

		if (StringUtils.isNotBlank(startDateStr)
				&& StringUtils.isNotBlank(endDateStr)) {
			Date start = DateUtil.strToYYMMDDDate(startDateStr);
			Date end = DateUtil.strToYYMMDDDate(endDateStr);
			conditions.append(" and  register_time > ? and register_time < ? ");
			values.add(start);
			values.add(end);
		}

		if (StringUtils.isNotBlank(name)) {
			conditions.append(" and name like ?");
			values.add("%" + name + "%");
		}

		if (NumberUtil.isNumericInt(orderTypeStr)) {
			order = Integer.parseInt(orderTypeStr);
			conditions.append(orderCondition[order]);
		}

		page.pageSize = pageSize;
		page.currPage = currPage;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			userBills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询本月到期账单情况时：" + e.getMessage());
			error.code = -3;
			error.msg = "";

			return page;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("keywords", name);
		conditionMap.put("type", type);
		conditionMap.put("startDate", startDateStr);
		conditionMap.put("endDate", endDateStr);
		conditionMap.put("order", order);

		page.page = userBills;
		page.totalCount = count;
		page.conditions = conditionMap;

		error.code = 0;

		return page;
	}

	/**
	 * 部门账单管理--待分配的借款会员列表
	 * 
	 * @param userId
	 * @return
	 */
	public static PageBean<v_user_loan_user_unassigned> queryUserUnassigned(
			String name, String startDate, String endDate, int productIdStr,
			String orderType, String currPageStr, String pageSizeStr,
			ErrorInfo error) {
		error.clear();

		int type = 0;
		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;
		String[] orderCondition = { " ", " order by register_time asc ",
				" order by register_time  desc ", " order by bid_no asc ",
				" order by bid_no desc ", " order by amount asc ",
				" order by amount  desc " };

		StringBuffer conditions = new StringBuffer(" 1=1   ");
		List<Object> values = new ArrayList<Object>();

		List<v_user_loan_user_unassigned> userBills = new ArrayList<v_user_loan_user_unassigned>();
		int count = 0;

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		if (productIdStr != 0) {
			conditions.append(" and product_id = ?");
			values.add(productIdStr);
		}

		if (StringUtils.isNotBlank(startDate)
				&& StringUtils.isNotBlank(endDate)) {
			Date start = DateUtil.strToYYMMDDDate(startDate);
			Date end = DateUtil.strToYYMMDDDate(endDate);
			conditions.append(" and  register_time > ? and register_time < ? ");
			values.add(start);
			values.add(end);
		}

		if (StringUtils.isNotBlank(name)) {
			conditions.append(" and  name like ? ");
			values.add("%" + name + "%");
		}

		if (NumberUtil.isNumericInt(orderType)) {
			type = Integer.parseInt(orderType);
			conditions.append(orderCondition[type]);
		}

		PageBean<v_user_loan_user_unassigned> page = new PageBean<v_user_loan_user_unassigned>();

		try {
			count = (int) GenericModel.count(
					conditions.toString(), values.toArray());
			userBills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询本月到期账单情况时：" + e.getMessage());
			error.code = -3;
			error.msg = "";

			return null;
		}

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		conditionMap.put("name", name);
		conditionMap.put("startDate", startDate);
		conditionMap.put("endDate", endDate);
		conditionMap.put("order", type);

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = userBills;
		page.totalCount = count;
		page.conditions = conditionMap;

		error.code = 0;

		return page;
	}

	/**
	 * 查询用户所有的交易记录
	 * 
	 * @param error
	 * @return
	 */
	public static List<v_user_details> queryAllDetails(ErrorInfo error) {
		error.clear();

		List<v_user_details> details = new ArrayList<v_user_details>();

		try {
			details = GenericModel.find("user_id = ?", User.currUser().id)
					.fetch();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询交易记录时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询交易记录失败";

			return null;
		}

		error.code = 0;

		return details;
	}

	/**
	 * 查询交易记录
	 * 
	 * @param userId
	 * @param type
	 *            类别
	 * @param beginTime
	 *            开始时间
	 * @param endTime
	 *            结束时间
	 * @param currPage
	 * @param pageSize
	 * @return
	 */

	public static PageBean<v_user_details> queryUserDetails(long userId,
			long type, Date beginTime, Date endTime, int currPage, int pageSize) {
		if (currPage == 0) {
			currPage = 1;
		}

		if (pageSize == 0) {
			pageSize = Constants.PAGE_SIZE;
		}

		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> conditionMap = new HashMap<String, Object>();

		conditionMap.put("type", type);
		conditionMap.put("dateBegin", beginTime);
		conditionMap.put("dateEnd", endTime);

		if (type != 0) {
			switch ((int) type) {
			/* 充值 */
			case 1:
				conditions
						.append("and (operation = ? or operation = ? or operation = ?) ");
				values.add(NumberUtil.getLongVal(DealType.RECHARGE_USER));
				values.add(NumberUtil.getLongVal(DealType.RECHARGE_HAND));
				values.add(NumberUtil.getLongVal(DealType.RECHARGE_OFFLINE));

				break;

			/* 提现 */
			case 2:
				conditions.append("and (operation = ? or "
						+ "operation = ? or " + "operation = ? or "
						+ "operation = ? or " + "operation = ?) ");
				values.add(NumberUtil.getLongVal(DealType.THAW_WITHDRAWALT));
				values.add(NumberUtil.getLongVal(DealType.FREEZE_WITHDRAWAL));
				values.add(NumberUtil.getLongVal(DealType.FREEZE_WITHDRAWAL_P));
				values.add(NumberUtil
						.getLongVal(DealType.CHARGE_LEFT_WITHDRAWALT));
				values.add(NumberUtil
						.getLongVal(DealType.CHARGE_AWARD_WITHDRAWALT));

				break;

			/* 服务费 */
			case 3:
				conditions.append("and (operation = ? or "
						+ "operation = ? or " + "operation = ? or "
						+ "operation = ? or " + "operation = ?) ");
				values.add(NumberUtil.getLongVal(DealType.CHARGE_RECHARGE_FEE));
				values.add(NumberUtil.getLongVal(DealType.CHARGE_WITHDRAWALT));
				values.add(NumberUtil
						.getLongVal(DealType.CHARGE_LOAN_SERVER_FEE));
				values.add(NumberUtil.getLongVal(DealType.CHARGE_INVEST_FEE));
				values.add(NumberUtil
						.getLongVal(DealType.CHARGE_DEBT_TRANSFER_MANAGEFEE));

				break;

			/* 账单还款 */
			case 4:
				conditions.append("and (operation = ? or operation = ?) ");
				values.add(NumberUtil.getLongVal(DealType.CHARGE_OVER_PAY));
				values.add(NumberUtil.getLongVal(DealType.CHARGE_NOMAL_PAY));

				break;

			/* 账单收入 */
			case 5:
				conditions
						.append("and (operation = ? or operation = ? or operation = ? or operation = ?) ");
				values.add(NumberUtil.getLongVal(DealType.PRICIPAL_PAY));
				values.add(NumberUtil.getLongVal(DealType.OVER_RECEIVE));
				values.add(NumberUtil.getLongVal(DealType.NOMAL_RECEIVE));
				values.add(NumberUtil.getLongVal(DealType.OFFLINE_COLLECTION));

				break;
			}
		}

		if (beginTime != null) {
			conditions.append("and time > ? ");
			values.add(beginTime);
		}

		if (endTime != null) {
			conditions.append("and time < ? ");
			values.add(endTime);
		}

		conditions.append("and user_id = ?");
		values.add(userId);
		conditions.append("and amount > 0");

		PageBean<v_user_details> page = new PageBean<v_user_details>();
		page.currPage = currPage;
		page.pageSize = pageSize;
		page.conditions = conditionMap;

		List<v_user_details> userDetails = new ArrayList<v_user_details>();

		try {
			page.totalCount = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			userDetails = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, page.pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询交易记录时：" + e.getMessage());

			return null;
		}

		page.page = userDetails;

		return page;
	}

	/**
	 * 用户最近动态
	 * 
	 * @param userId
	 * @param currPage
	 * @param pageSize
	 * @return
	 */
	public static PageBean<t_user_events> queryUserEvnets(long userId,
			ErrorInfo error, String currPageStr, String pageSizeStr) {
		error.clear();

		int currPage = Constants.ONE;
		int pageSize = Constants.TWO;

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		List<t_user_events> userDetails = new ArrayList<t_user_events>();
		int count = 0;

		try {
			count = (int) GenericModel.count("user_id = ?", userId);
			userDetails = GenericModel.find("user_id = ? order by time desc",
					userId).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询用户最近动态时：" + e.getMessage());
			error.code = -1;
			error.msg = "用户最近动态查询失败";

			return null;
		}

		PageBean<t_user_events> page = new PageBean<t_user_events>();
		page.currPage = currPage;
		page.pageSize = pageSize;
		page.totalCount = count;

		page.page = userDetails;

		error.code = 0;

		return page;
	}

	/**
	 * 理财情况统计表
	 * 
	 * @param userId
	 * @param currPage
	 * @param pageSize
	 * @return
	 */
	public static PageBean<v_bill_invest_statistics> queryUserInvestStatistics(
			long userId, int year, int month, int orderType, int currPage,
			ErrorInfo error) {
		error.clear();

		Map<String, Object> conditionMap = new HashMap<String, Object>();
		List<Object> values = new ArrayList<Object>();
		User user = User.currUser();

		conditionMap.put("year", year);
		conditionMap.put("month", month);
		conditionMap.put("orderType", orderType);

		StringBuffer conditions = new StringBuffer("1=1 and user_id = ? ");

		values.add(user.id);

		if (year != 0) {
			conditions.append("and year = ? ");
			values.add(year);
		}

		if (month != 0) {
			conditions.append("and month = ? ");
			values.add(month);
		}

		if (orderType != 0) {
			conditions.append(Constants.INVEST_STATISTICS[orderType]);
		}

		List<v_bill_invest_statistics> bills = new ArrayList<v_bill_invest_statistics>();
		int count = 0;

		try {
			count = (int) GenericModel.count(conditions.toString(),
					values.toArray());
			bills = GenericModel.find(conditions.toString(),
					values.toArray()).fetch(currPage, Constants.PAGE_SIZE);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询理财情况统计表时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询理财情况统计表时出现异常";

			return null;
		}

		PageBean<v_bill_invest_statistics> page = new PageBean<v_bill_invest_statistics>();

		page.pageSize = Constants.PAGE_SIZE;
		page.currPage = currPage;
		page.page = bills;
		page.totalCount = count;
		page.conditions = conditionMap;

		error.code = 0;

		return page;
	}

	/**
	 * 前台--站内信
	 * 
	 * @param userId
	 * @param currPageStr
	 * @param pageSizeStr
	 * @param error
	 * @return
	 */
	public static PageBean<v_user_for_message> queryUserForMessage(long userId,
			String currPageStr, String pageSizeStr, ErrorInfo error) {
		error.clear();

		int currPage = Constants.ONE;
		int pageSize = Constants.TEN;

		if (NumberUtil.isNumericInt(currPageStr)) {
			currPage = Integer.parseInt(currPageStr);
		}

		if (NumberUtil.isNumericInt(pageSizeStr)) {
			pageSize = Integer.parseInt(pageSizeStr);
		}

		List<v_user_for_message> users = new ArrayList<v_user_for_message>();
		int count = 0;

		try {
			count = (int) GenericModel.count("user_id = ?", userId);
			users = GenericModel.find("user_id = ?", userId).fetch(
					currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("发站内信选择会员时：" + e.getMessage());

			return null;
		}

		PageBean<v_user_for_message> page = new PageBean<v_user_for_message>();

		page.pageSize = pageSize;
		page.currPage = currPage;
		page.page = users;
		page.totalCount = count;

		error.code = 0;

		return page;
	}

	/**
	 * 查询账户信息（资金管理中的账户信息）
	 * 
	 * @param userId
	 * @return
	 */
	public static v_user_account_info queryUserAccountInfo(long userId,
			ErrorInfo error) {
		error.clear();

		v_user_account_info accountInfo = null;

		try {
			accountInfo = GenericModel.findById(userId);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询账户信息（资金管理中的账户信息）时：" + e.getMessage());
			error.code = -1;
			error.msg = "查询账户信息（资金管理中的账户信息）出现异常！";

			return null;
		}

		error.code = 0;

		return accountInfo;
	}

	/**
	 * 查询注册会员总数
	 * 
	 * @param error
	 * @return
	 */
	public static long queryTotalRegisterUserCount(ErrorInfo error) {
		error.clear();

		long count = 0;

		try {
			count = GenericModel.count();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("数据库异常");
			error.code = -1;
			error.msg = "查询注册会员总数失败";

			return -1;
		}

		error.code = 0;

		return count;
	}

	/**
	 * 查询今日注册会员总数
	 * 
	 * @param error
	 * @return
	 */
	public static long queryTodayRegisterUserCount(ErrorInfo error) {
		error.clear();

		long count = 0;

		try {
			count = GenericModel.count("DATEDIFF(NOW(), time) < 1");
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("数据库异常");
			error.code = -1;
			error.msg = "查询今日注册会员数失败";

			return -1;
		}

		error.code = 0;

		return count;
	}

	/**
	 * 查询在线会员数量
	 * 
	 * @return
	 */
	public static long queryOnlineUserNum() {
		return CacheManager.getCacheSize("online_user_");
	}

	public long successLoanCount; // 成功借款次数
	public long normalRepaymentCount; // 正常还款次数
	public long overdueRepaymentCount; // 逾期
	public long flowBids; // 流标

	public long getSuccessLoanCount() {

		ErrorInfo error = new ErrorInfo();

		return querySuccessLoanCount(this.id, error);
	}

	public long getNormalRepaymentCount() {
		ErrorInfo error = new ErrorInfo();

		return queryNormalRepaymentCount(this.id, error);
	}

	public long getOverdueRepaymentCount() {
		ErrorInfo error = new ErrorInfo();

		return queryOverdueRepaymentCount(this.id, error);
	}

	public long getFlowBids() {
		ErrorInfo error = new ErrorInfo();
		return queryFlowBids(this.id, error);
	}

	/**
	 * 统计用户成功借款次数
	 * 
	 * @param userId
	 * @param error
	 * @return
	 */
	public static long querySuccessLoanCount(long userId, ErrorInfo error) {
		String sql = "select count(*) from t_bids where user_id = ? and status in (4,5)";

		long count = 0;

		try {
			count = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		error.code = 1;
		return count;
	}

	/**
	 * 用户正常还款次数
	 * 
	 * @param userId
	 * @return
	 */
	public static long queryNormalRepaymentCount(long userId, ErrorInfo error) {

		long count = 0;
		List<Long> bidIds = null;

		String sql = "select id from t_bids where user_id = ? and status in (4,5)";

		try {
			bidIds = GenericModel.find(sql, userId).fetch();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (bidIds.size() > 0) {

			String idStr = StringUtils.join(bidIds, ",");

			sql = "select count(*) from t_bills where bid_id in ( " + idStr
					+ " ) and status = 0";

			try {
				count = GenericModel.find(sql).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
			}
		}
		error.code = 1;
		return count;
	}

	/**
	 * 用户逾期还款次数
	 * 
	 * @param userId
	 * @return
	 */
	public static long queryOverdueRepaymentCount(long userId, ErrorInfo error) {

		long count = 0;
		List<Long> bidIds = null;

		String sql = "select id from t_bids where user_id = ? and status in (4,5)";

		try {
			bidIds = GenericModel.find(sql, userId).fetch();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (bidIds.size() > 0) {

			String idStr = StringUtils.join(bidIds, ",");

			sql = "select count(*) from t_bills where bid_id in ( " + idStr
					+ " ) and status in (-2,-3) ";

			try {
				count = GenericModel.find(sql).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
			}
		}
		error.code = 1;
		return count;
	}

	/**
	 * 用户借入金额
	 * 
	 * @return
	 */
	public static Double loanAmount(long userId, ErrorInfo error) {

		Double loanAmount = 0.0;

		String sql = "select sum(amount) from t_bids where user_id = ? and status in (4,5)";

		try {
			loanAmount = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (null == loanAmount) {
			loanAmount = 0.0;
		}
		error.code = 1;
		return loanAmount;
	}

	/**
	 * 用户待付款金额
	 * 
	 * @param userId
	 * @param error
	 * @return
	 */
	public static Double pendingRepaymentAmount(long userId, ErrorInfo error) {

		Double count = 0.0;

		List<Long> bidIds = null;

		String sql = "select id from t_bids where user_id = ? and status = 4 ";

		try {
			bidIds = GenericModel.find(sql, userId).fetch();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (bidIds.size() > 0) {

			String idStr = StringUtils.join(bidIds, ",");
			sql = "select sum(repayment_corpus + repayment_interest +overdue_fine) from t_bills where bid_id in ( "
					+ idStr + " ) and status in (-1,-2)";

			try {
				count = GenericModel.find(sql).first();
			} catch (Exception e) {
				e.printStackTrace();
				error.code = -1;
			}

			if (null == count) {
				count = 0.0;
			}
		}

		error.code = 1;
		return count;

	}

	/**
	 * 用户理财投标笔数
	 * 
	 * @param userId
	 * @param error
	 * @return
	 */
	public static long financialCount(long userId, ErrorInfo error) {

		long count = 0;

		String sql = "select count(*) from t_invests where user_id = ?";

		try {
			count = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}
		error.code = 1;
		return count;

	}

	/**
	 * 用户待收金额
	 * 
	 * @param userId
	 * @param error
	 * @return
	 */
	public static Double receivingAmount(long userId, ErrorInfo error) {

		Double count = 0.0;

		String sql = "select sum(receive_corpus + receive_interest + overdue_fine) from t_bill_invests where user_id = ? and status in (-1,-2)";

		try {
			count = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		if (null == count) {
			count = 0.0;
		}
		error.code = 1;
		return count;
	}

	/**
	 * 用户流标次数
	 * 
	 * @param userId
	 * @param error
	 * @return
	 */
	public static long queryFlowBids(long userId, ErrorInfo error) {

		long count = 0;

		String sql = "select count(*) from t_bids where user_id = ? and status in (-1,-2,-3,-4)";

		try {
			count = GenericModel.find(sql, userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
		}

		error.code = 1;
		return count;
	}

	/**
	 * 查询借款用户历史记录情况
	 * 
	 * @param userId
	 * @return
	 */
	public static Map<String, String> historySituation(long userId,
			ErrorInfo error) {

		Map<String, String> historySituationMap = new HashMap<String, String>();
		java.text.DecimalFormat df = new java.text.DecimalFormat("0.00");// 保留2位小数

		long successLoanCount = querySuccessLoanCount(userId, error);

		historySituationMap.put("successBidCount", successLoanCount + "");// 成功借款次数

		long normalRepaymentCount = queryNormalRepaymentCount(userId, error);

		historySituationMap.put("normalRepaymentCount", normalRepaymentCount
				+ "");// 正常还款次数

		long overdueRepaymentCount = queryOverdueRepaymentCount(userId, error);

		historySituationMap.put("overdueRepaymentCount", overdueRepaymentCount
				+ "");// 逾期还款次数

		double loanAmount = loanAmount(userId, error);

		historySituationMap.put("loanAmount", loanAmount + "");// 借入金额

		double pendingRepaymentAmount = pendingRepaymentAmount(userId, error);

		historySituationMap.put("pendingRepaymentAmount",
				df.format(pendingRepaymentAmount));// 待付款金额

		long financialCount = financialCount(userId, error);

		historySituationMap.put("financialCount", financialCount + "");// 理财投标笔数

		Double receivingAmount = receivingAmount(userId, error);

		historySituationMap.put("receivingAmount", df.format(receivingAmount));// 待收金额

		long flowBids = queryFlowBids(userId, error);

		historySituationMap.put("flowBids", flowBids + "");// 流标次数

		return historySituationMap;
	}

	/**
	 * 查询债权用户历史记录情况
	 * 
	 * @param userId
	 * @return
	 */
	public static Map<String, String> debtUserhistorySituation(long userId,
			ErrorInfo error) {

		Map<String, String> historySituationMap = new HashMap<String, String>();

		long successBidCount = querySuccessLoanCount(userId, error);

		historySituationMap.put("successBidCount", successBidCount + "");// 成功借款次数

		long normalRepaymentCount = queryNormalRepaymentCount(userId, error);

		historySituationMap.put("normalRepaymentCount", normalRepaymentCount
				+ "");// 正常还款次数

		long overdueRepaymentCount = queryOverdueRepaymentCount(userId, error);

		historySituationMap.put("overdueRepaymentCount", overdueRepaymentCount
				+ "");// 逾期还款次数

		long flowBids = queryFlowBids(userId, error);

		historySituationMap.put("flowBids", flowBids + "");// 流标次数

		return historySituationMap;
	}

	/**
	 * 环迅
	 * 
	 * @param money
	 * @param bankType
	 * @param error
	 * @return
	 */
	public static Map<String, String> ipay(BigDecimal money, int bankType,
			int type, ErrorInfo error) {
		error.clear();

		if (bankType < 0 || bankType > 7) {
			error.code = -1;
			error.msg = "传入参数有误";

			return null;
		}

		t_dict_payment_gateways gateway = gateway(Constants.IPS_GATEWAY, error);

		if (error.code < 0) {
			return null;
		}

		DecimalFormat currentNumberFormat = new DecimalFormat("#0.00");
		Map<String, String> args = new HashMap<String, String>();

		String billno = NumberUtil.getBillNo(type);

		args.put("Mer_code", gateway.pid);
		args.put("Billno", billno);
		args.put("Amount", currentNumberFormat.format(money));
		args.put("Date", DateUtil.simple(new Date()));
		args.put("Currency_Type", "RMB");
		args.put("Gateway_Type", Constants.IPS_TYPE[bankType]);
		args.put("Lang", "");
		args.put("Merchanturl", Constants.IPS_MERCHANT_URL);
		args.put("Attach", "");
		args.put("OrderEncodeType", "5");
		args.put("RetEncodeType", "17");
		args.put("Rettype", "1");
		args.put("ServerUrl", Constants.IPS_MERCHANT_URL);
		StringBuilder singnMd5 = new StringBuilder();
		singnMd5.append("billno");
		singnMd5.append(args.get("Billno"));
		singnMd5.append("currencytype");
		singnMd5.append(args.get("Currency_Type"));
		singnMd5.append("amount");
		singnMd5.append(args.get("Amount"));
		singnMd5.append("date");
		singnMd5.append(args.get("Date"));
		singnMd5.append("orderencodetype");
		singnMd5.append(args.get("OrderEncodeType"));
		String ips = gateway._key;
		singnMd5.append(ips);

		cryptix.jce.provider.MD5 b = new cryptix.jce.provider.MD5();

		args.put("SignMD5", b.toMD5(singnMd5.toString()).toLowerCase());
		String url = Constants.IPS_URL;
		args.put("url", url);

		sequence(2, billno, money.doubleValue(), Constants.GATEWAY_RECHARGE,
				error);

		if (error.code < 0) {
			return null;
		}

		return args;
	}

	/**
	 * 所有支付接口
	 * 
	 * @param error
	 * @return
	 */
	public static List<t_dict_payment_gateways> gateways(ErrorInfo error) {
		error.clear();

		List<t_dict_payment_gateways> gateways = null;

		try {
			gateways = GenericModel.findAll();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询支付方式时：" + e.getMessage());

			error.code = -1;
			error.msg = "查询支付方式失败";

			return null;
		}

		error.code = 0;

		return gateways;
	}

	/**
	 * 用于前台充值，判断支付方式是否使用
	 * 
	 * @param error
	 * @return
	 */
	public static List<t_dict_payment_gateways> gatewayForUse(ErrorInfo error) {
		error.clear();

		List<t_dict_payment_gateways> gateways = null;

		String sql = "select new t_dict_payment_gateways(id, is_use) from t_dict_payment_gateways";

		try {
			gateways = GenericModel.find(sql).fetch();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询支付方式是否使用时：" + e.getMessage());

			error.code = -1;
			error.msg = "查询支付方式失败";

			return null;
		}

		error.code = 0;

		return gateways;
	}

	/**
	 * 支付接口
	 */
	public static t_dict_payment_gateways gateway(long way, ErrorInfo error) {
		t_dict_payment_gateways gateway = null;

		try {
			gateway = GenericModel.find("id = ? and is_use = ?",
					way, true).first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询支付方式时：" + e.getMessage());

			error.code = -1;
			error.msg = "查询支付方式失败";

			return null;
		}

		if (gateway == null) {
			error.code = -1;
			error.msg = "您选择的充值方式已停止使用，请选择其他充值方式";

			return null;
		}

		error.code = 0;

		return gateway;
	}

	/**
	 * 保存支付接口
	 */
	public static void saveGateways(int select1, String account1, String pid1,
			String key1, int select2, String account2, String pid2,
			String key2, ErrorInfo error) {
		error.clear();

		Query query1 = JPA
				.em()
				.createQuery(
						"update t_dict_payment_gateways set "
								+ " account =  ?,  pid = ? , _key = ?, is_use = ? where id= 1")
				.setParameter(1, account1).setParameter(2, pid1)
				.setParameter(3, key1)
				.setParameter(4, select1 == 0 ? false : true);

		Query query2 = JPA
				.em()
				.createQuery(
						"update t_dict_payment_gateways set "
								+ " account =  ?,  pid = ? , _key = ?, is_use = ? where id= 2")
				.setParameter(1, account2).setParameter(2, pid2)
				.setParameter(3, key2)
				.setParameter(4, select2 == 0 ? false : true);

		try {
			query1.executeUpdate();
			query2.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("保存支付接口时：" + e.getMessage());

			error.code = -1;
			error.msg = "保存支付接口失败";

			return;
		}

		DealDetail.supervisorEvent(Supervisor.currSupervisor().id,
				SupervisorEvent.PAYMENT_SET, "设置支付方式", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return;
		}

		error.msg = "保存成功";
	}

	/**
	 * 充值前
	 * 
	 * @param payNumber
	 * @param amount
	 * @param error
	 */
	public static void sequence(int gateway, String payNumber, double amount,
			int type, ErrorInfo error) {
		error.clear();

		t_user_recharge_details detail = new t_user_recharge_details();

		detail.user_id = User.currUser().id;
		// detail.user_id = 1;
		detail.time = new Date();
		detail.payment_gateway_id = gateway;
		detail.pay_number = payNumber;
		detail.amount = amount;
		detail.is_completed = false;
		detail.type = type;

		try {
			detail.save();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("充值插入充值数据时：" + e.getMessage());

			error.code = -1;
			error.msg = "充值请求失败";

			return;
		}

		error.code = 0;
	}

	/**
	 * 手工充值
	 * 
	 * @param userId
	 * @param gateway
	 * @param amount
	 * @param type
	 * @param error
	 */
	public static void sequence(long userId, int gateway, double amount,
			int type, ErrorInfo error) {
		error.clear();

		t_user_recharge_details detail = new t_user_recharge_details();

		detail.user_id = userId;
		detail.type = type;
		detail.amount = amount;
		detail.time = new Date();
		detail.completed_time = new Date();
		detail.payment_gateway_id = gateway;
		detail.is_completed = true;

		try {
			detail.save();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("充值插入充值数据时：" + e.getMessage());

			error.code = -1;
			error.msg = "充值请求失败";

			return;
		}

		error.code = 0;
	}

	/**
	 * 充值确认
	 * 
	 * @param payNumber
	 * @param amount
	 * @param error
	 */
	public static void recharge(String payNumber, double amount, ErrorInfo error) {
		error.clear();

		long count = 0;
		long count2 = 0;
		long userId = -1;
		t_user_recharge_details user_recharge = null;
		try {
			count = GenericModel.count(
					"pay_number = ? and amount = ?", payNumber, amount);
			count2 = GenericModel.count(
					"pay_number = ? and is_completed = ?", payNumber, true);
			user_recharge = GenericModel.find(
					"pay_number = ? and is_completed = ?", payNumber, false)
					.first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("判断是否已经充值时：" + e.getMessage());

			error.code = -1;
			error.msg = "充值回调失败";

			return;
		}

		if (count > 0 || count2 > 0) {
			error.code = -1;
			error.msg = "充值已完成";

			return;
		}

		userId = Convert.strToLong(user_recharge.user_id + "", -1);
		if (userId < 0) {
			error.code = -1;
			error.msg = "不存在的用户";

			return;
		}

		User user = new User();
		user.id = userId;

		DataSafety data = new DataSafety();

		data.id = user.id;

		if (!data.signCheck(error)) {
			JPA.setRollbackOnly();

			return;
		}

		t_system_recharge_completed_sequences sequence = new t_system_recharge_completed_sequences();

		sequence.pay_number = payNumber;
		sequence.time = new Date();
		sequence.amount = amount;

		String sql = "update t_user_recharge_details set is_completed = ?, completed_time = ? where pay_number = ? and is_completed = 0";

		int rows = 0;

		try {
			sequence.save();
			rows = JPA.em().createQuery(sql).setParameter(1, true)
					.setParameter(2, new Date()).setParameter(3, payNumber)
					.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("在充值确认插入更新数据时：" + e.getMessage());

			error.code = -1;
			error.msg = "充值回调失败";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		v_user_for_details forDetail = DealDetail.queryUserBalance(user.id,
				error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		double balance = forDetail.user_amount;
		double balance2 = forDetail.user_amount2;
		int rechargeType = Convert.strToInt(payNumber.split("type")[0],
				RechargeType.Normal);

		// t_user_recharge_details welfare1 =
		// t_user_recharge_details.find("is_completed=? and type=? and pay_number = ?",true,RechargeType.welfare,payNumber).first();

		t_user_recharge_details user_Type = GenericModel.find(
				"pay_number = ? and is_completed = ?", payNumber, true).first();
		if (user_Type.type == RechargeType.welfare) {
			balance += amount;
			DealDetail detail = new DealDetail(user.id, DealType.RECHARGE_USER,
					amount, sequence.id, balance, forDetail.freeze,
					forDetail.receive_amount, "公益充值");

			detail.addDealDetail(error);

		}

		if (user_Type.type == RechargeType.Normal) {
			balance += amount;
			DealDetail detail = new DealDetail(user.id, DealType.RECHARGE_USER,
					amount, sequence.id, balance, forDetail.freeze,
					forDetail.receive_amount, "充值");

			detail.addDealDetail(error);
		}

		// else {
		/* 资金托管模式下，普通网关充值 */
		// balance2 += amount;
		// DealDetail detail = new DealDetail(user.id,
		// DealType.IPS_NORMAL_RECHARGE, amount, sequence.id,
		// balance2, forDetail.freeze, forDetail.receive_amount, "资金托管充值");

		// // detail.addDealDetail2(error);
		// }

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		if (rechargeType == RechargeType.Normal) {
			DealDetail.updateUserBalance(user.id, balance, forDetail.freeze,
					error);
		} else {
			error.code = DealDetail.addUserFund2(user.id, amount);
		}

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		data.id = user.id;
		data.updateSign(error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		// 添加用户的推荐用户CPS推荐奖励
		rewardCPS(user.id, 0, user.id, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		User.updateActive(user.id, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		DealDetail.userEvent(user.id, UserEvent.RECHARGE, "充值", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		// 发送站内信 [userName]充值了￥[money]元
		TemplateStation station = new TemplateStation();
		station.id = Templets.S_HAND_RECHARGE;

		if (station.status) {
			String mContent = station.content.replace("userName", user.name);
			mContent = mContent.replace("money", amount + "");
			// TemplateStation.addSmsTask(user.email, mContent);
			TemplateStation.addMessageTask(user._id, station.title, mContent);
		}

		// 发送邮件[userName]充值了￥[money]元
		TemplateEmail email = new TemplateEmail();
		email.id = Templets.E_HAND_RECHARGE;

		if (email.status) {
			String eContent = email.content.replace("userName", user.name);
			eContent = eContent.replace("money", amount + "");
			eContent = eContent.replace("date", formatter.format(date) + "");

			TemplateEmail.addEmailTask(user.email, email.title, eContent);

		}

		// 发送短信 [userName]充值了￥[money]元
		TemplateSms sms = new TemplateSms();
		if (StringUtils.isNotBlank(user.mobile)) {
			sms.id = Templets.S_HAND_RECHARGE;

			if (sms.status) {
				String eContent = sms.content.replace("userName", user.name);
				eContent = eContent.replace("money", amount + "");
				eContent = eContent
						.replace("date", formatter.format(date) + "");

				// TemplateSms.addSmsTask(user.mobile, eContent);
				SMSUtil.sendSMS(user.mobile, eContent, error);
			}
		}

		user.balance = balance;
		user.balance2 = balance2;
		User.setCurrUser(user);

		error.code = 0;
	}

	/**
	 * 宝付充值确认
	 * 
	 * @param payNumber
	 * @param amount
	 * @param error
	 */
	public static void bfRecharge(String payNumber, double amounts,
			ErrorInfo error) {
		error.clear();

		double amount = amounts / 100;
		long count = 0;
		long count2 = 0;
		long userId = -1;
		t_user_recharge_details user_recharge = null;
		try {
			count = GenericModel.count(
					"pay_number = ? and amount = ?", payNumber, amount);
			count2 = GenericModel.count(
					"pay_number = ? and is_completed = ?", payNumber, true);
			user_recharge = GenericModel.find(
					"pay_number = ? and is_completed = ?", payNumber, false)
					.first();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("判断是否已经充值时：" + e.getMessage());

			error.code = -1;
			error.msg = "充值回调失败";

			return;
		}

		if (count > 0 || count2 > 0) {
			error.code = -1;
			error.msg = "充值已完成";

			return;
		}

		userId = Convert.strToLong(user_recharge.user_id + "", -1);
		if (userId < 0) {
			error.code = -1;
			error.msg = "不存在的用户";

			return;
		}

		User user = new User();
		user.id = userId;

		DataSafety data = new DataSafety();

		data.id = user.id;

		if (!data.signCheck(error)) {
			JPA.setRollbackOnly();

			return;
		}

		t_system_recharge_completed_sequences sequence = new t_system_recharge_completed_sequences();

		sequence.pay_number = payNumber;
		sequence.time = new Date();
		sequence.amount = amount;

		String sql = "update t_user_recharge_details set is_completed = ?, completed_time = ? where pay_number = ? and is_completed = 0";

		int rows = 0;

		try {
			sequence.save();
			rows = JPA.em().createQuery(sql).setParameter(1, true)
					.setParameter(2, new Date()).setParameter(3, payNumber)
					.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("在充值确认插入更新数据时：" + e.getMessage());

			error.code = -1;
			error.msg = "充值回调失败";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		v_user_for_details forDetail = DealDetail.queryUserBalance(user.id,
				error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		double balance = forDetail.user_amount;
		double balance2 = forDetail.user_amount2;
		int rechargeType = Convert.strToInt(payNumber.split("type")[0],
				RechargeType.Normal);

		// t_user_recharge_details welfare1 =
		// t_user_recharge_details.find("is_completed=? and type=? and pay_number = ?",true,RechargeType.welfare,payNumber).first();

		t_user_recharge_details user_Type = GenericModel.find(
				"pay_number = ? and is_completed = ?", payNumber, true).first();
		if (user_Type.type == RechargeType.welfare) {
			balance += amount;
			DealDetail detail = new DealDetail(user.id, DealType.RECHARGE_USER,
					amount, sequence.id, balance, forDetail.freeze,
					forDetail.receive_amount, "公益充值");

			detail.addDealDetail(error);

		}

		if (user_Type.type == RechargeType.Normal) {
			balance += amount;
			DealDetail detail = new DealDetail(user.id, DealType.RECHARGE_USER,
					amount, sequence.id, balance, forDetail.freeze,
					forDetail.receive_amount, "充值");

			detail.addDealDetail(error);
		}

		// else {
		/* 资金托管模式下，普通网关充值 */
		// balance2 += amount;
		// DealDetail detail = new DealDetail(user.id,
		// DealType.IPS_NORMAL_RECHARGE, amount, sequence.id,
		// balance2, forDetail.freeze, forDetail.receive_amount, "资金托管充值");

		// // detail.addDealDetail2(error);
		// }

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		if (rechargeType == RechargeType.Normal) {
			DealDetail.updateUserBalance(user.id, balance, forDetail.freeze,
					error);
		} else {
			error.code = DealDetail.addUserFund2(user.id, amount);
		}

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		data.id = user.id;
		data.updateSign(error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		// 添加用户的推荐用户CPS推荐奖励
		rewardCPS(user.id, 0, user.id, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		User.updateActive(user.id, error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		DealDetail.userEvent(user.id, UserEvent.RECHARGE, "充值", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();

			return;
		}

		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		// 发送站内信 [userName]充值了￥[money]元
		TemplateStation station = new TemplateStation();
		station.id = Templets.S_HAND_RECHARGE;

		if (station.status) {
			String mContent = station.content.replace("userName", user.name);
			mContent = mContent.replace("money", amount + "");
			// TemplateStation.addSmsTask(user.email, mContent);
			TemplateStation.addMessageTask(user._id, station.title, mContent);
		}

		// 发送邮件[userName]充值了￥[money]元
		TemplateEmail email = new TemplateEmail();
		email.id = Templets.E_HAND_RECHARGE;

		if (email.status) {
			String eContent = email.content.replace("userName", user.name);
			eContent = eContent.replace("money", amount + "");
			eContent = eContent.replace("date", formatter.format(date) + "");

			TemplateEmail.addEmailTask(user.email, email.title, eContent);

		}

		// 发送短信 [userName]充值了￥[money]元
		TemplateSms sms = new TemplateSms();
		if (StringUtils.isNotBlank(user.mobile)) {
			sms.id = Templets.S_HAND_RECHARGE;

			if (sms.status) {
				String eContent = sms.content.replace("userName", user.name);
				eContent = eContent.replace("money", amount + "");
				eContent = eContent
						.replace("date", formatter.format(date) + "");

				// TemplateSms.addSmsTask(user.mobile, eContent);
				SMSUtil.sendSMS(user.mobile, eContent, error);
			}
		}

		user.balance = balance;
		user.balance2 = balance2;
		User.setCurrUser(user);

		error.code = 0;
	}

	/**
	 * 环迅返回sign校验
	 * 
	 * @param content
	 * @param signature
	 * @param error
	 */
	public static void validSign(String content, String signature,
			ErrorInfo error) {
		error.clear();

		t_dict_payment_gateways gateway = gateway(Constants.IPS_GATEWAY, error);

		if (error.code < 0) {
			return;
		}

		cryptix.jce.provider.MD5 b = new cryptix.jce.provider.MD5();
		String SignMD5 = b.toMD5(content + gateway._key).toLowerCase();

		if (!SignMD5.equals(signature)) {
			error.code = -1;
			error.msg = "验证失败";
		}

		error.code = 0;
	}

	/**
	 * 国付宝
	 * 
	 * @param money
	 * @param bankType
	 * @return
	 */
	public static Map<String, String> gpay(BigDecimal money, int bankType,
			int type, ErrorInfo error) {
		error.clear();

		if (bankType < 0 || bankType > 7) {
			error.code = -1;
			error.msg = "传入参数有误";

			return null;
		}

		t_dict_payment_gateways gateway = gateway(Constants.GO_GATEWAY, error);

		if (error.code < 0) {
			return null;
		}

		DecimalFormat currentNumberFormat = new DecimalFormat("#0.00");
		String version = "2.1";
		// String charset = "utf-8";
		// String language = "";
		// String signType = "";
		// 交易代码
		String tranCode = Constants.TRANCODE;

		String merchantID = gateway.pid;
		String merOrderNum = NumberUtil.getBillNo(type); // 订单号 ---- 支付流水号
		String tranAmt = currentNumberFormat.format(money);
		// String feeAmt = "0";
		String frontMerUrl = Constants.GO_MER_URL;
		String backgroundMerUrl = Constants.GO_MER_URL;
		String tranIP = GopayUtils.getIpAddr(Request.current());
		// String gopayServerTime = GopayUtils.getGopayServerTime();
		String tranDateTime = DateUtil.simple2(new Date());

		Map<String, String> args = new HashMap<String, String>();
		args.put("version", version);
		args.put("charset", "2");
		args.put("language", "1");
		args.put("signType", "1");
		args.put("tranCode", tranCode);
		args.put("merchantID", merchantID);
		args.put("merOrderNum", merOrderNum);
		args.put("tranAmt", tranAmt);
		args.put("feeAmt", "");
		args.put("tranDateTime", tranDateTime);
		args.put("frontMerUrl", frontMerUrl);
		args.put("backgroundMerUrl", backgroundMerUrl);
		args.put("currencyType", Constants.CURRENCYTYPE);
		args.put("virCardNoIn", gateway.account);
		String bank = Constants.GO_TYPE[bankType];
		if (!"DEFAULT".equals(bank)) {
			args.put("bankCode", bank);
			args.put("userType", "1");
		} else {
			args.put("bankCode", bank);
		}

		args.put("orderId", "");
		args.put("gopayOutOrderId", "");
		args.put("tranIP", tranIP);
		args.put("respCode", "");
		args.put("VerficationCode", gateway._key);
		args.put("gopayServerTime", "");
		args.put("merRemark1", "");
		Logger.info("args:%s", args);
		// 组织加密明文
		StringBuffer plain = new StringBuffer();
		plain.append("version=[");
		plain.append(version);
		plain.append("]tranCode=[");
		plain.append(tranCode);
		plain.append("]merchantID=[");
		plain.append(merchantID);
		plain.append("]merOrderNum=[");
		plain.append(merOrderNum);
		plain.append("]tranAmt=[");
		plain.append(tranAmt);
		plain.append("]feeAmt=[]");
		plain.append("tranDateTime=[");
		plain.append(tranDateTime);
		plain.append("]frontMerUrl=[");
		plain.append(frontMerUrl);
		plain.append("]backgroundMerUrl=[");
		plain.append(backgroundMerUrl);
		plain.append("]orderId=[]gopayOutOrderId=[]tranIP=[");
		plain.append(tranIP);
		plain.append("]respCode=[]gopayServerTime=[]");
		plain.append("VerficationCode=[");
		plain.append(gateway._key);
		plain.append("]");
		String signValue = GopayUtils.md5(plain.toString());
		String url = Constants.GO_URL;
		args.put("signValue", signValue);
		args.put("url", url);

		sequence(1, merOrderNum, money.doubleValue(),
				Constants.GATEWAY_RECHARGE, error);

		if (error.code < 0) {
			return null;
		}

		return args;
	}

	/**
	 * 
	 * 网银在线
	 * 
	 * ***/

	public static Map<String, String> cbpay(BigDecimal money, int bankType,
			int type, ErrorInfo error) {
		if (bankType < 0 || bankType > 7) {
			error.code = -1;
			error.msg = "传入参数有误";

			return null;
		}

		// t_dict_payment_gateways gateway = gateway(Constants.CBP_GATEWAY,
		// error);

		if (error.code < 0) {
			return null;
		}
		// 格式化人民币
		DecimalFormat currentNumberFormat = new DecimalFormat("#0.00");
		// 定义数组
		Map<String, String> args = new HashMap<String, String>();
		// 钱
		String v_amount = currentNumberFormat.format(money);
		String v_moneytype = "CNY";
		String v_mid = "22952714";
		// 生成订单号
		Date currTime = new Date();
		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd-" + v_mid
				+ "-hhmmss", Locale.US);
		String v_oid = sf.format(currTime);

		String url = Constants.CBP_URL;

		String CbMerUrl = Constants.CBP_MER_URL;

		String v_url = CbMerUrl;

		String key = "duxingyao@751115*8006*9";

		String valifymd5 = v_amount + v_moneytype + v_oid + v_mid + v_url + key;

		MD5 md5 = new MD5();

		String v_md5info = md5.getMD5ofStr(valifymd5);

		args.put("v_mid", v_mid);
		args.put("v_oid", v_oid);
		args.put("v_amount", currentNumberFormat.format(money));
		args.put("v_md5info", v_md5info);
		args.put("v_moneytype", v_moneytype);
		args.put("v_url", v_url);
		args.put("remark1", "东邦易贷");
		args.put("remark2", "东邦投资");
		args.put("url", url);

		sequence(3, v_oid, money.doubleValue(), type, error);

		if (error.code < 0) {
			return null;
		}

		return args;
	}

	/**
	 * 
	 * 汇潮支付
	 * **/

	public static Map<String, String> huipay(BigDecimal money, int bankType,
			int type, ErrorInfo error) {

		if (bankType < 0 || bankType > 7) {
			error.code = -1;
			error.msg = "传入参数有误";

			return null;
		}

		// t_dict_payment_gateways gateway = gateway(Constants.BF_GATEWAY,
		// error);

		if (error.code < 0) {
			return null;
		}

		java.util.Date currentTime = new java.util.Date();// 得到当前系统时间
		java.text.SimpleDateFormat formatter2 = new java.text.SimpleDateFormat(
				"yyyyMMddHHmmss");
		DecimalFormat currentNumberFormat = new DecimalFormat("#0.00");

		String MD5key = "HidGbB]o"; // MD5key值

		String MerNo = "17889"; // 商户ID

		String BillNo = NumberUtil.getBillNo(5); // 订单编号

		String orderTime = formatter2.format(currentTime); // [必填]交易时间：YYYYMMDDHHMMSS

		String Amount = currentNumberFormat.format(money); // 支付金额

		String ReturnURL = Constants.HC_BACKAJ;  // 返回地址

		String AdviceURL = Constants.HC_BACK;

		// [必填]返回数据给商户的地址(商户自己填写):::注意请在测试前将该地址告诉我方人员;否则测试通不过
		// ReturnURL = "http://192.168.1.108/ecpss/payresult.jsp";

		String md5src; // 加密字符串

		md5src = MerNo + "&" + BillNo + "&" + Amount + "&" + ReturnURL + "&"
				+ MD5key;

		controllers.front.account.MD5 md5 = new controllers.front.account.MD5();

		String SignInfo = md5.getMD5ofStr(md5src); // MD5加密后的字符串

		String url = Constants.HC_URL;

		Map<String, String> args = new HashMap<String, String>();
		args.put("MerNo", MerNo);
		args.put("BillNo", BillNo);
		args.put("orderTime", orderTime);
		args.put("Amount", Amount);
		args.put("ReturnURL", ReturnURL);
		args.put("AdviceURL", AdviceURL);
		args.put("SignInfo", SignInfo);
		args.put("url", url);

		sequence(5, BillNo, money.doubleValue(), type, error);

		if (error.code < 0) {
			return null;
		}

		return args;

	}

	/**
	 * 宝付
	 * **/
	public static Map<String, String> baopay(BigDecimal money, int bankType,
			int type, ErrorInfo error) {

		BigDecimal b = new BigDecimal("100");
		BigDecimal moneys = money.multiply(b);
		if (bankType < 0 || bankType > 7) {
			error.code = -1;
			error.msg = "传入参数有误";

			return null;
		}

		t_dict_payment_gateways gateway = gateway(Constants.BF_GATEWAY, error);

		if (error.code < 0) {
			return null;
		}

		java.util.Date currentTime = new java.util.Date();// 得到当前系统时间
		java.text.SimpleDateFormat formatter2 = new java.text.SimpleDateFormat(
				"yyyyMMddHHmmss"); // 当前时间格式化

		DecimalFormat currentNumberFormat = new DecimalFormat("#0");
		Map<String, String> args = new HashMap<String, String>();

		String billno = NumberUtil.getBillNo(4);

		// MD5
		String PageUrl_t =Constants.BF_BACK; 
		String ReturnUrl_t = Constants.ReturnUrl;
		String MARK = "|";
		String MemberID = "120321";
		String PayID = "";
		String TerminalID = "15156";
		String TradeDate = formatter2.format(currentTime);
		String TransID = billno;
		String OrderMoney = currentNumberFormat.format(moneys);
		String PageUrl = PageUrl_t;
		String ReturnUrl = ReturnUrl_t;
		String NoticeType = "1";
		String Md5key = "8qwnf558b36uucdr";

		String md5 = MemberID + MARK + PayID + MARK + TradeDate + MARK
				+ TransID + MARK + OrderMoney + MARK + PageUrl + MARK
				+ ReturnUrl + MARK + NoticeType + MARK + Md5key;

		User md = new User();

		String Signature = md.getMD5ofStr(md5);

		args.put("MemberID", MemberID);
		args.put("TerminalID", TerminalID);
		args.put("InterfaceVersion", "4.0");
		args.put("KeyType", "1");
		args.put("PayID", PayID);
		args.put("TradeDate", TradeDate);
		args.put("TransID", TransID);
		args.put("OrderMoney", OrderMoney);
		// 底层通知，注意修改
		args.put("ReturnUrl", ReturnUrl);
		// MD5签名
		args.put("Signature", Signature);
		args.put("PageUrl", PageUrl);

		args.put("NoticeType", NoticeType);

		String url = Constants.BF_URL;
		args.put("url", url);
		sequence(4, billno, money.doubleValue(), type, error);

		if (error.code < 0) {
			return null;
		}
       
		return args;
	}

	/**
	 * 宝付md5
	 * 
	 * */
	public String getMD5ofStr(String str) {
		return this.getMD5ofStr(str, "utf-8");
	}

	public String getMD5ofStr(String str, String encode) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(str.getBytes(encode));
			byte[] digest = md5.digest();

			StringBuffer hexString = new StringBuffer();
			String strTemp;
			for (int i = 0; i < digest.length; i++) {
				// byteVar &
				// 0x000000FF的作用是，如果digest[i]是负数，则会清除前面24个零，正的byte整型不受影响。
				// (...) | 0xFFFFFF00的作用是，如果digest[i]是正数，则置前24位为一，
				// 这样toHexString输出一个小于等于15的byte整型的十六进制时，倒数第二位为零且不会被丢弃，这样可以通过substring方法进行截取最后两位即可。
				strTemp = Integer.toHexString(
						(digest[i] & 0x000000FF) | 0xFFFFFF00).substring(6);
				hexString.append(strTemp);
			}
			return hexString.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}

	}

	/**
	 * 宝付错误信息
	 * */

	public String getErrorInfo(String result, String resultDesc) {
		String retInfo = "";
		int a;
		int b;
		if (!"".equals(result)) {
			a = Integer.parseInt(result);
		} else {
			a = 0;
		}
		if (!"".equals(resultDesc)) {
			b = Integer.parseInt(resultDesc);
		} else {
			b = 0;
		}

		if (a == 1) {
			retInfo = "支付成功";
			return retInfo;
		} else {
			switch (b) {
			case 0:
				retInfo = "充值失败";
				break;
			case 1:
				retInfo = "系统错误";
				break;
			case 2:
				retInfo = "订单超时";
				break;
			case 11:
				retInfo = "系统维护";
				break;
			case 12:
				retInfo = "无效商户";
				break;
			case 13:
				retInfo = "余额不足";
				break;
			case 14:
				retInfo = "超过支付限额";
				break;
			case 15:
				retInfo = "卡号或卡密错误";
				break;
			case 16:
				retInfo = "不合法的IP地址";
				break;
			case 17:
				retInfo = "重复订单金额不符";
				break;
			case 18:
				retInfo = "卡密已被使用";
				break;
			case 19:
				retInfo = "订单金额错误";
				break;
			case 20:
				retInfo = "支付的类型错误";
				break;
			case 21:
				retInfo = "卡类型有误";
				break;
			case 22:
				retInfo = "卡信息不完整";
				break;
			case 23:
				retInfo = "卡号，卡密，金额不正确";
				break;
			case 24:
				retInfo = "不能用此卡继续做交易";
				break;
			case 25:
				retInfo = "订单无效";
				break;
			case 26:
				retInfo = "卡无效";
				break;
			default:
				retInfo = "支付失败";
				break;
			}

			return retInfo;
		}
	}

	/**
	 * 国付宝掉单处理
	 * 
	 * @param uid
	 * @param payNumber
	 * @param user_recharge
	 * @param error
	 */
	public static void goffSingle(String payNumber,
			t_user_recharge_details user_recharge, ErrorInfo error) {
		String uid = user_recharge.user_id + "";
		String url = Constants.GO_URL;
		t_dict_payment_gateways gateway = User.gateway(Constants.GO_GATEWAY,
				error);
		// 交易代码
		String tranCode = Constants.TRAN_QUERY_CODE;

		String merchantID = gateway.pid;
		String merOrderNum = NumberUtil.getBillNo(uid); // 订单号 ---- 支付流水号
		String orgOrderNum = payNumber; // 原订单号
		String tranIP = GopayUtils.getIpAddr(Request.current());
		String tranDateTime = DateUtil.simple2(new Date());
		String currencyType = Constants.CURRENCYTYPE;
		String tranAmt = user_recharge.amount + "";
		String virCardNoIn = gateway.account;
		String VerficationCode = gateway._key;
		String orgtranDateTime = user_recharge.time + "";
		orgtranDateTime = orgtranDateTime.replace("-", "").replace(" ", "")
				.replace(":", "");
		orgtranDateTime = orgtranDateTime.substring(0, 14);

		StringBuffer plain = new StringBuffer();
		plain.append("tranCode=[");
		plain.append(tranCode);
		plain.append("]merchantID=[");
		plain.append(merchantID);
		plain.append("]merOrderNum=[");
		plain.append(merOrderNum);
		plain.append("]tranAmt=[");
		plain.append(tranAmt);
		plain.append("]ticketAmt=[]");
		plain.append("tranDateTime=[");
		plain.append(tranDateTime);
		plain.append("]currencyType=[");
		plain.append(currencyType);
		plain.append("]merURL=[");
		plain.append("]customerEMail=[");
		plain.append("]authID=[]orgOrderNum=[");
		plain.append(orgOrderNum);
		plain.append("]orgtranDateTime=[");
		plain.append(orgtranDateTime);
		plain.append("]orgtranAmt=[]orgTxnType=[");
		plain.append("]orgTxnStat=[]msgExt=[");
		plain.append("]virCardNo=[]virCardNoIn=[");
		plain.append(virCardNoIn);
		plain.append("]tranIP=[");
		plain.append(tranIP);
		plain.append("]isLocked=[");
		plain.append("]feeAmt=[0]respCode=[");
		plain.append("]VerficationCode=[");
		plain.append(VerficationCode);
		plain.append("]");
		String signValue = GopayUtils.md5(plain.toString());
		GopayUtils.validateQuerySign(tranCode, merchantID, merOrderNum,
				tranAmt, "", currencyType, "", tranDateTime, "", "",
				virCardNoIn, tranIP, "", "", orgtranDateTime, orgOrderNum, "",
				"", "", "", "", VerficationCode, signValue);

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("tranCode", tranCode);
		args.put("merchantID", merchantID);
		args.put("merOrderNum", merOrderNum);
		args.put("tranAmt", tranAmt);
		args.put("ticketAmt", "");
		args.put("tranDateTime", tranDateTime);
		args.put("currencyType", currencyType);
		args.put("merURL", "");
		args.put("customerEMail", "");
		args.put("authID", "");
		args.put("orgOrderNum", orgOrderNum);
		args.put("orgtranDateTime", orgtranDateTime);
		args.put("orgtranAmt", "");
		args.put("orgTxnType", "");
		args.put("orgTxnStat", "");
		args.put("msgExt", "");
		args.put("virCardNo", "");
		args.put("virCardNoIn", virCardNoIn);
		args.put("tranIP", tranIP);
		args.put("isLocked", "");
		args.put("feeAmt", "0");
		args.put("respCode", "");
		args.put("VerficationCode", VerficationCode);
		args.put("signValue", signValue);

		// 调用国付宝订单查询接口
		Node node = WS.url(url).params(args).post().getXml().getFirstChild();
		NodeList nodeList = node.getChildNodes();

		String respCode = Convert.strToStr(nodeList.item(0).getTextContent(),
				"");
		tranCode = Convert.strToStr(nodeList.item(1).getTextContent(), "");
		merchantID = Convert.strToStr(nodeList.item(2).getTextContent(), "");
		merOrderNum = Convert.strToStr(nodeList.item(3).getTextContent(), "");
		tranAmt = Convert.strToStr(nodeList.item(4).getTextContent(), "");
		String feeAmt = Convert.strToStr(nodeList.item(5).getTextContent(), "");
		currencyType = Convert.strToStr(nodeList.item(6).getTextContent(), "");
		String merURL = Convert.strToStr(nodeList.item(7).getTextContent(), "");
		tranDateTime = Convert.strToStr(nodeList.item(8).getTextContent(), "");
		String customerEMail = Convert.strToStr(nodeList.item(9)
				.getTextContent(), "");
		String virCardNo = Convert.strToStr(nodeList.item(10).getTextContent(),
				"");
		virCardNoIn = Convert.strToStr(nodeList.item(11).getTextContent(), "");
		tranIP = Convert.strToStr(nodeList.item(12).getTextContent(), "");
		String msgExt = Convert
				.strToStr(nodeList.item(13).getTextContent(), "");
		orgtranDateTime = Convert.strToStr(nodeList.item(14).getTextContent(),
				"");
		String orgTxnStat = Convert.strToStr(
				nodeList.item(15).getTextContent(), "");
		String orgTxnType = Convert.strToStr(
				nodeList.item(16).getTextContent(), "");
		String orgtranAmt = Convert.strToStr(
				nodeList.item(17).getTextContent(), "");
		orgOrderNum = Convert.strToStr(nodeList.item(18).getTextContent(), "");
		String authID = Convert
				.strToStr(nodeList.item(19).getTextContent(), "");
		String isLocked = Convert.strToStr(nodeList.item(20).getTextContent(),
				"");
		String signVal = Convert.strToStr(nodeList.item(21).getTextContent(),
				"");

		if (GopayUtils.validateQuerySign(tranCode, merchantID, merOrderNum,
				tranAmt, feeAmt, currencyType, merURL, tranDateTime,
				customerEMail, virCardNo, virCardNoIn, tranIP, msgExt,
				respCode, orgtranDateTime, orgOrderNum, orgtranAmt, orgTxnType,
				orgTxnStat, authID, isLocked, gateway._key, signVal)) {
			error.code = -1;
			error.msg = "验证失败，支付失败！";
		}

		if (!"0000".equals(respCode)) {
			error.code = -2;

			if ("ST01".equals(respCode)) {
				error.msg = "补单失败！(原订单不存在)";
			} else if ("ST10".equals(respCode)) {
				error.msg = "补单失败！(交易重复，订单号已存在)";
			} else {
				error.msg = "补单失败！(" + respCode + ")";
			}
			return;
		}

		if (!"0000".equals(orgTxnStat)) {
			error.code = -3;
			error.msg = "交易未成功订单，不可补单！(" + orgTxnStat + ")";
			return;
		}

		if (!payNumber.equals(orgOrderNum)) {
			error.code = -4;
			error.msg = "订单号与交易订单号不匹配！";
			return;
		}

		User.recharge(payNumber, Double.parseDouble(tranAmt), error);

		if (error.code < 0) {
			error.code = -4;
			return;
		}

		error.code = 1;
		error.msg = "补单成功";
	}

	/**
	 * 查询是否已绑定QQ
	 * 
	 * @param qqKey
	 * @param error
	 * @return
	 */
	public static boolean isBindedQQ(String qqKey, ErrorInfo error) {
		error.clear();

		long count = -1;

		try {
			// 调用构造方法t_users(String name, String email),该处实际上将密码存储在email中
			t_users usr = GenericModel
					.find(" select new t_users(name,password) from t_users where (qq_key = ?)",
							qqKey).first();
			if (null != usr) {
				User user = new User();
				user.name = usr.name;
				count = user.login(usr.email, true, error);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("QQ绑定信息 ：" + e.getMessage());
			error.code = -1;
			error.msg = "QQ绑定新查询失败";

			return false;
		}
		if (error.code < 0) {
			return false;
		}
		if (count < 0) {
			error.code = 1;
			error.msg = "QQ未进行绑定";

			return false;
		}

		error.code = 1;

		return true;
	}

	/**
	 * 查询是否绑定微博
	 * 
	 * @param weiboKey
	 * @param error
	 * @return
	 */
	public static boolean isBindedWEIBO(String weiboKey, ErrorInfo error) {
		error.clear();

		long count = -1;

		try {
			// 调用构造方法t_users(String name, String email),该处实际上将密码存储在email中
			t_users usr = GenericModel
					.find(" select new t_users(name,password) from t_users where (weibo_key = ?)",
							weiboKey).first();
			if (null != usr) {
				User user = new User();
				user.name = usr.name;
				count = user.login(usr.email, true, error);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("微博绑定信息 ：" + e.getMessage());
			error.code = -1;
			error.msg = "微博绑定新查询失败";

			return false;
		}
		if (error.code < 0) {
			return false;
		}
		if (count < 0) {
			error.code = 1;
			error.msg = "微博未进行绑定";

			return false;
		}

		error.code = 1;

		return true;
	}

	public int bindingQQ(ErrorInfo error) {
		error.clear();

		EntityManager em = JPA.em();
		Query query = em
				.createQuery("update t_users set qq_key = ? where id = ?")
				.setParameter(1, this.qqKey).setParameter(2, this.id);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("绑定QQ登录信息：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次绑定QQ信息保存失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		error.code = 1;

		return error.code;
	}

	public int bindingWEIBO(ErrorInfo error) {
		error.clear();

		EntityManager em = JPA.em();
		Query query = em
				.createQuery("update t_users set weibo_key = ? where id = ?")
				.setParameter(1, this.weiboKey).setParameter(2, this.id);

		int rows = 0;

		try {
			rows = query.executeUpdate();
		} catch (Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("绑定微博登录信息：" + e.getMessage());
			error.code = -1;
			error.msg = "对不起，由于平台出现故障，此次绑定微博信息保存失败！";

			return error.code;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return error.code;
		}

		error.code = 1;

		return error.code;
	}

	public int isOverdue; // 是否有过逾期

	/**
	 * 用户是否是否有过逾期
	 */
	public int getIsOverdue() {
		String sql = "select u.id from t_users u JOIN t_bids b ON u.id = b.id JOIN t_bills bill ON bill.bid_id = b.id and u.id = ? and b.`status` IN(?, ?) and bill.overdue_mark IN(?, ?, ?)";

		Query query = JPA.em().createNativeQuery(sql);
		query.setParameter(1, this.id);
		query.setParameter(2, Constants.BID_REPAYMENT);
		query.setParameter(3, Constants.BID_REPAYMENTS);
		query.setParameter(4, Constants.BILL_NORMAL_OVERDUE);
		query.setParameter(5, Constants.BILL_OVERDUE);
		query.setParameter(6, Constants.BILL_BAD_DEBTS);
		List<?> row = null;

		try {
			row = query.getResultList();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("用户是否是否有过逾期：" + e.getMessage());

			return 0;
		}

		return row == null || row.size() == 0 ? 0 : 1;
	}

	/**
	 * 更新IPS账号信息
	 * 
	 * @param error
	 */
	public void updateIpsAcctNo(long userId, ErrorInfo error) {
		error.clear();

		String sql = "update t_users set ips_acct_no = ? where id= ?";
		int rows = 0;

		try {
			rows = JpaHelper.execute(sql, this.ipsAcctNo, userId)
					.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("添加IPS账号时：" + e.getMessage());

			error.code = -1;
			error.msg = "添加IPS失败";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		DealDetail.userEvent(userId, UserEvent.IPS_ACCT_NO, "开户成功", error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return;
		}

		User user = User.currUser();
		user.ipsAcctNo = this.ipsAcctNo;

		setCurrUser(user);

		error.code = 0;
		error.msg = "开户成功！";
	}

	/**
	 * 更新自动投标授权号信息
	 * 
	 * @param error
	 */
	public void updateIpsBidAuthNo(long userId, ErrorInfo error) {
		error.clear();

		String sql = "update t_users set ips_bid_auth_no = ? where id= ?";
		int rows = 0;

		try {
			rows = JpaHelper.execute(sql, this.ipsBidAuthNo, userId)
					.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("添加自动投标授权号时：" + e.getMessage());

			error.code = -1;
			error.msg = "添加自动投标授权号失败";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		sql = "update t_user_automatic_invest_options set status = true where user_id = ?";
		rows = 0;

		try {
			rows = JpaHelper.execute(sql, userId).executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("开启自动投标时：" + e.getMessage());

			error.code = -1;
			error.msg = "开启自动投标失败";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		DealDetail.userEvent(userId, UserEvent.IPS_BID_AUTH_NO, "添加自动投标授权号",
				error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return;
		}

		User user = User.currUser();
		user.ipsBidAuthNo = this.ipsBidAuthNo;

		setCurrUser(user);

		error.code = 0;
		error.msg = "添加自动投标授权号成功！";
	}

	/**
	 * 更新自动还款授权号信息
	 * 
	 * @param error
	 */
	public void updateIpsRepayAuthNo(long userId, ErrorInfo error) {
		error.clear();

		String sql = "update t_users set ips_repay_auth_no = ? where id= ?";
		int rows = 0;

		try {
			rows = JpaHelper.execute(sql, this.ipsRepayAuthNo, userId)
					.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("添加自动还款授权号时：" + e.getMessage());

			error.code = -1;
			error.msg = "添加自动还款授权号失败";

			return;
		}

		if (rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";

			return;
		}

		DealDetail.userEvent(userId, UserEvent.IPS_REPAY_AUTH_NO, "添加自动还款授权号",
				error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			return;
		}

		User user = User.currUser();
		user.ipsRepayAuthNo = this.ipsRepayAuthNo;

		setCurrUser(user);

		error.code = 0;
		error.msg = "添加自动还款授权号成功！";
	}

	/**
	 * 根据当前用户ID计算产生的CPS推广费
	 * 
	 * @param userId
	 * @param error
	 * @param managefee
	 *            管理费
	 * @param relationId
	 *            投资ID或者借款ID
	 */
	public static void rewardCPS(long userId, double managefee,
			long relationId, ErrorInfo error) {

		Long recommendUserId = 0l;// 推荐用户
		Integer CPStype = 0;// 奖励方式
		Calendar cal = Calendar.getInstance();// 使用日历类
		int year = cal.get(Calendar.YEAR);// 得到年
		int month = cal.get(Calendar.MONTH) + 1;// 得到月，因为从0开始的，所以要加1

		try {
			recommendUserId = GenericModel.find(
					" select recommend_user_id from t_users where id = ? ",
					userId).first();
			CPStype = GenericModel.find(
					" select recommend_reward_type from t_users where id = ? ",
					userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "查询当前用户的推荐者异常";

			return;
		}

		if (recommendUserId == null || recommendUserId == 0) {
			error.code = 3;
			error.msg = "该用户没有被任何会员推荐";

			return;
		}

		// 推广规则：1:-按会员数 2-按交易额
		if (CPStype == null) {
			error.code = -2;
			error.msg = "查询当前用户的推荐奖励方式异常";

			return;
		}

		DealDetail detail = null;

		BackstageSet backstageSet = BackstageSet.getCurrentBackstageSet();

		// 查询当前CPS推广费设置规则
		double rewardForCounts = backstageSet.rewardForCounts;// 按会员数，每个的金额
		double rewardForRate = backstageSet.rewardForRate;// 按交易额的比例

		if (CPStype == Constants.CPSREWARDTYPE_USER_COUNT) {// 1:-按会员数

			if (User.isActiveUser(userId, error) >= 0) {// 已被奖励
				error.msg = "已被奖励";
				error.code = 2;

				return;
			}

			saveOrUpdateCPSIncome(year, month, recommendUserId,
					rewardForCounts, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.code = -3;
				error.msg = "保存用户推广收入表异常";

				return;
			}
			int result = -1;

			if (Constants.IPS_ENABLE) {
				result = DealDetail.addUserFund2(recommendUserId, rewardForCounts);
			} else {
				result = DealDetail.addUserFund(recommendUserId, rewardForCounts);
			}

			if (result < 0) {
				JPA.setRollbackOnly();
				error.code = -3;
				error.msg = "添加用户可用余额异常";

				return;
			}

			Map<String, Double> funds = DealDetail.queryUserFund(
					recommendUserId, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.code = -2;
				error.msg = "查询推荐用户资金异常";

				return;
			}

			// 添加交易记录
			double balance = Constants.IPS_ENABLE ? funds.get("user_amount2")
					: funds.get("user_amount");
			detail = new DealDetail(recommendUserId, DealType.CPS_COUNT,
					rewardForCounts, userId, balance, funds.get("freeze"),
					funds.get("receive_amount"), "推广的会员成功投资，获得平台CPS推广奖励");
		} else { // 2-按交易额
			double award = Arith.mul(rewardForRate / 100, managefee);

			saveOrUpdateCPSIncome(year, month, recommendUserId, award, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.code = -3;
				error.msg = "保存用户推广收入表异常";

				return;
			}

			int result = -1;

			if (Constants.IPS_ENABLE) {
				result = DealDetail.addUserFund2(recommendUserId, award);
			} else {
				result = DealDetail.addUserFund(recommendUserId, award);
			}

			if (result < 0) {
				JPA.setRollbackOnly();
				error.code = -3;
				error.msg = "添加用户可用余额异常";

				return;
			}

			Map<String, Double> funds = DealDetail.queryUserFund(
					recommendUserId, error);

			if (error.code < 0) {
				JPA.setRollbackOnly();
				error.code = -2;
				error.msg = "查询推荐用户资金异常";

				return;
			}
			// 添加交易记录
			double balance = Constants.IPS_ENABLE ? funds.get("user_amount2")
					: funds.get("user_amount");
			detail = new DealDetail(recommendUserId, DealType.CPS_AMOUNT,
					award, relationId, balance, funds.get("freeze"),
					funds.get("receive_amount"), "推广的会员成功投资，获得平台CPS推广奖励");
		}

		if (Constants.IPS_ENABLE) {
			detail.addDealDetail2(error);
		} else {
			detail.addDealDetail(error);
		}

		if (error.code < 0) {
			JPA.setRollbackOnly();
			error.code = -3;
			error.msg = "添加交易记录异常";

			return;
		}

		// 更新数据防篡改字段
		DataSafety dataSafety = new DataSafety();// 数据防篡改
		dataSafety.setId(recommendUserId);
		dataSafety.updateSign(error);

		if (error.code < 0) {
			JPA.setRollbackOnly();
			error.code = -3;
			error.msg = "更新数据防篡改字段异常";

			return;
		}

		error.code = 1;
		error.msg = "奖励CPS推广费成功";

		return;
	}

	public int getIpsStatus() {
		if (StringUtils.isNotBlank(this.ipsAcctNo)) {
			return IpsCheckStatus.IPS;
		}

		if (this.isMobileVerified) {
			return IpsCheckStatus.MOBILE;
		}

		if (StringUtils.isNotBlank(this.realityName)
				&& StringUtils.isNotBlank(this.idNumber)) {
			return IpsCheckStatus.REAL_NAME;
		}

		if (this.isEmailVerified) {
			return IpsCheckStatus.EMAIL;
		}

		return IpsCheckStatus.NONE;
	}

	/**
	 * 保存或更新用户推广收入表
	 * 
	 * @param year
	 * @param month
	 * @param userId
	 * @param cpsReward
	 * @param error
	 */
	public static void saveOrUpdateCPSIncome(int year, int month, long userId,
			double cpsReward, ErrorInfo error) {

		t_user_cps_income cpsIncome = null;
		long spread_user_account = -1;
		long effective_user_account = -1;

		try {
			cpsIncome = GenericModel.find(
					" year = ? and month = ? and user_id = ? ", year, month,
					userId).first();
			spread_user_account = GenericModel
					.find("select count(id) from t_users where recommend_user_id = ? ",
							userId).first();
			effective_user_account = GenericModel
					.find("select count(id) from t_users where recommend_user_id = ? and is_active = 1",
							userId).first();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -2;
			error.msg = "查询当前用户的推荐奖励收入异常";

			return;
		}

		if (null == cpsIncome) {
			cpsIncome = new t_user_cps_income();
			cpsIncome.year = year;
			cpsIncome.month = month;
			cpsIncome.user_id = userId;
			cpsIncome.spread_user_account = 1;
			cpsIncome.effective_user_account = 1;
			cpsIncome.invalid_user_account = 0;
			cpsIncome.cps_reward = cpsReward;
		} else {
			cpsIncome.spread_user_account = spread_user_account;
			cpsIncome.effective_user_account = effective_user_account;
			cpsIncome.invalid_user_account = spread_user_account
					- effective_user_account;
			cpsIncome.cps_reward = cpsIncome.cps_reward + cpsReward;
		}

		try {
			cpsIncome.save();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -3;
			error.msg = "保存当前用户的推荐奖励收入异常";

			return;
		}

		error.code = 0;

	}

}
