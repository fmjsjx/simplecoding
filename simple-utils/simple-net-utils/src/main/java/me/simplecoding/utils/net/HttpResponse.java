package me.simplecoding.utils.net;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {

	private final int code;
	private final String message;
	private final Map<String, String> headers = new LinkedHashMap<>();
	private final InputStream bodyStream;

	public HttpResponse(int code, String message, InputStream bodyStream) {
		this.code = code;
		this.message = message;
		this.bodyStream = bodyStream;
	}

	public HttpResponse(int code, String message, Map<String, String> headers, InputStream bodyStream) {
		this(code, message, bodyStream);
		this.headers.putAll(headers);
	}

	public int code() {
		return code;
	}

	public boolean isOk() {
		return code == 200;
	}

	public boolean isSuccessful() {
		return code >= 200 && code < 400;
	}

	public String message() {
		return message;
	}

	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}

	public String header(String name) {
		return headers.get(name.toLowerCase());
	}

	HttpResponse header(String name, String value) {
		headers.put(name.toLowerCase(), value);
		return this;
	}

	public boolean headerEquals(String name, String value) {
		String actual = header(name);
		return actual == null ? false : value.equals(actual);
	}

	public InputStream bodyStream() {
		return bodyStream;
	}

	@Override
	public String toString() {
		return "HttpResponse[code=" + code + ", message=" + message + ", headers=" + headers + ", bodyStream="
				+ bodyStream + "]";
	}

}
