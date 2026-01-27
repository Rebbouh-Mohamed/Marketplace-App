package com.marketplace.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {
        @Query("SELECT pl FROM ProductLike pl WHERE pl.user.id = :userId AND pl.product.id = :productId")
        Optional<ProductLike> findByUserIdAndProductId(
                        @org.springframework.data.repository.query.Param("userId") Long userId,
                        @org.springframework.data.repository.query.Param("productId") Long productId);

        @Query("SELECT COUNT(pl) > 0 FROM ProductLike pl WHERE pl.user.id = :userId AND pl.product.id = :productId")
        boolean existsByUserIdAndProductId(@org.springframework.data.repository.query.Param("userId") Long userId,
                        @org.springframework.data.repository.query.Param("productId") Long productId);

        @Query("SELECT COUNT(pl) FROM ProductLike pl WHERE pl.product.id = :productId")
        long countByProductId(@org.springframework.data.repository.query.Param("productId") Long productId);

        @org.springframework.transaction.annotation.Transactional
        @org.springframework.data.jpa.repository.Modifying
        @Query("DELETE FROM ProductLike pl WHERE pl.user.id = :userId AND pl.product.id = :productId")
        void deleteByUserIdAndProductId(@org.springframework.data.repository.query.Param("userId") Long userId,
                        @org.springframework.data.repository.query.Param("productId") Long productId);

        @Query("SELECT pl FROM ProductLike pl WHERE pl.user.id = :userId")
        java.util.List<ProductLike> findByUserId(
                        @org.springframework.data.repository.query.Param("userId") Long userId);
}
