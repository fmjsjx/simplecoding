package me.simplecoding.beans;

import java.util.function.Consumer;

import me.simplecoding.beans.exception.BeanException;

public interface BeanFactory extends AutoCloseable {

	default <T> T create(BeanConstructor<? extends T> constructor) throws BeanException {
		try {
			return constructor.create();
		} catch (Exception e) {
			throw new BeanException(e);
		}
	}

	default <T, A> T create(BeanInitargConstructor<? extends T, A> constructor, A arg) throws BeanException {
		try {
			return constructor.create(arg);
		} catch (Exception e) {
			throw new BeanException(e);
		}
	}

	default <T> T singleton(String name, BeanConstructor<? extends T> constructor) throws BeanException {
		T bean = create(constructor);
		setSingleton(name, bean);
		return bean;
	}

	default <T, A> T singleton(String name, BeanInitargConstructor<? extends T, A> constructor, A arg)
			throws BeanException {
		T bean = create(constructor, arg);
		setSingleton(name, bean);
		return bean;
	}

	default <T> T singleton(BeanConstructor<? extends T> constructor) throws BeanException {
		T bean = create(constructor);
		setSingleton(bean.getClass().getName(), bean);
		return bean;
	}

	default <T, A> T singleton(BeanInitargConstructor<? extends T, A> constructor, A arg) throws BeanException {
		T bean = create(constructor, arg);
		setSingleton(bean.getClass().getName(), bean);
		return bean;
	}

	<T> void setSingleton(String name, T bean) throws BeanException;

	default void setPrototype(Class<?> beanType) throws BeanException {
		setPrototype(beanType.getName(), beanType);
	}

	void setPrototype(String name, Class<?> beanType) throws BeanException;

	void setPrototype(String name, BeanConstructor<?> constructor) throws BeanException;

	<T> T bean(String name) throws BeanException;

	<T> T bean(Class<? extends T> beanType) throws BeanException;

	default <T> T bean(String name, Class<? extends T> beanType) throws BeanException {
		return bean(name);
	}

	boolean isPrototype(String name);

	boolean isPrototype(Class<?> beanType);

	boolean isAssembled();

	ApplicationContext assemble() throws BeanException;

	ApplicationContext destroy();

	@Override
	default void close() {
		destroy();
	}

	interface SingletonBean {

		Class<?> type();

		Object bean();

	}

	void singletons(Consumer<SingletonBean> action);

}
