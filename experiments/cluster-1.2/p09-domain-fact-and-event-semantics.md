# C1.2-P09 — Domain Fact and Event-Semantics Experiments

Base 5f78c68fc04b76f34fbc431010a677c0ba83bc9d. Targeted tests: OrderDomainFactDecisionBoundaryTest (9 cases).
1 accepted placement: OrderPlaced matches the current initial Order state after in-process repository save.
2 rejected placement: no accepted fact and no Order published.
3 later ACCEPTED Order: factory rejects historical placement reconstruction from current advanced status.
4 modified-but-still-PLACED Order: factory rejects; its total is no longer original placement total.
5 paid-milestone-but-still-PLACED Order: factory rejects because historical trace is non-empty despite status PLACED.
6 original historical OrderPlaced immutable after later Order line modification: placement total stays old, new Order total changes.
7 repository fails before publication: no accepted result returned and no current Order in controlled repository.
8 repository publishes then throws: calling service gets technical error but current Order exists in controlled adapter; no returned accepted fact. This does not prove DB commit or real response loss.
9 local RESTAURANT_ACCEPTED occurrence lives in immutable successor, not broker.
Inherited negatives: P15 retry may produce another OrderId, P16 external charge vs local fact, P17 stale history overwrite, P18 SIGKILL restart loss.
Each test intentionally distinguishes source-observed behavior, controlled injection and untested future guarantee. CI root mvn -B -ntp verify and real P18 shell experiment required at exact P09 commit; no pre-CI pass claims.
