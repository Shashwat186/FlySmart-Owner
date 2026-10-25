package smart.fly.airline.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;

import smart.fly.airline.model.Airline;
import smart.fly.airline.repository.AirlineRepository;
import smart.fly.repository.RepositoryTestBase;

class AirlineRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private AirlineRepository airlineRepository;

    @Test
    void findById_mapsAllColumns() {
        Airline airline = airlineRepository.findById(1);

        assertEquals(1, airline.getAirlineId());
        assertEquals("Air India", airline.getName());
        assertEquals("AI", airline.getCode());
        assertEquals("care@airindia.test", airline.getContactInfo());
    }

    @Test
    void findById_unknown_throwsEmptyResult() {
        assertThrows(EmptyResultDataAccessException.class, () -> airlineRepository.findById(999));
    }

    @Test
    void findByCode_returnsMatchingAirline() {
        assertEquals("IndiGo", airlineRepository.findByCode("6E").getName());
    }

    @Test
    void findAll_isOrderedByName() {
        List<Airline> airlines = airlineRepository.findAll();

        assertEquals(List.of("Air India", "IndiGo"), airlines.stream().map(Airline::getName).toList());
    }

    @Test
    void save_insertsNewAirline() {
        airlineRepository.save(new Airline(0, "Vistara", "UK", "care@vistara.test"));

        Airline saved = airlineRepository.findByCode("UK");
        assertEquals("Vistara", saved.getName());
        assertTrue(saved.getAirlineId() >= 1000);
        assertEquals(3, airlineRepository.countAirline());
    }

    // Controllers rely on DuplicateKeyException to show "code already exists"
    @Test
    void save_duplicateCode_throwsDuplicateKey() {
        assertThrows(DuplicateKeyException.class,
                () -> airlineRepository.save(new Airline(0, "Another", "AI", "x")));
    }

    @Test
    void update_changesFields() {
        airlineRepository.update(new Airline(2, "IndiGo Airlines", "6E", "new@indigo.test"));

        Airline updated = airlineRepository.findById(2);
        assertEquals("IndiGo Airlines", updated.getName());
        assertEquals("new@indigo.test", updated.getContactInfo());
    }

    @Test
    void update_toExistingCode_throwsDuplicateKey() {
        assertThrows(DuplicateKeyException.class,
                () -> airlineRepository.update(new Airline(2, "IndiGo", "AI", "x")));
    }

    @Test
    void delete_removesAirline() {
        airlineRepository.delete(2);

        assertEquals(1, airlineRepository.countAirline());
        assertThrows(EmptyResultDataAccessException.class, () -> airlineRepository.findById(2));
    }

    @Test
    void countAirline_countsAllRows() {
        assertEquals(2, airlineRepository.countAirline());
    }
}
