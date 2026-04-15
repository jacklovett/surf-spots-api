package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.SurfSessionMedia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurfSessionMediaDTO {
    private String id;
    private Long surfSessionId;
    private String originalUrl;
    private String thumbUrl;
    private String mediaType;
    private LocalDateTime createdAt;

    public SurfSessionMediaDTO(SurfSessionMedia media) {
        this.id = media.getId();
        this.surfSessionId = media.getSurfSession().getId();
        this.originalUrl = media.getOriginalUrl();
        this.thumbUrl = media.getThumbUrl();
        this.mediaType = media.getMediaType();
        this.createdAt = media.getCreatedAt();
    }
}
