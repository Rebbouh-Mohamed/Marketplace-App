# Oudekeniss Marketplace - UML Diagrams

## 1. Use Case Diagram

The following diagram illustrates the primary actors and their interactions with the system.

```mermaid
usecaseDiagram
    actor "Guest User" as Guest
    actor "Registered User" as User
    actor "Seller" as Seller
    actor "Administrator" as Admin

    package "Oudekeniss Marketplace" {
        usecase "Search Products" as UC1
        usecase "View Product Details" as UC2
        usecase "Filter by Category/Price" as UC3
        
        usecase "Register / Login" as UC4
        usecase "Manage Profile" as UC5
        usecase "Send Message" as UC6
        usecase "Add to Favorites" as UC7
        
        usecase "Create Listing" as UC8
        usecase "Upload Photos" as UC9
        usecase "Edit/Delete Listing" as UC10
        
        usecase "Moderate Listings" as UC11
        usecase "Manage Users" as UC12
        usecase "View Statistics" as UC13
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
    Seller --> UC10

    Admin --> UC11
    Admin --> UC12
    Admin --> UC13

    UC8 ..> UC9 : <<include>>
```

## 2. Sequence Diagrams

### a) User Registration Sequence
```mermaid
sequenceDiagram
    participant Client as Web Browser
    participant Controller as AuthController
    participant Service as UserService
    participant Repo as UserRepository
    participant DB as User Database

    Client->>Controller: POST /register (username, email, password)
    activate Controller
    Controller->>Service: createUser(UserDTO)
    activate Service
    
    Service->>Repo: findByEmail(email)
    activate Repo
    Repo-->>Service: null (User not found)
    deactivate Repo

    Service->>Service: encryptPassword(password)
    Service->>Repo: save(User)
    activate Repo
    Repo->>DB: INSERT INTO users...
    DB-->>Repo: User ID
    Repo-->>Service: User Entity
    deactivate Repo

    Service-->>Controller: User Entity
    deactivate Service
    Controller-->>Client: 200 OK (Redirect to Login)
    deactivate Controller
```

### b) Product Listing Creation Sequence
```mermaid
sequenceDiagram
    participant User
    participant Controller as ProductController
    participant PService as ProductService
    participant IService as ImageService
    participant S3 as AWS S3 / LocalStack
    participant Repo as ProductRepository
    participant Search as SearchService

    User->>Controller: POST /products/new (Form Data + Images)
    activate Controller
    
    Controller->>PService: createProduct(product, images)
    activate PService
    
    PService->>Repo: save(Product) (Initial Save)
    Repo-->>PService: Product (ID generated)
    
    loop For Each Image
        PService->>IService: upload(file)
        activate IService
        IService->>S3: PutObject
        S3-->>IService: S3 Key / URL
        IService-->>PService: Image URL
        deactivate IService
    end
    
    PService->>PService: addImagesToProduct(urls)
    PService->>Repo: save(Product) (Update with Images)
    
    PService->>Search: indexProduct(Product)
    activate Search
    Search->>Search: Update Elasticsearch Index
    Search-->>PService: void
    deactivate Search
    
    PService-->>Controller: Product
    deactivate PService
    
    Controller-->>User: Redirect to Product Page
    deactivate Controller
```

### c) Product Search Sequence
```mermaid
sequenceDiagram
    participant User
    participant Controller as ProductController
    participant Service as SearchService
    participant ES_Repo as ProductSearchRepository
    participant ES as Elasticsearch

    User->>Controller: GET /products?q=camera&category=tech
    activate Controller
    
    Controller->>Service: searchProductsDetailed(query, filters)
    activate Service
    
    Service->>Service: buildCriteriaQuery()
    Service->>ES_Repo: search(query)
    activate ES_Repo
    ES_Repo->>ES: Query Index
    ES-->>ES_Repo: SearchHits (Ranked Results)
    ES_Repo-->>Service: Page<ProductDocument>
    deactivate ES_Repo
    
    Service-->>Controller: Page<ProductDocument>
    deactivate Service
    
    Controller-->>User: Render List View (Thymeleaf)
    deactivate Controller
```

### d) Messaging Sequence
```mermaid
sequenceDiagram
    participant Buyer
    participant Controller as ChatController
    participant Service as MessageService
    participant Repo as MessageRepository
    participant DB as Message Database
    participant Seller

    Buyer->>Controller: POST /chat/send (content, recipientId)
    activate Controller
    
    Controller->>Service: sendMessage(content, sender, recipient)
    activate Service
    
    Service->>Repo: save(Message)
    activate Repo
    Repo->>DB: INSERT INTO messages...
    DB-->>Repo: success
    deactivate Repo
    
    note right of Service: In a real-time system, this would trigger a WebSocket event
    
    Service-->>Controller: Message Entity
    deactivate Service
    
    Controller-->>Buyer: Update Chat View
    deactivate Controller
    
    note over Seller: Seller refreshes or receives notification
```

### e) User Authentication Sequence
```mermaid
sequenceDiagram
    participant User
    participant Controller as LoginController
    participant AuthMgr as AuthenticationManager
    participant Provider as DaoAuthenticationProvider
    participant Service as UserDetailsService
    participant Repo as UserRepository
    participant Session as HttpSession

    User->>Controller: POST /login (username, password)
    activate Controller
    
    Controller->>AuthMgr: authenticate(token)
    activate AuthMgr
    
    AuthMgr->>Provider: authenticate()
    activate Provider
    
    Provider->>Service: loadUserByUsername()
    activate Service
    Service->>Repo: findByUsername()
    Repo-->>Service: UserDetails
    deactivate Service
    
    Provider->>Provider: checkPassword(hash)
    Provider-->>AuthMgr: Authentication Object
    deactivate Provider
    
    AuthMgr-->>Controller: Authenticated
    deactivate AuthMgr
    
    Controller->>Session: setSecurityContext(Auth)
    Controller-->>User: Redirect to Home
    deactivate Controller
```

## 3. Activity Diagram

### Product Posting Workflow
This diagram represents the flow of creating a new product listing.

```mermaid
flowchart TD
    Start([Start]) --> Login{User Logged In?}
    Login -- No --> RedirectLogin[Redirect to Login]
    RedirectLogin --> Stop([Stop])
    
    Login -- Yes --> ShowForm[Display 'Sell Product' Form]
    
    ShowForm --> InputData[/User Enters Title, Price, Description, Category/]
    InputData --> UploadImages[/User Selects Images/]
    
    UploadImages --> Validate{Validation Pass?}
    
    Validate -- No --> ShowErrors[Display Error Messages]
    ShowErrors --> InputData
    
    Validate -- Yes --> SaveDraft[Save Initial Product to DB]
    
    SaveDraft --> ProcessImages[Resize & Compress Images]
    ProcessImages --> UploadS3[Upload to S3 Storage]
    
    UploadS3 --> CheckUpload{Upload Success?}
    CheckUpload -- No --> HandleError[Log Error & Notify User]
    
    CheckUpload -- Yes --> LinkImages[Link Image URLs to Product]
    LinkImages --> IndexES[Index Product in Elasticsearch]
    
    IndexES --> Notify[Show Success Toast]
    Notify --> RedirectPage[Redirect to New Product Page]
    RedirectPage --> End([End])
```
