## 1. Introduction

### 1.1 Purpose
This Software Requirements Specification (SRS) describes the functional and non-functional requirements for a Splitwise-like expense sharing application, designed as a microservice-based system.  
It is based on the existing monolithic Spring Boot backend currently implemented in this repository (users, groups, expenses, splits, and settlements) and generalizes it into a scalable, secure, and extensible architecture.

### 1.2 Scope
The system allows users to:
- Register and authenticate.
- Create and manage groups (e.g., trips, households, events).
- Add members to groups.
- Create expenses in groups (equal split or custom participants).
- Track how much each member owes or is owed.
- View recommended settlements and record them.

The SRS focuses on:
- Converting the existing monolith into a set of microservices.
- Defining behavior and requirements for the core services needed to support a Splitwise-style app.
- Defining APIs and data contracts between services at a high level.

### 1.3 Definitions, Acronyms, and Abbreviations
- **User**: A registered person using the application.
- **Group**: A collection of users (e.g., friends on a trip) among whom expenses are shared.
- **Expense**: A monetary transaction recorded within a group.
- **Split**: The allocation of an expense amount across users.
- **Settlement**: A transaction (actual or virtual) indicating that debt between users has been settled.
- **Microservice**: Independently deployable service with its own data store and bounded context.
- **API Gateway**: Entry point to the backend that routes requests to microservices.
- **SRS**: Software Requirements Specification.

### 1.4 References
- Current monolith codebase (this repository):
  - Domain models: `User`, `Group`, `GroupMember`, `Expense`, `ExpenseSplit`.
  - Services: `UserService`, `GroupService`, `ExpenseService`, `BalanceService`.
  - Controllers: `UserController`, `GroupController`, `ExpenseController`, `DashboardController`.
  - DB schema: `split-app.sql`.

### 1.5 Overview
This document first summarizes the current behavior visible in the monolithic implementation, then defines the overall microservice architecture and detailed functional/non-functional requirements. It is intended for developers, architects, QA engineers, and product stakeholders.

---

## 2. Overall Description

### 2.1 Product Perspective
The existing codebase is a **monolithic Spring Boot application**:
- Spring MVC controllers render HTML templates.
- JPA entities map to PostgreSQL tables (`users`, `groups`, `group_members`, `expenses`, `expense_splits`).
- Services encapsulate domain logic:
  - `UserService` handles registration, login, and basic user queries.
  - `GroupService` manages groups and membership.
  - `ExpenseService` records expenses, splits, and settlements.
  - `BalanceService` computes per-user balances and recommended settlements.

Target microservice-based architecture:
- **API Gateway / Edge Service** (REST/GraphQL) – single external entrypoint.
- **Auth & Identity Service** – login, JWT/OAuth2, profile, credentials.
- **User Profile Service** – user details, contact info, preferences.
- **Group Service** – groups & membership.
- **Expense Service** – expenses and splits.
- **Balance/Settlement Service** – balance computation & recommended settlements.
- **Notification Service** – email/push/real-time notifications.
- **Reporting/Analytics Service** – summaries, charts, exports.

Each microservice will own its own database schema and communicate over HTTP/REST (and optionally async messaging later).

### 2.2 Product Functions (High-level)
- **User Management**
  - Register, authenticate, manage profile.
  - Manage password (change/reset).
  - View list of groups and balances.

- **Group Management**
  - Create group, edit details, archive group.
  - Invite users to group (by username/email) and manage membership.

- **Expense Management**
  - Add an expense to a group, specifying:
    - Description, amount, date.
    - Payer (paidByUserId).
    - Participants (all group members or selected subset).
    - Split strategy (equal, percentage, fixed amounts – equal is currently implemented).
  - Edit or delete expenses (subject to permissions and audit rules).
  - View expense history for a group.

- **Balances & Settlements**
  - Compute each user’s net balance per group.
  - Compute recommended settlement transactions between users in a group.
  - Record settlement operations as special “settlement expenses”.

- **Notifications**
  - Notify users on new expenses where they are participants.
  - Notify users on settlement requests/confirmations.

