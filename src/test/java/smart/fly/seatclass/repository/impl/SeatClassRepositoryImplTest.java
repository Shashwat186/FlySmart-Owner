package smart.fly.seatclass.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import smart.fly.repository.RepositoryTestBase;
import smart.fly.seatclass.model.SeatClass;
import smart.fly.seatclass.repository.SeatClassRepository;

class SeatClassRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private SeatClassRepository seatClassRepository;

    @Test
    void findById_mapsAllColumns() {
        SeatClass seatClass = seatClassRepository.findById(2);

        assertEquals(2, seatClass.getSeatClassId());
        assertEquals("Business", seatClass.getName());
        assertEquals(12000.0, seatClass.getPrice());
    }

    @Test
    void findAll_returnsEveryClass() {
        assertEquals(3, seatClassRepository.findAll().size());
    }

    @Test
    void findByName_returnsClass() {
        assertEquals(3, seatClassRepository.findByName("First").getSeatClassId());
    }

    @Test
    void save_insertsClass() {
        SeatClass premium = new SeatClass();
        premium.setName("Premium Economy");
        premium.setPrice(8000);

        seatClassRepository.save(premium);

        assertEquals(8000.0, seatClassRepository.findByName("Premium Economy").getPrice());
    }

    @Test
    void updatePrice_changesPrice() {
        seatClassRepository.updatePrice(1, 5500);

        assertEquals(5500.0, seatClassRepository.findById(1).getPrice());
    }

    @Test
    void findByFlightIdWithAvailableSeats_excludesSoldOutClasses() {
        List<SeatClass> classes = seatClassRepository.findByFlightIdWithAvailableSeats(1);

        assertEquals(List.of("Economy"), classes.stream().map(SeatClass::getName).toList());
    }

    @Test
    void getAvailableSeatsForFlightSeatClass_returnsCount() {
        assertEquals(100, seatClassRepository.getAvailableSeatsForFlightSeatClass(1, 1));
        assertEquals(0, seatClassRepository.getAvailableSeatsForFlightSeatClass(1, 2));
    }

    @Test
    void getAvailableSeatsForFlightSeatClass_noInventory_returnsNull() {
        assertNull(seatClassRepository.getAvailableSeatsForFlightSeatClass(1, 3));
    }
}
