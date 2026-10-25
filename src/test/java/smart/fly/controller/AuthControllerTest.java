package smart.fly.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static smart.fly.controller.MockMvcSupport.mockMvcFor;
import static smart.fly.controller.MockMvcSupport.sessionFor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import smart.fly.exception.AuthenticationException;
import smart.fly.service.AuthService;
import smart.fly.service.PassengerService;
import smart.fly.service.PasswordResetService;
import smart.fly.service.UserService;
import smart.fly.user.model.User;
import smart.fly.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private PassengerService passengerService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetService passwordResetService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvcFor(new AuthController(authService, passengerService, userRepository, passwordResetService,
                jdbcTemplate, userService));
    }

    private User userWithRole(String role) {
        User user = new User();
        user.setUserId(21);
        user.setEmail("asha@example.com");
        user.setRole(role);
        return user;
    }

    // ---- login ----

    @Test
    void showLoginForm_rendersLoginWithEmptyUser() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void processLogin_passenger_redirectsToPassengerDashboardAndStoresSession() throws Exception {
        User user = userWithRole("PASSENGER");
        when(authService.authenticate("asha@example.com", "pw")).thenReturn(user);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/auth/login").session(session)
                .param("email", "asha@example.com").param("password", "pw").param("role", "passenger"))
                .andExpect(view().name("redirect:/passenger/dashboard"));

        assertSame(user, session.getAttribute("user"));
        assertEquals(21, session.getAttribute("loggedInUserId"));
    }

    @Test
    void processLogin_manager_redirectsToManagerDashboard() throws Exception {
        when(authService.authenticate("asha@example.com", "pw")).thenReturn(userWithRole("Manager"));

        mockMvc.perform(post("/auth/login")
                .param("email", "asha@example.com").param("password", "pw").param("role", "MANAGER"))
                .andExpect(view().name("redirect:/manager/dashboard"));
    }

    @Test
    void processLogin_owner_redirectsToOwnerDashboard() throws Exception {
        when(authService.authenticate("asha@example.com", "pw")).thenReturn(userWithRole("OWNER"));

        mockMvc.perform(post("/auth/login")
                .param("email", "asha@example.com").param("password", "pw").param("role", "OWNER"))
                .andExpect(view().name("redirect:/owner/dashboard"));
    }

    @Test
    void processLogin_unknownRole_redirectsBackToLogin() throws Exception {
        when(authService.authenticate("asha@example.com", "pw")).thenReturn(userWithRole("AUDITOR"));

        mockMvc.perform(post("/auth/login")
                .param("email", "asha@example.com").param("password", "pw").param("role", "AUDITOR"))
                .andExpect(view().name("redirect:/auth/login"));
    }

    @Test
    void processLogin_roleMismatch_showsErrorAndDoesNotLogIn() throws Exception {
        when(authService.authenticate("asha@example.com", "pw")).thenReturn(userWithRole("PASSENGER"));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/auth/login").session(session)
                .param("email", "asha@example.com").param("password", "pw").param("role", "OWNER"))
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("error", "Invalid role for this user"))
                .andExpect(model().attribute("email", "asha@example.com"))
                .andExpect(model().attribute("role", "OWNER"));

        assertNull(session.getAttribute("user"));
    }

    @Test
    void processLogin_badCredentials_showsErrorWithFormRepopulated() throws Exception {
        when(authService.authenticate("asha@example.com", "wrong"))
                .thenThrow(new AuthenticationException("Invalid email or password"));

        mockMvc.perform(post("/auth/login")
                .param("email", "asha@example.com").param("password", "wrong").param("role", "PASSENGER"))
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("error", "Invalid email or password"))
                .andExpect(model().attribute("email", "asha@example.com"));
    }

    // ---- registration ----

    @Test
    void showRegistrationForm_rendersRegister() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void processRegistration_success_registersAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/auth/register")
                .param("name", "Asha").param("email", "asha@example.com").param("phone", "9876543210")
                .param("role", "PASSENGER").param("password", "pw"))
                .andExpect(view().name("redirect:/auth/login"));

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(authService).registerUser(user.capture(), eq("pw"));
        assertEquals("asha@example.com", user.getValue().getEmail());
        assertNull(user.getValue().getProfilePhoto());
    }

    @Test
    void processRegistration_withProfileImage_storesPhotoBytes() throws Exception {
        byte[] photo = { 1, 2, 3 };

        mockMvc.perform(multipart("/auth/register")
                .file(new MockMultipartFile("profileImage", "me.png", "image/png", photo))
                .param("email", "asha@example.com").param("password", "pw"))
                .andExpect(view().name("redirect:/auth/login"));

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(authService).registerUser(user.capture(), eq("pw"));
        assertArrayEquals(photo, user.getValue().getProfilePhoto());
    }

    @Test
    void processRegistration_duplicateAccount_showsFriendlyError() throws Exception {
        when(authService.registerUser(any(User.class), eq("pw")))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        mockMvc.perform(post("/auth/register").param("email", "asha@example.com").param("password", "pw"))
                .andExpect(view().name("auth/register"))
                .andExpect(model().attribute("error", "An account with this email or phone number already exists"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void processRegistration_authenticationFailure_showsGenericError() throws Exception {
        when(authService.registerUser(any(User.class), eq("pw"))).thenThrow(new AuthenticationException("nope"));

        mockMvc.perform(post("/auth/register").param("email", "asha@example.com").param("password", "pw"))
                .andExpect(view().name("auth/register"))
                .andExpect(model().attribute("error", "An error occured"));
    }

    // ---- forgot / reset password ----

    @Test
    void showForgotPasswordForm_rendersPage() throws Exception {
        mockMvc.perform(get("/auth/forgot-password")).andExpect(view().name("auth/forgot-password"));
    }

    @Test
    void processForgotPassword_createsTokenAndShowsResetLink() throws Exception {
        when(jdbcTemplate.queryForObject("SELECT token FROM password_reset_tokens WHERE email = ?", String.class,
                "asha@example.com")).thenReturn("tok-1");

        mockMvc.perform(post("/auth/forgot-password").param("email", "asha@example.com"))
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attribute("resetLink", "/auth/reset-password?token=tok-1"))
                .andExpect(model().attributeExists("message"));

        verify(passwordResetService).createResetToken("asha@example.com");
    }

    @Test
    void showResetPasswordForm_validToken_rendersForm() throws Exception {
        when(passwordResetService.validateToken("tok-1")).thenReturn(true);

        mockMvc.perform(get("/auth/reset-password").param("token", "tok-1"))
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("token", "tok-1"));
    }

    @Test
    void showResetPasswordForm_invalidToken_redirectsToForgotPassword() throws Exception {
        when(passwordResetService.validateToken("bad")).thenReturn(false);

        mockMvc.perform(get("/auth/reset-password").param("token", "bad"))
                .andExpect(view().name("redirect:/auth/forgot-password"));
    }

    @Test
    void processResetPassword_passwordsDiffer_showsErrorWithoutTouchingToken() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                .param("token", "tok-1").param("newPassword", "a").param("confirmPassword", "b"))
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("error", "Passwords do not match"))
                .andExpect(model().attribute("token", "tok-1"));

        verifyNoInteractions(passwordResetService);
    }

    @Test
    void processResetPassword_expiredToken_redirectsToForgotPassword() throws Exception {
        when(passwordResetService.validateToken("tok-1")).thenReturn(false);

        mockMvc.perform(post("/auth/reset-password")
                .param("token", "tok-1").param("newPassword", "a").param("confirmPassword", "a"))
                .andExpect(view().name("redirect:/auth/forgot-password"));

        verify(passwordResetService, never()).updatePassword(anyString(), anyString());
    }

    @Test
    void processResetPassword_success_updatesPasswordAndConsumesToken() throws Exception {
        when(passwordResetService.validateToken("tok-1")).thenReturn(true);
        when(passwordResetService.getEmailFromToken("tok-1")).thenReturn("asha@example.com");
        when(passwordResetService.updatePassword("asha@example.com", "a")).thenReturn(true);

        mockMvc.perform(post("/auth/reset-password")
                .param("token", "tok-1").param("newPassword", "a").param("confirmPassword", "a"))
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("success"));

        verify(passwordResetService).deleteToken("tok-1");
    }

    @Test
    void processResetPassword_updateFails_keepsTokenAndShowsError() throws Exception {
        when(passwordResetService.validateToken("tok-1")).thenReturn(true);
        when(passwordResetService.getEmailFromToken("tok-1")).thenReturn("asha@example.com");
        when(passwordResetService.updatePassword("asha@example.com", "a")).thenReturn(false);

        mockMvc.perform(post("/auth/reset-password")
                .param("token", "tok-1").param("newPassword", "a").param("confirmPassword", "a"))
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("error", "Failed to reset password"));

        verify(passwordResetService, never()).deleteToken(anyString());
    }

    // ---- logout / change password ----

    @Test
    void logout_invalidatesSession() throws Exception {
        MockHttpSession session = sessionFor(21, "PASSENGER");

        mockMvc.perform(get("/auth/logout").session(session)).andExpect(view().name("redirect:/auth/login"));

        assertTrue(session.isInvalid());
    }

    @Test
    void showChangePasswordForm_rendersPage() throws Exception {
        mockMvc.perform(get("/auth/change-password")).andExpect(view().name("auth/change-password"));
    }

    @Test
    void processChangePassword_notLoggedIn_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                .param("currentPassword", "old").param("newPassword", "n").param("confirmPassword", "n"))
                .andExpect(view().name("redirect:/auth/login"));

        verifyNoInteractions(authService);
    }

    @Test
    void processChangePassword_confirmationMismatch_showsError() throws Exception {
        mockMvc.perform(post("/auth/change-password").session(sessionFor(21, "PASSENGER"))
                .param("currentPassword", "old").param("newPassword", "n1").param("confirmPassword", "n2"))
                .andExpect(view().name("auth/change-password"))
                .andExpect(request().attribute("error", "New passwords do not match"));

        verifyNoInteractions(authService);
    }

    @Test
    void processChangePassword_success_showsConfirmation() throws Exception {
        MockHttpSession session = sessionFor(21, "PASSENGER");

        mockMvc.perform(post("/auth/change-password").session(session)
                .param("currentPassword", "old").param("newPassword", "n").param("confirmPassword", "n"))
                .andExpect(view().name("auth/change-password"))
                .andExpect(request().attribute("success", "Password changed successfully"));

        verify(authService).changePassword((User) session.getAttribute("user"), "old", "n");
    }

    @Test
    void processChangePassword_wrongCurrentPassword_showsServiceError() throws Exception {
        doThrow(new AuthenticationException("Current password is incorrect"))
                .when(authService).changePassword(any(User.class), eq("old"), eq("n"));

        mockMvc.perform(post("/auth/change-password").session(sessionFor(21, "PASSENGER"))
                .param("currentPassword", "old").param("newPassword", "n").param("confirmPassword", "n"))
                .andExpect(view().name("auth/change-password"))
                .andExpect(request().attribute("error", "Current password is incorrect"));
    }
}
