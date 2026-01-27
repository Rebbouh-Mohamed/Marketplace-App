package com.marketplace.search;

import com.marketplace.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final org.springframework.data.elasticsearch.core.ElasticsearchOperations elasticsearchOperations;

    public void indexProduct(Product product) {
        ProductDocument doc = ProductDocument.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .categoryName(product.getCategory().getName())
                .condition(product.getCondition().name())
                .location(product.getLocation())
                .imageUrl(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl())
                .build();
        productSearchRepository.save(doc);
    }

    public Page<ProductDocument> searchProducts(String query, Pageable pageable) {
        return productSearchRepository.findByTitleOrDescription(query, query, pageable);
    }

    public Page<ProductDocument> searchProductsDetailed(String query, String category, java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice, String condition, Pageable pageable) {
        org.springframework.data.elasticsearch.core.query.Criteria criteria = new org.springframework.data.elasticsearch.core.query.Criteria();

        // If 'query' is provided, look in title or description
        if (query != null && !query.trim().isEmpty()) {
            criteria = new org.springframework.data.elasticsearch.core.query.Criteria("title").contains(query)
                    .or(new org.springframework.data.elasticsearch.core.query.Criteria("description").contains(query));
        }

        // Apply filters
        if (category != null && !category.trim().isEmpty()) {
            criteria = criteria
                    .and(new org.springframework.data.elasticsearch.core.query.Criteria("categoryName").is(category));
        }

        if (minPrice != null) {
            criteria = criteria.and(
                    new org.springframework.data.elasticsearch.core.query.Criteria("price").greaterThanEqual(minPrice));
        }

        if (maxPrice != null) {
            criteria = criteria.and(
                    new org.springframework.data.elasticsearch.core.query.Criteria("price").lessThanEqual(maxPrice));
        }

        if (condition != null && !condition.trim().isEmpty()) {
            criteria = criteria
                    .and(new org.springframework.data.elasticsearch.core.query.Criteria("condition").is(condition));
        }

        org.springframework.data.elasticsearch.core.query.Query searchQuery = new org.springframework.data.elasticsearch.core.query.CriteriaQuery(
                criteria).setPageable(pageable);
        org.springframework.data.elasticsearch.core.SearchHits<ProductDocument> searchHits = elasticsearchOperations
                .search(searchQuery, ProductDocument.class);

        java.util.List<ProductDocument> list = searchHits.stream()
                .map(org.springframework.data.elasticsearch.core.SearchHit::getContent)
                .collect(java.util.stream.Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(list, pageable, searchHits.getTotalHits());
    }

    public void deleteProductIndex(Long id) {
        productSearchRepository.deleteById(id);
    }

    public void deleteProduct(Long id) {
        productSearchRepository.deleteById(id);
    }
}
