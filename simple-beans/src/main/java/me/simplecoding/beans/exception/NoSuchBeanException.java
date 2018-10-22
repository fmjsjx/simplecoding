package me.simplecoding.beans.exception;

public class NoSuchBeanException extends BeanException {

	private static final long serialVersionUID = 1L;

	private String name;
	private Class<?> type;

	public NoSuchBeanException(String name) {
		super("name " + name);
		this.name = name;
	}

	public NoSuchBeanException(Class<?> type) {
		super("type " + type.getName());
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public Class<?> getType() {
		return type;
	}

}
