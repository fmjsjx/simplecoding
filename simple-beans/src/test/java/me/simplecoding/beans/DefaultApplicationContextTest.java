package me.simplecoding.beans;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Closeable;

import org.junit.jupiter.api.Test;

import me.simplecoding.beans.annotation.DestroyMethod;
import me.simplecoding.beans.annotation.PrototypeConstructor;
import me.simplecoding.beans.annotation.Required;
import me.simplecoding.beans.exception.BeanException;

public class DefaultApplicationContextTest {

	@Test
	public void testPrototype() {
		Bean4 bean4 = null;
		Bean5 bean5 = null;
		try (DefaultApplicationContext ctx = new DefaultApplicationContext("")) {

			ctx.setSingleton("bean3Name", "TestBean3");
			ctx.setSingleton("bean4Name", "TestBean4");
			bean4 = ctx.singleton("bean4", Bean4::new);
			ctx.setPrototype("bean1", Bean1.class);
			ctx.setPrototype("bean2", Bean2.class);
			ctx.setPrototype("bean3", Bean3.class);
			bean5 = ctx.singleton("bean5", Bean5::new);

			ctx.assemble();

			Bean1 bean1_0 = ctx.bean("bean1");
			assertNotNull(bean1_0);
			assertNotNull(bean1_0.bean2);
			assertNotNull(bean1_0.bean2.bean3);
			assertNotNull(bean1_0.bean2.bean3.bean4);
			assertNotNull(bean1_0.bean2.bean3.name);
			assertEquals("TestBean3", bean1_0.bean2.bean3.name);
			assertNotNull(bean1_0.bean2.bean3.bean4.name);
			assertEquals("TestBean4", bean1_0.bean2.bean3.bean4.name);

			Bean1 bean1_1 = ctx.bean(Bean1.class);
			assertNotNull(bean1_1);
			assertNotNull(bean1_1.bean2);
			assertNotNull(bean1_1.bean2.bean3);
			assertNotNull(bean1_1.bean2.bean3.bean4);
			assertNotNull(bean1_1.bean2.bean3.name);
			assertEquals("TestBean3", bean1_1.bean2.bean3.name);
			assertNotNull(bean1_1.bean2.bean3.bean4.name);
			assertEquals("TestBean4", bean1_1.bean2.bean3.bean4.name);

			assertNotEquals(bean1_0, bean1_1);
			assertNotEquals(bean1_0.bean2, bean1_1.bean2);
			assertNotEquals(bean1_0.bean2.bean3, bean1_1.bean2.bean3);
			assertEquals(bean1_0.bean2.bean3.name, bean1_1.bean2.bean3.name);
			assertEquals(bean1_0.bean2.bean3.bean4, bean1_1.bean2.bean3.bean4);
			System.out.println("ok");
		} catch (BeanException e) {
			fail(e);
		}
		assertTrue(bean4.closed);
		assertTrue(bean5.shutdowned);
	}

	private static final class Bean1 {

		private final Bean2 bean2;

		@PrototypeConstructor
		private Bean1(Bean2 bean2) {
			this.bean2 = bean2;
		}

		private Bean1() {
			this(null);
		}

	}

	private static final class Bean2 {

		private final Bean3 bean3;

		private Bean2(@Required("bean3") Bean3 bean3) {
			this.bean3 = bean3;
		}

	}

	private static final class Bean3 {

		@Required("bean4")
		private Bean4 bean4;
		@Required("bean3Name")
		private String name;

	}

	private static final class Bean4 implements Closeable {

		boolean closed = false;
		// singleton
		@Required("bean4Name")
		private String name;

		@Override
		public void close() {
			closed = true;
		}
	}

	private static final class Bean5 {
		boolean shutdowned = false;

		@DestroyMethod
		public void shutdown() {
			shutdowned = true;
		}
	}

}
