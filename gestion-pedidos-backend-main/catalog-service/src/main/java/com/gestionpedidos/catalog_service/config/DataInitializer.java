package com.gestionpedidos.catalog_service.config;

import com.gestionpedidos.catalog_service.model.Product;
import com.gestionpedidos.catalog_service.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

	@Bean
	public CommandLineRunner seedProducts(ProductRepository productRepository) {
		return args -> {
			if (productRepository.count() == 0) {
				Product product = new Product();
				product.setName("Producto Demo");
				product.setDescription("Producto base para la demostracion de Orders -> Catalog");
				product.setPrice(49.99);
				product.setStock(25);
				productRepository.save(product);
			}
		};
	}
}