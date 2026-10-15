# C1.2-P13 — PostgreSQL Defense-in-Depth Controlled Experiments

## Common method
For each experiment: Question -> hypothesis -> prediction -> setup -> controlled variable -> execution -> observation -> evidence -> interpretation -> limitations -> conclusion.
Environment: PostgreSQL 16 via Testcontainers; Flyway V1+V2; direct JDBC (intentionally bypass Java constructors). Assertions use SQLState 23514 for CHECK violations; JDBC adapter and two-process HTTP recovery separately verified.

## New test class: OrderRelationalIntegrityConstraintsTest (12 tests)
E1 legal values: INSERT Order/Line/Occurrence then set unit price=0 and status ACCEPTED; writes succeed. Zero price legal under current Java domain; status label validity alone does not imply valid transition.
E2 positive quantity: UPDATE qty to 0 and -2; each fails CHECK 23514. Before V2 these writes succeeded.
E3 price: UPDATE to negative decimal, NUMERIC NaN, Infinity, -Infinity; all fail 23514. A finite nonnegative price remains valid.
E4 root identifiers: ASCII-space-only customer_id and empty restaurant_id fail 23514. Unicode String.isBlank parity not claimed.
E5 line textual value: blank menu_item_id and item_name fail 23514.
E6 ordered list representation: negative line_position and occurrence_position fail 23514; gaps not constrained.
E7 known OrderStatus vocabulary: DELIVERED or empty symbol fail 23514; enum evolution requires future migration.
E8 known occurrence vocabulary: unknown action or participant fail 23514. Participant authenticity and valid actor source not proven.
E9 rejected multi-column update: attempted qty=0,unit_price=99 fails; original qty=2,price=5.50 remain as observed; P15 expands transaction failure coverage.
E10 cross-row negative control: status COMPLETED with missing required history passes DB row-local checks. P12 domain reconstitution rejects selected missing-history combination.
E11 cross-row negative control: parent Order with zero line rows accepted at schema level. P12 domain reconstitution rejects empty line list.
E12 forward-only migration with existing invalid data: actual Postgres starts V1 only, inserts qty 0 price -2; V2 ADD CHECK fails. Test explicitly fixes data, reapplies V2 successfully, verifies versioned history now two and new qty 0 rejected 23514. No silent correction is performed by migration.
Existing P11 OrderRelationalSchemaMigrationTest now expects V1+V2 and rejects formerly accepted qty/negative price. Existing P12 PostgresOrderRepositoryIntegrationTest now asserts invalid line UPDATE rejected at SQL boundary; other invalid history/missing line tests preserved.

## Exact candidate run
GitHub Actions https://github.com/fasiltadesse22/food-ordering-platform-v2/actions/runs/38694233940 commit 2dc184df0fc1e50726ae814507b7862460abdb00: Maven 168 tests, 0 failures, 0 errors, 0 skipped. PostgreSQL Testcontainers V2 applied; E12 dirty V1 migration rejected then repaired/retried. Both P18 memory negative and P12 PostgreSQL positive process-restart scripts still pass.

## Do not overclaim
No database enforcement of full lifecycle status/history, nonempty owned lines, workflow causality, actor authenticity, external charges, version/CAS conflicts, end-to-end idempotency, high-availability failover, production-scale migration performance or Unicode normalization equality. A successful CHECK is one storage-barrier proof, not complete aggregate correctness.
