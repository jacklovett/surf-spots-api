package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.User;

/**
 * Outcome of {@code UserService.registerUser}: the persisted user and whether a new account row
 * was created (vs sign-in, OAuth provider match, or linking a provider to an existing email).
 */
public record UserRegistrationResult(User user, boolean newlyCreatedAccount) {}
