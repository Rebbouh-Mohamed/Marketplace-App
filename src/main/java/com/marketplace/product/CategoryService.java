package com.marketplace.product;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable("categories")
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }

    @Transactional
    public Category createCategory(Category category) {
        if (category.getParent() != null && category.getParent().getId() != null) {
            Category parent = categoryRepository.findById(category.getParent().getId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParent(parent);
        }
        return categoryRepository.save(category);
    }
}
