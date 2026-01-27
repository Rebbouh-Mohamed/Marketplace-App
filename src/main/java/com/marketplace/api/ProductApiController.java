package com.marketplace.api;

import com.marketplace.product.Product;
import com.marketplace.product.ProductService;
import com.marketplace.search.ProductDocument;
import com.marketplace.search.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductApiController {

    private final ProductService productService;
    private final ProductSearchService productSearchService;

    @GetMapping
    public ResponseEntity<Page<Product>> getAllProducts(@RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(productService.findAll(PageRequest.of(page, 10)));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductDocument>> searchProducts(@RequestParam String q,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(productSearchService.searchProducts(q, PageRequest.of(page, 10)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
