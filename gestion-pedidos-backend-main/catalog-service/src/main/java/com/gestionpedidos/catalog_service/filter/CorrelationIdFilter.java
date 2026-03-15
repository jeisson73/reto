package com.gestionpedidos.catalog_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);
	public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String correlationId = request.getHeader(CORRELATION_ID_HEADER);
		if (correlationId == null || correlationId.isBlank()) {
			correlationId = UUID.randomUUID().toString();
		}

		request.setAttribute(CORRELATION_ID_HEADER, correlationId);
		response.setHeader(CORRELATION_ID_HEADER, correlationId);
		LOGGER.info("Request received with X-Correlation-Id: {}", correlationId);
		filterChain.doFilter(request, response);
	}
}