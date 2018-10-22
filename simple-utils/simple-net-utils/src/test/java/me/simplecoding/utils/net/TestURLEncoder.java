package me.simplecoding.utils.net;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TestURLEncoder {

	@Test
	public void testEncode() {
		try {
			String s = "abc def*~";
			assertEquals("abc%20def*%7E", URLEncoderBuilder.newInstance().build().encode(s));
			assertEquals("abc+def*%7E", URLEncoderBuilder.newJDK().build().encode(s));
			assertEquals("abc%20def%2A~", URLEncoderBuilder.newOAuth10a().build().encode(s));
		} catch (Exception e) {
			fail(e);
		}
	}

}
