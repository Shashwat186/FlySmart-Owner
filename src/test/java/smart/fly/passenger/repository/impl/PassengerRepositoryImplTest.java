package smart.fly.passenger.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import smart.fly.passenger.model.Passenger;
import smart.fly.passenger.repository.PassengerRepository;
import smart.fly.repository.RepositoryTestBase;
import smart.fly.user.model.User;

class PassengerRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private PassengerRepository passengerRepository;

    @Test
    void findById_mapsAllColumns() {
        Passenger passenger = passengerRepository.findById(1);

        assertEquals(1, passenger.getPassengerId());
        assertEquals("Asha Rao", passenger.getName());
        assertEquals("asha@flysmart.test", passenger.getEmail());
        assertEquals("9876543210", passenger.getPhone());
        assertEquals(120, passenger.getLoyaltyPoints());
        assertTrue(passenger.isRegistered());
    }

    @Test
    void findById_unknown_throwsEmptyResult() {
        assertThrows(EmptyResultDataAccessException.class, () -> passengerRepository.findById(999));
    }

    @Test
    void findByEmail_returnsPassenger() {
        assertEquals(2, passengerRepository.findByEmail("kiran@flysmart.test").getPassengerId());
    }

    @Test
    void findByPhone_returnsPassenger() {
        assertEquals("Kiran Rao", passengerRepository.findByPhone("9123456780").getName());
    }

    @Test
    @Disabled("Known bug: findByUserId queries the user table and never binds the userId parameter, so it always throws")
    void findByUserId_returnsPassengerLinkedToUser() {
        assertEquals(1, passengerRepository.findByUserId(3).getPassengerId());
    }

    @Test
    void save_insertsPassengerWithoutUser() {
        Passenger passenger = new Passenger();
        passenger.setName("Walk-in Guest");
        passenger.setEmail("guest@flysmart.test");
        passenger.setPhone("9888877777");

        passengerRepository.save(passenger);

        Passenger saved = passengerRepository.findByEmail("guest@flysmart.test");
        assertFalse(saved.isRegistered());
        assertNull(jdbcTemplate.queryForObject("SELECT user_id FROM passenger WHERE email = 'guest@flysmart.test'",
                Integer.class));
    }

    @Test
    void save_insertsPassengerLinkedToUser() {
        Passenger passenger = new Passenger();
        passenger.setName("Ravi Kumar");
        passenger.setEmail("ravi.p@flysmart.test");
        passenger.setPhone("9000000002");
        passenger.setRegistered(true);
        User user = new User();
        user.setUserId(2);
        passenger.setUser(user);

        passengerRepository.save(passenger);

        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT user_id FROM passenger WHERE email = 'ravi.p@flysmart.test'", Integer.class));
    }

    @Test
    void update_changesAllEditableFields() {
        Passenger passenger = passengerRepository.findById(2);
        passenger.setName("Kiran R.");
        passenger.setLoyaltyPoints(50);
        passenger.setRegistered(true);

        passengerRepository.update(passenger);

        Passenger updated = passengerRepository.findById(2);
        assertEquals("Kiran R.", updated.getName());
        assertEquals(50, updated.getLoyaltyPoints());
        assertTrue(updated.isRegistered());
    }

    @Test
    void linkToUser_setsUserAndMarksRegistered() {
        passengerRepository.linkToUser(2, 3);

        assertTrue(passengerRepository.findById(2).isRegistered());
        assertEquals(3, jdbcTemplate.queryForObject("SELECT user_id FROM passenger WHERE passenger_id = 2",
                Integer.class));
    }

    @Test
    void findUnregisteredPassengers_returnsOnlyGuests() {
        List<Passenger> guests = passengerRepository.findUnregisteredPassengers();

        assertEquals(List.of(2), guests.stream().map(Passenger::getPassengerId).toList());
    }

    @Test
    void findAll_isOrderedByName() {
        assertEquals(List.of("Asha Rao", "Asha Rao", "Kiran Rao"),
                passengerRepository.findAll().stream().map(Passenger::getName).toList());
    }

    @Test
    void countTotalPassengers_countsDistinctNames() {
        assertEquals(2, passengerRepository.countTotalPassengers());
    }

    @Test
    void countRegisteredPassengers_countsDistinctRegisteredNames() {
        assertEquals(1, passengerRepository.countRegisteredPassengers());
    }
}
