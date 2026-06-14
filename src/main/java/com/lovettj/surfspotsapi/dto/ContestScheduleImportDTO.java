package com.lovettj.surfspotsapi.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class ContestScheduleImportDTO {
    private Integer year;
    private List<ContestScheduleEventDTO> events;

    @Data
    public static class ContestScheduleEventDTO {
        private String name;
        private String locationName;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
    }
}
