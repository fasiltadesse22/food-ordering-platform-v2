-- C1.2-P13 — defense-in-depth for selected *row-local* invariants.
-- V1 is immutable: all changes live in this additive Flyway V2.
-- PostgreSQL validates existing rows when adding CHECK constraints; the
-- deployment must detect/remediate historical invalid data before migration.
--
-- Java String.isBlank and SQL btrim are not exactly equivalent for all Unicode.
-- These checks cover the common ASCII-space blank case without claiming
-- language-independent equivalence.
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_customer_nonblank CHECK (length(btrim(customer_id)) > 0),
    ADD CONSTRAINT chk_orders_restaurant_nonblank CHECK (length(btrim(restaurant_id)) > 0),
    ADD CONSTRAINT chk_orders_status_known CHECK (status IN (
        'PLACED', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'PREPARING', 'COMPLETED'
    ));

ALTER TABLE order_lines
    ADD CONSTRAINT chk_lines_position_nonnegative CHECK (line_position >= 0),
    ADD CONSTRAINT chk_lines_menu_item_nonblank CHECK (length(btrim(menu_item_id)) > 0),
    ADD CONSTRAINT chk_lines_name_nonblank CHECK (length(btrim(item_name)) > 0),
    ADD CONSTRAINT chk_lines_quantity_positive CHECK (quantity > 0),
    ADD CONSTRAINT chk_lines_unit_price_nonnegative_finite CHECK (
        unit_price >= 0
        AND unit_price <> 'NaN'::NUMERIC
        AND unit_price <> 'Infinity'::NUMERIC
    );

ALTER TABLE order_workflow_occurrences
    ADD CONSTRAINT chk_occurrences_position_nonnegative CHECK (occurrence_position >= 0),
    ADD CONSTRAINT chk_occurrences_action_known CHECK (action IN (
        'PAYMENT_RECORDED', 'RESTAURANT_ACCEPTED', 'RESTAURANT_REJECTED',
        'ORDER_CANCELLED', 'REFUND_REQUESTED', 'ORDER_MODIFIED',
        'PREPARATION_STARTED', 'ORDER_COMPLETED'
    )),
    ADD CONSTRAINT chk_occurrences_participant_known CHECK (participant IN (
        'CUSTOMER', 'PAYMENT_PARTICIPANT', 'RESTAURANT_OPERATOR', 'PLATFORM'
    ));

-- These are row-local/representation constraints, NOT a complete business
-- state machine, cross-row status/history guarantee or payment-provider proof.
-- Missing/fragmented history, empty order_lines, actor authenticity,
-- sequence chronology, cross-aggregate invariants and stale updates remain.
