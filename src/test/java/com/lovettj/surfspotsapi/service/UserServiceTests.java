package com.lovettj.surfspotsapi.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.entity.AuthProvider;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.Settings;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;
import com.lovettj.surfspotsapi.requests.UserRequest;
import com.lovettj.surfspotsapi.requests.SettingsRequest;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-id-123";
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setProvider(AuthProvider.EMAIL);
        testUser.setPassword("hashedPassword");
        
        // Initialize settings
        Settings settings = Settings.builder()
            .newSurfSpotEmails(true)
            .nearbySurfSpotsEmails(true)
            .swellSeasonEmails(true)
            .eventEmails(true)
            .promotionEmails(true)
            .user(testUser)
            .build();
        testUser.setSettings(settings);
    }

    @Test
    void getUserProfileShouldReturnUserProfile() {
        doReturn(Optional.of(testUser)).when(userRepository).findById(testUserId);

        Optional<UserProfile> result = userService.getUserProfile(testUserId);

        assertTrue(result.isPresent());
        assertEquals(testUser.getName(), result.get().getName());
    }

    @Test
    void getUserProfileShouldReturnEmptyWhenUserNotFound() {
        doReturn(Optional.empty()).when(userRepository).findById("not-found-id");

        Optional<UserProfile> result = userService.getUserProfile("not-found-id");

        assertFalse(result.isPresent());
    }

    @Test
    void updatePasswordShouldUpdateUserPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUserId(testUserId);
        request.setCurrentPassword("currentPass");
        request.setNewPassword("newPassword123");

        doReturn(Optional.of(testUser)).when(userRepository).findById(testUserId);
        doReturn(true).when(passwordEncoder).matches("currentPass", "hashedPassword");
        doReturn("newHashedPassword").when(passwordEncoder).encode("newPassword123");

        assertDoesNotThrow(() -> userService.updatePassword(request));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserProfileShouldUpdateUserDetails() {
        UserRequest updateRequest = new UserRequest();
        updateRequest.setEmail("test@example.com");
        updateRequest.setCity("New City");

        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");

        UserProfile result = userService.updateUserProfile(updateRequest);

        assertEquals("New City", result.getCity());
        verify(userRepository).save(testUser);
    }

    @Test
    void registerUserShouldCreateNewEmailUser() {
        AuthRequest request = new AuthRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setProvider(AuthProvider.EMAIL);
        request.setName("New User");

        doReturn(Optional.empty()).when(userRepository).findByEmail("new@example.com");
        doReturn("hashedPassword").when(passwordEncoder).encode("password123");

        userService.registerUser(request);

        verify(userRepository).findByEmail("new@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository, times(2)).save(any(User.class)); // Now expecting 2 saves
    }

    @Test
    void registerUserShouldCreateNewGoogleUser() {
        AuthRequest request = new AuthRequest();
        request.setEmail("google@example.com");
        request.setProvider(AuthProvider.GOOGLE);
        request.setProviderId("google123");
        request.setName("Google User");

        doReturn(Optional.empty()).when(userRepository).findByEmail("google@example.com");

        userService.registerUser(request);

        verify(userRepository).findByEmail("google@example.com");
        verify(userRepository).save(argThat(user -> {
            assertEquals("google@example.com", user.getEmail());
            assertEquals("Google User", user.getName());
            assertEquals(AuthProvider.GOOGLE, user.getProvider());
            assertEquals("google123", user.getProviderId());
            assertNull(user.getPassword());
            assertNotNull(user.getSettings());
            assertTrue(user.getSettings().isNewSurfSpotEmails());
            assertTrue(user.getSettings().isNearbySurfSpotsEmails());
            assertTrue(user.getSettings().isSwellSeasonEmails());
            assertTrue(user.getSettings().isEventEmails());
            assertTrue(user.getSettings().isPromotionEmails());
            return true;
        }));
    }

    @Test
    void registerUserShouldUpdateExistingUserWithGoogleAuth() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setProvider(AuthProvider.GOOGLE);
        request.setProviderId("google123");
        request.setName("Updated Name");

        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");

        userService.registerUser(request);

        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(argThat(user -> {
            assertEquals("test@example.com", user.getEmail());
            assertEquals("Updated Name", user.getName());
            assertEquals(AuthProvider.GOOGLE, user.getProvider());
            assertEquals("google123", user.getProviderId());
            assertEquals("hashedPassword", user.getPassword()); // Password should remain unchanged
            return true;
        }));
    }

    @Test
    void registerUserShouldThrowWhenEmailExistsWithEmailProvider() {
        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");
        
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setProvider(AuthProvider.EMAIL);
        request.setPassword("password123");
        request.setName("Test User");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> userService.registerUser(request));
        
        assertEquals("An account with this email already exists. Please try signing in.", 
            exception.getReason());
    }

    @Test
    void registerUserShouldThrowWhenPasswordTooShort() {
        AuthRequest request = new AuthRequest();
        request.setEmail("new@example.com");
        request.setPassword("short");
        request.setProvider(AuthProvider.EMAIL);
        request.setName("New User");

        doReturn(Optional.empty()).when(userRepository).findByEmail("new@example.com");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> userService.registerUser(request));

        assertEquals("Password must be at least 8 characters", exception.getReason());
    }

    @Test
    void registerUserShouldThrowWhenPasswordMissingForEmailProvider() {
        AuthRequest request = new AuthRequest();
        request.setEmail("new@example.com");
        request.setProvider(AuthProvider.EMAIL);
        request.setName("New User");

        doReturn(Optional.empty()).when(userRepository).findByEmail("new@example.com");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> userService.registerUser(request));

        assertEquals("Password must be at least 8 characters", exception.getReason());
    }

    @Test
    void registerUserShouldThrowWhenProviderIdMissingForGoogleProvider() {
        AuthRequest request = new AuthRequest();
        request.setEmail("google@example.com");
        request.setProvider(AuthProvider.GOOGLE);
        request.setName("Google User");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> userService.registerUser(request));

        assertEquals("A Provider Id is required for OAuth providers.", exception.getReason());
    }

    @Test
    void loginUserShouldReturnUserWithValidCredentials() {
        // First verify testUser has the password we expect
        assertEquals("hashedPassword", testUser.getPassword());

        // Mock the repository call
        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");
        // Mock the password match using testUser's actual password
        doReturn(true).when(passwordEncoder).matches("password123", testUser.getPassword());

        User result = userService.loginUser("test@example.com", "password123");

        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void loginUserShouldThrowWithInvalidPassword() {
        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");
        doReturn(false).when(passwordEncoder).matches(anyString(), anyString());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.loginUser("test@example.com", "wrongpass"));

        assertEquals("Invalid password", exception.getReason());
        verify(passwordEncoder).matches("wrongpass", testUser.getPassword());
    }

    @Test
    void findUserByEmailShouldReturnUser() {
        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");

        User result = userService.findUserByEmail("test@example.com");

        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void findUserByEmailShouldThrowWhenNotFound() {
        doReturn(Optional.empty()).when(userRepository).findByEmail("notfound@example.com");

        assertThrows(ResponseStatusException.class,
                () -> userService.findUserByEmail("notfound@example.com"));
    }

    @Test
    void testUpdateSettings() {
        doReturn(Optional.of(testUser)).when(userRepository).findById(testUserId);

        SettingsRequest request = new SettingsRequest();
        request.setUserId(testUserId);
        request.setNewSurfSpotEmails(true);
        request.setNearbySurfSpotsEmails(true);
        request.setSwellSeasonEmails(true);
        request.setEventEmails(true);
        request.setPromotionEmails(true);

        userService.updateSettings(request);
        verify(userRepository).save(any(User.class));
    }
}
