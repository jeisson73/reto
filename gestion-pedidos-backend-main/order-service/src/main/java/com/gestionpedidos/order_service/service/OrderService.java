package com.gestionpedidos.order_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestionpedidos.order_service.model.CatalogProduct;
import com.gestionpedidos.order_service.model.OrderEvent;
import com.gestionpedidos.order_service.model.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Service
public class OrderService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final RabbitTemplate rabbitTemplate;
	private final String catalogServiceUrl;
	private final int timeoutMs;
	private final String queueName;

	public OrderService(
			ObjectMapper objectMapper,
			RabbitTemplate rabbitTemplate,
			@Value("${catalog.service.url}") String catalogServiceUrl,
			@Value("${order.request.timeout-ms}") int timeoutMs,
			@Value("${order.events.queue}") String queueName) {
		this.objectMapper = objectMapper;
		this.rabbitTemplate = rabbitTemplate;
		this.catalogServiceUrl = catalogServiceUrl;
		this.timeoutMs = timeoutMs;
		this.queueName = queueName;
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofMillis(timeoutMs))
			.build();
	}

	public ResponseEntity<?> processOrder(String correlationId) {
		try {
			LOGGER.info("Calling catalog-service");
			LOGGER.info("Calling catalog-service with X-Correlation-Id: {}", correlationId);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(catalogServiceUrl + "/catalog/products/1"))
				.timeout(Duration.ofMillis(timeoutMs))
				.header("X-Correlation-Id", correlationId)
				.GET()
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			CatalogProduct product = objectMapper.readValue(response.body(), CatalogProduct.class);

			LOGGER.info("Product retrieved successfully");
			LOGGER.info("Publishing event to RabbitMQ");
			LOGGER.info("X-Correlation-Id: {}", correlationId);

			OrderEvent event = new OrderEvent(correlationId, "Order created", product.getId());
			String eventPayload = objectMapper.writeValueAsString(event);
			rabbitTemplate.convertAndSend("", queueName, eventPayload, message -> {
				message.getMessageProperties().setHeader("X-Correlation-Id", correlationId);
				return message;
			});

			return ResponseEntity.ok(new OrderResponse("SUCCESS", correlationId, product, "Order created successfully"));
		} catch (HttpTimeoutException timeoutException) {
			LOGGER.warn("Timeout after {}ms", timeoutMs);
			LOGGER.warn("Catalog unavailable");
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new OrderResponse("CATALOG_UNAVAILABLE", correlationId, null, "Catalog unavailable"));
		} catch (ConnectException connectException) {
			LOGGER.warn("Timeout after {}ms", timeoutMs);
			LOGGER.warn("Catalog unavailable");
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new OrderResponse("CATALOG_UNAVAILABLE", correlationId, null, "Catalog unavailable"));
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			LOGGER.warn("Catalog unavailable", interruptedException);
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new OrderResponse("CATALOG_UNAVAILABLE", correlationId, null, "Catalog unavailable"));
		} catch (Exception exception) {
			LOGGER.error("Unexpected error while processing order", exception);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new OrderResponse("INTERNAL_ERROR", correlationId, null, "Unexpected error"));
		}
	}
}