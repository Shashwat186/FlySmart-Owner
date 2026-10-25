package smart.fly.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;

import smart.fly.airline.model.Airline;
import smart.fly.airline.repository.AirlineRepository;
import smart.fly.airport.model.Airport;
import smart.fly.airport.repository.AirportRepository;
import smart.fly.businness.owner.model.BusinessOwner;
import smart.fly.dto.FlightFormDTO;
import smart.fly.exception.FlightManagementException;
import smart.fly.flight.model.Flight;
import smart.fly.flight.repository.FlightRepository;
import smart.fly.flight.seats.repository.FlightSeatsRepository;
import smart.fly.flightmanager.model.FlightManager;
import smart.fly.flightmanager.repository.FlightManagerRepository;
import smart.fly.seatclass.repository.SeatClassRepository;
import smart.fly.user.model.User;
import smart.fly.user.repository.UserRepository;
import smart.fly.util.PasswordUtil;

@ExtendWith(MockitoExtension.class)
class FlightManagementServiceTest {

    private static final String SEAT_INSERT = "INSERT INTO flight_seats (Flight_ID, Seat_Class_ID, Available_Seats, Total_Seats, price) VALUES (?, ?, ?, ?, ?)";

    @Mock
    private FlightRepository flightRepository;
    @Mock
    private FlightManagerRepository flightManagerRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AirlineRepository airlineRepository;
    @Mock
    private AirportRepository airportRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordUtil passwordUtil;
    @Mock
    private FlightSeatsRepository flightSeatsRepository;
    @Mock
    private SeatClassRepository seatClassRepository;
    @Mock
    private DataSource dataSource;

    @InjectMocks
    private FlightManagementService flightService;

    private Airline airline;
    private Airport delhi;
    private Airport mumbai;
    private FlightManager manager;
    private Flight flight;

    @BeforeEach
    void setUp() {
        airline = new Airline(1, "Air India", "AI", "contact@airindia.example");
        delhi = new Airport(2, "Indira Gandhi International", "DEL", "del@example.com", "Delhi");
        mumbai = new Airport(3, "Chhatrapati Shivaji Maharaj International", "BOM", "bom@example.com", "Mumbai");
        manager = new FlightManager(4);

        flight = new Flight();
        flight.setFlightNumber("AI101");
        flight.setDepartureDateTime(Timestamp.valueOf("2026-11-01 08:00:00"));
        flight.setArrivalDateTime(Timestamp.valueOf("2026-11-01 10:15:00"));
        flight.setStatus("SCHEDULED");
    }

    private void stubReferenceData() {
        when(airlineRepository.findById(1)).thenReturn(airline);
        when(airportRepository.findById(2)).thenReturn(delhi);
        when(airportRepository.findById(3)).thenReturn(mumbai);
        when(flightManagerRepository.findById(4)).thenReturn(manager);
    }

    // ---- simple queries ----

    @Test
    void getAllFlights_delegatesToRepository() {
        List<Flight> flights = List.of(flight);
        when(flightRepository.findAll()).thenReturn(flights);

        assertEquals(flights, flightService.getAllFlights());
    }

    @Test
    void getFlightsByManager_delegatesToRepository() {
        List<Flight> flights = List.of(flight);
        when(flightRepository.findByManagerId(4)).thenReturn(flights);

        assertEquals(flights, flightService.getFlightsByManager(4));
    }

    @Test
    void getFlightById_delegatesToRepository() {
        when(flightRepository.findById(9)).thenReturn(flight);

        assertSame(flight, flightService.getFlightById(9));
    }

    // ---- addFlight ----

    @Test
    void addFlight_validInput_wiresRelationshipsAndSaves() {
        stubReferenceData();

        flightService.addFlight(flight, 1, 2, 3, 4, 100);

        assertSame(airline, flight.getAirline());
        assertSame(delhi, flight.getDepartureAirport());
        assertSame(mumbai, flight.getArrivalAirport());
        assertSame(manager, flight.getFlightManager());
        verify(flightRepository).save(flight);
    }

