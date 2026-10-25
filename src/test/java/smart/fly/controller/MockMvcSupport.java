package smart.fly.controller;

import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import smart.fly.exception.GlobalExceptionHandler;
import smart.fly.user.model.User;

/**
 * Builds a standalone MockMvc for a single controller, mirroring the app's JSP view setup.
 * Standalone setup is used because FlySmartApplication's explicit @ComponentScan defeats @WebMvcTest slicing.
 */
final class MockMvcSupport {

    private MockMvcSupport() {
    }

    static MockMvc mockMvcFor(Object controller) {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver("/WEB-INF/views/", ".jsp");
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }

    static MockHttpSession sessionFor(int userId, String role) {
        User user = new User();
        user.setUserId(userId);
        user.setRole(role);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }
}
