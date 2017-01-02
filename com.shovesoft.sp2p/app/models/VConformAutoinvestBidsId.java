package models;

import java.sql.Timestamp;

/**
 * VConformAutoinvestBidsId entity. @author MyEclipse Persistence Tools
 */

public class VConformAutoinvestBidsId implements java.io.Serializable {

	// Fields

	private Long id;
	private Long userId;
	private Double amount;
	private Integer period;
	private Double minInvestAmount;
	private Short status;
	private Double repaymentSchedule;
	private Double hasInvestedAmount;
	private Timestamp auditTime;
	private Integer creditLevelId;
	private Integer periodUnit;
	private Double averageInvestAmount;
	private Double apr;
	private Double minInterestRate;
	private Double maxInterestRate;
	private Short loanType;

	// Constructors

	/** default constructor */
	public VConformAutoinvestBidsId() {
	}

	/** minimal constructor */
	public VConformAutoinvestBidsId(Long id) {
		this.id = id;
	}

	/** full constructor */
	public VConformAutoinvestBidsId(Long id, Long userId, Double amount,
			Integer period, Double minInvestAmount, Short status,
			Double repaymentSchedule, Double hasInvestedAmount,
			Timestamp auditTime, Integer creditLevelId, Integer periodUnit,
			Double averageInvestAmount, Double apr, Double minInterestRate,
			Double maxInterestRate, Short loanType) {
		this.id = id;
		this.userId = userId;
		this.amount = amount;
		this.period = period;
		this.minInvestAmount = minInvestAmount;
		this.status = status;
		this.repaymentSchedule = repaymentSchedule;
		this.hasInvestedAmount = hasInvestedAmount;
		this.auditTime = auditTime;
		this.creditLevelId = creditLevelId;
		this.periodUnit = periodUnit;
		this.averageInvestAmount = averageInvestAmount;
		this.apr = apr;
		this.minInterestRate = minInterestRate;
		this.maxInterestRate = maxInterestRate;
		this.loanType = loanType;
	}

