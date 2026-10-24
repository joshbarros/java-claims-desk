package com.claims.adjudication;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ClaimController {

    private final ClaimDecisionRepository decisions;

    public ClaimController(ClaimDecisionRepository decisions) {
        this.decisions = decisions;
    }

    @GetMapping("/claims/{id}")
    public ClaimDecision claim(@PathVariable String id) {
        return decisions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "claim not found"));
    }
}
