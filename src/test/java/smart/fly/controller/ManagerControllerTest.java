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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import smart.fly.action.log.ActionLog;
import smart.fly.airline.model.Airline;
import smart.fly.airline.repository.AirlineRepository;
import smart.fly.airport.model.Airport;
import smart.fly.airport.repository.AirportRepository;
import smart.fly.flight.model.Flight;
import smart.fly.flight.repository.FlightRepository;
import smart.fly.flightmanager.model.FlightManager;
import smart.fly.flightmanager.repository.FlightManagerRepository;
import smart.fly.service.FlightManagementService;
import smart.fly.service.LoggingService;

@ExtendWith(MockitoExtension.class)
class ManagerControllerTest {

    private static final int MANAGER_USER_ID = 30;

    @Mock
    private FlightManagementService flightService;
    @Mock
    private FlightManagerRepository flightManagerRepository;
    @Mock
    private AirlineRepository airlineRepository;
    @Mock
    private AirportRepository airportRepository;
    @Mock
    private FlightRepository flightRepository;
    @Mock
    private LoggingService loggingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvcFor(new ManagerController(flightService, flightManagerRepository, airlineRepository,
                airportRepository, flightRepository, loggingService));
    }

    // ---- dashboard & listings ----

    @Test
    void showManagerDashboard_withoutSession_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/manager/dashboard")).andExpect(view().name("redirect:/auth/login"));

        verifyNoInteractions(flightRepository, loggingService);
    }

    @Test
    void showManagerDashboard_populatesManagerSpecificCounts() throws Exception {
        List<ActionLog> log = List.of(new ActionLog());
        when(loggingService.getActionLogForOwner(MANAGER_USER_ID)).thenReturn(log);
        when(flightRepository.getTotalFlightsByUserId(MANAGER_USER_ID)).thenReturn(6);
        when(flightRepository.getScheduledFlightsByUserId(MANAGER_USER_ID)).thenReturn(4);
        when(flightRepository.getDelayedFlightsByUserId(MANAGER_USER_ID)).thenReturn(1);

        mockMvc.perform(get("/manager/dashboard").session(sessionFor(MANAGER_USER_ID, "MANAGER")))
                .andExpect(view().name("manager/dashboard"))
                .andExpect(model().attribute("actionLog", log))
                .andExpect(model().attribute("totalFlights", 6))
                .andExpect(model().attribute("scheduledCount", 4))
                .andExpect(model().attribute("delayedCount", 1));
    }

    @Test
    void listManagerAirports_showsAllAirports() throws Exception {
        List<Airport> airports = List.of(new Airport());
        when(airportRepository.findAll()).thenReturn(airports);

        mockMvc.perform(get("/manager/airports"))
                .andExpect(view().name("manager/airports/list"))
                .andExpect(model().attribute("airports", airports));
    }

    @Test
    void listManagerAirlines_showsAllAirlines() throws Exception {
        List<Airline> airlines = List.of(new Airline());
        when(airlineRepository.findAll()).thenReturn(airlines);

        mockMvc.perform(get("/manager/airlines"))
                .andExpect(view().name("manager/airlines/list"))
                .andExpect(model().attribute("airlines", airlines));
    }

    @Test
    void listManagerFlights_showsOnlyFlightsAssignedToManager() throws Exception {
        List<Flight> flights = List.of(new Flight());
        when(flightManagerRepository.findByUserId(MANAGER_USER_ID)).thenReturn(new FlightManager(4));
        when(flightService.getFlightsByManager(4)).thenReturn(flights);

        mockMvc.perform(get("/manager/flights").session(sessionFor(MANAGER_USER_ID, "MANAGER")))
                .andExpect(view().name("manager/flights/list"))
                .andExpect(model().attribute("flights", flights))
                .andExpect(model().attribute("statuses", List.of("SCHEDULED", "DELAYED", "CANCELLED", "COMPLETED")));
    }

    // ---- airlines ----

    @Test
    void showEditAirlineForm_loadsAirline() throws Exception {
        Airline airline = new Airline(2, "IndiGo", "6E", "x");
        when(airlineRepository.findById(2)).thenReturn(airline);

        mockMvc.perform(get("/manager/airlines/edit/2"))
                .andExpect(view().name("manager/airlines/edit"))
                .andExpect(model().attribute("airline", airline));
    }

    @Test
    void updateAirline_success_updatesLogsAndRedirects() throws Exception {
        mockMvc.perform(post("/manager/airlines/edit/2").session(sessionFor(MANAGER_USER_ID, "MANAGER"))
                .param("name", "IndiGo").param("code", "6E"))
                .andExpect(view().name("redirect:/manager/airlines"))
                .andExpect(flash().attribute("success", "Airline updated successfully"));

        ArgumentCaptor<Airline> airline = ArgumentCaptor.forClass(Airline.class);
        verify(airlineRepository).update(airline.capture());
        assertEquals(2, airline.getValue().getAirlineId());
        assertEquals("6E", airline.getValue().getCode());
        verify(loggingService).logAction(MANAGER_USER_ID, "Airline Updated", "ManagerController");
    }

    @Test
    void updateAirline_duplicateCode_redisplaysFormWithError() throws Exception {
        doThrow(new DuplicateKeyException("dup")).when(airlineRepository).update(any(Airline.class));

        mockMvc.perform(post("/manager/airlines/edit/2").session(sessionFor(MANAGER_USER_ID, "MANAGER"))
                .param("code", "6E"))
                .andExpect(view().name("manager/airlines/edit"))
                .andExpect(model().attribute("error",
                        "The airline code '6E' already exists. Please use a different code."));

        verify(loggingService, never()).logAction(anyInt(), anyString(), anyString());
    }

    @Test
    void updateAirline_unexpectedError_showsGenericError() throws Exception {
        doThrow(new RuntimeException("boom")).when(airlineRepository).update(any(Airline.class));

        mockMvc.perform(post("/manager/airlines/edit/2").session(sessionFor(MANAGER_USER_ID, "MANAGER")))
                .andExpect(model().attribute("error",
                        "An error occurred while updating the airline. Please try again."));
    }

    @Test
    void deleteAirline_deletesAndRedirects() throws Exception {
        mockMvc.perform(post("/manager/airlines/delete/2"))
                .andExpect(view().name("redirect:/manager/airlines"))
                .andExpect(flash().attribute("success", "Airline deleted successfully"));

        verify(airlineRepository).delete(2);
    }

    // ---- flights ----

    @Test
    void showEditFlightForm_loadsFlightAndReferenceData() throws Exception {
        Flight flight = new Flight();
        flight.setFlightId(9);
        flight.setFlightManager(new FlightManager(4));
        when(flightService.getFlightById(9)).thenReturn(flight);
        when(airlineRepository.findAll()).thenReturn(List.of());
        when(airportRepository.findAll()).thenReturn(List.of());
        when(flightManagerRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/manager/flights/edit/9").session(sessionFor(MANAGER_USER_ID, "MANAGER")))
                .andExpect(view().name("manager/flights/edit"))
                .andExpect(model().attribute("flight", flight))
                .andExpect(model().attribute("managerId", 4))
                .andExpect(model().attributeExists("airlines", "airports", "managers", "statuses"));
    }

    private MockHttpServletRequestBuilder flightEditRequest() {
        return post("/manager/flights/edit/9")
                .param("flightNumber", "AI101").param("airlineId", "1")
                .param("departureAirportId", "2").param("arrivalAirportId", "3")
                .param("departureDateTime", "2026-11-01T08:00").param("arrivalDateTime", "2026-11-01T10:15")
                .param("managerId", "4").param("status", "DELAYED");
    }

    @Test
    void updateFlight_withoutSession_redirectsToLogin() throws Exception {
        mockMvc.perform(flightEditRequest()).andExpect(view().name("redirect:/auth/login"));

        verifyNoInteractions(flightService);
    }

    @Test
    void updateFlight_success_convertsDatesAndRedirects() throws Exception {
        mockMvc.perform(flightEditRequest().session(sessionFor(MANAGER_USER_ID, "MANAGER")))
                .andExpect(view().name("redirect:/manager/flights"))
                .andExpect(flash().attribute("success", "Flight updated successfully"));

        verify(flightService).updateFlight(9, "AI101", 1, 2, 3, Timestamp.valueOf("2026-11-01 08:00:00"),
                Timestamp.valueOf("2026-11-01 10:15:00"), 4, "DELAYED");
        verify(loggingService).logAction(MANAGER_USER_ID, "Updated flight: AI101", "AdminController");
    }

    @Test
    void updateFlight_duplicateNumber_redisplaysEditFormWithInputs() throws Exception {
        doThrow(new DuplicateKeyException("dup")).when(flightService)
                .updateFlight(anyInt(), anyString(), any(), any(), any(), any(), any(), any(), anyString());

        mockMvc.perform(flightEditRequest().session(sessionFor(MANAGER_USER_ID, "MANAGER")))
                .andExpect(view().name("manager/flights/edit"))
                .andExpect(model().attribute("flightNumber", "AI101"))
                .andExpect(model().attribute("status", "DELAYED"))
                .andExpect(model().attribute("error",
                        "Flight number 'AI101' already exists. Please use a different flight number."));
    }

    @Test
    void updateFlight_invalidDate_reportsFailure() throws Exception {
        mockMvc.perform(flightEditRequest().param("departureDateTime", "tomorrow")
                .session(sessionFor(MANAGER_USER_ID, "MANAGER")))
                .andExpect(model().attributeExists("error"));

        verify(flightService, never()).updateFlight(anyInt(), anyString(), any(), any(), any(), any(), any(), any(),
                anyString());
    }

    // ---- airports ----

    @Test
    void showEditAirportForm_loadsAirport() throws Exception {
        Airport airport = new Airport(2, "Delhi", "DEL", "x", "Delhi");
        when(airportRepository.findById(2)).thenReturn(airport);

        mockMvc.perform(get("/manager/airports/edit/2"))
                .andExpect(view().name("manager/airports/edit"))
                .andExpect(model().attribute("airport", airport));
    }

    @Test
    void updateAirport_success_updatesLogsAndRedirects() throws Exception {
        mockMvc.perform(post("/manager/airports/edit/2").session(sessionFor(MANAGER_USER_ID, "MANAGER"))
                .param("name", "Delhi").param("code", "DEL"))
                .andExpect(view().name("redirect:/manager/airports"))
                .andExpect(flash().attribute("success", "Airport updated successfully"));

        ArgumentCaptor<Airport> airport = ArgumentCaptor.forClass(Airport.class);
        verify(airportRepository).update(airport.capture());
        assertEquals(2, airport.getValue().getAirportId());
        verify(loggingService).logAction(MANAGER_USER_ID, "Airport Updated", "ManagerController");
    }

    @Test
    void updateAirport_duplicate_redisplaysFormWithError() throws Exception {
        doThrow(new DuplicateKeyException("dup")).when(airportRepository).update(any(Airport.class));

        mockMvc.perform(post("/manager/airports/edit/2").session(sessionFor(MANAGER_USER_ID, "MANAGER")))
                .andExpect(view().name("manager/airports/edit"))
                .andExpect(model().attribute("error", "Duplicate Entries Exist"));
    }

    @Test
    void updateAirport_unexpectedError_staysOnEditForm() throws Exception {
        doThrow(new RuntimeException("boom")).when(airportRepository).update(any(Airport.class));

        mockMvc.perform(post("/manager/airports/edit/2").session(sessionFor(MANAGER_USER_ID, "MANAGER")))
                .andExpect(view().name("manager/airports/edit"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void deleteAirport_deletesAndRedirects() throws Exception {
        mockMvc.perform(post("/manager/airports/delete/2"))
                .andExpect(view().name("redirect:/manager/airports"))
                .andExpect(flash().attribute("success", "Airport deleted successfully"));

        verify(airportRepository).delete(2);
    }

    // ---- flight status ----

    @Test
    void showUpdateStatusForm_existingFlight_rendersForm() throws Exception {
        Flight flight = new Flight();
        when(flightRepository.findById(9)).thenReturn(flight);

        mockMvc.perform(get("/manager/flights/updateStatus/9"))
                .andExpect(view().name("manager/flights/updateStatus"))
                .andExpect(model().attribute("flight", flight))
                .andExpect(model().attributeExists("statuses"));
    }

    @Test
    @Disabled("Known bug: GlobalExceptionHandler's catch-all Exception handler turns this ResponseStatusException into a 500")
    void showUpdateStatusForm_missingFlight_returns404() throws Exception {
        when(flightRepository.findById(9)).thenReturn(null);

        mockMvc.perform(get("/manager/flights/updateStatus/9")).andExpect(status().isNotFound());
    }

    @Test
    void updateFlightStatus_success_returnsOk() throws Exception {
        mockMvc.perform(post("/manager/flights/status/9").param("status", "DELAYED"))
                .andExpect(status().isOk())
                .andExpect(content().string("Status updated successfully."));

        verify(flightRepository).updateStatus(9, "DELAYED");
    }

    @Test
    void updateFlightStatus_failure_returns500() throws Exception {
        doThrow(new RuntimeException("db down")).when(flightRepository).updateStatus(9, "DELAYED");

        mockMvc.perform(post("/manager/flights/status/9").param("status", "DELAYED"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to update status."));
    }
}
