package com.lovettj.surfspotsapi.util;

import java.sql.SQLException;

/**
 * Traverses exception causes for JDBC/SQL state codes (used after persistence failures).
 * Production DB target is PostgreSQL ({@link #isPostgresUniqueViolation}); constraint matching avoids mis-mapping
 * unrelated unique violations when multiple indexes exist on the same insert.
 */
public final class SqlExceptionInspection {

    /** Flyway {@code V34}: partial unique index on external sync idempotency key. */
    public static final String UQ_SURF_SESSION_USER_PROVIDER_EXTERNAL = "uq_surf_session_user_provider_external";

    private SqlExceptionInspection() {}

    /**
     * PostgreSQL {@code SQLSTATE 23505} ({@code unique_violation}), including wrapped causes.
     */
    public static boolean isPostgresUniqueViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Unique violation on {@link #UQ_SURF_SESSION_USER_PROVIDER_EXTERNAL} (preferred), or {@code 23505} when the
     * constraint name does not appear in messages (simplified stacks / test doubles).
     */
    public static boolean isSurfSessionExternalSyncUniqueViolation(Throwable throwable) {
        if (constraintNameAppearsInCauseMessages(throwable, UQ_SURF_SESSION_USER_PROVIDER_EXTERNAL)) {
            return true;
        }
        return isPostgresUniqueViolation(throwable);
    }

    private static boolean constraintNameAppearsInCauseMessages(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
