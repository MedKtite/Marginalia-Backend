CREATE TABLE IF NOT EXISTS book_metadata_cache (
  isbn_13 TEXT PRIMARY KEY,
  source TEXT NOT NULL,
  title TEXT,
  authors TEXT[],
  publisher TEXT,
  published_year SMALLINT,
  page_count INT,
  description TEXT,
  cover_url TEXT,
  raw JSONB,
  fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '30 days')
);

ALTER TABLE book_metadata_cache DROP CONSTRAINT IF EXISTS book_metadata_cache_source_check;
ALTER TABLE book_metadata_cache ADD CONSTRAINT book_metadata_cache_source_check
  CHECK (source IN ('google_books','open_library','gutendex','wikidata','manual'));
CREATE INDEX IF NOT EXISTS idx_metadata_expires ON book_metadata_cache(expires_at);
