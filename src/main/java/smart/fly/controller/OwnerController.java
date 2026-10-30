package smart.fly.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import smart.fly.action.log.ActionLog;
import smart.fly.airline.repository.AirlineRepository;
import smart.fly.airport.repository.AirportRepository;
import smart.fly.businness.owner.model.BusinessOwner;
import smart.fly.businness.owner.repository.BusinessOwnerRepository;
import smart.fly.dto.FeedbackDTO;
import smart.fly.feedback.repository.FeedbackRepository;
import smart.fly.flight.repository.FlightRepository;
import smart.fly.flight.seats.repository.FlightSeatsRepository;
import smart.fly.flight.seats.repository.impl.FlightSeatsRepositoryImpl.FlightSeatAvailability;
import smart.fly.flightmanager.model.FlightManager;
import smart.fly.flightmanager.repository.FlightManagerRepository;
import smart.fly.payment.repository.PaymentRepository;
import smart.fly.service.*;
import smart.fly.user.model.User;
import smart.fly.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/owner")
public class OwnerController {
	private static final Logger logger = LoggerFactory.getLogger(OwnerController.class);


	private final UserService userService;
	private final FlightManagementService flightService;

	private final BusinessOwnerRepository businessOwnerRepository;
	private final FlightManagerRepository flightManagerRepository;
	private final FlightRepository flightRepository;

	private final LoggingService loggingService;
	private final FeedbackRepository feedbackRepository;
	private final PassengerService passengerService;
	private final AirportRepository airportRepository;
	private final AirlineRepository airlineRepository;
	private final PaymentRepository paymentRepository;
	private final FlightSeatsRepository flightSeatsRepository;

	public OwnerController(UserService userService, FlightManagementService flightService,
			BusinessOwnerRepository businessOwnerRepository, FlightManagerRepository flightManagerRepository,
			FlightRepository flightRepository, UserRepository userRepository, LoggingService loggingService,
			FeedbackRepository feedbackRepository, PassengerService passengerService,
			AirportRepository airportRepository, AirlineRepository airlineRepository,
			PaymentRepository paymentRepository, FlightSeatsRepository flightSeatsRepository) {
		super();
		this.userService = userService;
		this.flightService = flightService;

		this.businessOwnerRepository = businessOwnerRepository;
		this.flightManagerRepository = flightManagerRepository;
		this.flightRepository = flightRepository;

		this.loggingService = loggingService;
		this.feedbackRepository = feedbackRepository;
		this.passengerService = passengerService;
		this.airportRepository = airportRepository;
		this.airlineRepository = airlineRepository;
		this.paymentRepository = paymentRepository;
		this.flightSeatsRepository = flightSeatsRepository;
	}

	@GetMapping("/dashboard")
	public String showOwnerDashboard(HttpSession session, Model model) {
		User user = (User) session.getAttribute("user");
		if (user == null) {
			logger.debug("No user in session, redirecting to login");

			return "redirect:/auth/login";
		}
		int userId = user.getUserId();
		logger.debug("Dashboard - User ID from session: {}", userId);

		int totalFlights = flightRepository.findAll().size();
		model.addAttribute("totalFlights", totalFlights);

		int totalUsers = passengerService.countRegisteredPassengers();
		model.addAttribute("totalUsers", totalUsers);

		List<ActionLog> actionLog = loggingService.getActionLogForOwner(userId);
		logger.debug("Action log from database: {}", actionLog);
		model.addAttribute("actionLog", actionLog);

		return "owner/dashboard";
	}

	@GetMapping("/managers")
	public String listManagers(Model model) {
		List<User> managers = userService.getUsersByRole("MANAGER");
		model.addAttribute("managers", managers);
		return "owner/managers/list";
	}

	@GetMapping("/dashboard/flightmanager/add")
	public String showAddManagerForm(Model model) {
		model.addAttribute("managerForm", new User());
		return "owner/dashboard/flightmanager/add";
	}

