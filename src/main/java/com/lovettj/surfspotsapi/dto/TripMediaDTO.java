package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.TripMedia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripMediaDTO {
    private String id;
    private String url;
    private String mediaType;
    private String ownerId;
    private String ownerName;
    private LocalDateTime uploadedAt;

    public TripMediaDTO(TripMedia tripMedia) {
        this.id = tripMedia.getId();
        this.url = tripMedia.getUrl();
        this.mediaType = tripMedia.getMediaType();
        this.ownerId = tripMedia.getOwner().getId();
        this.ownerName = tripMedia.getOwner().getName();
        this.uploadedAt = tripMedia.getUploadedAt();
    }
}