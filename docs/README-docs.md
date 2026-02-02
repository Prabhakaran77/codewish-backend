## Splitwise-Like Backend – Documentation Index

This folder contains the main documents that describe and specify the Splitwise-style expense-sharing system and the migration from the current monolith.

Use this file as a **starting point** when exploring or auto-generating code.

---

## 1. Document List & When to Use What

- **`SRS-splitwise-microservice.md`**
  - Software Requirements Specification.
  - Functional + non-functional requirements.
  - System features, user roles, and behavior.
  - Use this to understand **what the system must do**.

- **`PRD-splitwise-microservice.md`**
  - Product Requirements Document.
  - Vision, personas, use cases, KPIs, and roadmap.
  - Use this for **product-level decisions and prioritization**.

- **`TECH-SPEC-splitwise-microservices.md`**
  - Detailed technical build spec.
  - Service boundaries, DB schemas, REST contracts, DTOs, algorithms, Gradle multi-module layout.
  - Primary source for **code generation and implementation**.

- **`ARCHITECTURE-splitwise-microservices.md`**
  - High-level architecture & system context.
  - Service landscape, data ownership, request flows, deployment topology.
  - Use this to quickly understand **how all services fit together**.

- **`MIGRATION-monolith-to-microservices.md`**
  - Step-by-step migration plan from current monolith to the microservice architecture.
  - Phased approach, risks, and checklists.
  - Use this to **incrementally move** from the existing codebase to the new design.

---

## 2. Recommended Reading Order

1. `PRD-splitwise-microservice.md` – understand product goals and use cases.
2. `SRS-splitwise-microservice.md` – understand detailed requirements.
3. `ARCHITECTURE-splitwise-microservices.md` – grasp high-level system design.
4. `TECH-SPEC-splitwise-microservices.md` – dive into implementation details.
5. `MIGRATION-monolith-to-microservices.md` – plan the transition from the existing monolith.

---

## 3. Using These Docs with Cursor

- To **build new services** from scratch:
  - Use `TECH-SPEC-splitwise-microservices.md` + `ARCHITECTURE-splitwise-microservices.md` as primary guides.
- To **evolve the current monolith**:
  - Use `MIGRATION-monolith-to-microservices.md` as a checklist.
- To ensure features match expectations:
  - Cross-check against `SRS-splitwise-microservice.md` and `PRD-splitwise-microservice.md`.

