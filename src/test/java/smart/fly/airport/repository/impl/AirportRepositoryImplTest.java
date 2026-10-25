package smart.fly.airport.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;

import smart.fly.airport.model.Airport;
import smart.fly.airport.repository.AirportRepository;
import smart.fly.repository.RepositoryTestBase;

class AirportRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private AirportRepository airportRepository;

    @Test
    void findById_mapsAllColumns() {
        Airport airport = airportRepository.findById(1);

        assertEquals(1, airport.getAirportId());
        assertEquals("Indira Gandhi International", airport.getName());
        assertEquals("DEL", airport.getCode());
        assertEquals("del@airport.test", airport.getContactInfo());
        assertEquals("New Delhi", airport.getLocation());
    }

    @Test
    void findById_unknown_throwsEmptyResult() {
        assertThrows(EmptyResultDataAccessException.class, () -> airportRepository.findById(999));
    }

    @Test
    void findByCode_returnsMatchingAirport() {
        assertEquals("Mumbai", airportRepository.findByCode("BOM").getLocation());
    }

    @Test
    void findAll_isOrderedByName() {
        assertEquals(List.of("BOM", "DEL", "BLR"),
                airportRepository.findAll().stream().map(Airport::getCode).toList());
    }

    @Test
    void findByLocation_matchesPartialText() {
        List<Airport> airports = airportRepository.findByLocation("Delhi");

        assertEquals(1, airports.size());
        assertEquals("DEL", airports.get(0).getCode());
    }

    @Test
    void findByLocation_noMatch_returnsEmpty() {
        assertTrue(airportRepository.findByLocation("Chennai").isEmpty());
    }

    @Test
    void findAllForSearch_omitsContactInfo() {
        List<Airport> airports = airportRepository.findAllForSearch();

        assertEquals(3, airports.size());
        assertTrue(airports.stream().allMatch(a -> a.getContactInfo() == null));
        assertTrue(airports.stream().allMatch(a -> a.getLocation() != null && a.getAirportId() > 0));
    }

    @Test
    void save_insertsNewAirport() {
        airportRepository.save(new Airport(0, "Chennai International", "MAA", "maa@airport.test", "Chennai"));

        assertEquals("Chennai", airportRepository.findByCode("MAA").getLocation());
        assertEquals(4, airportRepository.countAirport());
    }

    @Test
    void save_duplicateCode_throwsDuplicateKey() {
        assertThrows(DuplicateKeyException.class,
                () -> airportRepository.save(new Airport(0, "Dup", "DEL", "x", "y")));
    }

    @Test
    void update_changesFields() {
        airportRepository.update(new Airport(3, "Bengaluru International", "BLR", "new@blr.test", "Bangalore"));

        Airport updated = airportRepository.findById(3);
        assertEquals("Bengaluru International", updated.getName());
        assertEquals("Bangalore", updated.getLocation());
    }

    @Test
    void delete_removesAirport() {
        airportRepository.delete(3);

        assertEquals(2, airportRepository.countAirport());
    }
}
