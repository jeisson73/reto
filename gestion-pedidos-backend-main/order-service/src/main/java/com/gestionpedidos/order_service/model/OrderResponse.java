package com.gestionpedidos.order_service.model;

public record OrderResponse(String status, String correlationId, CatalogProduct product, String message) {
}