package com.gestionpedidos.order_service.model;

public record OrderEvent(String correlationId, String message, Long productId) {
}