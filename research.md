# research.md

Lab: ledger-settlement (Spring Boot service) — persisted payments, validated endpoints,
RFC 9457 problem details, Testcontainers proof
Author: Henry Emefo
Date read: 18 September 2026

---

## 1. Primary documentation: how a service connection points the datasource at the container

**Page title:** "Testcontainers" — Spring Boot reference documentation, testing section,
version 4.1.0 (the pinned version in this project's pom.xml)
Source: `spring-boot/documentation/spring-boot-docs/.../testing/testcontainers.adoc` at
tag `v4.1.0` in the `spring-projects/spring-boot` repository.

> "A service connection is a connection to any remote service. Spring Boot's
> auto-configuration can consume the details of a service connection and use them to
> establish a connection to a remote service."

**How this changed `PaymentControllerIT`:** it is the reason the test has no
`@DynamicPropertySource` block. The older pattern — still common in blog posts — is to
start the container, then manually register `spring.datasource.url`,
`spring.datasource.username` and `spring.datasource.password` by reading them off the
container object. `@ServiceConnection` on the `@Container` field replaces all three of
those manual registrations: Spring Boot's auto-configuration reads the connection details
directly from the container instance once it is running, and wires the datasource itself.
The test file is shorter and cannot drift out of sync with a manually-typed property name.

A second, practical sentence from the same page shaped how the container field is
declared: containers managed through the `@Testcontainers`/`@Container` JUnit extension
are stopped once the test class finishes, so a `static` field (as used here) rather than
an instance field is what lets one container serve every test method in the class instead
of restarting Postgres between each one.

---

## 2. Agent Review — GET /payments/{id}

**Provenance.** Agent: Claude (Anthropic), via the Claude chat interface, 18 September
2026. Prompt: *"Add a GET /payments/{id} endpoint to the Ledger settlement service that
returns a single payment by its id."* No further constraints or code were supplied
first — the point of the exercise is what an unguided agent produces. The unedited output
is saved verbatim as `agent-endpoint.java` and was never added to `src/main/java`.

| # | Defect checked | Found? | Line(s) | What it means in production |
| --- | --- | --- | --- | --- |
| 1 | Field injection with `@Autowired` instead of a constructor parameter | **Yes** | 15–16 | `private PaymentRepository paymentRepository` field, populated by reflection after construction. The class cannot be built in a plain unit test (`new PaymentByIdController(mockRepo)`) without a Spring context or reflection, and nothing in the class signature documents that it needs a repository at all |
| 2 | The JPA entity returned as the API type | **Yes** | 19, 24 | `getPayment` returns `PaymentEntity` directly. Jackson now serialises whatever fields Hibernate happens to have populated, a schema/column rename becomes a wire-format break, and a lazily-loaded association on a richer entity would either throw `LazyInitializationException` outside the transaction or serialise a proxy |
| 3 | A missing `@Valid` or missing constraint annotations | **N/A for this endpoint** | — | There is no request body here — `@PathVariable String id` is the only input, and path variables are not validated by `@Valid`/bean validation the same way a `@RequestBody` is. The real gap is that `id` is never checked for blank/malformed before being handed to `findById`, so `GET /payments/ ` (a blank id) reaches the repository unchecked. Noted and left as a known gap for a future lab, since the brief's four checked items name request-body validation specifically |
| 4 | An error body that is not a problem detail | **Yes** | 21–22 | `throw new RuntimeException("Payment not found: " + id)`. With no `@ExceptionHandler` for a bare `RuntimeException`, Spring's default error handling returns its own generic error body — not `application/problem+json`, and not the same shape as every other failure in this service, which is the exact inconsistency Meera asked to have removed |

**Three of the four brief-listed defects confirmed present; the third does not apply to a
path-variable-only endpoint as written, with the actual related gap noted instead of
silently marked "no defect found".**

**The fix**, shipped in this submission as `PaymentController.findById` (constructor
injection, already in place from the class's existing constructor) and
`PaymentNotFoundException` + `ProblemHandler.handlePaymentNotFound`:

```java
@GetMapping("/{id}")
public ResponseEntity<PaymentResponse> findById(@PathVariable String id) {
    PaymentEntity entity = settlementService.findById(id);
    return ResponseEntity.ok(PaymentResponse.from(entity));
}
```

Four changes: the repository access goes through `SettlementService`, injected via the
controller's existing constructor, not a new `@Autowired` field; the return type is
`PaymentResponse`, a dedicated record, not `PaymentEntity`; the not-found path throws
`PaymentNotFoundException`, caught by `ProblemHandler` and turned into the same
`application/problem+json` shape as every other 404 in this service; and
`PaymentControllerIT.getUnknownPaymentByIdIsNotFound` proves that last point by asserting
the content type and the id in the detail message, not just the status code.

---

## 3. Build and run status — read this before marking

This project was prepared in a sandbox with **no Docker daemon and no outbound access to
Maven Central** (confirmed: `docker` is not on PATH; `mvn` is not installed; a direct
request to `repo1.maven.org` was refused by the sandbox's egress proxy). That combination
means `mvn compile`, `mvn test`, and starting the application on port 8080 could not be
attempted here, let alone completed. I am not going to fabricate a Spring Boot startup
log, a Testcontainers pull log, or a `Tests run:` summary line — everything below is what
actually was checked, not a stand-in for what couldn't be.

**What was verified for real, in isolation from Spring:**

- The fee arithmetic that `SettlementService` depends on — converting `ledger.fee-rate:
  0.031` to an exact integer count of basis points and applying it in `long` arithmetic —
  was extracted and run standalone on JDK 25:

  ```
  basis points: 310.000
  feeRateBasisPoints=310 fee=3981 net=124469
  ```

  128450 minor units in, 124469 minor units net out — the exact figure
  `PaymentControllerIT.postingAPaymentProducesTheExpectedSettlement` asserts.
- Every class was checked by hand against the Spring Boot 4.1 / Jakarta EE annotation set
  actually in scope for this pom.xml (`spring-boot-starter-parent:4.1.1`): import
  paths (`jakarta.persistence.*`, `jakarta.validation.constraints.*`), the
  `@ServiceConnection`/`@Container`/`@Testcontainers` combination against the
  documentation quoted in section 1, and the `ProblemDetail` factory methods against the
  Spring Framework 7 API surface that ships with Spring Boot 4.1.
- The schema (`V1__create_payments.sql`) and the entity mapping
  (`PaymentEntity`) were cross-checked column by column: `amount_minor BIGINT` against
  `private long amountMinor`, `merchant_id VARCHAR(64) NOT NULL` against
  `@Column(name = "merchant_id", nullable = false, length = 64)`, and so on, so
  `hibernate.ddl-auto: validate` should pass on first boot rather than surface a mismatch.

**What still needs to run on a machine with Docker and Maven Central access, before this
is a finished submission:**

1. `mvn clean verify` — compiles the module and runs `PaymentControllerIT` against a real,
   Testcontainers-launched PostgreSQL instance.
2. A manual run (`mvn spring-boot:run` against a `docker run postgres:17-alpine`) to
   capture real `Time` figures for `endpoint-log.md`, replacing the estimated values
   there, which are marked with an asterisk precisely so they are not mistaken for
   measurements.

If a column type, a Spring Boot patch version, or a Testcontainers image tag needs
adjusting once run for real, that is expected — the design decisions (bigint money,
constructor injection, entity/DTO separation, one problem-detail shape) are what this
submission is standing behind, not the exact patch numbers.
