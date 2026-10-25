package smart.fly.flight.add.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import smart.fly.repository.RepositoryTestBase;

class AddFlightRepositoryTest extends RepositoryTestBase {

    @Autowired
    private AddFlightRepository addFlightRepository;

    @Test
    void addFlight_insertsScheduledFlightAndReturnsGeneratedId() {
        int flightId = addFlightRepository.addFlight("6E777", 2, 3, 1, Timestamp.valueOf("2030-04-01 06:00:00"),
                Timestamp.valueOf("2030-04-01 08:50:00"), 2);

        assertTrue(flightId >= 1000);
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM flight WHERE flight_id = ?", flightId);
        assertEquals("6E777", row.get("flight_number"));
        assertEquals(2, row.get("airline_id"));
        assertEquals(3, row.get("departure_airport_id"));
        assertEquals(1, row.get("arrival_airport_id"));
        assertEquals("SCHEDULED", row.get("status"));
        assertEquals(2, row.get("fm_id"));
    }

    // AdminController relies on this to show "flight number already exists"
    @Test
    void addFlight_duplicateNumber_throwsDuplicateKey() {
        assertThrows(DuplicateKeyException.class, () -> addFlightRepository.addFlight("AI101", 1, 1, 2,
                Timestamp.valueOf("2030-04-01 06:00:00"), Timestamp.valueOf("2030-04-01 08:00:00"), 1));
    }

    @Test
    void addFlightSeats_insertsFullyAvailableInventory() {
        assertEquals(1, addFlightRepository.addFlightSeats(2, 3, 12, 25000.0));

        Map<String, Object> row = jdbcTemplate
                .queryForMap("SELECT * FROM flight_seats WHERE flight_id = 2 AND seat_class_id = 3");
        assertEquals(12, row.get("available_seats"));
        assertEquals(12, row.get("total_seats"));
        assertEquals(25000.0, row.get("price"));
    }
}
