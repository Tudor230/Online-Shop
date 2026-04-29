CREATE TABLE shopping_cart (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE,
    session_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_shopping_cart_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    CONSTRAINT chk_shopping_cart_owner CHECK ((user_id IS NOT NULL) <> (session_id IS NOT NULL))
);

CREATE TABLE cart_item (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES shopping_cart(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT uq_cart_item_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT chk_cart_item_quantity CHECK (quantity > 0)
);
