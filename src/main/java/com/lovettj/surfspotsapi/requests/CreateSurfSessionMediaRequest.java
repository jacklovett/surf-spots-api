package com.lovettj.surfspotsapi.requests;

import lombok.Data;

@Data
public class CreateSurfSessionMediaRequest {
    private String mediaId;
    private String originalUrl;
    private String thumbUrl;
    private String mediaType;
}
