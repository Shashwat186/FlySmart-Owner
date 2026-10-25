package smart.fly.businness.owner.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import smart.fly.businness.owner.model.BusinessOwner;
import smart.fly.businness.owner.repository.BusinessOwnerRepository;
import smart.fly.repository.RepositoryTestBase;
import smart.fly.user.model.User;

class BusinessOwnerRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private BusinessOwnerRepository businessOwnerRepository;

    @Test
    void findById_mapsOwnerAndUserId() {
        BusinessOwner owner = businessOwnerRepository.findById(1);

        assertEquals(1, owner.getBoId());
        assertEquals(1, owner.getUser().getUserId());
    }

    @Test
    void findByUserId_returnsOwnerProfile() {
        assertEquals(1, businessOwnerRepository.findByUserId(1).getBoId());
    }

    @Test
    void findByUserId_nonOwner_throwsEmptyResult() {
        assertThrows(EmptyResultDataAccessException.class, () -> businessOwnerRepository.findByUserId(3));
    }

    @Test
    void findAll_returnsAllOwners() {
        assertEquals(1, businessOwnerRepository.findAll().size());
    }

    @Test
    void save_insertsOwnerForUser() {
        BusinessOwner owner = new BusinessOwner();
        User user = new User();
        user.setUserId(4);
        owner.setUser(user);

        businessOwnerRepository.save(owner);

        assertEquals(2, businessOwnerRepository.findAll().size());
        assertEquals(4, businessOwnerRepository.findByUserId(4).getUser().getUserId());
    }

    @Test
    void addFlightManager_reassignsManagerToOwner() {
        jdbcTemplate.update("INSERT INTO business_owner (bo_id, user_id) VALUES (2, 4)");

        businessOwnerRepository.addFlightManager(2, 1);

        assertEquals(List.of(1), businessOwnerRepository.getManagedFlightManagerIds(2));
        assertEquals(List.of(2), businessOwnerRepository.getManagedFlightManagerIds(1));
    }

    @Test
    void getManagedFlightManagerIds_listsManagersOfOwner() {
        assertEquals(List.of(1, 2), businessOwnerRepository.getManagedFlightManagerIds(1).stream().sorted().toList());
    }

    @Test
    void getFlightCount_countsFlightsAcrossOwnersManagers() {
        assertEquals(4, businessOwnerRepository.getFlightCount(1));
        assertEquals(0, businessOwnerRepository.getFlightCount(99));
    }
}
