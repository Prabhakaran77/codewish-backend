## 1. Purpose & Scope

This document is a **technical build spec** for a Splitwise-like backend, decomposed into microservices, targeted specifically for **automatic implementation in Java/Spring Boot** (using Gradle, PostgreSQL, and Docker).

It is intentionally **prescriptive and detailed** so that a tool like Cursor can:
- Generate full Spring Boot projects (one per microservice, or Spring Boot multi-module) with consistent structure.
- Create entities, repositories, services, controllers, DTOs, mappers, and tests.
- Wire security, configuration, and basic CI-ready structure with minimal ambiguity.

This spec focuses on:
- **Core domain**: Users, Groups, Members, Expenses, Splits, Balances, Settlements.
- **Core microservices** (MVP):
  - Auth Service
  - User Service
  - Group Service
  - Expense Service
  - Balance & Settlement Service
  - API Gateway

Notification and Reporting services are described briefly for future phases and can be implemented analogously.

---

## 2. Global Technical Decisions

### 2.1 Tech Stack
- **Language**: Java 17+
- **Framework**: Spring Boot 3+
  - `spring-boot-starter-web`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-security`
  - `spring-boot-starter-actuator`
- **Database**: PostgreSQL 14+
- **Build Tool**: Gradle
- **Packaging**:
  - Option A (recommended): One Git repo, **multi-module Gradle**:
    - `auth-service`
    - `user-service`
    - `group-service`
    - `expense-service`
    - `balance-service`
    - `api-gateway`
  - Option B: Separate repos per service, each a standalone Spring Boot project (same conventions).
- **Communication**:
  - Synchronous: REST (JSON over HTTPS).
  - Asynchronous (future): Kafka or RabbitMQ for domain events.

### 2.2 Common Patterns
- **Package naming**: `com.codewish.<serviceName>.<layer>`
  - Example for expense-service:
    - `com.codewish.expense.controller`
    - `com.codewish.expense.service`
    - `com.codewish.expense.model`
    - `com.codewish.expense.repository`
    - `com.codewish.expense.dto`
    - `com.codewish.expense.config`
    - `com.codewish.expense.exception`
- **Controller style**: REST controllers returning DTOs (`@RestController`, `@RequestMapping("/api/v1/...")`).
- **DTOs**: No entity exposure outside the service boundary.
- **Error model**: Shared response format:

```jsonc
{
  "timestamp": "2026-02-02T12:34:56.789Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Amount must be positive",
  "path": "/api/v1/groups/1/expenses"
}
```

- **Authentication**: JWT bearer tokens validated at API Gateway and optionally revalidated in services.
- **ID types**: `Long` for primary keys & references.
- **Currency**: Single currency (e.g., `USD`) for MVP; all amounts `BigDecimal(10,2)`.

### 2.3 Environment & Configuration
- All services accept configuration via environment variables:
  - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
  - `JWT_SECRET`, `JWT_EXPIRATION_MS`.
  - Service-specific ports (e.g., `SERVER_PORT=8081` for auth-service, etc.).
- Standard profiles:
  - `dev`: local dev, show SQL, hbm2ddl `update` or migration tool.
  - `prod`: migrations via Flyway or Liquibase.

---

## 3. Base Domain Contracts (Cross-Service)

All IDs are opaque numeric IDs (`Long`) shared via API contracts:

### 3.1 User (cross-service contract)
Minimal fields that **other services rely on**:

```jsonc
{
  "id": 123,
  "username": "tara",
  "email": "tara@example.com",
  "createdAt": "2026-01-01T10:00:00Z",
  "status": "ACTIVE"
}
```

User Service can have additional internal fields (avatar, preferences, etc.).

### 3.2 Group

```jsonc
{
  "id": 10,
  "name": "Europe Trip 2026",
  "description": "Friends trip across Europe",
  "createdBy": 123,
  "createdAt": "2026-05-01T08:00:00Z",
  "status": "ACTIVE"
}
```

### 3.3 Group Member

```jsonc
{
  "id": 42,
  "groupId": 10,
  "userId": 123,
  "role": "ADMIN", // ADMIN or MEMBER
  "joinedAt": "2026-05-01T08:05:00Z"
}
```

### 3.4 Expense and Split

```jsonc
{
  "id": 501,
  "groupId": 10,
  "description": "Dinner in Rome",
  "amount": 120.00,
  "paidByUserId": 123,
  "expenseDate": "2026-05-02",
  "createdAt": "2026-05-02T21:00:00Z",
  "type": "NORMAL" // NORMAL or SETTLEMENT
}
```

Split:

```jsonc
{
  "id": 1001,
  "expenseId": 501,
  "userId": 234,
  "amountOwed": 30.00
}
```

---

## 4. Auth Service (AS) – Detailed Spec

### 4.1 Responsibilities
- Register users (delegating profile storage to User Service or sharing DB in early phase).
- Authenticate users and issue JWT access tokens.
- Validate and refresh tokens.

### 4.2 Data Model (Auth)

For a **separate Auth DB**, minimal schema:

- `auth_users`:
  - `id` (BIGSERIAL, PK).
  - `username` (VARCHAR(50), unique, not null).
  - `email` (VARCHAR(100), unique, not null).
  - `password_hash` (VARCHAR(255), not null).
  - `status` (VARCHAR(20), default `ACTIVE`).
  - `created_at` (TIMESTAMP, default now()).
  - `updated_at` (TIMESTAMP).

> For MVP, Auth and User Service can share this table, with User Service treating it as its own `users` table.

### 4.3 Entities & Layers
- `AuthUser` entity.
- `AuthUserRepository` extends `JpaRepository<AuthUser, Long>`.
- `AuthService`:
  - `register(RegisterRequest request): AuthUser`.
  - `authenticate(LoginRequest request): AuthTokensResponse`.
- `JwtTokenProvider`:
  - `generateToken(AuthUser user)`.
  - `validateToken(String token)`.
  - `getUserIdFromToken(String token)`.

### 4.4 DTOs

```java
public record RegisterRequest(
    String username,
    String email,
    String password
) {}

