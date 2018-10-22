package me.simplecoding.beans;

public class DefaultConfigContext implements ConfigContext {

	private static final String DEFAULT_CONF_DIR = System.getProperty("simplecoding.conf.dir", "conf");

	private final String confDir;

	public DefaultConfigContext() {
		this(DEFAULT_CONF_DIR);
	}

	public DefaultConfigContext(String confDir) {
		this.confDir = confDir;
	}

	public final String confDir() {
		return confDir;
	}

	@Override
	public String toString() {
		return "DefaultConfigContext[confDir=" + confDir + "]";
	}

}
