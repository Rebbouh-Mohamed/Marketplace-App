package com.marketplace.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategory(Category category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.title LIKE %:keyword% OR p.description LIKE %:keyword%")
    Page<Product> search(@org.springframework.data.repository.query.Param("keyword") String keyword, Pageable pageable);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category c LEFT JOIN FETCH c.parent LEFT JOIN FETCH p.seller LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithDetails(@org.springframework.data.repository.query.Param("id") Long id);

    Page<Product> findByStatus(Product.ProductStatus status, Pageable pageable);
}
