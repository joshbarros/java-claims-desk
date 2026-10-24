package com.claims.adjudication;

import com.claims.common.Channels;
import com.claims.common.ClaimRules;
import com.claims.common.ClaimSubmitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DecisionWorker {

    private static final Logger log = LoggerFactory.getLogger(DecisionWorker.class);

    private final ClaimDecisionRepository decisions;

    public DecisionWorker(ClaimDecisionRepository decisions) {
        this.decisions = decisions;
    }

    @KafkaListener(topics = Channels.SUBMITTED, groupId = "claims-adjudication")
    @Transactional
    public void onSubmitted(ClaimSubmitted event) {
        String decision = ClaimRules.decide(event.documentName(), event.amountCents());
        decisions.upsertDecision(event.claimId(), event.claimantName(), event.documentName(),
                event.amountCents(), decision);
        log.info("claim {} -> {}", event.claimId(), decision);
    }
}
