package com.lovettj.surfspotsapi.service;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.entity.AuthProvider;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.exceptions.AuthException;
import com.lovettj.surfspotsapi.exceptions.SurfSpotsException;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Optional<UserProfile> getUserProfile(Long userId) {
        return userRepository.findById(userId)
                .map(UserProfile::new);
    }

    public void updatePassword(ChangePasswordRequest changePasswordRequest) {
        Long userId = changePasswordRequest.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SurfSpotsException("User not found"));

        // Check if the current password matches the stored password
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            throw new AuthException("The provided password doesn’t match your current password.");
        }

        String hashedNewPassword = passwordEncoder.encode(changePasswordRequest.getNewPassword());
        user.setPassword(hashedNewPassword);
        userRepository.save(user);
    }

    public UserProfile updateUserProfile(User updateUserRequest) {
        String email = updateUserRequest.getEmail();
        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setName(updateUserRequest.getName());
                    user.setCountry(updateUserRequest.getCountry());
                    user.setCity(updateUserRequest.getCity());
                    userRepository.save(user);
                    return new UserProfile(user);
                })
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findOrCreateUser(AuthRequest userRequest) {
        String email = userRequest.getEmail();
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            User foundUser = user.get();
            if (foundUser.getProviderId().isEmpty() && userRequest.getProviderId() != null) {
                foundUser.setProvider(userRequest.getProvider());
                foundUser.setProviderId(userRequest.getProviderId());
                userRepository.save(foundUser);
            }

            return foundUser;
        } else {
            AuthProvider provider = userRequest.getProvider();
            String providerId = userRequest.getProviderId();

            User newUser = new User();
            newUser.setName(userRequest.getName());
            newUser.setEmail(email);
            newUser.setProvider(provider);
            newUser.setProviderId(providerId);
            userRepository.save(newUser);
            return newUser;
        }
    }

    public void registerUser(User userRequest) {
        Optional<User> user = userRepository.findByEmail(userRequest.getEmail());

        if (user.isPresent()) {
            throw new AuthException("An account with this email already exists.");
        }

        // Logic to register user, including password hashing
        String hashedPassword = passwordEncoder.encode(userRequest.getPassword());
        userRequest.setPassword(hashedPassword);
        userRepository.save(userRequest);
    }

    public User loginUser(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found")); // Throw exception if user not found

        // Check if the password matches
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthException("Invalid password"); // Throw exception if password doesn't match
        }

        return user; // Return the authenticated user
    }
}
