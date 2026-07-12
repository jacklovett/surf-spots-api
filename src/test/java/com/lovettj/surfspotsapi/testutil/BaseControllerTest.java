package com.lovettj.surfspotsapi.testutil;

import com.lovettj.surfspotsapi.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Base class for controller tests that send authenticated session cookies.
 *
 * Mocks {@link UserRepository#existsById} to always return {@code true} so that
 * the {@code SessionCookieFilter}'s user-existence check passes for any test
 * user ID without requiring a live database row.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({MockMvcDefaults.class, BaseControllerTest.ControllerTestInfrastructure.class})
@TestPropertySource(
        properties = {
            "app.auth.session-secret=test-session-secret",
            "app.security.csrf-origin-filter-enabled=false",
        })
public abstract class BaseControllerTest {

    @MockBean
    protected UserRepository userRepository;

    @BeforeEach
    void allowAllTestUserIds() {
        Mockito.reset(userRepository);
        Mockito.when(userRepository.existsById(Mockito.anyString())).thenReturn(true);
    }

    /**
     * Ensures {@link MockMvcBuilderCustomizer} is registered for every controller test
     * context, including subclasses (nested {@code @TestConfiguration} on the base type).
     */
    @TestConfiguration
    static class ControllerTestInfrastructure {

        @Bean
        MockMvcBuilderCustomizer trustedOriginHeader() {
            return builder ->
                    builder.defaultRequest(MockMvcRequestBuilders.get("/")
                            .header("Origin", MockMvcDefaults.TRUSTED_TEST_ORIGIN));
        }
    }
}