    @Test
    void addFlight_missingId_throwsWithoutLookups() {
        FlightManagementException ex = assertThrows(FlightManagementException.class,
                () -> flightService.addFlight(flight, null, 2, 3, 4, 100));

        assertEquals("All required IDs must be provided", ex.getMessage());
        verifyNoInteractions(airlineRepository, airportRepository, flightRepository);
    }

    @Test
    void addFlight_missingManagerId_throws() {
        assertThrows(FlightManagementException.class, () -> flightService.addFlight(flight, 1, 2, 3, null, 100));
    }

    @Test
    void addFlight_unknownReference_throws() {
        when(airlineRepository.findById(1)).thenReturn(airline);
        when(airportRepository.findById(2)).thenReturn(delhi);
        when(airportRepository.findById(3)).thenReturn(null);
        when(flightManagerRepository.findById(4)).thenReturn(manager);

        FlightManagementException ex = assertThrows(FlightManagementException.class,
                () -> flightService.addFlight(flight, 1, 2, 3, 4, 100));

        assertEquals("Invalid reference data provided", ex.getMessage());
        verify(flightRepository, never()).save(any());
    }

    @Test
    void addFlight_arrivalBeforeDeparture_throws() {
        stubReferenceData();
        flight.setArrivalDateTime(Timestamp.valueOf("2026-11-01 07:00:00"));

        FlightManagementException ex = assertThrows(FlightManagementException.class,
                () -> flightService.addFlight(flight, 1, 2, 3, 4, 100));

        assertEquals("Arrival time must be after departure time", ex.getMessage());
        verify(flightRepository, never()).save(any());
    }

    // ---- updateFlightFromDTO ----

    private FlightFormDTO form() {
        return new FlightFormDTO("AI202", 1, 2, 3, "2026-12-05T06:30", "2026-12-05T08:45", 4);
    }

    @Test
    void updateFlightFromDTO_validForm_updatesExistingFlight() {
        Flight existing = new Flight();
        existing.setFlightId(9);
        when(flightRepository.findById(9)).thenReturn(existing);
        stubReferenceData();

        flightService.updateFlightFromDTO(9, form());

        assertEquals("AI202", existing.getFlightNumber());
        assertEquals(Timestamp.valueOf("2026-12-05 06:30:00"), existing.getDepartureDateTime());
        assertEquals(Timestamp.valueOf("2026-12-05 08:45:00"), existing.getArrivalDateTime());
        assertSame(airline, existing.getAirline());
        assertSame(delhi, existing.getDepartureAirport());
        assertSame(mumbai, existing.getArrivalAirport());
        assertSame(manager, existing.getFlightManager());
        verify(flightRepository).update(existing);
    }

    @Test
    void updateFlightFromDTO_flightMissing_throws() {
        when(flightRepository.findById(9)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> flightService.updateFlightFromDTO(9, form()));
        assertEquals("Flight not found", ex.getMessage());
    }

    @Test
    void updateFlightFromDTO_airlineMissing_throws() {
        when(flightRepository.findById(9)).thenReturn(new Flight());
        when(airlineRepository.findById(1)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> flightService.updateFlightFromDTO(9, form()));
        assertEquals("Airline not found", ex.getMessage());
        verify(flightRepository, never()).update(any());
    }

    @Test
    void updateFlightFromDTO_departureAirportMissing_throws() {
        when(flightRepository.findById(9)).thenReturn(new Flight());
        when(airlineRepository.findById(1)).thenReturn(airline);
        when(airportRepository.findById(2)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> flightService.updateFlightFromDTO(9, form()));
        assertEquals("Departure airport not found", ex.getMessage());
    }

    @Test
    void updateFlightFromDTO_arrivalAirportMissing_throws() {
        when(flightRepository.findById(9)).thenReturn(new Flight());
        when(airlineRepository.findById(1)).thenReturn(airline);
        when(airportRepository.findById(2)).thenReturn(delhi);
        when(airportRepository.findById(3)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> flightService.updateFlightFromDTO(9, form()));
        assertEquals("Arrival airport not found", ex.getMessage());
    }

