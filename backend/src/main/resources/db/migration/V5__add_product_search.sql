CREATE EXTENSION IF NOT EXISTS ltree;

CREATE EXTENSION IF NOT EXISTS vector;


-- Categories

ALTER TABLE category
    DROP CONSTRAINT IF EXISTS category_name_key,
    DROP CONSTRAINT IF EXISTS category_slug_key,
    ADD COLUMN path ltree NOT NULL UNIQUE;

CREATE INDEX idx_category_path
    ON category USING GIST (path);

CREATE OR REPLACE FUNCTION update_category_path() RETURNS TRIGGER AS $$
DECLARE
    parent_path ltree;
    clean_slug text;
BEGIN
    clean_slug := REPLACE(NEW.slug, '-', '_');

    IF NEW.parent_id IS NULL THEN
        NEW.path := clean_slug::ltree;
    ELSE
        SELECT path INTO parent_path FROM category WHERE id = NEW.parent_id;
        NEW.path := parent_path || clean_slug::ltree;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_category_path
  BEFORE INSERT OR UPDATE OF parent_id, slug
  ON category
  FOR EACH ROW EXECUTE FUNCTION update_category_path();


-- Products

ALTER TABLE product
    ADD COLUMN category_text TEXT DEFAULT '',
    ADD COLUMN IF NOT EXISTS embedding VECTOR(768);

CREATE INDEX idx_product_full_text_search
    ON product USING GIN (
        (
            setweight(to_tsvector('english', name), 'A') ||
            setweight(to_tsvector('english', COALESCE(category_text, '')), 'B') ||
            setweight(to_tsvector('english', COALESCE(description, '')), 'C')
        )
    );

CREATE INDEX idx_product_embedding
    ON product USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
