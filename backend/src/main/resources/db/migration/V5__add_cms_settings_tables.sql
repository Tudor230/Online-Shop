CREATE TABLE cms_page (
    id UUID PRIMARY KEY,
    slug VARCHAR(200) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    meta_title VARCHAR(200),
    meta_description TEXT,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_cms_page_slug ON cms_page (slug);
CREATE INDEX idx_cms_page_published ON cms_page (is_published);

CREATE TABLE notification_template (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    subject VARCHAR(200),
    body_template TEXT NOT NULL,
    variables JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_notification_template_type ON notification_template (notification_type);
