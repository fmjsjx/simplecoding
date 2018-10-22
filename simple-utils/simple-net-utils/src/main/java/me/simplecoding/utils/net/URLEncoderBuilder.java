package me.simplecoding.utils.net;

import java.io.CharArrayWriter;
import java.nio.charset.Charset;
import java.util.BitSet;

public class URLEncoderBuilder {

	static final Charset UTF_8 = Charset.forName("UTF-8");

	static final int caseDiff = ('a' - 'A');
	static BitSet dontNeedEncoding;
	static {

		dontNeedEncoding = new BitSet(256);
		int i;
		for (i = 'a'; i <= 'z'; i++) {
			dontNeedEncoding.set(i);
		}
		for (i = 'A'; i <= 'Z'; i++) {
			dontNeedEncoding.set(i);
		}
		for (i = '0'; i <= '9'; i++) {
			dontNeedEncoding.set(i);
		}
		// dontNeedEncoding.set(' ');
		dontNeedEncoding.set('-');
		dontNeedEncoding.set('_');
		dontNeedEncoding.set('.');
		dontNeedEncoding.set('*');
		// dontNeedEncoding.set('~');
	}

	public static final URLEncoderBuilder newInstance() {
		return new URLEncoderBuilder();
	}

	public static final URLEncoderBuilder newJDK() {
		return new URLEncoderBuilder().ignore(' ');
	}

	public static final URLEncoderBuilder newOAuth10a() {
		return new URLEncoderBuilder().need('*').ignore('~');
	}

	// default charset is UTF-8
	private Charset charset = UTF_8;
	private final BitSet ignores = (BitSet) dontNeedEncoding.clone();

	public URLEncoderBuilder charset(Charset charset) {
		this.charset = charset;
		return this;
	}

	public URLEncoderBuilder ignore(char c) {
		ignores.set(c);
		return this;
	}

	public URLEncoderBuilder need(char c) {
		ignores.clear(c);
		return this;
	}

	public URLEncoder build() {
		return new UrlEncoderImpl(charset, ignores);
	}

	private static final class UrlEncoderImpl implements URLEncoder {

		private final Charset charset;
		private final BitSet ignores;

		private UrlEncoderImpl(Charset charset, BitSet ignores) {
			this.charset = charset;
			this.ignores = ignores;
		}

		@Override
		public String encode(String s) {
			boolean needToChange = false;
			StringBuilder out = new StringBuilder(s.length());
			CharArrayWriter charArrayWriter = new CharArrayWriter();
			for (int i = 0; i < s.length();) {
				int c = (int) s.charAt(i);
				if (ignores.get(c)) {
					if (c == ' ') {
						c = '+';
						needToChange = true;
					}
					out.append((char) c);
					i++;
				} else {
					// convert to external encoding before hex conversion
					do {
						charArrayWriter.write(c);
						if (c >= 0xD800 && c <= 0xDBFF) {
							if ((i + 1) < s.length()) {
								int d = (int) s.charAt(i + 1);
								if (d >= 0xDC00 && d <= 0xDFFF) {
									charArrayWriter.write(d);
									i++;
								}
							}
						}
						i++;
					} while (i < s.length() && !ignores.get((c = (int) s.charAt(i))));

					charArrayWriter.flush();
					String str = new String(charArrayWriter.toCharArray());
					byte[] ba = str.getBytes(charset);
					for (int j = 0; j < ba.length; j++) {
						out.append('%');
						char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
						// converting to use uppercase letter as part of
						// the hex value if ch is a letter.
						if (Character.isLetter(ch)) {
							ch -= caseDiff;
						}
						out.append(ch);
						ch = Character.forDigit(ba[j] & 0xF, 16);
						if (Character.isLetter(ch)) {
							ch -= caseDiff;
						}
						out.append(ch);
					}
					charArrayWriter.reset();
					needToChange = true;
				}
			}

			return (needToChange ? out.toString() : s);
		}

	}

}
