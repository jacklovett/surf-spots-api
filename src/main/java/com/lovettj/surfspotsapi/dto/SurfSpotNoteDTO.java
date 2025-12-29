package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.SurfSpotNote;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurfSpotNoteDTO {
    private Long id;
    private String noteText;
    private Tide preferredTide;
    private String preferredSwellDirection;
    private String preferredWind;
    private String preferredSwellRange;
    private SkillLevel skillRequirement;
    private Long surfSpotId;

    public static SurfSpotNoteDTO fromEntity(SurfSpotNote note) {
        if (note == null) {
            return null;
        }
        return SurfSpotNoteDTO.builder()
                .id(note.getId())
                .noteText(note.getNoteText())
                .preferredTide(note.getPreferredTide())
                .preferredSwellDirection(note.getPreferredSwellDirection())
                .preferredWind(note.getPreferredWind())
                .preferredSwellRange(note.getPreferredSwellRange())
                .skillRequirement(note.getSkillRequirement())
                .surfSpotId(note.getSurfSpot() != null ? note.getSurfSpot().getId() : null)
                .build();
    }
}
