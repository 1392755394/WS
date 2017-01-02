package models;

/**
 * TUserFundId entity. @author MyEclipse Persistence Tools
 */

public class TUserFundId implements java.io.Serializable {

	// Fields

	private Long id;
	private Long bidCount;
	private Long investCount;
	private Long transferCount;

	// Constructors

	/** default constructor */
	public TUserFundId() {
	}

	/** full constructor */
	public TUserFundId(Long id, Long bidCount, Long investCount,
			Long transferCount) {
		this.id = id;
		this.bidCount = bidCount;
		this.investCount = investCount;
		this.transferCount = transferCount;
	}

	// Property accessors

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getBidCount() {
		return this.bidCount;
	}

	public void setBidCount(Long bidCount) {
		this.bidCount = bidCount;
	}

	public Long getInvestCount() {
		return this.investCount;
	}

	public void setInvestCount(Long investCount) {
		this.investCount = investCount;
	}

	public Long getTransferCount() {
		return this.transferCount;
	}

	public void setTransferCount(Long transferCount) {
		this.transferCount = transferCount;
	}

	@Override
	public boolean equals(Object other) {
		if ((this == other))
			return true;
		if ((other == null))
			return false;
		if (!(other instanceof TUserFundId))
			return false;
		TUserFundId castOther = (TUserFundId) other;

		return ((this.getId() == castOther.getId()) || (this.getId() != null
				&& castOther.getId() != null && this.getId().equals(
				castOther.getId())))
				&& ((this.getBidCount() == castOther.getBidCount()) || (this
						.getBidCount() != null
						&& castOther.getBidCount() != null && this
						.getBidCount().equals(castOther.getBidCount())))
				&& ((this.getInvestCount() == castOther.getInvestCount()) || (this
						.getInvestCount() != null
						&& castOther.getInvestCount() != null && this
						.getInvestCount().equals(castOther.getInvestCount())))
				&& ((this.getTransferCount() == castOther.getTransferCount()) || (this
						.getTransferCount() != null
						&& castOther.getTransferCount() != null && this
						.getTransferCount()
						.equals(castOther.getTransferCount())));
	}

	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + (getId() == null ? 0 : this.getId().hashCode());
		result = 37 * result
				+ (getBidCount() == null ? 0 : this.getBidCount().hashCode());
		result = 37
				* result
				+ (getInvestCount() == null ? 0 : this.getInvestCount()
						.hashCode());
		result = 37
				* result
				+ (getTransferCount() == null ? 0 : this.getTransferCount()
						.hashCode());
		return result;
	}

}