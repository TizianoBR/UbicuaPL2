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
INSERT INTO sensors (sensor_id, street_id, sensor_type, latitude, longitude, district, neighborhood) VALUES
('sensor_001', 'ST_0001', 'traffic_light', 40.4172, -3.7042, 'Centro', 'Calle Mayor');

INSERT INTO traffic_light (sensor_id, time, current_state, cycle_position, time_remaining, traffic_light_type, circulation_dir, pedestrian_waiting, pedestrian_button_pressed, malfunction_detected, cycle_count, state_changed, last_state_change) VALUES
('sensor_001', '2024-06-01 12:10:00', 'Gr', 3, 25, 'vehicle', 'north_south', TRUE, TRUE, FALSE, 15, TRUE, '2024-06-01 12:05:00');

-- Additional seed data for sensors
INSERT INTO sensors (sensor_id, street_id, sensor_type, latitude, longitude, district, neighborhood) VALUES
('sensor_002', 'ST_0001', 'traffic_light', 40.4180, -3.7050, 'Centro', 'Puerta del Sol'),
('sensor_003', 'ST_0002', 'traffic_light', 40.4200, -3.7065, 'Centro', 'Gran Vía'),
('sensor_004', 'ST_0002', 'traffic_light', 40.4235, -3.7070, 'Salamanca', 'Goya'),
('sensor_005', 'ST_0003', 'traffic_light', 40.4300, -3.7000, 'Chamberí', 'Bilbao'),
('sensor_006', 'ST_0003', 'traffic_light', 40.4370, -3.6920, 'Chamartín', 'Castellana'),
('sensor_007', 'ST_0004', 'traffic_light', 40.4110, -3.6990, 'Arganzuela', 'Atocha'),
('sensor_008', 'ST_0004', 'traffic_light', 40.4050, -3.6860, 'Retiro', 'Menéndez Pelayo'),
('sensor_009', 'ST_0005', 'traffic_light', 40.4510, -3.6900, 'Tetuán', 'Bravo Murillo'),
('sensor_010', 'ST_0005', 'traffic_light', 40.3560, -3.6940, 'Carabanchel', 'General Ricardos');

-- Additional seed data for traffic_light per sensor
INSERT INTO traffic_light (sensor_id, time, current_state, cycle_position, time_remaining, traffic_light_type, circulation_dir, pedestrian_waiting, pedestrian_button_pressed, malfunction_detected, cycle_count, state_changed, last_state_change) VALUES
('sensor_002', '2024-06-01 12:10:00', 'Rg',   1, 50, 'vehicle', 'east_west', FALSE, FALSE, FALSE, 10, TRUE,  '2024-06-01 12:09:30'),
('sensor_002', '2024-06-03 12:11:00', 'Gr', 3, 40, 'vehicle', 'east_west', TRUE,  FALSE, FALSE, 11, TRUE,  '2024-06-01 12:10:30'),
('sensor_002', '2024-06-04 12:12:00', 'Yr', 2,  5, 'vehicle', 'east_west', TRUE,  TRUE,  FALSE, 12, TRUE,  '2024-06-01 12:11:55'),

('sensor_003', '2024-06-02 12:10:00', 'Gr', 3, 30, 'vehicle', 'north_south', FALSE, FALSE, FALSE, 20, TRUE, '2024-06-01 12:09:30'),
('sensor_003', '2024-06-02 12:11:00', 'Yr', 2, 10, 'vehicle', 'north_south', TRUE,  FALSE, FALSE, 21, TRUE, '2024-06-01 12:10:50'),
('sensor_003', '2024-06-05 12:12:00', 'Rr1',   1, 50, 'vehicle', 'north_south', TRUE,  TRUE,  FALSE, 22, TRUE, '2024-06-01 12:11:10'),

('sensor_004', '2024-06-01 12:10:00', 'Gr', 3, 25, 'vehicle', 'east_west',  FALSE, FALSE, FALSE,  8, TRUE, '2024-06-01 12:09:35'),
('sensor_004', '2024-06-03 12:11:00', 'Yr', 2,  7, 'vehicle', 'east_west',  TRUE,  TRUE,  FALSE,  9, TRUE, '2024-06-01 12:10:53'),
('sensor_004', '2024-06-03 12:12:00', 'Rr2',   1, 45, 'vehicle', 'east_west',  TRUE,  FALSE, FALSE, 10, TRUE, '2024-06-01 12:11:15'),