### 2.3 User Classes and Characteristics
- **End User**
  - Non-technical.
  - Uses mobile or web client to manage shared expenses.
  - Cares about ease of use, accuracy, transparency.
- **Administrator (Ops)**
  - Technical/operations staff.
  - Manages system configuration, monitors logs and metrics.
- **Support**
  - Support staff can investigate user accounts, groups, and disputes.

### 2.4 Operating Environment
- Backend:
  - Java 17+ (consistent with existing Spring Boot stack).
  - Spring Boot (Web, Data JPA, possibly Spring Cloud components).
  - PostgreSQL as primary relational DB for core services.
  - Docker and container orchestrator (Docker Compose locally; Kubernetes/ECS in production).
- Clients:
  - Web SPA or server-rendered UI.
  - Mobile clients (future).

### 2.5 Design and Implementation Constraints
- Must maintain correct balances and splits (no rounding errors beyond 2 decimal places).
- GDPR-like data privacy expectations for user data (delete/deactivate upon request).
- Authentication and authorization must protect user data across services.
- Backward-compatible API evolution where possible (versioning).

### 2.6 Assumptions and Dependencies
- Single base currency (e.g., USD) initially; multi-currency may be added later.
- Single region deployment initially.
- Email/push infrastructure available (e.g., SMTP or third-party provider) for notifications.

---

## 3. System Features (Functional Requirements)

### 3.1 Auth & Identity Service

**Description**  
Manages user authentication and token issuance. The existing `UserController` handles session-based login; in the microservice architecture this will be refactored to token-based (e.g., JWT).

**FR-AUTH-1**: User Registration  
- The system shall allow a new user to register with:
  - Unique username.
  - Unique email.
  - Password meeting security requirements.
- The system shall validate that username and email are unique (similar to `UserService.existsByUsername/existsByEmail`).

**FR-AUTH-2**: User Login  
- The system shall allow a registered user to authenticate with username/email and password.
- On success, the system shall return an access token (JWT or opaque token).
- On failure, the system shall return an error without revealing whether username or password is incorrect.

**FR-AUTH-3**: Token Validation  
- The system shall validate tokens on every request to protected endpoints (e.g., groups, expenses).
- The system shall support token expiration and refresh.

**FR-AUTH-4**: Password Management  
- The system shall allow users to change their password.
- The system shall support a password-reset flow via email.

### 3.2 User Profile Service

**FR-USER-1**: View Profile  
- The system shall allow authenticated users to retrieve their profile (username, email, created date, preferences).

**FR-USER-2**: Update Profile  
- The system shall allow users to update non-auth fields (e.g., display name, avatar, notification preferences).

**FR-USER-3**: User Lookup  
- The system shall provide endpoints to look up a user by username/email (for inviting to groups), with appropriate rate limiting.

### 3.3 Group Service

Based on existing `Group`, `GroupMember`, and `GroupService`.

**FR-GROUP-1**: Create Group  
- Authenticated users shall be able to create a group with:
  - `name` (required).
  - `description` (optional).
- The creator shall be stored as `createdBy` and automatically added as a group member (as in `createGroup`).

**FR-GROUP-2**: Manage Group Details  
- Group admins (initially the creator) shall be able to update:
  - `name`.
  - `description`.
- Optionally archive/close a group so that no new expenses can be created.

**FR-GROUP-3**: Manage Members  
- Group admins shall be able to add a member by username or email:
  - If user exists, add them to the group.
  - If not, optionally send them an invite or show an error (existing `GroupController.addMember` uses username).
- The system shall prevent duplicate membership (`unique(user_id, group_id)` and `existsByUserIdAndGroupId` behavior).
- Admins shall be able to remove a user from the group (similar to `removeUserFromGroup`).

**FR-GROUP-4**: View Groups  
- Users shall be able to list:
  - Groups they created.
  - Groups they belong to (existing `GroupRepository.findGroupsByUserId`).

