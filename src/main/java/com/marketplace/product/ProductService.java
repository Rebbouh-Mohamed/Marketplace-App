package com.marketplace.product;

import com.marketplace.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final TagRepository tagRepository;
    private final com.marketplace.search.ProductSearchService productSearchService;

    // @org.springframework.cache.annotation.Cacheable(value = "products", key =
    // "#pageable.pageNumber")
    // @org.springframework.cache.annotation.Cacheable(value = "products", key =
    // "#pageable.pageNumber")
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
    }

    // @org.springframework.cache.annotation.Cacheable(value = "product", key =
    // "#id")
    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        return productRepository.findByIdWithDetails(id);
    }

    @Transactional
    public Product updateProductDetails(Long id, Product input, Long categoryId, String tagsInput, User user) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getSeller().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new RuntimeException("Unauthorized");
        }

        product.setTitle(input.getTitle());
        product.setDescription(input.getDescription());
        product.setPrice(input.getPrice());
        product.setLocation(input.getLocation());
        product.setCondition(input.getCondition());

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found for ID: " + categoryId));
            product.setCategory(category);
        }

        if (tagsInput != null) {
            product.getTags().clear(); // simplistic tag update
            for (String name : tagsInput.split(",")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    Tag tag = tagRepository.findByName(trimmed)
                            .orElseGet(() -> tagRepository.save(Tag.builder().name(trimmed).build()));
                    product.getTags().add(tag);
                }
            }
        }

        productRepository.save(product);
        productSearchService.indexProduct(product);
        return product;
    }

    public Product createProduct(Product product, Long categoryId, User seller, String tagsInput) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        product.setCategory(category);
        product.setSeller(seller);
        product.setStatus(Product.ProductStatus.ACTIVE);

        if (tagsInput != null && !tagsInput.isEmpty()) {
            String[] tagNames = tagsInput.split(",");
            for (String name : tagNames) {
                String trimmedName = name.trim();
                if (!trimmedName.isEmpty()) {
                    Tag tag = tagRepository.findByName(trimmedName)
                            .orElseGet(() -> tagRepository.save(Tag.builder().name(trimmedName).build()));
                    product.getTags().add(tag);
                }
            }
        }

        productRepository.save(product);
        productSearchService.indexProduct(product);
        return product;
    }

    public Page<Product> getAdminProducts(String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return productRepository.findByStatus(Product.ProductStatus.valueOf(status), pageable);
        }
        return productRepository.findAll(pageable); // Admin sees ALL, including deleted/pending
    }

    @Transactional
    public void updateProductStatus(Long id, Product.ProductStatus status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStatus(status);
        productRepository.save(product);

        // Update index if active, remove if not
        if (status == Product.ProductStatus.ACTIVE) {
            productSearchService.indexProduct(product);
        } else {
            productSearchService.deleteProduct(product.getId());
        }
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        productSearchService.deleteProduct(id);
    }

    public long countProducts() {
        return productRepository.count();
    }

    @Transactional
    public void addImages(Product product, List<String> imageUrls) {
        if (product.getImages() == null) {
            product.setImages(new java.util.ArrayList<>());
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage image = ProductImage.builder()
                    .product(product)
                    .imageUrl(imageUrls.get(i))
                    .isPrimary(i == 0)
                    .order(i)
                    .build();
            image = productImageRepository.save(image);
            product.getImages().add(image);
        }

        // Update search index with the new images
        productSearchService.indexProduct(product);
    }
}
