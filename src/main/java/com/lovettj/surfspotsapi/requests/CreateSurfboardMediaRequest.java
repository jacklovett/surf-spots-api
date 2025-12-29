package com.lovettj.surfspotsapi.requests;

import lombok.Data;

@Data
public class CreateSurfboardMediaRequest {
    private String originalUrl;
    private String thumbUrl;
    private String mediaType;
}

