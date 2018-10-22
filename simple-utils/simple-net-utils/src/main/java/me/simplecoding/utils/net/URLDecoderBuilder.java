package me.simplecoding.utils.net;

import java.nio.charset.Charset;

public class URLDecoderBuilder {

	public static final URLDecoderBuilder newInstance() {
		return new URLDecoderBuilder();
	}

	private Charset charset = URLEncoderBuilder.UTF_8;

	public URLDecoderBuilder charset(Charset charset) {
		this.charset = charset;
		return this;
	}

	public URLDecoder build() {
		return new URLDecoderImpl(charset);
	}

	private static final class URLDecoderImpl implements URLDecoder {

		private final Charset charset;

		private URLDecoderImpl(Charset charset) {
			this.charset = charset;
		}

		@Override
		public String decode(String s) {
			boolean needToChange = false;
			int numChars = s.length();
			StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
			int i = 0;
			char c;
			byte[] bytes = null;
			while (i < numChars) {
				c = s.charAt(i);
				switch (c) {
				case '+':
					sb.append(' ');
					i++;
					needToChange = true;
					break;
				case '%':
					try {
						// (numChars-i)/3 is an upper bound for the number
						// of remaining bytes
						if (bytes == null)
							bytes = new byte[(numChars - i) / 3];
						int pos = 0;

						while (((i + 2) < numChars) && (c == '%')) {
							int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
							if (v < 0)
								throw new IllegalArgumentException(
										"URLDecoder: Illegal hex characters in escape (%) pattern - negative value");
							bytes[pos++] = (byte) v;
							i += 3;
							if (i < numChars)
								c = s.charAt(i);
						}
						// A trailing, incomplete byte encoding such as
						// "%x" will cause an exception to be thrown
						if ((i < numChars) && (c == '%'))
							throw new IllegalArgumentException("URLDecoder: Incomplete trailing escape (%) pattern");
						sb.append(new String(bytes, 0, pos, charset));
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(
								"URLDecoder: Illegal hex characters in escape (%) pattern - " + e.getMessage());
					}
					needToChange = true;
					break;
				default:
					sb.append(c);
					i++;
					break;
				}
			}
			return needToChange ? sb.toString() : s;
		}

	}

}
