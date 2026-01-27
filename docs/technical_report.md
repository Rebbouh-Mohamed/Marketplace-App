# Oudekeniss Marketplace - Technical Report

## 1. Technology Stack Overview

The **Oudekeniss** marketplace application is built on a robust, industry-standard stack designed for reliability, developer productivity, and performance.

*   **Frontend**: **Thymeleaf** + TailwindCSS + Alpine.js
    *   *Rationale*: Thymeleaf provides server-side rendering (SSR), ensuring excellent initial load performance and SEO compatibility, which is critical for an e-commerce platform. TailwindCSS offers rapid UI development with a utility-first approach, establishing a consistent "Oudekeniss" branding (Dark Mode, Custom Currency). Alpine.js adds necessary interactivity (dropdowns, image sliders) without the overhead of a full SPA framework.
*   **Backend**: **Spring Boot 3** (Java 17)
    *   *Rationale*: Spring Boot enables rapid application development with "batteries included" configuration. Java 17 offers modern language features (Records, Switch expressions) and performance improvements.
*   **Database**: **MySQL 8.0**
    *   *Rationale*: A mature Relational Database Management System (RDBMS) is essential for transactional integrity (ACID), especially for user accounts, orders, and payment records.
*   **Search Engine**: **Elasticsearch**
    *   *Rationale*: To provide fast, relevant, and fuzzy search capabilities for products that SQL databases struggle to deliver at scale.
*   **Mobile**: **Flutter** (Planned/Prototype)
    *   *Rationale*: Allows building natively compiled applications for mobile, web, and desktop from a single codebase.

---

## 2. Backend Architecture Decisions

### a) Spring Modulith (Modular Monolith)
*   **Choice**: We adopted a **Modular Monolith** architecture over a distributed Microservices architecture.
*   **Justification**:
    *   **Complexity Management**: Microservices introduce significant operational overhead (service discovery, distributed tracing, network latency). For a team of our size and a project of this scope, a monolith avoids "distributed ball of mud" syndrome.
    *   **Deployment**: The entire application deploys as a single artifact (JAR), simplifying CI/CD pipelines and infrastructure requirements.
    *   **Boundaries**: Spring Modulith helps enforce logical boundaries between packages (e.g., `user`, `product`, `search`), preventing spaghetti code dependencies while keeping the option open to extract microservices later if scaling requires it.

### b) Spring Data JPA + Hibernate
*   **Choice**: Object-Relational Mapping (ORM) via Hibernate, abstracted by Spring Data JPA.
*   **Justification**:
    *   **Productivity**: Automatically generates SQL for standard CRUD operations, reducing boilerplate code by ~70%.
    *   **Abstraction**: Allows switching underlying databases (e.g., from H2 in tests to MySQL in prod) with zero code changes.
    *   **Type Safety**: Spring Data Repositories provide build-time checks for query methods (`findByTitleContaining`), preventing runtime SQL errors.

---

## 3. Performance Optimization Choices

### a) Elasticsearch Integration
*   **Choice**: Offloading search queries from MySQL to Elasticsearch.
*   **Why**: MySQL `LIKE '%query%'` queries cannot use standard B-Tree indexes efficiently (requiring full table scans) and offer poor relevance scoring.
*   **Strategy**:
    *   **Indexing**: We use a `ProductDocument` entity mapped to an Elasticsearch index.
    *   **Synchronization**: Utilized a "Dual-Write" strategy in the `ProductService`. When a product is created or images are added, the service updates MySQL (source of truth) and immediately triggers an index update in Elasticsearch.
    *   **Benefit**: Search response times remain sub-100ms even as the dataset grows, and features like fuzzy matching handle user typos gracefully.

### b) Database Indexing
*   **Implementation**: Applied indexes on high-cardinality foreign keys (`category_id`, `seller_id`) and frequent filter fields (`status`, `price`).
*   **Impact**: drastically reduces lookup time for queries like "Show all active products in Electronics category", changing complexity from O(N) scan to O(log N) seek.

### c) Image Handling (Compression)
*   **Choice**: Client-side or Server-side compression before storage.
*   **Implementation**: Images are processed to ensure consistent formats (JPG/WebP) and reasonable resolution (e.g., max 1080p) before being uploaded to S3. This reduces bandwidth usage and improves page load speed for end-users.

---

## 4. Frontend Choices

### a) Thymeleaf (Server-Side Rendering)
*   **Requirement Compliance**: Directly addresses the project requirement for a Java-based template engine.
*   **Adavantages**:
    *   **SEO**: Unlike Single Page Apps (React/Angular) which require client-side JS execution, Thymeleaf sends fully populated HTML to the crawler. This is crucial for "Oudekeniss" product pages to rank on Google.
    *   **Security Integration**: Seamlessly works with Spring Security (`sec:authorize`) to conditionally render UI elements (e.g., "Edit" button only for the owner) on the server, preventing sensitive UI leakage.
    *   **Performance**: First Contentful Paint (FCP) is faster because the browser receives ready-to-render HTML.

