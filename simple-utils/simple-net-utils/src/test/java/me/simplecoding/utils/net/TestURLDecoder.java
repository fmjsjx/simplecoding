package me.simplecoding.utils.net;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TestURLDecoder {

	@Test
	public void testDecode() {
		try {
			String s = "abc def*~";
			assertEquals(s, URLDecoderBuilder.newInstance().build().decode("abc%20def*%7E"));
			assertEquals(s, URLDecoderBuilder.newInstance().build().decode("abc+def*%7E"));
			assertEquals(s, URLDecoderBuilder.newInstance().build().decode("abc%20def%2A~"));
		} catch (Exception e) {
			fail(e);
		}
	}

}
