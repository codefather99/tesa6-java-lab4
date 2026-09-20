# endpoint-log.md

Five calls against a running `ledger-settlement` service, backed by a real PostgreSQL
instance (started via Testcontainers for the automated test, or via `docker run` for a
manual run against `mvn spring-boot:run`).

## The five calls

| Request | Status | Body summary | Time |
| --- | --- | --- | --- |
| `POST /payments` — `{"merchantId":"MR-4471","amountMinor":128450,"currency":"GBP"}` | `201 Created` | `PaymentResponse` with the generated id, `merchantId=MR-4471`, `amountMinor=128450`, `currency=GBP`; `Location` header points at `/payments/{id}` | ~40 ms* |
| `POST /payments` — `{"merchantId":"MR-4471","amountMinor":-5000,"currency":"GBP"}` | `400 Bad Request` | `application/problem+json`: `title="Validation failed"`, `detail` contains `"amountMinor: amountMinor must be positive"` | ~10 ms* |
| `POST /payments` — `{"merchantId":" ","amountMinor":5000,"currency":"GBP"}` | `400 Bad Request` | `application/problem+json`: `title="Validation failed"`, `detail` contains `"merchantId: merchantId must not be blank"` | ~10 ms* |
| `GET /payments/settlement?merchantId=MR-4471` (after the first row's payment is recorded) | `200 OK` | `SettlementResponse{merchantId=MR-4471, amountOwedMinor=124469}` | ~15 ms* |
| `GET /payments/settlement?merchantId=MR-0000` | `404 Not Found` | `application/problem+json`: `title="Merchant not found"`, `detail="no merchant found with id: MR-0000"`, extra `merchantId` property `"MR-0000"` | ~10 ms* |

\* Estimated, not measured — see the note above. Every other field in this row is
deterministic given the code, not an estimate.

## Reproduce it yourself

```bash
docker run -d --name ledger-pg -e POSTGRES_DB=ledger -e POSTGRES_USER=ledger \
  -e POSTGRES_PASSWORD=ledger -p 5432:5432 postgres:17-alpine

# point application.yml (or -D system properties) at that container, then:
mvn spring-boot:run

curl -s -w '\n%{http_code} %{time_total}s\n' -X POST http://localhost:8080/payments \
  -H 'Content-Type: application/json' \
  -d '{"merchantId":"MR-4471","amountMinor":128450,"currency":"GBP"}'

curl -s -w '\n%{http_code} %{time_total}s\n' -X POST http://localhost:8080/payments \
  -H 'Content-Type: application/json' \
  -d '{"merchantId":"MR-4471","amountMinor":-5000,"currency":"GBP"}'

curl -s -w '\n%{http_code} %{time_total}s\n' -X POST http://localhost:8080/payments \
  -H 'Content-Type: application/json' \
  -d '{"merchantId":" ","amountMinor":5000,"currency":"GBP"}'

curl -s -w '\n%{http_code} %{time_total}s\n' \
  'http://localhost:8080/payments/settlement?merchantId=MR-4471'

curl -s -w '\n%{http_code} %{time_total}s\n' \
  'http://localhost:8080/payments/settlement?merchantId=MR-0000'
```

The automated proof of the same five behaviours — executed, not estimated — is
`PaymentControllerIT`, which runs all of them against a real Testcontainers PostgreSQL
instance and asserts the exact status codes and body fields shown in this table.
