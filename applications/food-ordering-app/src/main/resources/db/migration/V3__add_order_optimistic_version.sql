-- P17: authoritative revision for conditional aggregate updates.
-- Existing V1/V2 rows begin at revision zero on migration; no historical
-- operation count can be inferred from their current representation.
ALTER TABLE orders
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
  ADD CONSTRAINT chk_orders_version_nonnegative CHECK (version >= 0);
-- All writer versions must advance the revision after rollout. Old binaries
-- that ignore version must not keep writing against this schema.
