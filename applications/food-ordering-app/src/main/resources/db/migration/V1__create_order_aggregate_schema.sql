-- V8.0 C1.2-P11 — baseline relational representation of the Order aggregate.
-- The domain aggregate may span tables; the tables are NOT separate aggregates.
-- Runtime PostgreSQL repository is deferred to P12; business constraints P13,
-- multi-table transaction policy P15, version/currentness enforcement P17.
CREATE TABLE orders (
  id UUID PRIMARY KEY,
  customer_id TEXT NOT NULL,
  restaurant_id TEXT NOT NULL,
  status TEXT NOT NULL,
  placed_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE order_lines (
  order_id UUID NOT NULL REFERENCES orders(id),
  line_position INTEGER NOT NULL,
  menu_item_id TEXT NOT NULL,
  item_name TEXT NOT NULL,
  quantity INTEGER NOT NULL,
  unit_price NUMERIC NOT NULL,
  PRIMARY KEY (order_id, line_position)
);
CREATE TABLE order_workflow_occurrences (
  order_id UUID NOT NULL REFERENCES orders(id),
  occurrence_position INTEGER NOT NULL,
  action TEXT NOT NULL,
  participant TEXT NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (order_id, occurrence_position)
);
-- No stored total, Money/currency policy, Payment/Restaurant/Customer authority,
-- integration event stream or optimistic version field is invented here.
