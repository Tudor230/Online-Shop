package org.endava.onlineshop.repository;

import org.endava.onlineshop.model.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByName(String name);
    List<Product> findByPriceBetween(Double priceStart, Double priceEnd);
    List<Product> findByPriceLessThan(Double price);
    List<Product> findByPriceGreaterThan(Double price);
}
