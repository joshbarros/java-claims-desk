package com.claims.adjudication;

import com.claims.common.Channels;
import com.claims.common.ClaimRules;
import com.claims.common.DocumentCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DocumentWorker {

    private static final Logger log = LoggerFactory.getLogger(DocumentWorker.class);

    private final ClaimDecisionRepository decisions;

    public DocumentWorker(ClaimDecisionRepository decisions) {
        this.decisions = decisions;
    }

    @RabbitListener(queues = Channels.DOCUMENTS)
    @Transactional
    public void onDocument(DocumentCommand command) {
        if (!ClaimRules.documentHealthy(command.documentName())) {
            log.info("unreadable document for {}", command.claimId());
            throw new AmqpRejectAndDontRequeueException("unreadable document");
        }
        decisions.upsertChecked(command.claimId(), command.documentName());
    }
}
