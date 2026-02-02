## 1. Product Overview

### 1.1 Product Vision
Build a **Splitwise-like, multi-platform expense-sharing product** that makes it effortless for small groups (friends, families, roommates, travel buddies) to track shared expenses and settle up fairly, backed by a scalable microservice architecture.

### 1.2 Goals
- **G1 – Accuracy & Trust**: Users must trust that balances and settlements are always correct.
- **G2 – Simplicity**: Minimize friction to record expenses and understand “who owes whom”.
- **G3 – Scalability**: Architect the backend as microservices so we can scale by service and evolve features independently.
- **G4 – Extensibility**: Make it easy to add features such as advanced splits, multi-currency, and integrations (e.g., payments).

### 1.3 Non-Goals (for initial releases)
- Direct payment processing between users (e.g., credit card wallets). We’ll link out to payment apps or record settlements manually.
- Complex enterprise multi-tenant billing or corporate expense policies.
- High-frequency trading-like performance; we’re optimizing for typical consumer group sizes (< 100 members per group).

---

## 2. Target Users & Use Cases

### 2.1 Personas
- **P1 – Traveler Tara**
  - 27, frequently travels with friends.
  - Pain point: constantly calculating who paid for what during trips.
  - Needs: quick expense entry on the go, clear trip summary at the end.

- **P2 – Roommate Raj**
  - 25, shares an apartment.
  - Pain point: splitting rent, utilities, groceries without conflicts.
  - Needs: recurring expenses, monthly reports, fair splits.

- **P3 – Organizer Olivia**
  - 32, organizes events/outings.
  - Pain point: chasing everyone for their share afterwards.
  - Needs: ability to add many participants, view recommended settlement paths, reminders.

### 2.2 Primary Use Cases
- **UC1 – Create Account & Login**
  - User signs up with email & password, verifies account, logs in.

- **UC2 – Create Group (Trip/Household/Event)**
  - User creates a group (e.g., “Europe Trip 2026”) and invites members.

- **UC3 – Add Expense**
  - User records an expense:
    - “Dinner at Rome”, 120 USD, paid by Tara, split among 4 people equally.

- **UC4 – View Balances**
  - Any group member sees:
    - “Raj owes Tara 60, Olivia owes Tara 20, …”.

- **UC5 – Settle Up**
  - User sees recommended settlement transactions and marks them as “settled” after paying via cash or another payment app.

- **UC6 – Notifications**
  - Users get notified when:
    - They are added to a group.
    - They are added as participant to an expense.
    - Someone records a settlement involving them.

---

## 3. High-Level Feature Set

### 3.1 MVP Feature Set (Phase 1)
- Account:
  - Email/password sign-up and login (JWT-based).
  - Basic profile (username, email).
- Groups:
  - Create and edit groups.
  - Add/remove members (by username/email).
  - View groups you belong to.
- Expenses:
  - Create expenses with equal split among all group members or selected participants.
  - List expenses per group.
  - View expense details including splits.
- Balances:
  - Per-group balance for current user.
  - Recommended one-step settlements among members.
- Settlements:
  - Record a settlement (one user pays another).
  - Adjust balances accordingly.

### 3.2 Phase 2+ Enhancements
- Advanced splits (percentage, exact amounts, shares).
- Rich notifications (email + push + in-app).
- Reporting & analytics (per-month, per-category).
- Multi-currency groups.
- Archived/closed groups and read-only history.

---

## 4. Microservice Product Breakdown

> This section maps product features to backend services. Details match the SRS but focus on what needs to be built, not low-level implementation.

### 4.1 Auth Service (AS)

**Responsibilities**
- User registration, login, logout.
- Token issuance and validation.
- Password management.

**Key API capabilities**
- `POST /auth/register` – create account.
- `POST /auth/login` – obtain access + refresh tokens.
- `POST /auth/token/refresh` – refresh access token.
- `POST /auth/password/forgot` – trigger reset email.
- `POST /auth/password/reset` – reset with token.

**Product requirements**
- Passwords must be stored using secure hashing (bcrypt/argon2).
- Rate limiting on login and registration endpoints.
- Email verification is desirable (Phase 2).

