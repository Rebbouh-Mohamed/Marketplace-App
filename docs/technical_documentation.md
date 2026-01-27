# Oudekenis Marketplace - Technical Documentation

## 1. System Overview

**Oudekenis** is a full-stack e-commerce marketplace application built with Spring Boot. It serves as a platform for users to buy and sell pre-loved items, similar to Ouedkniss. The application provides a seamless user experience for listing products, searching with advanced filters, real-time messaging, and managing user profiles.

### Key Features
*   **User Management**: Secure registration, authentication, and profile management.
*   **Product Listings**: Detailed listings with multi-image uploads (S3/LocalStack storage).
*   **Advanced Search**: High-performance filtering powered by Elasticsearch.
*   **Integrated UI**: Responsive frontend with Thymeleaf, TailwindCSS, and Alpine.js.
*   **Real-Time Chat**: Innovative WebSocket-powered chat tied directly to product listings.

## 2. System Architecture

The system follows a **Layered Architecture** with a clear modular boundary for the Chat system.

```mermaid
graph TB
    subgraph "Presentation Layer"
        Browser[Web Browser / Mobile]
        Thymeleaf[Thymeleaf Templates]
        Alpine[Alpine.js / JavaScript]
        WebSocketClient[SockJS / STOMP Client]
    end

    subgraph "API / Web Layer"
        PageControllers[Web Page Controllers]
        WebSocketController[STOMP Message Handlers]
        RestControllers[REST Controllers]
        Security[Spring Security / WebSocket Handshake]
    end

    subgraph "Service Layer"
        ProductService[Product & Image Management]
        ChatService[Chat Room & Message Persistence]
        SearchService[Elasticsearch Coordination]
        UserService[Identity & Access]
    end

    subgraph "Data Store"
        MySQL[(MySQL Release DB)]
        Elasticsearch[(Elasticsearch Index)]
        S3[(AWS S3 / LocalStack)]
    end

    Browser -->|HTTPS| Security
    WebSocketClient -->|STOMP/WS| Security
    Security -->|Authorized| PageControllers
    Security -->|Authorized| WebSocketController
    
    WebSocketController --> ChatService
    PageControllers --> ProductService
    PageControllers --> ChatService
    
    ChatService --> MySQL
    ProductService --> MySQL
    ProductService --> S3
    ProductService --> SearchService
    
    SearchService --> Elasticsearch
```

## 3. System Flow Diagrams

### 3.1 Real-Time Messaging Flow (WebSocket)
```mermaid
sequenceDiagram
    participant Buyer as User (Tab A)
    participant Server as Spring Boot (STOMP)
    participant DB as MySQL Database
    participant Seller as User (Tab B)

    Note over Buyer, Seller: Subscription to /topic/chatroom/{id}
    
    Buyer->>Server: SEND /app/chat.send/{id}
    activate Server
    Server->>DB: saveMessage(entity)
    Server->>Server: updateChatRoomMetadata()
    Server-->>Buyer: BROADCAST /topic/chatroom/{id} (MessageDTO)
    Server-->>Seller: BROADCAST /topic/chatroom/{id} (MessageDTO)
    deactivate Server
    
    Note over Buyer, Seller: Client automatically appends message to UI
```

### 3.2 Product Listing & Indexing Flow
```mermaid
sequenceDiagram
    participant User
    participant Controller as ProductController
    participant Service as ProductService
    participant S3 as S3Storage
    participant DB as MySQL
    participant ES as SearchService (ES)

    User->>Controller: POST /products/new
    Controller->>Service: createProduct(details)
    Service->>DB: save(Product)
    
    loop For Each Image
        Service->>S3: uploadFile()
        Service->>DB: save(ProductImage)
    end
    
    Service->>ES: indexProduct(Product)
    ES->>ES: Update Elasticsearch Index
    Service-->>Controller: Success
    Controller-->>User: Redirect to Details
```

### 3.3 User Registration Flow
```mermaid
sequenceDiagram
    participant User
    participant AuthController
    participant UserService
    participant UserRepository
    participant DB as MySQL

    User->>AuthController: GET /register
    AuthController-->>User: Registration Form
    
    User->>AuthController: POST /register (User Data)
    AuthController->>UserService: registerUser(User)
    UserService->>UserRepository: save(User)
    UserRepository->>DB: INSERT User
    UserService-->>AuthController: Success
    AuthController-->>User: Redirect /login?registered
```

### 3.4 User Login Flow (Spring Security)
```mermaid
sequenceDiagram
    participant User
    participant Security as Spring Security Filter
    participant Provider as DaoAuthInterceptor
    participant Service as CustomUserDetailsService
    participant DB as MySQL
    
    User->>Security: POST /login (Credentials)
    Security->>Provider: authenticate()
    Provider->>Service: loadUserByUsername()
    Service->>DB: findByUsername()
    DB-->>Service: UserDetails
    Service-->>Provider: UserDetails
    Provider-->>Security: Authentication Success
    Security-->>User: Redirect to Home (/)
```

