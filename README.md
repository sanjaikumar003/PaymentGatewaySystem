# 💳 PaymentGatewaySystem

A **Fintech Payment Gateway System** built with Java 21 and Spring Boot 3. This project provides a secure, RESTful backend for processing payments, managing transactions, and handling user authentication — designed as a clean, production-ready integration wrapper.

> **Author:** [sanjaikumar003](https://github.com/sanjaikumar003)  
> **Repository:** [PaymentGatewaySystem](https://github.com/sanjaikumar003/PaymentGatewaySystem)

---

## 📋 Table of Contents

- [Features](#features)
- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Configuration / Environment Variables](#configuration--environment-variables)
- [API Reference](#api-reference)
- [Security Notes](#security-notes)
- [Contributing Guidelines](#contributing-guidelines)
- [License](#license)

---

## ✨ Features

- 🔐 **JWT-based Authentication** — Secure user login and session management using JSON Web Tokens
- 💸 **Payment Processing** — Initiate, track, and manage payment transactions
- 🗄️ **Persistent Storage** — All transactions stored in MySQL via Spring Data JPA
- ✅ **Input Validation** — Bean Validation on all incoming requests
- 📖 **Auto-generated API Docs** — Swagger UI via SpringDoc OpenAPI
- 🔒 **Spring Security** — Role-based access control for protected endpoints
- 🧪 **Unit & Integration Tests** — JUnit 5 + Mockito test coverage

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Client (REST / JSON)                     │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                Spring Boot Application                      │
│                                                             │
│  ┌────────────────────┐     ┌──────────────────────────┐   │
│  │   Auth Controller  │     │   Payment Controller     │   │
│  │  (Login/Register)  │     │  (Create/Status/Refund)  │   │
│  └────────┬───────────┘     └────────────┬─────────────┘   │
│           │                              │                  │
│  ┌────────▼──────────────────────────────▼─────────────┐   │
│  │                   Service Layer                      │   │
│  │         (Business Logic & Orchestration)             │   │
│  └────────────────────────┬─────────────────────────────┘  │
│                           │                                 │
│  ┌────────────────────────▼─────────────────────────────┐   │
│  │              Repository Layer (JPA)                  │   │
│  └────────────────────────┬─────────────────────────────┘   │
│                           │                                 │
│  ┌────────────────────────▼─────────────────────────────┐   │
│  │         Spring Security + JWT Filter Chain           │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │         MySQL Database          │
          │   (users, transactions, etc.)   │
          └─────────────────────────────────┘
```

**Key Layers:**

| Layer | Responsibility |
|---|---|
| Controller | Exposes REST endpoints, handles HTTP request/response |
| Service | Business logic — payment orchestration, auth, token generation |
| Repository | JPA data access to MySQL database |
| Security | JWT filter, Spring Security config, role-based access |
| DTO / Model | Request/response mapping, entity definitions |

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.6 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Persistence | Spring Data JPA + MySQL |
| Validation | Spring Boot Starter Validation (Bean Validation) |
| API Docs | SpringDoc OpenAPI 2.5.0 (Swagger UI) |
| Build Tool | Maven (Maven Wrapper included) |
| Boilerplate | Lombok 1.18.32 |
| Testing | JUnit 5 + Mockito |

---

## ✅ Prerequisites

Ensure you have the following installed before running the project:

- **Java 21+** — [Download Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21)
- **Maven 3.8+** — or use the included `./mvnw` wrapper (no installation needed)
- **MySQL 8.0+** — [Download MySQL](https://dev.mysql.com/downloads/)
- **Git** — to clone the repository

---

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/sanjaikumar003/PaymentGatewaySystem.git
cd PaymentGatewaySystem
```

### 2. Create the MySQL Database

Log into MySQL and run:

```sql
CREATE DATABASE payment_gateway_db;
```

### 3. Configure Application Properties

Edit `src/main/resources/application.properties` with your database and JWT settings:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/payment_gateway_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# JWT
jwt.secret=your_jwt_secret_key_here
jwt.expiration=86400000

# Server
server.port=8080
```

> ⚠️ **Never commit real credentials.** Use environment variables or a `.env` file in production.

### 4. Build the Project

```bash
./mvnw clean install
```

### 5. Run the Application

```bash
./mvnw spring-boot:run
```

The application starts at: `http://localhost:8080`

### 6. Access Swagger UI

Once running, visit the interactive API documentation at:

```
http://localhost:8080/swagger-ui/index.html
```

### 7. Run Tests

```bash
./mvnw test
```

---

## ⚙️ Configuration / Environment Variables

The following properties should be configured in `application.properties` or passed as environment variables.

### Database

| Property | Description | Example |
|---|---|---|
| `spring.datasource.url` | MySQL connection URL | `jdbc:mysql://localhost:3306/payment_gateway_db` |
| `spring.datasource.username` | DB username | `root` |
| `spring.datasource.password` | DB password | `your_password` |
| `spring.jpa.hibernate.ddl-auto` | Schema management strategy | `update` |

### JWT

| Property | Description | Example |
|---|---|---|
| `jwt.secret` | Secret key for signing tokens | `mySecretKey123!` |
| `jwt.expiration` | Token expiry in milliseconds | `86400000` (24 hrs) |

### Server

| Property | Description | Default |
|---|---|---|
| `server.port` | Port to run the app on | `8080` |

> 💡 **Production Tip:** Reference secrets from environment variables:
> ```properties
> jwt.secret=${JWT_SECRET}
> spring.datasource.password=${DB_PASSWORD}
> ```

---

## 📡 API Reference

Full interactive documentation is available via **Swagger UI** at `/swagger-ui/index.html` when the app is running.

### Authentication

#### Register User

```http
POST /api/auth/register
```
```json
{
  "username": "sanjai",
  "email": "sanjai@example.com",
  "password": "securePassword123"
}
```

#### Login

```http
POST /api/auth/login
```
```json
{
  "email": "sanjai@example.com",
  "password": "securePassword123"
}
```
**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 86400000
}
```

> All payment endpoints require the `Authorization: Bearer <token>` header.

---

### Payments

#### Create Payment

```http
POST /api/payments
Authorization: Bearer <token>
```
```json
{
  "amount": 1500.00,
  "currency": "INR",
  "description": "Order #101",
  "paymentMethod": "CARD"
}
```
**Response:**
```json
{
  "paymentId": "PAY-20260507-001",
  "status": "PENDING",
  "amount": 1500.00,
  "currency": "INR",
  "createdAt": "2026-05-07T10:00:00Z"
}
```

#### Get Payment Status

```http
GET /api/payments/{paymentId}
Authorization: Bearer <token>
```

#### Get All Payments

```http
GET /api/payments
Authorization: Bearer <token>
```

#### Refund Payment

```http
POST /api/payments/{paymentId}/refund
Authorization: Bearer <token>
```
```json
{
  "amount": 1500.00,
  "reason": "Item out of stock"
}
```

---

### Payment Status Values

| Status | Description |
|---|---|
| `PENDING` | Payment initiated, awaiting processing |
| `SUCCESS` | Payment completed successfully |
| `FAILED` | Payment attempt failed |
| `REFUNDED` | Full or partial refund processed |
| `CANCELLED` | Payment cancelled before completion |

---

## 🔐 Security Notes

This project implements multiple layers of security to protect user data and transactions.

### 1. JWT Authentication
All protected endpoints validate a **JWT Bearer token** on every request. Tokens are signed using a secret key and expire after the configured duration. Requests without a valid token receive `401 Unauthorized`.

### 2. Spring Security Filter Chain
All incoming requests pass through the JWT filter before reaching any controller. Public endpoints (register, login) are whitelisted; all others require authentication.

### 3. Password Encryption
User passwords are **never stored in plain text**. They are hashed using **BCryptPasswordEncoder** before being persisted to the database.

### 4. Input Validation
All request bodies are validated using **Jakarta Bean Validation** (`@Valid`, `@NotBlank`, `@NotNull`). Invalid inputs return `400 Bad Request` with descriptive error messages.

### 5. Secret Management
- JWT secret and DB credentials must be set via **environment variables** in production — never hardcoded in source files.
- Use tools like **AWS Secrets Manager** or **HashiCorp Vault** for enterprise deployments.

### 6. SQL Injection Prevention
All database interactions use **Spring Data JPA** with parameterized queries, preventing SQL injection attacks by design.

### 7. Sensitive Data Logging
- Never log passwords, JWT tokens, or payment card details.
- Mask or omit sensitive fields in all log output.

---

## 🤝 Contributing Guidelines

Contributions, issues, and feature requests are welcome!

### Getting Started

1. **Fork** the repository on GitHub
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/your-username/PaymentGatewaySystem.git
   ```
3. **Create a feature branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **Make your changes** and write tests
5. **Build and verify:**
   ```bash
   ./mvnw clean install
   ```
6. **Commit** with a clear message and open a **Pull Request**

### Commit Message Format

```
type(scope): short description

Types: feat | fix | docs | test | refactor | chore

Examples:
feat(auth): add refresh token endpoint
fix(payment): handle null amount in request
docs(readme): update API reference section
test(service): add PaymentService unit tests
```

### Coding Standards

- Follow standard Java / Spring Boot naming conventions
- Use **Lombok** to minimize boilerplate (`@Data`, `@Builder`, `@RequiredArgsConstructor`)
- All new features must include **JUnit 5 + Mockito** unit tests
- Keep controllers thin — business logic belongs in the service layer
- Add **Swagger annotations** (`@Operation`, `@ApiResponse`) to new endpoints

### Reporting Bugs

Please open a [GitHub Issue](https://github.com/sanjaikumar003/PaymentGatewaySystem/issues) with:
- Steps to reproduce
- Expected vs. actual behaviour
- Relevant stack trace or logs
- Your Java / MySQL version

---

## 📄 License

This project is open source and available under the **MIT License**.

---

> Built with ☕ Java 21 and Spring Boot 3.2.6 by [sanjaikumar003](https://github.com/sanjaikumar003).  
> For questions, open an [issue](https://github.com/sanjaikumar003/PaymentGatewaySystem/issues) on GitHub.