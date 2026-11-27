DROP TABLE IF EXISTS app_user;
DROP TABLE IF EXISTS trip;
DROP TABLE IF EXISTS vehicle;
DROP TABLE IF EXISTS driver;

CREATE TABLE driver (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    license_category VARCHAR(1) NOT NULL,
    affiliation_date DATE NOT NULL
);

CREATE TABLE vehicle (
    id VARCHAR(20) PRIMARY KEY,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    capacity_tons DOUBLE PRECISION NOT NULL
);

CREATE TABLE trip (
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

CREATE TABLE app_user (
    username VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
