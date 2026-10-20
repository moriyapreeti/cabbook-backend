CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  phone VARCHAR(20),
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE drivers (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
  vehicle_model VARCHAR(80) NOT NULL,
  vehicle_number VARCHAR(20) NOT NULL UNIQUE,
  available BOOLEAN NOT NULL DEFAULT FALSE,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  location_updated_at TIMESTAMP,
  avg_rating DOUBLE PRECISION NOT NULL DEFAULT 0,
  rating_count INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_drivers_available ON drivers(available, latitude, longitude);

CREATE TABLE promo_codes (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(30) NOT NULL UNIQUE,
  discount_percent INT NOT NULL,
  max_discount NUMERIC(10,2) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  expires_at TIMESTAMP
);

CREATE TABLE rides (
  id BIGSERIAL PRIMARY KEY,
  rider_id BIGINT NOT NULL REFERENCES users(id),
  driver_id BIGINT REFERENCES drivers(id),
  pickup_address VARCHAR(255) NOT NULL,
  pickup_lat DOUBLE PRECISION NOT NULL,
  pickup_lng DOUBLE PRECISION NOT NULL,
  drop_address VARCHAR(255) NOT NULL,
  drop_lat DOUBLE PRECISION NOT NULL,
  drop_lng DOUBLE PRECISION NOT NULL,
  distance_km NUMERIC(8,2) NOT NULL,
  duration_min INT NOT NULL,
  fare NUMERIC(10,2) NOT NULL,
  discount NUMERIC(10,2) NOT NULL DEFAULT 0,
  promo_code VARCHAR(30),
  status VARCHAR(20) NOT NULL,
  requested_at TIMESTAMP NOT NULL DEFAULT now(),
  accepted_at TIMESTAMP,
  started_at TIMESTAMP,
  completed_at TIMESTAMP,
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_rides_rider ON rides(rider_id, requested_at DESC);
CREATE INDEX idx_rides_driver ON rides(driver_id, requested_at DESC);
CREATE INDEX idx_rides_status ON rides(status);

CREATE TABLE payments (
  id BIGSERIAL PRIMARY KEY,
  ride_id BIGINT NOT NULL UNIQUE REFERENCES rides(id),
  amount NUMERIC(10,2) NOT NULL,
  currency VARCHAR(5) NOT NULL,
  status VARCHAR(20) NOT NULL,
  provider VARCHAR(20) NOT NULL,
  provider_ref VARCHAR(100) UNIQUE,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  paid_at TIMESTAMP
);

CREATE TABLE ratings (
  id BIGSERIAL PRIMARY KEY,
  ride_id BIGINT NOT NULL UNIQUE REFERENCES rides(id),
  driver_id BIGINT NOT NULL REFERENCES drivers(id),
  stars INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
  comment VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_ratings_driver ON ratings(driver_id);

CREATE TABLE support_tickets (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  ride_id BIGINT REFERENCES rides(id),
  subject VARCHAR(150) NOT NULL,
  message VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO promo_codes(code, discount_percent, max_discount) VALUES ('WELCOME20', 20, 100.00);
