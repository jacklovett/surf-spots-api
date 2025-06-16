package com.lovettj.surfspotsapi.requests;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String userId;
    private String currentPassword;
    private String newPassword;
}
