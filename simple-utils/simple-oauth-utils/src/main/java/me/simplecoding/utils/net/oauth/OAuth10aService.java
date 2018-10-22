package me.simplecoding.utils.net.oauth;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import me.simplecoding.utils.net.HttpRequest;
import me.simplecoding.utils.net.URLEncoder;
import me.simplecoding.utils.net.URLEncoderBuilder;

public class OAuth10aService {

	private static final Random R = new Random();
	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static boolean isEmpty(String val) {
		return val == null || val.isEmpty();
	}

	private static boolean isNotEmpty(String val) {
		return !isEmpty(val);
	}

	private final String consumerKey;
	private final String secret;
	private URLEncoder urlEncoder;
	private final Map<String, String> ext = new LinkedHashMap<>();

	private OAuth10aService(String consumerKey, String secret) {
		this.consumerKey = consumerKey;
		this.secret = secret;
	}

	public OAuth10aService oauthParam(String name, String value) {
		ext.put(name, value);
		return this;
	}

	public String removeOauthParam(String name) {
		return ext.remove(name);
	}

	public String consumerKey() {
		return consumerKey;
	}

	public String secret() {
		return secret;
	}

	public OAuth10aService urlEncoder(URLEncoder urlEncoder) {
		this.urlEncoder = urlEncoder;
		return this;
	}

	public HttpRequest sign(HttpRequest request) {
		return sign(request, null, null);
	}

	public HttpRequest sign(HttpRequest request, String token, String tokenSecret) {
		long time = System.currentTimeMillis() / 1000;
		String nonce = String.valueOf(time + R.nextInt());
		StringBuilder baseStringBuilder = new StringBuilder().append(request.method().name()).append('&')
				.append(urlEncoder.encode(request.url()));
		List<Parameter> params = new ArrayList<>();
		request.queryParams().forEach((k, vs) -> vs.forEach(v -> params.add(new Parameter(k, v))));
		params.add(new Parameter("oauth_consumer_key", consumerKey));
		params.add(new Parameter("oauth_nonce", nonce));
		params.add(new Parameter("oauth_signature_method", "HMAC-SHA1"));
		params.add(new Parameter("oauth_timestamp", String.valueOf(time)));
		if (isNotEmpty(token)) {
			params.add(new Parameter("oauth_token", token));
		}
		params.add(new Parameter("oauth_version", "1.0"));
		ext.forEach((k, v) -> params.add(new Parameter(k, v)));
		baseStringBuilder.append('&');
		int len = baseStringBuilder.length();
		params.stream().sorted().forEach(p -> {
			if (len != baseStringBuilder.length()) {
				baseStringBuilder.append("%26");
			}
			baseStringBuilder.append(urlEncoder.encode(p.name)).append("%3D").append(urlEncoder.encode(p.value));
		});
		String baseString = baseStringBuilder.toString();
		String keyString = isEmpty(tokenSecret) ? urlEncoder.encode(secret) + "&"
				: urlEncoder.encode(secret) + "&" + urlEncoder.encode(tokenSecret);
		String signature = generateSignature(baseString, keyString);
		Map<String, String> oauthParams = new LinkedHashMap<>();
		oauthParams.put("oauth_consumer_key", consumerKey);
		oauthParams.put("oauth_nonce", nonce);
		oauthParams.put("oauth_signature", signature);
		oauthParams.put("oauth_signature_method", "HMAC-SHA1");
		oauthParams.put("oauth_timestamp", String.valueOf(time));
		if (isNotEmpty(token)) {
			oauthParams.put("oauth_token", token);
		}
		oauthParams.put("oauth_version", "1.0");
		ext.forEach(oauthParams::put);
		StringBuilder ab = new StringBuilder().append("OAuth ");
		oauthParams.forEach((k, v) -> {
			if (ab.length() != 6) {
				ab.append(", ");
			}
			ab.append(k).append('=').append('"').append(v).append('"');
		});
		String authorization = ab.toString();
		request.header("authorization", authorization);
		return request;
	}

	private String generateSignature(String baseString, String keyString) {
		try {
			SecretKeySpec key = new SecretKeySpec(keyString.getBytes(UTF_8), "HmacSHA1");
			Mac mac;
			mac = Mac.getInstance("HmacSHA1");
			mac.init(key);
			byte[] b = mac.doFinal(baseString.getBytes(UTF_8));
			return urlEncoder.encode(Base64.getEncoder().encodeToString(b));
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalArgumentException("invalid key " + keyString, e);
		}
	}

	public static final class Builder {

		public static final Builder newInstance() {
			return new Builder();
		}

		private String consumerKey;
		private String secret;
		private URLEncoder urlEncoder;

		private Builder() {
		}

		public Builder consumerKey(String consumerKey) {
			this.consumerKey = consumerKey;
			return this;
		}

		public Builder secret(String secret) {
			this.secret = secret;
			return this;
		}

		public Builder urlEncoder(URLEncoder urlEncoder) {
			this.urlEncoder = urlEncoder;
			return this;
		}

		public OAuth10aService build() {
			return new OAuth10aService(consumerKey, secret)
					.urlEncoder(urlEncoder == null ? URLEncoderBuilder.newOAuth10a().build() : urlEncoder);
		}

	}

	private static class Parameter implements Comparable<Parameter> {
		private final String name;
		private final String value;

		private Parameter(String name, String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public int compareTo(Parameter o) {
			return name.compareTo(o.name);
		}
	}

}
