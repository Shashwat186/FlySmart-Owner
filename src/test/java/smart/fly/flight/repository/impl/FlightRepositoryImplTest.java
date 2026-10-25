package smart.fly.flight.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import smart.fly.airline.model.Airline;
import smart.fly.airport.model.Airport;
import smart.fly.dto.FlightSearchDTO;
import smart.fly.exception.FlightManagementException;
import smart.fly.flight.model.Flight;
import smart.fly.flight.repository.FlightRepository;
import smart.fly.flightmanager.model.FlightManager;
import smart.fly.repository.RepositoryTestBase;

class FlightRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private FlightRepository flightRepository;

    private Flight newFlight(String number) {
        Flight flight = new Flight();
        flight.setFlightNumber(number);
        flight.setAirline(new Airline(2, null, null, null));
        flight.setDepartureAirport(new Airport(3, null, null, null, null));
        flight.setArrivalAirport(new Airport(1, null, null, null, null));
        flight.setDepartureDateTime(Timestamp.valueOf("2030-03-01 07:00:00"));
        flight.setArrivalDateTime(Timestamp.valueOf("2030-03-01 09:40:00"));
        flight.setStatus("Scheduled");
        flight.setFlightManager(new FlightManager(2));
        return flight;
    }

    @Test
    void findById_mapsFlightWithAirlineAndAirports() {
        Flight flight = flightRepository.findById(1);

        assertEquals(1, flight.getFlightId());
        assertEquals("AI101", flight.getFlightNumber());
        assertEquals(1, flight.getFmId());
        assertEquals("Scheduled", flight.getStatus());
        assertEquals(Timestamp.valueOf("2030-01-10 08:00:00"), flight.getDepartureDateTime());
        assertEquals(Timestamp.valueOf("2030-01-10 10:15:00"), flight.getArrivalDateTime());
        assertEquals("Air India", flight.getAirline().getName());
        assertEquals("AI", flight.getAirline().getCode());
        assertEquals(1, flight.getDepartureAirport().getAirportId());
        assertEquals("DEL", flight.getDepartureAirport().getCode());
        assertEquals(2, flight.getArrivalAirport().getAirportId());
        assertEquals("BOM", flight.getArrivalAirport().getCode());
    }

    @Test
    void findById_unknown_throwsFlightManagementException() {
        FlightManagementException ex = assertThrows(FlightManagementException.class,
                () -> flightRepository.findById(999));
        assertEquals("Flight not found with ID: 999", ex.getMessage());
    }

    @Test
    @Disabled("Known bug: findByNumber does not select FM_ID, which FlightRowMapper reads, so it always throws")
    void findByNumber_returnsFlight() {
        assertEquals(2, flightRepository.findByNumber("6E202").getFlightId());
    }

    @Test
    void findByManagerId_returnsManagersFlightsByDepartureTime() {
        assertEquals(List.of("AI101", "6E202"),
                flightRepository.findByManagerId(1).stream().map(Flight::getFlightNumber).toList());
    }

    @Test
    void save_insertsFlight() {
        flightRepository.save(newFlight("6E999"));

        assertEquals(5, flightRepository.countTotalFlight());
        Flight saved = flightRepository.findAll().stream()
                .filter(f -> f.getFlightNumber().equals("6E999")).findFirst().orElseThrow();
        assertEquals("IndiGo", saved.getAirline().getName());
        assertEquals("BLR", saved.getDepartureAirport().getCode());
        assertEquals(2, saved.getFmId());
    }

    @Test
    void update_changesAllColumns() {
        Flight flight = newFlight("AI101X");
        flight.setFlightId(1);
        flight.setStatus("Delayed");

        flightRepository.update(flight);

        Flight updated = flightRepository.findById(1);
        assertEquals("AI101X", updated.getFlightNumber());
        assertEquals("Delayed", updated.getStatus());
        assertEquals("BLR", updated.getDepartureAirport().getCode());
        assertEquals(2, updated.getFmId());
    }

    @Test
    void delete_removesFlight() {
        flightRepository.delete(4);

        assertEquals(3, flightRepository.countTotalFlight());
        assertThrows(FlightManagementException.class, () -> flightRepository.findById(4));
    }

    @Test
    void statusCounters_countByStatus() {
        assertEquals(1, flightRepository.scheduledFlights());
        assertEquals(1, flightRepository.delayedFlights());
        assertEquals(1, flightRepository.completedFlights());
        assertEquals(4, flightRepository.countTotalFlight());
    }

    @Test
    void updateStatus_changesOnlyStatus() {
        flightRepository.updateStatus(1, "Delayed");

        assertEquals("Delayed", flightRepository.findById(1).getStatus());
        assertEquals(2, flightRepository.delayedFlights());
    }

    @Test
    void findAll_isOrderedByDepartureTime() {
        assertEquals(List.of("AI303", "AI101", "6E202", "AI404"),
                flightRepository.findAll().stream().map(Flight::getFlightNumber).toList());
    }

    @Test
    void findAllFlightManagersWithUsers_returnsManagerUsers() {
        List<FlightManager> managers = flightRepository.findAllFlightManagersWithUsers();

        assertEquals(2, managers.size());
        FlightManager ravi = managers.stream().filter(m -> m.getFmId() == 1).findFirst().orElseThrow();
        assertEquals("Ravi Kumar", ravi.getUser().getName());
        assertEquals("MANAGER", ravi.getUser().getRole());
        assertEquals(1, ravi.getBusinessOwner().getBoId());
    }

    @Test
    void perManagerCounters_countFlightsOfManagerUser() {
        assertEquals(2, flightRepository.getTotalFlightsByUserId(2));
        assertEquals(1, flightRepository.getScheduledFlightsByUserId(2));
        assertEquals(1, flightRepository.getDelayedFlightsByUserId(2));
        assertEquals(0, flightRepository.getScheduledFlightsByUserId(4));
        assertEquals(0, flightRepository.getTotalFlightsByUserId(3));
    }

    @Test
    void getTotalFlightsByFlightManagerId_returnsManagersFlights() {
        assertEquals(List.of(3, 4), flightRepository.getTotalFlightsByFlightManagerId(2).stream()
                .map(Flight::getFlightId).sorted().toList());
    }

    @Test
    @Disabled("Known bug: findAllForCustomers does not select FM_ID, which FlightRowMapper reads, so it always throws")
    void findAllForCustomers_returnsAllFlights() {
        assertEquals(4, flightRepository.findAllForCustomers().size());
    }

    @Test
    void searchFlights_filtersByRouteDateClassAndAvailability() {
        List<FlightSearchDTO> results = flightRepository.searchFlights(1, 2, Date.valueOf("2026-10-25"), 1);

        assertEquals(1, results.size());
        FlightSearchDTO dto = results.get(0);
        assertEquals("AI101", dto.getFlightNumber());
        assertEquals("Scheduled", dto.getStatus());
        assertEquals(5000.0, dto.getPrice());
        assertEquals(135, dto.getDuration());
        assertEquals("DEL", dto.getDepartureAirport().getCode());
        assertEquals("BOM", dto.getArrivalAirport().getCode());
        assertEquals("Air India", dto.getAirline().getName());
        assertNotNull(dto.getDepartureDate());
    }

    @Test
    void searchFlights_classWithNoSeatsLeft_returnsNothing() {
        assertTrue(flightRepository.searchFlights(1, 2, Date.valueOf("2026-10-25"), 2).isEmpty());
    }

    @Test
    @Disabled("Known bug: with no seat class the SQL has 3 placeholders but 4 arguments are always bound")
    void searchFlights_withoutSeatClass_searchesAllClasses() {
        assertFalse(flightRepository.searchFlights(1, 2, Date.valueOf("2026-10-25"), null).isEmpty());
    }
}
