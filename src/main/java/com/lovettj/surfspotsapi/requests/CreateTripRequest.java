package com.lovettj.surfspotsapi.requests;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateTripRequest {
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
}







