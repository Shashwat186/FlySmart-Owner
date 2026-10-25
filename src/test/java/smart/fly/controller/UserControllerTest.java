package smart.fly.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static smart.fly.controller.MockMvcSupport.mockMvcFor;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import smart.fly.service.UserService;
import smart.fly.user.model.User;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvcFor(new UserController(userService));
    }

    @Test
    void showProfile_existingUserWithPhoto_rendersProfileWithBase64Photo() throws Exception {
        User user = new User();
        user.setUserId(4);
        byte[] photo = { 10, 20, 30 };
        user.setProfilePhoto(photo);
        when(userService.getUserById(4)).thenReturn(user);

        mockMvc.perform(get("/passenger/profile/4"))
                .andExpect(view().name("passenger/profile"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("profilePhotoBase64", Base64.getEncoder().encodeToString(photo)));
    }

    @Test
    void showProfile_userWithoutPhoto_omitsPhotoAttribute() throws Exception {
        when(userService.getUserById(4)).thenReturn(new User());

        mockMvc.perform(get("/passenger/profile/4"))
                .andExpect(view().name("passenger/profile"))
                .andExpect(model().attributeDoesNotExist("profilePhotoBase64"));
    }

    @Test
    void showProfile_emptyPhoto_omitsPhotoAttribute() throws Exception {
        User user = new User();
        user.setProfilePhoto(new byte[0]);
        when(userService.getUserById(4)).thenReturn(user);

        mockMvc.perform(get("/passenger/profile/4"))
                .andExpect(model().attributeDoesNotExist("profilePhotoBase64"));
    }

    @Test
    void showProfile_unknownUser_rendersErrorPage() throws Exception {
        when(userService.getUserById(99)).thenReturn(null);

        mockMvc.perform(get("/passenger/profile/99"))
                .andExpect(view().name("error"))
                .andExpect(model().attribute("error", "User not found"));
    }
}
