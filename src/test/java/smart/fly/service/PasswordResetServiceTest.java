package smart.fly.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import smart.fly.util.PasswordUtil;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String EMAIL = "asha@example.com";
    private static final String TOKEN = "3f2b1c9e-token";
    private static final String VALIDATE_SQL = "SELECT email, expiry_date FROM password_reset_tokens WHERE token = ?";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UserService userService;

    @Mock
    private PasswordUtil passwordUtil;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void createResetToken_replacesOldTokenWithFreshOneExpiringInAnHour() {
        long before = System.currentTimeMillis();

        passwordResetService.createResetToken(EMAIL);

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).update("DELETE FROM password_reset_tokens WHERE email = ?", EMAIL);
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Timestamp> expiry = ArgumentCaptor.forClass(Timestamp.class);
        inOrder.verify(jdbcTemplate).update(startsWith("INSERT INTO password_reset_tokens"), eq(EMAIL),
                token.capture(), expiry.capture());

        assertDoesNotThrow(() -> UUID.fromString(token.getValue()));
        long expiresIn = expiry.getValue().getTime() - before;
        assertTrue(expiresIn >= 3_600_000 && expiresIn < 3_660_000, "expiry should be ~1 hour ahead");
    }

    @Test
    void createResetToken_generatesUniqueTokens() {
        passwordResetService.createResetToken(EMAIL);
        passwordResetService.createResetToken(EMAIL);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).update(startsWith("INSERT"), eq(EMAIL), token.capture(), any());
        assertNotEquals(token.getAllValues().get(0), token.getAllValues().get(1));
    }

    @Test
    void updatePassword_userFound_storesFreshHashAndSalt() {
        when(passwordUtil.generateSalt()).thenReturn("salt");
        when(passwordUtil.hashPassword("newPw", "salt")).thenReturn("hash");
        when(jdbcTemplate.update("UPDATE user SET password_hash = ?, password_salt = ? WHERE email = ?", "hash",
                "salt", EMAIL)).thenReturn(1);

        assertTrue(passwordResetService.updatePassword(EMAIL, "newPw"));
    }

    @Test
    void updatePassword_noMatchingUser_returnsFalse() {
        when(passwordUtil.generateSalt()).thenReturn("salt");
        when(passwordUtil.hashPassword("newPw", "salt")).thenReturn("hash");
        when(jdbcTemplate.update(anyString(), eq("hash"), eq("salt"), eq(EMAIL))).thenReturn(0);

        assertFalse(passwordResetService.updatePassword(EMAIL, "newPw"));
    }

    @Test
    void updatePassword_databaseError_returnsFalse() {
        when(passwordUtil.generateSalt()).thenReturn("salt");
        when(passwordUtil.hashPassword("newPw", "salt")).thenReturn("hash");
        when(jdbcTemplate.update(anyString(), eq("hash"), eq("salt"), eq(EMAIL)))
                .thenThrow(new DataAccessResourceFailureException("down"));

        assertFalse(passwordResetService.updatePassword(EMAIL, "newPw"));
    }

    @Test
    void validateToken_futureTimestamp_isValid() {
        when(jdbcTemplate.queryForMap(VALIDATE_SQL, TOKEN)).thenReturn(
                Map.of("email", EMAIL, "expiry_date", Timestamp.valueOf(LocalDateTime.now().plusMinutes(30))));

        assertTrue(passwordResetService.validateToken(TOKEN));
    }

    @Test
    void validateToken_pastTimestamp_isExpired() {
        when(jdbcTemplate.queryForMap(VALIDATE_SQL, TOKEN)).thenReturn(
                Map.of("email", EMAIL, "expiry_date", Timestamp.valueOf(LocalDateTime.now().minusMinutes(1))));

        assertFalse(passwordResetService.validateToken(TOKEN));
    }

    @Test
    void validateToken_futureLocalDateTime_isValid() {
        when(jdbcTemplate.queryForMap(VALIDATE_SQL, TOKEN))
                .thenReturn(Map.of("email", EMAIL, "expiry_date", LocalDateTime.now().plusHours(1)));

        assertTrue(passwordResetService.validateToken(TOKEN));
    }

    @Test
    void validateToken_pastLocalDateTime_isExpired() {
        when(jdbcTemplate.queryForMap(VALIDATE_SQL, TOKEN))
                .thenReturn(Map.of("email", EMAIL, "expiry_date", LocalDateTime.now().minusHours(1)));

        assertFalse(passwordResetService.validateToken(TOKEN));
    }

    @Test
    void validateToken_unexpectedExpiryType_isInvalid() {
        when(jdbcTemplate.queryForMap(VALIDATE_SQL, TOKEN))
                .thenReturn(Map.of("email", EMAIL, "expiry_date", "2099-01-01"));

        assertFalse(passwordResetService.validateToken(TOKEN));
    }

    @Test
    void validateToken_unknownToken_isInvalid() {
        when(jdbcTemplate.queryForMap(VALIDATE_SQL, TOKEN)).thenThrow(new EmptyResultDataAccessException(1));

        assertFalse(passwordResetService.validateToken(TOKEN));
    }

    @Test
    void getEmailFromToken_knownToken_returnsEmail() {
        when(jdbcTemplate.queryForObject("SELECT email FROM password_reset_tokens WHERE token = ?", String.class,
                TOKEN)).thenReturn(EMAIL);

        assertEquals(EMAIL, passwordResetService.getEmailFromToken(TOKEN));
    }

    @Test
    void getEmailFromToken_unknownToken_returnsNull() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(TOKEN)))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertNull(passwordResetService.getEmailFromToken(TOKEN));
    }

    @Test
    void deleteToken_removesToken() {
        passwordResetService.deleteToken(TOKEN);

        verify(jdbcTemplate).update("DELETE FROM password_reset_tokens WHERE token = ?", TOKEN);
    }
}