### 3.5 Product Search & Filtering Flow
```mermaid
sequenceDiagram
    participant User
    participant Controller as ProductController
    participant SearchService as ProductSearchService
    participant ER as ElasticsearchRepository
    participant ES as Elasticsearch
    
    User->>Controller: GET /products?q=query&cat=category...
    
    alt Filters Applied
        Controller->>SearchService: searchProductsDetailed(filters)
        SearchService->>ER: search()
        ER->>ES: Query Index
        ES-->>ER: Results (ProductDocuments)
        ER-->>SearchService: Page<ProductDocument>
        SearchService-->>Controller: Page<ProductDocument>
    else No Filters
        Controller->>ProductService: findAll()
        ProductService->>DB: Fetch from MySQL
    end
    
    Controller-->>User: Render Product List View
```

### 3.6 Product Edit & Update Flow
```mermaid
sequenceDiagram
    participant User
    participant Controller as ProductController
    participant Service as ProductService
    participant DB as MySQL
    
    User->>Controller: GET /products/edit/{id}
    Controller->>Service: findById(id)
    Service->>DB: SELECT * FROM products
    Service-->>Controller: Product
    Controller-->>User: Edit Form
    
    User->>Controller: POST /products/update/{id}
    Controller->>Service: updateProductDetails(id, updates)
    Service->>DB: UPDATE products
    Service-->>Controller: Success
    Controller-->>User: Redirect /products/{id}
```

### 3.7 User: Manage Own Posts (Delete/Mark as Sold) Flow
```mermaid
sequenceDiagram
    participant User
    participant Controller as ProductController
    participant ProductService
    participant DB as MySQL
    
    Note over User: User views their profile listings
    
    alt Mark Product as Sold
        User->>Controller: POST /products/{id}/sold
        Controller->>ProductService: findById(id)
        ProductService->>DB: SELECT * FROM products WHERE id = ?
        DB-->>ProductService: Product
        
        alt User is Owner
            ProductService-->>Controller: Product (verified)
            Controller->>ProductService: updateProductStatus(id, SOLD)
            ProductService->>DB: UPDATE products SET status = 'SOLD'
            DB-->>ProductService: Success
            ProductService-->>Controller: Success
            Controller-->>User: Redirect /profile?tab=listings
        else User is NOT Owner
            Controller-->>User: Redirect /error (403)
        end
        
    else Delete Product
        User->>Controller: POST /products/{id}/delete
        Controller->>ProductService: findById(id)
        ProductService->>DB: SELECT * FROM products WHERE id = ?
        DB-->>ProductService: Product
        
        alt User is Owner or Admin
            ProductService-->>Controller: Product (verified)
            Controller->>ProductService: updateProductStatus(id, DELETED)
            ProductService->>DB: UPDATE products SET status = 'DELETED'
            DB-->>ProductService: Success
            ProductService-->>Controller: Success
            Controller-->>User: Redirect /profile?tab=listings
        else User is NOT Owner/Admin
            Controller-->>User: Redirect /error (403)
        end
    end
```

### 3.7 Chat Initiation Flow
```mermaid
sequenceDiagram
    participant User
    participant Controller as ChatPageController
    participant Service as ChatRoomService
    participant DB as MySQL
    
    User->>Controller: GET /chat/product/{productId}
    Controller->>Service: getOrCreateChatRoom(productId, userId)
    Service->>DB: Check existing Room
    
    alt Room Exists
        DB-->>Service: ChatRoom
    else New Room Needed
        Service->>DB: INSERT ChatRoom
        DB-->>Service: New ChatRoom
    end
    
    Service-->>Controller: ChatRoom
    Controller-->>User: Redirect /chat/room/{id}
```

### 3.8 User Profile Load Flow
```mermaid
sequenceDiagram
    participant User
    participant Controller as UserController
    participant Service as USER/Product/Chat Services
    participant DB as MySQL
    
    User->>Controller: GET /profile
    Controller->>Service: getUser(), getMyProducts(), getLiked(), getChats()
    Service->>DB: Multiple SELECT queries
    DB-->>Controller: User Data, Product Lists, Chat History
    Controller-->>User: Render Profile View
```

### 3.9 User: Update Profile Flow
```mermaid
sequenceDiagram
    participant User
    participant Controller as UserController
    participant UserService
    participant PasswordEncoder
    participant DB as MySQL
    
    User->>Controller: POST /profile/update
    Note over User: Submits: username, email, phone,<br/>address, currentPassword, newPassword
    
    Controller->>UserService: updateProfile(userId, updates)
    UserService->>DB: findById(userId)
    DB-->>UserService: User Entity
    
    alt Password Change Requested
        UserService->>PasswordEncoder: matches(currentPassword, storedHash)
        
        alt Current Password Incorrect
            PasswordEncoder-->>UserService: false
            UserService-->>Controller: Exception: Invalid Password
            Controller-->>User: Redirect with Error
        else Current Password Correct
            PasswordEncoder-->>UserService: true
            UserService->>PasswordEncoder: encode(newPassword)
            PasswordEncoder-->>UserService: New Hash
            UserService->>UserService: user.setPassword(newHash)
        end
    end
    
    UserService->>UserService: Update user fields (username, email, etc.)
    UserService->>DB: save(User)
    DB-->>UserService: Updated User
    UserService-->>Controller: Success
    
    alt Username Changed
        Controller-->>User: Redirect /logout (Re-login required)
    else Username Unchanged
        Controller-->>User: Redirect /profile?tab=settings (Success)
    end
```

