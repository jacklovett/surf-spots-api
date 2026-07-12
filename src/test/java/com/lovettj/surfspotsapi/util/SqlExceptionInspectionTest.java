package com.lovettj.surfspotsapi.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class SqlExceptionInspectionTest {

    @Test
    void isSurfSessionExternalSyncUniqueViolationShouldMatchConstraintNameInCauseChain() {
        String message =
                "duplicate key value violates unique constraint \""
                        + SqlExceptionInspection.UQ_SURF_SESSION_USER_PROVIDER_EXTERNAL
                        + "\"";
        SQLException root = new SQLException(message, "23505");
        Exception wrapper = new Exception("wrapped", root);

        assertTrue(SqlExceptionInspection.isSurfSessionExternalSyncUniqueViolation(wrapper));
    }

    @Test
    void isSurfSessionExternalSyncUniqueViolationShouldFallbackTo23505WhenConstraintTextMissing() {
        SQLException root = new SQLException("duplicate key", "23505");

        assertTrue(SqlExceptionInspection.isSurfSessionExternalSyncUniqueViolation(root));
    }

    @Test
    void isPostgresUniqueViolationShouldReturnTrueWhenSqlState23505InCauseChain() {
        SQLException root = new SQLException("duplicate key", "23505");
        Exception wrapper = new Exception("wrapped", root);

        assertTrue(SqlExceptionInspection.isPostgresUniqueViolation(wrapper));
    }

    @Test
    void isSurfSessionInProgressUniqueViolationShouldMatchConstraintNameInCauseChain() {
        String message =
                "duplicate key value violates unique constraint \""
                        + SqlExceptionInspection.UQ_SURF_SESSION_ONE_IN_PROGRESS_PER_USER
                        + "\"";
        SQLException root = new SQLException(message, "23505");
        Exception wrapper = new Exception("wrapped", root);

        assertTrue(SqlExceptionInspection.isSurfSessionInProgressUniqueViolation(wrapper));
    }

    @Test
    void isSurfSessionInProgressUniqueViolationShouldNotMatchGeneric23505WithoutConstraintName() {
        SQLException root = new SQLException("duplicate key", "23505");

        assertFalse(SqlExceptionInspection.isSurfSessionInProgressUniqueViolation(root));
    }
}
