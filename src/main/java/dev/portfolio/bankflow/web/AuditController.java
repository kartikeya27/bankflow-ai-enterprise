package dev.portfolio.bankflow.web;
import dev.portfolio.bankflow.service.AuditService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/cases/{id}/audit")
public class AuditController {
    private final AuditService audit;
    public AuditController(AuditService audit){this.audit=audit;}
    @GetMapping
    public Map<String,Object> audit(@PathVariable String id){
        // Events are restricted to reviewers in SecurityConfiguration.
        return Map.of("verified",audit.verify(id),"events",audit.events(id));
    }
}
