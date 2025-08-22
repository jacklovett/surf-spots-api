package com.lovettj.surfspotsapi.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.Settings;
import com.lovettj.surfspotsapi.entity.AuthProvider;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;
import com.lovettj.surfspotsapi.requests.SettingsRequest;
import com.lovettj.surfspotsapi.requests.UserRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder;

    public Optional<UserProfile> getUserProfile(String userId) {
        return userRepository.findById(userId)
                .map(UserProfile::new);
    }

    public void updatePassword(ChangePasswordRequest changePasswordRequest) {
        String userId = changePasswordRequest.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check if the current password matches the stored password
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The provided password doesn't match your current password.");
        }

        setUserPassword(user, changePasswordRequest.getNewPassword());
    }

    public void updateSettings(SettingsRequest settingsRequest) {
        String userId = settingsRequest.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Settings settings = user.getSettings();
        settings.setNewSurfSpotEmails(settingsRequest.isNewSurfSpotEmails());
        settings.setNearbySurfSpotsEmails(settingsRequest.isNearbySurfSpotsEmails());
        settings.setSwellSeasonEmails(settingsRequest.isSwellSeasonEmails());
        settings.setEventEmails(settingsRequest.isEventEmails());
        settings.setPromotionEmails(settingsRequest.isPromotionEmails());
        userRepository.save(user);
    }

    public UserProfile updateUserProfile(UserRequest updateUserRequest) {
        String email = updateUserRequest.getEmail();
        User user = findUserByEmail(email);
        user.setName(updateUserRequest.getName());
        user.setCountry(updateUserRequest.getCountry());
        user.setCity(updateUserRequest.getCity());
        userRepository.save(user);
        return new UserProfile(user);
    }

    public User registerUser(AuthRequest authRequest) {
        // Validate providerId for non-EMAIL providers
        if (authRequest.getProvider() != AuthProvider.EMAIL && authRequest.getProviderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A Provider Id is required for OAuth providers.");
        }
        Optional<User> existingUser = userRepository.findByEmail(authRequest.getEmail());

        if (existingUser.isPresent()) {
            return handleExistingUser(existingUser.get(), authRequest);
        } else {
           return createNewUser(authRequest);
        }
    }

    private User handleExistingUser(User existingUser, AuthRequest authRequest) {
        if (existingUser.getPassword() != null && authRequest.getProvider() == AuthProvider.EMAIL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "An account with this email already exists. Please try signing in.");
        }

        if (authRequest.getProvider() != AuthProvider.EMAIL) {
            return updateProviderDetails(existingUser, authRequest);
        } else {
            setUserPassword(existingUser, authRequest.getPassword());
            return userRepository.save(existingUser);
        }
    }

    private User updateProviderDetails(User user, AuthRequest authRequest) {
        user.setProvider(authRequest.getProvider());
        user.setProviderId(authRequest.getProviderId());
        if (authRequest.getName() != null) {
            user.setName(authRequest.getName());
        }
        return userRepository.save(user);
    }

    private User createNewUser(AuthRequest authRequest) {
        User newUser = createUserFromRequest(authRequest);
        setupUserSettings(newUser);
        return userRepository.save(newUser);
    }

    private User createUserFromRequest(AuthRequest authRequest) {
        User newUser = new User();
        newUser.setEmail(authRequest.getEmail());
        newUser.setName(authRequest.getName());
        newUser.setProvider(authRequest.getProvider());
        newUser.setProviderId(authRequest.getProviderId());
        
        if (authRequest.getProvider() == AuthProvider.EMAIL) {
            setUserPassword(newUser, authRequest.getPassword());
        }
        
        return newUser;
    }

    private void setupUserSettings(User user) {
        Settings settings = Settings.builder()
            .newSurfSpotEmails(true)
            .nearbySurfSpotsEmails(true)
            .swellSeasonEmails(true)
            .eventEmails(true)
            .promotionEmails(true)
            .user(user)
            .build();
        user.setSettings(settings);
    }

    public User loginUser(String email, String password) {
        User user = findUserByEmail(email);
        // Check if the password matches
        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
         
        if (!passwordMatches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        return user;
    }

    public void setUserPassword(User user, String password) {
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        // hash new password
        String hashedPassword = passwordEncoder.encode(password);
        user.setPassword(hashedPassword);
        userRepository.save(user);
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
