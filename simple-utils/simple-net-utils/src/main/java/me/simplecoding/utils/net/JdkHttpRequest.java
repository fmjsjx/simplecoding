package me.simplecoding.utils.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class JdkHttpRequest implements AutoCloseable {

	public static final JdkHttpRequest execute(HttpRequest request)
			throws MalformedURLException, ProtocolException, IOException {
		return new JdkHttpRequest(request);
	}

	private final HttpRequest request;
	private final HttpURLConnection connection;
	private final HttpResponse response;

	private JdkHttpRequest(HttpRequest request) throws MalformedURLException, ProtocolException, IOException {
		this.request = request;
		String completeUrl = request.completeUrl();
		this.connection = (HttpURLConnection) new URL(completeUrl).openConnection();
		connection.setInstanceFollowRedirects(true);
		connection.setUseCaches(false);
		HttpMethod method = request.method();
		connection.setRequestMethod(method.name());
		// add headers
		request.headers().forEach(connection::addRequestProperty);
		// body
		if (method.permitBody()) {
			int contentLength = request.body().length;
			if (method.requiredBody() || contentLength > 0) {
				connection.setRequestProperty("content-length", String.valueOf(contentLength));
			}
			if (connection.getRequestProperty("content-type") == null) {
				connection.setRequestProperty("content-type", "application/x-www-form-urlencoded");
			}
			connection.setDoOutput(true);
			connection.connect();
			try (OutputStream out = connection.getOutputStream()) {
				out.write(request.body());
				out.flush();
			}
		} else {
			connection.connect();
		}
		int responseCode = connection.getResponseCode();
		this.response = new HttpResponse(responseCode, connection.getResponseMessage(),
				responseCode >= 200 && responseCode < 400 ? connection.getInputStream() : connection.getErrorStream());
		connection.getHeaderFields().forEach((k, v) -> {
			if (k != null) {
				if (v.size() == 1) {
					response.header(k, v.get(0));
				} else {
					response.header(k, String.join(", ", v));
				}
			}
		});
	}

	public HttpRequest request() {
		return request;
	}

	public HttpResponse response() {
		return response;
	}

	@Override
	public void close() throws Exception {
		if (response != null && response.bodyStream() != null) {
			response.bodyStream().close();
		}
		connection.disconnect();
	}

	@Override
	public String toString() {
		return "JdkHttpRequest[request=" + request + ", connection=" + connection + ", response=" + response + "]";
	}

}
