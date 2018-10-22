package me.simplecoding.utils.net;

public enum HttpMethod {

	GET(false), POST(true), PUT(true), DELETE(false, true), HEAD(false), OPTIONS(false), TRACE(false), PATCH(true);

	private final boolean requiredBody;
	private final boolean permitBody;

	private HttpMethod(boolean requreBody) {
		this(requreBody, requreBody);
	}

	private HttpMethod(boolean requreBody, boolean permitBody) {
		this.requiredBody = requreBody;
		this.permitBody = permitBody;
	}

	public boolean requiredBody() {
		return requiredBody;
	}

	public boolean permitBody() {
		return permitBody;
	}

}
