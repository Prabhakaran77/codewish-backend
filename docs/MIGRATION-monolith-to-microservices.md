## 1. Purpose

This document describes a **step-by-step migration plan** to move from the existing monolithic Spring Boot application in this repo to the Splitwise-like microservice architecture defined in the SRS, PRD, and TECH-SPEC docs.

It is written so that an engineer (or Cursor) can follow it as a checklist.

---

## 2. Current State Summary (Monolith)

Monolith characteristics:
- Single Spring Boot application (`com.codewish.CodewishApplication`).
- Single PostgreSQL schema (see `src/main/resources/db/split-app.sql`).
- Main domain objects:
  - `User`, `Group`, `GroupMember`, `Expense`, `ExpenseSplit`.
- Business logic:
  - `UserService`, `GroupService`, `ExpenseService`, `BalanceService`.
- Web layer:
  - MVC controllers rendering HTML: `UserController`, `GroupController`, `ExpenseController`, `DashboardController`.

Constraints:
- Session-based login with plaintext password comparison.
- Tight coupling of web views and backend logic.

---

## 3. Target State Summary (Microservices)

- Independent Spring Boot services:
  - Auth Service
  - User Service
  - Group Service
  - Expense Service
  - Balance & Settlement Service
  - API Gateway
- JWT-based authentication.
- Each service has its own database schema.
- Modern REST APIs consumed by clients (web/mobile).

---

## 4. Migration Strategy Overview

Strategy: **Strangler Fig** pattern – introduce new services and gradually route traffic away from the monolith.

Phases:
1. Preparation & hardening.
2. Introduce Auth & User services.
3. Extract Group & Membership.
4. Extract Expense & Balance logic.
5. Retire monolith presentation and/or convert to pure frontend.

Each phase should be:
- Backwards-compatible where possible.
- Supported by automated tests and feature flags.

---

## 5. Phase 1 – Preparation & Hardening

### 5.1 Secure Existing Monolith
- Replace plaintext password checks with:
  - Password hashing (bcrypt or argon2).
  - Migration approach:
    - Add `password_hash` column.
    - For new registrations, store only hash.
    - For existing users, on next login:
      - Verify plaintext.
      - Replace with hashed password.

### 5.2 Add API Layer (Optional)
- Gradually add REST endpoints in the monolith that mirror future microservice APIs:
  - e.g., `/api/v1/groups`, `/api/v1/groups/{id}/expenses`.
- These can be reused/ported when splitting into services.

### 5.3 Introduce Centralized Error Response
- Implement global exception handler returning consistent JSON errors.
- This aligns with the TECH-SPEC error format.

---

## 6. Phase 2 – Auth & User Services

### 6.1 Create Auth Service
- New module or repo `auth-service`:
  - Implement endpoints:
    - `POST /api/v1/auth/register`
    - `POST /api/v1/auth/login`
  - Use the data model defined in TECH-SPEC.
- Database options:
  - Option A (simple): share the current `users` table with monolith temporarily.
  - Option B (clean): create new `auth_users` and plan a data migration.

### 6.2 Create User Service
- New module `user-service`:
  - Own `users` profile table (or reuse `users` table).
  - Implement endpoints:
    - `GET /api/v1/users/me`
    - `PATCH /api/v1/users/me`
    - `GET /api/v1/users/search`

### 6.3 Introduce API Gateway
- Add **API Gateway** in front of the monolith:
  - For `/api/v1/auth/**` and `/api/v1/users/**`, route to new services.
  - Everything else still goes to the monolith.
- Gateway:
  - Validates JWT for protected routes.
  - For now, monolith can still use session auth internally if needed.

### 6.4 Data Migration (If Needed)
- If using new tables:
  - Migrate existing `users` data from monolith DB to Auth/User service DBs.
  - Ensure same `id` values to maintain continuity.

---

## 7. Phase 3 – Group Service Extraction

### 7.1 Create Group Service
- Implement `Group` and `GroupMember` entities and endpoints as per TECH-SPEC.
- Copy relevant logic from `GroupService` in monolith:
  - Group creation and membership management.

