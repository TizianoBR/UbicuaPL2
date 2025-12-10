\c postgres

DROP DATABASE IF EXISTS ubicuabbdd;
CREATE DATABASE ubicuabbdd;
\c ubicuabbdd

BEGIN;

CREATE TABLE sensors (
    sensor_id TEXT,
    street_id CHAR(7),
    sensor_type TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    district TEXT,
    neighborhood TEXT,
    PRIMARY KEY (sensor_id)
);

CREATE TABLE traffic_light (
    sensor_id TEXT,
    time timestamp,
    current_state TEXT,
    cycle_position INTEGER,
    time_remaining INTEGER,
    traffic_light_type TEXT,
    circulation_dir TEXT,
    pedestrian_waiting BOOLEAN,
    pedestrian_button_pressed BOOLEAN,
    malfunction_detected BOOLEAN,
    cycle_count INTEGER,
    state_changed BOOLEAN,
    last_state_change timestamp,
    PRIMARY KEY (sensor_id, time),
    FOREIGN KEY (sensor_id) REFERENCES sensors(sensor_id)
);


CREATE TABLE measurements(
    time timestamp,
    value INTEGER,
    PRIMARY KEY (time)
);
INSERT INTO measurements (time, value) VALUES
('2024-06-01 12:00:00', 100),
('2024-06-01 13:00:00', 150),
('2024-06-01 14:00:00', 200);

-- Inserts
INSERT INTO sensors (sensor_id, sensor_type, latitude, longitude, district, neighborhood) VALUES
('sensor_001', 'traffic_light', 40.4172, -3.7042, 'Centro', 'Calle Mayor');

INSERT INTO traffic_light (sensor_id, time, current_state, cycle_position, time_remaining, traffic_light_type, circulation_dir, pedestrian_waiting, pedestrian_button_pressed, malfunction_detected, cycle_count, state_changed, last_state_change) VALUES
('sensor_001', '2024-06-01 12:10:00', 'green', 3, 25, 'vehicle', 'north_south', TRUE, TRUE, FALSE, 15, TRUE, '2024-06-01 12:05:00');
    
COMMIT;