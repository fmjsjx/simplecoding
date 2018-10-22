package me.simplecoding.utils.uuid;

import static java.util.Objects.nonNull;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UUIDUtils {

	private static final Logger logger = LoggerFactory.getLogger(UUIDUtils.class);

	private static final long variantAndNode;

	private static volatile long clockSeq = System.nanoTime() & 0x3fff;
	private static volatile long lastMillis = System.currentTimeMillis();
	private static volatile long baseNanos = System.nanoTime();
	private static volatile long lastNanos100 = 0;

	static {
		long variant = 0x8000000000000000L;
		byte[] address = null;
		try {
			Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
			if (ifs != null) {
				while (ifs.hasMoreElements()) {
					NetworkInterface iface = ifs.nextElement();
					byte[] hardware = iface.getHardwareAddress();
					if (hardware != null && hardware.length == 6 && hardware[1] != (byte) 0xff) {
						address = hardware;
						break;
					}
				}
			}
		} catch (Exception e) {
			// ignore
		}
		long node;
		if (nonNull(address)) {
			node = (address[0] & 0xffL) << 40;
			node |= (address[1] & 0xffL) << 32;
			node |= (address[2] & 0xffL) << 24;
			node |= (address[3] & 0xffL) << 16;
			node |= (address[4] & 0xffL) << 8;
			node |= address[5] & 0xffL;
		} else {
			try {
				address = InetAddress.getLocalHost().getAddress();
				node = (address[0] & 0xFFL) << 24;
				node |= (address[1] & 0xFFL) << 16;
				node |= (address[2] & 0xFFL) << 8;
				node |= address[3] & 0xFFL;
			} catch (UnknownHostException e) {
				node = System.nanoTime() & 0x7fffffff;
				logger.warn("Can't get node info, use nano time based node value {}.", node);
			}
		}
		variantAndNode = (variant | node);
	}

	private static final Pattern hexPattern = Pattern.compile("[0-9a-fA-F]+");

	public static final UUID uuid1() {
		long utc = utcTime();
		// version 1
		long mostSigBits = 0x0000000000001000L;
		// time low
		mostSigBits |= utc << 32;
		// time mid
		mostSigBits |= (utc & 0xFFFF00000000L) >> 16;
		// time hi and version
		mostSigBits |= (utc >>> 48) & 0x0FFF;
		long leastSigBits = variantAndNode | (clockSeq << 48);
		return new UUID(mostSigBits, leastSigBits);
	}

	private synchronized static final long utcTime() {
		long millis = System.currentTimeMillis();
		if (millis > lastMillis) {
			baseNanos = System.nanoTime();
			lastNanos100 = 0;
			lastMillis = millis;
		} else {
			long nano = System.nanoTime();
			long nanos100 = Math.min(9999, (nano - baseNanos) / 100);
			if (lastNanos100 == nanos100) {
				clockSeq++;
				if (clockSeq == 0x4000) {
					clockSeq = 0;
				}
			} else {
				lastNanos100 = nanos100;
			}
		}
		return lastMillis * 10000 + 0x01B21DD213814000L + lastNanos100;
	}

	public static final String toHexString(UUID uuid) {
		long mostSigBits = uuid.getMostSignificantBits();
		long leastSigBits = uuid.getLeastSignificantBits();
		return new StringBuilder(32).append(digits(mostSigBits >> 32, 8)) // time low
				.append(digits(mostSigBits >> 16, 4)) // time mid
				.append(digits(mostSigBits, 4)) // version and time hi
				.append(digits(leastSigBits >> 48, 4)) // variant and sequence
				.append(digits(leastSigBits, 12)) // node
				.toString();
	}

	public static final String uuid1Hex() {
		return toHexString(uuid1());
	}

	private static final String digits(long val, int digits) {
		long hi = 1L << (digits * 4);
		return Long.toHexString(hi | (val & (hi - 1))).substring(1);
	}

	public static final UUID fromHex(String hex) {
		if (hex.length() != 32 && !hexPattern.matcher(hex).matches()) {
			throw new IllegalArgumentException("Invalid UUID HEX string: " + hex);
		}
		String[] components = new String[5];
		components[0] = hex.substring(0, 8);
		components[1] = hex.substring(8, 12);
		components[2] = hex.substring(12, 16);
		components[3] = hex.substring(16, 20);
		components[4] = hex.substring(20);
		long mostSigBits = Long.parseLong(components[0], 16);
		mostSigBits <<= 16;
		mostSigBits |= Long.parseLong(components[1], 16);
		mostSigBits <<= 16;
		mostSigBits |= Long.parseLong(components[2], 16);

		long leastSigBits = Long.parseLong(components[3], 16);
		leastSigBits <<= 48;
		leastSigBits |= Long.parseLong(components[4], 16);
		return new UUID(mostSigBits, leastSigBits);
	}

	private UUIDUtils() {
		// only provides class methods
	}

}
