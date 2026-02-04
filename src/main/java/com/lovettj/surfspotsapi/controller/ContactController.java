package com.lovettj.surfspotsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lovettj.surfspotsapi.requests.ContactRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.service.EmailService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);
    
    // Admin email where contact form messages will be sent
    private static final String ADMIN_EMAIL = "hello@surfspots.com";

    public ContactController(EmailService emailService) {
        this.emailService = emailService;
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

            // Send email to admin
            emailService.sendEmail(
                ADMIN_EMAIL,
                "Contact Form: " + contactRequest.getSubject(),
                "contact-message",
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
