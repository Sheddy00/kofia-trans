-- Speeds up trip lookups by driver and period (revenue, statistics, filtered and paginated lists).
CREATE INDEX IF NOT EXISTS idx_trip_driver_date ON trip (driver_id, trip_date);
CREATE INDEX IF NOT EXISTS idx_trip_date ON trip (trip_date);
