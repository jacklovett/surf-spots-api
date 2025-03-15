package com.lovettj.surfspotsapi.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder;

    public Optional<UserProfile> getUserProfile(Long userId) {
        return userRepository.findById(userId)
                .map(UserProfile::new);
    }

    public void updatePassword(ChangePasswordRequest changePasswordRequest) {
        Long userId = changePasswordRequest.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check if the current password matches the stored password
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The provided password doesn’t match your current password.");
        }

        setUserPassword(user, changePasswordRequest.getNewPassword());
    }

    public UserProfile updateUserProfile(User updateUserRequest) {
        String email = updateUserRequest.getEmail();
        User user = findUserByEmail(email);
        user.setName(updateUserRequest.getName());
        user.setCountry(updateUserRequest.getCountry());
        user.setCity(updateUserRequest.getCity());
        userRepository.save(user);
        return new UserProfile(user);
    }

    public User findOrCreateUser(AuthRequest userRequest) {
        String email = userRequest.getEmail();
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            User foundUser = user.get();
            String providerId = userRequest.getProviderId();
            // Only update provider details if providerId is provided
            if (providerId != null && !providerId.isEmpty()) {
                foundUser.setProvider(userRequest.getProvider());
                foundUser.setProviderId(providerId);
                userRepository.save(foundUser);
            }
            return foundUser;
        } else {
            User newUser = new User();
            newUser.setName(userRequest.getName());
            newUser.setEmail(email);
            newUser.setProvider(userRequest.getProvider());
            // Only set providerId if it exists
            if (userRequest.getProviderId() != null) {
                newUser.setProviderId(userRequest.getProviderId());
            }
            userRepository.save(newUser);
            return newUser;
        }
    }

    public void registerUser(User userRequest) {
        Optional<User> user = userRepository.findByEmail(userRequest.getEmail());

        if (user.isPresent()) {
            User existingUser = user.get();
            if (existingUser.getPassword() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
            }
            setUserPassword(existingUser, userRequest.getPassword());
        } else {
            // Register new user with hashed password
            setUserPassword(userRequest, userRequest.getPassword());
        }
    }

    public User loginUser(String email, String password) {
        User user = findUserByEmail(email);
        // Check if the password matches
        if (!passwordEncoder.matches(password, user.getPassword())) {
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
