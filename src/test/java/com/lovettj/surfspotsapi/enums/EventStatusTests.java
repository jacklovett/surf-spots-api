package com.lovettj.surfspotsapi.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EventStatusTests {

    @Test
    void testfromContestPageLabelShouldReturnScheduledWhenLabelMissing() {
        assertEquals(EventStatus.SCHEDULED, EventStatus.fromContestPageLabel(null));
        assertEquals(EventStatus.SCHEDULED, EventStatus.fromContestPageLabel(""));
        assertEquals(EventStatus.SCHEDULED, EventStatus.fromContestPageLabel("   "));
    }

    @Test
    void testfromContestPageLabelShouldMapUpcomingLabels() {
        assertEquals(EventStatus.UPCOMING, EventStatus.fromContestPageLabel("Upcoming"));
        assertEquals(EventStatus.UPCOMING, EventStatus.fromContestPageLabel("tentative"));
        assertEquals(EventStatus.UPCOMING, EventStatus.fromContestPageLabel("STANDBY"));
    }

    @Test
    void testfromContestPageLabelShouldMapActiveLabels() {
        assertEquals(EventStatus.ACTIVE, EventStatus.fromContestPageLabel("Active"));
        assertEquals(EventStatus.ACTIVE, EventStatus.fromContestPageLabel("live"));
    }

    @Test
    void testfromContestPageLabelShouldMapCompletedLabel() {
        assertEquals(EventStatus.COMPLETED, EventStatus.fromContestPageLabel("Completed"));
    }

    @Test
    void testfromContestPageLabelShouldMapCancelledAndPostponedLabels() {
        assertEquals(EventStatus.CANCELLED, EventStatus.fromContestPageLabel("Cancelled"));
        assertEquals(EventStatus.CANCELLED, EventStatus.fromContestPageLabel("Canceled"));
        assertEquals(EventStatus.CANCELLED, EventStatus.fromContestPageLabel("Postponed"));
    }

    @Test
    void testfromContestPageLabelShouldDefaultUnknownLabelsToScheduled() {
        assertEquals(EventStatus.SCHEDULED, EventStatus.fromContestPageLabel("TBD"));
    }

    @Test
    void testExcludedFromSeasonActivityShouldIncludeCompletedAndCancelled() {
        assertEquals(
                java.util.List.of(EventStatus.COMPLETED, EventStatus.CANCELLED),
                EventStatus.excludedFromSeasonActivity());
    }
}
