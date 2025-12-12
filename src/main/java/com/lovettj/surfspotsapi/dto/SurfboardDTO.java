package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.Surfboard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurfboardDTO {
    private String id;
    private String userId;
    private String name;
    private String boardType;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal thickness;
    private BigDecimal volume;
    private String finSetup;
    private String description;
    private String modelUrl;
    private List<SurfboardImageDTO> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SurfboardDTO(Surfboard surfboard) {
        this.id = surfboard.getId();
        this.userId = surfboard.getUser().getId();
        this.name = surfboard.getName();
        this.boardType = formatBoardType(surfboard.getBoardType());
        this.length = surfboard.getLength();
        this.width = surfboard.getWidth();
        this.thickness = surfboard.getThickness();
        this.volume = surfboard.getVolume();
        this.finSetup = formatFinSetup(surfboard.getFinSetup());
        this.description = surfboard.getDescription();
        this.modelUrl = surfboard.getModelUrl();
        this.createdAt = surfboard.getCreatedAt();
        this.updatedAt = surfboard.getUpdatedAt();

        if (surfboard.getImages() != null) {
            this.images = surfboard.getImages().stream()
                    .map(SurfboardImageDTO::new)
                    .collect(Collectors.toList());
        }
    }

    private String formatBoardType(String boardType) {
        if (boardType == null || boardType.isEmpty()) {
            return null;
        }
        String lower = boardType.toLowerCase();
        return switch (lower) {
            case "shortboard" -> "Shortboard";
            case "longboard" -> "Longboard";
            case "fish" -> "Fish";
            case "mid-length" -> "Mid-Length";
            case "funboard" -> "Funboard";
            case "gun" -> "Gun";
            case "hybrid" -> "Hybrid";
            case "soft-top" -> "Soft-Top";
            case "other" -> "Other";
            default -> boardType; // Return original if not recognized
        };
    }

    private String formatFinSetup(String finSetup) {
        if (finSetup == null || finSetup.isEmpty()) {
            return null;
        }
        String lower = finSetup.toLowerCase();
        return switch (lower) {
            case "single" -> "Single";
            case "twin" -> "Twin";
            case "thruster" -> "Thruster";
            case "quad" -> "Quad";
            case "5-fin" -> "5-Fin";
            case "other" -> "Other";
            default -> finSetup; // Return original if not recognized
        };
    }
}



