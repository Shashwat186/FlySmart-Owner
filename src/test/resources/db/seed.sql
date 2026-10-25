-- Baseline data shared by all repository tests. Re-applied before every test method.

TRUNCATE TABLE user;
TRUNCATE TABLE business_owner;
TRUNCATE TABLE flight_manager;
TRUNCATE TABLE airline;
TRUNCATE TABLE airport;
TRUNCATE TABLE seat_class;
TRUNCATE TABLE flight;
TRUNCATE TABLE flight_seats;
TRUNCATE TABLE booking;
TRUNCATE TABLE passenger;
TRUNCATE TABLE payment;
TRUNCATE TABLE feedback;

INSERT INTO user (user_id, name, email, phone, role, gender, dob, password_hash, password_salt) VALUES
    (1, 'Olivia Owner', 'owner@flysmart.test', '9000000001', 'OWNER', 'Female', '1980-01-15', 'h1', 's1'),
    (2, 'Ravi Kumar', 'ravi@flysmart.test', '9000000002', 'MANAGER', 'Male', '1990-06-20', 'h2', 's2'),
    (3, 'Asha Rao', 'asha@flysmart.test', '9876543210', 'PASSENGER', 'Female', '1995-03-08', 'h3', 's3'),
    (4, 'Meera Iyer', 'meera@flysmart.test', '9000000004', 'MANAGER', 'Female', '1988-11-02', 'h4', 's4');

INSERT INTO business_owner (bo_id, user_id) VALUES (1, 1);

INSERT INTO flight_manager (fm_id, bo_id, user_id) VALUES (1, 1, 2), (2, 1, 4);

INSERT INTO airline (airline_id, name, code, contact_info) VALUES
    (1, 'Air India', 'AI', 'care@airindia.test'),
    (2, 'IndiGo', '6E', 'care@indigo.test');

INSERT INTO airport (airport_id, name, code, contact_info, location) VALUES
    (1, 'Indira Gandhi International', 'DEL', 'del@airport.test', 'New Delhi'),
    (2, 'Chhatrapati Shivaji Maharaj International', 'BOM', 'bom@airport.test', 'Mumbai'),
    (3, 'Kempegowda International', 'BLR', 'blr@airport.test', 'Bengaluru');

INSERT INTO seat_class (seat_class_id, class_name, price) VALUES
    (1, 'Economy', 5000), (2, 'Business', 12000), (3, 'First', 25000);

INSERT INTO flight (flight_id, flight_number, airline_id, departure_airport_id, arrival_airport_id,
                    departure_date_time, arrival_date_time, status, fm_id) VALUES
    (1, 'AI101', 1, 1, 2, '2030-01-10 08:00:00', '2030-01-10 10:15:00', 'Scheduled', 1),
    (2, '6E202', 2, 2, 3, '2030-01-11 12:00:00', '2030-01-11 13:45:00', 'Delayed', 1),
    (3, 'AI303', 1, 1, 2, '2020-05-01 06:00:00', '2020-05-01 08:00:00', 'Completed', 2),
    (4, 'AI404', 1, 1, 2, '2030-02-01 09:00:00', '2030-02-01 11:00:00', 'Cancelled', 2);

INSERT INTO flight_seats (flight_seat_id, flight_id, seat_class_id, available_seats, total_seats, price) VALUES
    (1, 1, 1, 100, 120, 5000),
    (2, 1, 2, 0, 30, 12000),
    (3, 2, 1, 50, 50, 4500),
    (4, 3, 1, 10, 120, 5000),
    (5, 4, 1, 100, 100, 5000);

INSERT INTO booking (booking_id, user_id, passenger_id, flight_id, seat_class_id, booking_date_time,
                     number_of_seats, total_price, number_of_adults, number_of_children, status) VALUES
    (1, 3, 1, 1, 1, '2026-10-01 10:00:00', 2, 10000, 2, 0, 'BOOKED'),
    (2, 3, 1, 3, 1, '2020-04-01 09:00:00', 1, 5000, 1, 0, 'BOOKED'),
    (3, 3, 1, 2, 1, '2026-10-05 18:30:00', 1, 4500, 1, 0, 'CANCELLED');

INSERT INTO passenger (passenger_id, booking_id, name, email, phone, age, loyalty_points, is_registered, link_reg_id, user_id) VALUES
    (1, 1, 'Asha Rao', 'asha@flysmart.test', '9876543210', 31, 120, TRUE, 3, 3),
    (2, 1, 'Kiran Rao', 'kiran@flysmart.test', '9123456780', 8, 0, FALSE, 3, NULL),
    (3, 2, 'Asha Rao', 'asha.alt@flysmart.test', '9876500000', 31, 0, TRUE, 3, 3);

INSERT INTO payment (payment_id, booking_id, payment_mode, card_number, card_holder_name, expiry_date, card_type,
                     transaction_id, upi_id, amount, payment_date_time, status) VALUES
    (1, 1, 'CARD', '4111111111111111', 'ASHA RAO', '2028-12-31', 'Credit', 'TXN-1', NULL, 10000, '2026-10-01 10:01:00', 'SUCCESS'),
    (2, 2, 'UPI', NULL, NULL, NULL, NULL, 'TXN-2', 'asha@upi', 5000, '2020-04-01 09:01:00', 'SUCCESS'),
    (3, 3, 'UPI', NULL, NULL, NULL, NULL, 'TXN-3', 'asha@upi', 4500, '2026-10-05 18:31:00', 'REFUNDED');

INSERT INTO feedback (feedback_id, user_id, booking_id, feedback_date_time, rating, feedback_content, status) VALUES
    (1, 3, 1, '2026-10-02 09:30:00', 5, 'Great crew', 'unchecked'),
    (2, 3, 2, '2020-05-02 10:00:00', 3, 'Delayed boarding', 'checked');
