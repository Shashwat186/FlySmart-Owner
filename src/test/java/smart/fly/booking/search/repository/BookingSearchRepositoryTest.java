package smart.fly.booking.search.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import smart.fly.flight.details.FlightDetails;
import smart.fly.repository.RepositoryTestBase;

class BookingSearchRepositoryTest extends RepositoryTestBase {

    @Autowired
    private BookingSearchRepository bookingSearchRepository;

    @Test
    void createBooking_insertsRowAndReturnsGeneratedId() throws Exception {
        int bookingId = bookingSearchRepository.createBooking(1, 1, 3, 15000.0, 3, 2, 1);

        assertTrue(bookingId >= 1000);
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM booking WHERE booking_id = ?", bookingId);
        assertEquals(1, row.get("flight_id"));
        assertEquals(3, row.get("number_of_seats"));
        assertEquals(15000.0, row.get("total_price"));
        assertEquals(3, row.get("user_id"));
        assertEquals(2, row.get("number_of_adults"));
        assertEquals(1, row.get("number_of_children"));
        assertNotNull(row.get("booking_date_time"));
    }

    @Test
    void verifyBookingExists_doesNotThrowForKnownOrUnknownIds() {
        assertDoesNotThrow(() -> bookingSearchRepository.verifyBookingExists(1));
        assertDoesNotThrow(() -> bookingSearchRepository.verifyBookingExists(999));
    }

    @Test
    void createPassenger_storesCoTraveller() throws Exception {
        bookingSearchRepository.createPassenger(1, "Neha Rao", "neha@flysmart.test", 29, false, 3);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM passenger WHERE name = 'Neha Rao'");
        assertEquals(1, row.get("booking_id"));
        assertEquals("neha@flysmart.test", row.get("email"));
        assertEquals(29, row.get("age"));
        assertEquals(false, row.get("is_registered"));
        assertEquals(3, row.get("link_reg_id"));
    }

    @Test
    void createPassenger_allowsMissingEmailAndAge() throws Exception {
        bookingSearchRepository.createPassenger(1, "Infant Rao", null, null, false, 3);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM passenger WHERE name = 'Infant Rao'");
        assertNull(row.get("email"));
        assertNull(row.get("age"));
    }

    @Test
    void createPayment_storesPaymentDetails() throws Exception {
        bookingSearchRepository.createPayment(2, "WALLET", null, null, null, null, "TXN-W", null, "Paytm", 5000.0);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM payment WHERE transaction_id = 'TXN-W'");
        assertEquals(2, row.get("booking_id"));
        assertEquals("WALLET", row.get("payment_mode"));
        assertEquals("Paytm", row.get("wallet_name"));
        assertEquals(5000.0, row.get("amount"));
        assertNotNull(row.get("payment_date_time"));
    }

    @Test
    void updateFlightSeats_decrementsAvailability() throws Exception {
        bookingSearchRepository.updateFlightSeats(1, 1, 4);

        assertEquals(96, jdbcTemplate.queryForObject(
                "SELECT available_seats FROM flight_seats WHERE flight_id = 1 AND seat_class_id = 1", Integer.class));
    }

    @Test
    void getFlightDetails_mapsJoinedFlightData() throws Exception {
        FlightDetails details = bookingSearchRepository.getFlightDetails(2);

        assertEquals("6E202", details.getFlightNumber());
        assertEquals("IndiGo", details.getAirlineName());
        assertEquals("6E", details.getAirlineCode());
        assertEquals("Chhatrapati Shivaji Maharaj International", details.getDepartureAirportName());
        assertEquals("BOM", details.getDepartureAirportCode());
        assertEquals("Kempegowda International", details.getArrivalAirportName());
        assertEquals("BLR", details.getArrivalAirportCode());
        assertTrue(details.getDepartureDateTime().startsWith("2030-01-11 12:00"));
        assertTrue(details.getArrivalDateTime().startsWith("2030-01-11 13:45"));
        assertEquals("Delayed", details.getFlightStatus());
    }

    @Test
    void getFlightDetails_unknownFlight_returnsNull() throws Exception {
        assertNull(bookingSearchRepository.getFlightDetails(999));
    }
}
