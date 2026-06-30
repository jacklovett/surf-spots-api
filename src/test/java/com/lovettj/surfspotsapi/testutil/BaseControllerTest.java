package com.lovettj.surfspotsapi.testutil;

import com.lovettj.surfspotsapi.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

/**
 * Base class for controller tests that send authenticated session cookies.
 *
 * Mocks {@link UserRepository#existsById} to always return {@code true} so that
 * the {@code SessionCookieFilter}'s user-existence check passes for any test
 * user ID without requiring a live database row.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMvcDefaults.class)
public abstract class BaseControllerTest {

    @MockBean
    protected UserRepository userRepository;

    @BeforeEach
    void allowAllTestUserIds() {
        Mockito.when(userRepository.existsById(Mockito.anyString())).thenReturn(true);
    }
}
