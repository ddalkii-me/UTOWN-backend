# UTown Backend

[![CI](https://github.com/Habsida-Projects/utown-backend-aug7/actions/workflows/ci.yml/badge.svg?branch=dev)](https://github.com/Habsida-Projects/utown-backend-aug7/actions/workflows/ci.yml)

Food delivery platform backend for the UTown project. Built with **Spring Boot 4**, **Java 21**, **Spring Security (JWT)**, **Spring Data JPA**, and **MySQL 8.4**.

---

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Architecture & Key Features](#architecture--key-features)
- [Prerequisites](#prerequisites)
- [Environment Variables](#environment-variables)
- [Docker & Docker Compose](#docker--docker-compose)
- [Docker Verification Plan](#docker-verification-plan)
- [Local Development (Without Docker)](#local-development-without-docker)
- [API Quick Reference](#api-quick-reference)
- [Testing](#testing)

---

## Overview

The UTown backend powers a multi-role food delivery platform servicing customers, restaurant owners, and administrators. It provides robust REST APIs for restaurant catalog browsing, menu and dish options management, order tracking, and stateless user authentication.

---

## Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 (Eclipse Temurin) |
| **Framework** | Spring Boot 4.1.0 |
| **Security** | Spring Security, Stateless JWT (`jjwt` 0.12.6), BCrypt |
| **Data & Persistence** | Spring Data JPA, Hibernate, MySQL Connector/J |
| **Database** | MySQL 8.4 (with `utf8mb4` encoding and `Asia/Seoul` timezone) |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, H2 In-Memory DB |
| **Build Tool** | Apache Maven 3.9 (via `mvnw` wrapper) |
| **Containerization** | Docker (Multi-stage Alpine build), Docker Compose v2+ |

---

## Architecture & Key Features

* **Stateless Authentication (AuthN)**:
  * Primary identifier: User phone number (standardized with `+82` country code for South Korea).
  * Cryptographic HMAC-SHA256 token signing with strict minimum 256-bit (32-byte) secret enforcement.
  * True refresh token rotation with single-use revocation and soft-delete persistence.
  * Stateless `CustomUserDetails` principal providing `userId`, phone number, and user authorities without querying the database on every authenticated request.
* **Database & Data Layer**:
  * Hibernate JPA with soft-delete convention (`deletedAt IS NULL`).
  * `utf8mb4` character set and `utf8mb4_unicode_ci` collation for native Korean text support (addresses, menu items, restaurant names).
  * Timestamps configured for `Asia/Seoul` (+09:00).
* **Multi-Container Deployment**:
  * Isolated MySQL service and Spring Boot application orchestrated via Docker Compose.
  * Automated health checks to prevent database connection race conditions during startup.

---

## Prerequisites

* **Java**: JDK 21+ (for local bare-metal builds)
* **Docker**: Docker Desktop or Docker Engine v24+ with Docker Compose v2+
* **Maven**: Embedded Maven Wrapper (`./mvnw`) included

---

## Environment Variables

The project uses `.env` to configure application settings. A template is provided in `.env.example`.

Copy `.env.example` to create your local `.env`:

```bash
cp .env.example .env
```

### Key Configuration Variables

| Variable | Default Value | Description |
|---|---|---|
| `DB_NAME` | `utown_db` | MySQL database name |
| `DB_USERNAME` | `utown_user` | Application database username |
| `DB_PASSWORD` | `utown_password` | Application database user password |
| `DB_ROOT_PASSWORD` | `root_password` | MySQL root administrative password |
| `MYSQL_PORT` | `3306` | Port exposed by the MySQL container on the host |
| `APP_PORT` | `8080` | Port exposed by the Spring Boot container on the host |
| `SERVER_ADDRESS` | `0.0.0.0` (Docker) / `127.0.0.1` (Local) | Server binding IP |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Hibernate schema management strategy |
| `JWT_SECRET` | `404E635...` | Base64 or plaintext secret (must be >= 32 bytes) |
| `JWT_EXPIRATION` | `86400000` | Access token lifespan in milliseconds (24 hours) |

> [!NOTE]
> `server.address` is configured as `${SERVER_ADDRESS:127.0.0.1}`. When running directly on your host machine without Docker, it safely defaults to `127.0.0.1`. Inside Docker containers, Docker Compose automatically supplies `SERVER_ADDRESS=0.0.0.0` so host port forwardings are accepted.

---

## Docker & Docker Compose

The environment is split into two isolated containers connected over a private bridge network (`utown-network`):

1. **`mysql`**: Official `mysql:8.4` container with persistent volume storage (`mysql_data`), character encoding flags, and a healthcheck probe.
2. **`app`**: Built from a multi-stage [Dockerfile](Dockerfile) using `eclipse-temurin:21-jre-alpine` running as a non-privileged user (`utown`). It waits for the MySQL health check before starting.

### Spinning Up the Environment

```bash
# Build images and start all containers in detached mode
docker compose up -d --build

# View container status and health check state
docker compose ps

# Follow live application logs
docker compose logs -f app

# Follow database logs
docker compose logs -f mysql

# Stop all containers
docker compose down

# Stop containers and remove persistent database volumes (Clean reset)
docker compose down -v
```

---

## Docker Verification Plan

Follow these steps to verify that the containerized environment is running properly:

### 1. Build & Compose Configuration Validation
Ensure the Dockerfile builds cleanly and Docker Compose parses all environment variables:
```bash
# Validate Docker Compose interpolation and syntax
docker compose config

# Verify Maven builds the executable JAR
./mvnw clean package -DskipTests
```

### 2. Start the Stack & Monitor Health Checks
```bash
docker compose up -d --build
```
Inspect the running status:
```bash
docker compose ps
```
* **Expected Result**: 
  * `utown-mysql` should display `Up (healthy)` after 10–15 seconds.
  * `utown-backend` should start only after MySQL reports `healthy`, preventing HikariCP pool errors.

### 3. Verify Database Initialization & Connection
Inspect Spring Boot startup logs:
```bash
docker compose logs app | grep -E "HikariPool|Started UtownBackendApplication"
```
* **Expected Result**: Logs indicate `HikariPool-1 - Added connection` and `Started UtownBackendApplication in X.XXX seconds`.

### 4. Verify API External Access (Host Port Binding)
Test the registration endpoint through host port `8080`:
```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "01012345678",
    "password": "Password123!",
    "confirmPassword": "Password123!"
  }'
```
* **Expected Result**: HTTP `200 OK` or `201 Created` with JWT tokens returned.

### 5. Verify Database Persistence Across Restarts
1. Stop the containers without removing volumes:
   ```bash
   docker compose down
   ```
2. Restart the containers:
   ```bash
   docker compose up -d
   ```
3. Attempt to log in with the previously registered user:
   ```bash
   curl -i -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "phoneNumber": "01012345678",
       "password": "Password123!"
     }'
   ```
* **Expected Result**: Authentication succeeds, proving data persisted in `mysql_data`.

---

## Local Development (Without Docker)

If developing locally without Docker, you can connect to an existing MySQL instance or use the embedded H2 database configured in test profiles.

1. **Configure Environment Variables**:
   Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `JWT_EXPIRATION` in your environment or IDE run configuration.
2. **Run the Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
3. The server will bind to `http://127.0.0.1:8080`.

---

## API Quick Reference

### Authentication Endpoints (`/api/auth`)

| Method | Endpoint | Description | Request Body |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register new user | `phoneNumber`, `password`, `confirmPassword`, `verificationToken` (optional) |
| `POST` | `/api/auth/login` | Authenticate & get JWT tokens | `phoneNumber`, `password` |
| `POST` | `/api/auth/refresh` | Rotate refresh token & get new tokens | `refreshToken` |
| `POST` | `/api/auth/logout` | Revoke refresh token & end session | `refreshToken` |

### General Domain Endpoints

| Resource | Endpoints |
|---|---|
| **Restaurants** | `/api/restaurants` (browse, search, view details) |
| **Dishes** | `/api/dishes`, `/api/dish-options`, `/api/dish-option-groups` |
| **Categories** | `/api/categories` |
| **Delivery Areas** | `/api/delivery-areas`, `/api/restaurant-delivery-areas` |
| **Orders** | `/api/orders` |
| **Addresses** | `/api/addresses` |
| **Favorites** | `/api/favorite-restaurants` |

---

## Testing

The project includes unit and integration tests using an in-memory H2 database (`MODE=MySQL`) and mock MVC contexts:

```bash
# Run the complete test suite
./mvnw test
```

All 360+ tests can be executed without requiring a running MySQL instance or Docker daemon.