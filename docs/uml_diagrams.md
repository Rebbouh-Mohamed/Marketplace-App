# Oudekeniss Marketplace - UML Diagrams

## 1. Use Case Diagram

The following diagram illustrates the primary actors and their interactions with the system.

```mermaid
usecaseDiagram
    actor "Guest" as Guest
    actor "User" as User
    actor "Seller" as Seller

    package "Oudekeniss Marketplace" {
        usecase "Browse Products" as UC1
        usecase "Search Items" as UC2
        usecase "View Details" as UC3
        
        usecase "Authenticate" as UC4
        usecase "Manage Dashboard" as UC5
        usecase "Real-Time Chat" as UC6
        usecase "Favorite Item" as UC7
        
        usecase "Post Listing" as UC8
        usecase "Update Listing" as UC9
    }

    Guest --> UC1
    Guest --> UC2
    Guest --> UC3
    Guest --> UC4

    User --|> Guest
    User --> UC5
    User --> UC6
    User --> UC7

    Seller --|> User
    Seller --> UC8
    Seller --> UC9

    UC8 ..> UC3 : <<leads to>>
```

## 2. Sequence Diagrams

### a) Real-Time Messaging Sequence (WebSocket)
```mermaid
sequenceDiagram
    participant ClientA as Browser (Buyer)
    participant WS as WebSocket Handler
    participant Service as ChatService
    participant DB as MySQL
    participant ClientB as Browser (Seller)

    Note over ClientA, ClientB: Users established STOMP connection
    
    ClientA->>WS: SEND /app/chat.send/{roomId} (JSON)
    activate WS
    WS->>Service: saveMessage(dto, user)
    activate Service
    Service->>DB: INSERT INTO chat_messages...
    Service->>DB: UPDATE chat_rooms (last_msg, unread)
    Service-->>WS: MessageEntity
    deactivate Service
    
    WS-->>ClientA: BROADCAST /topic/chatroom/{roomId} (JSON)
    WS-->>ClientB: BROADCAST /topic/chatroom/{roomId} (JSON)
    deactivate WS
    
    Note over ClientB: UI updates instantly without refresh
```

### b) Product Listing Sequence
```mermaid
sequenceDiagram
    participant User
    participant Controller as ProductController
    participant Service as ProductService
    participant S3 as AWS S3
    participant DB as MySQL
    participant Search as SearchService

    User->>Controller: POST /products/new
    activate Controller
    
    Controller->>Service: createProduct(dto, images)
    activate Service
    
    Service->>DB: save(Product)
    
    loop Each Image
        Service->>S3: upload(file)
        Service->>DB: save(ProductImage)
    end
    
    Service->>Search: index(Product)
    Search->>Search: Update Elasticsearch
    
    Service-->>Controller: Success
    deactivate Service
    
    Controller-->>User: Redirect to details
    deactivate Controller
```

### c) User Authenticated Flow (SSO/Session)
```mermaid
sequenceDiagram
    participant User
    participant Sec as Spring Security
    participant Service as UserDetailsService
    participant DB as MySQL

    User->>Sec: Submit Login Form
    activate Sec
    Sec->>Service: loadByUsername()
    activate Service
    Service->>DB: SELECT from app_users
    DB-->>Service: UserData
    Service-->>Sec: UserDetails
    deactivate Service
    
    Sec->>Sec: Verify Password (BCrypt)
    Sec-->>User: Create Session (Set-Cookie)
    deactivate Sec
```

## 3. Activity Diagram

### Product Engagement Workflow
```mermaid
flowchart TD
    Start([Start]) --> Browse[Browse Product List]
    Browse --> Click[Click on Product Item]
    Click --> Details[View Product Details]
    
    Details --> Auth{Is Logged In?}
    Auth -- No --> Login[Navigate to Login]
    Login --> Details
    
    Auth -- Yes --> Choice{User Action?}
    Choice -- Like --> Fav[Toggle Favorite Status]
    Choice -- Chat --> OpenChat[Click 'Message Seller']
    
    OpenChat --> Room[Enter Real-Time Chat Room]
    Room --> Msg[Send Message via WebSocket]
    Msg --> Room
    
    Fav --> Details
    Room --> End([End])
```
