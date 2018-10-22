package me.simplecoding.beans.exception;

public class BeanException extends Exception {

	private static final long serialVersionUID = 1L;

	public BeanException() {
		super();
	}

	public BeanException(String message, Throwable cause) {
		super(message, cause);
	}

	public BeanException(String message) {
		super(message);
	}

	public BeanException(Throwable cause) {
		super(cause);
	}

}