**FR-GROUP-5**: Group Dashboard  
- For each group, users shall be able to see:
  - Basic group info.
  - List of members.
  - List of recent expenses.
  - Their personal balance in the group (via Balance Service).

### 3.4 Expense Service

Based on existing `Expense`, `ExpenseSplit`, and `ExpenseService`.

**FR-EXP-1**: Create Equal-Split Expense  
- The system shall allow creating an expense with:
  - `groupId`, `description`, `amount`, `paidByUserId`, `expenseDate`.
- The system shall:
  - Validate `groupId` exists.
  - Validate `paidByUserId` is a member of the group.
  - Equally split `amount` among all group members (similar to `createExpenseWithEqualSplit`).
  - Create a row in `expenses`.
  - Create `expense_splits` rows for each member with `amountOwed = amount / memberCount` (rounded to 2 decimals).

**FR-EXP-2**: Create Custom-Participant Expense  
- The system shall allow specifying a subset of participants (`participantIds`).
- It shall:
  - Validate participants are members of the group.
  - Equally split `amount` among only those participants (as in `createExpenseWithCustomSplit`).

**FR-EXP-3**: Advanced Split Modes (Future)  
- The system shall support additional split modes:
  - By percentage.
  - By exact amount per user.
  - By shares/weights.
- Validation shall ensure total allocated amount equals the expense amount.

**FR-EXP-4**: Edit/Delete Expense  
- The system shall allow the payer or group admin to edit an expense, with audit trail.
- The system shall allow soft-deleting expenses (mark deleted) instead of physical deletion, recomputing balances accordingly.

**FR-EXP-5**: View Group Expenses  
- Users shall be able to get a paginated list of expenses for a group (similar to `getGroupExpenses`).
- Users shall be able to drill down into expense details, including per-user splits (`getExpenseSplits`).

### 3.5 Balance & Settlement Service

Based on existing `BalanceService` and settlement logic in `ExpenseService.createSettlementExpense`.

**FR-BAL-1**: Compute User Balance in Group  
- The system shall compute net balance per user in a group as:
  - `totalPaid` (sum of expenses paid by the user in that group).
  - `totalOwed` (sum of amounts owed by the user across splits in that group).
  - `balance = totalPaid - totalOwed`.
- Negative balance ⇒ user owes money; positive ⇒ user is owed money.

**FR-BAL-2**: Compute All Balances in Group  
- The system shall provide an endpoint to return net balances for all members of a group.

**FR-BAL-3**: Compute Recommended Settlements  
- The system shall compute recommended settlement transactions among members using a deterministic algorithm:
  - Identify debtors (negative balances) and creditors (positive balances).
  - Generate a set of (fromUser, toUser, amount) pairs that, if executed, reduce all balances to zero or near-zero (similar to `getGroupSettlements`).

**FR-BAL-4**: Record Settlements  
- The system shall allow creating a “settlement” transaction that adjusts balances:
  - Implemented as a special expense (existing approach) or a separate settlement entity.
  - E.g., `createSettlementExpense(groupId, fromUserId, toUserId, amount)`.

### 3.6 Notification Service

**FR-NOTIF-1**: Expense Notifications  
- When an expense is created, all participants shall receive a notification (email/push/in-app).

**FR-NOTIF-2**: Settlement Notifications  
- When a settlement is requested or recorded, the involved users shall receive notifications.

**FR-NOTIF-3**: Preference Management  
- Users shall be able to opt in/out of specific notification types.

### 3.7 Reporting & Analytics Service

**FR-REP-1**: Per-Group Summary  
- The system shall provide summaries per group:
  - Total spent.
  - Total owed per user.
  - Time-based charts (monthly, weekly).

**FR-REP-2**: User-Level Summary  
- The system shall provide summaries per user across all groups (e.g., total paid, total owed, top groups).

---

## 4. External Interface Requirements

### 4.1 User Interfaces
- Web UI and mobile apps consume backend APIs.
- Dashboard views:
  - Global dashboard: list of groups and net balances.
  - Group dashboard: list of expenses, members, and suggested settlements.

