package smart.fly.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import smart.fly.exception.AuthenticationException;
import smart.fly.passenger.model.Passenger;
import smart.fly.passenger.repository.PassengerRepository;
import smart.fly.user.model.User;
import smart.fly.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PassengerServiceImplTest {

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PassengerServiceImpl passengerService;

    private Passenger passenger;
    private User user;

    @BeforeEach
    void setUp() {
        passenger = new Passenger();
        passenger.setPassengerId(10);
        passenger.setName("Asha Rao");
        passenger.setEmail("asha@example.com");
        passenger.setPhone("9876543210");

        user = new User();
        user.setUserId(5);
        user.setName("Asha Rao");
        user.setEmail("asha@example.com");
        user.setPhone("9876543210");
    }

    @Test
    void save_validPassenger_persistsAndReturnsIt() {
        Passenger result = passengerService.save(passenger);

        assertSame(passenger, result);
        verify(passengerRepository).save(passenger);
    }

    @Test
    void save_invalidEmail_throwsWithoutPersisting() {
        passenger.setEmail("not-an-email");

        assertThrows(IllegalArgumentException.class, () -> passengerService.save(passenger));
        verifyNoInteractions(passengerRepository);
    }

    @Test
    void save_invalidPhone_throwsWithoutPersisting() {
        passenger.setPhone("123");

        assertThrows(IllegalArgumentException.class, () -> passengerService.save(passenger));
        verifyNoInteractions(passengerRepository);
    }

    @Test
    void registerPassenger_newEmail_savesUserAsPassengerAndLinksProfile() {
        User savedUser = new User();
        savedUser.setUserId(99);
        when(userRepository.emailExists("asha@example.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(savedUser);

        Passenger result = passengerService.registerPassenger(passenger, user, "pw");

        assertEquals("PASSENGER", user.getRole());
        assertSame(savedUser, result.getUser());
        assertTrue(result.isRegistered());
        verify(passengerRepository).save(passenger);
    }

    @Test
    void registerPassenger_emailAlreadyRegistered_throws() {
        when(userRepository.emailExists("asha@example.com")).thenReturn(true);

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> passengerService.registerPassenger(passenger, user, "pw"));

        assertEquals("Email already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passengerRepository);
    }

    @Test
    void registerPassenger_invalidUserEmail_throwsBeforeLookup() {
        user.setEmail("bad");

        assertThrows(IllegalArgumentException.class, () -> passengerService.registerPassenger(passenger, user, "pw"));
        verifyNoInteractions(userRepository, passengerRepository);
    }

    @Test
    void registerPassenger_invalidUserPhone_throwsBeforeLookup() {
        user.setPhone("abc");

        assertThrows(IllegalArgumentException.class, () -> passengerService.registerPassenger(passenger, user, "pw"));
        verifyNoInteractions(userRepository, passengerRepository);
    }

    @Test
    void findByUserId_found_returnsPassenger() {
        when(passengerRepository.findByUserId(5)).thenReturn(passenger);

        assertSame(passenger, passengerService.findByUserId(5));
    }

    @Test
    void findByUserId_missing_throws() {
        when(passengerRepository.findByUserId(5)).thenReturn(null);

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> passengerService.findByUserId(5));
        assertEquals("Passenger profile not found", ex.getMessage());
    }

    @Test
    void addUnregisteredPassenger_marksAsUnregisteredAndSaves() {
        passenger.setRegistered(true);

        Passenger result = passengerService.addUnregisteredPassenger(passenger);

        assertFalse(result.isRegistered());
        verify(passengerRepository).save(passenger);
    }

    @Test
    void addUnregisteredPassenger_invalidEmail_throws() {
        passenger.setEmail(null);

        assertThrows(IllegalArgumentException.class, () -> passengerService.addUnregisteredPassenger(passenger));
        verifyNoInteractions(passengerRepository);
    }

    @Test
    void linkToRegisteredUser_bothExist_links() {
        when(passengerRepository.findById(10)).thenReturn(passenger);
        when(userRepository.findById(5)).thenReturn(user);

        passengerService.linkToRegisteredUser(10, 5);

        verify(passengerRepository).linkToUser(10, 5);
    }

    @Test
    void linkToRegisteredUser_missingPassenger_throws() {
        when(passengerRepository.findById(10)).thenReturn(null);

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> passengerService.linkToRegisteredUser(10, 5));

        assertEquals("Passenger not found", ex.getMessage());
        verify(passengerRepository, never()).linkToUser(anyInt(), anyInt());
    }

    @Test
    void linkToRegisteredUser_missingUser_throws() {
        when(passengerRepository.findById(10)).thenReturn(passenger);
        when(userRepository.findById(5)).thenReturn(null);

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> passengerService.linkToRegisteredUser(10, 5));

        assertEquals("User not found", ex.getMessage());
        verify(passengerRepository, never()).linkToUser(anyInt(), anyInt());
    }

    @Test
    void updatePassengerProfile_keepsRegistrationAndUserFromExistingRecord() {
        Passenger existing = new Passenger();
        existing.setRegistered(true);
        existing.setUser(user);
        Passenger edited = new Passenger();
        edited.setPassengerId(10);
        edited.setName("Asha R.");
        edited.setRegistered(false);
        when(passengerRepository.findById(10)).thenReturn(existing);

        passengerService.updatePassengerProfile(edited);

        assertTrue(edited.isRegistered());
        assertSame(user, edited.getUser());
        verify(passengerRepository).update(edited);
    }

    @Test
    void updatePassengerProfile_missing_throws() {
        when(passengerRepository.findById(10)).thenReturn(null);

        assertThrows(AuthenticationException.class, () -> passengerService.updatePassengerProfile(passenger));
        verify(passengerRepository, never()).update(any());
    }

    @Test
    void findById_found_returnsPassenger() {
        when(passengerRepository.findById(10)).thenReturn(passenger);

        assertSame(passenger, passengerService.findById(10));
    }

    @Test
    void findById_missing_throws() {
        when(passengerRepository.findById(10)).thenReturn(null);

        AuthenticationException ex = assertThrows(AuthenticationException.class, () -> passengerService.findById(10));
        assertEquals("Passenger not found", ex.getMessage());
    }

    @Test
    void findByEmail_delegatesToRepository() {
        when(passengerRepository.findByEmail("asha@example.com")).thenReturn(passenger);

        assertSame(passenger, passengerService.findByEmail("asha@example.com"));
    }

    @Test
    void findByPhone_delegatesToRepository() {
        when(passengerRepository.findByPhone("9876543210")).thenReturn(passenger);

        assertSame(passenger, passengerService.findByPhone("9876543210"));
    }

    @Test
    void ensurePassengerProfile_existingProfile_returnsItWithoutSaving() {
        when(passengerRepository.findByUserId(5)).thenReturn(passenger);

        assertSame(passenger, passengerService.ensurePassengerProfile(user));
        verify(passengerRepository, never()).save(any());
    }

    @Test
    void ensurePassengerProfile_noProfile_createsOneFromUser() {
        when(passengerRepository.findByUserId(5)).thenReturn(null);

        Passenger result = passengerService.ensurePassengerProfile(user);

        assertEquals("Asha Rao", result.getName());
        assertEquals("asha@example.com", result.getEmail());
        assertEquals("9876543210", result.getPhone());
        assertSame(user, result.getUser());
        assertTrue(result.isRegistered());
        verify(passengerRepository).save(result);
    }

    @Test
    void findUnregisteredPassengers_delegatesToRepository() {
        List<Passenger> passengers = List.of(passenger);
        when(passengerRepository.findUnregisteredPassengers()).thenReturn(passengers);

        assertEquals(passengers, passengerService.findUnregisteredPassengers());
    }

    @Test
    void getAllPassengers_delegatesToRepository() {
        List<Passenger> passengers = List.of(passenger);
        when(passengerRepository.findAll()).thenReturn(passengers);

        assertEquals(passengers, passengerService.getAllPassengers());
    }

    @Test
    void countRegisteredPassengers_delegatesToRepository() {
        when(passengerRepository.countRegisteredPassengers()).thenReturn(12);

        assertEquals(12, passengerService.countRegisteredPassengers());
    }

    @Test
    void countTotalPassengers_delegatesToRepository() {
        when(passengerRepository.countTotalPassengers()).thenReturn(30);

        assertEquals(30, passengerService.countTotalPassengers());
    }
}