	@PostMapping("/dashboard/flightmanager/add")
	public String addManager(@ModelAttribute("managerForm") User user, HttpSession session,
			RedirectAttributes redirectAttributes,Model model) {
		User currentUser = (User) session.getAttribute("user");
		if (currentUser == null) {
			logger.debug("No user in session, redirecting to login");
			// Handle this error!
			return "redirect:/auth/login"; // Or appropriate error handling
		}

		int ownerUserId = currentUser.getUserId(); // Correct User ID
		logger.debug("addManager - Logging action for user ID: {}", ownerUserId);

		try {

			BusinessOwner owner = businessOwnerRepository.findByUserId(currentUser.getUserId());
			user.setRole("MANAGER");
			flightService.createFlightManager(user, user.getPassword(), owner);

			loggingService.logAction(ownerUserId, "Flight Manager added", "OwnerController");
			redirectAttributes.addFlashAttribute("success", "Flight manager added successfully (Owner Controller)");

			return "redirect:/owner/dashboard/flightmanager/list";

		} catch (Exception e) {
			model.addAttribute("managerForm",user);
			model.addAttribute("error", "Error: " + e.getMessage());
			return "owner/dashboard/flightmanager/add";
		}
	}

	

	@GetMapping("/reports")
	public String generateReports(HttpSession session, Model model) {
		User currentUser = (User) session.getAttribute("user");
		if (currentUser == null) {
			logger.debug("No user in session, redirecting to login");
			// Handle this error!
			return "redirect:/auth/login"; // Or appropriate error handling
		}

		int ownerUserId = currentUser.getUserId();
		int airport = airportRepository.countAirport();
		model.addAttribute("airport", airport);
		int airline = airlineRepository.countAirline();
		model.addAttribute("airline", airline);
		int user = passengerService.countRegisteredPassengers();
		model.addAttribute("user", user);
		int passenger = passengerService.countTotalPassengers();
		model.addAttribute("passenger", passenger);
		int flights = flightRepository.countTotalFlight();
		model.addAttribute("flights", flights);
		int totalPrice = paymentRepository.totalPayment();
		model.addAttribute("totalPrice", totalPrice);
		int scheduledFlights = flightRepository.scheduledFlights();
		model.addAttribute("scheduledFlights", scheduledFlights);
		int delayedFlights = flightRepository.delayedFlights();
		model.addAttribute("delayedFlights", delayedFlights);
		List<FlightSeatAvailability> flightsAvailability = flightSeatsRepository.findFlightSeatAvailability();
		model.addAttribute("flightsAvailability", flightsAvailability);
		return "owner/reports";
	}

	@GetMapping("/dashboard/flightmanager/list")
	public String listFlightManagers(HttpSession session, Model model) {
		User user = (User) session.getAttribute("user");
		if (user == null) {
			logger.debug("No user in session, redirecting to login");
			// Handle this error!
			return "redirect:/auth/login"; // Or appropriate error handling
		}

		// Get the business owner for the current user
		BusinessOwner owner = businessOwnerRepository.findByUserId(user.getUserId());
		// Get all flight managers assigned to this business owner
		List<FlightManager> flightManagers = flightManagerRepository.findByBusinessOwner(owner.getBoId());
		model.addAttribute("flightManagers", flightManagers);
		return "owner/dashboard/flightmanager/list";
	}

	@GetMapping("/feedback")
	public String viewAllFeedback(Model model, HttpSession session) {
		User user = (User) session.getAttribute("user");
		if (user == null) {
			logger.debug("No user in session, redirecting to login");
			return "redirect:/auth/login";
		}

		// Get all feedback entries with their corresponding flight details
		List<FeedbackDTO> allFeedback = feedbackRepository.getAllFeedbackWithFlightDetails();
		model.addAttribute("feedbackList", allFeedback);

		return "owner/feedback";
	}

	@PostMapping("/feedback/check/{id}")
	public String markFeedbackAsChecked(@PathVariable int id, HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = (User) session.getAttribute("user");
		if (currentUser == null) {
			logger.debug("No user in session, redirecting to login");
			return "redirect:/auth/login";
		}

		int ownerUserId = currentUser.getUserId();

		try {
			// Update the feedback status to "checked"
			feedbackRepository.updateFeedbackStatus(id, "checked");

			loggingService.logAction(ownerUserId, "Marked feedback ID: " + id + " as checked", "OwnerController");

			redirectAttributes.addFlashAttribute("success", "Feedback marked as checked successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error updating feedback status: " + e.getMessage());
		}

		return "redirect:/owner/feedback";
	}

}