	// Property accessors

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return this.userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Double getAmount() {
		return this.amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public Integer getPeriod() {
		return this.period;
	}

	public void setPeriod(Integer period) {
		this.period = period;
	}

	public Double getMinInvestAmount() {
		return this.minInvestAmount;
	}

	public void setMinInvestAmount(Double minInvestAmount) {
		this.minInvestAmount = minInvestAmount;
	}

	public Short getStatus() {
		return this.status;
	}

	public void setStatus(Short status) {
		this.status = status;
	}

	public Double getRepaymentSchedule() {
		return this.repaymentSchedule;
	}

	public void setRepaymentSchedule(Double repaymentSchedule) {
		this.repaymentSchedule = repaymentSchedule;
	}

	public Double getHasInvestedAmount() {
		return this.hasInvestedAmount;
	}

	public void setHasInvestedAmount(Double hasInvestedAmount) {
		this.hasInvestedAmount = hasInvestedAmount;
	}

	public Timestamp getAuditTime() {
		return this.auditTime;
	}

	public void setAuditTime(Timestamp auditTime) {
		this.auditTime = auditTime;
	}

	public Integer getCreditLevelId() {
		return this.creditLevelId;
	}

	public void setCreditLevelId(Integer creditLevelId) {
		this.creditLevelId = creditLevelId;
	}

	public Integer getPeriodUnit() {
		return this.periodUnit;
	}

	public void setPeriodUnit(Integer periodUnit) {
		this.periodUnit = periodUnit;
	}

	public Double getAverageInvestAmount() {
		return this.averageInvestAmount;
	}

	public void setAverageInvestAmount(Double averageInvestAmount) {
		this.averageInvestAmount = averageInvestAmount;
	}

	public Double getApr() {
		return this.apr;
	}

	public void setApr(Double apr) {
		this.apr = apr;
	}

	public Double getMinInterestRate() {
		return this.minInterestRate;
	}

	public void setMinInterestRate(Double minInterestRate) {
		this.minInterestRate = minInterestRate;
	}

	public Double getMaxInterestRate() {
		return this.maxInterestRate;
	}

	public void setMaxInterestRate(Double maxInterestRate) {
		this.maxInterestRate = maxInterestRate;
	}

	public Short getLoanType() {
		return this.loanType;
	}

	public void setLoanType(Short loanType) {
		this.loanType = loanType;
	}

	@Override
	public boolean equals(Object other) {
		if ((this == other))
			return true;
		if ((other == null))
			return false;
		if (!(other instanceof VConformAutoinvestBidsId))
			return false;
		VConformAutoinvestBidsId castOther = (VConformAutoinvestBidsId) other;

		return ((this.getId() == castOther.getId()) || (this.getId() != null
				&& castOther.getId() != null && this.getId().equals(
				castOther.getId())))
				&& ((this.getUserId() == castOther.getUserId()) || (this
						.getUserId() != null
						&& castOther.getUserId() != null && this.getUserId()
						.equals(castOther.getUserId())))
				&& ((this.getAmount() == castOther.getAmount()) || (this
						.getAmount() != null
						&& castOther.getAmount() != null && this.getAmount()
						.equals(castOther.getAmount())))
				&& ((this.getPeriod() == castOther.getPeriod()) || (this
						.getPeriod() != null
						&& castOther.getPeriod() != null && this.getPeriod()
						.equals(castOther.getPeriod())))
				&& ((this.getMinInvestAmount() == castOther
						.getMinInvestAmount()) || (this.getMinInvestAmount() != null
						&& castOther.getMinInvestAmount() != null && this
						.getMinInvestAmount().equals(
								castOther.getMinInvestAmount())))
				&& ((this.getStatus() == castOther.getStatus()) || (this
						.getStatus() != null
						&& castOther.getStatus() != null && this.getStatus()
						.equals(castOther.getStatus())))
				&& ((this.getRepaymentSchedule() == castOther
						.getRepaymentSchedule()) || (this
						.getRepaymentSchedule() != null
						&& castOther.getRepaymentSchedule() != null && this
						.getRepaymentSchedule().equals(
								castOther.getRepaymentSchedule())))
				&& ((this.getHasInvestedAmount() == castOther
						.getHasInvestedAmount()) || (this
						.getHasInvestedAmount() != null
						&& castOther.getHasInvestedAmount() != null && this
						.getHasInvestedAmount().equals(
								castOther.getHasInvestedAmount())))
				&& ((this.getAuditTime() == castOther.getAuditTime()) || (this
						.getAuditTime() != null
						&& castOther.getAuditTime() != null && this
						.getAuditTime().equals(castOther.getAuditTime())))
				&& ((this.getCreditLevelId() == castOther.getCreditLevelId()) || (this
						.getCreditLevelId() != null
						&& castOther.getCreditLevelId() != null && this
						.getCreditLevelId()
						.equals(castOther.getCreditLevelId())))
				&& ((this.getPeriodUnit() == castOther.getPeriodUnit()) || (this
						.getPeriodUnit() != null
						&& castOther.getPeriodUnit() != null && this
						.getPeriodUnit().equals(castOther.getPeriodUnit())))
				&& ((this.getAverageInvestAmount() == castOther
						.getAverageInvestAmount()) || (this
						.getAverageInvestAmount() != null
						&& castOther.getAverageInvestAmount() != null && this
						.getAverageInvestAmount().equals(
								castOther.getAverageInvestAmount())))
				&& ((this.getApr() == castOther.getApr()) || (this.getApr() != null
						&& castOther.getApr() != null && this.getApr().equals(
						castOther.getApr())))
				&& ((this.getMinInterestRate() == castOther
						.getMinInterestRate()) || (this.getMinInterestRate() != null
						&& castOther.getMinInterestRate() != null && this
						.getMinInterestRate().equals(
								castOther.getMinInterestRate())))
				&& ((this.getMaxInterestRate() == castOther
						.getMaxInterestRate()) || (this.getMaxInterestRate() != null
						&& castOther.getMaxInterestRate() != null && this
						.getMaxInterestRate().equals(
								castOther.getMaxInterestRate())))
				&& ((this.getLoanType() == castOther.getLoanType()) || (this
						.getLoanType() != null
						&& castOther.getLoanType() != null && this
						.getLoanType().equals(castOther.getLoanType())));
	}

	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + (getId() == null ? 0 : this.getId().hashCode());
		result = 37 * result
				+ (getUserId() == null ? 0 : this.getUserId().hashCode());
		result = 37 * result
				+ (getAmount() == null ? 0 : this.getAmount().hashCode());
		result = 37 * result
				+ (getPeriod() == null ? 0 : this.getPeriod().hashCode());
		result = 37
				* result
				+ (getMinInvestAmount() == null ? 0 : this.getMinInvestAmount()
						.hashCode());
		result = 37 * result
				+ (getStatus() == null ? 0 : this.getStatus().hashCode());
		result = 37
				* result
				+ (getRepaymentSchedule() == null ? 0 : this
						.getRepaymentSchedule().hashCode());
		result = 37
				* result
				+ (getHasInvestedAmount() == null ? 0 : this
						.getHasInvestedAmount().hashCode());
		result = 37 * result
				+ (getAuditTime() == null ? 0 : this.getAuditTime().hashCode());
		result = 37
				* result
				+ (getCreditLevelId() == null ? 0 : this.getCreditLevelId()
						.hashCode());
		result = 37
				* result
				+ (getPeriodUnit() == null ? 0 : this.getPeriodUnit()
						.hashCode());
		result = 37
				* result
				+ (getAverageInvestAmount() == null ? 0 : this
						.getAverageInvestAmount().hashCode());
		result = 37 * result
				+ (getApr() == null ? 0 : this.getApr().hashCode());
		result = 37
				* result
				+ (getMinInterestRate() == null ? 0 : this.getMinInterestRate()
						.hashCode());
		result = 37
				* result
				+ (getMaxInterestRate() == null ? 0 : this.getMaxInterestRate()
						.hashCode());
		result = 37 * result
				+ (getLoanType() == null ? 0 : this.getLoanType().hashCode());
		return result;
	}

}