package smart.fly.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static smart.fly.controller.MockMvcSupport.mockMvcFor;
import static smart.fly.controller.MockMvcSupport.sessionFor;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import smart.fly.action.log.ActionLog;
import smart.fly.airline.repository.AirlineRepository;
import smart.fly.airport.repository.AirportRepository;
import smart.fly.businness.owner.model.BusinessOwner;
import smart.fly.businness.owner.repository.BusinessOwnerRepository;
import smart.fly.dto.FeedbackDTO;
import smart.fly.feedback.repository.FeedbackRepository;
import smart.fly.flight.model.Flight;
import smart.fly.flight.repository.FlightRepository;
import smart.fly.flight.seats.repository.FlightSeatsRepository;
import smart.fly.flight.seats.repository.impl.FlightSeatsRepositoryImpl.FlightSeatAvailability;
import smart.fly.flightmanager.model.FlightManager;
import smart.fly.flightmanager.repository.FlightManagerRepository;
import smart.fly.payment.repository.PaymentRepository;
import smart.fly.service.FlightManagementService;
import smart.fly.service.LoggingService;
import smart.fly.service.PassengerService;
import smart.fly.service.UserService;
import smart.fly.user.model.User;
import smart.fly.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OwnerControllerTest {

    private static final int OWNER_ID = 1;

    @Mock
    private UserService userService;
    @Mock
    private FlightManagementService flightService;
    @Mock
    private BusinessOwnerRepository businessOwnerRepository;
    @Mock
    private FlightManagerRepository flightManagerRepository;
    @Mock
    private FlightRepository flightRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LoggingService loggingService;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private PassengerService passengerService;
    @Mock
    private AirportRepository airportRepository;
    @Mock
    private AirlineRepository airlineRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private FlightSeatsRepository flightSeatsRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvcFor(new OwnerController(userService, flightService, businessOwnerRepository,
                flightManagerRepository, flightRepository, userRepository, loggingService, feedbackRepository,
                passengerService, airportRepository, airlineRepository, paymentRepository, flightSeatsRepository));
    }

    // ---- session guard: every owner page bounces anonymous users to login ----

    @Test
    void ownerPages_withoutSession_redirectToLogin() throws Exception {
        for (String url : List.of("/owner/dashboard", "/owner/reports", "/owner/dashboard/flightmanager/list",
                "/owner/feedback")) {
            mockMvc.perform(get(url)).andExpect(view().name("redirect:/auth/login"));
        }
        mockMvc.perform(post("/owner/dashboard/flightmanager/add")).andExpect(view().name("redirect:/auth/login"));
        mockMvc.perform(post("/owner/feedback/check/3")).andExpect(view().name("redirect:/auth/login"));

        verifyNoInteractions(flightService, loggingService, feedbackRepository, businessOwnerRepository);
    }

    // ---- dashboard ----

    @Test
    void showOwnerDashboard_populatesCountsAndActionLog() throws Exception {
        List<ActionLog> log = List.of(new ActionLog());
        when(flightRepository.findAll()).thenReturn(List.of(new Flight(), new Flight()));
        when(passengerService.countRegisteredPassengers()).thenReturn(7);
        when(loggingService.getActionLogForOwner(OWNER_ID)).thenReturn(log);

        mockMvc.perform(get("/owner/dashboard").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("owner/dashboard"))
                .andExpect(model().attribute("totalFlights", 2))
                .andExpect(model().attribute("totalUsers", 7))
                .andExpect(model().attribute("actionLog", log));
    }

    @Test
    void listManagers_showsUsersWithManagerRole() throws Exception {
        List<User> managers = List.of(new User());
        when(userService.getUsersByRole("MANAGER")).thenReturn(managers);

        mockMvc.perform(get("/owner/managers"))
                .andExpect(view().name("owner/managers/list"))
                .andExpect(model().attribute("managers", managers));
    }

    // ---- add flight manager ----

    @Test
    void showAddManagerForm_rendersEmptyForm() throws Exception {
        mockMvc.perform(get("/owner/dashboard/flightmanager/add"))
                .andExpect(view().name("owner/dashboard/flightmanager/add"))
                .andExpect(model().attributeExists("managerForm"));
    }

    @Test
    void addManager_success_createsManagerUnderOwnerAndLogs() throws Exception {
        BusinessOwner owner = new BusinessOwner(8);
        when(businessOwnerRepository.findByUserId(OWNER_ID)).thenReturn(owner);

        mockMvc.perform(post("/owner/dashboard/flightmanager/add").session(sessionFor(OWNER_ID, "OWNER"))
                .param("name", "Ravi").param("email", "ravi@example.com").param("password", "pw"))
                .andExpect(view().name("redirect:/owner/dashboard/flightmanager/list"))
                .andExpect(flash().attributeExists("success"));

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(flightService).createFlightManager(user.capture(), eq("pw"), eq(owner));
        assertEquals("ravi@example.com", user.getValue().getEmail());
        assertEquals("MANAGER", user.getValue().getRole());
        verify(loggingService).logAction(OWNER_ID, "Flight Manager added", "OwnerController");
    }

    @Test
    void addManager_serviceFailure_redisplaysFormWithError() throws Exception {
        when(businessOwnerRepository.findByUserId(OWNER_ID)).thenReturn(new BusinessOwner(8));
        when(flightService.createFlightManager(any(User.class), eq("pw"), any(BusinessOwner.class)))
                .thenThrow(new RuntimeException("Email already exists"));

        mockMvc.perform(post("/owner/dashboard/flightmanager/add").session(sessionFor(OWNER_ID, "OWNER"))
                .param("email", "ravi@example.com").param("password", "pw"))
                .andExpect(view().name("owner/dashboard/flightmanager/add"))
                .andExpect(model().attribute("error", "Error: Email already exists"))
                .andExpect(model().attributeExists("managerForm"));

        verify(loggingService, never()).logAction(anyInt(), anyString(), anyString());
    }

    // ---- reports ----

    @Test
    void generateReports_populatesAllMetrics() throws Exception {
        List<FlightSeatAvailability> availability = List.of();
        when(airportRepository.countAirport()).thenReturn(5);
        when(airlineRepository.countAirline()).thenReturn(3);
        when(passengerService.countRegisteredPassengers()).thenReturn(40);
        when(passengerService.countTotalPassengers()).thenReturn(90);
        when(flightRepository.countTotalFlight()).thenReturn(12);
        when(paymentRepository.totalPayment()).thenReturn(250000);
        when(flightRepository.scheduledFlights()).thenReturn(9);
        when(flightRepository.delayedFlights()).thenReturn(2);
        when(flightSeatsRepository.findFlightSeatAvailability()).thenReturn(availability);

        mockMvc.perform(get("/owner/reports").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("owner/reports"))
                .andExpect(model().attribute("airport", 5))
                .andExpect(model().attribute("airline", 3))
                .andExpect(model().attribute("user", 40))
                .andExpect(model().attribute("passenger", 90))
                .andExpect(model().attribute("flights", 12))
                .andExpect(model().attribute("totalPrice", 250000))
                .andExpect(model().attribute("scheduledFlights", 9))
                .andExpect(model().attribute("delayedFlights", 2))
                .andExpect(model().attribute("flightsAvailability", availability));
    }

    // ---- flight manager list ----

    @Test
    void listFlightManagers_showsManagersOfCurrentOwner() throws Exception {
        List<FlightManager> managers = List.of(new FlightManager(4));
        when(businessOwnerRepository.findByUserId(OWNER_ID)).thenReturn(new BusinessOwner(8));
        when(flightManagerRepository.findByBusinessOwner(8)).thenReturn(managers);

        mockMvc.perform(get("/owner/dashboard/flightmanager/list").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("owner/dashboard/flightmanager/list"))
                .andExpect(model().attribute("flightManagers", managers));
    }

    // ---- feedback ----

    @Test
    void viewAllFeedback_listsFeedbackWithFlightDetails() throws Exception {
        List<FeedbackDTO> feedback = List.of(new FeedbackDTO());
        when(feedbackRepository.getAllFeedbackWithFlightDetails()).thenReturn(feedback);

        mockMvc.perform(get("/owner/feedback").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("owner/feedback"))
                .andExpect(model().attribute("feedbackList", feedback));
    }

    @Test
    void markFeedbackAsChecked_updatesStatusAndLogs() throws Exception {
        mockMvc.perform(post("/owner/feedback/check/3").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("redirect:/owner/feedback"))
                .andExpect(flash().attribute("success", "Feedback marked as checked successfully"));

        verify(feedbackRepository).updateFeedbackStatus(3, "checked");
        verify(loggingService).logAction(OWNER_ID, "Marked feedback ID: 3 as checked", "OwnerController");
    }

    @Test
    void markFeedbackAsChecked_unknownFeedback_flashesError() throws Exception {
        doThrow(new RuntimeException("Feedback not found with id: 3"))
                .when(feedbackRepository).updateFeedbackStatus(3, "checked");

        mockMvc.perform(post("/owner/feedback/check/3").session(sessionFor(OWNER_ID, "OWNER")))
                .andExpect(view().name("redirect:/owner/feedback"))
                .andExpect(flash().attribute("error", "Error updating feedback status: Feedback not found with id: 3"));

        verify(loggingService, never()).logAction(anyInt(), anyString(), anyString());
    }
}