### 3.9 Admin: User Management Flow
```mermaid
sequenceDiagram
    participant Admin
    participant Controller as AdminController
    participant UserService
    participant DB as MySQL
    
    Admin->>Controller: GET /admin/users
    Controller->>UserService: getUsers(pageable)
    UserService->>DB: SELECT * FROM users
    DB-->>Controller: Page<User>
    Controller-->>Admin: User List View
    
    alt Change User Status
        Admin->>Controller: POST /admin/users/{id}/status
        Controller->>UserService: updateUserStatus(id, status)
        UserService->>DB: UPDATE users SET status = ?
        UserService-->>Controller: Success
        Controller-->>Admin: Redirect /admin/users
    else Delete User
        Admin->>Controller: POST /admin/users/{id}/delete
        Controller->>UserService: deleteUser(id)
        UserService->>DB: DELETE FROM users
        UserService-->>Controller: Success
        Controller-->>Admin: Redirect /admin/users
    end
```

### 3.10 Admin: Post Management Flow
```mermaid
sequenceDiagram
    participant Admin
    participant Controller as AdminController
    participant ProductService
    participant DB as MySQL
    
    Admin->>Controller: GET /admin/posts?status=PENDING
    Controller->>ProductService: getAdminProducts(status)
    ProductService->>DB: SELECT * FROM products WHERE status = ?
    DB-->>Controller: List<Product>
    Controller-->>Admin: Post List View
    
    Admin->>Controller: POST /admin/posts/{id}/status
    Controller->>ProductService: updateProductStatus(id, status)
    ProductService->>DB: UPDATE products SET status = ?
    ProductService-->>Controller: Success
    ProductService-->>Controller: Success
    Controller-->>Admin: Redirect /admin/posts
```

### 3.11 Admin: View Statistics (Dashboard) Flow
```mermaid
sequenceDiagram
    participant Admin
    participant Controller as AdminController
    participant UserService
    participant ProductService
    participant DB as MySQL
    
    Admin->>Controller: GET /admin
    
    par Fetch User Stats
        Controller->>UserService: countUsers()
        UserService->>DB: COUNT(*) FROM users
        DB-->>UserService: Long
        Controller->>UserService: getRecentUsers()
        UserService->>DB: SELECT * FROM users ORDER BY created_at DESC LIMIT 5
        DB-->>UserService: List<User>
    and Fetch Product Stats
        Controller->>ProductService: countProducts()
        ProductService->>DB: COUNT(*) FROM products
        DB-->>ProductService: Long
    end
    
    Controller-->>Admin: Render Dashboard (with stats)
```

### 3.12 User: Like/Unlike Product Flow
```mermaid
sequenceDiagram
    participant User
    participant Controller as LikeController
    participant Service as ProductLikeService
    participant DB as MySQL
    
    User->>Controller: POST /like/toggle/{productId} (AJAX)
    Controller->>Service: toggleLike(user, productId)
    Service->>DB: findByUserAndProduct(user, product)
    
    alt Like Exists
        DB-->>Service: ProductLike
        Service->>DB: delete(ProductLike) (Unlike)
        Service-->>Controller: "unliked"
    else Like Not Found
        DB-->>Service: null
        Service->>DB: save(new ProductLike) (Like)
        Service-->>Controller: "liked"
    end
    
    Controller-->>User: String Response ("liked" / "unliked")
    Note right of User: Frontend updates Heart Icon color
```

## 4. Component Architecture (Chat Module)

The application is structured logically by feature.

```mermaid
classDiagram
    class ChatPageController {
        +showChatList()
        +startChat()
        +showChatRoom()
    }
    
    class ChatWebSocketController {
        +sendMessage()
    }
    
    class ChatRoomService {
        +getOrCreateChatRoom()
        +resetUnreadCount()
    }
    
    class ChatMessageService {
        +saveMessage()
        +getHistory()
    }
    
    class ChatRoomRepository {
        <<Interface>>
        +findByParticipants()
    }

    ChatPageController --> ChatRoomService
    ChatWebSocketController --> ChatMessageService
    ChatMessageService --> ChatRoomService
    ChatRoomService --> ChatRoomRepository
```

## 5. Deployment Architecture

```mermaid
graph TD
    UserClient[User Device]
    
    subgraph "Docker Host"
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
    
    UserClient -->|HTTPS / WSS| LB
    LB -->|Proxy| Tomcat
    SpringBoot -->|JDBC| MySQL_C
    SpringBoot -->|REST| ES_C
    SpringBoot -->|AWS SDK| LocalStack
```
