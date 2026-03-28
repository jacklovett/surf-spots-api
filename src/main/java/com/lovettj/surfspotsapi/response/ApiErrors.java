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

    /** Account could not be found. */
    public static final String USER_NOT_FOUND = "User not found";

    /** Surf spot does not exist or could not be loaded. */
    public static final String SURF_SPOT_NOT_FOUND = "Surf spot not found";

    /** Another surf spot in the same region already uses this name (slug). */
    public static final String SURF_SPOT_NAME_EXISTS_IN_REGION =
            "A surf spot with this name already exists in this region";

    /** Profile must include a skill level before logging a surf session. */
    public static final String SKILL_LEVEL_REQUIRED_FOR_SESSION =
            "Add your skill level to your profile before logging a session.";

    /** Surfboard id was provided but that board was not found for the signed-in user. */
    public static final String SURFBOARD_NOT_FOUND_FOR_USER =
            "That surfboard was not found for your account.";

    /** Session summary endpoint requires a userId query parameter. */
    public static final String SESSION_SUMMARY_USER_ID_REQUIRED =
            "The userId query parameter is required.";

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
