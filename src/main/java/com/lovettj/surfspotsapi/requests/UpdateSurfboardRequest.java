package com.lovettj.surfspotsapi.requests;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateSurfboardRequest {
    private String name;
    private String boardType;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal thickness;
    private BigDecimal volume;
    private String finSetup;
    private String description;
    private String modelUrl;
}