### b) JavaScript Enhancements (Alpine.js)
*   **Choice**: Using **Alpine.js** for interactive bits.
*   **Why**: We needed interactivity (Dark mode toggle, Mobile menu, Tab switching in Profile) but didn't want the build complexity of a full Node.js toolchain (Webpack/Vite) for the frontend.
*   **Benefit**: Keeps the frontend codebase lightweight and directly embeddable in HTML attributes, maintaining low cognitive load for backend-focused developers.

---

## 5. Security Implementation

### Spring Security
*   **Configuration**: Stateless restrictions for API endpoints, Stateful session management for Web views.
*   **Password Encoding**: **BCrypt** hashing algorithm (strength 10) is used. It is slow by design to resist rainbow table and brute-force attacks.
*   **CSRF Protection**: Enabled by default for all POST requests. This prevents Cross-Site Request Forgery attacks, essential for protecting actions like "Delete Product" or "Update Profile".
*   **Authentication**: Form-based authentication for web users. The `UserDetailsService` implementation loads users from our MySQL database, supporting custom roles (`BUYER`, `SELLER`).
*   **Authorization**: Method-level security (`@PreAuthorize`) ensures that only the owner of a product (or an Admin) can edit or delete it.

---

## 6. Mobile Application Approach

*   **Technology**: **Flutter**.
*   **Strategy**: Cross-platform development.
*   **Justification**:
    *   **Efficiency**: Write once, run on both iOS and Android. This drastically reduces the development time and maintenance cost compared to writing separate Swift and Kotlin apps.
    *   **Performance**: Flutter compiles to native ARM code, offering near-native performance (60fps).
*   **API Design**: The Spring Boot backend exposes RESTful APIs (`/api/v1/products`) which the mobile app consumes. This creates a clear separation where the backend serves as a "Headless CMS" for the mobile client.

---

## 7. Data Management

### File Storage Strategy (S3)
*   **Choice**: **AWS S3** (simulated via LocalStack for dev).
*   **Why**: Storing images as BLOBs (Binary Large Objects) in MySQL creates massive database bloat, making backups slow and expensive.
*   **Implementation**: We store simple string references (S3 Keys / URLs) in the MySQL `product_images` table. The actual files live in object storage, which is designed for cheap, scalable file hosting.

### Database Schema Design
*   **Normalization**: The schema is normalized (3NF) to prevent data redundancy. For example, `Categories` are stored in a separate table and referenced by ID, rather than duplicating category names in the `Products` table.
*   **Soft Deletes**: Implemented via a `status` field (`DELETED`) rather than physical row deletion. This preserves transaction history and allows for data recovery or audit analysis.

---

## 8. Testing Strategy

*   **Unit Tests (JUnit 5 + Mockito)**:
    *   Focus on the *Business Layer* (`ProductService`). We mock the Repository and SearchService to test business logic (e.g., "User cannot sell item with negative price") in isolation.
*   **Integration Tests (Spring Boot Test)**:
    *   Focus on *Controllers*. We use `@SpringBootTest` and `MockMvc` to verify that HTTP endpoints accept correct JSON/Form data and return expected Status Codes (200, 400, 403).
*   **Repository Tests (DataJpaTest)**:
    *   Verifies that custom JPQL queries actually map to valid SQL syntax.

---

## 9. DevOps & Deployment

*   **Build Tool**: **Maven**.
    *   *Why*: Standard, declarative dependency management. The `pom.xml` provides a clear manifest of all project libraries.
*   **Containerization**: **Docker**.
    *   We use `docker-compose.yml` to spin up the supporting infrastructure (MySQL, Redis/Elasticsearch, LocalStack). This solves the "works on my machine" problem, as every developer gets the exact same database version and environment configuration.
*   **Configuration Management**:
    *   `application.properties` (or YAML) with profile support (`dev`, `prod`). Sensitive credentials (DB passwords, API keys) are injected via Environment Variables at runtime, adhering to 12-Factor App principles.

---

## 10. Scalability Considerations

*   **Current Architecture**:
    *   The Modular Monolith is efficient for current loads.
    *   **Stateless Services**: The service layer is stateless.
*   **Vertical Scaling**: The application can benefit from simply increasing CPU/RAM on the server.
*   **Horizontal Scaling**:
    *   Because user sessions are stored in-memory (currently), horizontal scaling would require implementing **Spring Session with Redis**. This would allow us to run multiple instances of the application behind a Load Balancer (Nginx/AWS ALB), sharing the session state.
    *   **Database**: Read Replicas could be added to MySQL to offload read-heavy traffic (product browsing), while the primary instance handles writes (orders/listings).
*   **Future Proofing**: If the "Search" module becomes too resource-intensive, the strict module boundaries we established allow us to peel it off into a dedicated microservice with minimal refactoring.
