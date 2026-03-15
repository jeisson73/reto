package com.gestionpedidos.api_gateway.filter;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.UUID;

@Component("gatewayRequestContextFilter")
public class RequestContextFilter extends OncePerRequestFilter {

	public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
	public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

	private final Key jwtKey;

	public RequestContextFilter(@Value("${jwt.secret}") String secret) {
		this.jwtKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String correlationId = request.getHeader(CORRELATION_ID_HEADER);
		if (correlationId == null || correlationId.isBlank()) {
			correlationId = UUID.randomUUID().toString();
		}

		request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
		response.setHeader(CORRELATION_ID_HEADER, correlationId);

		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			try {
				Jwts.parserBuilder().setSigningKey(jwtKey).build().parseClaimsJws(token);
			} catch (JwtException exception) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
				return;
			}
		}

		filterChain.doFilter(request, response);
	}
}