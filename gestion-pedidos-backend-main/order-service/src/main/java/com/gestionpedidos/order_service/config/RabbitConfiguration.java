package com.gestionpedidos.order_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

	@Bean
	public Queue orderEventsQueue(@Value("${order.events.queue}") String queueName) {
		return new Queue(queueName, true);
	}
}