    @Test
    void updateFlightFromDTO_managerMissing_throws() {
        when(flightRepository.findById(9)).thenReturn(new Flight());
        when(airlineRepository.findById(1)).thenReturn(airline);
        when(airportRepository.findById(2)).thenReturn(delhi);
        when(airportRepository.findById(3)).thenReturn(mumbai);
        when(flightManagerRepository.findById(4)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> flightService.updateFlightFromDTO(9, form()));
        assertEquals("Manager not found", ex.getMessage());
    }

    @Test
    void updateFlightFromDTO_badDateFormat_throws() {
        when(flightRepository.findById(9)).thenReturn(new Flight());
        FlightFormDTO form = form();
        form.setDepartureDateTime("05/12/2026 06:30");

        assertThrows(java.time.format.DateTimeParseException.class, () -> flightService.updateFlightFromDTO(9, form));
        verify(flightRepository, never()).update(any());
    }

    // ---- updateFlight ----

    @Test
    void updateFlight_issuesUpdateWithAllColumns() {
        Timestamp dep = Timestamp.valueOf("2026-11-01 08:00:00");
        Timestamp arr = Timestamp.valueOf("2026-11-01 10:00:00");

        flightService.updateFlight(9, "AI101", 1, 2, 3, dep, arr, 4, "DELAYED");

        verify(jdbcTemplate).update(startsWith("UPDATE flight SET flight_number = ?"), eq("AI101"), eq(1), eq(2),
                eq(3), eq(dep), eq(arr), eq(4), eq("DELAYED"), eq(9));
    }

    // ---- deleteFlight ----

    @Test
    void deleteFlight_existing_deletes() {
        when(flightRepository.findById(9)).thenReturn(flight);

        flightService.deleteFlight(9);

        verify(flightRepository).delete(9);
    }

    @Test
    void deleteFlight_missing_throwsWrappedNotFound() {
        when(flightRepository.findById(9)).thenReturn(null);

        FlightManagementException ex = assertThrows(FlightManagementException.class, () -> flightService.deleteFlight(9));

        assertEquals("Failed to delete flight: Flight not found", ex.getMessage());
        verify(flightRepository, never()).delete(anyInt());
    }

    @Test
    void deleteFlight_repositoryFailure_wrapsException() {
        when(flightRepository.findById(9)).thenReturn(flight);
        doThrow(new RuntimeException("FK constraint")).when(flightRepository).delete(9);

        FlightManagementException ex = assertThrows(FlightManagementException.class, () -> flightService.deleteFlight(9));

        assertEquals("Failed to delete flight: FK constraint", ex.getMessage());
    }

    // ---- updateFlightStatus ----

    @Test
    void updateFlightStatus_setsNewStatusAndPersists() {
        when(flightRepository.findById(9)).thenReturn(flight);

        flightService.updateFlightStatus(9, "DELAYED");

        assertEquals("DELAYED", flight.getStatus());
        verify(flightRepository).update(flight);
    }

    // ---- createFlightManager ----

    @Test
    void createFlightManager_newEmail_hashesPasswordAndLinksOwner() {
        User user = new User();
        user.setEmail("mgr@example.com");
        User savedUser = new User();
        savedUser.setUserId(50);
        BusinessOwner owner = new BusinessOwner(8);
        when(userRepository.emailExists("mgr@example.com")).thenReturn(false);
        when(passwordUtil.generateSalt()).thenReturn("salt");
        when(passwordUtil.hashPassword("pw", "salt")).thenReturn("hash");
        when(userRepository.save(user)).thenReturn(savedUser);
        when(flightManagerRepository.save(any(FlightManager.class))).thenAnswer(inv -> inv.getArgument(0));

        FlightManager result = flightService.createFlightManager(user, "pw", owner);

        assertEquals("hash", user.getPasswordHash());
        assertEquals("salt", user.getPasswordSalt());
        assertEquals("Manager", user.getRole());
        assertSame(savedUser, result.getUser());
        assertSame(owner, result.getBusinessOwner());
    }

