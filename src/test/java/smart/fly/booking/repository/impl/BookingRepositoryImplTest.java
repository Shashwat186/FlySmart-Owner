package smart.fly.booking.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import smart.fly.booking.model.Booking;
import smart.fly.booking.repository.BookingRepository;
import smart.fly.flight.model.Flight;
import smart.fly.passenger.model.Passenger;
import smart.fly.repository.RepositoryTestBase;
import smart.fly.seatclass.model.SeatClass;

class BookingRepositoryImplTest extends RepositoryTestBase {

    private static final LocalDateTime TODAY = LocalDateTime.of(2026, 10, 25, 0, 0);

    @Autowired
    private BookingRepository bookingRepository;

    private static List<Integer> ids(List<Booking> bookings) {
        return bookings.stream().map(Booking::getBookingId).toList();
    }

    @Test
    void findById_mapsBasicColumns() {
        Booking booking = bookingRepository.findById(1);

        assertEquals(1, booking.getBookingId());
        assertEquals(3, booking.getUserId());
        assertEquals(1, booking.getFlight().getFlightId());
        assertEquals(1, booking.getSeatClass().getSeatClassId());
        assertEquals(Timestamp.valueOf("2026-10-01 10:00:00"), booking.getBookingDateTime());
        assertEquals(2, booking.getNumberOfSeats());
        assertEquals(10000.0, booking.getTotalPrice());
    }

    @Test
    void findById_unknown_throwsEmptyResult() {
        assertThrows(EmptyResultDataAccessException.class, () -> bookingRepository.findById(999));
    }

    @Test
    void findByPassenger_isNewestFirst() {
        assertEquals(List.of(3, 1, 2), ids(bookingRepository.findByPassenger(1)));
    }

    @Test
    void findByFlight_returnsFlightBookings() {
        assertEquals(List.of(1), ids(bookingRepository.findByFlight(1)));
    }

    @Test
    void findBookingByUserId_returnsAllUserBookings() {
        assertEquals(List.of(1, 2, 3), ids(bookingRepository.findBookingByUserId(3)).stream().sorted().toList());
        assertTrue(bookingRepository.findBookingByUserId(1).isEmpty());
    }

    @Test
    void save_insertsBooking() {
        Booking booking = new Booking();
        Passenger passenger = new Passenger();
        passenger.setPassengerId(2);
        booking.setPassenger(passenger);
        Flight flight = new Flight();
        flight.setFlightId(4);
        booking.setFlight(flight);
        SeatClass seatClass = new SeatClass();
        seatClass.setSeatClassId(1);
        booking.setSeatClass(seatClass);
        booking.setBookingDateTime(Timestamp.valueOf("2026-10-25 08:00:00"));
        booking.setNumberOfSeats(3);
        booking.setTotalPrice(15000);

        bookingRepository.save(booking);

        List<Booking> saved = bookingRepository.findByFlight(4);
        assertEquals(1, saved.size());
        assertEquals(3, saved.get(0).getNumberOfSeats());
        assertEquals(15000.0, saved.get(0).getTotalPrice());
    }

    @Test
    void cancel_deletesBooking() {
        bookingRepository.cancel(2);

        assertEquals(2, count("booking"));
    }

    @Test
    void updateSeats_changesSeatCount() {
        bookingRepository.updateSeats(1, 4);

        assertEquals(4, bookingRepository.findById(1).getNumberOfSeats());
    }

    @Test
    void findBookingDetailsById_joinsFlightAirportsAirlineAndClass() {
        Optional<Booking> result = bookingRepository.findBookingDetailsById(1);

        Booking booking = result.orElseThrow();
        assertEquals("BOOKED", booking.getStatus());
        assertEquals(2, booking.getNumberOfAdults());
        assertEquals(0, booking.getNumberOfChildren());
        Flight flight = booking.getFlight();
        assertEquals("AI101", flight.getFlightNumber());
        assertEquals(1, flight.getFmId());
        assertEquals(Timestamp.valueOf("2030-01-10 08:00:00"), flight.getDepartureDateTime());
        assertEquals("DEL", flight.getDepartureAirport().getCode());
        assertEquals("New Delhi", flight.getDepartureAirport().getLocation());
        assertEquals("BOM", flight.getArrivalAirport().getCode());
        assertEquals("Mumbai", flight.getArrivalAirport().getLocation());
        assertEquals("AI", flight.getAirline().getCode());
        assertEquals("Air India", flight.getAirline().getName());
        assertEquals("Economy", booking.getSeatClass().getName());
        assertEquals(5000.0, booking.getSeatClass().getPrice());
    }

    @Test
    void findBookingDetailsById_unknown_returnsEmpty() {
        assertTrue(bookingRepository.findBookingDetailsById(999).isEmpty());
    }

    @Test
    void findBookingDetailsByUserId_returnsDetailedBookings() {
        List<Booking> bookings = ((BookingRepositoryImpl) bookingRepository).findBookingDetailsByUserId(3);

        assertEquals(3, bookings.size());
        assertTrue(bookings.stream().allMatch(b -> b.getFlight().getAirline().getName() != null));
    }

    @Test
    void findUpcomingBookingsByUserId_returnsFutureDeparturesSoonestFirst() {
        assertEquals(List.of(1, 3), ids(bookingRepository.findUpcomingBookingsByUserId(3, TODAY)));
    }

    @Test
    void findPreviousBookingsByUserId_returnsPastDepartures() {
        assertEquals(List.of(2), ids(bookingRepository.findPreviousBookingsByUserId(3, TODAY)));
    }

    @Test
    void updateBookingStatus_returnsAffectedRowCount() {
        assertEquals(1, bookingRepository.updateBookingStatus(1, "CANCELLED"));
        assertEquals("CANCELLED", bookingRepository.findBookingDetailsById(1).orElseThrow().getStatus());
        assertEquals(0, bookingRepository.updateBookingStatus(999, "CANCELLED"));
    }
}
