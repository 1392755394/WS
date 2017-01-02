package models;

import java.sql.Timestamp;

/**
 * VUsersId entity. @author MyEclipse Persistence Tools
 */

public class VUsersId implements java.io.Serializable {

	// Fields

	private Long id;
	private Timestamp time;
	private String name;
	private String photo;
	private String realityName;
	private String password;
	private Integer passwordContinuousErrors;
	private Boolean isPasswordErrorLocked;
	private Timestamp passwordErrorLockedTime;
	private String payPassword;
	private Integer payPasswordContinuousErrors;
	private Boolean isPayPasswordErrorLocked;
	private Timestamp payPasswordErrorLockedTime;
	private Boolean isSecretSet;
	private Timestamp secretSetTime;
	private Boolean isAllowLogin;
	private Long loginCount;
	private Timestamp lastLoginTime;
	private String lastLoginIp;
	private Timestamp lastLogoutTime;
	private String email;
	private Boolean isEmailVerified;
	private String mobile1;
	private String mobile;
	private Boolean isMobileVerified;
	private String mobile2;
	private String idNumber;
	private String address;
	private String postcode;
	private Short sex;
	private Timestamp birthday;
	private Integer cityId;
	private String familyAddress;
	private String familyTelephone;
	private String company;
	private String companyAddress;
	private String officeTelephone;
	private String faxNumber;
	private Integer educationId;
	private Integer maritalId;
	private Integer houseId;
	private Integer carId;
	private Boolean isAddBaseInfo;
	private Boolean isErased;
	private Long recommendUserId;
	private Short recommendRewardType;
	private Short masterIdentity;
	private Boolean vipStatus;
	private Double balance;
	private Double freeze;
	private Double score;
	private Double creditScore;
	private Integer creditLevelId;
	private Boolean isRefusedReceive;
	private Timestamp refusedTime;
	private String refusedReason;
	private Boolean isBlacklist;
	private Timestamp joinedTime;
	private String joinedReason;
	private Timestamp assignedTime;
	private Long assignedToSupervisorId;
	private String email2;
	private String telephone;
	private Double creditLine;
	private Boolean isActive;
	private String cityName;
	private String provinceName;
	private Integer provinceId;
	private String educationName;
	private String houseName;
	private String maritalName;
	private String carName;

	// Constructors

	/** default constructor */
	public VUsersId() {
	}

	/** minimal constructor */
	public VUsersId(Long id, Double creditLine, Boolean isActive) {
		this.id = id;
		this.creditLine = creditLine;
		this.isActive = isActive;
	}

	/** full constructor */
	public VUsersId(Long id, Timestamp time, String name, String photo,
			String realityName, String password,
			Integer passwordContinuousErrors, Boolean isPasswordErrorLocked,
			Timestamp passwordErrorLockedTime, String payPassword,
			Integer payPasswordContinuousErrors,
			Boolean isPayPasswordErrorLocked,
			Timestamp payPasswordErrorLockedTime, Boolean isSecretSet,
			Timestamp secretSetTime, Boolean isAllowLogin, Long loginCount,
			Timestamp lastLoginTime, String lastLoginIp,
			Timestamp lastLogoutTime, String email, Boolean isEmailVerified,
			String mobile1, String mobile, Boolean isMobileVerified,
			String mobile2, String idNumber, String address, String postcode,
			Short sex, Timestamp birthday, Integer cityId,
			String familyAddress, String familyTelephone, String company,
			String companyAddress, String officeTelephone, String faxNumber,
			Integer educationId, Integer maritalId, Integer houseId,
			Integer carId, Boolean isAddBaseInfo, Boolean isErased,
			Long recommendUserId, Short recommendRewardType,
			Short masterIdentity, Boolean vipStatus, Double balance,
			Double freeze, Double score, Double creditScore,
			Integer creditLevelId, Boolean isRefusedReceive,
			Timestamp refusedTime, String refusedReason, Boolean isBlacklist,
			Timestamp joinedTime, String joinedReason, Timestamp assignedTime,
			Long assignedToSupervisorId, String email2, String telephone,
			Double creditLine, Boolean isActive, String cityName,
			String provinceName, Integer provinceId, String educationName,
			String houseName, String maritalName, String carName) {
		this.id = id;
		this.time = time;
		this.name = name;
		this.photo = photo;
		this.realityName = realityName;
		this.password = password;
		this.passwordContinuousErrors = passwordContinuousErrors;
		this.isPasswordErrorLocked = isPasswordErrorLocked;
		this.passwordErrorLockedTime = passwordErrorLockedTime;
		this.payPassword = payPassword;
		this.payPasswordContinuousErrors = payPasswordContinuousErrors;
		this.isPayPasswordErrorLocked = isPayPasswordErrorLocked;
		this.payPasswordErrorLockedTime = payPasswordErrorLockedTime;
		this.isSecretSet = isSecretSet;
		this.secretSetTime = secretSetTime;
		this.isAllowLogin = isAllowLogin;
		this.loginCount = loginCount;
		this.lastLoginTime = lastLoginTime;
		this.lastLoginIp = lastLoginIp;
		this.lastLogoutTime = lastLogoutTime;
		this.email = email;
		this.isEmailVerified = isEmailVerified;
		this.mobile1 = mobile1;
		this.mobile = mobile;
		this.isMobileVerified = isMobileVerified;
		this.mobile2 = mobile2;
		this.idNumber = idNumber;
		this.address = address;
		this.postcode = postcode;
		this.sex = sex;
		this.birthday = birthday;
		this.cityId = cityId;
		this.familyAddress = familyAddress;
		this.familyTelephone = familyTelephone;
		this.company = company;
		this.companyAddress = companyAddress;
		this.officeTelephone = officeTelephone;
		this.faxNumber = faxNumber;
		this.educationId = educationId;
		this.maritalId = maritalId;
		this.houseId = houseId;
		this.carId = carId;
		this.isAddBaseInfo = isAddBaseInfo;
		this.isErased = isErased;
		this.recommendUserId = recommendUserId;
		this.recommendRewardType = recommendRewardType;
		this.masterIdentity = masterIdentity;
		this.vipStatus = vipStatus;
		this.balance = balance;
		this.freeze = freeze;
		this.score = score;
		this.creditScore = creditScore;
		this.creditLevelId = creditLevelId;
		this.isRefusedReceive = isRefusedReceive;
		this.refusedTime = refusedTime;
		this.refusedReason = refusedReason;
		this.isBlacklist = isBlacklist;
		this.joinedTime = joinedTime;
		this.joinedReason = joinedReason;
		this.assignedTime = assignedTime;
		this.assignedToSupervisorId = assignedToSupervisorId;
		this.email2 = email2;
		this.telephone = telephone;
		this.creditLine = creditLine;
		this.isActive = isActive;
		this.cityName = cityName;
		this.provinceName = provinceName;
		this.provinceId = provinceId;
		this.educationName = educationName;
		this.houseName = houseName;
		this.maritalName = maritalName;
		this.carName = carName;
	}

