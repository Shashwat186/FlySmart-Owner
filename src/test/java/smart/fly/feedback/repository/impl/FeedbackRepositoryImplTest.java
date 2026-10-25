package smart.fly.feedback.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import smart.fly.dto.FeedbackDTO;
import smart.fly.feedback.model.Feedback;
import smart.fly.feedback.repository.FeedbackRepository;
import smart.fly.repository.RepositoryTestBase;

class FeedbackRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    void addFeedback_insertsUncheckedFeedback() {
        assertTrue(feedbackRepository.addFeedback(new Feedback(3, 3, 4, "Refund was quick")));

        assertTrue(feedbackRepository.checkFeedbackExists(3, 3));
        Feedback saved = feedbackRepository.getFeedbacksByUser(3).stream()
                .filter(f -> f.getBookingId() == 3).findFirst().orElseThrow();
        assertEquals("unchecked", saved.getStatus());
        assertEquals(4, saved.getRating());
    }

    @Test
    void checkFeedbackExists_reflectsPresence() {
        assertTrue(feedbackRepository.checkFeedbackExists(3, 1));
        assertFalse(feedbackRepository.checkFeedbackExists(3, 3));
        assertFalse(feedbackRepository.checkFeedbackExists(2, 1));
    }

    @Test
    void getBookingsWithFeedback_listsBookingIds() {
        assertEquals(List.of(1, 2), feedbackRepository.getBookingsWithFeedback(3).stream().sorted().toList());
        assertTrue(feedbackRepository.getBookingsWithFeedback(1).isEmpty());
    }

    @Test
    void getAllFeedbackWithFlightDetails_isNewestFirstWithJoinedDetails() {
        List<FeedbackDTO> feedback = feedbackRepository.getAllFeedbackWithFlightDetails();

        assertEquals(List.of(1, 2), feedback.stream().map(FeedbackDTO::getFeedbackId).toList());
        FeedbackDTO latest = feedback.get(0);
        assertEquals(3, latest.getUserId());
        assertEquals("Asha Rao", latest.getUserName());
        assertEquals(LocalDateTime.of(2026, 10, 2, 9, 30), latest.getFeedbackDateTime());
        assertEquals("Oct 02, 2026 09:30", latest.getFormattedFeedbackDateTime());
        assertEquals(5, latest.getRating());
        assertEquals("Great crew", latest.getFeedbackContent());
        assertEquals("unchecked", latest.getStatus());
        assertEquals(1, latest.getBookingId());
        assertEquals(1, latest.getFlightId());
        assertEquals("AI101", latest.getFlightNumber());
        assertEquals("Air India", latest.getAirlineName());
        assertEquals("Indira Gandhi International", latest.getDepartureAirportName());
        assertEquals("DEL", latest.getDepartureAirportCode());
        assertEquals("Chhatrapati Shivaji Maharaj International", latest.getArrivalAirportName());
        assertEquals("BOM", latest.getArrivalAirportCode());
        assertEquals(LocalDateTime.of(2030, 1, 10, 8, 0), latest.getDepartureDateTime());
        assertEquals(LocalDateTime.of(2030, 1, 10, 10, 15), latest.getArrivalDateTime());
    }

    @Test
    void updateFeedbackStatus_marksChecked() {
        feedbackRepository.updateFeedbackStatus(1, "checked");

        assertEquals("checked", feedbackRepository.getAllFeedbackWithFlightDetails().get(0).getStatus());
    }

    @Test
    void updateFeedbackStatus_unknownId_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> feedbackRepository.updateFeedbackStatus(999, "checked"));
        assertEquals("Feedback not found with id: 999", ex.getMessage());
    }

    @Test
    void getFeedbacksByUser_mapsAllColumns() {
        Feedback feedback = feedbackRepository.getFeedbacksByUser(3).stream()
                .filter(f -> f.getFeedbackId() == 2).findFirst().orElseThrow();

        assertEquals(3, feedback.getUserId());
        assertEquals(2, feedback.getBookingId());
        assertEquals(LocalDateTime.of(2020, 5, 2, 10, 0), feedback.getFeedbackDateTime());
        assertEquals(3, feedback.getRating());
        assertEquals("Delayed boarding", feedback.getFeedbackContent());
        assertEquals("checked", feedback.getStatus());
    }
}
