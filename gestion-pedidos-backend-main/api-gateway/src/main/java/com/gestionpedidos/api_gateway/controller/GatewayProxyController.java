package com.gestionpedidos.api_gateway.controller;

import com.gestionpedidos.api_gateway.filter.RequestContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@RestController
public class GatewayProxyController {

	private final HttpClient httpClient;
	private final String authServiceUrl;
	private final String orderServiceUrl;

	public GatewayProxyController(
			@Value("${auth.service.url}") String authServiceUrl,
			@Value("${order.service.url}") String orderServiceUrl) {
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
		this.authServiceUrl = authServiceUrl;
		this.orderServiceUrl = orderServiceUrl;
	}

	@PostMapping(path = "/auth/register", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> register(@RequestBody String body, HttpServletRequest request)
			throws IOException, InterruptedException {
		return forwardJsonPost(authServiceUrl + "/auth/register", body, request);
	}

	@PostMapping(path = "/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> login(@RequestBody String body, HttpServletRequest request)
			throws IOException, InterruptedException {
		return forwardJsonPost(authServiceUrl + "/auth/login", body, request);
	}

	@GetMapping("/orders")
	public ResponseEntity<String> getOrders(HttpServletRequest request) throws IOException, InterruptedException {
		HttpRequest.Builder builder = baseRequest(orderServiceUrl + "/orders", request).GET();
		HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
		return ResponseEntity.status(HttpStatusCode.valueOf(response.statusCode())).body(response.body());
	}

	private ResponseEntity<String> forwardJsonPost(String url, String body, HttpServletRequest request)
			throws IOException, InterruptedException {
		HttpRequest.Builder builder = baseRequest(url, request)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.POST(HttpRequest.BodyPublishers.ofString(body));
		HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
		return ResponseEntity.status(HttpStatusCode.valueOf(response.statusCode())).body(response.body());
	}

	private HttpRequest.Builder baseRequest(String url, HttpServletRequest request) {
		HttpRequest.Builder builder = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.timeout(Duration.ofSeconds(5));

		String correlationId = (String) request.getAttribute(RequestContextFilter.CORRELATION_ID_ATTRIBUTE);
		if (correlationId != null && !correlationId.isBlank()) {
			builder.header(RequestContextFilter.CORRELATION_ID_HEADER, correlationId);
		}

		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader != null && !authHeader.isBlank()) {
			builder.header(HttpHeaders.AUTHORIZATION, authHeader);
		}

		return builder;
	}
}