ALTER TABLE wishlist_item
    ADD COLUMN created_at TIMESTAMPTZ;

UPDATE wishlist_item
SET created_at = added_at
WHERE created_at IS NULL;

ALTER TABLE wishlist_item
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE wishlist_item
    DROP COLUMN added_at;

