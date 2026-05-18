package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.email.EmailLayoutVariables;
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;

import com.lovettj.surfspotsapi.config.AppProperties;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final boolean emailEnabled;
    private final String appBaseUrl;
    private final String mailFrom;
    private final String resolvedEmailLogoUrl;
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${app.mail.enabled:true}") boolean emailEnabled,
            AppProperties appProperties,
            @Value("${app.mail.from:}") String mailFrom,
            @Value("${app.email.logo-url:}") String emailLogoUrlOverride) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailEnabled = emailEnabled;
        this.appBaseUrl = EmailLayoutVariables.normalizeAppBaseUrl(appProperties.getUrl());
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
        this.resolvedEmailLogoUrl =
                EmailLayoutVariables.resolveLogoImageUrl(emailLogoUrlOverride, this.appBaseUrl);
    }

    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        Map<String, Object> mergedVariables = mergeEmailLayoutVariables(variables);
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would send email to {} with subject: {}", to, subject);
            logger.debug("Email content: {}", generateHtmlContent(templateName, mergedVariables));
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (!mailFrom.isEmpty()) {
                helper.setFrom(mailFrom);
            }
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(generateHtmlContent(templateName, mergedVariables), true);

            mailSender.send(message);
            logger.info("Email sent to {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to build email to {}: {}", to, e.getMessage());
        } catch (MailException e) {
            logger.warn("Failed to send email to {}: {}. This is non-critical and the operation will continue.", to, e.getMessage());
            // Don't throw - email failures shouldn't break the app
        }
    }

    private Map<String, Object> mergeEmailLayoutVariables(Map<String, Object> variables) {
        Map<String, Object> merged = new HashMap<>(variables);
        merged.putIfAbsent("emailLogoUrl", resolvedEmailLogoUrl);
        merged.putIfAbsent("appUrl", appBaseUrl);
        return merged;
    }

    private String generateHtmlContent(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }

    public void sendTripMemberAddedNotification(
            String to,
            String memberName,
            String inviterName,
            String tripTitle,
            LocalDate tripStartDate,
            LocalDate tripEndDate,
            String tripDescription) {
        String subject = "You've been added to a trip: " + tripTitle;
        Map<String, Object> variables = new HashMap<>();
        variables.put("memberName", memberName);
        variables.put("inviterName", inviterName);
        variables.put("tripTitle", tripTitle);
        variables.put("appUrl", appBaseUrl);
        putTripDetailFieldsForEmail(variables, tripStartDate, tripEndDate, tripDescription);
        sendEmail(to, subject, TransactionalEmailTemplate.TRIP_MEMBER_ADDED.getLogicalName(), variables);
    }

    public void sendTripInvitation(
            String to,
            String inviterName,
            String tripTitle,
            String token,
            LocalDate tripStartDate,
            LocalDate tripEndDate,
            String tripDescription) {
        String subject = inviterName + " invited you to join a surf trip on Surf Spots";
        String inviteLink = appBaseUrl + "/auth/sign-up?invite=" + token;
        Map<String, Object> variables = new HashMap<>();
        variables.put("inviterName", inviterName);
        variables.put("tripTitle", tripTitle);
        variables.put("inviteLink", inviteLink);
        variables.put("appUrl", appBaseUrl);

        putTripDetailFieldsForEmail(variables, tripStartDate, tripEndDate, tripDescription);

        sendEmail(to, subject, TransactionalEmailTemplate.TRIP_INVITATION.getLogicalName(), variables);
    }

    private static void putTripDetailFieldsForEmail(
            Map<String, Object> variables,
            LocalDate tripStartDate,
            LocalDate tripEndDate,
            String tripDescription) {
        String tripDatesLine = formatTripDateRangeForEmail(tripStartDate, tripEndDate);
        variables.put("tripHasSchedule", tripDatesLine != null);
        if (tripDatesLine != null) {
            variables.put("tripDatesLine", tripDatesLine);
        }

        String tripSummaryText = clipTripDescriptionForEmail(tripDescription);
        variables.put("tripHasSummary", tripSummaryText != null);
        if (tripSummaryText != null) {
            variables.put("tripSummaryText", tripSummaryText);
        }
    }

    private static final DateTimeFormatter TRIP_EMAIL_DATE =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    /**
     * Single-line schedule for transactional mail; null when the trip has no start or end date.
     */
    private static String formatTripDateRangeForEmail(LocalDate tripStartDate, LocalDate tripEndDate) {
        if (tripStartDate == null && tripEndDate == null) {
            return null;
        }
        if (tripStartDate != null && tripEndDate != null) {
            if (tripStartDate.equals(tripEndDate)) {
                return TRIP_EMAIL_DATE.format(tripStartDate);
            }
            return TRIP_EMAIL_DATE.format(tripStartDate) + " – " + TRIP_EMAIL_DATE.format(tripEndDate);
        }
        if (tripStartDate != null) {
            return "Starts " + TRIP_EMAIL_DATE.format(tripStartDate);
        }
        return "Ends " + TRIP_EMAIL_DATE.format(tripEndDate);
    }

    /**
     * Returns the stored description unchanged except for an optional length cap (very long notes break narrow mail clients).
     * Omits only null, empty, or whitespace-only values.
     */
    private static String clipTripDescriptionForEmail(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return null;
        }
        int maxLength = 320;
        if (rawDescription.length() <= maxLength) {
            return rawDescription;
        }
        return rawDescription.substring(0, maxLength) + "…";
    }
}