### 7.2 Database Migration
- For data owned by Group Service:
  - Migrate `groups` and `group_members` tables from monolith DB to Group Service DB.
  - Preserve IDs.

### 7.3 Routing Changes
- In API Gateway:
  - Route `/api/v1/groups/**` to Group Service.
- In monolith:
  - Controllers which need group data (e.g., HTML views) should call Group Service via REST instead of JPA repositories (temporary).

### 7.4 Decommission Monolith Group Repos
- Once sufficient confidence is gained:
  - Remove or disable `GroupRepository` and `GroupMemberRepository` usage in monolith.

---

## 8. Phase 4 – Expense & Balance Extraction

### 8.1 Create Expense Service
- Implement entities from monolith:
  - `Expense`, `ExpenseSplit`.
- Port logic from `ExpenseService`:
  - `createExpenseWithEqualSplit`.
  - `createExpenseWithCustomSplit`.
  - `createSettlementExpense`.
- Add REST endpoints:
  - `POST /api/v1/groups/{groupId}/expenses`
  - `GET /api/v1/groups/{groupId}/expenses`
  - `GET /api/v1/expenses/{expenseId}`
  - `POST /api/v1/groups/{groupId}/expenses/settlements`

### 8.2 Create Balance & Settlement Service
- Implement algorithm from monolith `BalanceService` with improvements:
  - `getUserBalanceInGroup`.
  - `getGroupSettlements`.
- Connect to Expense Service:
  - On-demand via REST.

### 8.3 Database Migration
- Migrate `expenses` and `expense_splits` from monolith DB to Expense Service DB.

### 8.4 Routing
- In API Gateway:
  - Route `/api/v1/groups/{groupId}/expenses/**` to Expense Service.
  - Route `/api/v1/groups/{groupId}/balances/**` to Balance Service.

### 8.5 Monolith Changes
- Controllers that display dashboards:
  - Replace repository calls with REST calls to Group, Expense, and Balance services.
- Gradually remove `ExpenseRepository`, `ExpenseSplitRepository`, and `BalanceService` logic from monolith once parity is confirmed.

---

## 9. Phase 5 – UI & Monolith Decommissioning

### 9.1 Decide UI Strategy
- Option 1: Keep monolith as thin UI client:
  - It becomes essentially a server-side rendered frontend that talks only to the new services.
- Option 2: Move to SPA or separate frontend project:
  - React/Angular/Vue (or similar) consuming the microservice APIs via API Gateway.

### 9.2 Strip Monolith of Domain Logic
- After all features (auth, users, groups, expenses, balances) have been migrated:
  - Remove JPA entities and repositories.
  - Remove business services.
  - Either:
    - Drop the monolith project entirely, or
    - Keep only UI components that call the new APIs.

---

## 10. Risk Mitigation & Rollback

### 10.1 Feature Flags
- Use feature flags to control:
  - Whether a particular controller calls monolith repos or microservice APIs.
  - Whether API Gateway routes a path to monolith or microservice.

### 10.2 Gradual Cutover
- Start with small user segments or internal users:
  - Route only some traffic to new services.
  - Monitor error rates and performance.

### 10.3 Rollback Strategy
- If a new microservice encounters issues:
  - Switch Gateway routing back to monolith for affected endpoints.
  - Keep data migrations idempotent and reversible when possible.

---

## 11. Migration Checklist (Condensed)

- [ ] Harden passwords in monolith (hashing).
- [ ] Add consistent error responses in monolith.
- [ ] Create Auth Service and User Service; wire via API Gateway.
- [ ] Migrate users if necessary.
- [ ] Create Group Service; migrate groups and members; route `/groups` to it.
- [ ] Update monolith to consume Group Service via REST.
- [ ] Create Expense Service; migrate expenses and splits; route `/expenses` to it.
- [ ] Create Balance Service; implement algorithms and route `/balances` to it.
- [ ] Update monolith UI to use microservice APIs exclusively.
- [ ] Decide UI strategy and decommission or slim down monolith.

