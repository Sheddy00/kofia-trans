CREATE TABLE IF NOT EXISTS driver (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    license_category VARCHAR(1) NOT NULL,
    affiliation_date DATE NOT NULL
);
