package smart.fly.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidationUtilTest {

    @ParameterizedTest
    @ValueSource(strings = { "user@example.com", "first.last@mail.example.org", "a-b_c@domain.io", "x@y.info" })
    void validateEmail_acceptsWellFormedAddresses(String email) {
        assertDoesNotThrow(() -> ValidationUtil.validateEmail(email));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "plainaddress", "@example.com", "user@", "user@example", "user@example.c",
            "user@example.toolong", "user name@example.com" })
    void validateEmail_rejectsMalformedAddresses(String email) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtil.validateEmail(email));
        assertEquals("Invalid email format", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "9876543210", "+919876543210", "123456789012345" })
    void validatePhone_acceptsTenToFifteenDigitsWithOptionalPlus(String phone) {
        assertDoesNotThrow(() -> ValidationUtil.validatePhone(phone));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "123456789", "1234567890123456", "98765-43210", "98765 43210", "++919876543210",
            "phone12345" })
    void validatePhone_rejectsInvalidNumbers(String phone) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtil.validatePhone(phone));
        assertEquals("Invalid phone number", ex.getMessage());
    }

    @Test
    void validatePhone_rejectsPlusInMiddle() {
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validatePhone("98765+43210"));
    }
}
