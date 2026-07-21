# Ticket Booking System

A Spring Boot service for creating events and selling limited-availability tickets under high-concurrency flash-sale conditions, without overselling.

## Overview

The core problem this project solves is: when many users try to buy the last few tickets of an event at the same time, how do you guarantee no more tickets are sold than exist, while still staying fast?

The approach:

- **Redis holds the source of truth for "tickets left"** during a sale. Every purchase attempt does an atomic `DECR` on a per-event counter — no database row-locking on the hot path.
- **Idempotency keys** (also stored in Redis with a TTL) prevent duplicate purchases from retried/duplicate requests.
- **PostgreSQL holds the durable state** (events, tickets, orders, order items), with optimistic locking (`@Version`) on `Event` and `Ticket`.
- **A reconciliation job** periodically resyncs the Redis counters from the database (`AVAILABLE` ticket counts), and also runs once on startup, so Redis can never drift permanently out of sync with Postgres.
- **An outbox table** (`OutboxEvent`) is in place for reliably publishing domain events (e.g. `OrderConfirmed`, `TicketSold`) to downstream consumers.

## Tech Stack

| Concern | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 4 (Web MVC, Data JPA, Validation, Data Redis) |
| Database | PostgreSQL |
| Cache / Inventory counters | Redis |
| Messaging (provisioned) | RabbitMQ |
| Object mapping | MapStruct |
| Boilerplate reduction | Lombok |
| Build | Maven (wrapper included) |
| Containerization | Docker, Docker Compose |
| Load balancing | Nginx (round-robin across 3 app instances) |

## Architecture

```
                 ┌────────┐
   clients ───▶  │ Nginx  │  (port 80, load balancer)
                 └───┬────┘
          ┌──────────┼──────────┐
          ▼           ▼          ▼
       app1:8080  app2:8080  app3:8080     (Spring Boot instances)
          │           │          │
          ├─────── Redis ────────┤          (ticket counters, idempotency keys)
          │                      │
          └────── PostgreSQL ────┘          (events, tickets, orders, outbox)

       RabbitMQ is provisioned alongside the stack for future
       outbox-event publishing / async processing.
```

### Domain model

- **Event** — `name`, `description`, `totalTickets`, `saleStartAt`/`saleEndAt`, `status` (`DRAFT`, `ON_SALE`, `SOLD_OUT`, `CLOSED`).
- **Ticket** — belongs to an `Event`, has a `seatCode`, `price`, `status` (`AVAILABLE`, `RESERVED`, `SOLD`, `CANCELED`).
- **Order** — belongs to an `Event`, keyed by a unique `idempotencyKey`, `status` (`PENDING`, `CONFIRMED`, `CANCELLED`, `FAILED`).
- **OrderItem** — links an `Order` to the `Ticket` that was reserved for it, with the price paid.
- **OutboxEvent** — transactional outbox row (`aggregateType`, `aggregateId`, `eventType`, JSON `payload`) for reliable event publishing.

### Purchase flow (`POST /api/events/{eventId}/buy`)

1. Atomically claim the idempotency key in Redis (`SETIFABSENT`, 5 min TTL). If it's already claimed, the request is treated as a duplicate and returns success with no body.
2. Atomically `DECR` the event's available-ticket counter in Redis.
   - If the result is negative, the decrement is rolled back and the request fails with `404` (event not found) or `409` (sold out).
3. On success, reserve a `Ticket` row, create the `Order` and `OrderItem` in Postgres within a transaction.
4. If anything after the decrement fails, the Redis counter is incremented back to avoid permanently losing inventory.

## API

Base path: `/api/events`. All responses are wrapped in a common envelope:

```json
{ "success": true, "data": { ... }, "error": null, "meta": null }
```

On failure, `error` is populated with `{ "status", "title", "detail" }` and the appropriate HTTP status is set (`404` event not found, `409` no tickets available, `500` unexpected error).

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/events` | Create a new event |
| `POST` | `/api/events/{eventId}/buy` | Buy one ticket for an event (idempotent) |

**Create event**

```http
POST /api/events
Content-Type: application/json

{
  "name": "Summer Music Festival",
  "description": "An outdoor concert",
  "totalTickets": 500,
  "ticketPrice": 49.99,
  "saleStartAt": "2026-08-01T00:00:00Z",
  "saleEndAt": "2026-08-10T00:00:00Z"
}
```

**Buy ticket**

```http
POST /api/events/{eventId}/buy
Content-Type: application/json

{
  "idempotencyKey": "a-client-generated-unique-key"
}
```

A ready-to-use request collection is available at [`postman/ticketbooking.postman_collection.json`](postman/ticketbooking.postman_collection.json).

## Getting Started

### Prerequisites

- Java 21
- Docker & Docker Compose (recommended), or locally running PostgreSQL + Redis

### Run with Docker Compose (recommended)

1. Copy the env template and fill in credentials:

   ```bash
   cp .env.example .env
   ```

   ```env
   POSTGRES_USER=
   POSTGRES_PASS=
   POSTGRES_DB=

   RABBITMQ_DEFAULT_USER=
   RABBITMQ_DEFAULT_PASS=
   ```

2. Start the full stack (3 app instances behind Nginx, Postgres, Redis, RabbitMQ):

   ```bash
   docker compose up --build
   ```

3. The API is available at `http://localhost` (Nginx, port 80), load-balanced across `app1`/`app2`/`app3`.

### Run locally (without Docker)

1. Start PostgreSQL and Redis locally.
2. Update `src/main/resources/application-local.yaml` with your local database credentials if they differ from the defaults.
3. Run with the `local` profile:

   ```bash
   ./mvnw spring-boot:run
   ```

   The app starts on `http://localhost:8080`.

### Configuration

Configuration is environment-driven via Spring profiles (`spring.profiles.active`, default `local`):

| Variable | Purpose | Default |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `local` |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `POSTGRES_USER` / `POSTGRES_PASS` / `POSTGRES_DB` | PostgreSQL credentials | — |
| `REDIS_HOST` | Redis host | `localhost` |
| `RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS` | RabbitMQ credentials (Docker Compose) | — |

Database schema is managed via `hibernate.ddl-auto: update` for development convenience.

## Building & Testing

```bash
./mvnw clean package   # build the jar
./mvnw test             # run tests
```

## Project Structure

```
src/main/java/me/kitkas1412/ticketbooking/
├── controller/    REST controllers
├── dto/           Request/response records
├── entity/        JPA entities
├── exception/     Domain exceptions + global exception handler
├── mapper/        MapStruct entity <-> DTO mappers
├── redis/         Redis inventory keys, reconciliation job, startup sync
├── repository/    Spring Data JPA repositories
└── service/       Business logic (interfaces + impl)
```
