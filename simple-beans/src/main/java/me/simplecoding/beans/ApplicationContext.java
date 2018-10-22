package me.simplecoding.beans;

public interface ApplicationContext extends BeanFactory, ConfigContext {

	default <T> T loadSingletonConfig(String filename, ConfigConstructor<? extends T> constructor) throws Exception {
		T bean = loadConfig(filename, constructor);
		setSingleton(bean.getClass().getName(), bean);
		return bean;
	}

	default <T> T loadSingletonConfig(String name, String filename, ConfigConstructor<? extends T> constructor)
			throws Exception {
		T bean = loadConfig(filename, constructor);
		setSingleton(name, bean);
		return bean;
	}

}
