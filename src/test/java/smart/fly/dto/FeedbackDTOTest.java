package smart.fly.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class FeedbackDTOTest {

    @Test
    void getFormattedFeedbackDateTime_formatsForDisplay() {
        FeedbackDTO dto = new FeedbackDTO();
        dto.setFeedbackDateTime(LocalDateTime.of(2026, 3, 7, 14, 5, 42));

        assertEquals("Mar 07, 2026 14:05", dto.getFormattedFeedbackDateTime());
    }

    // Regression: LocalDateTime.toString() drops ":00" seconds, which used to crash the owner feedback page
    @Test
    void getFormattedFeedbackDateTime_handlesZeroSeconds() {
        FeedbackDTO dto = new FeedbackDTO();
        dto.setFeedbackDateTime(LocalDateTime.of(2026, 10, 25, 9, 30, 0));

        assertEquals("Oct 25, 2026 09:30", dto.getFormattedFeedbackDateTime());
    }

    @Test
    void getFormattedFeedbackDateTime_handlesMidnight() {
        FeedbackDTO dto = new FeedbackDTO();
        dto.setFeedbackDateTime(LocalDateTime.of(2026, 1, 1, 0, 0));

        assertEquals("Jan 01, 2026 00:00", dto.getFormattedFeedbackDateTime());
    }

    @Test
    void getFormattedFeedbackDateTime_emptyWhenDateMissing() {
        assertEquals("", new FeedbackDTO().getFormattedFeedbackDateTime());
    }
}
