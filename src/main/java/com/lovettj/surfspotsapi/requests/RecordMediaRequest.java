package com.lovettj.surfspotsapi.requests;

import lombok.Data;

@Data
public class RecordMediaRequest {
    private String mediaId;
    private String url;
    private String mediaType;
}


