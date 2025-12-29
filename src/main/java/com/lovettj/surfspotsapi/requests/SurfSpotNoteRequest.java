package com.lovettj.surfspotsapi.requests;

import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class SurfSpotNoteRequest {
    @Size(max = 10000, message = "Note text cannot exceed 10000 characters")
    private String noteText;

    private Tide preferredTide;
    
    @Size(max = 100)
    private String preferredSwellDirection;
    
    @Size(max = 100)
    private String preferredWind;
    
    @Size(max = 100)
    private String preferredSwellRange;
    
    private SkillLevel skillRequirement;
    
    private String userId; // Required for authentication/authorization
}

