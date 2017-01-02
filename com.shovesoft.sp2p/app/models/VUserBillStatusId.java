package models;

/**
 * VUserBillStatusId entity. @author MyEclipse Persistence Tools
 */

public class VUserBillStatusId implements java.io.Serializable {

	// Fields

	private Long id;
	private Long bidId;
	private Short status;
	private Long billId;

	// Constructors

	/** default constructor */
	public VUserBillStatusId() {
	}

	/** minimal constructor */
	public VUserBillStatusId(Long id, Long bidId, Long billId) {
		this.id = id;
		this.bidId = bidId;
		this.billId = billId;
	}

	/** full constructor */
	public VUserBillStatusId(Long id, Long bidId, Short status, Long billId) {
		this.id = id;
		this.bidId = bidId;
		this.status = status;
		this.billId = billId;
	}

	// Property accessors

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getBidId() {
		return this.bidId;
	}

	public void setBidId(Long bidId) {
		this.bidId = bidId;
	}

	public Short getStatus() {
		return this.status;
	}

	public void setStatus(Short status) {
		this.status = status;
	}

	public Long getBillId() {
		return this.billId;
	}

	public void setBillId(Long billId) {
		this.billId = billId;
	}

	@Override
	public boolean equals(Object other) {
		if ((this == other))
			return true;
		if ((other == null))
			return false;
		if (!(other instanceof VUserBillStatusId))
			return false;
		VUserBillStatusId castOther = (VUserBillStatusId) other;

		return ((this.getId() == castOther.getId()) || (this.getId() != null
				&& castOther.getId() != null && this.getId().equals(
				castOther.getId())))
				&& ((this.getBidId() == castOther.getBidId()) || (this
						.getBidId() != null
						&& castOther.getBidId() != null && this.getBidId()
						.equals(castOther.getBidId())))
				&& ((this.getStatus() == castOther.getStatus()) || (this
						.getStatus() != null
						&& castOther.getStatus() != null && this.getStatus()
						.equals(castOther.getStatus())))
				&& ((this.getBillId() == castOther.getBillId()) || (this
						.getBillId() != null
						&& castOther.getBillId() != null && this.getBillId()
						.equals(castOther.getBillId())));
	}

	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + (getId() == null ? 0 : this.getId().hashCode());
		result = 37 * result
				+ (getBidId() == null ? 0 : this.getBidId().hashCode());
		result = 37 * result
				+ (getStatus() == null ? 0 : this.getStatus().hashCode());
		result = 37 * result
				+ (getBillId() == null ? 0 : this.getBillId().hashCode());
		return result;
	}

}