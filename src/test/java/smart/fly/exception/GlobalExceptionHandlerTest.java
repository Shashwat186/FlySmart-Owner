package smart.fly.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAuthException_returnsUnauthorizedWithMessage() {
        ResponseEntity<String> response = handler.handleAuthException(new AuthenticationException("Bad credentials"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Bad credentials", response.getBody());
    }

    @Test
    void handleBusinessExceptions_returnsBadRequestForBookingException() {
        ResponseEntity<String> response = handler.handleBusinessExceptions(new BookingException("No seats left"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No seats left", response.getBody());
    }

    @Test
    void handleBusinessExceptions_returnsBadRequestForFlightManagementException() {
        ResponseEntity<String> response = handler
                .handleBusinessExceptions(new FlightManagementException("Flight not found"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Flight not found", response.getBody());
    }

    @Test
    void handleBusinessExceptions_returnsBadRequestForFeedbackException() {
        ResponseEntity<String> response = handler.handleBusinessExceptions(new FeedbackException("Already rated"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Already rated", response.getBody());
    }

    @Test
    void handleGenericException_hidesInternalDetails() {
        ResponseEntity<String> response = handler.handleGenericException(new IllegalStateException("db password=x"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody());
    }
}
