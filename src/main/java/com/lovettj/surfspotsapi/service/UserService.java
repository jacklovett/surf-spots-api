package com.lovettj.surfspotsapi.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.UserAuthProvider;
import com.lovettj.surfspotsapi.entity.Settings;
import com.lovettj.surfspotsapi.entity.AuthProvider;
import com.lovettj.surfspotsapi.repository.UserAuthProviderRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;
import com.lovettj.surfspotsapi.requests.SettingsRequest;
import com.lovettj.surfspotsapi.requests.UserRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    @Lazy
    private final TripService tripService;

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
        boolean isOAuthProvider = authRequest.getProvider() != AuthProvider.EMAIL;
        
        if (isOAuthProvider) {
            // Validate providerId for OAuth providers
            if (authRequest.getProviderId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A Provider Id is required for OAuth providers.");
            }
            
            // Check if this provider+providerId combination already exists
            Optional<UserAuthProvider> existingProvider = userAuthProviderRepository
                .findByProviderAndProviderId(authRequest.getProvider(), authRequest.getProviderId());
            if (existingProvider.isPresent()) {
                // User already exists with this OAuth provider
                return existingProvider.get().getUser();
            }
        }
        
        Optional<User> existingUser = userRepository.findByEmail(authRequest.getEmail());

        if (existingUser.isPresent()) {
            return handleExistingUser(existingUser.get(), authRequest, isOAuthProvider);
        } else {
           return createNewUser(authRequest, isOAuthProvider);
        }
    }

    private User handleExistingUser(User existingUser, AuthRequest authRequest, boolean isOAuthProvider) {
        if (existingUser.getPassword() != null && !isOAuthProvider) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "An account with this email already exists. Please try signing in.");
        }

        if (isOAuthProvider) {
            return addProviderToExistingUser(existingUser, authRequest);
        } else {
            setUserPassword(existingUser, authRequest.getPassword());
            return userRepository.save(existingUser);
        }
    }

    private User addProviderToExistingUser(User user, AuthRequest authRequest) {
        // Check if user already has this provider
        if (userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), authRequest.getProvider())) {
            // User already has this provider, just return the user
            return user;
        }
        
        // Add new provider to existing user
        UserAuthProvider newProvider = UserAuthProvider.builder()
            .user(user)
            .provider(authRequest.getProvider())
            .providerId(authRequest.getProviderId())
            .build();
        
        userAuthProviderRepository.save(newProvider);
        
        // Update user name if provided and user doesn't have a name
        if (authRequest.getName() != null && (user.getName() == null || user.getName().trim().isEmpty())) {
            user.setName(authRequest.getName());
            userRepository.save(user);
        }
        
        return user;
    }

    private User createNewUser(AuthRequest authRequest, boolean isOAuthProvider) {
        User newUser = createUserFromRequest(authRequest);
        setupUserSettings(newUser);
        newUser = userRepository.save(newUser);
        
        // Add the auth provider to the new user
        if (isOAuthProvider) {
            UserAuthProvider authProvider = UserAuthProvider.builder()
                .user(newUser)
                .provider(authRequest.getProvider())
                .providerId(authRequest.getProviderId())
                .build();
            userAuthProviderRepository.save(authProvider);
        }
        
        // Process any pending trip invitations for this email
        tripService.processPendingInvitations(newUser.getEmail(), newUser.getId());
        
        return newUser;
    }

    private User createUserFromRequest(AuthRequest authRequest) {
        User newUser = new User();
        newUser.setEmail(authRequest.getEmail());
        newUser.setName(authRequest.getName());
        
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
