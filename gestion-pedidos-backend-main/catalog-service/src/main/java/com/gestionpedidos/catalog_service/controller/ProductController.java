package com.gestionpedidos.catalog_service.controller;

import com.gestionpedidos.catalog_service.model.Product;
import com.gestionpedidos.catalog_service.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/products", "/catalog/products"})
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    public Product create(@RequestBody Product product){
        return service.createProduct(product);
    }

    @GetMapping
    public List<Product> getAll(){
        return service.getProducts();
    }

    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id){
        return service.getProduct(id);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product product){
        return service.updateProduct(id, product);
    }
}