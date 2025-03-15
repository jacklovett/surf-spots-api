package com.lovettj.surfspotsapi.service;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceTests {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private Map<String, Object> variables;

    @BeforeEach
    void setUp() {
        variables = new HashMap<>();
        doReturn(mimeMessage).when(mailSender).createMimeMessage();
    }

    @Test
    void testSendEmailSuccess() {
        doReturn("<html>Test</html>").when(templateEngine).process(anyString(), any(Context.class));
        emailService.sendEmail("test@example.com", "Test Subject", "templateName", variables);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailMessagingException() throws MessagingException {
        doThrow(new MessagingException("Failed to build email"))
                .when(mimeMessage)
                .setSubject("Test Subject", "UTF-8");

        emailService.sendEmail("test@example.com", "Test Subject", "templateName", variables);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailMailException() {
        doReturn("<html>Test</html>").when(templateEngine).process(anyString(), any(Context.class));

        doThrow(new MailSendException("Failed to send email"))
                .when(mailSender)
                .send(any(MimeMessage.class));

        emailService.sendEmail("test@example.com", "Test Subject", "templateName", variables);

        verify(mailSender).send(any(MimeMessage.class));
    }
}