	// Property accessors

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Timestamp getTime() {
		return this.time;
	}

	public void setTime(Timestamp time) {
		this.time = time;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhoto() {
		return this.photo;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}

	public String getRealityName() {
		return this.realityName;
	}

	public void setRealityName(String realityName) {
		this.realityName = realityName;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Integer getPasswordContinuousErrors() {
		return this.passwordContinuousErrors;
	}

	public void setPasswordContinuousErrors(Integer passwordContinuousErrors) {
		this.passwordContinuousErrors = passwordContinuousErrors;
	}

	public Boolean getIsPasswordErrorLocked() {
		return this.isPasswordErrorLocked;
	}

	public void setIsPasswordErrorLocked(Boolean isPasswordErrorLocked) {
		this.isPasswordErrorLocked = isPasswordErrorLocked;
	}

	public Timestamp getPasswordErrorLockedTime() {
		return this.passwordErrorLockedTime;
	}

	public void setPasswordErrorLockedTime(Timestamp passwordErrorLockedTime) {
		this.passwordErrorLockedTime = passwordErrorLockedTime;
	}

	public String getPayPassword() {
		return this.payPassword;
	}

	public void setPayPassword(String payPassword) {
		this.payPassword = payPassword;
	}

	public Integer getPayPasswordContinuousErrors() {
		return this.payPasswordContinuousErrors;
	}

	public void setPayPasswordContinuousErrors(
			Integer payPasswordContinuousErrors) {
		this.payPasswordContinuousErrors = payPasswordContinuousErrors;
	}

	public Boolean getIsPayPasswordErrorLocked() {
		return this.isPayPasswordErrorLocked;
	}

	public void setIsPayPasswordErrorLocked(Boolean isPayPasswordErrorLocked) {
		this.isPayPasswordErrorLocked = isPayPasswordErrorLocked;
	}

	public Timestamp getPayPasswordErrorLockedTime() {
		return this.payPasswordErrorLockedTime;
	}

	public void setPayPasswordErrorLockedTime(
			Timestamp payPasswordErrorLockedTime) {
		this.payPasswordErrorLockedTime = payPasswordErrorLockedTime;
	}

	public Boolean getIsSecretSet() {
		return this.isSecretSet;
	}

	public void setIsSecretSet(Boolean isSecretSet) {
		this.isSecretSet = isSecretSet;
	}

	public Timestamp getSecretSetTime() {
		return this.secretSetTime;
	}

	public void setSecretSetTime(Timestamp secretSetTime) {
		this.secretSetTime = secretSetTime;
	}

	public Boolean getIsAllowLogin() {
		return this.isAllowLogin;
	}

	public void setIsAllowLogin(Boolean isAllowLogin) {
		this.isAllowLogin = isAllowLogin;
	}

	public Long getLoginCount() {
		return this.loginCount;
	}

	public void setLoginCount(Long loginCount) {
		this.loginCount = loginCount;
	}

	public Timestamp getLastLoginTime() {
		return this.lastLoginTime;
	}

	public void setLastLoginTime(Timestamp lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public String getLastLoginIp() {
		return this.lastLoginIp;
	}

	public void setLastLoginIp(String lastLoginIp) {
		this.lastLoginIp = lastLoginIp;
	}

	public Timestamp getLastLogoutTime() {
		return this.lastLogoutTime;
	}

	public void setLastLogoutTime(Timestamp lastLogoutTime) {
		this.lastLogoutTime = lastLogoutTime;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Boolean getIsEmailVerified() {
		return this.isEmailVerified;
	}

	public void setIsEmailVerified(Boolean isEmailVerified) {
		this.isEmailVerified = isEmailVerified;
	}

	public String getMobile1() {
		return this.mobile1;
	}

	public void setMobile1(String mobile1) {
		this.mobile1 = mobile1;
	}

	public String getMobile() {
		return this.mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public Boolean getIsMobileVerified() {
		return this.isMobileVerified;
	}

	public void setIsMobileVerified(Boolean isMobileVerified) {
		this.isMobileVerified = isMobileVerified;
	}

	public String getMobile2() {
		return this.mobile2;
	}

	public void setMobile2(String mobile2) {
		this.mobile2 = mobile2;
	}

	public String getIdNumber() {
		return this.idNumber;
	}

	public void setIdNumber(String idNumber) {
		this.idNumber = idNumber;
	}

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPostcode() {
		return this.postcode;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	public Short getSex() {
		return this.sex;
	}

	public void setSex(Short sex) {
		this.sex = sex;
	}

	public Timestamp getBirthday() {
		return this.birthday;
	}

	public void setBirthday(Timestamp birthday) {
		this.birthday = birthday;
	}

	public Integer getCityId() {
		return this.cityId;
	}

	public void setCityId(Integer cityId) {
		this.cityId = cityId;
	}

	public String getFamilyAddress() {
		return this.familyAddress;
	}

	public void setFamilyAddress(String familyAddress) {
		this.familyAddress = familyAddress;
	}

	public String getFamilyTelephone() {
		return this.familyTelephone;
	}

	public void setFamilyTelephone(String familyTelephone) {
		this.familyTelephone = familyTelephone;
	}

	public String getCompany() {
		return this.company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getCompanyAddress() {
		return this.companyAddress;
	}

	public void setCompanyAddress(String companyAddress) {
		this.companyAddress = companyAddress;
	}

	public String getOfficeTelephone() {
		return this.officeTelephone;
	}

	public void setOfficeTelephone(String officeTelephone) {
		this.officeTelephone = officeTelephone;
	}

	public String getFaxNumber() {
		return this.faxNumber;
	}

	public void setFaxNumber(String faxNumber) {
		this.faxNumber = faxNumber;
	}

	public Integer getEducationId() {
		return this.educationId;
	}

	public void setEducationId(Integer educationId) {
		this.educationId = educationId;
	}

	public Integer getMaritalId() {
		return this.maritalId;
	}

	public void setMaritalId(Integer maritalId) {
		this.maritalId = maritalId;
	}

	public Integer getHouseId() {
		return this.houseId;
	}

	public void setHouseId(Integer houseId) {
		this.houseId = houseId;
	}

	public Integer getCarId() {
		return this.carId;
	}

	public void setCarId(Integer carId) {
		this.carId = carId;
	}

	public Boolean getIsAddBaseInfo() {
		return this.isAddBaseInfo;
	}

	public void setIsAddBaseInfo(Boolean isAddBaseInfo) {
		this.isAddBaseInfo = isAddBaseInfo;
	}

	public Boolean getIsErased() {
		return this.isErased;
	}

	public void setIsErased(Boolean isErased) {
		this.isErased = isErased;
	}

	public Long getRecommendUserId() {
		return this.recommendUserId;
	}

	public void setRecommendUserId(Long recommendUserId) {
		this.recommendUserId = recommendUserId;
	}

	public Short getRecommendRewardType() {
		return this.recommendRewardType;
	}

	public void setRecommendRewardType(Short recommendRewardType) {
		this.recommendRewardType = recommendRewardType;
	}

	public Short getMasterIdentity() {
		return this.masterIdentity;
	}

	public void setMasterIdentity(Short masterIdentity) {
		this.masterIdentity = masterIdentity;
	}

	public Boolean getVipStatus() {
		return this.vipStatus;
	}

	public void setVipStatus(Boolean vipStatus) {
		this.vipStatus = vipStatus;
	}

	public Double getBalance() {
		return this.balance;
	}

	public void setBalance(Double balance) {
		this.balance = balance;
	}

	public Double getFreeze() {
		return this.freeze;
	}

	public void setFreeze(Double freeze) {
		this.freeze = freeze;
	}

	public Double getScore() {
		return this.score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public Double getCreditScore() {
		return this.creditScore;
	}

	public void setCreditScore(Double creditScore) {
		this.creditScore = creditScore;
	}

	public Integer getCreditLevelId() {
		return this.creditLevelId;
	}

	public void setCreditLevelId(Integer creditLevelId) {
		this.creditLevelId = creditLevelId;
	}

	public Boolean getIsRefusedReceive() {
		return this.isRefusedReceive;
	}

	public void setIsRefusedReceive(Boolean isRefusedReceive) {
		this.isRefusedReceive = isRefusedReceive;
	}

	public Timestamp getRefusedTime() {
		return this.refusedTime;
	}

	public void setRefusedTime(Timestamp refusedTime) {
		this.refusedTime = refusedTime;
	}

	public String getRefusedReason() {
		return this.refusedReason;
	}

	public void setRefusedReason(String refusedReason) {
		this.refusedReason = refusedReason;
	}

	public Boolean getIsBlacklist() {
		return this.isBlacklist;
	}

	public void setIsBlacklist(Boolean isBlacklist) {
		this.isBlacklist = isBlacklist;
	}

	public Timestamp getJoinedTime() {
		return this.joinedTime;
	}

	public void setJoinedTime(Timestamp joinedTime) {
		this.joinedTime = joinedTime;
	}

	public String getJoinedReason() {
		return this.joinedReason;
	}

	public void setJoinedReason(String joinedReason) {
		this.joinedReason = joinedReason;
	}

	public Timestamp getAssignedTime() {
		return this.assignedTime;
	}

	public void setAssignedTime(Timestamp assignedTime) {
		this.assignedTime = assignedTime;
	}

	public Long getAssignedToSupervisorId() {
		return this.assignedToSupervisorId;
	}

	public void setAssignedToSupervisorId(Long assignedToSupervisorId) {
		this.assignedToSupervisorId = assignedToSupervisorId;
	}

	public String getEmail2() {
		return this.email2;
	}

	public void setEmail2(String email2) {
		this.email2 = email2;
	}

	public String getTelephone() {
		return this.telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}

	public Double getCreditLine() {
		return this.creditLine;
	}

	public void setCreditLine(Double creditLine) {
		this.creditLine = creditLine;
	}

	public Boolean getIsActive() {
		return this.isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public String getCityName() {
		return this.cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public String getProvinceName() {
		return this.provinceName;
	}

	public void setProvinceName(String provinceName) {
		this.provinceName = provinceName;
	}

	public Integer getProvinceId() {
		return this.provinceId;
	}

	public void setProvinceId(Integer provinceId) {
		this.provinceId = provinceId;
	}

	public String getEducationName() {
		return this.educationName;
	}

	public void setEducationName(String educationName) {
		this.educationName = educationName;
	}

	public String getHouseName() {
		return this.houseName;
	}

	public void setHouseName(String houseName) {
		this.houseName = houseName;
	}

	public String getMaritalName() {
		return this.maritalName;
	}

	public void setMaritalName(String maritalName) {
		this.maritalName = maritalName;
	}

	public String getCarName() {
		return this.carName;
	}

	public void setCarName(String carName) {
		this.carName = carName;
	}

	@Override
	public boolean equals(Object other) {
		if ((this == other))
			return true;
		if ((other == null))
			return false;
		if (!(other instanceof VUsersId))
			return false;
		VUsersId castOther = (VUsersId) other;

		return ((this.getId() == castOther.getId()) || (this.getId() != null
				&& castOther.getId() != null && this.getId().equals(
				castOther.getId())))
				&& ((this.getTime() == castOther.getTime()) || (this.getTime() != null
						&& castOther.getTime() != null && this.getTime()
						.equals(castOther.getTime())))
				&& ((this.getName() == castOther.getName()) || (this.getName() != null
						&& castOther.getName() != null && this.getName()
						.equals(castOther.getName())))
				&& ((this.getPhoto() == castOther.getPhoto()) || (this
						.getPhoto() != null
						&& castOther.getPhoto() != null && this.getPhoto()
						.equals(castOther.getPhoto())))
				&& ((this.getRealityName() == castOther.getRealityName()) || (this
						.getRealityName() != null
						&& castOther.getRealityName() != null && this
						.getRealityName().equals(castOther.getRealityName())))
				&& ((this.getPassword() == castOther.getPassword()) || (this
						.getPassword() != null
						&& castOther.getPassword() != null && this
						.getPassword().equals(castOther.getPassword())))
				&& ((this.getPasswordContinuousErrors() == castOther
						.getPasswordContinuousErrors()) || (this
						.getPasswordContinuousErrors() != null
						&& castOther.getPasswordContinuousErrors() != null && this
						.getPasswordContinuousErrors().equals(
								castOther.getPasswordContinuousErrors())))
				&& ((this.getIsPasswordErrorLocked() == castOther
						.getIsPasswordErrorLocked()) || (this
						.getIsPasswordErrorLocked() != null
						&& castOther.getIsPasswordErrorLocked() != null && this
						.getIsPasswordErrorLocked().equals(
								castOther.getIsPasswordErrorLocked())))
				&& ((this.getPasswordErrorLockedTime() == castOther
						.getPasswordErrorLockedTime()) || (this
						.getPasswordErrorLockedTime() != null
						&& castOther.getPasswordErrorLockedTime() != null && this
						.getPasswordErrorLockedTime().equals(
								castOther.getPasswordErrorLockedTime())))
				&& ((this.getPayPassword() == castOther.getPayPassword()) || (this
						.getPayPassword() != null
						&& castOther.getPayPassword() != null && this
						.getPayPassword().equals(castOther.getPayPassword())))
				&& ((this.getPayPasswordContinuousErrors() == castOther
						.getPayPasswordContinuousErrors()) || (this
						.getPayPasswordContinuousErrors() != null
						&& castOther.getPayPasswordContinuousErrors() != null && this
						.getPayPasswordContinuousErrors().equals(
								castOther.getPayPasswordContinuousErrors())))
				&& ((this.getIsPayPasswordErrorLocked() == castOther
						.getIsPayPasswordErrorLocked()) || (this
						.getIsPayPasswordErrorLocked() != null
						&& castOther.getIsPayPasswordErrorLocked() != null && this
						.getIsPayPasswordErrorLocked().equals(
								castOther.getIsPayPasswordErrorLocked())))
				&& ((this.getPayPasswordErrorLockedTime() == castOther
						.getPayPasswordErrorLockedTime()) || (this
						.getPayPasswordErrorLockedTime() != null
						&& castOther.getPayPasswordErrorLockedTime() != null && this
						.getPayPasswordErrorLockedTime().equals(
								castOther.getPayPasswordErrorLockedTime())))
				&& ((this.getIsSecretSet() == castOther.getIsSecretSet()) || (this
						.getIsSecretSet() != null
						&& castOther.getIsSecretSet() != null && this
						.getIsSecretSet().equals(castOther.getIsSecretSet())))
				&& ((this.getSecretSetTime() == castOther.getSecretSetTime()) || (this
						.getSecretSetTime() != null
						&& castOther.getSecretSetTime() != null && this
						.getSecretSetTime()
						.equals(castOther.getSecretSetTime())))
				&& ((this.getIsAllowLogin() == castOther.getIsAllowLogin()) || (this
						.getIsAllowLogin() != null
						&& castOther.getIsAllowLogin() != null && this
						.getIsAllowLogin().equals(castOther.getIsAllowLogin())))
				&& ((this.getLoginCount() == castOther.getLoginCount()) || (this
						.getLoginCount() != null
						&& castOther.getLoginCount() != null && this
						.getLoginCount().equals(castOther.getLoginCount())))
				&& ((this.getLastLoginTime() == castOther.getLastLoginTime()) || (this
						.getLastLoginTime() != null
						&& castOther.getLastLoginTime() != null && this
						.getLastLoginTime()
						.equals(castOther.getLastLoginTime())))
				&& ((this.getLastLoginIp() == castOther.getLastLoginIp()) || (this
						.getLastLoginIp() != null
						&& castOther.getLastLoginIp() != null && this
						.getLastLoginIp().equals(castOther.getLastLoginIp())))
				&& ((this.getLastLogoutTime() == castOther.getLastLogoutTime()) || (this
						.getLastLogoutTime() != null
						&& castOther.getLastLogoutTime() != null && this
						.getLastLogoutTime().equals(
								castOther.getLastLogoutTime())))
				&& ((this.getEmail() == castOther.getEmail()) || (this
						.getEmail() != null
						&& castOther.getEmail() != null && this.getEmail()
						.equals(castOther.getEmail())))
				&& ((this.getIsEmailVerified() == castOther
						.getIsEmailVerified()) || (this.getIsEmailVerified() != null
						&& castOther.getIsEmailVerified() != null && this
						.getIsEmailVerified().equals(
								castOther.getIsEmailVerified())))
				&& ((this.getMobile1() == castOther.getMobile1()) || (this
						.getMobile1() != null
						&& castOther.getMobile1() != null && this.getMobile1()
						.equals(castOther.getMobile1())))
				&& ((this.getMobile() == castOther.getMobile()) || (this
						.getMobile() != null
						&& castOther.getMobile() != null && this.getMobile()
						.equals(castOther.getMobile())))
				&& ((this.getIsMobileVerified() == castOther
						.getIsMobileVerified()) || (this.getIsMobileVerified() != null
						&& castOther.getIsMobileVerified() != null && this
						.getIsMobileVerified().equals(
								castOther.getIsMobileVerified())))
				&& ((this.getMobile2() == castOther.getMobile2()) || (this
						.getMobile2() != null
						&& castOther.getMobile2() != null && this.getMobile2()
						.equals(castOther.getMobile2())))
				&& ((this.getIdNumber() == castOther.getIdNumber()) || (this
						.getIdNumber() != null
						&& castOther.getIdNumber() != null && this
						.getIdNumber().equals(castOther.getIdNumber())))
				&& ((this.getAddress() == castOther.getAddress()) || (this
						.getAddress() != null
						&& castOther.getAddress() != null && this.getAddress()
						.equals(castOther.getAddress())))
				&& ((this.getPostcode() == castOther.getPostcode()) || (this
						.getPostcode() != null
						&& castOther.getPostcode() != null && this
						.getPostcode().equals(castOther.getPostcode())))
				&& ((this.getSex() == castOther.getSex()) || (this.getSex() != null
						&& castOther.getSex() != null && this.getSex().equals(
						castOther.getSex())))
				&& ((this.getBirthday() == castOther.getBirthday()) || (this
						.getBirthday() != null
						&& castOther.getBirthday() != null && this
						.getBirthday().equals(castOther.getBirthday())))
				&& ((this.getCityId() == castOther.getCityId()) || (this
						.getCityId() != null
						&& castOther.getCityId() != null && this.getCityId()
						.equals(castOther.getCityId())))
				&& ((this.getFamilyAddress() == castOther.getFamilyAddress()) || (this
						.getFamilyAddress() != null
						&& castOther.getFamilyAddress() != null && this
						.getFamilyAddress()
						.equals(castOther.getFamilyAddress())))
				&& ((this.getFamilyTelephone() == castOther
						.getFamilyTelephone()) || (this.getFamilyTelephone() != null
						&& castOther.getFamilyTelephone() != null && this
						.getFamilyTelephone().equals(
								castOther.getFamilyTelephone())))
				&& ((this.getCompany() == castOther.getCompany()) || (this
						.getCompany() != null
						&& castOther.getCompany() != null && this.getCompany()
						.equals(castOther.getCompany())))
				&& ((this.getCompanyAddress() == castOther.getCompanyAddress()) || (this
						.getCompanyAddress() != null
						&& castOther.getCompanyAddress() != null && this
						.getCompanyAddress().equals(
								castOther.getCompanyAddress())))
				&& ((this.getOfficeTelephone() == castOther
						.getOfficeTelephone()) || (this.getOfficeTelephone() != null
						&& castOther.getOfficeTelephone() != null && this
						.getOfficeTelephone().equals(
								castOther.getOfficeTelephone())))
				&& ((this.getFaxNumber() == castOther.getFaxNumber()) || (this
						.getFaxNumber() != null
						&& castOther.getFaxNumber() != null && this
						.getFaxNumber().equals(castOther.getFaxNumber())))
				&& ((this.getEducationId() == castOther.getEducationId()) || (this
						.getEducationId() != null
						&& castOther.getEducationId() != null && this
						.getEducationId().equals(castOther.getEducationId())))
				&& ((this.getMaritalId() == castOther.getMaritalId()) || (this
						.getMaritalId() != null
						&& castOther.getMaritalId() != null && this
						.getMaritalId().equals(castOther.getMaritalId())))
				&& ((this.getHouseId() == castOther.getHouseId()) || (this
						.getHouseId() != null
						&& castOther.getHouseId() != null && this.getHouseId()
						.equals(castOther.getHouseId())))
				&& ((this.getCarId() == castOther.getCarId()) || (this
						.getCarId() != null
						&& castOther.getCarId() != null && this.getCarId()
						.equals(castOther.getCarId())))
				&& ((this.getIsAddBaseInfo() == castOther.getIsAddBaseInfo()) || (this
						.getIsAddBaseInfo() != null
						&& castOther.getIsAddBaseInfo() != null && this
						.getIsAddBaseInfo()
						.equals(castOther.getIsAddBaseInfo())))
				&& ((this.getIsErased() == castOther.getIsErased()) || (this
						.getIsErased() != null
						&& castOther.getIsErased() != null && this
						.getIsErased().equals(castOther.getIsErased())))
				&& ((this.getRecommendUserId() == castOther
						.getRecommendUserId()) || (this.getRecommendUserId() != null
						&& castOther.getRecommendUserId() != null && this
						.getRecommendUserId().equals(
								castOther.getRecommendUserId())))
				&& ((this.getRecommendRewardType() == castOther
						.getRecommendRewardType()) || (this
						.getRecommendRewardType() != null
						&& castOther.getRecommendRewardType() != null && this
						.getRecommendRewardType().equals(
								castOther.getRecommendRewardType())))
				&& ((this.getMasterIdentity() == castOther.getMasterIdentity()) || (this
						.getMasterIdentity() != null
						&& castOther.getMasterIdentity() != null && this
						.getMasterIdentity().equals(
								castOther.getMasterIdentity())))
				&& ((this.getVipStatus() == castOther.getVipStatus()) || (this
						.getVipStatus() != null
						&& castOther.getVipStatus() != null && this
						.getVipStatus().equals(castOther.getVipStatus())))
				&& ((this.getBalance() == castOther.getBalance()) || (this
						.getBalance() != null
						&& castOther.getBalance() != null && this.getBalance()
						.equals(castOther.getBalance())))
				&& ((this.getFreeze() == castOther.getFreeze()) || (this
						.getFreeze() != null
						&& castOther.getFreeze() != null && this.getFreeze()
						.equals(castOther.getFreeze())))
				&& ((this.getScore() == castOther.getScore()) || (this
						.getScore() != null
						&& castOther.getScore() != null && this.getScore()
						.equals(castOther.getScore())))
				&& ((this.getCreditScore() == castOther.getCreditScore()) || (this
						.getCreditScore() != null
						&& castOther.getCreditScore() != null && this
						.getCreditScore().equals(castOther.getCreditScore())))
				&& ((this.getCreditLevelId() == castOther.getCreditLevelId()) || (this
						.getCreditLevelId() != null
						&& castOther.getCreditLevelId() != null && this
						.getCreditLevelId()
						.equals(castOther.getCreditLevelId())))
				&& ((this.getIsRefusedReceive() == castOther
						.getIsRefusedReceive()) || (this.getIsRefusedReceive() != null
						&& castOther.getIsRefusedReceive() != null && this
						.getIsRefusedReceive().equals(
								castOther.getIsRefusedReceive())))
				&& ((this.getRefusedTime() == castOther.getRefusedTime()) || (this
						.getRefusedTime() != null
						&& castOther.getRefusedTime() != null && this
						.getRefusedTime().equals(castOther.getRefusedTime())))
				&& ((this.getRefusedReason() == castOther.getRefusedReason()) || (this
						.getRefusedReason() != null
						&& castOther.getRefusedReason() != null && this
						.getRefusedReason()
						.equals(castOther.getRefusedReason())))
				&& ((this.getIsBlacklist() == castOther.getIsBlacklist()) || (this
						.getIsBlacklist() != null
						&& castOther.getIsBlacklist() != null && this
						.getIsBlacklist().equals(castOther.getIsBlacklist())))
				&& ((this.getJoinedTime() == castOther.getJoinedTime()) || (this
						.getJoinedTime() != null
						&& castOther.getJoinedTime() != null && this
						.getJoinedTime().equals(castOther.getJoinedTime())))
				&& ((this.getJoinedReason() == castOther.getJoinedReason()) || (this
						.getJoinedReason() != null
						&& castOther.getJoinedReason() != null && this
						.getJoinedReason().equals(castOther.getJoinedReason())))
				&& ((this.getAssignedTime() == castOther.getAssignedTime()) || (this
						.getAssignedTime() != null
						&& castOther.getAssignedTime() != null && this
						.getAssignedTime().equals(castOther.getAssignedTime())))
				&& ((this.getAssignedToSupervisorId() == castOther
						.getAssignedToSupervisorId()) || (this
						.getAssignedToSupervisorId() != null
						&& castOther.getAssignedToSupervisorId() != null && this
						.getAssignedToSupervisorId().equals(
								castOther.getAssignedToSupervisorId())))
				&& ((this.getEmail2() == castOther.getEmail2()) || (this
						.getEmail2() != null
						&& castOther.getEmail2() != null && this.getEmail2()
						.equals(castOther.getEmail2())))
				&& ((this.getTelephone() == castOther.getTelephone()) || (this
						.getTelephone() != null
						&& castOther.getTelephone() != null && this
						.getTelephone().equals(castOther.getTelephone())))
				&& ((this.getCreditLine() == castOther.getCreditLine()) || (this
						.getCreditLine() != null
						&& castOther.getCreditLine() != null && this
						.getCreditLine().equals(castOther.getCreditLine())))
				&& ((this.getIsActive() == castOther.getIsActive()) || (this
						.getIsActive() != null
						&& castOther.getIsActive() != null && this
						.getIsActive().equals(castOther.getIsActive())))
				&& ((this.getCityName() == castOther.getCityName()) || (this
						.getCityName() != null
						&& castOther.getCityName() != null && this
						.getCityName().equals(castOther.getCityName())))
				&& ((this.getProvinceName() == castOther.getProvinceName()) || (this
						.getProvinceName() != null
						&& castOther.getProvinceName() != null && this
						.getProvinceName().equals(castOther.getProvinceName())))
				&& ((this.getProvinceId() == castOther.getProvinceId()) || (this
						.getProvinceId() != null
						&& castOther.getProvinceId() != null && this
						.getProvinceId().equals(castOther.getProvinceId())))
				&& ((this.getEducationName() == castOther.getEducationName()) || (this
						.getEducationName() != null
						&& castOther.getEducationName() != null && this
						.getEducationName()
						.equals(castOther.getEducationName())))
				&& ((this.getHouseName() == castOther.getHouseName()) || (this
						.getHouseName() != null
						&& castOther.getHouseName() != null && this
						.getHouseName().equals(castOther.getHouseName())))
				&& ((this.getMaritalName() == castOther.getMaritalName()) || (this
						.getMaritalName() != null
						&& castOther.getMaritalName() != null && this
						.getMaritalName().equals(castOther.getMaritalName())))
				&& ((this.getCarName() == castOther.getCarName()) || (this
						.getCarName() != null
						&& castOther.getCarName() != null && this.getCarName()
						.equals(castOther.getCarName())));
	}

	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + (getId() == null ? 0 : this.getId().hashCode());
		result = 37 * result
				+ (getTime() == null ? 0 : this.getTime().hashCode());
		result = 37 * result
				+ (getName() == null ? 0 : this.getName().hashCode());
		result = 37 * result
				+ (getPhoto() == null ? 0 : this.getPhoto().hashCode());
		result = 37
				* result
				+ (getRealityName() == null ? 0 : this.getRealityName()
						.hashCode());
		result = 37 * result
				+ (getPassword() == null ? 0 : this.getPassword().hashCode());
		result = 37
				* result
				+ (getPasswordContinuousErrors() == null ? 0 : this
						.getPasswordContinuousErrors().hashCode());
		result = 37
				* result
				+ (getIsPasswordErrorLocked() == null ? 0 : this
						.getIsPasswordErrorLocked().hashCode());
		result = 37
				* result
				+ (getPasswordErrorLockedTime() == null ? 0 : this
						.getPasswordErrorLockedTime().hashCode());
		result = 37
				* result
				+ (getPayPassword() == null ? 0 : this.getPayPassword()
						.hashCode());
		result = 37
				* result
				+ (getPayPasswordContinuousErrors() == null ? 0 : this
						.getPayPasswordContinuousErrors().hashCode());
		result = 37
				* result
				+ (getIsPayPasswordErrorLocked() == null ? 0 : this
						.getIsPayPasswordErrorLocked().hashCode());
		result = 37
				* result
				+ (getPayPasswordErrorLockedTime() == null ? 0 : this
						.getPayPasswordErrorLockedTime().hashCode());
		result = 37
				* result
				+ (getIsSecretSet() == null ? 0 : this.getIsSecretSet()
						.hashCode());
		result = 37
				* result
				+ (getSecretSetTime() == null ? 0 : this.getSecretSetTime()
						.hashCode());
		result = 37
				* result
				+ (getIsAllowLogin() == null ? 0 : this.getIsAllowLogin()
						.hashCode());
		result = 37
				* result
				+ (getLoginCount() == null ? 0 : this.getLoginCount()
						.hashCode());
		result = 37
				* result
				+ (getLastLoginTime() == null ? 0 : this.getLastLoginTime()
						.hashCode());
		result = 37
				* result
				+ (getLastLoginIp() == null ? 0 : this.getLastLoginIp()
						.hashCode());
		result = 37
				* result
				+ (getLastLogoutTime() == null ? 0 : this.getLastLogoutTime()
						.hashCode());
		result = 37 * result
				+ (getEmail() == null ? 0 : this.getEmail().hashCode());
		result = 37
				* result
				+ (getIsEmailVerified() == null ? 0 : this.getIsEmailVerified()
						.hashCode());
		result = 37 * result
				+ (getMobile1() == null ? 0 : this.getMobile1().hashCode());
		result = 37 * result
				+ (getMobile() == null ? 0 : this.getMobile().hashCode());
		result = 37
				* result
				+ (getIsMobileVerified() == null ? 0 : this
						.getIsMobileVerified().hashCode());
		result = 37 * result
				+ (getMobile2() == null ? 0 : this.getMobile2().hashCode());
		result = 37 * result
				+ (getIdNumber() == null ? 0 : this.getIdNumber().hashCode());
		result = 37 * result
				+ (getAddress() == null ? 0 : this.getAddress().hashCode());
		result = 37 * result
				+ (getPostcode() == null ? 0 : this.getPostcode().hashCode());
		result = 37 * result
				+ (getSex() == null ? 0 : this.getSex().hashCode());
		result = 37 * result
				+ (getBirthday() == null ? 0 : this.getBirthday().hashCode());
		result = 37 * result
				+ (getCityId() == null ? 0 : this.getCityId().hashCode());
		result = 37
				* result
				+ (getFamilyAddress() == null ? 0 : this.getFamilyAddress()
						.hashCode());
		result = 37
				* result
				+ (getFamilyTelephone() == null ? 0 : this.getFamilyTelephone()
						.hashCode());
		result = 37 * result
				+ (getCompany() == null ? 0 : this.getCompany().hashCode());
		result = 37
				* result
				+ (getCompanyAddress() == null ? 0 : this.getCompanyAddress()
						.hashCode());
		result = 37
				* result
				+ (getOfficeTelephone() == null ? 0 : this.getOfficeTelephone()
						.hashCode());
		result = 37 * result
				+ (getFaxNumber() == null ? 0 : this.getFaxNumber().hashCode());
		result = 37
				* result
				+ (getEducationId() == null ? 0 : this.getEducationId()
						.hashCode());
		result = 37 * result
				+ (getMaritalId() == null ? 0 : this.getMaritalId().hashCode());
		result = 37 * result
				+ (getHouseId() == null ? 0 : this.getHouseId().hashCode());
		result = 37 * result
				+ (getCarId() == null ? 0 : this.getCarId().hashCode());
		result = 37
				* result
				+ (getIsAddBaseInfo() == null ? 0 : this.getIsAddBaseInfo()
						.hashCode());
		result = 37 * result
				+ (getIsErased() == null ? 0 : this.getIsErased().hashCode());
		result = 37
				* result
				+ (getRecommendUserId() == null ? 0 : this.getRecommendUserId()
						.hashCode());
		result = 37
				* result
				+ (getRecommendRewardType() == null ? 0 : this
						.getRecommendRewardType().hashCode());
		result = 37
				* result
				+ (getMasterIdentity() == null ? 0 : this.getMasterIdentity()
						.hashCode());
		result = 37 * result
				+ (getVipStatus() == null ? 0 : this.getVipStatus().hashCode());
		result = 37 * result
				+ (getBalance() == null ? 0 : this.getBalance().hashCode());
		result = 37 * result
				+ (getFreeze() == null ? 0 : this.getFreeze().hashCode());
		result = 37 * result
				+ (getScore() == null ? 0 : this.getScore().hashCode());
		result = 37
				* result
				+ (getCreditScore() == null ? 0 : this.getCreditScore()
						.hashCode());
		result = 37
				* result
				+ (getCreditLevelId() == null ? 0 : this.getCreditLevelId()
						.hashCode());
		result = 37
				* result
				+ (getIsRefusedReceive() == null ? 0 : this
						.getIsRefusedReceive().hashCode());
		result = 37
				* result
				+ (getRefusedTime() == null ? 0 : this.getRefusedTime()
						.hashCode());
		result = 37
				* result
				+ (getRefusedReason() == null ? 0 : this.getRefusedReason()
						.hashCode());
		result = 37
				* result
				+ (getIsBlacklist() == null ? 0 : this.getIsBlacklist()
						.hashCode());
		result = 37
				* result
				+ (getJoinedTime() == null ? 0 : this.getJoinedTime()
						.hashCode());
		result = 37
				* result
				+ (getJoinedReason() == null ? 0 : this.getJoinedReason()
						.hashCode());
		result = 37
				* result
				+ (getAssignedTime() == null ? 0 : this.getAssignedTime()
						.hashCode());
		result = 37
				* result
				+ (getAssignedToSupervisorId() == null ? 0 : this
						.getAssignedToSupervisorId().hashCode());
		result = 37 * result
				+ (getEmail2() == null ? 0 : this.getEmail2().hashCode());
		result = 37 * result
				+ (getTelephone() == null ? 0 : this.getTelephone().hashCode());
		result = 37
				* result
				+ (getCreditLine() == null ? 0 : this.getCreditLine()
						.hashCode());
		result = 37 * result
				+ (getIsActive() == null ? 0 : this.getIsActive().hashCode());
		result = 37 * result
				+ (getCityName() == null ? 0 : this.getCityName().hashCode());
		result = 37
				* result
				+ (getProvinceName() == null ? 0 : this.getProvinceName()
						.hashCode());
		result = 37
				* result
				+ (getProvinceId() == null ? 0 : this.getProvinceId()
						.hashCode());
		result = 37
				* result
				+ (getEducationName() == null ? 0 : this.getEducationName()
						.hashCode());
		result = 37 * result
				+ (getHouseName() == null ? 0 : this.getHouseName().hashCode());
		result = 37
				* result
				+ (getMaritalName() == null ? 0 : this.getMaritalName()
						.hashCode());
		result = 37 * result
				+ (getCarName() == null ? 0 : this.getCarName().hashCode());
		return result;
	}

}