### 4.2 APIs
- All microservices shall expose REST APIs (JSON over HTTPS).
- API Gateway shall:
  - Handle authentication/authorization.
  - Route requests to appropriate services.
  - Provide request/response logging and rate limiting.

### 4.3 Hardware Interface
- Standard cloud infrastructure; no special hardware interfaces required.

### 4.4 Software Interface
- PostgreSQL or compatible RDBMS for persistence.
- SMTP / third-party email API for notifications.
- Message broker (Kafka/RabbitMQ) may be introduced for asynchronous communication.

---

## 5. Non-Functional Requirements

### 5.1 Performance Requirements
- The system shall support:
  - 1k–10k concurrent active users in initial phase.
  - Expense creation with p95 latency < 300 ms under normal load.
  - Group dashboard p95 latency < 500 ms (includes balance computation).
- Balance computation algorithms shall be efficient enough to handle groups with at least 100 members and 10k expenses.

### 5.2 Security Requirements
- Passwords must be stored hashed with a strong algorithm (e.g., bcrypt/argon2).
- All external traffic must use HTTPS/TLS.
- JWT tokens must be signed and verified by the Auth service.
- User A must not be able to access data (groups, expenses) of User B without proper membership/authorization.
- Input validation and sanitization must be performed on all external inputs.

### 5.3 Reliability & Availability
- Target initial availability: 99.5% or higher.
- In case of partial microservice failure:
  - Core read operations (view balances, expenses) should remain functional if possible.
  - Non-core services (notifications, reporting) may degrade gracefully.

### 5.4 Maintainability
- Each microservice shall have:
  - Clear bounded context.
  - Independent CI/CD pipeline.
  - Linting, tests, and documentation.
- Log correlation IDs across services.

### 5.5 Scalability
- Horizontal scaling at service level (multiple instances).
- Each microservice can be scaled independently based on load.

### 5.6 Audit & Compliance
- All changes to financial data (expenses, splits, settlements) must be auditable:
  - Timestamps.
  - User who performed action.
  - Old and new values (or event log).

---

## 6. Data Requirements (Conceptual Model)

Entities (generalized from current schema):
- **User**: `id`, `username`, `email`, `password_hash`, `created_date`, `status`, `preferences`.
- **Group**: `id`, `name`, `description`, `created_by`, `created_date`, `status`.
- **GroupMember**: `id`, `user_id`, `group_id`, `joined_date`, `role`.
- **Expense**: `id`, `group_id`, `description`, `amount`, `paid_by_user_id`, `expense_date`, `created_date`, `type`.
- **ExpenseSplit**: `id`, `expense_id`, `user_id`, `amount_owed`.
- **Settlement** (optional separate entity): `id`, `group_id`, `from_user_id`, `to_user_id`, `amount`, `date`, `status`.

Each microservice will maintain its own schema; foreign references across services will be by IDs, not direct foreign keys.

---

## 7. Microservice Decomposition (High-Level)

### 7.1 Services List
- Auth Service
- User Service
- Group Service
- Expense Service
- Balance/Settlement Service
- Notification Service
- Reporting Service
- API Gateway

### 7.2 Inter-Service Interactions (Examples)
- Client → API Gateway → Auth Service (login, tokens).
- Client → API Gateway → Group Service (CRUD groups, membership).
- Client → API Gateway → Expense Service (CRUD expenses) → emits events to:
  - Balance Service (for recalculations) – or Balance Service recomputes on demand from Expense DB.
  - Notification Service (for expense/settlement notifications).

### 7.3 Migration from Monolith
- Phase 1: Extract Auth & User into dedicated service (keeping DB schema close to current `User` model).
- Phase 2: Extract Group + Membership.
- Phase 3: Extract Expense and Balance/Settlement logic.
- Phase 4: Add Notifications and Reporting.

---

## 8. Future Enhancements

- Multi-currency and FX conversion.
- Recurring expenses and reminders.
- OCR for bill scanning.
- Real-time collaboration (websockets).
- Open banking integration to link real payments with recorded settlements.

