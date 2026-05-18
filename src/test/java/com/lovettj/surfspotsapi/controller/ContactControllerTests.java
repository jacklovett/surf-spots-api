package com.lovettj.surfspotsapi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.requests.ContactRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.service.EmailService;
import com.lovettj.surfspotsapi.testutil.AppPropertiesFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactControllerTests {

    @Mock
    private EmailService emailService;

    private ContactController contactController;

    private ContactRequest validContactRequest;

    @BeforeEach
    void setUp() {
        contactController = new ContactController(
                emailService, "hello@surfspots.com", AppPropertiesFactory.localhostDefaults());
        validContactRequest = new ContactRequest();
        validContactRequest.setName("John Doe");
        validContactRequest.setEmail("john@example.com");
        validContactRequest.setSubject("Test Subject");
        validContactRequest.setMessage("This is a test message");
    }

    @Test
    void sendContactMessageShouldReturnSuccessWhenEmailSent() {
        // Act
        ResponseEntity<ApiResponse<String>> response = contactController.sendContactMessage(validContactRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        ApiResponse<String> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("Contact message sent successfully.", body.getMessage());

        // Verify email service was called with correct parameters
        verify(emailService).sendEmail(
            eq("hello@surfspots.com"),
            eq("Contact Form: Test Subject"),
            eq(TransactionalEmailTemplate.CONTACT_MESSAGE.getLogicalName()),
            any()
        );
    }

    @Test
    void sendContactMessageShouldReturnErrorWhenEmailServiceThrowsException() {
        // Arrange
        doThrow(new RuntimeException("Email service error"))
            .when(emailService)
            .sendEmail(anyString(), anyString(), anyString(), any());

        // Act
        ResponseEntity<ApiResponse<String>> response = contactController.sendContactMessage(validContactRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        ApiResponse<String> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals(ApiErrors.formatErrorMessage("send", "contact message"), body.getMessage());
    }

    @Test
    void sendContactMessageShouldPassCorrectEmailVariables() {
        // Act
        contactController.sendContactMessage(validContactRequest);

        // Assert
        verify(emailService).sendEmail(
            eq("hello@surfspots.com"),
            eq("Contact Form: Test Subject"),
            eq(TransactionalEmailTemplate.CONTACT_MESSAGE.getLogicalName()),
            argThat(variables -> {
                Map<String, Object> vars = (Map<String, Object>) variables;
                return "John Doe".equals(vars.get("name")) &&
                       "john@example.com".equals(vars.get("email")) &&
                       "Test Subject".equals(vars.get("subject")) &&
                       "This is a test message".equals(vars.get("message")) &&
                       AppPropertiesFactory.LOCAL_APP_URL.equals(vars.get("appUrl"));
            })
        );
    }

    @Test
    void sendContactMessageShouldHandleEmptyFields() {
        // Arrange
        ContactRequest emptyRequest = new ContactRequest();
        emptyRequest.setName("");
        emptyRequest.setEmail("");
        emptyRequest.setSubject("");
        emptyRequest.setMessage("");

        // Act
        ResponseEntity<ApiResponse<String>> response = contactController.sendContactMessage(emptyRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailService).sendEmail(
            eq("hello@surfspots.com"),
            eq("Contact Form: "),
            eq(TransactionalEmailTemplate.CONTACT_MESSAGE.getLogicalName()),
            any()
        );
    }

    @Test
    void sendContactMessageShouldHandleSpecialCharactersInSubject() {
        // Arrange
        validContactRequest.setSubject("Special chars: !@#$%^&*()");

        // Act
        ResponseEntity<ApiResponse<String>> response = contactController.sendContactMessage(validContactRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailService).sendEmail(
            eq("hello@surfspots.com"),
            eq("Contact Form: Special chars: !@#$%^&*()"),
            eq(TransactionalEmailTemplate.CONTACT_MESSAGE.getLogicalName()),
            any()
        );
    }

    @Test
    void sendContactMessageShouldHandleLongMessage() {
        // Arrange
        String longMessage = "This is a very long message. ".repeat(100);
        validContactRequest.setMessage(longMessage);

        // Act
        ResponseEntity<ApiResponse<String>> response = contactController.sendContactMessage(validContactRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailService).sendEmail(
            eq("hello@surfspots.com"),
            eq("Contact Form: Test Subject"),
            eq(TransactionalEmailTemplate.CONTACT_MESSAGE.getLogicalName()),
            argThat(variables -> {
                Map<String, Object> vars = (Map<String, Object>) variables;
                return longMessage.equals(vars.get("message")) &&
                       AppPropertiesFactory.LOCAL_APP_URL.equals(vars.get("appUrl"));
            })
        );
    }
}
