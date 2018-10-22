package me.simplecoding.beans;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public interface ConfigContext {

	Charset UTF_8 = Charset.forName("UTF-8");

	default Charset defaultCharset() {
		return UTF_8;
	}

	String confDir();

	default File configFile(String filename) {
		return new File(confDir(), filename);
	}

	default String configFilePath(String filename) {
		return configFile(filename).getPath();
	}

	default void loadConfig(String filename, SourceConsumer consumer) throws IOException {
		try (FileInputStream in = new FileInputStream(configFile(filename))) {
			consumer.accept(in);
		}
	}

	default <T> T loadConfig(String filename, ConfigConstructor<T> constructor) throws IOException {
		try (FileInputStream in = new FileInputStream(configFile(filename))) {
			return constructor.create(in);
		}
	}

	@FunctionalInterface
	public interface SourceConsumer {
		void accept(InputStream in) throws IOException;
	}

	@FunctionalInterface
	public interface ConfigConstructor<T> {
		T create(InputStream in) throws IOException;
	}

}
