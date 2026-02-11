# Ecommerce Platform

A scalable, production-ready e-commerce platform built with a microservices architecture (Java Spring Boot) and a modern frontend (React, Redux Toolkit, Tailwind CSS). Designed for extensibility, reliability, and clean code.

---

## Table of Contents
- [Project Overview](#project-overview)
- [Features](#features)
- [Architecture & Folder Structure](#architecture--folder-structure)
- [Backend](#backend)
- [Frontend](#frontend)
- [Setup Instructions](#setup-instructions)
- [API Usage](#api-usage)
- [Development & Deployment](#development--deployment)
- [Testing](#testing)
- [Contribution Guidelines](#contribution-guidelines)
- [License](#license)
- [Credits](#credits)

---

## Project Overview
A full-stack e-commerce solution featuring:
- Microservices backend (Spring Boot, Kafka, Redis, Eureka, Feign, etc.)
- Modern React frontend (Redux Toolkit, Tailwind CSS, React Router)
- API Gateway, centralized config, and service discovery
- Responsive, clean UI matching Figma designs

---

## Features
- Product search, listing, and category navigation
- Cart, wishlist, and checkout flows (UI only)
- Modular microservices: cart, checkout, inventory, notification, order, payment, product, rating, search, user, wishlist
- API Gateway for routing and security
- Service discovery (Eureka)
- Centralized configuration (Config Server)
- Kafka for event-driven communication
- Redis for caching
- OpenAPI/Swagger documentation
- Resilience4j for circuit breaking and retries

---

## Architecture & Folder Structure
```
Ecommerce/
├── backend/
│   ├── api-gateway/
│   ├── cart-service/
│   ├── checkout-service/
│   ├── common-dtos/
│   ├── config-server/
│   ├── eureka/
│   ├── inventory-service/
│   ├── notification-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── product-service/
│   ├── rating-service/
│   ├── search-service/
│   ├── user-service/
│   └── wishlist-service/
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── features/
│   │   ├── pages/
│   │   ├── routes/
│   │   ├── store/
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── public/
│   └── package.json
└── logs/
```

---

## Backend
### Technology Stack
- Java 21, Spring Boot 3.4+
- Microservices: cart, checkout, inventory, notification, order, payment, product, rating, search, user, wishlist
- API Gateway (Spring Cloud Gateway)
- Eureka (Service Discovery)
- Config Server (Centralized config)
- Redis (Caching)
- Kafka (Messaging)
- Feign (Inter-service communication)
- Resilience4j (Circuit breaker, retry)
- OpenAPI/Swagger (API docs)
- Docker (optional)

### Key Endpoints (Example)
- `/api/v1/search?category=Smartphones` (used by frontend)
- Each microservice exposes REST endpoints (see Swagger docs)

---

## Frontend
### Technology Stack
- React (functional components)
- Redux Toolkit (state management)
- Tailwind CSS (styling)
- React Router (routing)
- Axios (API calls)

### Main Screens & Components
- Top Navigation Bar (Logo, Categories, Search, Wishlist, Cart, Profile)
- Home Page (Hero, Featured Products, Shop by Category, Popular Products)
- Product Card (image, badge, rating, name, description, price, Add to Cart)
- Product Listing Page
- Checkout Page (UI only)

---

## Setup Instructions
### Prerequisites
- Java 21+
- Node.js 18+
- Maven 3.8+
- Docker (optional)

### Backend
1. Navigate to `backend/`
2. Build all services:
   ```powershell
   mvnw clean install
   ```
3. Start Eureka, Config Server, and API Gateway first
4. Start each microservice (use `mvnw spring-boot:run` or Docker Compose if available)

### Frontend
1. Navigate to `frontend/`
2. Install dependencies:
   ```powershell
   npm install
   ```
3. Start development server:
   ```powershell
   npm run dev
   ```

---

## API Usage
- **Frontend:** Uses `GET http://localhost:8080/api/v1/search?category=Smartphones` for all product data (initial implementation)
- **Backend:** Each microservice exposes REST APIs (see Swagger UI at `/swagger-ui.html`)

---

## Development & Deployment
- Use `.env` files for environment variables
- Backend can be containerized with Docker
- Frontend can be built with `npm run build` and served via Nginx/Apache
- CI/CD recommended for production

---

## Testing
- Backend: JUnit, Spring Boot Test
- Frontend: Jest, React Testing Library
- Run tests:
  - Backend: `mvnw test`
  - Frontend: `npm test`

---

## Contribution Guidelines
- Fork the repo, create a branch, submit PRs
- Follow code style and commit conventions
- Write tests for new features
- See `CONTRIBUTING.md` (if available)

---

## License
This project is licensed under the MIT License.

---

## Credits
- Built by the Ecommerce Team
- Uses open-source libraries: Spring Boot, React, Tailwind CSS, Redux Toolkit, Kafka, Redis, etc.

---

## Contact
For questions or support, open an issue or contact the maintainers.

