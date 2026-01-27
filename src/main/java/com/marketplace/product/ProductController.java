package com.marketplace.product;

import com.marketplace.service.ImageService;
import com.marketplace.service.S3Service;
import com.marketplace.user.User;
import com.marketplace.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final S3Service s3Service;
    private final ImageService imageService;
    private final UserService userService;
    private final com.marketplace.search.ProductSearchService productSearchService;
    private final ProductLikeRepository productLikeRepository;

    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) String condition,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        Page<Product> products;

        boolean isUnfiltered = (q == null || q.trim().isEmpty()) && (category == null || category.trim().isEmpty())
                && minPrice == null && maxPrice == null && (condition == null || condition.trim().isEmpty());

        if (isUnfiltered) {
            products = productService.findAll(PageRequest.of(page, 12));
        } else {
            Page<com.marketplace.search.ProductDocument> docs = productSearchService.searchProductsDetailed(
                    q, category, minPrice, maxPrice, condition, PageRequest.of(page, 12));

            products = docs.map(doc -> {
                Product p = new Product();
                p.setId(doc.getId());
                p.setTitle(doc.getTitle());
                p.setPrice(doc.getPrice());

                Category c = new Category();
                c.setName(doc.getCategoryName());
                p.setCategory(c);

                if (doc.getImageUrl() != null) {
                    ProductImage img = new ProductImage();
                    img.setImageUrl(doc.getImageUrl());
                    p.setImages(java.util.Collections.singletonList(img));
                } else {
                    p.setImages(new java.util.ArrayList<>());
                }
                // CreatedAt is used in UI
                p.setCreatedAt(java.time.LocalDateTime.now());
                return p;
            });
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.findAll());

        // Preserve filter state
        model.addAttribute("q", q);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("condition", condition);

        if (userDetails != null) {
            userService.findByUsername(userDetails.getUsername()).ifPresent(user -> {
                Set<Long> likedIds = productLikeRepository.findByUserId(user.getId())
                        .stream().map(pl -> pl.getProduct().getId())
                        .collect(Collectors.toSet());
                model.addAttribute("likedProductIds", likedIds);
            });
        }

        return "products/list";
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String productDetail(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Product product = productService.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        model.addAttribute("product", product);

        if (userDetails != null) {
            userService.findByUsername(userDetails.getUsername()).ifPresent(user -> {
                java.util.Set<Long> likedIds = productLikeRepository.findByUserId(user.getId())
                        .stream().map(pl -> pl.getProduct().getId())
                        .collect(Collectors.toSet());
                model.addAttribute("likedProductIds", likedIds);
            });
        }

        return "products/detail";
    }

    @GetMapping("/new")
    public String createProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAll());
        return "products/form";
    }

    @PostMapping
    public String createProduct(@ModelAttribute Product product,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("imageFiles") List<MultipartFile> imageFiles,
            @RequestParam(value = "tagsInput", required = false) String tagsInput,
            @AuthenticationPrincipal UserDetails userDetails) {

        User seller = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product savedProduct = productService.createProduct(product, categoryId, seller, tagsInput);

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : imageFiles) {
            if (!file.isEmpty()) {
                try {
                    byte[] compressedImage = imageService.compressImage(file);
                    String s3Key = s3Service.uploadFile(compressedImage, file.getOriginalFilename());
                    imageUrls.add(s3Key);
                } catch (Exception e) {
                    e.printStackTrace(); // Log error properly in real app
                }
            }
        }
        productService.addImages(savedProduct, imageUrls);

        return "redirect:/products/" + savedProduct.getId();
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String editProductForm(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        Product product = productService.findById(id).orElseThrow();
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        if (!product.getSeller().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            return "redirect:/error"; // or 403 page
        }

        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.findAll());
        // Join tags for input
        String tags = product.getTags().stream().map(com.marketplace.product.Tag::getName)
                .collect(Collectors.joining(", "));
        model.addAttribute("tagsInput", tags);

        return "products/edit";
    }

    @PostMapping("/update/{id}")
    @PreAuthorize("isAuthenticated()")
    public String updateProduct(@PathVariable Long id, @ModelAttribute Product product,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "tagsInput", required = false) String tagsInput,
            @AuthenticationPrincipal UserDetails userDetails) {
        System.out.println("Updating Product ID: " + id + " with Category ID: " + categoryId);
        System.out.println("Available Categories:");
        categoryService.findAll().forEach(c -> System.out.println(" - ID: " + c.getId() + ", Name: " + c.getName()));

        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        productService.updateProductDetails(id, product, categoryId, tagsInput, user);
        return "redirect:/products/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deleteProduct(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        Product product = productService.findById(id).orElseThrow();

        if (product.getSeller().getId().equals(user.getId()) || user.getRole().name().equals("ADMIN")) {
            productService.updateProductStatus(id, Product.ProductStatus.DELETED);
        }
        return "redirect:/profile?tab=listings"; // Redirect back to profile
    }

    @PostMapping("/{id}/sold")
    @PreAuthorize("isAuthenticated()")
    public String markAsSold(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        Product product = productService.findById(id).orElseThrow();

        if (product.getSeller().getId().equals(user.getId())) {
            productService.updateProductStatus(id, Product.ProductStatus.SOLD);
        }
        return "redirect:/profile?tab=listings";
    }
}
