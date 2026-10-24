package com.claims.common;

public record ClaimSubmitted(String claimId, String claimantName, long amountCents, String documentName) {
}
