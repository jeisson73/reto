package com.gestionpedidos.catalog_service.service;

import com.gestionpedidos.catalog_service.model.Product;
import com.gestionpedidos.catalog_service.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Product createProduct(Product product){
        return repository.save(product);
    }

    public List<Product> getProducts(){
        return repository.findAll();
    }

    public Product getProduct(Long id){
        return repository.findById(id).orElseThrow();
    }

    public Product updateProduct(Long id, Product product){
        Product existing = repository.findById(id).orElseThrow();
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setStock(product.getStock());
        return repository.save(existing);
    }
}