package com.lovettj.surfspotsapi.response;

/**
 * Safe, user-facing error messages. Use these instead of exposing exception messages
 * or internal details (paths, stack traces, DB/API keys). Never concatenate
 * user input or exception.getMessage() into these when returning to the client.
 */
public final class ApiErrors {

    private static final String TRY_AGAIN_LATER = ". Please try again later.";

    private ApiErrors() {}

    /** Generic message when something goes wrong and we must not leak details. */
    public static final String SOMETHING_WENT_WRONG =
            "Something went wrong" + TRY_AGAIN_LATER + ". If the problem continues, contact support.";

    /** Media upload / storage is temporarily unavailable. */
    public static final String MEDIA_UPLOAD_UNAVAILABLE =
            "Media upload failed" + TRY_AGAIN_LATER;

    /**
     * Formats a safe user-facing error message from action and target, e.g. "Unable to create trip. Please try again later."
     * Use only fixed action/target strings (e.g. "create", "trip")—never user input. If target is null or empty,
     * returns "Unable to [action]."
     */
    public static String formatErrorMessage(String action, String target) {
        if (target == null || target.isEmpty()) {
            return "Unable to " + action + TRY_AGAIN_LATER;
        }
        return "Unable to " + action + " " + target + TRY_AGAIN_LATER;
    }
}
