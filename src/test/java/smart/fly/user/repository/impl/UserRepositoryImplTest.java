package smart.fly.user.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;

import smart.fly.repository.RepositoryTestBase;
import smart.fly.user.model.User;
import smart.fly.user.repository.UserRepository;

class UserRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private UserRepository userRepository;

    private User newUser(String email) {
        User user = new User();
        user.setName("Dev Patel");
        user.setEmail(email);
        user.setPhone("9555512345");
        user.setRole("PASSENGER");
        user.setGender("Male");
        user.setDob(Date.valueOf("2000-02-29"));
        user.setPasswordHash("hash");
        user.setPasswordSalt("salt");
        return user;
    }

    @Test
    void findByEmail_mapsAllColumns() {
        User user = userRepository.findByEmail("asha@flysmart.test");

        assertEquals(3, user.getUserId());
        assertEquals("Asha Rao", user.getName());
        assertEquals("9876543210", user.getPhone());
        assertEquals("PASSENGER", user.getRole());
        assertEquals("Female", user.getGender());
        assertEquals(Date.valueOf("1995-03-08"), user.getDob());
        assertEquals("h3", user.getPasswordHash());
        assertEquals("s3", user.getPasswordSalt());
    }

    @Test
    void findByEmail_unknown_returnsNull() {
        assertNull(userRepository.findByEmail("nobody@flysmart.test"));
    }

    @Test
    void findById_returnsUser() {
        assertEquals("ravi@flysmart.test", userRepository.findById(2).getEmail());
    }

    @Test
    void findById_unknown_throwsEmptyResult() {
        assertThrows(EmptyResultDataAccessException.class, () -> userRepository.findById(999));
    }

    @Test
    void save_assignsGeneratedIdAndPersistsPhoto() {
        User user = newUser("dev@flysmart.test");
        user.setProfilePhoto(new byte[] { 7, 8, 9 });

        User saved = userRepository.save(user);

        assertTrue(saved.getUserId() >= 1000);
        User reloaded = userRepository.findById(saved.getUserId());
        assertEquals("dev@flysmart.test", reloaded.getEmail());
        assertEquals(Date.valueOf("2000-02-29"), reloaded.getDob());
        assertArrayEquals(new byte[] { 7, 8, 9 }, reloaded.getProfilePhoto());
    }

    @Test
    void save_withoutPhotoOrDob_storesNulls() {
        User user = newUser("nophoto@flysmart.test");
        user.setDob(null);

        User reloaded = userRepository.findById(userRepository.save(user).getUserId());

        assertNull(reloaded.getProfilePhoto());
        assertNull(reloaded.getDob());
    }

    // AuthController maps this to "An account with this email ... already exists"
    @Test
    void save_duplicateEmail_throwsDataIntegrityViolation() {
        assertThrows(DuplicateKeyException.class, () -> userRepository.save(newUser("asha@flysmart.test")));
    }

    @Test
    void update_changesProfileFields() {
        User user = userRepository.findById(3);
        user.setName("Asha R.");
        user.setPhone("9000099999");
        user.setGender("Other");
        user.setDob(Date.valueOf("1995-03-09"));

        userRepository.update(user);

        User updated = userRepository.findById(3);
        assertEquals("Asha R.", updated.getName());
        assertEquals("9000099999", updated.getPhone());
        assertEquals("Other", updated.getGender());
        assertEquals(Date.valueOf("1995-03-09"), updated.getDob());
        assertEquals("asha@flysmart.test", updated.getEmail());
    }

    @Test
    void delete_removesUser() {
        userRepository.delete(4);

        assertNull(userRepository.findByEmail("meera@flysmart.test"));
    }

    @Test
    void changePassword_updatesHashAndSalt() {
        userRepository.changePassword(3, "newHash", "newSalt");

        User user = userRepository.findById(3);
        assertEquals("newHash", user.getPasswordHash());
        assertEquals("newSalt", user.getPasswordSalt());
    }

    @Test
    void emailExists_reflectsPresence() {
        assertTrue(userRepository.emailExists("ravi@flysmart.test"));
        assertFalse(userRepository.emailExists("ghost@flysmart.test"));
    }

    @Test
    void findAllByRole_returnsOnlyThatRole() {
        List<User> managers = userRepository.findAllByRole("MANAGER");

        assertEquals(List.of(2, 4), managers.stream().map(User::getUserId).sorted().toList());
    }

    @Test
    void countByRole_countsUsersWithRole() {
        assertEquals(2, userRepository.countByRole("MANAGER"));
        assertEquals(1, userRepository.countByRole("OWNER"));
        assertEquals(0, userRepository.countByRole("ADMIN"));
    }

    @Test
    void updateProfilePhoto_replacesPhoto() {
        userRepository.updateProfilePhoto(3, new byte[] { 1, 1, 2, 3 });

        assertArrayEquals(new byte[] { 1, 1, 2, 3 }, userRepository.findById(3).getProfilePhoto());
    }
}
