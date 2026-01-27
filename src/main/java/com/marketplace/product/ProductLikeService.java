package com.marketplace.product;

import com.marketplace.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductLikeService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;

    /**
     * Check if user already liked a product
     */
    public boolean isLiked(Long userId, Long productId) {
        return productLikeRepository.existsByUserIdAndProductId(userId, productId);
    }

    /**
     * Add a like
     */
    @Transactional
    public void addLike(User user, Long productId) {
        if (isLiked(user.getId(), productId)) {
            return;
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductLike like = ProductLike.builder()
                .user(user)
                .product(product)
                .build();

        productLikeRepository.save(like);
        product.setLikeCount(product.getLikeCount() + 1);
        productRepository.save(product);
    }

    /**
     * Remove a like (unlike)
     */
    @Transactional
    public void removeLike(Long userId, Long productId) {
        productLikeRepository.deleteByUserIdAndProductId(userId, productId);

        productRepository.findById(productId).ifPresent(product -> {
            product.setLikeCount(Math.max(0, product.getLikeCount() - 1));
            productRepository.save(product);
        });
    }

    /**
     * Get all product IDs liked by user (for checking button state)
     */
    public java.util.List<Long> getLikedProductIds(Long userId) {
        return productLikeRepository.findByUserId(userId)
                .stream()
                .map(pl -> pl.getProduct().getId())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Toggle like - returns "liked" or "unliked"
     */
    @Transactional
    public String toggleLike(User user, Long productId) {
        if (isLiked(user.getId(), productId)) {
            removeLike(user.getId(), productId);
            return "unliked";
        } else {
            addLike(user, productId);
            return "liked";
        }
    }
}
