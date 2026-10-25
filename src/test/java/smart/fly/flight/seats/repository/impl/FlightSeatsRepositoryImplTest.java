package smart.fly.flight.seats.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import smart.fly.flight.model.Flight;
import smart.fly.flight.seats.model.FlightSeats;
import smart.fly.flight.seats.repository.FlightSeatsRepository;
import smart.fly.flight.seats.repository.impl.FlightSeatsRepositoryImpl.FlightSeatAvailability;
import smart.fly.repository.RepositoryTestBase;
import smart.fly.seatclass.model.SeatClass;

class FlightSeatsRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private FlightSeatsRepository flightSeatsRepository;

    @Test
    void findByFlightAndClass_mapsAllColumns() {
        FlightSeats seats = flightSeatsRepository.findByFlightAndClass(1, 2);

        assertEquals(2, seats.getFlightSeatId());
        assertEquals(1, seats.getFlight().getFlightId());
        assertEquals(2, seats.getSeatClass().getSeatClassId());
        assertEquals(0, seats.getAvailableSeats());
        assertEquals(30, seats.getTotalSeats());
        assertEquals(12000.0, seats.getPrice());
    }

    @Test
    void findByFlightAndClass_missing_throwsEmptyResult() {
        assertThrows(EmptyResultDataAccessException.class, () -> flightSeatsRepository.findByFlightAndClass(1, 3));
    }

    @Test
    void findByFlight_returnsEveryClassForFlight() {
        assertEquals(2, flightSeatsRepository.findByFlight(1).size());
        assertTrue(flightSeatsRepository.findByFlight(999).isEmpty());
    }

    @Test
    void findByid_returnsRow() {
        assertEquals(2, flightSeatsRepository.findByid(3).getFlight().getFlightId());
    }

    @Test
    void updateAvailableSeats_setsNewCount() {
        flightSeatsRepository.updateAvailableSeats(1, 97);

        assertEquals(97, flightSeatsRepository.findByid(1).getAvailableSeats());
    }

    @Test
    void save_insertsSeatInventory() {
        FlightSeats seats = new FlightSeats();
        Flight flight = new Flight();
        flight.setFlightId(2);
        seats.setFlight(flight);
        SeatClass seatClass = new SeatClass();
        seatClass.setSeatClassId(3);
        seats.setSeatClass(seatClass);
        seats.setAvailableSeats(8);
        seats.setTotalSeats(8);
        seats.setPrice(30000);

        flightSeatsRepository.save(seats);

        FlightSeats saved = flightSeatsRepository.findByFlightAndClass(2, 3);
        assertEquals(8, saved.getTotalSeats());
        assertEquals(30000.0, saved.getPrice());
    }

    @Test
    void findFlightSeatAvailability_joinsFlightRouteAirlineAndClass() {
        List<FlightSeatAvailability> rows = flightSeatsRepository.findFlightSeatAvailability();

        assertEquals(5, rows.size());
        FlightSeatAvailability business = rows.stream()
                .filter(r -> r.getFlightNumber().equals("AI101") && r.getSeatClassName().equals("Business"))
                .findFirst().orElseThrow();
        assertEquals("Indira Gandhi International", business.getDepartureAirport());
        assertEquals("Chhatrapati Shivaji Maharaj International", business.getArrivalAirport());
        assertEquals(LocalDateTime.of(2030, 1, 10, 8, 0), business.getDepartureDateTime());
        assertEquals(LocalDateTime.of(2030, 1, 10, 10, 15), business.getArrivalDateTime());
        assertEquals("Scheduled", business.getStatus());
        assertEquals("Air India", business.getAirlineName());
        assertEquals(0, business.getAvailableSeats());
        assertEquals(30, business.getTotalSeats());
        assertEquals(12000.0, business.getSeatPrice());
    }
}
