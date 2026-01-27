package com.marketplace.config;

import com.marketplace.product.*;
import com.marketplace.search.ProductSearchService;
import com.marketplace.user.Role;
import com.marketplace.user.User;
import com.marketplace.user.UserRepository;
import com.marketplace.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSearchService productSearchService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting Data Seeding...");
        seedUsers();
        seedCategories();
        seedProducts();
        log.info("Data Seeding Completed.");
    }

    private void seedUsers() {
        log.info("Seeding Users...");
        if (userRepository.findByUsername("seller").isEmpty()) {
            User seller = User.builder()
                    .username("seller")
                    .email("seller@example.com")
                    .passwordHash("password")
                    .firstName("John")
                    .lastName("Seller")
                    .role(Role.SELLER)
                    .build();
            userService.registerUser(seller);
        }

        if (userRepository.findByUsername("buyer").isEmpty()) {
            User buyer = User.builder()
                    .username("buyer")
                    .email("buyer@example.com")
                    .passwordHash("password")
                    .firstName("Jane")
                    .lastName("Buyer")
                    .role(Role.BUYER)
                    .build();
            userService.registerUser(buyer);
        }

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .passwordHash("admin123")
                    .firstName("Admin")
                    .lastName("User")
                    .role(Role.ADMIN)
                    .build();
            userService.registerUser(admin);
            log.info("Admin user created: admin / admin123");
        }
    }

    private void seedCategories() {
        log.info("Seeding Categories...");
        createCategoryIfNotFound("Smartphones & Tablets", "smartphones-tablets");
        createCategoryIfNotFound("Computers & Accessories", "computers-accessories");
        createCategoryIfNotFound("Video Games & Consoles", "video-games-consoles");
        createCategoryIfNotFound("Cars & Vehicles", "cars-vehicles");
        createCategoryIfNotFound("Real Estate", "real-estate");
        createCategoryIfNotFound("Furniture & Decor", "furniture-decor");
        createCategoryIfNotFound("Kitchen Appliances", "kitchen-appliances");
        createCategoryIfNotFound("Men's Fashion", "mens-fashion");
        createCategoryIfNotFound("Women's Fashion", "womens-fashion");
        createCategoryIfNotFound("Sports & Fitness", "sports-fitness");
        createCategoryIfNotFound("Books & Media", "books-media");
        createCategoryIfNotFound("Toys & Hobbies", "toys-hobbies");
        createCategoryIfNotFound("Health & Beauty", "health-beauty");
        createCategoryIfNotFound("Tools & DIY", "tools-diy");
        createCategoryIfNotFound("Jobs & Services", "jobs-services");
    }

    private Category createCategoryIfNotFound(String name, String slug) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(Category.builder().name(name).slug(slug).build()));
    }

    private void seedProducts() {
        log.info("Seeding Products...");
        if (productRepository.count() > 0) {
            log.info("Products already exist. Skipping product seeding.");
            return;
        }

        User seller = userRepository.findByUsername("seller").orElseThrow();
        Category smartphones = categoryRepository.findByName("Smartphones & Tablets").orElseThrow();
        Category computers = categoryRepository.findByName("Computers & Accessories").orElseThrow();
        Category fashion = categoryRepository.findByName("Men's Fashion").orElseThrow();
        Category home = categoryRepository.findByName("Furniture & Decor").orElseThrow();
        Category vehicles = categoryRepository.findByName("Cars & Vehicles").orElseThrow();

        createProduct(seller, smartphones, "iPhone 14 Pro",
                "Barely used iPhone 14 Pro, 256GB, Deep Purple. Comes with box and charger.",
                new BigDecimal("850.00"), Product.Condition.USED_LIKE_NEW, "Algiers",
                "https://images.unsplash.com/photo-1678685888221-cda773a3dcdb?auto=format&fit=crop&q=80&w=800");

        createProduct(seller, computers, "MacBook Air M2",
                "Apple MacBook Air M2 chip, 8GB RAM, 256GB SSD. Midnight color.",
                new BigDecimal("950.00"), Product.Condition.NEW, "Oran",
                "https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?auto=format&fit=crop&q=80&w=800");

        createProduct(seller, fashion, "Vintage Denim Jacket", "Classic styling, size L. Great condition.",
                new BigDecimal("45.00"), Product.Condition.USED_GOOD, "Constantine",
                "https://images.unsplash.com/photo-1576871337622-98d48d1cf531?auto=format&fit=crop&q=80&w=800");

        createProduct(seller, home, "Modern Sofa", "Grey fabric sofa, 3-seater. Very comfortable.",
                new BigDecimal("300.00"), Product.Condition.USED_GOOD, "Algiers",
                "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&q=80&w=800");

        createProduct(seller, vehicles, "Mountain Bike", "Trek Marlin 7, freshly serviced.",
                new BigDecimal("400.00"), Product.Condition.USED_GOOD, "Setif",
                "https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?auto=format&fit=crop&q=80&w=800");
    }

    private void createProduct(User seller, Category category, String title, String desc, BigDecimal price,
            Product.Condition condition, String location, String imageUrl) {
        try {
            Product product = Product.builder()
                    .title(title)
                    .description(desc)
                    .price(price)
                    .condition(condition)
                    .category(category)
                    .seller(seller)
                    .location(location)
                    .status(Product.ProductStatus.ACTIVE)
                    .build();

            product = productRepository.save(product);

            ProductImage image = ProductImage.builder()
                    .product(product)
                    .imageUrl(imageUrl)
                    .isPrimary(true)
                    .order(0)
                    .build();
            image = productImageRepository.save(image);

            // Add to product's list so indexer sees it
            if (product.getImages() == null)
                product.setImages(new java.util.ArrayList<>());
            product.getImages().add(image);

            try {
                productSearchService.indexProduct(product);
            } catch (Exception e) {
                log.warn("Failed to index product in Elasticsearch during seeding: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error creating product '{}': {}", title, e.getMessage());
        }
    }
}
