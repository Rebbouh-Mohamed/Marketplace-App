# Oudekenis Marketplace - API Documentation

This document describes the REST API endpoints available in the Oudekenis Marketplace application.

## Base URL
Since this is a primarily SSR (Server-Side Rendering) application, our REST APIs follow standard REST conventions but are integrated within the main application path at: `/api/...`. For consistency with future mobile developments, versioning starts at: `/api/v1`.

---

## 1. Authentication

### Register User
*   **URL**: `/register` (via Form) or `/api/auth/register` (REST)
*   **Method**: `POST`
*   **Body (REST)**:
    ```json
    {
      "username": "johndoe",
      "email": "john@example.com",
      "password": "strongpassword"
    }
    ```

### Login User
*   **URL**: `/login` (via Form) or `/api/auth/login` (REST)
*   **Method**: `POST`
*   **Body (REST)**:
    ```json
    {
      "username": "johndoe",
      "password": "strongpassword"
    }
    ```

---

## 2. Products

### List All Products
*   **URL**: `/api/v1/products`
*   **Method**: `GET`
*   **Query Parameters**:
    *   `page` (optional, default: 0): Page number.
*   **Success Response**: `200 OK` (Paginated list of Products)

### Search Products
*   **URL**: `/api/v1/products/search`
*   **Method**: `GET`
*   **Query Parameters**:
    *   `q` (required): Search query string.
    *   `page` (optional, default: 0): Page number.
*   **Success Response**: `200 OK` (Paginated list of ProductDocuments from Elasticsearch)

### Get Product Details
*   **URL**: `/api/v1/products/{id}`
*   **Method**: `GET`
*   **Success Response**: `200 OK`

---

## 3. Real-Time Chat

### Get All Chat Rooms
*   **URL**: `/api/chat/rooms` (Required Auth)
*   **Method**: `GET`
*   **Success Response**: `200 OK` (List of ChatRoomDTOs)

### Start a Conversation
*   **URL**: `/api/chat/start` (Required Auth)
*   **Method**: `POST`
*   **Body**:
    ```json
    {
      "productId": 1
    }
    ```
*   **Success Response**: `200 OK` (The created or existing ChatRoomDTO)

### Send a Message (REST Backup)
*   **URL**: `/api/chat/room/{chatRoomId}/send`
*   **Method**: `POST`
*   **Body**:
    ```json
    {
      "content": "Hello, is this still available?"
    }
    ```

### Mark Messages as Read
*   **URL**: `/api/chat/room/{chatRoomId}/read`
*   **Method**: `POST`

---

## 4. WebSocket (Real-Time Communication)

### Connection Endpoint
*   **URL**: `/ws` (protocol: `stomp`)
*   **Fallback**: SockJS supported

### Topic Subscription
*   **Destination**: `/topic/chatroom/{chatRoomId}`
*   **Description**: Subscribe to this topic to receive new messages in real-time.

### Messaging (Client-to-Server)
*   **Destination**: `/app/chat.send/{chatRoomId}`
*   **Payload**:
    ```json
    {
      "senderId": 1,
      "content": "Your message here"
    }
    ```

