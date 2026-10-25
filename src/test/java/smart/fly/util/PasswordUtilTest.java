package smart.fly.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {

    private final PasswordUtil passwordUtil = new PasswordUtil();

    @Test
    void generateSalt_returnsBase64EncodedSixteenBytes() {
        String salt = passwordUtil.generateSalt();

        assertEquals(16, Base64.getDecoder().decode(salt).length);
    }

    @Test
    void generateSalt_returnsDifferentValueEachCall() {
        assertNotEquals(passwordUtil.generateSalt(), passwordUtil.generateSalt());
    }

    @Test
    void hashPassword_isDeterministicForSamePasswordAndSalt() {
        String salt = passwordUtil.generateSalt();

        assertEquals(passwordUtil.hashPassword("secret", salt), passwordUtil.hashPassword("secret", salt));
    }

    @Test
    void hashPassword_producesSha256SizedDigest() {
        String hash = passwordUtil.hashPassword("secret", passwordUtil.generateSalt());

        assertEquals(32, Base64.getDecoder().decode(hash).length);
    }

    @Test
    void hashPassword_differsForDifferentSalts() {
        assertNotEquals(passwordUtil.hashPassword("secret", passwordUtil.generateSalt()),
                passwordUtil.hashPassword("secret", passwordUtil.generateSalt()));
    }

    @Test
    void hashPassword_differsForDifferentPasswords() {
        String salt = passwordUtil.generateSalt();

        assertNotEquals(passwordUtil.hashPassword("secret", salt), passwordUtil.hashPassword("Secret", salt));
    }

    @Test
    void hashPassword_doesNotReturnPlainText() {
        String salt = passwordUtil.generateSalt();

        assertNotEquals("secret", passwordUtil.hashPassword("secret", salt));
    }

    @Test
    void hashPassword_rejectsSaltThatIsNotBase64() {
        assertThrows(IllegalArgumentException.class, () -> passwordUtil.hashPassword("secret", "not base64!"));
    }

    @Test
    void verifyPassword_trueForMatchingPassword() {
        String salt = passwordUtil.generateSalt();
        String hash = passwordUtil.hashPassword("owner123", salt);

        assertTrue(passwordUtil.verifyPassword("owner123", hash, salt));
    }

    @Test
    void verifyPassword_falseForWrongPassword() {
        String salt = passwordUtil.generateSalt();
        String hash = passwordUtil.hashPassword("owner123", salt);

        assertFalse(passwordUtil.verifyPassword("owner124", hash, salt));
    }

    @Test
    void verifyPassword_falseForWrongSalt() {
        String hash = passwordUtil.hashPassword("owner123", passwordUtil.generateSalt());

        assertFalse(passwordUtil.verifyPassword("owner123", hash, passwordUtil.generateSalt()));
    }
}
