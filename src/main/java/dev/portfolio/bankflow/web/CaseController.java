package dev.portfolio.bankflow.web;

import dev.portfolio.bankflow.model.*;
import dev.portfolio.bankflow.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class CaseController {
    private final CaseService service;
    private final PolicyKnowledgeBase policies;
    public CaseController(CaseService service, PolicyKnowledgeBase policies) {
        this.service = service; this.policies = policies;
    }
    @PostMapping("/cases") @ResponseStatus(HttpStatus.CREATED)
    public CaseResult create(@Valid @RequestBody CaseInput input) { return service.create(input); }
    @GetMapping("/cases") public List<CaseResult> list() { return service.list(); }
    @GetMapping("/cases/{id}") public CaseResult get(@PathVariable String id) { return service.get(id); }
    @PostMapping("/cases/{id}/decision")
    public CaseResult decide(@PathVariable String id, @Valid @RequestBody ReviewInput review, Authentication auth) {
        return service.review(id, review, auth.getName());
    }
    @GetMapping("/policies") public List<BankPolicy> policies() { return policies.all(); }
    @GetMapping("/metrics") public Map<String, Object> metrics() { return service.metrics(); }
}
