package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.SurfboardMedia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurfboardMediaDTO {
    private String id;
    private String surfboardId;
    private String originalUrl;
    private String thumbUrl;
    private String mediaType;
    private LocalDateTime createdAt;

    public SurfboardMediaDTO(SurfboardMedia media) {
        this.id = media.getId();
        this.surfboardId = media.getSurfboard().getId();
        this.originalUrl = media.getOriginalUrl();
        this.thumbUrl = media.getThumbUrl();
        this.mediaType = media.getMediaType();
        this.createdAt = media.getCreatedAt();
    }
}

