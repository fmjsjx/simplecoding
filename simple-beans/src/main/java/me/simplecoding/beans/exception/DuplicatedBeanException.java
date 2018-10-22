package me.simplecoding.beans.exception;

public class DuplicatedBeanException extends BeanException {

	private static final long serialVersionUID = 1L;

	private String name;
	private Class<?> type;

	public DuplicatedBeanException(String name) {
		super("name " + name);
		this.name = name;
	}

	public DuplicatedBeanException(Class<?> type) {
		super("type " + type);
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public Class<?> getType() {
		return type;
	}

}
