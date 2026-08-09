# Relivio — Disaster Relief Management System

A microservices-based platform for coordinating disaster relief: incident reporting, relief
requests, volunteer assignment, resource allocation, shelter management, and notifications —
built with Spring Boot on the backend and React + TypeScript on the frontend.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Example Flow: Reporting an Incident](#example-flow-reporting-an-incident)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Key Business Rules](#key-business-rules)
- [Project Structure](#project-structure)
- [Tests](#tests)
- [Out of Scope](#out-of-scope)

## Overview

Relivio coordinates the full lifecycle of a disaster response:

1. An **incident** is reported and verified.
2. Citizens raise **relief requests** tied to that incident.
3. **Volunteers** are matched and assigned to fulfill requests.
4. **Resources** (supplies) and **shelters** are allocated.
5. Every state change fires a **notification** to the relevant user.

Each concern above is an independently deployable Spring Boot microservice with its own
PostgreSQL database, registered with a Eureka service registry, and exposed to the frontend
through a single Spring Cloud API Gateway.

## Architecture

```
                         ┌─────────────────┐
                         │  React Frontend  │
                         │   (Vite, :5173)  │
                         └────────┬─────────┘
                                  │  http://localhost:8080
                         ┌────────▼─────────┐
                         │   API Gateway     │
                         │     (:8080)       │
                         └────────┬─────────┘
                                  │ lb:// (via Eureka)
        ┌───────────┬────────────┼────────────┬───────────┐
        ▼           ▼            ▼            ▼           ▼
   Incident     ReliefReq    Volunteer     Resource    Notification
   Service      Service      Service       Service      Service
   (:8081)      (:8082)      (:8083)       (:8084)      (:8085)
        │            │            │             │             │
   incident_db  relief_      volunteer_    resource_     notification_
                request_db      db             db             db

                         ┌──────────────────┐
                         │   Eureka Server   │
                         │      (:8761)      │
                         └──────────────────┘
```

Services discover each other through **Eureka** and call one another directly via
**Feign clients** (declarative HTTP interfaces) for cross-service workflows — e.g. Incident
Service notifies admins through Notification Service, and checks open relief requests through
Relief Request Service before allowing an incident to be resolved.

## Services

| Service            | Port | Route prefix                            | Database             | Responsibility |
| ------------------ | ---- | ---------------------------------------- | --------------------- | -------------- |
| Eureka Server      | 8761 | — (registry dashboard)                   | —                      | Service discovery |
| API Gateway        | 8080 | `/api/**`                                | —                      | Routing, CORS, single entry point |
| Auth Service        | —    | `/api/auth/**`                           | —                      | User registration/login (not yet wired into gateway routes/frontend) |
| Incident Service   | 8081 | `/api/incidents/**`                      | `incident_db`          | Incident lifecycle & severity/status workflow |
| Relief Request Svc | 8082 | `/api/relief-requests/**`                | `relief_request_db`    | Citizen needs, assignment & fulfilment workflow |
| Volunteer Service  | 8083 | `/api/volunteers/**`                     | `volunteer_db`         | Volunteer registration, assignment, availability |
| Resource Service   | 8084 | `/api/resources/**`, `/api/shelters/**`  | `resource_db`          | Inventory, allocation, restock, shelters |
| Notification Svc   | 8085 | `/api/notifications/**`                  | `notification_db`      | Per-user notification inbox |

The frontend talks **only** to the API Gateway at `http://localhost:8080` (override via the
`VITE_API_URL` env var) — it never calls a downstream service directly.

## Example Flow: Reporting an Incident

A end-to-end walk through the code, illustrating the microservices pattern used throughout:

1. `POST /api/incidents` hits the **Gateway**, which matches the incident route and forwards it
   (load-balanced via Eureka) to an `incident-service` instance.
2. `IncidentController` validates the request and delegates to `IncidentServiceImpl`.
3. The service creates the `Incident` entity, defaults its status to `REPORTED`, and persists it
   to `incident_db`.
4. It then calls `NotificationClient` (a Feign client) to notify admins — this is a real HTTP
   call to Notification Service, resolved dynamically through Eureka by service name.
5. Notification Service persists its own `Notification` row in `notification_db`, completely
   decoupled from Incident Service's schema.
6. **Resilience by design:** the notification call is wrapped in a try/catch — if Notification
   Service is unreachable, the failure is logged and swallowed, and incident creation still
   succeeds. A non-critical downstream failure never blocks the core operation.
7. The saved incident is returned through the Gateway to the frontend as `201 Created`.

Status changes (`updateIncident` / `patchIncident`) are governed by an explicit state machine
(`REPORTED → VERIFIED → IN_PROGRESS → RESOLVED → CLOSED`, with `REOPENED` from `RESOLVED`/`CLOSED`).
Attempting to mark an incident `RESOLVED` triggers a Feign call to Relief Request Service to
confirm there are no open relief requests first — again degrading gracefully if that service is
down.

## Tech Stack

**Backend:** Java 17, Spring Boot, Spring Cloud Gateway, Spring Cloud Netflix Eureka, Spring Data
JPA / Hibernate, OpenFeign, PostgreSQL, Maven

**Frontend:** React 19, TypeScript, Vite

## Prerequisites

- JDK 17+ and Maven (or the bundled `mvnw` wrappers)
- Node.js 20+ and npm
- PostgreSQL 15 — either a local instance or via `docker compose`

## Getting Started

### 1. Database

Using Docker:

```powershell
docker compose up -d
```

This starts PostgreSQL on `localhost:5432` (user `postgres`) and creates all five service
databases. Credentials match each service's `application.properties`.

Using an existing local PostgreSQL: run `scripts/postgres-init.sql` as a superuser to create the
databases. Tables are created automatically by Hibernate (`ddl-auto=update`).

> **Schema drift:** databases created before the current enums may carry stale CHECK / NOT NULL
> constraints (Hibernate `ddl-auto=update` never alters existing constraints). If you hit errors
> like `violates check constraint`, run `scripts/fix-schema.sql` against `incident_db`,
> `relief_request_db`, and `notification_db`. The script is idempotent.

### 2. Backend

Start everything (Eureka → services → gateway):

```powershell
.\start-services.ps1
```

Or start each module individually (order doesn't matter once Eureka is up):

```powershell
cd Backend\EurekaServer;         .\mvnw spring-boot:run
cd Backend\IncidentService;      .\mvnw spring-boot:run
cd Backend\ReliefRequestService; .\mvnw spring-boot:run
cd Backend\VolunteerService;     .\mvnw spring-boot:run
cd Backend\ResourceService;      .\mvnw spring-boot:run
cd Backend\NotificationService;  .\mvnw spring-boot:run
cd Backend\APIGateway;           mvn spring-boot:run   # no wrapper — needs Maven on PATH
```

Verify:
- Eureka dashboard: http://localhost:8761
- Gateway health: http://localhost:8080/actuator/health

### 3. Frontend

```powershell
cd Frontend\Relivio
npm install
npm run dev          # http://localhost:5173
npm run build         # type-check + production build
```

### 4. Seed demo data

```powershell
.\scripts\seed-data.ps1          # PowerShell
./scripts/seed-data.sh           # bash (requires curl + jq)
```

Seeds incidents, volunteers, relief requests, resources, and shelters, then walks a full
workflow: incident verified → volunteer assigned → request advanced to `IN_PROGRESS` → water
allocated → shelter beds allocated → notifications created for the affected reporter.

## Key Business Rules

- **Incidents**: `REPORTED → VERIFIED → IN_PROGRESS → RESOLVED → CLOSED` (may `REOPEN`). Cannot
  be marked `RESOLVED` while it has relief requests in `PENDING`/`ASSIGNED`/`IN_PROGRESS`.
- **Relief requests**: `PENDING → ASSIGNED → IN_PROGRESS → FULFILLED → CLOSED`. Can only reach
  `FULFILLED` with an assigned volunteer; matched to nearby volunteers automatically when
  registered with coordinates.
- **Volunteers**: `AVAILABLE / ASSIGNED / ON_LEAVE / UNAVAILABLE`. Assignment sets status to
  `ASSIGNED` and notifies the linked relief request.
- **Resources**: `AVAILABLE / LOW_STOCK / OUT_OF_STOCK / EXPIRED`, governed by a configurable
  low-stock threshold. Allocating a resource to a request sets that request to `ASSIGNED`.
- **Shelters**: occupancy-based; a shelter at full capacity is excluded from nearby matching.

## Project Structure

```
LatestProject/
├─ Backend/
│  ├─ EurekaServer/            Service registry
│  ├─ APIGateway/              Routes + CORS on port 8080
│  ├─ AuthService/             User auth (registration/login)
│  ├─ IncidentService/         Incident lifecycle, severity/status workflow
│  ├─ ReliefRequestService/    Citizen needs, assignment & fulfilment workflow
│  ├─ VolunteerService/        Volunteer registration, assignment, availability
│  ├─ ResourceService/         Inventory, allocation, restock, shelters
│  └─ NotificationService/     Per-user notification inbox
├─ Frontend/
│  └─ Relivio/                 React 19 + TypeScript + Vite SPA
├─ scripts/
│  ├─ postgres-init.sql        Creates the five service databases
│  ├─ fix-schema.sql           Repairs stale CHECK / NOT NULL constraints
│  └─ seed-data.ps1 / .sh      Demo data + workflow through the API Gateway
├─ start-services.ps1 / .sh    One-shot launcher for all services
└─ docker-compose.yml          PostgreSQL with init + volume
```

## Tests

Each backend module ships with JUnit tests. Run `mvn test` in any service directory
(63 tests total across the seven modules).

## Out of Scope

Full authentication/authorization wiring across services, an AI chatbot, SMS/email dispatch, and
native mobile apps are deferred for this release.
