## 1. Purpose

This document gives a **high-level architectural overview** of the Splitwise-like system described in the SRS, PRD, and TECH-SPEC docs.  
It is meant as a quick entry point for engineers and tools (like Cursor) to understand:
- What services exist.
- How they communicate.
- How data flows through the system.

---

## 2. System Context

### 2.1 Actors
- **End User Clients**
  - Web (browser) / Mobile apps.
  - Communicate only with the **API Gateway** via HTTPS.
- **Operations / DevOps**
  - Deploy and monitor services.

### 2.2 External Systems (optional/future)
- Email provider (SMTP / SendGrid-like).
- Push notification provider (APNs / FCM).
- Analytics / logging stack (ELK, Grafana, etc.).

---

## 3. Microservice Landscape (Containers)

Core runtime components:

- **API Gateway**
  - Single external entrypoint.
  - Performs JWT authentication and routing.

- **Auth Service**
  - Handles registration, login, token issuing.
  - Owns `auth_users` (or shares `users` table in early phase).

- **User Service**
  - Owns user profiles and preferences.
  - Provides search and lookup by ID / username / email.

- **Group Service**
  - Owns groups and group membership.
  - Enforces roles (ADMIN vs MEMBER).

- **Expense Service**
  - Owns expenses and expense splits.
  - Responsible for creating normal and settlement expenses.

- **Balance & Settlement Service**
  - Computes per-user balances and recommended settlements.
  - Reads from Expense Service (or its own projection in future).

- **(Future) Notification Service**
  - Listens to domain events and sends notifications.

- **(Future) Reporting Service**
  - Aggregates historical data for reports.

Each service:
- Runs as an independent **Spring Boot** application.
- Has its own **PostgreSQL** schema/database.
- Exposes **REST APIs** (JSON/HTTPS).

---

## 4. High-Level Data Ownership

- **Auth Service**
  - `auth_users` (credentials, password hash, auth status).

- **User Service**
  - `users` (profile and preferences).

- **Group Service**
  - `groups`, `group_members`.

- **Expense Service**
  - `expenses`, `expense_splits`.

- **Balance & Settlement Service**
  - Derived data / cached projections only (optional).

Key rule:  
**Only the owning service writes to its data; other services interact through APIs.**

---

## 5. Request Flows (Sequences)

### 5.1 Login
1. Client → **API Gateway**: `POST /api/v1/auth/login`.
2. Gateway routes to **Auth Service**.
3. Auth Service validates credentials, returns JWT.
4. Gateway forwards JWT back to client.

### 5.2 Create Group
1. Client → Gateway: `POST /api/v1/groups`.
2. Gateway:
   - Validates JWT, extracts `userId`.
   - Passes `X-User-Id` header to **Group Service**.
3. Group Service:
   - Creates `Group` with `createdBy = userId`.
   - Creates `GroupMember` for creator (`role = ADMIN`).
4. Response: new `GroupDto` returned via Gateway.

### 5.3 Add Expense (Equal Split)
1. Client → Gateway: `POST /api/v1/groups/{groupId}/expenses`.
2. Gateway:
   - Validates JWT.
   - Routes to **Expense Service** with `X-User-Id`.
3. Expense Service:
   - Calls **Group Service** to verify:
     - Group exists.
     - Current user is member.
   - Retrieves group members (for equal split or validates given participants).
   - Saves `Expense` and `ExpenseSplit` rows.
   - Returns `ExpenseDetailsDto`.
4. (Future) Expense Service emits `ExpenseCreated` event to **Notification Service**.

### 5.4 View Group Balances
1. Client → Gateway: `GET /api/v1/groups/{groupId}/balances`.
2. Gateway routes to **Balance & Settlement Service**.
3. Balance Service:
   - Fetches necessary aggregates from **Expense Service** (or its own store).
   - Computes balances and returns them.

### 5.5 Recommended Settlements
1. Client → Gateway: `GET /api/v1/groups/{groupId}/balances/settlements/recommended`.
2. Gateway routes to **Balance & Settlement Service**.
3. Balance Service:
   - Uses balances to generate `(fromUser, toUser, amount)` list.
   - Returns list to client.

---

## 6. Deployment Topology

### 6.1 Local Development
- All services can be run via:
  - `docker-compose` (recommended) with:
    - One Postgres instance with multiple schemas or multiple DBs.
    - One container per service.
  - Or directly via Gradle (`./gradlew :auth-service:bootRun`, etc.).

### 6.2 Production (Example)
- Kubernetes (or ECS) cluster with:
  - Deployment per service (auth, user, group, expense, balance, gateway).
  - ConfigMaps/Secrets for environment variables.
  - Horizontal Pod Autoscaler on high-traffic services (e.g., Expense, Gateway).
  - Ingress controller exposing only **API Gateway** externally.
- Centralized:
  - Logging (e.g., ELK).
  - Metrics (Prometheus + Grafana).

---

## 7. Cross-Cutting Concerns

### 7.1 Security
- All external traffic goes through API Gateway over **HTTPS**.
- JWT-based auth:
  - Gateway validates tokens (shared secret or public key).
  - Gateway injects `X-User-Id` into downstream requests.
- Services perform **authorization** based on:
  - Membership (Group Service).
  - Role (ADMIN vs MEMBER).

### 7.2 Observability
- Each service exposes:
  - `/actuator/health` for readiness/liveness.
  - `/actuator/metrics` for metrics.
- Distributed tracing:
  - Correlation IDs included in headers (`X-Request-Id`, `X-Correlation-Id`) and logs.

### 7.3 Error Handling
- Consistent error response format across services.
- Gateway can map downstream errors into a consistent client-facing schema.

---

## 8. Alignment with Existing Monolith

The current monolith already implements:
- Users, Groups, GroupMembers, Expenses, ExpenseSplits.
- Basic balance and settlement logic in `BalanceService`.

This architecture:
- Extracts that logic into separate services.
- Keeps the same **core domain concepts** but assigns them clear ownership boundaries.
- Allows gradual migration: routes can be moved from monolith to gateway-backed microservices incrementally.

