package com.claims.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClaimRulesTest {

    @Test
    void approvesAnOrdinaryClaim() {
        assertEquals("APPROVED", ClaimRules.decide("policy.pdf", 2_500));
        assertTrue(ClaimRules.documentHealthy("policy.pdf"));
    }

    @Test
    void sendsMissingOrLargeClaimsToReview() {
        assertEquals("REVIEW", ClaimRules.decide("missing-page.pdf", 2_500));
        assertEquals("REVIEW", ClaimRules.decide("invoice.pdf", 1_000_001));
        assertEquals("APPROVED", ClaimRules.decide("invoice.pdf", 1_000_000));
    }

    @Test
    void flagsAnUnreadableDocument() {
        assertFalse(ClaimRules.documentHealthy("scan-corrupt.pdf"));
    }
}
