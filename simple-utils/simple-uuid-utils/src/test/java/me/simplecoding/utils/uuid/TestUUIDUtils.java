package me.simplecoding.utils.uuid;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class TestUUIDUtils {

	@Test
	public void test() {
		try {
			UUID uuid = UUIDUtils.uuid1();
			assertNotNull(uuid);
			assertEquals(1, uuid.version());

			String hex = UUIDUtils.uuid1Hex();
			assertNotNull(hex);
			assertTrue(hex.matches("[0-9a-fA-F]{32}"));
			uuid = UUIDUtils.fromHex(hex);
			assertNotNull(uuid);
			assertEquals(1, uuid.version());
			String name = hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-"
					+ hex.substring(16, 20) + "-" + hex.substring(20);
			UUID expected = UUID.fromString(name);
			assertNotNull(expected);
			assertEquals(expected, uuid);
		} catch (Exception e) {
			fail(e);
		}
	}

}
