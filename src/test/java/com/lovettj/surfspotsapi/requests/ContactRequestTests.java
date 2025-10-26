package com.lovettj.surfspotsapi.requests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContactRequestTests {

    private ContactRequest contactRequest;

    @BeforeEach
    void setUp() {
        contactRequest = new ContactRequest();
    }

    @Test
    void shouldSetAndGetName() {
        // Arrange
        String name = "John Doe";

        // Act
        contactRequest.setName(name);

        // Assert
        assertEquals(name, contactRequest.getName());
    }

    @Test
    void shouldSetAndGetEmail() {
        // Arrange
        String email = "john@example.com";

        // Act
        contactRequest.setEmail(email);

        // Assert
        assertEquals(email, contactRequest.getEmail());
    }

    @Test
    void shouldSetAndGetSubject() {
        // Arrange
        String subject = "Test Subject";

        // Act
        contactRequest.setSubject(subject);

        // Assert
        assertEquals(subject, contactRequest.getSubject());
    }

    @Test
    void shouldSetAndGetMessage() {
        // Arrange
        String message = "This is a test message";

        // Act
        contactRequest.setMessage(message);

        // Assert
        assertEquals(message, contactRequest.getMessage());
    }

    @Test
    void shouldHandleNullValues() {
        // Act & Assert
        assertNull(contactRequest.getName());
        assertNull(contactRequest.getEmail());
        assertNull(contactRequest.getSubject());
        assertNull(contactRequest.getMessage());
    }

    @Test
    void shouldHandleEmptyStrings() {
        // Arrange
        String emptyString = "";

        // Act
        contactRequest.setName(emptyString);
        contactRequest.setEmail(emptyString);
        contactRequest.setSubject(emptyString);
        contactRequest.setMessage(emptyString);

        // Assert
        assertEquals(emptyString, contactRequest.getName());
        assertEquals(emptyString, contactRequest.getEmail());
        assertEquals(emptyString, contactRequest.getSubject());
        assertEquals(emptyString, contactRequest.getMessage());
    }

    @Test
    void shouldHandleSpecialCharacters() {
        // Arrange
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        String emailWithSpecialChars = "test+tag@example.com";

        // Act
        contactRequest.setName(specialChars);
        contactRequest.setEmail(emailWithSpecialChars);
        contactRequest.setSubject(specialChars);
        contactRequest.setMessage(specialChars);

        // Assert
        assertEquals(specialChars, contactRequest.getName());
        assertEquals(emailWithSpecialChars, contactRequest.getEmail());
        assertEquals(specialChars, contactRequest.getSubject());
        assertEquals(specialChars, contactRequest.getMessage());
    }

    @Test
    void shouldHandleLongStrings() {
        // Arrange
        String longString = "This is a very long string. ".repeat(100);

        // Act
        contactRequest.setName(longString);
        contactRequest.setEmail(longString);
        contactRequest.setSubject(longString);
        contactRequest.setMessage(longString);

        // Assert
        assertEquals(longString, contactRequest.getName());
        assertEquals(longString, contactRequest.getEmail());
        assertEquals(longString, contactRequest.getSubject());
        assertEquals(longString, contactRequest.getMessage());
    }
}
