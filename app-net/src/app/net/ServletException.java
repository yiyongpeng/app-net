package app.net;

import java.util.Arrays;

public class ServletException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3927890470139293685L;

	private int type;
	private String[] params;

	public ServletException(int type, String message, String... params) {
		super(message);
		this.type = type;
		this.params = params;
	}

	public ServletException(Throwable e, int type, String message,
			String... params) {
		super(message, e);
		this.type = type;
		this.params = params;
	}

	public ServletException(int type) {
		this(type, "Server Error!");
	}

	public ServletException(int type, Throwable e) {
		this(e, type, "Server Error!");
	}

	public ServletException(String message) {
		this(500, message);
	}

	public int getType() {
		return type;
	}

	public String[] getParams() {
		return params;
	}

	@Override
	public String toString() {
		return new StringBuilder(super.toString()).append("  ErroCode: ")
				.append(type).append("  Params: ")
				.append(Arrays.toString(params)).toString();
	}
}
