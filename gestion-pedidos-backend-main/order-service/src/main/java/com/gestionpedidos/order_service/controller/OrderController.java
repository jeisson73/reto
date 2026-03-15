package com.gestionpedidos.order_service.controller;

import com.gestionpedidos.order_service.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping("/orders")
	public ResponseEntity<?> getOrders(@RequestAttribute(name = "X-Correlation-Id", required = false) String correlationId) {
		return orderService.processOrder(correlationId);
	}
}