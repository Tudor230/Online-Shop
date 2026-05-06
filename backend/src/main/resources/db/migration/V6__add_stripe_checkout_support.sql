ALTER TABLE "order"
    ADD COLUMN shipping_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN currency_code VARCHAR(3) NOT NULL DEFAULT 'RON',
    ADD COLUMN stripe_checkout_session_id VARCHAR(255) UNIQUE,
    ADD COLUMN stripe_payment_intent_id VARCHAR(255),
    ADD COLUMN paid_at TIMESTAMPTZ;

CREATE TABLE stripe_webhook_event (
    id UUID PRIMARY KEY,
    stripe_event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

ALTER TABLE "order"
    ADD CONSTRAINT chk_order_shipping_amount CHECK (shipping_amount >= 0),
    ADD CONSTRAINT chk_order_tax_amount CHECK (tax_amount >= 0),
    ADD CONSTRAINT chk_order_currency_code CHECK (char_length(currency_code) = 3);
