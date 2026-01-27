# Oudekenis Marketplace - Codebase Summary

This document provides a high-level overview of the project structure and the responsibilities of each component.

## Project Structure (src/main/java/com/marketplace)

The application is structured into the following main packages:

### 1. `config`
Contains configuration classes for various integrations and security:
*   `SecurityConfig`: Spring Security configuration, defining access rules and authentication filters.
*   `ElasticsearchConfig` & `RedisConfig`: Connection and client beans for search and caching.
*   `S3Config`: AWS SDK configuration for image storage.
*   `DataSeeder`: Populates the database with initial categories and test data.

### 2. `user`
Handles user-related functionality:
*   `User`: JPA entity for user data (username, email, roles, etc.).
*   `UserService`: Business logic for registration, authentication, and profile management.
*   `UserRepository`: Data access for User entities.
*   `AuthController` & `AuthApiController`: Controllers for handling web and REST authentication.

### 3. `product`
Core marketplace features (listings, categories, images):
*   `Product`: Main entity for items listed on the marketplace.
*   `Category`: Hierarchical classification for products.
*   `ProductImage`: Metadata for images stored in S3/LocalStack.
*   `ProductService`: Managing creation, updates, and indexing of products.
*   `ProductController`: Web views for listing and detailing products.
*   `ProductRestController`: REST API for product operations.

### 4. `search`
Elasticsearch integration for high-performance search:
*   `ProductDocument`: Dedicated model for Elasticsearch indexing.
*   `ProductSearchService`: Logic for performing complex searches and filters against ES.
*   `ProductSearchRepository`: Spring Data Elasticsearch interface.

### 5. `service` & `storage`
General purpose services:
*   `S3Service`: Wrapper for AWS S3 interactions.
*   `ImageService`: Logic for image compression and optimization.
*   `MessageService`: Management of user-to-user chat messages.

### 6. `model`
Shared or simple domain models (e.g., `ChatMessage`).

---

## Technical Stack
*   **Backend**: Spring Boot 3.x, Spring Data JPA, Spring Security.
*   **Database**: MySQL (relational), Elasticsearch (search), Redis (caching).
*   **Frontend**: Thymeleaf, TailwindCSS, Alpine.js.
*   **Storage**: AWS S3 (via LocalStack in development).
