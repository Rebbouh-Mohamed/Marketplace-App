# Oudekenis Marketplace - API Documentation

This document describes the REST API endpoints available in the Oudekenis Marketplace application.

## Base URL
All API endpoints are prefixed with: `/api/v1`

---

## 1. Authentication

### Register User
*   **URL**: `/auth/register`
*   **Method**: `POST`
*   **Body**:
    ```json
    {
      "username": "johndoe",
      "email": "john@example.com",
      "password": "strongpassword"
    }
    ```
*   **Success Response**: `200 OK`
    ```json
    {
      "userId": 1,
      "username": "johndoe",
      "message": "User registered successfully"
    }
    ```

### Login User
*   **URL**: `/auth/login`
*   **Method**: `POST`
*   **Body**:
    ```json
    {
      "username": "johndoe",
      "password": "strongpassword"
    }
    ```
*   **Success Response**: `200 OK`
    ```json
    {
      "userId": 1,
      "username": "johndoe",
      "message": "Login successful"
    }
    ```

---

## 2. Products

### List All Products
*   **URL**: `/products`
*   **Method**: `GET`
*   **Query Parameters**:
    *   `page` (optional, default: 0): Page number.
*   **Success Response**: `200 OK` (Paginated list of Products)

### Search Products
*   **URL**: `/products/search`
*   **Method**: `GET`
*   **Query Parameters**:
    *   `q` (required): Search query string.
    *   `page` (optional, default: 0): Page number.
*   **Success Response**: `200 OK` (Paginated list of ProductDocuments from Elasticsearch)

### Get Product Details
*   **URL**: `/products/{id}`
*   **Method**: `GET`
*   **Success Response**: `200 OK`
    ```json
    {
      "id": 1,
      "title": "Smartphone X",
      "description": "Latest model...",
      "price": 699.99,
      "category": { "name": "Electronics" },
      "condition": "NEW",
      "location": "Algiers",
      "status": "ACTIVE",
      "viewCount": 120,
      "likeCount": 15
    }
    ```
*   **Error Response**: `404 Not Found` if product does not exist.
