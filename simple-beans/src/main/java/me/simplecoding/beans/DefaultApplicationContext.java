package me.simplecoding.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.simplecoding.beans.annotation.DestroyMethod;
import me.simplecoding.beans.annotation.PrototypeConstructor;
import me.simplecoding.beans.annotation.Required;
import me.simplecoding.beans.exception.AssembedException;
import me.simplecoding.beans.exception.BeanException;
import me.simplecoding.beans.exception.DuplicatedBeanException;
import me.simplecoding.beans.exception.NoSuchBeanException;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class DefaultApplicationContext extends DefaultConfigContext implements ApplicationContext {

	private static final Logger logger = LoggerFactory.getLogger(DefaultApplicationContext.class);

	private final AtomicBoolean assembled = new AtomicBoolean(false);
	private final ConcurrentMap<String, BeanProvider<?>> beanMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Class<?>> prototypeTypeMap = new ConcurrentHashMap<>();
	private final BlockingQueue<Runnable> destroyListeners = new LinkedBlockingQueue<>();

	public DefaultApplicationContext() throws BeanException {
		this(true);
	}

	public DefaultApplicationContext(boolean selfSingleton) throws BeanException {
		super();
		setSingleton(getClass().getName(), this);
	}

	public DefaultApplicationContext(String confDir) throws BeanException {
		this(confDir, true);
	}

	public DefaultApplicationContext(String confDir, boolean selfSingleton) throws BeanException {
		super(confDir);
		setSingleton(getClass().getName(), this);
	}

	@Override
	public <T> void setSingleton(String name, T bean) throws BeanException {
		requireNotAssembed();
		checkBeanName(name);
		beanMap.put(name, new SingletonProvider(bean));
		addDestoryListener(name, bean);
	}

	private void checkBeanName(String name) throws DuplicatedBeanException {
		if (beanMap.containsKey(name) || prototypeTypeMap.containsKey(name)) {
			throw new DuplicatedBeanException(name);
		}
	}

	private void requireNotAssembed() throws AssembedException {
		if (isAssembled()) {
			throw new AssembedException();
		}
	}

	@Override
	public void setPrototype(String name, Class<?> beanType) throws BeanException {
		requireNotAssembed();
		checkBeanName(name);
		prototypeTypeMap.put(name, beanType);
	}

	@Override
	public void setPrototype(String name, BeanConstructor<?> constructor) throws BeanException {
		requireNotAssembed();
		checkBeanName(name);
		beanMap.put(name, new PrototypeProvider(constructor));
	}

	@Override
	public <T> T bean(String name) throws BeanException {
		if (isAssembled()) {
			BeanProvider<T> provider = getBeanProvider(name);
			if (provider == null) {
				throw new NoSuchBeanException(name);
			}
			return provider.get();
		}
		return null;
	}

	private <T> BeanProvider<T> getBeanProvider(String name) {
		return (BeanProvider<T>) beanMap.get(name);
	}

	@Override
	public <T> T bean(Class<? extends T> beanType) throws BeanException {
		if (isAssembled()) {
			return getBeanProvider(beanType).get();
		}
		return null;
	}

	private <T> BeanProvider<T> getBeanProvider(Class<? extends T> beanType)
			throws NoSuchBeanException, DuplicatedBeanException, BeanException {
		BeanProvider<T>[] providers = beanMap.values().stream()
				.filter(p -> p.beanType() != null && beanType.isAssignableFrom(p.beanType()))
				.toArray(BeanProvider[]::new);
		if (providers.length == 1) {
			return providers[0];
		} else if (providers.length == 0) {
			throw new NoSuchBeanException(beanType);
		} else {
			throw new DuplicatedBeanException(beanType);
		}
	}

	@Override
	public boolean isPrototype(String name) {
		BeanProvider<?> provider = (BeanProvider<?>) beanMap.get(name);
		return provider != null && !provider.isSingleton();
	}

	@Override
	public boolean isPrototype(Class<?> beanType) {
		BeanProvider<?>[] providers = beanMap.values().stream()
				.filter(p -> p.beanType() != null && beanType.isAssignableFrom(p.beanType()))
				.toArray(BeanProvider[]::new);
		if (providers.length == 1) {
			return !providers[0].isSingleton();
		}
		return false;
	}

	@Override
	public boolean isAssembled() {
		return assembled.get();
	}

	@Override
	public ApplicationContext assemble() throws BeanException {
		if (assembled.compareAndSet(false, true)) {
			try {
				// generate constructors for prototypes
				for (Iterator<Entry<String, Class<?>>> iterator = prototypeTypeMap.entrySet().iterator(); iterator
						.hasNext();) {
					Entry<String, Class<?>> entry = iterator.next();
					Class<?> type = entry.getValue();
					BeanConstructor<?> constructor = generatePrototypeFactory(type);
					beanMap.put(entry.getKey(), new PrototypeProvider(constructor, type));
				}
				prototypeTypeMap.clear();
				// assemble singleton dependencies
				Object[] singletonBeans = beanMap.values().stream().filter(BeanProvider::isSingleton)
						.map(p -> ((SingletonProvider) p).get()).toArray();
				for (Object bean : singletonBeans) {
					for (Class<?> type = bean.getClass(); type != Object.class; type = type.getSuperclass()) {
						Field[] fields = type.getDeclaredFields();
						for (Field field : fields) {
							Required required = field.getAnnotation(Required.class);
							if (required != null) {
								if (!field.isAccessible()) {
									field.setAccessible(true);
								}
								BeanProvider<?> provider = "".equals(required.value())
										? getBeanProvider(field.getType())
										: getBeanProvider(required.value());
								if (provider == null) {
									String name = required.value();
									if ("".equals(name)) {
										name = field.getType().getSimpleName();
									}
									throw new BeanException("missing required bean " + name);
								}
								field.set(bean, provider.get());
							}
						}
					}
				}
			} catch (BeanException e) {
				throw e;
			} catch (Exception e) {
				throw new BeanException(e);
			}
			return this;
		}
		throw new AssembedException();
	}

	private void addDestoryListener(String name, Object bean) throws BeanException {
		if (bean == this) {
			// ignore
			return;
		}
		if (bean instanceof AutoCloseable) {
			destroyListeners.add(() -> {
				try {
					((AutoCloseable) bean).close();
				} catch (Exception e) {
					// just logging
					logger.error("Error occurs when close bean named {}: {}.", name, bean, e);
				}
			});
		} else if (bean instanceof ExecutorService) {
			destroyListeners.add(() -> {
				try {
					((ExecutorService) bean).shutdown();
				} catch (Exception e) {
					// just logging
					logger.error("Error occurs when shutdown executor named {}: {}.", name, bean, e);
				}
			});
		} else {
			List<Method> methods = new ArrayList<>();
			for (Class<?> type = bean.getClass(); type != Object.class; type = type.getSuperclass()) {
				Arrays.stream(type.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(DestroyMethod.class))
						.forEach(methods::add);
			}
			if (methods.size() > 1) {
				throw new BeanException("multiple @DestoryMethod exists on " + bean.getClass().getSimpleName());
			} else if (methods.size() == 1) {
				Method dm = methods.get(0);
				if (!dm.isAccessible()) {
					dm.setAccessible(true);
				}
				if (dm.getParameters().length > 0) {
					throw new BeanException("error @DestoryMethod on " + bean.getClass().getSimpleName());
				}
				destroyListeners.add(() -> {
					try {
						dm.invoke(bean);
					} catch (Exception e) {
						// just logging
						logger.error("Error occurs when distory bean named {}: {}, {}.", name, bean, dm, e);
					}
				});
			}
		}
	}

	private BeanConstructor<?> generatePrototypeFactory(Class<?> type) throws BeanException {
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		Constructor<?> constructor = constructors.length == 1 ? constructors[0] : chooseConstructor(type, constructors);
		if (!constructor.isAccessible()) {
			constructor.setAccessible(true);
		}
		List<Field> fieldList = Arrays.stream(type.getDeclaredFields())
				.filter(f -> f.isAnnotationPresent(Required.class)).collect(Collectors.toList());
		for (Class<?> superType = type.getSuperclass(); superType != Object.class; superType = superType
				.getSuperclass()) {
			Arrays.stream(superType.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Required.class))
					.forEach(fieldList::add);
		}
		FieldProvider[] fieldProviders = fieldList.stream().map(FieldProvider::new).toArray(FieldProvider[]::new);
		Parameter[] parameters = constructor.getParameters();
		if (parameters.length == 0) {
			return () -> {
				Object bean = constructor.newInstance();
				for (int i = 0; i < fieldProviders.length; i++) {
					fieldProviders[i].setValue(bean);
				}
				return bean;
			};
		}
		DependencyProvider[] parameterProviders = Arrays.stream(parameters).map(ParameterProviderImpl::new)
				.toArray(DependencyProvider[]::new);
		return () -> {
			Object[] initargs = new Object[parameterProviders.length];
			for (int i = 0; i < initargs.length; i++) {
				initargs[i] = parameterProviders[i].get();
			}
			Object bean = constructor.newInstance(initargs);
			for (int i = 0; i < fieldProviders.length; i++) {
				fieldProviders[i].setValue(bean);
			}
			return bean;
		};
	}

	private Constructor<?> chooseConstructor(Class<?> type, Constructor<?>[] constructors) throws BeanException {
		Constructor<?>[] array = Arrays.stream(constructors)
				.filter(c -> c.isAnnotationPresent(PrototypeConstructor.class)).toArray(Constructor<?>[]::new);
		if (array.length == 1) {
			return array[0];
		} else if (array.length == 0) {
			throw new BeanException("missing @PrototypeConstructor on " + type);
		} else {
			throw new BeanException("more than one @PrototypeConstructor on " + type);
		}
	}

	@Override
	public ApplicationContext destroy() {
		destroyListeners.forEach(Runnable::run);
		return this;
	}

	@Override
	public void singletons(Consumer<SingletonBean> action) {
		if (isAssembled()) {
			beanMap.values().stream().filter(BeanProvider::isSingleton).map(SingletonBeanImpl::new).forEach(action);
		}
	}

	@Override
	public String toString() {
		return "DefaultApplicationContext[confDir=" + confDir() + ", assembled=" + assembled + "]";
	}

	private interface BeanProvider<T> {

		Class<? extends T> beanType();

		boolean isSingleton();

		T get() throws BeanException;

	}

	private static final class SingletonProvider<T> implements BeanProvider<T> {
		private final T bean;

		private SingletonProvider(T bean) {
			this.bean = bean;
		}

		@Override
		public Class<? extends T> beanType() {
			return (Class<? extends T>) bean.getClass();
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		public T get() {
			return bean;
		}
	}

	private static final class PrototypeProvider<T> implements BeanProvider<T> {

		private final BeanConstructor<T> beanConstructor;
		private final Class<? extends T> beanType;

		private PrototypeProvider(BeanConstructor<T> beanConstructor, Class<? extends T> beanType) {
			this.beanConstructor = Objects.requireNonNull(beanConstructor, "beanConstructor");
			this.beanType = beanType;
		}

		private PrototypeProvider(BeanConstructor<T> beanConstructor) {
			this(beanConstructor, null);
		}

		@Override
		public Class<? extends T> beanType() {
			return beanType;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}

		@Override
		public T get() throws BeanException {
			try {
				return beanConstructor.create();
			} catch (Exception e) {
				throw new BeanException(e);
			}
		}

	}

	@FunctionalInterface
	private interface DependencyProvider {
		Object get() throws Exception;
	}

	private final class FieldProvider implements DependencyProvider {

		private BeanProvider<?> beanProvider;
		private final Field field;

		private FieldProvider(Field field) {
			this.field = field;
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
		}

		private void setValue(Object obj) throws Exception {
			field.set(obj, get());
		}

		@Override
		public Object get() throws Exception {
			if (beanProvider == null) {
				synchronized (this) {
					if (beanProvider == null) {
						Required required = field.getDeclaredAnnotation(Required.class);
						if (required != null) {
							String name = required.value();
							if ("".equals(name)) {
								beanProvider = getBeanProvider(field.getType());
							} else {
								beanProvider = getBeanProvider(name);
								if (beanProvider == null) {
									throw new NoSuchBeanException(name);
								}
							}
						}
					}
				}
			}
			return beanProvider.get();
		}

	}

	private final class ParameterProviderImpl implements DependencyProvider {

		private BeanProvider<?> beanProvider;
		private final Parameter parameter;

		private ParameterProviderImpl(Parameter parameter) {
			this.parameter = parameter;
		}

		@Override
		public Object get() throws Exception {
			if (beanProvider == null) {
				synchronized (this) {
					if (beanProvider == null) {
						Required required = parameter.getDeclaredAnnotation(Required.class);
						if (required != null) {
							String name = required.value();
							if ("".equals(name)) {
								beanProvider = getBeanProvider(parameter.getType());
							} else {
								beanProvider = getBeanProvider(name);
								if (beanProvider == null) {
									throw new NoSuchBeanException(name);
								}
							}
						} else {
							beanProvider = getBeanProvider(parameter.getType());
						}
					}
				}
			}
			return beanProvider.get();
		}

	}

	private static final class SingletonBeanImpl implements SingletonBean {

		private final Class<?> type;
		private final Object bean;

		private SingletonBeanImpl(BeanProvider<?> beanProvider) {
			this.type = beanProvider.beanType();
			try {
				this.bean = beanProvider.get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Class<?> type() {
			return type;
		}

		@Override
		public Object bean() {
			return bean;
		}

		@Override
		public String toString() {
			return "SingletonBean[type=" + type + ", bean=" + bean + "]";
		}

	}

}