### 4.2 User Service (US)

**Responsibilities**
- Store user profiles and preferences.
- Provide lookups for invitations and mentions.

**Key API capabilities**
- `GET /users/me` – current user profile.
- `PATCH /users/me` – update profile fields.
- `GET /users/search?query=` – search by username/email.

**Product requirements**
- Provide stable `userId` to be used across all other services.
- Manage notification preferences (email/push toggles).

### 4.3 Group Service (GS)

**Responsibilities**
- CRUD for groups and group membership.
- Enforce membership rules and roles.

**Key API capabilities**
- `POST /groups` – create group.
- `GET /groups` – list groups for current user.
- `GET /groups/{groupId}` – group details (meta + member list).
- `POST /groups/{groupId}/members` – add member.
- `DELETE /groups/{groupId}/members/{userId}` – remove member.

**Product requirements**
- Only group admins can:
  - Edit group metadata.
  - Add/remove members.
- Track `role` per membership (`ADMIN`, `MEMBER`).
- Emit events when:
  - Group created.
  - Member added/removed (for notifications).

### 4.4 Expense Service (ES)

**Responsibilities**
- Record expenses for groups.
- Store splits per participant.

**Key API capabilities (MVP)**
- `POST /groups/{groupId}/expenses`
  - Request:
    - `description`, `amount`, `expenseDate`, `paidByUserId`, `participantIds` (optional for custom subset).
    - `splitMode` (initially `EQUAL_ALL` or `EQUAL_PARTICIPANTS`).
- `GET /groups/{groupId}/expenses?cursor=...` – list expenses (paginated).
- `GET /expenses/{expenseId}` – expense details with splits.

**Product requirements**
- Strict validation:
  - Group must exist.
  - Payer and participants must be group members.
  - Amount must be positive and within reasonable max.
- Splits:
  - For equal splits, show each user’s share with two decimals; keep internal consistency (handle rounding).
- Emit domain events:
  - `ExpenseCreated`.
  - `ExpenseUpdated` / `ExpenseDeleted` (future).

### 4.5 Balance & Settlement Service (BSS)

**Responsibilities**
- Compute and expose balances for groups/users.
- Recommend settlement transactions.
- Record settlement events.

**Key API capabilities**
- `GET /groups/{groupId}/balances`
  - Returns:
    - For each member: `userId`, `balance`.
- `GET /groups/{groupId}/settlements/recommended`
  - Returns:
    - List of suggested settlements: `fromUserId`, `toUserId`, `amount`.
- `POST /groups/{groupId}/settlements`
  - Records that a settlement was performed.

**Product requirements**
- Settlement algorithm should:
  - Minimize number of transactions, when feasible.
  - Be deterministic so users see consistent suggestions.
- Recording settlement should:
  - Update effective balances (e.g., via special “settlement expense” or dedicated settlement entity).
  - Trigger notifications to involved users.

### 4.6 Notification Service (NS)

**Responsibilities**
- Send and record notifications for key events.

**Key notification triggers**
- Member added to group.
- Expense created where user is participant.
- Settlement recorded involving user.

**Key API capabilities**
- Internal/event-driven:
  - Consume domain events from GS, ES, BSS.
- External:
  - `GET /notifications` – list recent notifications (for in-app feed).
  - `PATCH /notifications/preferences` – update preferences.

**Product requirements**
- Start with email notifications; add push/in-app later.
- Provide a user-facing way to mute/limit notifications.

### 4.7 Reporting Service (RS) – Later Phase

**Responsibilities**
- Aggregate data for long-running analytics and exports.

**Example features**
- Monthly per-group report.
- Per-user yearly spending summary.

---

## 5. User Flows

### 5.1 Onboarding Flow
1. User lands on signup page.
2. Enters email, username, password.
3. Receives confirmation (Phase 1: immediate; Phase 2: email verification).
4. Logged in and taken to dashboard (list of groups).

### 5.2 Create Group & Add Members
1. From dashboard, user clicks “Create group”.
2. Enters name and optional description.
3. Group created; creator is admin and auto-added as member.
4. User adds members via username or email search.
5. Added members receive notifications and see the group in their list.

