# Checkpoint Manifest — C1.2-P10 Candidate

Title: Repository Semantics, Persistence Authority and Reconstitution
Source: V8.0 Cluster 1.2 + derived blueprint part 1.2.10
Inherits: checkpoints/C1.2-P09 (000c2960fdbbe85106fc68c051193f5fafc37675)

Evolution: Type B source change + Type C controlled tests + Type A ADR/evidence.
Production: OrderRepository.createNew, OrderAlreadyExistsException, InMemoryOrderRepository.putIfAbsent, PlaceOrderService creation path changed to createNew. Existing saveCurrent intentionally left unconditional.
Test-only repository stubs updated with explicit createNew handling; nine new repo tests.

Verification required:
1. root Maven reactor verify in GitHub Actions on exact final P10 commit;
2. P18 real process restart negative-control experiment;
3. preserve P13/P14/P17 concurrency failures and P15 replay, P16 provider divergence.

Known non-guarantees: no durable storage, persisted mapping/reconstitution, DB constraints, transaction, version CAS or cross-process unique authority. createNew does not deduplicate same logical command with different IDs; saveCurrent can still upsert and overwrite.
P11 next: Relational Persistence Design and Schema Evolution — add PostgreSQL/migrations/reproducible DB tests, ensure placement creation maps to unique insert; P12 real adapter and durability; later real transaction, isolation and version controls.
This file is a candidate manifest until exact-commit CI verified.
