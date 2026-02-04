package com.lovettj.surfspotsapi.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private T data;
    private String message;
    private int status;
    private boolean success;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, "Success", 200, true);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(data, message, 200, true);
    }

    public static <T> ApiResponse<T> success(T data, int status) {
        return new ApiResponse<>(data, "Success", status, true);
    }

    public static <T> ApiResponse<T> success(T data, String message, int status) {
        return new ApiResponse<>(data, message, status, true);
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        String safeMessage = (message == null || message.isBlank()) ? "Request failed." : message;
        return new ApiResponse<>(null, safeMessage, status, false);
    }
} 