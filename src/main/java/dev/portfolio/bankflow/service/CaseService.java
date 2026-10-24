package dev.portfolio.bankflow.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.portfolio.bankflow.model.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
@Service
public class CaseService {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final WorkflowEngine engine;
    private final AuditService audit;
    private final MeterRegistry meter;
    public CaseService(JdbcTemplate db,ObjectMapper json,WorkflowEngine engine,AuditService audit,MeterRegistry meter) {
        this.db=db;this.json=json;this.engine=engine;this.audit=audit;this.meter=meter;
    }
    private String encode(Object obj) {
        try{return json.writeValueAsString(obj);}catch(Exception e){throw new IllegalStateException(e);}
    }
    private CaseResult decode(String s) {
        try{return json.readValue(s,CaseResult.class);}catch(Exception e){throw new IllegalStateException(e);}
    }
    @Transactional
    public CaseResult create(CaseInput input) {
        // Serialize duplicate submissions for a given reference within one DB transaction.
        // This prevents concurrent duplicate submissions from racing on UNIQUE(reference).
        db.queryForList("SELECT pg_advisory_xact_lock(hashtext(?))", input.reference());
        // Idempotent intake: the same reference and payload returns the original case.
        var previous=db.query("SELECT input_json::text,case_json::text FROM bank_cases WHERE reference=?",
            (rs,n)->Map.entry(rs.getString(1),rs.getString(2)),input.reference());
        if(!previous.isEmpty()) {
            try {
                var stored=json.readValue(previous.get(0).getKey(),CaseInput.class);
                if(!stored.equals(input))throw new ResponseStatusException(HttpStatus.CONFLICT,"Reference reused with different payload");
            } catch(ResponseStatusException e){throw e;}
              catch(Exception e){throw new IllegalStateException(e);}
            return decode(previous.get(0).getValue());
        }
        long start=System.nanoTime();
        WorkflowEngine.Result run;
        try {run=engine.run(input);}
        catch (IllegalStateException e) {
            // No case is committed when a required external integration fails.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Investigation blocked: "+e.getMessage(),e);
        }
        String id=UUID.randomUUID().toString();
        long ms=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);
        var result=new CaseResult(id,input,run.risk(),run.status(),run.flags(),run.policies(),run.trace(),
            run.recommendation(),run.explanation(),run.provider(),Instant.now(),null,null,null,ms);
        db.update("""
            INSERT INTO bank_cases(id,reference,input_json,case_json,status,created_at)
            VALUES (CAST(? AS uuid),?,CAST(? AS jsonb),CAST(? AS jsonb),?,CAST(? AS timestamptz))
            """,id,input.reference(),encode(input),encode(result),result.status(),result.createdAt().toString());
        audit.event(id,"system","CASE_CREATED","risk="+result.riskLevel()+";state="+result.status());
        meter.counter("bankflow_cases_created_total", "risk",result.riskLevel()).increment();
        return result;
    }
    public CaseResult get(String id) {
        List<String> found=db.query("SELECT case_json::text FROM bank_cases WHERE id=CAST(? AS uuid)",
            (rs,n)->rs.getString(1),validUuid(id));
        if(found.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Case not found");
        return decode(found.get(0));
    }
    private String validUuid(String id) {
        try{return UUID.fromString(id).toString();}
        catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid case ID");}
    }
    public List<CaseResult> list() {
        return db.query("SELECT case_json::text FROM bank_cases ORDER BY created_at DESC LIMIT 100",
            (rs,n)->decode(rs.getString(1)));
    }
    @Transactional
    public CaseResult review(String id,ReviewInput input,String reviewer) {
        var rows=db.query("SELECT case_json::text FROM bank_cases WHERE id=CAST(? AS uuid) FOR UPDATE",
            (rs,n)->rs.getString(1),validUuid(id));
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Case not found");
        CaseResult previous=decode(rows.get(0));
        if(!Set.of("PENDING_REVIEW","ESCALATED_COMPLIANCE").contains(previous.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Case already reviewed");
        if(previous.input().sanctionsHit()&&input.decision().equals("APPROVE"))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Sanctions hold: approval prohibited");
        String next=input.decision().equals("APPROVE")?"REVIEW_APPROVED":"REVIEW_REJECTED";
        var updated=previous.withReview(next,reviewer,input.note());
        db.update("UPDATE bank_cases SET case_json=CAST(? AS jsonb),status=?,updated_at=now() WHERE id=CAST(? AS uuid)",
            encode(updated),next,id);
        audit.event(id,reviewer,"REVIEW_"+input.decision(),input.note());
        meter.counter("bankflow_reviews_total","decision",input.decision()).increment();
        return updated;
    }
    public Map<String,Object> metrics(){
        Long processed=db.queryForObject("SELECT COUNT(*) FROM bank_cases",Long.class);
        Long reviewed=db.queryForObject("SELECT COUNT(*) FROM bank_cases WHERE status LIKE 'REVIEW_%'",Long.class);
        Long open=db.queryForObject("SELECT COUNT(*) FROM bank_cases WHERE status IN ('PENDING_REVIEW','ESCALATED_COMPLIANCE')",Long.class);
        return Map.of("processed",processed,"reviewed",reviewed,"open",open,"model",engine.getClass().getSimpleName());
    }
}
