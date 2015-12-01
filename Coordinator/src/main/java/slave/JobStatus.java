package slave;

public enum JobStatus {
	READY("0"), DONE("1"), FAILED("-1"), RUNNING("2");

	private String status;

	private JobStatus(String status) {
		this.status = status;
	}

	public String getStatus() {
		return this.status;
	}
}
