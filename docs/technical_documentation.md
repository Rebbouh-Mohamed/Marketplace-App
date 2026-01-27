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
