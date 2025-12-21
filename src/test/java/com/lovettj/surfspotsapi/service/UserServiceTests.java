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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.entity.AuthProvider;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.UserAuthProvider;
import com.lovettj.surfspotsapi.entity.Settings;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.repository.UserAuthProviderRepository;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;
import com.lovettj.surfspotsapi.requests.UserRequest;
import com.lovettj.surfspotsapi.requests.SettingsRequest;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private TripService tripService;

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
        updateRequest.setAge(25);
        updateRequest.setGender("Male");
        updateRequest.setHeight(180); // height in cm
        updateRequest.setWeight(75); // weight in kg
        updateRequest.setSkillLevel(com.lovettj.surfspotsapi.enums.SkillLevel.INTERMEDIATE);

        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");

        UserProfile result = userService.updateUserProfile(updateRequest);

        assertEquals("New City", result.getCity());
        assertEquals(25, result.getAge());
        assertEquals("Male", result.getGender());
        assertEquals(180, result.getHeight());
        assertEquals(75, result.getWeight());
        assertEquals(com.lovettj.surfspotsapi.enums.SkillLevel.INTERMEDIATE, result.getSkillLevel());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfileShouldAcceptBoundaryValues() {
        UserRequest updateRequest = new UserRequest();
        updateRequest.setEmail("test@example.com");
        updateRequest.setAge(13); // min age
        updateRequest.setHeight(50); // min height
        updateRequest.setWeight(9); // min weight

        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");

        UserProfile result = userService.updateUserProfile(updateRequest);

        assertEquals(13, result.getAge());
        assertEquals(50, result.getHeight());
        assertEquals(9, result.getWeight());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfileShouldAcceptMaxBoundaryValues() {
        UserRequest updateRequest = new UserRequest();
        updateRequest.setEmail("test@example.com");
        updateRequest.setAge(120); // max age
        updateRequest.setHeight(305); // max height (120 inches converted)
        updateRequest.setWeight(500); // max weight

        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");

        UserProfile result = userService.updateUserProfile(updateRequest);

        assertEquals(120, result.getAge());
        assertEquals(305, result.getHeight());
        assertEquals(500, result.getWeight());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserProfileShouldAcceptNullOptionalFields() {
        UserRequest updateRequest = new UserRequest();
        updateRequest.setEmail("test@example.com");
        updateRequest.setAge(null);
        updateRequest.setHeight(null);
        updateRequest.setWeight(null);
        updateRequest.setGender(null);
        updateRequest.setSkillLevel(null);

        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");

        UserProfile result = userService.updateUserProfile(updateRequest);

        assertNull(result.getAge());
        assertNull(result.getHeight());
        assertNull(result.getWeight());
        assertNull(result.getGender());
        assertNull(result.getSkillLevel());
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
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId("generated-user-id");
            }
            return user;
        }).when(userRepository).save(any(User.class));
        doNothing().when(tripService).processPendingInvitations(anyString(), anyString());

        userService.registerUser(request);

        verify(userRepository).findByEmail("new@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository, times(2)).save(any(User.class)); // Now expecting 2 saves
        verify(tripService).processPendingInvitations(anyString(), anyString());
    }

    @Test
    void registerUserShouldCreateNewGoogleUser() {
        AuthRequest request = new AuthRequest();
        request.setEmail("google@example.com");
        request.setProvider(AuthProvider.GOOGLE);
        request.setProviderId("google123");
        request.setName("Google User");

        doReturn(Optional.empty()).when(userRepository).findByEmail("google@example.com");
        doReturn(Optional.empty()).when(userAuthProviderRepository).findByProviderAndProviderId(AuthProvider.GOOGLE, "google123");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId("generated-user-id");
            }
            return user;
        }).when(userRepository).save(any(User.class));
        doNothing().when(tripService).processPendingInvitations(anyString(), anyString());

        userService.registerUser(request);

        verify(userRepository).findByEmail("google@example.com");
        verify(userAuthProviderRepository).findByProviderAndProviderId(AuthProvider.GOOGLE, "google123");
        verify(userRepository).save(argThat(user -> {
            assertEquals("google@example.com", user.getEmail());
            assertEquals("Google User", user.getName());
            assertNotNull(user.getSettings());
            assertTrue(user.getSettings().isNewSurfSpotEmails());
            assertTrue(user.getSettings().isNearbySurfSpotsEmails());
            assertTrue(user.getSettings().isSwellSeasonEmails());
            assertTrue(user.getSettings().isEventEmails());
            assertTrue(user.getSettings().isPromotionEmails());
            return true;
        }));
        verify(userAuthProviderRepository).save(argThat(authProvider -> {
            assertEquals(AuthProvider.GOOGLE, authProvider.getProvider());
            assertEquals("google123", authProvider.getProviderId());
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
        doReturn(false).when(userAuthProviderRepository).existsByUserIdAndProvider(testUserId, AuthProvider.GOOGLE);

        userService.registerUser(request);

        verify(userRepository).findByEmail("test@example.com");
        verify(userAuthProviderRepository).existsByUserIdAndProvider(testUserId, AuthProvider.GOOGLE);
        verify(userAuthProviderRepository).save(argThat(authProvider -> {
            assertEquals(AuthProvider.GOOGLE, authProvider.getProvider());
            assertEquals("google123", authProvider.getProviderId());
            assertEquals(testUser, authProvider.getUser());
            return true;
        }));
        // User should not be saved since it already has a name
        verify(userRepository, never()).save(any(User.class));
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
    void registerUserShouldCreateNewFacebookUser() {
        AuthRequest request = new AuthRequest();
        request.setEmail("facebook@example.com");
        request.setProvider(AuthProvider.FACEBOOK);
        request.setProviderId("facebook123");
        request.setName("Facebook User");

        doReturn(Optional.empty()).when(userRepository).findByEmail("facebook@example.com");
        doReturn(Optional.empty()).when(userAuthProviderRepository).findByProviderAndProviderId(AuthProvider.FACEBOOK, "facebook123");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId("generated-user-id");
            }
            return user;
        }).when(userRepository).save(any(User.class));
        doNothing().when(tripService).processPendingInvitations(anyString(), anyString());

        userService.registerUser(request);

        verify(userRepository).findByEmail("facebook@example.com");
        verify(userAuthProviderRepository).findByProviderAndProviderId(AuthProvider.FACEBOOK, "facebook123");
        verify(userRepository).save(argThat(user -> {
            assertEquals("facebook@example.com", user.getEmail());
            assertEquals("Facebook User", user.getName());
            assertNotNull(user.getSettings());
            assertTrue(user.getSettings().isNewSurfSpotEmails());
            assertTrue(user.getSettings().isNearbySurfSpotsEmails());
            assertTrue(user.getSettings().isSwellSeasonEmails());
            assertTrue(user.getSettings().isEventEmails());
            assertTrue(user.getSettings().isPromotionEmails());
            return true;
        }));
        verify(userAuthProviderRepository).save(argThat(authProvider -> {
            assertEquals(AuthProvider.FACEBOOK, authProvider.getProvider());
            assertEquals("facebook123", authProvider.getProviderId());
            return true;
        }));
    }

    @Test
    void registerUserShouldUpdateExistingUserWithFacebookAuth() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setProvider(AuthProvider.FACEBOOK);
        request.setProviderId("facebook123");
        request.setName("Updated Name");

        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");
        doReturn(false).when(userAuthProviderRepository).existsByUserIdAndProvider(testUserId, AuthProvider.FACEBOOK);

        userService.registerUser(request);

        verify(userRepository).findByEmail("test@example.com");
        verify(userAuthProviderRepository).existsByUserIdAndProvider(testUserId, AuthProvider.FACEBOOK);
        verify(userAuthProviderRepository).save(argThat(authProvider -> {
            assertEquals(AuthProvider.FACEBOOK, authProvider.getProvider());
            assertEquals("facebook123", authProvider.getProviderId());
            assertEquals(testUser, authProvider.getUser());
            return true;
        }));
        // User should not be saved since it already has a name
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUserShouldNotAddDuplicateProvider() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setProvider(AuthProvider.GOOGLE);
        request.setProviderId("google123");
        request.setName("Updated Name");

        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");
        doReturn(true).when(userAuthProviderRepository).existsByUserIdAndProvider(testUserId, AuthProvider.GOOGLE);

        userService.registerUser(request);

        verify(userRepository).findByEmail("test@example.com");
        verify(userAuthProviderRepository).existsByUserIdAndProvider(testUserId, AuthProvider.GOOGLE);
        // Should not save new auth provider since it already exists
        verify(userAuthProviderRepository, times(0)).save(any(UserAuthProvider.class));
        // Should not save user since provider already exists
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUserShouldAddProviderToUserWithoutName() {
        // Create a user without a name
        User userWithoutName = new User();
        userWithoutName.setId("user-no-name");
        userWithoutName.setEmail("noname@example.com");
        userWithoutName.setName(null); // No name
        userWithoutName.setPassword("hashedPassword");

        AuthRequest request = new AuthRequest();
        request.setEmail("noname@example.com");
        request.setProvider(AuthProvider.FACEBOOK);
        request.setProviderId("facebook123");
        request.setName("New Name");

        doReturn(Optional.of(userWithoutName)).when(userRepository).findByEmail("noname@example.com");
        doReturn(false).when(userAuthProviderRepository).existsByUserIdAndProvider("user-no-name", AuthProvider.FACEBOOK);

        userService.registerUser(request);

        verify(userRepository).findByEmail("noname@example.com");
        verify(userAuthProviderRepository).existsByUserIdAndProvider("user-no-name", AuthProvider.FACEBOOK);
        verify(userAuthProviderRepository).save(argThat(authProvider -> {
            assertEquals(AuthProvider.FACEBOOK, authProvider.getProvider());
            assertEquals("facebook123", authProvider.getProviderId());
            assertEquals(userWithoutName, authProvider.getUser());
            return true;
        }));
        // User should be saved when name is provided for user without name
        verify(userRepository).save(argThat(user -> {
            assertEquals("New Name", user.getName());
            return true;
        }));
    }

    @Test
    void registerUserShouldThrowWhenProviderIdMissingForFacebookProvider() {
        AuthRequest request = new AuthRequest();
        request.setEmail("facebook@example.com");
        request.setProvider(AuthProvider.FACEBOOK);
        request.setName("Facebook User");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> userService.registerUser(request));

        assertEquals("A Provider Id is required for OAuth providers.", exception.getReason());
    }

    @Test
    void registerUserShouldReturnExistingUserWhenOAuthProviderExists() {
        AuthRequest request = new AuthRequest();
        request.setEmail("new@example.com");
        request.setProvider(AuthProvider.GOOGLE);
        request.setProviderId("google123");
        request.setName("New User");

        UserAuthProvider existingAuthProvider = new UserAuthProvider();
        existingAuthProvider.setUser(testUser);
        existingAuthProvider.setProvider(AuthProvider.GOOGLE);
        existingAuthProvider.setProviderId("google123");

        doReturn(Optional.of(existingAuthProvider)).when(userAuthProviderRepository).findByProviderAndProviderId(AuthProvider.GOOGLE, "google123");

        User result = userService.registerUser(request);

        assertEquals(testUser, result);
        verify(userAuthProviderRepository).findByProviderAndProviderId(AuthProvider.GOOGLE, "google123");
        // Should not check email or create new user since OAuth provider already exists
        verify(userRepository, times(0)).findByEmail(anyString());
        verify(userRepository, times(0)).save(any(User.class));
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





    @Test
    void registerUserShouldHandleExistingUserWithNoPasswordAddingOAuth() {
        // Create a user that exists but has no password (OAuth user)
        User oauthUser = new User();
        oauthUser.setId("oauth-user");
        oauthUser.setEmail("oauth@example.com");
        oauthUser.setName("OAuth User");
        oauthUser.setPassword(null); // No password

        AuthRequest request = new AuthRequest();
        request.setEmail("oauth@example.com");
        request.setProvider(AuthProvider.FACEBOOK);
        request.setProviderId("facebook123");
        request.setName("Updated OAuth User");

        doReturn(Optional.of(oauthUser)).when(userRepository).findByEmail("oauth@example.com");
        doReturn(false).when(userAuthProviderRepository).existsByUserIdAndProvider("oauth-user", AuthProvider.FACEBOOK);

        userService.registerUser(request);

        verify(userRepository).findByEmail("oauth@example.com");
        verify(userAuthProviderRepository).existsByUserIdAndProvider("oauth-user", AuthProvider.FACEBOOK);
        verify(userAuthProviderRepository).save(argThat(authProvider -> {
            assertEquals(AuthProvider.FACEBOOK, authProvider.getProvider());
            assertEquals("facebook123", authProvider.getProviderId());
            assertEquals(oauthUser, authProvider.getUser());
            return true;
        }));
        // User should not be saved since it already has a name
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUserShouldHandleExistingUserWithPasswordAddingOAuth() {
        // Create a user that exists with password (email user)
        User emailUser = new User();
        emailUser.setId("email-user");
        emailUser.setEmail("email@example.com");
        emailUser.setName("Email User");
        emailUser.setPassword("hashedPassword");

        AuthRequest request = new AuthRequest();
        request.setEmail("email@example.com");
        request.setProvider(AuthProvider.GOOGLE);
        request.setProviderId("google123");
        request.setName("Updated Email User");

        doReturn(Optional.of(emailUser)).when(userRepository).findByEmail("email@example.com");
        doReturn(false).when(userAuthProviderRepository).existsByUserIdAndProvider("email-user", AuthProvider.GOOGLE);

        userService.registerUser(request);

        verify(userRepository).findByEmail("email@example.com");
        verify(userAuthProviderRepository).existsByUserIdAndProvider("email-user", AuthProvider.GOOGLE);
        verify(userAuthProviderRepository).save(argThat(authProvider -> {
            assertEquals(AuthProvider.GOOGLE, authProvider.getProvider());
            assertEquals("google123", authProvider.getProviderId());
            assertEquals(emailUser, authProvider.getUser());
            return true;
        }));
        // User should not be saved since it already has a name
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUserShouldNotUpdateNameWhenUserAlreadyHasName() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setProvider(AuthProvider.GOOGLE);
        request.setProviderId("google123");
        request.setName("New Name"); // Different name

        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");
        doReturn(false).when(userAuthProviderRepository).existsByUserIdAndProvider(testUserId, AuthProvider.GOOGLE);

        userService.registerUser(request);

        verify(userRepository).findByEmail("test@example.com");
        verify(userAuthProviderRepository).existsByUserIdAndProvider(testUserId, AuthProvider.GOOGLE);
        verify(userAuthProviderRepository).save(argThat(authProvider -> {
            assertEquals(AuthProvider.GOOGLE, authProvider.getProvider());
            assertEquals("google123", authProvider.getProviderId());
            assertEquals(testUser, authProvider.getUser());
            return true;
        }));
        // User should not be saved since it already has a name
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUserShouldHandleEmptyNameInRequest() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setProvider(AuthProvider.GOOGLE);
        request.setProviderId("google123");
        request.setName(""); // Empty name

        doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@example.com");
        doReturn(false).when(userAuthProviderRepository).existsByUserIdAndProvider(testUserId, AuthProvider.GOOGLE);

        userService.registerUser(request);

        verify(userRepository).findByEmail("test@example.com");
        verify(userAuthProviderRepository).existsByUserIdAndProvider(testUserId, AuthProvider.GOOGLE);
        verify(userAuthProviderRepository).save(argThat(authProvider -> {
            assertEquals(AuthProvider.GOOGLE, authProvider.getProvider());
            assertEquals("google123", authProvider.getProviderId());
            assertEquals(testUser, authProvider.getUser());
            return true;
        }));
        // User should not be saved since name is empty and testUser already has a name
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    void deleteAccountShouldDeleteUserAndTripEdgeCases() {
        User testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .build();
        
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        doNothing().when(tripService).deleteAllUserTrips(testUserId, testUser.getEmail());
        doNothing().when(userRepository).delete(testUser);

        userService.deleteAccount(testUserId);

        verify(tripService).deleteAllUserTrips(testUserId, testUser.getEmail());
        verify(userRepository).delete(testUser);
    }
}
