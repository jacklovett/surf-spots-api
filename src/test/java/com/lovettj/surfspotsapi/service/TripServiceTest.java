package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.dto.TripDTO;
import com.lovettj.surfspotsapi.entity.Trip;
import com.lovettj.surfspotsapi.entity.TripInvitation;
import com.lovettj.surfspotsapi.entity.TripMember;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.*;
import com.lovettj.surfspotsapi.requests.AddTripMemberRequest;
import com.lovettj.surfspotsapi.requests.CreateTripRequest;
import com.lovettj.surfspotsapi.requests.UpdateTripRequest;
import com.lovettj.surfspotsapi.requests.UploadMediaRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private TripSpotRepository tripSpotRepository;

    @Mock
    private TripMediaRepository tripMediaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SurfSpotRepository surfSpotRepository;

    @Mock
    private TripInvitationRepository tripInvitationRepository;

    @Mock
    private TripSurfboardRepository tripSurfboardRepository;

    @Mock
    private SurfboardRepository surfboardRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private TripService tripService;

    private User testUser;
    private Trip testTrip;
    private String userId;
    private String tripId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        tripId = UUID.randomUUID().toString();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test User")
                .emailVerified(true)
                .build();

        testTrip = Trip.builder()
                .id(tripId)
                .owner(testUser)
                .title("Test Trip")
                .description("Test Description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .build();
    }

    @Test
    void createTrip_Success() {
        // Given
        CreateTripRequest request = new CreateTripRequest();
        request.setTitle("New Trip");
        request.setDescription("Trip Description");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(5));

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(tripRepository.save(any(Trip.class))).thenReturn(testTrip);

        // When
        TripDTO result = tripService.createTrip(userId, request);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(userId);
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    void createTrip_UserNotFound() {
        // Given
        CreateTripRequest request = new CreateTripRequest();
        request.setTitle("New Trip");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            tripService.createTrip(userId, request);
        });
    }

    @Test
    void updateTrip_Success() {
        // Given
        UpdateTripRequest request = new UpdateTripRequest();
        request.setTitle("Updated Title");

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(tripRepository.save(any(Trip.class))).thenReturn(testTrip);

        // When
        TripDTO result = tripService.updateTrip(userId, tripId, request);

        // Then
        assertNotNull(result);
        verify(tripRepository).findById(tripId);
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    void updateTrip_TripNotFound() {
        // Given
        UpdateTripRequest request = new UpdateTripRequest();

        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            tripService.updateTrip(userId, tripId, request);
        });
    }

    @Test
    void updateTrip_NotOwner() {
        // Given
        String otherUserId = UUID.randomUUID().toString();
        UpdateTripRequest request = new UpdateTripRequest();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            tripService.updateTrip(otherUserId, tripId, request);
        });
    }

    @Test
    void deleteTrip_Success() {
        // Given
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        doNothing().when(tripRepository).delete(testTrip);

        // When
        tripService.deleteTrip(userId, tripId);

        // Then
        verify(tripRepository).findById(tripId);
        verify(tripRepository).delete(testTrip);
    }

    @Test
    void deleteTrip_NotOwner() {
        // Given
        String otherUserId = UUID.randomUUID().toString();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            tripService.deleteTrip(otherUserId, tripId);
        });
    }

    @Test
    void getUploadUrl_SuccessAsOwner() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        UploadMediaRequest request = new UploadMediaRequest();
        request.setMediaType("image");
        String expectedS3Key = "trips/media/image/" + mediaId;
        String expectedUploadUrl = "https://example.com/upload-url";

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(storageService.generateMediaKey(mediaId, "image", "trips/media")).thenReturn(expectedS3Key);
        when(storageService.generatePresignedUploadUrl(expectedS3Key, "image/jpeg")).thenReturn(expectedUploadUrl);

        // When
        String result = tripService.getUploadUrl(userId, tripId, request, mediaId);

        // Then
        assertEquals(expectedUploadUrl, result);
        verify(tripRepository).findById(tripId);
        verify(storageService).generateMediaKey(mediaId, "image", "trips/media");
        verify(storageService).generatePresignedUploadUrl(expectedS3Key, "image/jpeg");
    }

    @Test
    void getUploadUrl_SuccessAsMember() {
        // Given
        String memberUserId = UUID.randomUUID().toString();
        User memberUser = User.builder().id(memberUserId).email("member@example.com").emailVerified(true).build();
        TripMember member = TripMember.builder()
                .id(UUID.randomUUID().toString())
                .user(memberUser)
                .trip(testTrip)
                .build();
        testTrip.setMembers(Arrays.asList(member));

        String mediaId = UUID.randomUUID().toString();
        UploadMediaRequest request = new UploadMediaRequest();
        request.setMediaType("video");
        String expectedS3Key = "trips/media/video/" + mediaId;
        String expectedUploadUrl = "https://example.com/upload-url";

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(storageService.generateMediaKey(mediaId, "video", "trips/media")).thenReturn(expectedS3Key);
        when(storageService.generatePresignedUploadUrl(expectedS3Key, "video/mp4")).thenReturn(expectedUploadUrl);

        // When
        String result = tripService.getUploadUrl(memberUserId, tripId, request, mediaId);

        // Then
        assertEquals(expectedUploadUrl, result);
        verify(tripRepository).findById(tripId);
        verify(storageService).generateMediaKey(mediaId, "video", "trips/media");
        verify(storageService).generatePresignedUploadUrl(expectedS3Key, "video/mp4");
    }

    @Test
    void getUploadUrl_TripNotFound() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        UploadMediaRequest request = new UploadMediaRequest();
        request.setMediaType("image");

        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            tripService.getUploadUrl(userId, tripId, request, mediaId);
        });
        verify(storageService, never()).generateMediaKey(any(), any(), any());
    }

    @Test
    void getUploadUrl_ForbiddenNotOwnerOrMember() {
        // Given
        String otherUserId = UUID.randomUUID().toString();
        String mediaId = UUID.randomUUID().toString();
        UploadMediaRequest request = new UploadMediaRequest();
        request.setMediaType("image");

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        // When/Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            tripService.getUploadUrl(otherUserId, tripId, request, mediaId);
        });
        assertEquals(403, exception.getStatusCode().value());
        verify(storageService, never()).generateMediaKey(any(), any(), any());
    }

    @Test
    void getUploadUrl_InvalidMediaType() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        UploadMediaRequest request = new UploadMediaRequest();
        request.setMediaType("invalid");

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        // When/Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            tripService.getUploadUrl(userId, tripId, request, mediaId);
        });
        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Media type must be 'image' or 'video'"));
        verify(storageService, never()).generateMediaKey(any(), any(), any());
    }

    @Test
    void getUploadUrl_NullMediaType() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        UploadMediaRequest request = new UploadMediaRequest();
        request.setMediaType(null);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        // When/Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            tripService.getUploadUrl(userId, tripId, request, mediaId);
        });
        assertEquals(400, exception.getStatusCode().value());
        verify(storageService, never()).generateMediaKey(any(), any(), any());
    }

    @Test
    void addMemberShouldRejectWhenOwnerEmailNotVerified() {
        User unverifiedOwner = User.builder()
                .id(userId)
                .email("owner@example.com")
                .name("Owner")
                .emailVerified(false)
                .build();
        Trip tripOwnedByUnverified = Trip.builder()
                .id(tripId)
                .owner(unverifiedOwner)
                .title("Coast run")
                .description("Weekend")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(tripOwnedByUnverified));
        when(userRepository.findById(userId)).thenReturn(Optional.of(unverifiedOwner));

        AddTripMemberRequest request = new AddTripMemberRequest();
        request.setEmail("invitee@example.com");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> tripService.addMember(userId, tripId, request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(ApiErrors.EMAIL_VERIFICATION_REQUIRED, exception.getReason());
        verify(tripInvitationRepository, never()).save(any());
        verify(emailService, never()).sendTripInvitation(anyString(), anyString(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void processPendingInvitationsShouldSendMemberAddedEmailWithTripDetails() {
        String memberId = UUID.randomUUID().toString();
        User inviter = User.builder()
                .id(UUID.randomUUID().toString())
                .email("owner@example.com")
                .name("Alex Surfer")
                .emailVerified(true)
                .build();
        User member = User.builder()
                .id(memberId)
                .email("jamie@example.com")
                .name("Jamie")
                .emailVerified(true)
                .build();
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 14);
        Trip trip = Trip.builder()
                .id(tripId)
                .owner(inviter)
                .title("Spring swell")
                .description("West coast run")
                .startDate(start)
                .endDate(end)
                .build();
        TripInvitation invitation = TripInvitation.builder()
                .id(UUID.randomUUID().toString())
                .trip(trip)
                .email(member.getEmail())
                .invitedBy(inviter)
                .invitedAt(LocalDateTime.now())
                .status("PENDING")
                .token("preview-token")
                .build();

        when(tripInvitationRepository.findByEmail(member.getEmail())).thenReturn(List.of(invitation));
        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(tripMemberRepository.findByTripIdAndUserId(tripId, memberId)).thenReturn(Optional.empty());

        tripService.processPendingInvitations(member.getEmail(), memberId);

        verify(emailService)
                .sendTripMemberAddedNotification(
                        eq(member.getEmail()),
                        eq(member.getName()),
                        eq(inviter.getName()),
                        eq(trip.getTitle()),
                        eq(start),
                        eq(end),
                        eq(trip.getDescription()));
    }
}