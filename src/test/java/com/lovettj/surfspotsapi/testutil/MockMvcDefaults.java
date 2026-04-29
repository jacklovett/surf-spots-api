package com.lovettj.surfspotsapi.testutil;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Adds a trusted {@code Origin} header to every MockMvc request so
 * {@link com.lovettj.surfspotsapi.config.CsrfOriginFilter} accepts mutating requests
 * in {@code @SpringBootTest} suites. Node fetch (Remix SSR) does not set Origin;
 * MockMvc does not either. Use {@link #stripOrigin()} when a test must assert the
 * missing-Origin rejection path.
 *
 * <p>This class lives under {@code src/test/java}; it is not component-scanned.
 * Add {@code @Import(MockMvcDefaults.class)} on each {@code @AutoConfigureMockMvc} test class.
 */
@Configuration
public class MockMvcDefaults {

    public static final String TRUSTED_TEST_ORIGIN = "http://localhost:5173";

    @Bean
    MockMvcBuilderCustomizer defaultOriginHeader() {
        return builder -> builder.defaultRequest(
                MockMvcRequestBuilders.get("/")
                        .header("Origin", TRUSTED_TEST_ORIGIN));
    }

    public static RequestPostProcessor stripOrigin() {
        return request -> {
            request.removeHeader("Origin");
            return request;
        };
    }
}