public record LoginRequest(
    String usernameOrEmail,
    String password
) {}

public record AuthTokensResponse(
    String accessToken,
    String tokenType,   // "Bearer"
    long expiresInMs
) {}
```

### 4.5 REST Endpoints

All endpoints under `/api/v1/auth`.

1. **POST /register**
   - Request: `RegisterRequest`.
   - Validation:
     - Username/email not already used.
     - Password length >= 8.
   - Responses:
     - `201 Created` with body containing minimal user info and token (optional).
     - `400` for validation errors.

2. **POST /login**
   - Request: `LoginRequest`.
   - Responses:
     - `200 OK` with `AuthTokensResponse`.
     - `401 Unauthorized` if invalid credentials.

3. **GET /me**
   - Secured with `Authorization: Bearer <token>`.
   - Returns:
     - Authenticated user’s ID, username, email.

Error codes to implement as enums:
- `AUTH_INVALID_CREDENTIALS`
- `AUTH_USER_ALREADY_EXISTS`

---

## 5. User Service (US) – Detailed Spec

### 5.1 Responsibilities
- Maintain user profiles and preferences.
- Provide lookup endpoints for other services (by ID, username, email).

### 5.2 Data Model (User Service)

- `users`:
  - `id` (BIGSERIAL, PK).
  - `username` (VARCHAR(50), unique, not null).
  - `email` (VARCHAR(100), unique, not null).
  - `created_at` (TIMESTAMP, default now()).
  - `status` (VARCHAR(20), default `ACTIVE`).
  - `avatar_url` (VARCHAR(255), nullable).
  - `preferences_json` (JSONB, nullable).

### 5.3 Entities & Layers
- `User` entity.
- `UserRepository` extends `JpaRepository<User, Long>`.
- `UserService`:
  - `getCurrentUser(long userIdFromToken)`.
  - `findByUsername(String username)`.
  - `search(String query, Pageable page)`.

### 5.4 REST Endpoints

Base path: `/api/v1/users`.

1. **GET /me**
   - Inputs: Authenticated user (via JWT; userId passed from Gateway).
   - Output:

```jsonc
{
  "id": 123,
  "username": "tara",
  "email": "tara@example.com",
  "avatarUrl": null,
  "status": "ACTIVE"
}
```

2. **PATCH /me**
   - Request body:

```jsonc
{
  "avatarUrl": "https://...",
  "preferences": {
    "emailNotifications": true
  }
}
```

   - Response: updated user profile.

3. **GET /search?query=...**
   - Used by Group Service when adding members.
   - Returns paginated list of users with minimal info.

---

## 6. Group Service (GS) – Detailed Spec

### 6.1 Responsibilities
- Manage groups and group membership.
- Determine which users are members/admins for authorization decisions.

### 6.2 Data Model

- `groups`:
  - `id` (BIGSERIAL, PK).
  - `name` (VARCHAR(100), not null).
  - `description` (TEXT, nullable).
  - `created_by` (BIGINT, not null, userId).
  - `status` (VARCHAR(20), default `ACTIVE`).
  - `created_at` (TIMESTAMP, default now()).

- `group_members`:
  - `id` (BIGSERIAL, PK).
  - `group_id` (BIGINT, not null).
  - `user_id` (BIGINT, not null).
  - `role` (VARCHAR(20), default `MEMBER`).
  - `joined_at` (TIMESTAMP, default now()).
  - Unique constraint `(group_id, user_id)`.

### 6.3 Entities & Repos
- `Group` entity.
- `GroupMember` entity.
- `GroupRepository`, `GroupMemberRepository`.

### 6.4 Service Layer
- `GroupService`:
  - `Group createGroup(CreateGroupRequest, long creatorUserId)`.
  - `void addMember(long groupId, long userId, long actingUserId)`.
  - `void removeMember(long groupId, long userId, long actingUserId)`.
  - `List<GroupDto> getGroupsForUser(long userId)`.

### 6.5 REST Endpoints

Base path: `/api/v1/groups`.

1. **POST /** – create group
   - Request:

```jsonc
{
  "name": "Europe Trip 2026",
  "description": "Trip with college friends"
}
```

   - Authentication: required.
   - Logic:
     - Create `Group` with `createdBy = currentUserId`.
     - Create `GroupMember` for creator with role `ADMIN`.
   - Response: `201 Created` with `GroupDto`.

2. **GET /** – list groups for current user
   - Returns list of `GroupSummaryDto`:

```jsonc
[
  {
    "id": 10,
    "name": "Europe Trip 2026",
    "description": "Trip with college friends",
    "createdBy": 123,
    "createdAt": "2026-05-01T08:00:00Z"
  }
]
```

3. **GET /{groupId}** – group details
   - Includes:
     - Group info.
     - Members.

4. **POST /{groupId}/members** – add member
   - Request:

```jsonc
{
  "usernameOrEmail": "raj@example.com"
}
```

   - Logic:
     - Only `ADMIN` can add.
     - Use User Service to resolve userId.
     - Create `GroupMember` if not exists.

5. **DELETE /{groupId}/members/{userId}**
   - Only `ADMIN` can remove members.

---

## 7. Expense Service (ES) – Detailed Spec (Core of Splitwise Logic)

### 7.1 Responsibilities
- Create and manage expenses within groups.
- Create per-user splits (equal for MVP).

### 7.2 Data Model

- `expenses`:
  - `id` (BIGSERIAL, PK).
  - `group_id` (BIGINT, not null).
  - `description` (VARCHAR(255), not null).
  - `amount` (DECIMAL(10,2), not null).
  - `paid_by_user_id` (BIGINT, not null).
  - `expense_date` (DATE, not null).
  - `created_at` (TIMESTAMP, default now()).
  - `type` (VARCHAR(20), default `NORMAL`, values: `NORMAL`, `SETTLEMENT`).

- `expense_splits`:
  - `id` (BIGSERIAL, PK).
  - `expense_id` (BIGINT, not null).
  - `user_id` (BIGINT, not null).
  - `amount_owed` (DECIMAL(10,2), not null).
  - Unique `(expense_id, user_id)`.

### 7.3 Entities & Repos
- `Expense` entity.
- `ExpenseSplit` entity.
- `ExpenseRepository`, `ExpenseSplitRepository`.

### 7.4 Service Layer
- `ExpenseService`:
  - `ExpenseDto createEqualSplitExpense(CreateExpenseRequest, long currentUserId)`.
  - `ExpenseDto createCustomParticipantsExpense(CreateExpenseRequest, long currentUserId)`.
  - `Page<ExpenseDto> getGroupExpenses(long groupId, Pageable pageable)`.
  - `ExpenseDetailsDto getExpense(long expenseId)`.
  - `SettlementExpenseDto createSettlementExpense(CreateSettlementRequest, long currentUserId)`.

### 7.5 Validation Rules
- Group existence & membership:
  - Validate `groupId` exists via Group Service.
  - Validate `paidByUserId` is in group.
  - Validate all participant `userId`s are group members.
- Amount & date:
  - `amount > 0`.
  - `expenseDate` must not be more than e.g. 1 year in the future.
- Splitting:
  - For equal split across N participants:
    - Calculate share = `amount.divide(BigDecimal.valueOf(N), 2, RoundingMode.HALF_UP)`.

### 7.6 REST Endpoints

Base path: `/api/v1/groups/{groupId}/expenses`.

1. **POST /** – create expense

Request body:

```jsonc
{
  "description": "Dinner in Rome",
  "amount": 120.00,
  "paidByUserId": 123,
  "expenseDate": "2026-05-02",
  "participantIds": [123, 234, 345, 456], // optional, default = all group members
  "splitMode": "EQUAL" // MVP
}
```

Responses:
- `201 Created` with `ExpenseDetailsDto` including splits.
- `400` for validation.
- `403` if user not allowed (not in group).

2. **GET /** – list expenses
- Query params:
  - `page`, `size`, `sort` (default: `expenseDate,desc`).

3. **GET /{expenseId}** – get expense details
- Returns:

```jsonc
{
  "id": 501,
  "groupId": 10,
  "description": "Dinner in Rome",
  "amount": 120.00,
  "paidByUserId": 123,
  "expenseDate": "2026-05-02",
  "createdAt": "2026-05-02T21:00:00Z",
  "splits": [
    { "userId": 123, "amountOwed": 30.00 },
    { "userId": 234, "amountOwed": 30.00 },
    { "userId": 345, "amountOwed": 30.00 },
    { "userId": 456, "amountOwed": 30.00 }
  ]
}
```

4. **POST /settlements** – create settlement expense

Request:

```jsonc
{
  "fromUserId": 234,
  "toUserId": 123,
  "amount": 50.00
}
```

Logic:
- Create an `Expense` of `type = "SETTLEMENT"` with:
  - `paid_by_user_id = fromUserId`.
- Create a single `ExpenseSplit`:
  - `user_id = toUserId`, `amount_owed = amount`.

---

## 8. Balance & Settlement Service (BSS) – Detailed Spec

### 8.1 Responsibilities
- Compute net balances per user in a group.
- Provide recommended settlement transactions.

### 8.2 Data & Dependencies
- BSS can be:
  - **On-demand compute service** that queries Expense Service aggregates via REST.
  - Or maintain its own read-optimized store (future).

MVP: **On-demand**, using REST calls to Expense Service.

### 8.3 Algorithms

#### 8.3.1 Net Balance per User
- For each user U in group G:
  - `totalPaid(U) = SUM(amount) of expenses in G where paidByUserId = U`.
  - `totalOwed(U) = SUM(amountOwed) of splits in G where userId = U`.
  - `balance(U) = totalPaid(U) - totalOwed(U)`.
- Negative balance ⇒ user owes money; positive ⇒ user is owed money.

#### 8.3.2 Settlement Recommendation (Greedy Algorithm)
- Input: `balances` map `<userId, BigDecimal>`.
- Separate:
  - `debtors` = users with balance < 0 (sorted ascending).
  - `creditors` = users with balance > 0 (sorted descending).
- Iterate:
  - While both lists non-empty:
    - Take debtor D, creditor C.
    - `settlementAmount = min(-balance(D), balance(C))`.
    - Record settlement `(from=D, to=C, amount=settlementAmount)`.
    - Adjust balances:
      - `balance(D) += settlementAmount`.
      - `balance(C) -= settlementAmount`.
    - Remove users whose balance reaches ~0 (abs < 0.01).

### 8.4 REST Endpoints

Base path: `/api/v1/groups/{groupId}/balances`.

1. **GET /** – all balances in group

Response:

```jsonc
{
  "groupId": 10,
  "balances": [
    { "userId": 123, "balance": 40.00 },
    { "userId": 234, "balance": -20.00 },
    { "userId": 345, "balance": -20.00 }
  ]
}
```

2. **GET /settlements/recommended**

Response:

```jsonc
{
  "groupId": 10,
  "settlements": [
    {
      "fromUserId": 234,
      "toUserId": 123,
      "amount": 20.00
    },
    {
      "fromUserId": 345,
      "toUserId": 123,
      "amount": 20.00
    }
  ]
}
```

---

## 9. API Gateway – Detailed Spec

### 9.1 Responsibilities
- Expose single external base URL.
- Handle authentication (JWT validation).
- Route to microservices.
- Optionally perform response aggregation for some endpoints (e.g., Group dashboard).

### 9.2 Technology
- Spring Cloud Gateway (preferred), or Spring Boot simple routing with `RestTemplate`/`WebClient`.

### 9.3 Routing Rules (Examples)
- `/api/v1/auth/**` → Auth Service.
- `/api/v1/users/**` → User Service.
- `/api/v1/groups/**` → Group Service.
- `/api/v1/groups/{groupId}/expenses/**` → Expense Service.
- `/api/v1/groups/{groupId}/balances/**` → Balance Service.

### 9.4 Security
- Filter:
  - Extract `Authorization: Bearer <token>`.
  - Validate via Auth Service (or locally using shared `JWT_SECRET`).
  - Add `X-User-Id` header with resolved user ID for downstream services.
- Public endpoints:
  - `/api/v1/auth/**` (login/register).

---

## 10. Project & Code Structure (Multi-Module)

### 10.1 Root Project
- Files:
  - `settings.gradle` – includes all subprojects.
  - `build.gradle` – common dependency versions and plugins.

Example `settings.gradle` snippet:

```groovy
rootProject.name = 'splitwise-backend'
include 'auth-service', 'user-service', 'group-service', 'expense-service', 'balance-service', 'api-gateway'
```

Each submodule:
- Has its own `build.gradle`.
- Has `src/main/java` and `src/main/resources`.
- Uses same Java version and base dependencies with optional service-specific additions.

### 10.2 Common Code Conventions
- Use Lombok annotations (`@Data`, `@Builder`, etc.) if desired, or standard getters/setters.
- Use `@Service`, `@Repository`, `@RestController` per standard Spring idioms.
- All controllers:
  - Use request/response DTOs.
  - Use `@Valid` and Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Positive`).
- All services:
  - Throw custom exceptions (`DomainException`, `NotFoundException`, `ForbiddenException`) with global exception handlers returning standardized error JSON.

---

## 11. Example End-to-End Flow (Create Expense)

**Goal**: User Tara creates an equal-split dinner expense in `Europe Trip 2026` group.

1. **Client → API Gateway**: `POST /api/v1/auth/login` (Auth Service).
2. API Gateway forwards to Auth Service; Auth Service returns JWT.
3. Client stores JWT and calls:
   - `GET /api/v1/groups` – to list groups (via Group Service).
4. Tara picks `groupId = 10` and calls:

```http
POST /api/v1/groups/10/expenses
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "description": "Dinner in Rome",
  "amount": 120.00,
  "paidByUserId": 123,
  "expenseDate": "2026-05-02",
  "participantIds": null,
  "splitMode": "EQUAL"
}
```

5. API Gateway:
   - Validates JWT, extracts userId (123).
   - Forwards to Expense Service with `X-User-Id: 123`.
6. Expense Service:
   - Calls Group Service to verify group & membership.
   - Calls Group Service for list of members (if `participantIds` is null).
   - Validates inputs and creates `Expense` + `ExpenseSplit` entries.
   - Returns `ExpenseDetailsDto`.
7. Client updates UI with new expense.
8. (Optional) Expense Service emits `ExpenseCreated` event; Notification Service sends notifications.
9. When Tara opens the group “balances” screen:
   - Client requests `GET /api/v1/groups/10/balances`.
   - API Gateway routes to Balance Service.
   - Balance Service queries Expense Service (or own store), computes balances, responds.

---

## 12. Testing & Quality

### 12.1 Unit Tests
- For each service:
  - Test service methods with mocked repositories.
  - Test balance and settlement algorithms with scenario-based tests.

### 12.2 Integration Tests
- Use `@SpringBootTest` with Testcontainers for PostgreSQL.
- Verify repository mappings, REST endpoints, and security filters.

### 12.3 API Contracts
- Optional: generate and maintain OpenAPI (Swagger) specs per service using `springdoc-openapi`.
- Ensure spec matches the DTOs and endpoints defined above.

---

## 13. Implementation Checklist (Per Service)

For each microservice (Auth, User, Group, Expense, Balance, API Gateway):
- [ ] Create Gradle module with Spring Boot application class.
- [ ] Configure database connection (application-dev.yml, application-prod.yml).
- [ ] Create entities matching the data model.
- [ ] Create JPA repositories.
- [ ] Create DTOs for input/output.
- [ ] Implement service layer with required domain logic.
- [ ] Implement REST controllers with specified endpoints and validation.
- [ ] Implement exception handling & standardized error response.
- [ ] Secure endpoints with JWT (except explicitly public ones).
- [ ] Add tests (unit + minimal integration).
- [ ] Add Dockerfile and health endpoints (Actuator) for readiness/liveness.

This spec, together with the SRS and PRD, should be sufficient for Cursor (or any engineer) to **generate and implement each microservice end-to-end** for a Splitwise-like backend. 

