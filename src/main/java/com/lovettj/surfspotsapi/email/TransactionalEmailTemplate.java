package com.lovettj.surfspotsapi.email;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thymeleaf logical names for top-level transactional HTML under {@code templates/}.
 * Used by {@link com.lovettj.surfspotsapi.service.EmailService} and {@link com.lovettj.surfspotsapi.dev.MailPreviewController}
 * so preview allowlists and real sends cannot drift.
 */
public enum TransactionalEmailTemplate {
    TRIP_INVITATION("trip-invitation"),
    TRIP_MEMBER_ADDED("trip-member-added"),
    RESET_PASSWORD("reset-password"),
    VERIFY_EMAIL("verify-email"),
    CONTACT_MESSAGE("contact-message"),
    SESSION_STARTED("session-started"),
    SESSION_ENDED("session-ended");

    private final String logicalName;

    TransactionalEmailTemplate(String logicalName) {
        this.logicalName = logicalName;
    }

    /** Thymeleaf template name (matches {@code templates/{name}.html}). */
    public String getLogicalName() {
        return logicalName;
    }

    public static Optional<TransactionalEmailTemplate> fromLogicalName(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            return Optional.empty();
        }
        
        return Arrays.stream(values())
                .filter(candidate -> candidate.logicalName.equals(templateName))
                .findFirst();
    }

    public static Set<String> allLogicalNames() {
        return Arrays.stream(values())
                .map(TransactionalEmailTemplate::getLogicalName)
                .collect(Collectors.toUnmodifiableSet());
    }
}
