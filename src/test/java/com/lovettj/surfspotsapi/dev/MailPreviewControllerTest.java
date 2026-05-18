package com.lovettj.surfspotsapi.dev;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.testutil.AppPropertiesFactory;

/**
 * Standalone {@link MockMvc} avoids {@code @WebMvcTest} context load issues (async, sliced config,
 * profile interaction) while still exercising routing and Thymeleaf delegation on the controller.
 */
@ExtendWith(MockitoExtension.class)
class MailPreviewControllerTest {

    @Mock
    private TemplateEngine templateEngine;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MailPreviewController controller =
                new MailPreviewController(
                        templateEngine,
                        AppPropertiesFactory.localhostDefaults(),
                        "");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void tripInvitationPreviewShouldReturnRenderedHtml() throws Exception {
        String logicalName = TransactionalEmailTemplate.TRIP_INVITATION.getLogicalName();
        when(templateEngine.process(eq(logicalName), any(Context.class))).thenReturn("<html>preview</html>");

        mockMvc.perform(get("/api/dev/mail-preview/" + logicalName))
                .andExpect(status().isOk())
                .andExpect(content().string("<html>preview</html>"));
    }

    @Test
    void unknownTemplateShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/dev/mail-preview/not-a-template")).andExpect(status().isNotFound());
    }
}
