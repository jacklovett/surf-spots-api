package com.lovettj.surfspotsapi.email;

import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.util.UrlUtils;

/**
 * Shared values for transactional HTML email layout (masthead logo URL, canonical app base URL).
 */
public final class EmailLayoutVariables {

    private EmailLayoutVariables() {
    }

    /**
     * Normalizes a public base URL (trim, no trailing slash). If missing after normalization, throws
     * {@link IllegalStateException} with {@link ApiErrors#SOMETHING_WENT_WRONG} so the message is safe if it ever
     * surfaces in logs or default error handling; operators fix this by setting the app URL in deployment config.
     */
    public static String normalizeAppBaseUrl(String url) {
        String stripped = UrlUtils.stripTrailingSlashes(url);
        if (stripped == null || stripped.isBlank()) {
            throw new IllegalStateException(ApiErrors.SOMETHING_WENT_WRONG);
        }
        return stripped;
    }

    /**
     * @param logoUrlOverride optional full URL (e.g. API-served static asset in dev mail preview)
     * @param normalizedAppBaseUrl {@link #normalizeAppBaseUrl(String)} result for {@code app.url}
     */
    public static String resolveLogoImageUrl(String logoUrlOverride, String normalizedAppBaseUrl) {
        if (logoUrlOverride != null && !logoUrlOverride.isBlank()) {
            return logoUrlOverride.trim();
        }
        return normalizedAppBaseUrl + "/images/png/logo.png";
    }
}