    @Test
    void createFlightManager_duplicateEmail_throws() {
        User user = new User();
        user.setEmail("mgr@example.com");
        when(userRepository.emailExists("mgr@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightService.createFlightManager(user, "pw", new BusinessOwner(8)));

        assertEquals("Email already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(flightManagerRepository, never()).save(any());
    }

    // ---- addFlightWithSeats ----

    private void stubGeneratedFlightId(Integer id) {
        when(jdbcTemplate.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenAnswer(inv -> {
            if (id != null) {
                KeyHolder keyHolder = inv.getArgument(1);
                keyHolder.getKeyList().add(Map.of("GENERATED_KEY", id));
            }
            return 1;
        });
    }

    @Test
    void addFlightWithSeats_insertsFlightThenAllThreeSeatClasses() throws Exception {
        stubReferenceData();
        stubGeneratedFlightId(77);
        when(jdbcTemplate.update(eq(SEAT_INSERT), any(), any(), any(), any(), any())).thenReturn(1);

        flightService.addFlightWithSeats(flight, 1, 2, 3, 4, 100, 120, 30, 8);

        assertEquals(77, flight.getFlightId());
        verify(jdbcTemplate).update(SEAT_INSERT, 77, 1, 120, 120, 0.0);
        verify(jdbcTemplate).update(SEAT_INSERT, 77, 2, 30, 30, 0.0);
        verify(jdbcTemplate).update(SEAT_INSERT, 77, 3, 8, 8, 0.0);

        // Run the captured statement creator to check the flight row's bound values
        ArgumentCaptor<PreparedStatementCreator> creator = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate).update(creator.capture(), any(KeyHolder.class));
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
        creator.getValue().createPreparedStatement(connection);
        verify(ps).setString(1, "AI101");
        verify(ps).setInt(2, 1);
        verify(ps).setInt(3, 2);
        verify(ps).setInt(4, 3);
        verify(ps).setString(7, "SCHEDULED");
        verify(ps).setInt(8, 4);
    }

    @Test
    void addFlightWithSeats_noGeneratedKey_throws() {
        stubReferenceData();
        stubGeneratedFlightId(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightService.addFlightWithSeats(flight, 1, 2, 3, 4, 100, 120, 30, 8));

        assertEquals("Failed to retrieve generated Flight ID.", ex.getMessage());
    }

    @Test
    void addFlightWithSeats_economyInsertFails_throws() {
        stubReferenceData();
        stubGeneratedFlightId(77);
        when(jdbcTemplate.update(SEAT_INSERT, 77, 1, 120, 120, 0.0)).thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightService.addFlightWithSeats(flight, 1, 2, 3, 4, 100, 120, 30, 8));

        assertEquals("Failed to insert Economy class seats", ex.getMessage());
    }

    @Test
    void addFlightWithSeats_businessInsertFails_throws() {
        stubReferenceData();
        stubGeneratedFlightId(77);
        when(jdbcTemplate.update(SEAT_INSERT, 77, 1, 120, 120, 0.0)).thenReturn(1);
        when(jdbcTemplate.update(SEAT_INSERT, 77, 2, 30, 30, 0.0)).thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightService.addFlightWithSeats(flight, 1, 2, 3, 4, 100, 120, 30, 8));

        assertEquals("Failed to insert Business class seats", ex.getMessage());
    }

    @Test
    void addFlightWithSeats_firstClassInsertFails_throws() {
        stubReferenceData();
        stubGeneratedFlightId(77);
        when(jdbcTemplate.update(SEAT_INSERT, 77, 1, 120, 120, 0.0)).thenReturn(1);
        when(jdbcTemplate.update(SEAT_INSERT, 77, 2, 30, 30, 0.0)).thenReturn(1);
        when(jdbcTemplate.update(SEAT_INSERT, 77, 3, 8, 8, 0.0)).thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> flightService.addFlightWithSeats(flight, 1, 2, 3, 4, 100, 120, 30, 8));

        assertEquals("Failed to insert First Class seats", ex.getMessage());
    }
}
