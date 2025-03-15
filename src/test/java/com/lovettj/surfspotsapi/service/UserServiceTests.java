package com.lovettj.surfspotsapi.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.entity.AuthProvider;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(TEST_EMAIL);
        testUser.setName("Test User");
        testUser.setPassword("hashedPassword");
        testUser.setProvider(AuthProvider.EMAIL);
    }

    @Test
    void getUserProfileShouldReturnUserProfile() {
        doReturn(Optional.of(testUser)).when(userRepository).findById(1L);

        Optional<UserProfile> result = userService.getUserProfile(1L);

        assertTrue(result.isPresent());
        assertEquals(testUser.getName(), result.get().getName());
    }

    @Test
    void getUserProfileShouldReturnEmptyWhenUserNotFound() {
        doReturn(Optional.empty()).when(userRepository).findById(999L);

        Optional<UserProfile> result = userService.getUserProfile(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void updatePasswordShouldUpdateUserPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUserId(1L);
        request.setCurrentPassword("currentPass");
        request.setNewPassword("newPassword123");

        doReturn(Optional.of(testUser)).when(userRepository).findById(1L);
        doReturn(true).when(passwordEncoder).matches("currentPass", "hashedPassword");
        doReturn("newHashedPassword").when(passwordEncoder).encode("newPassword123");

        assertDoesNotThrow(() -> userService.updatePassword(request));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserProfileShouldUpdateUserDetails() {
        User updateRequest = new User();
        updateRequest.setId(1L);
        updateRequest.setEmail(TEST_EMAIL);
        updateRequest.setCity("New City");

        doReturn(Optional.of(testUser)).when(userRepository).findByEmail(updateRequest.getEmail());

        UserProfile result = userService.updateUserProfile(updateRequest);

        assertEquals("New City", result.getCity());
        verify(userRepository).save(testUser);
    }

    @Test
    void findOrCreateUserShouldReturnExistingUser() {
        AuthRequest request = new AuthRequest();
        request.setEmail(TEST_EMAIL);
        doReturn(Optional.of(testUser)).when(userRepository).findByEmail(TEST_EMAIL);

        User result = userService.findOrCreateUser(request);

        assertEquals(TEST_EMAIL, result.getEmail());
    }

    @Test
    void findOrCreateUserShouldCreateNewUser() {
        AuthRequest request = new AuthRequest();
        request.setEmail("new@example.com");
        request.setName("New User");
        doReturn(Optional.empty()).when(userRepository).findByEmail("new@example.com");

        User result = userService.findOrCreateUser(request);

        assertEquals("new@example.com", result.getEmail());
        assertEquals("New User", result.getName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUserShouldCreateNewUser() {
        User newUser = new User();
        newUser.setEmail("new@example.com");
        newUser.setPassword("password123");
        newUser.setProvider(AuthProvider.EMAIL);

        doReturn(Optional.empty()).when(userRepository).findByEmail("new@example.com");
        doReturn("hashedPassword").when(passwordEncoder).encode("password123");

        userService.registerUser(newUser);

        verify(userRepository).findByEmail("new@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(newUser);
    }

    @Test
    void registerUserShouldThrowWhenEmailExists() {
        doReturn(Optional.of(testUser)).when(userRepository).findByEmail(TEST_EMAIL);
        assertThrows(ResponseStatusException.class, () -> userService.registerUser(testUser));
    }

    @Test
    void loginUserShouldReturnUserWithValidCredentials() {
        // First verify testUser has the password we expect
        assertEquals("hashedPassword", testUser.getPassword());

        // Mock the repository call
        doReturn(Optional.of(testUser)).when(userRepository).findByEmail(TEST_EMAIL);
        // Mock the password match using testUser's actual password
        doReturn(true).when(passwordEncoder).matches("password123", testUser.getPassword());

        User result = userService.loginUser(TEST_EMAIL, "password123");

        assertEquals(TEST_EMAIL, result.getEmail());
    }

    @Test
    void loginUserShouldThrowWithInvalidPassword() {
        doReturn(Optional.of(testUser)).when(userRepository).findByEmail(TEST_EMAIL);
        doReturn(false).when(passwordEncoder).matches(anyString(), anyString());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.loginUser(TEST_EMAIL, "wrongpass"));

        assertEquals("Invalid password", exception.getReason());
        verify(passwordEncoder).matches("wrongpass", testUser.getPassword());
    }

    @Test
    void findUserByEmailShouldReturnUser() {
        doReturn(Optional.of(testUser)).when(userRepository).findByEmail(TEST_EMAIL);

        User result = userService.findUserByEmail(TEST_EMAIL);

        assertEquals(TEST_EMAIL, result.getEmail());
    }

    @Test
    void findUserByEmailShouldThrowWhenNotFound() {
        doReturn(Optional.empty()).when(userRepository).findByEmail("notfound@example.com");

        assertThrows(ResponseStatusException.class,
                () -> userService.findUserByEmail("notfound@example.com"));
    }
}
