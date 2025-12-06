package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.Trip;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripDTO {
    private String id;
    private String ownerId;
    private String ownerName;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TripSpotDTO> spots;
    private List<TripMemberDTO> members; // Includes both accepted members and pending invitations
    private List<TripMediaDTO> media;
    private Boolean isOwner;

    public TripDTO(Trip trip, String currentUserId) {
        this.id = trip.getId();
        this.ownerId = trip.getOwner().getId();
        this.ownerName = trip.getOwner().getName();
        this.title = trip.getTitle();
        this.description = trip.getDescription();
        this.startDate = trip.getStartDate();
        this.endDate = trip.getEndDate();
        this.createdAt = trip.getCreatedAt();
        this.updatedAt = trip.getUpdatedAt();
        this.isOwner = trip.getOwner().getId().equals(currentUserId);

        if (trip.getSpots() != null) {
            this.spots = trip.getSpots().stream()
                    .map(TripSpotDTO::new)
                    .collect(Collectors.toList());
        }

        if (trip.getMembers() != null) {
            this.members = trip.getMembers().stream()
                    .map(TripMemberDTO::new)
                    .collect(Collectors.toList());
        }

        if (trip.getMedia() != null) {
            this.media = trip.getMedia().stream()
                    .map(TripMediaDTO::new)
                    .collect(Collectors.toList());
        }
    }
}




