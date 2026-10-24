package com.claims.adjudication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimDecisionRepository extends JpaRepository<ClaimDecision, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO claim_decisions (id, claimant_name, document_name, amount_cents, decision)
            VALUES (:id, :claimantName, :documentName, :amountCents, :decision)
            ON CONFLICT (id) DO UPDATE SET
              claimant_name = EXCLUDED.claimant_name,
              document_name = EXCLUDED.document_name,
              amount_cents = EXCLUDED.amount_cents,
              decision = EXCLUDED.decision
            """, nativeQuery = true)
    void upsertDecision(@Param("id") String id, @Param("claimantName") String claimantName,
            @Param("documentName") String documentName, @Param("amountCents") long amountCents,
            @Param("decision") String decision);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO claim_decisions (id, document_name, document_status, amount_cents)
            VALUES (:id, :documentName, 'CHECKED', 0)
            ON CONFLICT (id) DO UPDATE SET
              document_status = 'CHECKED',
              document_name = COALESCE(claim_decisions.document_name, EXCLUDED.document_name)
            """, nativeQuery = true)
    void upsertChecked(@Param("id") String id, @Param("documentName") String documentName);
}
