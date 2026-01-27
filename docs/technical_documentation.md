# Oudekenis Marketplace - Technical Documentation

## 1. System Overview

**Oudekenis** is a full-stack e-commerce marketplace application built with Spring Boot. It serves as a platform for users to buy and sell pre-loved items, similar to Ouedkniss. The application provides a seamless user experience for listing products, searching with advanced filters, real-time messaging, and managing user profiles.

### Key Features
*   **User Management**: Secure registration, authentication, and profile management with avatar support.
*   **Product Listings**: Users can create detailed product listings with multi-image uploads (stored in S3/LocalStack) and rich descriptions.
*   **Advanced Search**: High-performance search and filtering (Category, Price, Condition) powered by Elasticsearch.
*   **Interactive UI**: Responsive frontend built with Thymeleaf, TailwindCSS, and Alpine.js, featuring Dark Mode support.
*   **Messaging System**: Integrated chat functionality allowing buyers and sellers to communicate directly about listings.
*   **Wishlist**: Ability for users to "like" and save products for later.

## 2. System Architecture

The system follows a classic **Layered Architecture** pattern, ensuring separation of concerns and maintainability.

```mermaid
graph TB
    subgraph "Presentation Layer"
        Browser[Web Browser / Mobile]
        Thymeleaf[Thymeleaf Templates]
        Alpine[Alpine.js Interactivity]
        Tailwind[TailwindCSS Styling]
    end

    subgraph "API / Web Layer"
        Controllers[REST Controllers]
        Security[Spring Security Filter Chain]
    end

    subgraph "Service Layer"
        BusinessLogic[Service Classes]
        Validation[Data Validation]
        EventHandling[Event Listeners]
    end

    subgraph "Data Access Layer"
        JPA[Spring Data JPA Repositories]
        ES_Repo[Spring Data Elasticsearch Repos]
        S3_Client[AWS S3 Client]
    end

    subgraph "Infrastructure / Storage"
        MySQL[(MySQL Release DB)]
        Elasticsearch[(Elasticsearch Index)]
        S3[(AWS S3 / LocalStack)]
    end

    Browser -->|HTTP/HTTPS| Security
    Security -->|Authorized Requests| Controllers
    Controllers -->|Model & View| Thymeleaf
    Thymeleaf -->|Rendered HTML| Browser
    
    Controllers -->|DTOs| BusinessLogic
    BusinessLogic -->|Entities| JPA
    BusinessLogic -->|Search Operations| ES_Repo
    BusinessLogic -->|File Streams| S3_Client
    
    JPA -->|SQL| MySQL
    ES_Repo -->|JSON| Elasticsearch
    S3_Client -->|Binary| S3
```

## 3. System Flow Diagrams

### 3.1 User Authentication Flow
```mermaid
sequenceDiagram
    participant User
    participant Controller as AuthController
    participant Service as UserService
    participant DB as User Repository
    
    User->>Controller: POST /register (data)
    Controller->>Service: createUser(dto)
    Service->>DB: existsByEmail?
    alt Email Exists
        DB-->>Service: true
        Service-->>Controller: Error
        Controller-->>User: Registration Failed
    else Email Unique
        Service->>Service: encryptPassword()
        Service->>DB: save(User)
        DB-->>Service: User Entity
        Service-->>Controller: Success
        Controller-->>User: Redirect to Login
    end
```

### 3.2 Product Listing & Indexing Flow
This flow demonstrates the dual-write consistency between the Database and Search Index.

```mermaid
sequenceDiagram
    participant User
    participant Controller as ProductController
    participant Service as ProductService
    participant S3 as S3Storage
    participant DB as MySQL
    participant ES as SearchService (ES)

    User->>Controller: POST /products/new (FormData + Images)
    Controller->>Service: createProduct(details)
    Service->>DB: save(Product)
    DB-->>Service: Product ID
    
    Controller->>Service: addImages(product, images)
    loop For Each Image
        Service->>S3: uploadFile()
        S3-->>Service: S3 Key / URL
        Service->>DB: save(ProductImage)
    end
    
    Service->>Service: refreshProductDetails()
    Service->>ES: indexProduct(Product)
    ES->>ES: Update Elasticsearch Index
    Service-->>Controller: Success
    Controller-->>User: Redirect to Product Page
```

### 3.3 Search & Filtering Flow
```mermaid
graph LR
    User[User Input] -->|Query Parameters| Controller[ProductController]
    Controller -->|Criteria| SearchService[ProductSearchService]
    
    subgraph "Search Logic"
        SearchService -->|Check| Cache{In Cache?}
        Cache -->|No| ES[Elasticsearch]
        ES -->|Return IDs| SearchService
    end
    
    SearchService -->|Product Documents| Controller
    Controller -->|Page<Product>| View[List View]
```

## 4. Component Architecture

The application is structured logically by feature (Domain-Driven Design influence).

```mermaid
classDiagram
    class ProductController {
        +listProducts()
        +createProduct()
        +showDetail()
    }
    
    class ProductService {
        +create()
        +addImages()
        +findById()
    }
    
    class ProductSearchService {
        +indexProduct()
        +searchProductsDetailed()
    }
    
    class ImageService {
        +uploadImage()
        +optimize()
    }
    
    class ProductRepository {
        <<Interface>>
        +findByIdWithDetails()
    }

    class ProductSearchRepository {
        <<Interface>>
        +findByTitleOrDescription()
    }

    ProductController --> ProductService
    ProductController --> ProductSearchService
    ProductService --> ProductRepository
    ProductService --> ProductSearchService
    ProductService ..> ImageService
    ProductSearchService --> ProductSearchRepository
```

## 5. Deployment Architecture

The system is designed to be containerized and deployed in a distributed environment.

```mermaid
graph TD
    UserClient[User Device]
    
    subgraph "Docker Host / Cloud Cluster"
        LB[Load Balancer / Nginx]
        
        subgraph "Application Container"
            SpringBoot[Spring Boot App]
            Tomcat[Embedded Tomcat:8080]
        end
        
        subgraph "Data Layer"
            MySQL_C[MySQL Container:3306]
            ES_C[Elasticsearch Container:9200]
            LocalStack[LocalStack S3:4566]
        end
    end
    
    UserClient -->|HTTPS| LB
    LB -->|Proxy| Tomcat
    SpringBoot -->|JDBC| MySQL_C
    SpringBoot -->|REST/HTTP| ES_C
    SpringBoot -->|AWS SDK| LocalStack
```
