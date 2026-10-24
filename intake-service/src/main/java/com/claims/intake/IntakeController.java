package com.claims.intake;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.claims.common.Channels;
import com.claims.common.ClaimSubmitted;
import com.claims.common.DocumentCommand;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class IntakeController {

    private final SubmissionRepository submissions;
    private final KafkaTemplate<String, Object> kafka;
    private final RabbitTemplate rabbit;

    public IntakeController(SubmissionRepository submissions, KafkaTemplate<String, Object> kafka,
            RabbitTemplate rabbit) {
        this.submissions = submissions;
        this.kafka = kafka;
        this.rabbit = rabbit;
    }

    @PostMapping("/claims")
    @Transactional
    public ResponseEntity<Submission> submit(@RequestBody SubmitClaimRequest request) {
        if (request == null || blank(request.claimantName()) || blank(request.documentName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "claimantName and documentName are required");
        }
        if (request.amountCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amountCents must be positive");
        }
        Submission submission = new Submission();
        submission.setId(UUID.randomUUID().toString());
        submission.setClaimantName(request.claimantName().trim());
        submission.setDocumentName(request.documentName().trim());
        submission.setAmountCents(request.amountCents());
        submissions.save(submission);
        ClaimSubmitted event = new ClaimSubmitted(
                submission.getId(), submission.getClaimantName(), submission.getAmountCents(),
                submission.getDocumentName());
        try {
            kafka.send(Channels.SUBMITTED, submission.getId(), event).get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "event log unavailable", ex);
        }
        rabbit.convertAndSend(Channels.DOCUMENTS, new DocumentCommand(submission.getId(), submission.getDocumentName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(submission);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record SubmitClaimRequest(String claimantName, long amountCents, String documentName) {
    }
}
