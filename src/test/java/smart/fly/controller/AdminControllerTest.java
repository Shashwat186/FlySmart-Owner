package smart.fly.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static smart.fly.controller.MockMvcSupport.mockMvcFor;
import static smart.fly.controller.MockMvcSupport.sessionFor;

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import smart.fly.airline.model.Airline;
import smart.fly.airline.repository.AirlineRepository;
import smart.fly.airport.model.Airport;
import smart.fly.airport.repository.AirportRepository;
import smart.fly.flight.add.repository.AddFlightRepository;
import smart.fly.flight.model.Flight;
import smart.fly.flight.repository.FlightRepository;
import smart.fly.flightmanager.model.FlightManager;
import smart.fly.flightmanager.repository.FlightManagerRepository;
import smart.fly.passenger.model.Passenger;
import smart.fly.service.FlightManagementService;
import smart.fly.service.LoggingService;
import smart.fly.service.PassengerService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private static final int OWNER_ID = 1;

    @Mock
    private FlightManagementService flightService;
    @Mock
    private FlightManagerRepository flightManagerRepository;
    @Mock
    private PassengerService passengerService;
    @Mock
    private AirlineRepository airlineRepository;
    @Mock
    private AirportRepository airportRepository;
    @Mock
    private FlightRepository flightRepository;
    @Mock
    private LoggingService loggingService;
    @Mock
    private AddFlightRepository addFlightRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvcFor(new AdminController(flightService, flightManagerRepository, passengerService,
                airlineRepository, airportRepository, flightRepository, loggingService, addFlightRepository));
    }

    // ---- dashboard ----

    @Test
    void showAdminDashboard_owner_redirectsToOwnerDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard").session(sessionFor(OWNER_ID, "owner")))
                .andExpect(view().name("redirect:/owner/dashboard"));
    }

    @Test
    void showAdminDashboard_nonOwner_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard").session(sessionFor(5, "PASSENGER")))
                .andExpect(view().name("redirect:/auth/login"));
    }

    // ---- flights: list / add ----

    @Test
    void listFlights_showsAllFlights() throws Exception {
        List<Flight> flights = List.of(new Flight());
        when(flightService.getAllFlights()).thenReturn(flights);

        mockMvc.perform(get("/admin/flights"))
                .andExpect(view().name("admin/flights/list"))
                .andExpect(model().attribute("flights", flights));
    }

    @Test
    void showAddFlightForm_loadsReferenceData() throws Exception {
        when(airlineRepository.findAll()).thenReturn(List.of());
        when(airportRepository.findAll()).thenReturn(List.of());
        when(flightRepository.findAllFlightManagersWithUsers()).thenReturn(List.of());

        mockMvc.perform(get("/admin/flights/add"))
                .andExpect(view().name("admin/flights/add"))
                .andExpect(model().attributeExists("airlines", "airports", "managers"))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @Test
    void showAddFlightForm_errorFlag_showsErrorMessage() throws Exception {
        mockMvc.perform(get("/admin/flights/add").param("error", "true"))
                .andExpect(model().attribute("error", "Failed to add flight. Please check your inputs."));
    }

    private MockHttpServletRequestBuilder addFlightRequest() {
        return post("/admin/flights/add")
                .param("flightNumber", "AI101").param("airlineId", "1")
                .param("departureAirportId", "2").param("arrivalAirportId", "3")
                .param("departureDateTime", "2026-11-01T08:00").param("arrivalDateTime", "2026-11-01T10:15")
                .param("managerId", "4")
                .param("economyTotalSeats", "120").param("businessTotalSeats", "30")
                .param("firstClassTotalSeats", "8");
    }

    private void stubAddFlightReferences() {
        when(airlineRepository.findById(1)).thenReturn(new Airline(1, "Air India", "AI", "x"));
        when(airportRepository.findById(2)).thenReturn(new Airport(2, "Delhi", "DEL", "x", "Delhi"));
        when(airportRepository.findById(3)).thenReturn(new Airport(3, "Mumbai", "BOM", "x", "Mumbai"));
        when(flightManagerRepository.findById(4)).thenReturn(new FlightManager(4));
    }

    @Test
    void addFlight_withoutSession_redirectsToLogin() throws Exception {
        mockMvc.perform(addFlightRequest()).andExpect(view().name("redirect:/auth/login"));

        verifyNoInteractions(addFlightRepository);
    }

    @Test
    void addFlight_success_insertsFlightAndPricedSeatClasses() throws Exception {
        stubAddFlightReferences();
        when(addFlightRepository.addFlight("AI101", 1, 2, 3, Timestamp.valueOf("2026-11-01 08:00:00"),
                Timestamp.valueOf("2026-11-01 10:15:00"), 4)).thenReturn(77);

        mockMvc.perform(addFlightRequest().session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("redirect:/admin/flights"))
                .andExpect(flash().attribute("success", "Flight added successfully"));

        verify(addFlightRepository).addFlightSeats(77, 1, 120, 5000.0);
        verify(addFlightRepository).addFlightSeats(77, 2, 30, 12000.0);
        verify(addFlightRepository).addFlightSeats(77, 3, 8, 25000.0);
        verify(loggingService).logAction(OWNER_ID, "Added flight", "AdminController");
    }

    @Test
    void addFlight_duplicateNumber_redisplaysFormWithInputs() throws Exception {
        stubAddFlightReferences();
        when(addFlightRepository.addFlight(anyString(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new DuplicateKeyException("dup"));

        mockMvc.perform(addFlightRequest().session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("admin/flights/add"))
                .andExpect(model().attribute("flightNumber", "AI101"))
                .andExpect(model().attribute("economyTotalSeats", 120))
                .andExpect(model().attribute("error",
                        "Flight number 'AI101' already exists. Please use a different flight number."));

        verify(loggingService).logAction(OWNER_ID, "Failed to add flight - Duplicate flight number: AI101",
                "AdminController");
        verify(addFlightRepository, never()).addFlightSeats(anyInt(), anyInt(), anyInt(), anyDouble());
    }

    @Test
    void addFlight_badDate_showsDateFormatError() throws Exception {
        mockMvc.perform(addFlightRequest().param("departureDateTime", "01/11/2026")
                .session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("admin/flights/add"))
                .andExpect(model().attribute("error",
                        "Invalid date format. Please ensure dates are in the correct format."));

        verifyNoInteractions(addFlightRepository);
    }

    @Test
    void addFlight_unexpectedError_showsFailureMessage() throws Exception {
        stubAddFlightReferences();
        when(addFlightRepository.addFlight(anyString(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("connection reset"));

        mockMvc.perform(addFlightRequest().session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("admin/flights/add"))
                .andExpect(model().attribute("error", "Failed to add flight: connection reset"));
    }

    // ---- flights: edit / delete ----

    @Test
    void showEditFlightForm_loadsFlightAndManagerId() throws Exception {
        Flight flight = new Flight();
        flight.setFlightManager(new FlightManager(4));
        when(flightService.getFlightById(9)).thenReturn(flight);

        mockMvc.perform(get("/admin/flights/edit/9"))
                .andExpect(view().name("admin/flights/edit"))
                .andExpect(model().attribute("flight", flight))
                .andExpect(model().attribute("managerId", 4));
    }

    @Test
    void showEditFlightForm_noManager_leavesManagerIdEmpty() throws Exception {
        when(flightService.getFlightById(9)).thenReturn(new Flight());

        mockMvc.perform(get("/admin/flights/edit/9"))
                .andExpect(view().name("admin/flights/edit"))
                .andExpect(model().attributeDoesNotExist("managerId"));
    }

    private MockHttpServletRequestBuilder editFlightRequest() {
        return post("/admin/flights/edit/9")
                .param("flightNumber", "AI101").param("airlineId", "1")
                .param("departureAirportId", "2").param("arrivalAirportId", "3")
                .param("departureDateTime", "2026-11-01T08:00").param("arrivalDateTime", "2026-11-01T10:15")
                .param("managerId", "4").param("status", "Scheduled");
    }

    @Test
    void updateFlight_withoutSession_redirectsToLogin() throws Exception {
        mockMvc.perform(editFlightRequest()).andExpect(view().name("redirect:/auth/login"));

        verifyNoInteractions(flightService);
    }

    @Test
    void updateFlight_success_redirectsToList() throws Exception {
        mockMvc.perform(editFlightRequest().session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("redirect:/admin/flights"))
                .andExpect(flash().attribute("success", "Flight updated successfully"));

        verify(flightService).updateFlight(9, "AI101", 1, 2, 3, Timestamp.valueOf("2026-11-01 08:00:00"),
                Timestamp.valueOf("2026-11-01 10:15:00"), 4, "Scheduled");
        verify(loggingService).logAction(OWNER_ID, "Updated flight AI101", "AdminController");
    }

    @Test
    void updateFlight_duplicateNumber_staysOnEditForm() throws Exception {
        doThrow(new DuplicateKeyException("dup")).when(flightService)
                .updateFlight(anyInt(), anyString(), any(), any(), any(), any(), any(), any(), anyString());

        mockMvc.perform(editFlightRequest().session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("admin/flights/edit"))
                .andExpect(model().attribute("error",
                        "Flight number 'AI101' already exists. Please use a different flight number."));
    }

    @Test
    void updateFlight_unexpectedError_staysOnEditForm() throws Exception {
        doThrow(new RuntimeException("boom")).when(flightService)
                .updateFlight(anyInt(), anyString(), any(), any(), any(), any(), any(), any(), anyString());

        mockMvc.perform(editFlightRequest().session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("admin/flights/edit"))
                .andExpect(model().attribute("error", "Failed to update flight: boom"));
    }

    @Test
    void deleteFlight_loggedIn_deletesAndLogs() throws Exception {
        mockMvc.perform(post("/admin/flights/delete/9").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("redirect:/admin/flights"));

        verify(flightService).deleteFlight(9);
        verify(loggingService).logAction(OWNER_ID, "Deleted flight", "AdminController");
    }

    @Test
    void deleteFlight_withoutSession_doesNotDelete() throws Exception {
        mockMvc.perform(post("/admin/flights/delete/9")).andExpect(view().name("redirect:/auth/login"));

        verifyNoInteractions(flightService);
    }

    // ---- airports ----

    @Test
    void listAirports_showsAllAirports() throws Exception {
        List<Airport> airports = List.of(new Airport());
        when(airportRepository.findAll()).thenReturn(airports);

        mockMvc.perform(get("/admin/airports"))
                .andExpect(view().name("admin/airports/list"))
                .andExpect(model().attribute("airports", airports));
    }

    @Test
    void showAddAirportForm_rendersEmptyForm() throws Exception {
        mockMvc.perform(get("/admin/airports/add"))
                .andExpect(view().name("admin/airports/add"))
                .andExpect(model().attributeExists("airport"));
    }

    @Test
    void addAirport_withoutSession_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/admin/airports/add")).andExpect(view().name("redirect:/auth/login"));

        verifyNoInteractions(airportRepository);
    }

    @Test
    void addAirport_success_savesAndLogs() throws Exception {
        mockMvc.perform(post("/admin/airports/add").session(sessionFor(OWNER_ID, "OWNER"))
                .param("name", "Delhi").param("code", "DEL").param("location", "Delhi"))
                .andExpect(view().name("redirect:/admin/airports"))
                .andExpect(flash().attribute("success", "Airport added successfully"));

        ArgumentCaptor<Airport> airport = ArgumentCaptor.forClass(Airport.class);
        verify(airportRepository).save(airport.capture());
        assertEquals("DEL", airport.getValue().getCode());
        verify(loggingService).logAction(OWNER_ID, "Added Airport ", "AdminController");
    }

    @Test
    void addAirport_duplicate_redisplaysForm() throws Exception {
        doThrow(new DuplicateKeyException("dup")).when(airportRepository).save(any(Airport.class));

        mockMvc.perform(post("/admin/airports/add").session(sessionFor(OWNER_ID, "OWNER")).param("code", "DEL"))
                .andExpect(view().name("admin/airports/add"))
                .andExpect(model().attribute("error", "Duplicate entries exist"));
    }

    @Test
    void addAirport_unexpectedError_redisplaysForm() throws Exception {
        doThrow(new RuntimeException("boom")).when(airportRepository).save(any(Airport.class));

        mockMvc.perform(post("/admin/airports/add").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("admin/airports/add"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void showEditAirportForm_loadsAirport() throws Exception {
        Airport airport = new Airport();
        when(airportRepository.findById(2)).thenReturn(airport);

        mockMvc.perform(get("/admin/airports/edit/2"))
                .andExpect(view().name("admin/airports/edit"))
                .andExpect(model().attribute("airport", airport));
    }

    @Test
    void updateAirport_success_updatesAndLogs() throws Exception {
        mockMvc.perform(post("/admin/airports/edit/2").session(sessionFor(OWNER_ID, "OWNER")).param("code", "DEL"))
                .andExpect(view().name("redirect:/admin/airports"))
                .andExpect(flash().attribute("success", "Airport updated successfully"));

        ArgumentCaptor<Airport> airport = ArgumentCaptor.forClass(Airport.class);
        verify(airportRepository).update(airport.capture());
        assertEquals(2, airport.getValue().getAirportId());
        verify(loggingService).logAction(OWNER_ID, "Edited Airport ", "AdminController");
    }

    @Test
    void updateAirport_withoutSession_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/admin/airports/edit/2")).andExpect(view().name("redirect:/auth/login"));

        verifyNoInteractions(airportRepository);
    }

    @Test
    void updateAirport_duplicate_redisplaysForm() throws Exception {
        doThrow(new DuplicateKeyException("dup")).when(airportRepository).update(any(Airport.class));

        mockMvc.perform(post("/admin/airports/edit/2").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("admin/airports/edit"))
                .andExpect(model().attribute("error", "Duplicate entries exist"));
    }

    @Test
    void updateAirport_unexpectedError_redisplaysForm() throws Exception {
        doThrow(new RuntimeException("boom")).when(airportRepository).update(any(Airport.class));

        mockMvc.perform(post("/admin/airports/edit/2").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("admin/airports/edit"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void deleteAirport_loggedIn_deletesAndLogs() throws Exception {
        mockMvc.perform(post("/admin/airports/delete/2").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("redirect:/admin/airports"))
                .andExpect(flash().attribute("success", "Airport deleted successfully"));

        verify(airportRepository).delete(2);
        verify(loggingService).logAction(OWNER_ID, "Deleted Airport ", "AdminController");
    }

    // ---- airlines ----

    @Test
    void listAirlines_showsAllAirlines() throws Exception {
        List<Airline> airlines = List.of(new Airline());
        when(airlineRepository.findAll()).thenReturn(airlines);

        mockMvc.perform(get("/admin/airlines"))
                .andExpect(view().name("admin/airlines/list"))
                .andExpect(model().attribute("airlines", airlines));
    }

    @Test
    void listPassengers_showsAllPassengers() throws Exception {
        List<Passenger> passengers = List.of(new Passenger());
        when(passengerService.getAllPassengers()).thenReturn(passengers);

        mockMvc.perform(get("/admin/passengers"))
                .andExpect(view().name("admin/passengers/list"))
                .andExpect(model().attribute("passengers", passengers));
    }

    @Test
    void showAddAirlineForm_rendersEmptyForm() throws Exception {
        mockMvc.perform(get("/admin/airlines/add"))
                .andExpect(view().name("admin/airlines/add"))
                .andExpect(model().attributeExists("airline"));
    }

    @Test
    void addAirline_success_savesAndRedirects() throws Exception {
        mockMvc.perform(post("/admin/airlines/add").session(sessionFor(OWNER_ID, "OWNER"))
                .param("name", "IndiGo").param("code", "6E"))
                .andExpect(view().name("redirect:/admin/airlines"))
                .andExpect(flash().attribute("success", "Airline added successfully"));

        ArgumentCaptor<Airline> airline = ArgumentCaptor.forClass(Airline.class);
        verify(airlineRepository).save(airline.capture());
        assertEquals("6E", airline.getValue().getCode());
    }

    @Test
    void addAirline_withoutSession_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/admin/airlines/add")).andExpect(view().name("redirect:/auth/login"));

        verifyNoInteractions(airlineRepository);
    }

    @Test
    void addAirline_duplicateCode_redisplaysForm() throws Exception {
        doThrow(new DuplicateKeyException("dup")).when(airlineRepository).save(any(Airline.class));

        mockMvc.perform(post("/admin/airlines/add").session(sessionFor(OWNER_ID, "OWNER")).param("code", "6E"))
                .andExpect(view().name("admin/airlines/add"))
                .andExpect(model().attribute("error",
                        "The airline code '6E' already exists. Please use a different code."));
    }

    @Test
    void addAirline_unexpectedError_redisplaysForm() throws Exception {
        doThrow(new RuntimeException("boom")).when(airlineRepository).save(any(Airline.class));

        mockMvc.perform(post("/admin/airlines/add").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("admin/airlines/add"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void showEditAirlineForm_loggedIn_loadsAirline() throws Exception {
        Airline airline = new Airline();
        when(airlineRepository.findById(2)).thenReturn(airline);

        mockMvc.perform(get("/admin/airlines/edit/2").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("admin/airlines/edit"))
                .andExpect(model().attribute("airline", airline));
    }

    @Test
    void showEditAirlineForm_withoutSession_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/airlines/edit/2")).andExpect(view().name("redirect:/auth/login"));
    }

    @Test
    void updateAirline_success_updatesAndLogs() throws Exception {
        mockMvc.perform(post("/admin/airlines/edit/2").session(sessionFor(OWNER_ID, "OWNER")).param("code", "6E"))
                .andExpect(view().name("redirect:/admin/airlines"))
                .andExpect(flash().attribute("success", "Airline updated successfully"));

        ArgumentCaptor<Airline> airline = ArgumentCaptor.forClass(Airline.class);
        verify(airlineRepository).update(airline.capture());
        assertEquals(2, airline.getValue().getAirlineId());
        verify(loggingService).logAction(OWNER_ID, "Updated Airline", "AdminController");
    }

    @Test
    void updateAirline_withoutSession_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/admin/airlines/edit/2")).andExpect(view().name("redirect:/auth/login"));

        verifyNoInteractions(airlineRepository);
    }

    @Test
    void updateAirline_duplicateCode_redisplaysForm() throws Exception {
        doThrow(new DuplicateKeyException("dup")).when(airlineRepository).update(any(Airline.class));

        mockMvc.perform(post("/admin/airlines/edit/2").session(sessionFor(OWNER_ID, "OWNER")).param("code", "6E"))
                .andExpect(view().name("admin/airlines/edit"))
                .andExpect(model().attribute("error",
                        "The airline code '6E' already exists. Please use a different code."));
    }

    @Test
    void updateAirline_unexpectedError_reportsError() throws Exception {
        doThrow(new RuntimeException("boom")).when(airlineRepository).update(any(Airline.class));

        mockMvc.perform(post("/admin/airlines/edit/2").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(model().attribute("error",
                        "An error occurred while updating the airline. Please try again."));
    }

    @Test
    void deleteAirline_loggedIn_deletesAndLogs() throws Exception {
        mockMvc.perform(post("/admin/airlines/delete/2").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("redirect:/admin/airlines"));

        verify(airlineRepository).delete(2);
        verify(loggingService).logAction(OWNER_ID, "Deleted Airline ", "AdminController");
    }
}
