package com.lovettj.surfspotsapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final boolean emailEnabled;
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${app.mail.enabled:true}") boolean emailEnabled) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailEnabled = emailEnabled;
    }

    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would send email to {} with subject: {}", to, subject);
            logger.debug("Email content: {}", generateHtmlContent(templateName, variables));
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(generateHtmlContent(templateName, variables), true);

            mailSender.send(message);
            logger.info("Email sent to {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to build email to {}: {}", to, e.getMessage());
        } catch (MailException e) {
            logger.warn("Failed to send email to {}: {}. This is non-critical and the operation will continue.", to, e.getMessage());
            // Don't throw - email failures shouldn't break the app
        }
    }

    private String generateHtmlContent(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }

    public void sendTripMemberAddedNotification(String to, String memberName, String inviterName, String tripTitle) {
        String subject = "You've been added to a trip: " + tripTitle;
        Map<String, Object> variables = Map.of(
            "memberName", memberName,
            "inviterName", inviterName,
            "tripTitle", tripTitle,
            "appUrl", "http://localhost:5173" // TODO: Use environment variable
        );
        sendEmail(to, subject, "trip-member-added", variables);
    }

    public void sendTripInvitation(String to, String inviterName, String tripTitle, String token) {
        String subject = inviterName + " invited you to join a surf trip on Surf Spots";
        String inviteLink = "http://localhost:5173/auth/sign-up?invite=" + token; // TODO: Use environment variable
        Map<String, Object> variables = Map.of(
            "inviterName", inviterName,
            "tripTitle", tripTitle,
            "inviteLink", inviteLink,
            "appUrl", "http://localhost:5173"
        );
        sendEmail(to, subject, "trip-invitation", variables);
    }
}
