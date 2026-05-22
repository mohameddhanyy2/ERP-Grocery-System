# Grocery ERP System

A full-stack ERP system for grocery store management built with Spring Boot (Java Beans) and React.

## Tech Stack

**Backend:**
- Java 17
- Spring Boot 3.x
- Spring Data JPA (Java Beans for entities/DTOs)
- Spring Security + JWT
- MySQL / PostgreSQL
- Maven

**Frontend:**
- React 18
- React Router
- Axios
- Tailwind CSS (or Material UI)

## Modules

- **Inventory Management** — products, categories, stock levels, expiry tracking
- **Sales & POS** — orders, invoices, receipts
- **Purchasing** — suppliers, purchase orders, goods receipts
- **Customers** — customer profiles, loyalty
- **Employees** — staff, roles, attendance
- **Reports** — sales analytics, stock reports, financial summaries
- **Authentication** — JWT-based auth, role-based access (Admin, Manager, Cashier)

## Project Structure

```
grocery-erp/
├── backend/                  # Spring Boot REST API
│   └── src/main/java/com/grocery/erp/
│       ├── config/           # Security, CORS, Swagger config
│       ├── controller/       # REST endpoints
│       ├── service/          # Business logic
│       ├── repository/       # JPA repositories
│       ├── model/            # JavaBean entities (@Entity)
│       ├── dto/              # JavaBean DTOs
│       └── exception/        # Custom exceptions + handlers
├── frontend/                 # React app
│   └── src/
│       ├── components/       # Reusable UI components
│       ├── pages/            # Route pages
│       ├── services/         # API calls (axios)
│       ├── context/          # React context (auth, etc.)
│       └── utils/            # Helpers
└── docs/                     # API docs, ERD, screenshots
```

## Getting Started

### Backend
```bash
cd backend
./mvnw spring-boot:run
```
API runs on `http://localhost:8080`

### Frontend
```bash
cd frontend
npm install
npm start
```
App runs on `http://localhost:3000`

## API Documentation

Swagger UI available at `http://localhost:8080/swagger-ui.html` after running the backend.

## Team

- [Your name]
- [Teammate names]

## License

MIT
