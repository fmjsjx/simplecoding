package me.simplecoding.utils.net;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class HttpRequest {

	private URLEncoder urlEncoder;
	private HttpMethod method = HttpMethod.GET;
	private String url;
	private final Map<String, String> headers = new LinkedHashMap<>();
	private final Map<String, List<String>> queryParams = new LinkedHashMap<>();
	private byte[] body;

	public URLEncoder urlEncoder() {
		return urlEncoder;
	}

	public HttpRequest urlEncoder(URLEncoder urlEncoder) {
		this.urlEncoder = urlEncoder;
		return this;
	}

	public HttpMethod method() {
		return method;
	}

	public HttpRequest method(HttpMethod method) {
		this.method = method;
		return this;
	}

	public String url() {
		return url;
	}

	public HttpRequest url(String url) {
		this.url = url;
		return this;
	}

	public byte[] body() {
		return body;
	}

	public HttpRequest body(byte[] body) {
		this.body = body;
		return this;
	}

	public Map<String, String> headers() {
		return headers;
	}

	public String header(String name) {
		return headers.get(name);
	}

	public HttpRequest header(String name, String value) {
		headers.put(name, value);
		return this;
	}

	public HttpRequest fixHeader(String name, Supplier<String> valueSupplier) {
		headers.computeIfAbsent(name, k -> valueSupplier.get());
		return this;
	}

	public String removeHeader(String name) {
		return headers.remove(name);
	}

	public Map<String, List<String>> queryParams() {
		return queryParams;
	}

	public HttpRequest addQuery(String name, String value) {
		queryParams.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
		return this;
	}

	public String query(String name) {
		List<String> values = queryParams.get(name);
		return values == null ? null : values.get(0);
	}

	public List<String> querys(String name) {
		List<String> values = queryParams.get(name);
		return values == null ? null : Collections.unmodifiableList(values);
	}

	public List<String> removeQuery(String name) {
		return queryParams.remove(name);
	}

	public String completeUrl() {
		if (queryParams.isEmpty()) {
			return url;
		}
		if (urlEncoder == null) {
			urlEncoder = DefaultURLEncoderHolder.defaultEncoder;
		}
		StringBuilder b = new StringBuilder(url).append("?");
		int baseLength = b.length();
		queryParams.forEach((n, vs) -> vs.forEach(v -> {
			if (b.length() != baseLength) {
				b.append('&');
			}
			b.append(urlEncoder.encode(n)).append('=').append(urlEncoder.encode(v));
		}));
		return null;
	}

	@Override
	public String toString() {
		return "HttpRequest[method=" + method + ", url=" + url + ", headers=" + headers + ", queryParams=" + queryParams
				+ ", body=" + (body == null ? null : new String(body, Charset.forName("UTF-8"))) + "]";
	}

	private static final class DefaultURLEncoderHolder {
		private static final URLEncoder defaultEncoder = URLEncoderBuilder.newInstance().build();
	}

}
