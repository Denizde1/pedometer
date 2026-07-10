-- Pedometer database schema (PostgreSQL)

CREATE TABLE IF NOT EXISTS users (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(64) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One row per device-day. Steps are cumulative for that day, sent by the
-- Android client. Upserts on (user_id, device_id, day) let the app resync
-- the same day repeatedly (e.g. every 15 min) without creating duplicates.
CREATE TABLE IF NOT EXISTS step_records (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id   VARCHAR(128) NOT NULL,
    day         DATE NOT NULL,
    steps       INTEGER NOT NULL CHECK (steps >= 0),
    synced_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, device_id, day)
);

CREATE INDEX IF NOT EXISTS idx_step_records_user_day
    ON step_records (user_id, day);
