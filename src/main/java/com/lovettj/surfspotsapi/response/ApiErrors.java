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

    /** Neither session date nor wearable/partner start instant was provided. */
    public static final String SESSION_DATE_OR_START_INSTANT_REQUIRED =
            "Provide a session date, or start/end instants from a wearable or partner.";

    /** Client sent an end time without a start time. */
    public static final String SESSION_END_TIME_REQUIRES_START =
            "Pick a start time first, then an end time.";

    /** End time is not after start time on the session day. */
    public static final String SESSION_END_BEFORE_START =
            "End time needs to be later than start time.";

    /** Derived span from start/end exceeds the maximum allowed window. */
    public static final String SESSION_DURATION_MINUTES_INVALID =
            "That session window is too long (maximum 24 hours).";

    /** Surfboard id was provided but that board was not found for the signed-in user. */
    public static final String SURFBOARD_NOT_FOUND_FOR_USER =
            "That surfboard was not found for your account.";

    /**
     * Sync payloads must send {@link com.lovettj.surfspotsapi.enums.ExternalSessionProvider} and provider-local
     * {@code externalSessionId} together; omit both for sessions logged only in the app.
     */
    public static final String EXTERNAL_SESSION_SYNC_PAIR_REQUIRED =
            "Provide both externalSessionProvider and externalSessionId for synced sessions, or omit both for logs entered only in the app.";

    /** Same user already stored this integration's external session id (replay or concurrent sync). */
    public static final String SURF_SESSION_ALREADY_SYNCED =
            "This session was already imported from that integration for this external id.";

    /** Session summary endpoint requires a userId query parameter. */
    public static final String SESSION_SUMMARY_USER_ID_REQUIRED =
            "The userId query parameter is required.";

    /** Surf session id does not exist. */
    public static final String SURF_SESSION_NOT_FOUND = "Surf session not found";

    /** Media id does not exist. */
    public static final String MEDIA_NOT_FOUND = "Media not found";

    /** Presigned upload / add-media: mediaType must be image or video. */
    public static final String MEDIA_TYPE_MUST_BE_IMAGE_OR_VIDEO =
            "Media type must be 'image' or 'video'";

    /** Signed-in user does not own this surf session; cannot add or upload media. */
    public static final String SURF_SESSION_MEDIA_ADD_FORBIDDEN =
            "You don't have permission to add media to this session";

    /** Signed-in user does not own this media; cannot delete. */
    public static final String MEDIA_DELETE_FORBIDDEN =
            "You don't have permission to delete this media";

    /** Auth: generic login failure. Used for BOTH unknown email AND wrong password to prevent account enumeration. */
    public static final String INVALID_CREDENTIALS =
            "That email and password didn't match. Try again or use Forgot password.";

    /** Auth: generic forgot-password response. Identical for existing and non-existing emails. */
    public static final String FORGOT_PASSWORD_ACCEPTED =
            "If an account with that email exists, a password reset link has been sent.";

    /** Auth: generic reset-password failure. Used for invalid, expired, or unknown reset tokens. */
    public static final String RESET_TOKEN_INVALID_OR_EXPIRED =
            "This reset link is invalid or has expired. Request a new one.";

    /** Auth: password does not meet policy. Text mirrors the NIST SP 800-63B minimum we enforce. */
    public static final String PASSWORD_POLICY_VIOLATION =
            "Password must be between 8 and 128 characters.";

    /** Auth: too many attempts from this caller. */
    public static final String TOO_MANY_ATTEMPTS =
            "Too many attempts. Please try again later.";

    /** Optional copy for clients that remind users to confirm their inbox. */
    public static final String EMAIL_NOT_VERIFIED =
            "This account's email is not verified yet. Check your inbox for the link we sent.";

    /**
     * Sensitive action (for example adding trip members / sending invitations) blocked until the inbox is verified.
     */
    public static final String EMAIL_VERIFICATION_REQUIRED =
            "Verify your email before doing this. Check your inbox for the link we sent.";

    /** Account delete: signed-in user may only delete their own account. */
    public static final String ACCOUNT_DELETE_NOT_PERMITTED =
            "You can only delete your own account.";

    /** Auth: email verification token unknown, expired, or already used. */
    public static final String VERIFY_EMAIL_TOKEN_INVALID_OR_EXPIRED =
            "This verification link is invalid or has expired. Use the latest message from your inbox, or contact support if it keeps failing.";

    /**
     * Auth: resend verification response. Same shape as forgot-password so callers cannot tell
     * whether the email exists or is already verified.
     */
    public static final String RESEND_VERIFICATION_ACCEPTED =
            "If that account exists and still needs verification, we sent a new email.";

    /** Auth: request blocked because its Origin / Referer is not on the allowlist (CSRF defence). */
    public static final String INVALID_ORIGIN =
            "Request blocked. Please reload the page and try again.";

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
