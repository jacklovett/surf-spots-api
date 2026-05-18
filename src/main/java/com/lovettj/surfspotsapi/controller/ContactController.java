package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.config.AppProperties;
import com.lovettj.surfspotsapi.email.EmailLayoutVariables;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lovettj.surfspotsapi.requests.ContactRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.service.EmailService;
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final EmailService emailService;
    private final String contactFormRecipient;
    private final String appBaseUrl;
    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);

    public ContactController(
            EmailService emailService,
            @Value("${app.mail.contact-to:hello@surfspots.com}") String contactFormRecipient,
            AppProperties appProperties) {
        this.emailService = emailService;
        this.contactFormRecipient = contactFormRecipient;
        this.appBaseUrl = EmailLayoutVariables.normalizeAppBaseUrl(appProperties.getUrl());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> sendContactMessage(@RequestBody ContactRequest contactRequest) {
        try {
            // Prepare email variables for the template
            Map<String, Object> emailVariables = new HashMap<>();
            emailVariables.put("name", contactRequest.getName());
            emailVariables.put("email", contactRequest.getEmail());
            emailVariables.put("subject", contactRequest.getSubject());
            emailVariables.put("message", contactRequest.getMessage());
            emailVariables.put("appUrl", appBaseUrl);

            // Send email to admin
            emailService.sendEmail(
                contactFormRecipient,
                "Contact Form: " + contactRequest.getSubject(),
                TransactionalEmailTemplate.CONTACT_MESSAGE.getLogicalName(),
                emailVariables
            );

            logger.info("Contact form message sent from {} ({})", 
                contactRequest.getName(), contactRequest.getEmail());
                
            return ResponseEntity.ok(ApiResponse.success(null, "Contact message sent successfully."));
            
        } catch (Exception e) {
            logger.error("Failed to send contact form message from {}: {}", 
                contactRequest.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(ApiErrors.formatErrorMessage("send", "contact message"), 500));
        }
    }
}
