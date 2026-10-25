package smart.fly.flight.search.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import smart.fly.dto.FlightSearchDTO;
import smart.fly.repository.RepositoryTestBase;

class FlightSearchRepositoryTest extends RepositoryTestBase {

    private static final Date TODAY = Date.valueOf("2026-10-25");

    @Autowired
    private FlightSearchRepository flightSearchRepository;

    private static List<String> numbers(List<FlightSearchDTO> flights) {
        return flights.stream().map(FlightSearchDTO::getFlightNumber).toList();
    }

    @Test
    void searchFlights_allFilters_returnsMatchingFlightWithDetails() {
        List<FlightSearchDTO> results = flightSearchRepository.searchFlights(1, 2, TODAY, 1, 2);

        assertEquals(List.of("AI101"), numbers(results));
        FlightSearchDTO flight = results.get(0);
        assertEquals(1, flight.getFlightId());
        assertEquals(5000.0, flight.getPrice());
        assertEquals(135, flight.getDuration());
        assertEquals("DEL", flight.getDepartureAirport().getCode());
        assertEquals(1, flight.getDepartureAirport().getAirportId());
        assertEquals("BOM", flight.getArrivalAirport().getCode());
        assertEquals("Air India", flight.getAirline().getName());
        assertEquals("AI", flight.getAirline().getCode());
    }

    @Test
    void searchFlights_withoutDateOrClass_includesPastCompletedFlightsAndSkipsCancelled() {
        // AI404 is Cancelled and the Business cabin on AI101 is sold out
        assertEquals(List.of("AI303", "AI101"), numbers(flightSearchRepository.searchFlights(1, 2, null, null, 2)));
    }

    @Test
    void searchFlights_tooManyPassengers_excludesFlightsWithoutEnoughSeats() {
        assertEquals(List.of("AI101"), numbers(flightSearchRepository.searchFlights(1, 2, null, null, 20)));
        assertTrue(flightSearchRepository.searchFlights(1, 2, null, null, 101).isEmpty());
    }

    @Test
    void searchFlights_soldOutClass_returnsNothing() {
        assertTrue(flightSearchRepository.searchFlights(1, 2, TODAY, 2, 1).isEmpty());
    }

    @Test
    void searchFlights_unknownRoute_returnsNothing() {
        assertTrue(flightSearchRepository.searchFlights(3, 1, null, null, 1).isEmpty());
    }
}
