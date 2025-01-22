package com.lovettj.surfspotsapi.requests;

import lombok.Data;

@Data
public class ChangePasswordRequest {

    private Long userId;
    private String currentPassword;
    private String newPassword;
}
