package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.dto.TripDTO;
import com.lovettj.surfspotsapi.entity.Trip;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.*;
import com.lovettj.surfspotsapi.requests.CreateTripRequest;
import com.lovettj.surfspotsapi.requests.UpdateTripRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
}