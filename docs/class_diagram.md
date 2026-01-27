# Oudekeniss Marketplace - Class Diagram

This document illustrates the domain model and relationships for the Oudekeniss marketplace application.

```mermaid
classDiagram
    %% Enumerations
    class Role {
        <<enumeration>>
        BUYER
        SELLER
        ADMIN
    }

    class ProductStatus {
        <<enumeration>>
        ACTIVE
        PENDING
        REJECTED
        SOLD
        INACTIVE
        DELETED
    }

    class Condition {
        <<enumeration>>
        NEW
        USED_LIKE_NEW
        USED_GOOD
        USED_FAIR
    }

    class UserStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
        BANNED
    }

    %% Domain Entities
    class User {
        +Long id
        +String username
        +String email
        +String passwordHash
        +Role role
        +String firstName
        +String lastName
        +String phoneNumber
        +String address
        +String avatarUrl
        +UserStatus status
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class Product {
        +Long id
        +String title
        +String description
        +BigDecimal price
        +Condition condition
        +String location
        +long viewCount
        +long likeCount
        +ProductStatus status
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class Category {
        +Long id
        +String name
        +String description
        +String icon
    }

    class ProductImage {
        +Long id
        +String imageUrl
        +Integer displayOrder
    }

    class ChatMessage {
        +Long id
        +String sender
        +String content
        +LocalDateTime timestamp
    }

    class Tag {
        +Long id
        +String name
    }

    class ProductLike {
        +Long id
        +LocalDateTime createdAt
    }

    %% Relationships
    User "1" --> "*" Product : owns/sells
    Product "*" --> "1" Category : belongs to
    Product "1" *-- "*" ProductImage : has
    Product "*" -- "*" Tag : tagged with
    User "1" -- "*" ProductLike : likes
    ProductLike "*" -- "1" Product : targets
    
    %% Enum Associations
    User ..> Role
    User ..> UserStatus
    Product ..> ProductStatus
    Product ..> Condition
```