### 5.3 Add Expense
1. User opens a group.
2. Clicks “Add expense”.
3. Fills in:
   - Description, amount, who paid, date.
   - Optional participants (defaults to all).
4. On save:
   - Expense Service stores expense and splits.
   - Balance Service updates or recalculates balances.
   - Notification Service notifies participants.
5. Group detail view updates with the new expense and balances.

### 5.4 View Balances & Settle Up
1. User opens “Settlements” or “Balances” tab for group.
2. Balance & Settlement Service returns:
   - Each user’s net balance.
   - Recommended settlement transactions.
3. User selects a recommended settlement and pays via external method (e.g., UPI, PayPal).
4. User records settlement via “Mark as Settled”.
5. Balances recompute; the recommendation list updates.

---

## 6. Success Metrics & KPIs

### 6.1 Product KPIs
- D1: **30-day retention** – % of users who create at least 1 expense in a group and return within 30 days.
- D2: **Average number of expenses per active group** – target growth over time.
- D3: **Average time to settle** – time between first expense and final settlement in a group.
- D4: **User satisfaction** – via in-app rating or NPS.

### 6.2 Reliability & Performance Metrics
- Error rate < 1% for expense creation/API calls.
- p95 latency:
  - Expense creation < 300 ms.
  - Group balances page < 500 ms.
- Uptime: 99.5% in initial production phase.

---

## 7. Constraints & Risks

### 7.1 Technical Constraints
- Must run on Java/Spring-based stack to leverage existing code and team expertise.
- Must support Postgres as primary data store.

### 7.2 Product Risks
- Complexity of explaining balances and settlements to non-technical users.
- Misunderstandings if users don’t trust the calculations (need clear UX and explanations).
- Legal/privacy expectations around financial data (must provide data export and deletion).

---

## 8. Implementation Phasing (Roadmap)

### Phase 0 – Current Monolith Analysis (Done)
- Existing features:
  - Users, groups, group membership.
  - Expenses with equal and custom participant splits.
  - Basic balance and settlement suggestion logic.
- Gaps:
  - No robust auth (password hashing, tokens).
  - No separate microservices or API gateway.
  - Limited UI/UX and notifications.

### Phase 1 – MVP Microservice Backend
- Deliverables:
  - Auth Service (JWT-based).
  - User Service.
  - Group Service.
  - Expense Service (with equal and custom splits).
  - Balance & Settlement Service.
  - API Gateway with routing and auth.
- Migration plan:
  - Reuse domain models/logic from monolith where possible.
  - Gradually route frontend from monolith endpoints to gateway.

### Phase 2 – Notifications & Reporting
- Deliverables:
  - Notification Service with email integration.
  - Reporting Service with basic summaries.
  - Preference management UI and APIs.

### Phase 3 – Advanced Features
- Advanced splits, multi-currency, recurring expenses.
- Rich analytics and export (PDF/CSV).

---

## 9. UX & UI Considerations (High-Level)

> Detailed wireframes are out of scope, but we capture must-have UX elements.

- Dashboards:
  - Home dashboard: list of groups with quick summary: “You owe 40 in 2 groups”.
  - Group dashboard: total spent, what you owe/are owed, recent expenses.
- Expense creation:
  - Single, simple form with defaults:
    - Today’s date.
    - Current user as payer.
    - All group members selected by default.
  - Ability to toggle advanced splits (Phase 2+).
- Balances:
  - Clearly distinguish:
    - “You owe X” vs “You should receive Y”.
  - Clear explanation of how a settlement recommendation was derived.

---

## 10. Dependencies & Integrations

- Email provider (SMTP or third-party like SendGrid).
- Optional: push notification provider (Firebase/APNs) for mobile apps.
- Optional: analytics tool (e.g., Mixpanel, GA) for behavioral metrics.

---

## 11. Open Questions

- Should we allow non-registered users to be added as “placeholder” members (emails only) and later claim accounts?
- Do we need categories/tags for expenses in MVP?
- Should we support multiple currencies per group from day one, or keep a single base currency initially?

