package com.marketplace.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
    Page<ProductDocument> findByTitleOrDescription(String title, String description, Pageable pageable);
}
