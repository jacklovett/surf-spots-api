package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.dto.TripDTO;
import com.lovettj.surfspotsapi.dto.TripMemberDTO;
import com.lovettj.surfspotsapi.dto.TripSpotDTO;
import com.lovettj.surfspotsapi.dto.TripSurfboardDTO;
import com.lovettj.surfspotsapi.entity.*;
import com.lovettj.surfspotsapi.repository.*;
import com.lovettj.surfspotsapi.requests.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TripService {

    private static final Logger logger = LoggerFactory.getLogger(TripService.class);

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripSpotRepository tripSpotRepository;
    private final TripMediaRepository tripMediaRepository;
    private final TripInvitationRepository tripInvitationRepository;
    private final TripSurfboardRepository tripSurfboardRepository;
    private final UserRepository userRepository;
    private final SurfSpotRepository surfSpotRepository;
    private final SurfboardRepository surfboardRepository;
    private final EmailService emailService;
    private final StorageService storageService;

    public TripService(
            TripRepository tripRepository,
            TripMemberRepository tripMemberRepository,
            TripSpotRepository tripSpotRepository,
            TripMediaRepository tripMediaRepository,
            TripInvitationRepository tripInvitationRepository,
            TripSurfboardRepository tripSurfboardRepository,
            UserRepository userRepository,
            SurfSpotRepository surfSpotRepository,
            SurfboardRepository surfboardRepository,
            EmailService emailService,
            StorageService storageService) {
        this.tripRepository = tripRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.tripSpotRepository = tripSpotRepository;
        this.tripMediaRepository = tripMediaRepository;
        this.tripInvitationRepository = tripInvitationRepository;
        this.tripSurfboardRepository = tripSurfboardRepository;
        this.userRepository = userRepository;
        this.surfSpotRepository = surfSpotRepository;
        this.surfboardRepository = surfboardRepository;
        this.emailService = emailService;
        this.storageService = storageService;
    }

    @Transactional
    public TripDTO createTrip(String userId, CreateTripRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Trip trip = Trip.builder()
                .id(UUID.randomUUID().toString())
                .owner(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        trip = tripRepository.save(trip);
        return new TripDTO(trip, userId);
    }

    @Transactional
    public TripDTO updateTrip(String userId, String tripId, UpdateTripRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        // Check if user is the owner
        if (!trip.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trip owner can update trip");
        }

        if (request.getTitle() != null) {
            trip.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            trip.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            trip.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            trip.setEndDate(request.getEndDate());
        }

        trip = tripRepository.save(trip);
        return new TripDTO(trip, userId);
    }

    @Transactional
    public void deleteTrip(String userId, String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        // Check if user is the owner
        if (!trip.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trip owner can delete trip");
        }

        // Delete related entities first to avoid foreign key constraint violations
        tripInvitationRepository.deleteAll(tripInvitationRepository.findByTripId(tripId));
        tripMediaRepository.deleteAll(tripMediaRepository.findByTripIdOrderByUploadedAtDesc(tripId));
        tripSpotRepository.deleteAll(tripSpotRepository.findByTripId(tripId));
        tripSurfboardRepository.deleteAll(tripSurfboardRepository.findByTripId(tripId));
        tripMemberRepository.deleteAll(tripMemberRepository.findByTripId(tripId));

        tripRepository.delete(trip);
    }

    public TripDTO getTrip(String userId, String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        // Check if user is owner or member
        boolean isOwner = trip.getOwner().getId().equals(userId);
        boolean isMember = trip.getMembers() != null && trip.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId));

        if (!isOwner && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to this trip");
        }

        // Load spots with rating sorting
        List<TripSpot> spots = tripSpotRepository.findByTripId(tripId);
        List<TripSpotDTO> sortedSpots = spots.stream()
                .map(TripSpotDTO::new)
                .sorted(Comparator
                        .comparing((TripSpotDTO ts) -> ts.getSurfSpotRating() != null ? ts.getSurfSpotRating() : 0)
                        .reversed()
                        .thenComparing(TripSpotDTO::getAddedAt))
                .collect(Collectors.toList());

        // Load surfboards
        List<TripSurfboard> tripSurfboards = tripSurfboardRepository.findByTripId(tripId);
        List<TripSurfboardDTO> surfboards = tripSurfboards.stream()
                .map(TripSurfboardDTO::new)
                .collect(Collectors.toList());

        // Load members and pending invitations, merge into single list
        List<TripMemberDTO> allMembers = new java.util.ArrayList<>();
        
        // Add accepted members
        if (trip.getMembers() != null) {
            List<TripMemberDTO> members = trip.getMembers().stream()
                    .map(TripMemberDTO::new)
                    .collect(Collectors.toList());
            allMembers.addAll(members);
        }
        
        // Add pending invitations
        List<TripInvitation> invitations = tripInvitationRepository.findByTripId(tripId);
        List<TripMemberDTO> pendingInvitations = invitations.stream()
                .filter(inv -> "PENDING".equals(inv.getStatus()))
                .map(TripMemberDTO::new)
                .collect(Collectors.toList());
        allMembers.addAll(pendingInvitations);

        TripDTO dto = new TripDTO(trip, userId);
        dto.setSpots(sortedSpots);
        dto.setMembers(allMembers);
        dto.setSurfboards(surfboards);
        return dto;
    }

    public List<TripDTO> getUserTrips(String userId) {
        List<Trip> trips = tripRepository.findByOwnerIdOrMemberId(userId);
        return trips.stream()
                .map(trip -> {
                    TripDTO dto = new TripDTO(trip, userId);
                    
                    // Merge members and pending invitations for each trip
                    List<TripMemberDTO> allMembers = new java.util.ArrayList<>();
                    
                    // Add accepted members
                    if (trip.getMembers() != null) {
                        List<TripMemberDTO> members = trip.getMembers().stream()
                                .map(TripMemberDTO::new)
                                .collect(Collectors.toList());
                        allMembers.addAll(members);
                    }
                    
                    // Add pending invitations
                    List<TripInvitation> invitations = tripInvitationRepository.findByTripId(trip.getId());
                    List<TripMemberDTO> pendingInvitations = invitations.stream()
                            .filter(inv -> "PENDING".equals(inv.getStatus()))
                            .map(TripMemberDTO::new)
                            .collect(Collectors.toList());
                    allMembers.addAll(pendingInvitations);
                    
                    dto.setMembers(allMembers);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void addSpot(String userId, String tripId, Long surfSpotId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        // Check if user is the owner
        if (!trip.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trip owner can add spots");
        }

        SurfSpot surfSpot = surfSpotRepository.findById(surfSpotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Surf spot not found"));

        // Check if spot is already in trip
        Optional<TripSpot> existing = tripSpotRepository.findByTripIdAndSurfSpotId(tripId, surfSpotId);
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spot already in trip");
        }

        TripSpot tripSpot = TripSpot.builder()
                .id(UUID.randomUUID().toString())
                .trip(trip)
                .surfSpot(surfSpot)
                .build();

        tripSpotRepository.save(tripSpot);
    }

    @Transactional
    public void removeSpot(String userId, String tripId, String tripSpotId) {
        TripSpot tripSpot = tripSpotRepository.findById(tripSpotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip spot not found"));

        // Check if user is the owner
        if (!tripSpot.getTrip().getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trip owner can remove spots");
        }

        tripSpotRepository.delete(tripSpot);
    }

    @Transactional
    public void addSurfboard(String userId, String tripId, String surfboardId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        // Check if user is the owner
        if (!trip.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trip owner can add surfboards");
        }

        Surfboard surfboard = surfboardRepository.findById(surfboardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Surfboard not found"));

        // Check if surfboard belongs to the user
        if (!surfboard.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only add your own surfboards");
        }

        // Check if surfboard is already in trip
        List<TripSurfboard> existing = tripSurfboardRepository.findByTripId(tripId);
        boolean alreadyExists = existing.stream()
                .anyMatch(ts -> ts.getSurfboard().getId().equals(surfboardId));
        if (alreadyExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Surfboard already in trip");
        }

        TripSurfboard tripSurfboard = TripSurfboard.builder()
                .id(UUID.randomUUID().toString())
                .trip(trip)
                .surfboard(surfboard)
                .build();

        tripSurfboardRepository.save(tripSurfboard);
    }

    @Transactional
    public void removeSurfboard(String userId, String tripId, String tripSurfboardId) {
        TripSurfboard tripSurfboard = tripSurfboardRepository.findById(tripSurfboardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip surfboard not found"));

        // Check if user is the owner
        if (!tripSurfboard.getTrip().getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trip owner can remove surfboards");
        }

        tripSurfboardRepository.delete(tripSurfboard);
    }

    @Transactional
    public void addMember(String userId, String tripId, AddTripMemberRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        // Check if user is the owner
        if (!trip.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trip owner can add members");
        }

        String email = request.getEmail();
        if (email == null || email.isEmpty()) {
            // Fallback to userId lookup for backward compatibility
            if (request.getUserId() != null && !request.getUserId().isEmpty()) {
                User memberUser = userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                email = memberUser.getEmail();
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either userId or email must be provided");
            }
        }

        // Check if there's already a pending invitation
        Optional<TripInvitation> existingInvite = tripInvitationRepository.findByTripIdAndEmail(tripId, email);
        if (existingInvite.isPresent() && existingInvite.get().getStatus().equals("PENDING")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An invitation has already been sent to this email");
        }

        // Try to find existing user
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            // User exists - add them directly and send notification
            User memberUser = existingUser.get();
            
            // Can't add owner as member
            if (trip.getOwner().getId().equals(memberUser.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner is already part of the trip");
            }
            
            // Check if user is already a member
            Optional<TripMember> existing = tripMemberRepository.findByTripIdAndUserId(tripId, memberUser.getId());
            if (existing.isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already a member");
            }

            TripMember tripMember = TripMember.builder()
                    .id(UUID.randomUUID().toString())
                    .trip(trip)
                    .user(memberUser)
                    .build();

            tripMemberRepository.save(tripMember);
            
            // Send notification email
            try {
                User inviter = userRepository.findById(userId).orElse(null);
                String inviterName = inviter != null ? inviter.getName() : "Someone";
                emailService.sendTripMemberAddedNotification(
                    memberUser.getEmail(),
                    memberUser.getName(),
                    inviterName,
                    trip.getTitle()
                );
            } catch (Exception e) {
                // Log but don't fail the request
                logger.warn("Failed to send notification email: {}", e.getMessage(), e);
            }
        } else {
            // User doesn't exist - create invitation and send invite email
            User inviter = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inviter not found"));
            
            String token = UUID.randomUUID().toString();
            TripInvitation invitation = TripInvitation.builder()
                    .id(UUID.randomUUID().toString())
                    .trip(trip)
                    .email(email)
                    .invitedBy(inviter)
                    .invitedAt(java.time.LocalDateTime.now())
                    .status("PENDING")
                    .token(token)
                    .build();

            tripInvitationRepository.save(invitation);
            
            // Send invitation email
            try {
                String inviterName = inviter.getName();
                emailService.sendTripInvitation(
                    email,
                    inviterName,
                    trip.getTitle(),
                    token
                );
            } catch (Exception e) {
                // Log but don't fail the request
                logger.warn("Failed to send invitation email: {}", e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void removeMember(String userId, String tripId, String memberUserId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        // Check if user is the owner
        if (!trip.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trip owner can remove members");
        }

        Optional<TripMember> tripMember = tripMemberRepository.findByTripIdAndUserId(tripId, memberUserId);
        if (tripMember.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in trip");
        }

        tripMemberRepository.delete(tripMember.get());
    }

    @Transactional
    public void cancelInvitation(String userId, String tripId, String invitationId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        // Check if user is the owner
        if (!trip.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trip owner can cancel invitations");
        }

        TripInvitation invitation = tripInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        // Verify invitation belongs to this trip
        if (!invitation.getTrip().getId().equals(tripId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation does not belong to this trip");
        }

        // Only allow canceling pending invitations
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only cancel pending invitations");
        }

        tripInvitationRepository.delete(invitation);
    }

    public String getUploadUrl(String userId, String tripId, UploadMediaRequest request, String mediaId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        // Check if user is owner or member
        boolean isOwner = trip.getOwner().getId().equals(userId);
        boolean isMember = trip.getMembers() != null && trip.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId));

        if (!isOwner && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to this trip");
        }

        // Validate media type
        String mediaType = request.getMediaType();
        if (mediaType == null || (!mediaType.equals("image") && !mediaType.equals("video"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media type must be 'image' or 'video'");
        }

        // Determine content type based on media type
        String contentType = "image".equals(mediaType) ? "image/jpeg" : "video/mp4";
        
        // Generate S3 key for the media file
        String s3Key = storageService.generateMediaKey(mediaId, mediaType, "trips/media");
        
        // Generate presigned URL for uploading to S3
        return storageService.generatePresignedUploadUrl(s3Key, contentType);
    }

    @Transactional
    public void recordMedia(String userId, String tripId, RecordMediaRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        // Check if user is owner or member
        boolean isOwner = trip.getOwner().getId().equals(userId);
        boolean isMember = trip.getMembers() != null && trip.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId));

        if (!isOwner && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to this trip");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        TripMedia tripMedia = TripMedia.builder()
                .id(request.getMediaId())
                .trip(trip)
                .owner(user)
                .url(request.getUrl())
                .mediaType(request.getMediaType())
                .build();

        tripMediaRepository.save(tripMedia);
    }

    @Transactional
    public void deleteMedia(String userId, String tripId, String mediaId) {
        TripMedia tripMedia = tripMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));

        // Check if user is owner of trip or owner of media
        boolean isTripOwner = tripMedia.getTrip().getOwner().getId().equals(userId);
        boolean isMediaOwner = tripMedia.getOwner().getId().equals(userId);

        if (!isTripOwner && !isMediaOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to delete this media");
        }

        tripMediaRepository.delete(tripMedia);
    }

    @Transactional
    public void processPendingInvitations(String email, String userId) {
        // Find all pending invitations for this email
        List<TripInvitation> pendingInvitations = tripInvitationRepository.findByEmail(email);
        
        for (TripInvitation invitation : pendingInvitations) {
            if ("PENDING".equals(invitation.getStatus())) {
                try {
                    // Add user to the trip
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    
                    // Check if already a member (in case of race condition)
                    Optional<TripMember> existing = tripMemberRepository.findByTripIdAndUserId(
                            invitation.getTrip().getId(), userId);
                    
                    TripMember tripMember;
                    if (existing.isEmpty()) {
                        tripMember = TripMember.builder()
                                .id(UUID.randomUUID().toString())
                                .trip(invitation.getTrip())
                                .user(user)
                                .build();
                        tripMemberRepository.save(tripMember);
                    } else {
                        tripMember = existing.get();
                    }
                    
                    // Mark invitation as accepted and link to TripMember (for audit trail)
                    invitation.setStatus("ACCEPTED");
                    invitation.setAcceptedAt(java.time.LocalDateTime.now());
                    invitation.setTripMember(tripMember);
                    tripInvitationRepository.save(invitation);
                } catch (Exception e) {
                    // Log error but continue processing other invitations
                    logger.warn("Failed to process invitation {}: {}", invitation.getId(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Deletes all trip-related data for a user.
     * Called explicitly during account deletion.
     * 
     * Handles:
     * - Trip invitations where user was invited (by email)
     * - Trip invitations where user was the inviter
     * - Trip memberships (where user is a member but not owner)
     * - Trips owned by the user
     * 
     * @param userId The ID of the user whose trip data should be deleted
     * @param userEmail The email of the user (for finding invitations by email)
     */
    @Transactional
    public void deleteAllUserTrips(String userId, String userEmail) {
        // Delete trip invitations where user was invited (by email)
        List<TripInvitation> invitationsByEmail = tripInvitationRepository.findByEmail(userEmail);
        if (!invitationsByEmail.isEmpty()) {
            tripInvitationRepository.deleteAll(invitationsByEmail);
        }

        // Delete trip invitations where user was the inviter
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found during trip deletion: " + userId));
        List<TripInvitation> invitationsByInviter = tripInvitationRepository.findByInvitedBy(user);
        if (!invitationsByInviter.isEmpty()) {
            tripInvitationRepository.deleteAll(invitationsByInviter);
        }

        // Delete trip memberships (where user is a member but not owner)
        List<TripMember> tripMemberships = tripMemberRepository.findByUserId(userId);
        if (!tripMemberships.isEmpty()) {
            tripMemberRepository.deleteAll(tripMemberships);
        }

        // Delete trips owned by the user
        List<Trip> userTrips = tripRepository.findByOwnerId(userId);
        for (Trip trip : userTrips) {
            // Delete related entities first to avoid foreign key constraint violations
            tripInvitationRepository.deleteAll(tripInvitationRepository.findByTripId(trip.getId()));
            tripMediaRepository.deleteAll(tripMediaRepository.findByTripIdOrderByUploadedAtDesc(trip.getId()));
            tripSpotRepository.deleteAll(tripSpotRepository.findByTripId(trip.getId()));
            tripSurfboardRepository.deleteAll(tripSurfboardRepository.findByTripId(trip.getId()));
            tripMemberRepository.deleteAll(tripMemberRepository.findByTripId(trip.getId()));
            
            tripRepository.delete(trip);
        }
    }
}

