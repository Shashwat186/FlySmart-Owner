package smart.fly.flightmanager.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import smart.fly.businness.owner.model.BusinessOwner;
import smart.fly.flightmanager.model.FlightManager;
import smart.fly.flightmanager.repository.FlightManagerRepository;
import smart.fly.repository.RepositoryTestBase;
import smart.fly.user.model.User;

// changePassword is not covered: it uses MySQL's multi-table UPDATE ... JOIN syntax, which H2 does not support.
class FlightManagerRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private FlightManagerRepository flightManagerRepository;

    @Test
    void findById_mapsOwnerAndUser() {
        FlightManager manager = flightManagerRepository.findById(1);

        assertEquals(1, manager.getFmId());
        assertEquals(1, manager.getBusinessOwner().getBoId());
        assertEquals(2, manager.getUser().getUserId());
        // name/email are not selected here; the mapper tolerates their absence
        assertNull(manager.getUser().getName());
    }

    @Test
    void findById_unknown_throwsEmptyResult() {
        assertThrows(EmptyResultDataAccessException.class, () -> flightManagerRepository.findById(999));
    }

    @Test
    void findByUserId_returnsManagerProfile() {
        assertEquals(2, flightManagerRepository.findByUserId(4).getFmId());
    }

    @Test
    void findByBusinessOwner_includesUserNameAndEmail() {
        List<FlightManager> managers = flightManagerRepository.findByBusinessOwner(1);

        assertEquals(2, managers.size());
        FlightManager ravi = managers.stream().filter(m -> m.getFmId() == 1).findFirst().orElseThrow();
        assertEquals("Ravi Kumar", ravi.getUser().getName());
        assertEquals("ravi@flysmart.test", ravi.getUser().getEmail());
    }

    @Test
    void save_assignsGeneratedId() {
        jdbcTemplate.update("INSERT INTO user (user_id, name, email, role) VALUES (5, 'New Mgr', 'new@flysmart.test', 'MANAGER')");
        FlightManager manager = new FlightManager();
        manager.setBusinessOwner(new BusinessOwner(1));
        User user = new User();
        user.setUserId(5);
        manager.setUser(user);

        FlightManager saved = flightManagerRepository.save(manager);

        assertTrue(saved.getFmId() >= 1000);
        assertEquals(5, flightManagerRepository.findById(saved.getFmId()).getUser().getUserId());
    }

    @Test
    void update_changesOwner() {
        FlightManager manager = flightManagerRepository.findById(2);
        manager.setBusinessOwner(new BusinessOwner(7));

        flightManagerRepository.update(manager);

        assertEquals(7, flightManagerRepository.findById(2).getBusinessOwner().getBoId());
    }

    @Test
    void assignToBusinessOwner_changesOwner() {
        flightManagerRepository.assignToBusinessOwner(1, 9);

        assertEquals(9, flightManagerRepository.findById(1).getBusinessOwner().getBoId());
    }

    @Test
    void findAll_returnsManagersOrderedByName() {
        List<FlightManager> managers = flightManagerRepository.findAll();

        assertEquals(List.of("Meera Iyer", "Ravi Kumar"), managers.stream().map(m -> m.getUser().getName()).toList());
    }

    @Test
    void findAll_excludesUsersWhoAreNoLongerManagers() {
        jdbcTemplate.update("UPDATE user SET role = 'PASSENGER' WHERE user_id = 4");

        assertEquals(List.of(1), flightManagerRepository.findAll().stream().map(FlightManager::getFmId).toList());
    }

    @Test
    void getManagedFlightManagers_listsManagersOfOwner() {
        assertEquals(2, flightManagerRepository.getManagedFlightManagers(1).size());
        assertTrue(flightManagerRepository.getManagedFlightManagers(99).isEmpty());
    }
}
