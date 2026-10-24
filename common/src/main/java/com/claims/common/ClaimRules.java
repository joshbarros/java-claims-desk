package com.claims.common;

import java.util.Locale;

public final class ClaimRules {

    private ClaimRules() {
    }

    public static String decide(String documentName, long amountCents) {
        if (documentName != null && documentName.toLowerCase(Locale.ROOT).contains("missing")) {
            return "REVIEW";
        }
        if (amountCents > 1_000_000L) {
            return "REVIEW";
        }
        return "APPROVED";
    }

    public static boolean documentHealthy(String documentName) {
        return documentName == null || !documentName.toLowerCase(Locale.ROOT).contains("corrupt");
    }
}
