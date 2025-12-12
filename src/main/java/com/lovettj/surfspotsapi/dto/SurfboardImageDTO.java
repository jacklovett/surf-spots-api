package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.SurfboardImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurfboardImageDTO {
    private String id;
    private String surfboardId;
    private String originalUrl;
    private String thumbUrl;
    private LocalDateTime createdAt;

    public SurfboardImageDTO(SurfboardImage image) {
        this.id = image.getId();
        this.surfboardId = image.getSurfboard().getId();
        this.originalUrl = image.getOriginalUrl();
        this.thumbUrl = image.getThumbUrl();
        this.createdAt = image.getCreatedAt();
    }
}



