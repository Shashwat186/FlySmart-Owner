package smart.fly.feedback.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class FeedbackTest {

    @Test
    void constructor_setsFieldsAndDefaultsStatusToUnchecked() {
        LocalDateTime before = LocalDateTime.now();

        Feedback feedback = new Feedback(7, 42, 4, "Smooth flight");

        assertEquals(7, feedback.getUserId());
        assertEquals(42, feedback.getBookingId());
        assertEquals(4, feedback.getRating());
        assertEquals("Smooth flight", feedback.getFeedbackContent());
        assertEquals("unchecked", feedback.getStatus());
        assertNotNull(feedback.getFeedbackDateTime());
        assertFalse(feedback.getFeedbackDateTime().isBefore(before));
        assertFalse(feedback.getFeedbackDateTime().isAfter(LocalDateTime.now()));
    }

    @Test
    void noArgConstructor_leavesStatusAndDateUnset() {
        Feedback feedback = new Feedback();

        assertNull(feedback.getStatus());
        assertNull(feedback.getFeedbackDateTime());
    }
}
