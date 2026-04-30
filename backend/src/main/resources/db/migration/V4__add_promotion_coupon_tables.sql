CREATE TYPE promotion_type AS ENUM ('PERCENTAGE', 'FIXED_AMOUNT', 'FREE_SHIPPING');
CREATE TYPE discount_type AS ENUM ('PERCENTAGE', 'FIXED_AMOUNT', 'FREE_SHIPPING');

CREATE TABLE promotion (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    promotion_type promotion_type NOT NULL,
    value DECIMAL(10, 2),
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    min_order_amount DECIMAL(10, 2),
    max_uses INTEGER,
    current_uses INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT chk_promotion_value CHECK (value >= 0),
    CONSTRAINT chk_promotion_max_uses CHECK (max_uses >= 0),
    CONSTRAINT chk_promotion_current_uses CHECK (current_uses >= 0)
);

CREATE TABLE coupon (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    discount_type discount_type NOT NULL,
    discount_value DECIMAL(10, 2) NOT NULL,
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ,
    min_purchase DECIMAL(10, 2),
    max_discount DECIMAL(10, 2),
    usage_limit INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT chk_coupon_discount_value CHECK (discount_value >= 0),
    CONSTRAINT chk_coupon_used_count CHECK (used_count >= 0)
);

CREATE TABLE promotion_applicable_category (
    promotion_id UUID NOT NULL,
    category_id UUID NOT NULL,
    PRIMARY KEY (promotion_id, category_id),
    CONSTRAINT fk_promo_cat_promotion FOREIGN KEY (promotion_id) REFERENCES promotion(id) ON DELETE CASCADE,
    CONSTRAINT fk_promo_cat_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
);

CREATE TABLE promotion_applicable_product (
    promotion_id UUID NOT NULL,
    product_id UUID NOT NULL,
    PRIMARY KEY (promotion_id, product_id),
    CONSTRAINT fk_promo_prod_promotion FOREIGN KEY (promotion_id) REFERENCES promotion(id) ON DELETE CASCADE,
    CONSTRAINT fk_promo_prod_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);
