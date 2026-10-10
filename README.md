# Food Ordering Platform — V8.0 Fresh-Start Evolution

Current verified checkpoint: C1.1-P18 — Process Crash, Transient State & Durability Non-Guarantees
Previous checkpoint: checkpoints/C1.1-P17

## Part type

Type C — process-loss / durability failure evolution.

## Verification

GitHub Actions:
38035108764

Root:
mvn -B -ntp verify

Observed:
Tests run: 86, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

Runtime durability experiment:
experiments/cluster-1.1/p18-process-restart-durability.sh

Observed:
- process A served the selected Order as CANCELLED;
- process A was terminated with SIGKILL;
- process B started successfully from the same jar;
- GET of the same OrderId returned 404.

## Critical conclusion

The current InMemoryOrderRepository is authoritative while its JVM is alive.

It is not a durable recovery source across JVM replacement.

A successful application restart restores compute, not acknowledged Order state.

## Important boundary

P18 proves the durability failure before fixing it.

PostgreSQL remains deferred to Cluster 1.2.

P19 next synthesizes and freezes Cluster 1.1.

See checkpoint-manifest.md and architecture/ for evidence-qualified details.
