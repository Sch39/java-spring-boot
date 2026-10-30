CREATE TABLE IF NOT EXISTS review (
    review_id     TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    movie_info_id BIGINT NOT NULL,
    comment       TEXT NOT NULL,
    rating        DOUBLE PRECISION NOT NULL
);