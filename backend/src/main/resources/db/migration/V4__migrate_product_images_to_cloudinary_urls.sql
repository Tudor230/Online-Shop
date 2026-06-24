ALTER TABLE product
    RENAME COLUMN image_placeholder TO image_id;

ALTER TABLE product
    ALTER COLUMN image_id TYPE VARCHAR(500);

ALTER TABLE product_image_gallery
    RENAME COLUMN image_label TO image_id;

ALTER TABLE product_image_gallery
    ALTER COLUMN image_id TYPE VARCHAR(500);

