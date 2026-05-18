package com.lovettj.surfspotsapi.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.testutil.AppPropertiesFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EmailServiceTests {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    private EmailService emailService;

    private Map<String, Object> variables;

    @BeforeEach
    void setUp() {
        variables = new HashMap<>();
        doReturn(mimeMessage).when(mailSender).createMimeMessage();
        emailService = new EmailService(
                mailSender, templateEngine, true, AppPropertiesFactory.localhostDefaults(), "", "");
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

    @Test
    void sendTripInvitationShouldStripTrailingSlashFromAppUrl() {
        ArgumentCaptor<Context> ctxCaptor = ArgumentCaptor.forClass(Context.class);
        emailService = new EmailService(
                mailSender,
                templateEngine,
                true,
                AppPropertiesFactory.withUrls("https://surf.example.com/", "http://localhost:8080"),
                "",
                "");
        doReturn("<html></html>").when(templateEngine).process(eq(TransactionalEmailTemplate.TRIP_INVITATION.getLogicalName()), ctxCaptor.capture());

        emailService.sendTripInvitation(
                "guest@example.com",
                "Pat",
                "Winter swell",
                "abc-token",
                null,
                null,
                null);

        Context ctx = ctxCaptor.getValue();
        assertEquals("https://surf.example.com/auth/sign-up?invite=abc-token", ctx.getVariable("inviteLink"));
        assertEquals("https://surf.example.com", ctx.getVariable("appUrl"));
        assertEquals(
                "https://surf.example.com/images/png/logo.png", ctx.getVariable("emailLogoUrl"));
        assertEquals(false, ctx.getVariable("tripHasSchedule"));
        assertEquals(false, ctx.getVariable("tripHasSummary"));
    }

    @Test
    void sendTripInvitationShouldPassScheduleAndSummaryWhenPresent() {
        ArgumentCaptor<Context> ctxCaptor = ArgumentCaptor.forClass(Context.class);
        doReturn("<html></html>").when(templateEngine).process(eq(TransactionalEmailTemplate.TRIP_INVITATION.getLogicalName()), ctxCaptor.capture());

        emailService.sendTripInvitation(
                "guest@example.com",
                "Pat",
                "Winter swell",
                "abc-token",
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 24),
                "  Peniche focus. Shared van. ");

        Context ctx = ctxCaptor.getValue();
        assertTrue((Boolean) ctx.getVariable("tripHasSchedule"));
        assertEquals("Mar 10, 2026 – Mar 24, 2026", ctx.getVariable("tripDatesLine"));
        assertTrue((Boolean) ctx.getVariable("tripHasSummary"));
        assertEquals("  Peniche focus. Shared van. ", ctx.getVariable("tripSummaryText"));
    }

    @Test
    void sendTripMemberAddedNotificationShouldUseNormalizedAppUrl() {
        ArgumentCaptor<Context> ctxCaptor = ArgumentCaptor.forClass(Context.class);
        emailService = new EmailService(
                mailSender,
                templateEngine,
                true,
                AppPropertiesFactory.withUrls("https://app.example.com///", "http://localhost:8080"),
                "",
                "");
        doReturn("<html></html>").when(templateEngine).process(eq(TransactionalEmailTemplate.TRIP_MEMBER_ADDED.getLogicalName()), ctxCaptor.capture());

        emailService.sendTripMemberAddedNotification("m@example.com", "Mo", "Pat", "Trip X", null, null, null);

        Context ctx = ctxCaptor.getValue();
        assertEquals("https://app.example.com", ctx.getVariable("appUrl"));
        assertEquals("https://app.example.com/images/png/logo.png", ctx.getVariable("emailLogoUrl"));
        assertEquals(false, ctx.getVariable("tripHasSchedule"));
        assertEquals(false, ctx.getVariable("tripHasSummary"));
    }

    @Test
    void sendEmailShouldUseConfiguredLogoUrlOverride() {
        ArgumentCaptor<Context> ctxCaptor = ArgumentCaptor.forClass(Context.class);
        emailService = new EmailService(
                mailSender,
                templateEngine,
                true,
                AppPropertiesFactory.withUrls("https://surf.example.com", "http://localhost:8080"),
                "",
                "https://cdn.example.com/logo.png");
        doReturn("<html></html>").when(templateEngine).process(eq(TransactionalEmailTemplate.TRIP_INVITATION.getLogicalName()), ctxCaptor.capture());

        emailService.sendTripInvitation(
                "guest@example.com", "Pat", "Winter swell", "tok", null, null, null);

        Context ctx = ctxCaptor.getValue();
        assertEquals("https://cdn.example.com/logo.png", ctx.getVariable("emailLogoUrl"));
    }
}
