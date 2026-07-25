CREATE TABLE books (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255)   NOT NULL,
    author          VARCHAR(255)   NOT NULL,
    isbn            VARCHAR(32)    NOT NULL,
    price           NUMERIC(10, 2) NOT NULL,
    published_year  INTEGER,
    created_at      TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP      NOT NULL,
    CONSTRAINT uq_books_isbn UNIQUE (isbn)
);

CREATE INDEX idx_books_author ON books (author);
