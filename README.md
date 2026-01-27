# Marketplace Application

A sophisticated marketplace web application built with Spring Boot, serving as a platform for buyers and sellers similar to Ouedkniss.

## Technology Stack
- **Backend**: Spring Boot 3.3, Spring Modulith, Spring Data JPA, Spring Security
- **Database**: MySQL 8.0
- **Search**: Elasticsearch 8.x
- **Cache**: Redis
- **Storage**: LocalStack (S3)
- **Frontend**: ThyemeLeaf, Tailwind CSS, Alpine.js (implied)
- **Real-time**: WebSocket (STOMP)

## Prerequisites
- Java 17+
- Docker & Docker Compose

## Quick Start
1.  **Start Infrastructure**:
    ```bash
    docker-compose up -d
    ```
2.  **Build Application**:
    ```bash
    ./mvnw clean compile
    ```
3.  **Run Application**:
    ```bash
    ./mvnw spring-boot:run
    ```
4.  Access the app at `http://localhost:8080`.

## Features
- **User Management**: Registration, Login, RBAC (Buyer/Seller/Admin).
- **Product Management**: Create, Edit, View Products with Images (S3).
- **Search**: Full-text search with Elasticsearch.
- **Categories**: Hierarchical category system.
- **Engagement**: Product Likes, Simple Tagging.
- **Chat**: Real-time messaging between users.
- **Admin**: Basic dashboard for metrics.

## Architecture
The application follows a Modular Monolith architecture enforced by Spring Modulith.
- `com.marketplace.user`: User & Auth
- `com.marketplace.product`: Catalog & Engagement
- `com.marketplace.search`: Search indexing
- `com.marketplace.messaging`: Chat
- `com.marketplace.storage`: File handling
