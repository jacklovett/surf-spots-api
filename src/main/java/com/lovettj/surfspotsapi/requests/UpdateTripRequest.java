package com.lovettj.surfspotsapi.requests;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateTripRequest {
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
}


