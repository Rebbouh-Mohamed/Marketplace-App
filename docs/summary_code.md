# Oudekenis Marketplace - Codebase Summary

This document provides a high-level overview of the project structure and the responsibilities of each component.

## Project Structure (src/main/java/com/marketplace)

The application is structured into the following main packages:

### 1. `config`
Contains configuration classes for various integrations and security:
*   `SecurityConfig`: Spring Security configuration, defining access rules and authentication filters.
*   `WebSocketConfig`: Configuration for real-time messaging using STOMP over WebSocket.
*   `ElasticsearchConfig`: Connection and client beans for search.
*   `S3Config`: AWS SDK configuration for image storage.
*   `DataSeeder`: Populates the database with initial categories and test data.

### 2. `user`
Handles user-related functionality:
*   `User`: JPA entity for user data.
*   `UserService`: Business logic for registration, authentication, and profile management.
*   `UserController`: Handles user profile views and updates.
*   `AuthController`: Controllers for handling authentication routes.

### 3. `product`
Core marketplace features (listings, categories, images):
*   `Product`: Main entity for items listed on the marketplace.
*   `Category`: Hierarchical classification for products.
*   `ProductImage`: Metadata for images stored in S3/LocalStack.
*   `ProductService`: Managing creation, updates, and indexing of products.

### 4. `chat` (NEW)
Comprehensive real-time messaging module:
*   `model`: `ChatRoom` and `ChatMessage` entities.
*   `service`: `ChatRoomService` and `ChatMessageService`.
*   `controller`: `ChatPageController` (UI), `ChatWebSocketController` (WS), and `ChatRestController` (API).
*   `dto`: Data Transfer Objects for messaging.

### 5. `search`
Elasticsearch integration for high-performance search:
*   `ProductDocument`: Dedicated model for Elasticsearch indexing.
*   `ProductSearchService`: Logic for performing complex searches.

### 6. `service` & `storage`
General purpose services:
*   `S3Service`: Wrapper for AWS S3 interactions.
*   `ImageService`: Logic for image compression and optimization.

---

## Technical Stack
*   **Backend**: Spring Boot 3.x, Spring Data JPA, Spring Security.
*   **Real-Time**: STOMP over WebSocket with SockJS.
*   **Database**: MySQL 8.0, Elasticsearch (search).
*   **Frontend**: Thymeleaf, TailwindCSS, Alpine.js (standalone chat rooms).
*   **Storage**: AWS S3 (via LocalStack in development).
