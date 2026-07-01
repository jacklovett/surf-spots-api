package com.lovettj.surfspotsapi.dev;

import java.util.HashMap;
import java.util.Map;

import com.lovettj.surfspotsapi.config.AppProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.lovettj.surfspotsapi.email.EmailLayoutVariables;
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;

/**
 * Renders transactional email HTML in the browser for local visual QA.
 * Active only when the {@code dev} Spring profile is enabled.
 */
@Profile("dev")
@Controller
@RequestMapping("/api/dev/mail-preview")
public class MailPreviewController {

    private static final String NO_STORE_CACHE = "no-store, no-cache, must-revalidate, max-age=0";

    private final TemplateEngine templateEngine;
    private final String previewAppBaseUrl;
    private final String previewPublicApiBaseUrl;
    private final String previewEmailLogoUrl;
    private final String mapboxAccessToken;

    public MailPreviewController(
            TemplateEngine templateEngine,
            AppProperties appProperties,
            @Value("${app.email.logo-url:}") String emailLogoUrlOverride) {
        this.templateEngine = templateEngine;
        this.previewAppBaseUrl = EmailLayoutVariables.normalizeAppBaseUrl(appProperties.getUrl());
        this.previewPublicApiBaseUrl =
                EmailLayoutVariables.normalizeAppBaseUrl(appProperties.getPublicApiBaseUrl());
        this.previewEmailLogoUrl =
                EmailLayoutVariables.resolveLogoImageUrl(emailLogoUrlOverride, previewAppBaseUrl);
        String token = appProperties.getMapbox().getAccessToken();
        this.mapboxAccessToken = token != null && !token.isBlank() ? token : null;
    }

    @GetMapping(value = { "", "/" }, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> index() {
        StringBuilder listItems = new StringBuilder();
        for (TransactionalEmailTemplate template : TransactionalEmailTemplate.values()) {
            String name = template.getLogicalName();
            listItems.append("<li><a href=\"/api/dev/mail-preview/")
                    .append(name)
                    .append("\">")
                    .append(name)
                    .append("</a></li>\n");
        }
        String html =
                """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
                <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate"/>
                <title>Email previews</title>
                </head>
                <body style="font-family: system-ui, sans-serif; max-width: 40rem; margin: 2rem;">
                <h1>Email template previews (dev)</h1>
                <p>Sample data is filled in. Hard-refresh the browser (Ctrl+F5) if styles look stale after edits.</p>
                <ul>
                """
                + listItems
                + """
                </ul>
                </body>
                </html>
                """;
        return previewResponse(html);
    }

    @GetMapping(value = "/{templateName}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@PathVariable String templateName) {
        TransactionalEmailTemplate template = TransactionalEmailTemplate.fromLogicalName(templateName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown template"));
        Context context = new Context();
        context.setVariables(withEmailLayoutVariables(sampleVariables(template)));
        String html = templateEngine.process(template.getLogicalName(), context);
        return previewResponse(html);
    }

    private Map<String, Object> withEmailLayoutVariables(Map<String, Object> variables) {
        Map<String, Object> merged = new HashMap<>(variables);
        merged.putIfAbsent("emailLogoUrl", previewEmailLogoUrl);
        merged.putIfAbsent("appUrl", previewAppBaseUrl);
        return merged;
    }

    private static ResponseEntity<String> previewResponse(String html) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, NO_STORE_CACHE)
                .header(HttpHeaders.PRAGMA, "no-cache")
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private Map<String, Object> sampleVariables(TransactionalEmailTemplate template) {
        return switch (template) {
            case TRIP_INVITATION -> {
                Map<String, Object> invitation = new HashMap<>();
                invitation.put("inviterName", "Alex Surfer");
                invitation.put("tripTitle", "Portugal spring trip");
                invitation.put("inviteLink", previewAppBaseUrl + "/auth/sign-up?invite=preview-token");
                invitation.put("appUrl", previewAppBaseUrl);
                invitation.put("tripHasSchedule", true);
                invitation.put("tripDatesLine", "Mar 14, 2026 – Mar 28, 2026");
                invitation.put("tripHasSummary", true);
                invitation.put(
                        "tripSummaryText",
                        "Nazaré and Peniche windows, shared stay in Ericeira. Bring a step-up if the charts go XL.");
                yield invitation;
            }
            case TRIP_MEMBER_ADDED -> {
                Map<String, Object> memberAdded = new HashMap<>();
                memberAdded.put("memberName", "Jamie");
                memberAdded.put("inviterName", "Alex Surfer");
                memberAdded.put("tripTitle", "Portugal spring trip");
                memberAdded.put("appUrl", previewAppBaseUrl);
                memberAdded.put("tripHasSchedule", true);
                memberAdded.put("tripDatesLine", "Mar 14, 2026 – Mar 28, 2026");
                memberAdded.put("tripHasSummary", true);
                memberAdded.put(
                        "tripSummaryText",
                        "Nazaré and Peniche windows, shared stay in Ericeira. Bring a step-up if the charts go XL.");
                yield memberAdded;
            }
            case RESET_PASSWORD -> {
                Map<String, Object> reset = new HashMap<>();
                reset.put("resetLink", previewAppBaseUrl + "/reset-password?token=preview-token");
                reset.put("appUrl", previewAppBaseUrl);
                yield reset;
            }
            case VERIFY_EMAIL -> {
                Map<String, Object> verify = new HashMap<>();
                verify.put("verifyLink", previewPublicApiBaseUrl + "/api/auth/verify-email?token=preview-token");
                verify.put("appUrl", previewAppBaseUrl);
                verify.put("userName", "Preview User");
                yield verify;
            }
            case CONTACT_MESSAGE -> {
                Map<String, Object> contact = new HashMap<>();
                contact.put("name", "Preview User");
                contact.put("email", "preview.user@example.com");
                contact.put("subject", "Question about tide windows");
                contact.put("message", "This is sample contact form body text.\nSecond line for pre-wrap.");
                contact.put("appUrl", previewAppBaseUrl);
                yield contact;
            }
            case SESSION_STARTED -> {
                Map<String, Object> started = new HashMap<>();
                started.put("contactName", "Jane Doe");
                started.put("userName", "Jack");
                started.put("spotName", "Bundoran Peak");
                started.put("startTime", "Tue 1 Jul 2026 at 09:15");
                started.put("expectedReturnTime", "Tue 1 Jul 2026 at 12:00");
                double previewLat = 54.4783;
                double previewLng = -8.2779;
                if (mapboxAccessToken != null) {
                    String mapImageUrl = String.format(
                            "https://api.mapbox.com/styles/v1/mapbox/light-v11/static/pin-s+035061(%.4f,%.4f)/%.4f,%.4f,13,0/500x250?access_token=%s",
                            previewLng, previewLat, previewLng, previewLat, mapboxAccessToken);
                    started.put("mapImageUrl", mapImageUrl);
                    started.put("mapsLink", String.format("https://www.google.com/maps?q=%.4f,%.4f", previewLat, previewLng));
                } else {
                    started.put("mapImageUrl", null);
                    started.put("mapsLink", null);
                }
                yield started;
            }
            case SESSION_ENDED -> {
                Map<String, Object> ended = new HashMap<>();
                ended.put("contactName", "Jane Doe");
                ended.put("userName", "Jack");
                ended.put("spotName", "Bundoran Peak");
                ended.put("startTime", "Tue 1 Jul 2026 at 09:15");
                ended.put("endTime", "Tue 1 Jul 2026 at 11:42");
                ended.put("duration", "2h 27m");
                yield ended;
            }
        };
    }
}
