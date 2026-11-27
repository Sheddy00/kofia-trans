CREATE TABLE IF NOT EXISTS trip (
    id VARCHAR(20) PRIMARY KEY,
    driver_id VARCHAR(20) NOT NULL REFERENCES driver (id),
    vehicle_id VARCHAR(20) NOT NULL REFERENCES vehicle (id),
    trip_date DATE NOT NULL,
    departure_city VARCHAR(100) NOT NULL,
    arrival_city VARCHAR(100) NOT NULL,
    distance_km INTEGER NOT NULL,
    billed_amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL
);
