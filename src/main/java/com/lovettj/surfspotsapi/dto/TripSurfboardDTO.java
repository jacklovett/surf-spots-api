package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.TripSurfboard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSurfboardDTO {
    private String id;
    private String surfboardId;
    private String surfboardName;
    private LocalDateTime addedAt;

    public TripSurfboardDTO(TripSurfboard tripSurfboard) {
        this.id = tripSurfboard.getId();
        this.surfboardId = tripSurfboard.getSurfboard().getId();
        this.surfboardName = tripSurfboard.getSurfboard().getName();
        this.addedAt = tripSurfboard.getAddedAt();
    }
}