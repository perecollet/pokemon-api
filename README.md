# Pokémon Ranking API

REST API exposing rankings of Pokémon (heaviest, tallest, most experienced) backed by an incremental sync from [PokéAPI](https://pokeapi.co/api/v2/).

Built as a technical assessment for **Alea** (iGaming aggregator, Barcelona).

---

## Quick start

### Prerequisites

- Docker & Docker Compose
- (Optional, for local development) Java 21, Maven 3.9+

### Run everything with Docker Compose

```bash
cp .env.example .env
# Edit .env to set your own POSTGRES_USER and POSTGRES_PASSWORD
docker compose up -d --build
```

The app exposes:

- API on http://localhost:8080
- Swagger UI on http://localhost:8080/swagger-ui.html
- Actuator on http://localhost:8080/actuator
- Prometheus metrics on http://localhost:8080/actuator/prometheus

On startup the application triggers a one-time synchronization against PokéAPI; the first ranking request will return `503` until the catalog is loaded (typically 30–60 seconds).

### Run locally (app on host, Postgres in Docker)

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

### Run tests

```bash
./mvnw clean test
```

Generates a JaCoCo coverage report at `target/site/jacoco/index.html`.

---

## Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| `GET` | `/api/v1/pokemon/top/heaviest?limit=5` | Top N Pokémon by weight (descending) |
| `GET` | `/api/v1/pokemon/top/tallest?limit=5` | Top N Pokémon by height (descending) |
| `GET` | `/api/v1/pokemon/top/most-experienced?limit=5` | Top N Pokémon by base experience |

`limit` is optional (default `5`, max `50`).

### Response format

```json
[
  {
    "id": 143,
    "name": "snorlax",
    "weightKg": 460.0,
    "heightCm": 210,
    "baseExperience": 189
  }
]
```

Weight is exposed in **kilograms** and height in **centimeters** (PokéAPI stores them in hectograms and decimeters; conversion happens at the DTO boundary).

### Error responses (RFC 7807)

| Status | Condition | Body |
|--------|-----------|------|
| `400` | Invalid `limit` parameter | ProblemDetail with title `Invalid request parameters` |
| `503` | Catalog not yet synchronized | ProblemDetail with title `Catalog not ready` |
| `500` | Unexpected error | ProblemDetail with title `Internal server error` |

---

## Architecture

Strict hexagonal (ports & adapters) architecture with three layers and a one-way dependency direction:

```
infrastructure ──> application ──> domain
```

### Package structure

```
com.alea.pokemon
├── domain/                           Pure Java, zero framework dependencies
│   ├── model/                        Pokemon record with self-validating invariants
│   ├── exception/                    Business exceptions (PokemonDataNotReadyException)
│   └── port/
│       ├── in/                       Use-case interfaces (driven by web layer)
│       └── out/                      Repository and external-system interfaces
│
├── application/                      Use-case orchestration
│   └── service/
│       ├── PokemonRankingService     Implements GetTopPokemonUseCase
│       ├── PokemonSynchronizationService  Implements SynchronizePokemonUseCase
│       ├── RankingCacheReader        Cache layer (extracted for AOP proxy correctness)
│       └── CacheInvalidator          Conditional cache eviction
│
└── infrastructure/                   Adapters and Spring wiring
    ├── adapter/
    │   ├── in/web/                   REST controllers, DTOs, GlobalExceptionHandler
    │   └── out/
    │       ├── persistence/          JPA adapter, entity, mapper
    │       └── pokeapi/              REST client with Resilience4j
    └── scheduler/                    Startup + periodic sync trigger
```

### Layer rules

- **Domain** has zero imports from Spring, JPA, or any infrastructure technology. It is plain Java.
- **Application** depends only on `domain`. No web, no JPA, no HTTP.
- **Infrastructure** is the only layer allowed to import Spring and integration libraries.
- Adapters are named after the technology they implement (`JpaPokemonRepository`, `PokeApiClient`), never with the `Impl` suffix.

---

## Technical decisions

### REST instead of GraphQL

PokéAPI exposes both REST (`/api/v2/`) and a beta GraphQL endpoint. The brief explicitly references the REST URL, and the GraphQL beta status conflicts with the "production ready" requirement. REST is also better understood by tooling (Resilience4j, OpenAPI, metrics).

The classic N+1 problem when fetching details after listing is mitigated with a bounded thread pool, not with GraphQL.

### PostgreSQL instead of in-memory storage

The brief asks for a production-ready service. Persistence reflects how aggregator platforms operate (Alea aggregates external game catalogs into its own store) and demonstrates JPA, Flyway, Testcontainers, and connection-pool tuning. An in-memory adapter is mentioned in *Evolution to production* below as a future extension.

### Caffeine instead of Redis

The data set changes once per day at most (new Pokémon appear with each generation release). A local in-memory cache:

- Is sufficient because there is no cross-instance consistency requirement.
- Avoids operating Redis for a 1351-entry data set.
- Sub-millisecond latency vs network hop to Redis.

The cache stores the **top 50** entries per ranking; the controller slices in memory based on the `limit` parameter. This avoids cache-entry duplication for different `limit` values (one entry per ranking, not one per `(ranking, limit)` combination).

### Cache layering

`RankingCacheReader` is a separate Spring bean from `PokemonRankingService`. This is intentional: `@Cacheable` works through Spring AOP proxies, which **do not fire on self-invocation**. By extracting the cached method to its own bean, the proxy is invoked correctly. The same pattern applies to `PokeApiHttpClient` for `@Retry` and `@CircuitBreaker`.

### Incremental synchronization

Instead of refetching all 1351 Pokémon every 24 hours, the service:

1. Fetches `/pokemon?limit=1` to read `count` (the total remote count).
2. Compares with `repository.count()` locally.
3. Fetches **only the new range** (`offset = local count`).

Since IDs in PokéAPI are sequential and rarely change once assigned, using `repository.count()` as offset is a safe approximation. Limitations of this approach are noted in *Evolution to production*.

### Resilience4j

Retry (3 attempts, exponential backoff starting at 500 ms) and Circuit Breaker (sliding window of 20 calls, opens at 50% failure rate) protect both the listing and detail HTTP calls. Annotated methods live in `PokeApiHttpClient`, called from `PokeApiClient`, so the Spring AOP proxy fires correctly.

Partial failures on individual detail calls are swallowed at the orchestration layer (`PokeApiClient.fetchDetailSafe`) so that one bad Pokémon does not abort the entire sync.

### Concurrency control in sync

`PokemonSynchronizationService.synchronize()` is guarded by an `AtomicBoolean` to prevent concurrent runs (startup + scheduled overlap, or manual triggers). Failed syncs preserve existing data — the cache is only evicted when new entries are actually written.

### Virtual threads

Java 21 virtual threads are enabled for Tomcat request handling:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

The application is I/O-bound (DB queries, HTTP calls), making virtual threads the right model. The `FixedThreadPool(20)` used during sync is deliberately preserved to respect PokéAPI rate limits — the constraint is the external API, not local resources.

### Why no microservices, Kafka, or auth

- **Microservices**: a single bounded context. Splitting would be over-engineering.
- **Kafka**: no event-driven consumers; synchronization is a periodic batch.
- **Auth**: not in the brief. Adding it would dilute the focus on the ranking logic.

All three are listed in *Evolution to production* as deliberate next steps if scope expanded.

---

## Testing strategy

A clear pyramid:

- **Unit tests** (Mockito): domain model, application services. Fast, no Spring context.
- **Slice tests** (`@WebMvcTest`, `@DataJpaTest`): controllers, JPA adapter. Lightweight Spring context.
- **Integration tests**: cache eviction through real Spring AOP context, JPA adapter against real Postgres via Testcontainers, PokéAPI client against WireMock.

Mocks are used to isolate behavior; never to mock the system under test. For example, the JPA adapter test runs against a real Postgres container — mocking JDBC would only verify that Mockito returns what it was told.

### Coverage

JaCoCo plugin runs on every build. Report at `target/site/jacoco/index.html`.

Current coverage focuses on:

- 100% on the domain model.
- 100% on application services (including cache eviction via Spring-context test).
- 100% on the JPA adapter.
- High coverage on the controller and exception handler via `@WebMvcTest`.
- PokéAPI client via WireMock stubs (success, partial failure, null `base_experience`).

---

## Observability

- **Health**: `/actuator/health` (Postgres connection included).
- **Metrics**: `/actuator/prometheus` exposes JVM, HTTP request histograms (P50/P95/P99), HikariCP pool, Caffeine hit rates, Resilience4j circuit-breaker state, scheduled-task latencies, Spring Data repository invocations.
- **Cache stats**: `/actuator/caches` lists cache names and statistics (enabled via `recordStats` on Caffeine).
- **OpenAPI**: `/v3/api-docs` (raw JSON) and `/swagger-ui.html` (UI).

---

## Configuration

Driven entirely by environment variables with safe defaults (12-factor):

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_HOST` | `localhost` | Postgres host |
| `POSTGRES_PORT` | `5432` | Postgres port |
| `POSTGRES_DB` | `pokemon` | Database name |
| `POSTGRES_USER` | `alea` | Database user |
| `POSTGRES_PASSWORD` | `alea` | Database password |
| `SERVER_PORT` | `8080` | API port |

PokéAPI sync behavior (in `application.yml`):

| Property | Default | Description |
|----------|---------|-------------|
| `alea.pokeapi.base-url` | `https://pokeapi.co/api/v2` | PokéAPI base URL |
| `alea.pokeapi.fetch-concurrency` | `20` | Parallel detail fetches per sync |
| `alea.pokeapi.sync.enabled` | `true` | Enable the scheduler |
| `alea.pokeapi.sync.interval` | `PT24H` | ISO-8601 sync interval |

---

## Evolution to production

This section describes deliberate next steps if scope expanded beyond the technical assessment.

### Distributed cache (L1 + L2)

Add Redis as L2 (shared across instances), keep Caffeine as L1 (per-instance fast path). Cache invalidation on sync would publish to a Redis Pub/Sub channel so all instances clear their L1.

### Event-driven sync

Replace the periodic `@Scheduled` trigger with a Kafka topic populated by an upstream catalog-publisher service. New Pokémon arrive as domain events; the API consumes and updates locally. Removes the need for polling PokéAPI altogether.

### Multiple repository adapters

Add `InMemoryPokemonRepository` behind a `@Profile("in-memory")` for local development and acceptance tests without Postgres. The hexagonal port is already in place — only a new adapter is needed.

### Authentication and rate limiting

Spring Security with OAuth2/JWT for client identification, plus Bucket4j for per-client rate limits (the iGaming aggregator pattern typically requires both).

### Health indicator with sync freshness

A custom `HealthIndicator` reporting `DOWN` if the last successful sync is older than 48h. Load balancers would stop routing traffic to a stale instance.

### Structured JSON logs

`logstash-logback-encoder` for JSON output, ready for ELK/Loki ingestion without log parsing.

### Multi-instance deployment

The cache invalidation, the sync concurrency guard, and the count-as-offset assumption all work for single-instance. For multi-instance:

- Move the scheduler to a distributed lock (Redis or ShedLock) so only one instance triggers the sync.
- Use Redis L2 cache as above.
- The count-as-offset assumption should be replaced with a more robust "fetch all IDs and diff" approach, since out-of-order remote insertions or local deletions would otherwise be missed.

### Sync incrementality limitations

Using `repository.count()` as offset assumes:

- PokéAPI IDs are strictly sequential and only appended.
- Local entries are never deleted.

For the assessment scope this is safe (PokéAPI is append-only). In a production aggregator the safer pattern is to fetch the remote ID set and diff against the local set, accepting the trade-off of one extra listing call.

---

## Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 16 |
| Migration | Flyway |
| Cache | Caffeine |
| Resilience | Resilience4j (retry + circuit breaker) |
| HTTP client | Spring `RestClient` |
| API documentation | springdoc-openapi 2.8 |
| Observability | Spring Boot Actuator + Micrometer + Prometheus |
| Testing | JUnit 5, Mockito, AssertJ, Testcontainers, WireMock |
| Build | Maven |
| Container | Multi-stage Dockerfile (Eclipse Temurin 21 JRE Alpine) |

---

## Commit history

The commit history follows Conventional Commits and is structured to tell the development story from domain model outward:

1. Project scaffolding and domain model.
2. Ports (in/out).
3. Application services with TDD.
4. Persistence adapter with Flyway and Testcontainers.
5. PokéAPI client with concurrency control.
6. Resilience4j integration.
7. Synchronization service and scheduler.
8. Web layer with ProblemDetail.
9. Caffeine cache layer with conditional invalidation.
10. Observability (Prometheus, Actuator).
11. Containerization (Dockerfile, full compose stack).

Each commit is scoped to a single concern and passes the full test suite.