('sensor_005', '2024-06-02 12:10:00', 'Rg',   1, 55, 'vehicle', 'north_south', TRUE,  FALSE, FALSE, 30, TRUE, '2024-06-01 12:09:05'),
('sensor_005', '2024-06-04 12:11:00', 'Gr', 3, 35, 'vehicle', 'north_south', FALSE, FALSE, FALSE, 31, TRUE, '2024-06-01 12:10:25'),
('sensor_005', '2024-06-05 12:12:00', 'Yr', 2,  8, 'vehicle', 'north_south', TRUE,  TRUE,  FALSE, 32, TRUE, '2024-06-01 12:11:52'),

('sensor_006', '2024-06-03 12:10:00', 'Gr', 3, 20, 'vehicle', 'east_west',  FALSE, FALSE, FALSE, 40, TRUE, '2024-06-01 12:09:45'),
('sensor_006', '2024-06-04 12:11:00', 'Yr', 2, 12, 'vehicle', 'east_west',  TRUE,  FALSE, FALSE, 41, TRUE, '2024-06-01 12:10:48'),
('sensor_006', '2024-06-05 12:12:00', 'Rr1',   1, 48, 'vehicle', 'east_west',  TRUE,  TRUE,  FALSE, 42, TRUE, '2024-06-01 12:11:12'),

('sensor_007', '2024-06-01 12:10:00', 'Rg',   1, 52, 'vehicle', 'north_south', TRUE,  TRUE,  FALSE, 18, TRUE, '2024-06-01 12:09:08'),
('sensor_007', '2024-06-02 12:11:00', 'Gr', 3, 33, 'vehicle', 'north_south', FALSE, FALSE, FALSE, 19, TRUE, '2024-06-01 12:10:27'),
('sensor_007', '2024-06-05 12:12:00', 'Yr', 2,  6, 'vehicle', 'north_south', TRUE,  FALSE, FALSE, 20, TRUE, '2024-06-01 12:11:54'),

('sensor_008', '2024-06-04 12:10:00', 'Gr', 3, 28, 'vehicle', 'east_west',  FALSE, FALSE, FALSE, 26, TRUE, '2024-06-01 12:09:32'),
('sensor_008', '2024-06-04 12:11:00', 'Yr', 2, 11, 'vehicle', 'east_west',  TRUE,  TRUE,  FALSE, 27, TRUE, '2024-06-01 12:10:50'),
('sensor_008', '2024-06-05 12:12:00', 'Rg',   1, 42, 'vehicle', 'east_west',  TRUE,  FALSE, FALSE, 28, TRUE, '2024-06-01 12:11:18'),

('sensor_009', '2024-06-02 12:10:00', 'Rr2',   1, 49, 'vehicle', 'north_south', TRUE,  FALSE, FALSE, 35, TRUE, '2024-06-01 12:09:10'),
('sensor_009', '2024-06-03 12:11:00', 'Gr', 3, 31, 'vehicle', 'north_south', FALSE, FALSE, FALSE, 36, TRUE, '2024-06-01 12:10:29'),
('sensor_009', '2024-06-04 12:12:00', 'Yr', 2,  9, 'vehicle', 'north_south', TRUE,  TRUE,  FALSE, 37, TRUE, '2024-06-01 12:11:53'),

('sensor_010', '2024-06-01 12:10:00', 'Gr', 3, 22, 'vehicle', 'east_west',  FALSE, FALSE, FALSE, 45, TRUE, '2024-06-01 12:09:40'),
('sensor_010', '2024-06-04 12:11:00', 'Yr', 2, 13, 'vehicle', 'east_west',  TRUE,  FALSE, FALSE, 46, TRUE, '2024-06-01 12:10:49'),
('sensor_010', '2024-06-04 12:12:00', 'Rg',   1, 47, 'vehicle', 'east_west',  TRUE,  TRUE,  FALSE, 47, TRUE, '2024-06-01 12:11:13');
    
COMMIT;