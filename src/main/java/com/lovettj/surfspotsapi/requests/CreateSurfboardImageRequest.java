package com.lovettj.surfspotsapi.requests;

import lombok.Data;

@Data
public class CreateSurfboardImageRequest {
    private String originalUrl;
    private String thumbUrl;
}