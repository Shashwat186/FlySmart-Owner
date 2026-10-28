-- Baseline schema, reconstructed from the SQL the repositories issue.
-- AUTO_INCREMENT starts at 1000 so test seed rows (ids < 1000) never collide with generated keys.

CREATE TABLE user (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    role VARCHAR(20),
    gender VARCHAR(10),
    dob DATE,
    password_hash VARCHAR(255),
    password_salt VARCHAR(255),
    profile_photo LONGBLOB
) AUTO_INCREMENT = 1000;

CREATE TABLE business_owner (
    bo_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT
) AUTO_INCREMENT = 1000;

CREATE TABLE flight_manager (
    fm_id INT AUTO_INCREMENT PRIMARY KEY,
    bo_id INT,
    user_id INT
) AUTO_INCREMENT = 1000;

CREATE TABLE airline (
    airline_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    code VARCHAR(10) UNIQUE,
    contact_info VARCHAR(255)
) AUTO_INCREMENT = 1000;

CREATE TABLE airport (
    airport_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    code VARCHAR(10) UNIQUE,
    contact_info VARCHAR(255),
    location VARCHAR(100)
) AUTO_INCREMENT = 1000;

CREATE TABLE seat_class (
    seat_class_id INT AUTO_INCREMENT PRIMARY KEY,
    class_name VARCHAR(50),
    price DOUBLE
) AUTO_INCREMENT = 1000;

CREATE TABLE flight (
    flight_id INT AUTO_INCREMENT PRIMARY KEY,
    flight_number VARCHAR(20) UNIQUE,
    airline_id INT,
    departure_airport_id INT,
    arrival_airport_id INT,
    departure_date_time DATETIME,
    arrival_date_time DATETIME,
    status VARCHAR(20),
    fm_id INT
) AUTO_INCREMENT = 1000;

CREATE TABLE flight_seats (
    flight_seat_id INT AUTO_INCREMENT PRIMARY KEY,
    flight_id INT,
    seat_class_id INT,
    available_seats INT,
    total_seats INT,
    price DOUBLE
) AUTO_INCREMENT = 1000;

CREATE TABLE booking (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    passenger_id INT,
    flight_id INT,
    seat_class_id INT,
    booking_date_time DATETIME,
    number_of_seats INT,
    total_price DOUBLE,
    number_of_adults INT,
    number_of_children INT,
    status VARCHAR(20) DEFAULT 'BOOKED'
) AUTO_INCREMENT = 1000;

CREATE TABLE passenger (
    passenger_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT,
    name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    age INT,
    loyalty_points INT DEFAULT 0,
    is_registered BOOLEAN,
    link_reg_id INT,
    user_id INT
) AUTO_INCREMENT = 1000;

CREATE TABLE payment (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT,
    payment_mode VARCHAR(20),
    card_number VARCHAR(30),
    card_holder_name VARCHAR(100),
    expiry_date VARCHAR(20),
    card_type VARCHAR(20),
    transaction_id VARCHAR(100),
    upi_id VARCHAR(100),
    wallet_name VARCHAR(50),
    amount DOUBLE,
    payment_date_time DATETIME,
    status VARCHAR(20)
) AUTO_INCREMENT = 1000;

CREATE TABLE feedback (
    feedback_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    booking_id INT,
    feedback_date_time DATETIME,
    rating INT,
    feedback_content VARCHAR(1000),
    status VARCHAR(20)
) AUTO_INCREMENT = 1000;

CREATE TABLE action_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action_performed VARCHAR(255),
    controller_name VARCHAR(100),
    timestamp DATETIME
) AUTO_INCREMENT = 1000;

CREATE TABLE password_reset_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100),
    token VARCHAR(255) UNIQUE,
    expiry_date DATETIME
) AUTO_INCREMENT = 1000;
