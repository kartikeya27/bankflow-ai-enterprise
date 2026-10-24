package dev.portfolio.bankflow.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
@Component
public class AuditService {
    private final JdbcTemplate db;
    public AuditService(JdbcTemplate db){this.db=db;}
    private static String sha(String input) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch(Exception e){throw new IllegalStateException(e);}
    }
    /** Must be called inside the same transaction as case writes. */
    public void event(String caseId,String actor,String action,String detail) {
        List<String> previous=db.query("SELECT event_hash FROM audit_events WHERE case_id=?::uuid ORDER BY seq DESC LIMIT 1",
            (rs,n)->rs.getString(1),caseId);
        String prev=previous.isEmpty()?"GENESIS":previous.get(0);
        String ts=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS).toString();
        String hash=sha(caseId+"|"+actor+"|"+action+"|"+detail+"|"+ts+"|"+prev);
        db.update("""
          INSERT INTO audit_events(case_id,actor,action,detail,occurred_at,previous_hash,event_hash)
          VALUES (CAST(? AS uuid),?,?,?,CAST(? AS timestamptz),?,?)
          """,caseId,actor,action,detail,ts,prev,hash);
    }
    public List<Map<String,Object>> events(String caseId) {
        return db.queryForList("""
            SELECT seq,actor,action,detail,occurred_at,previous_hash,event_hash
            FROM audit_events WHERE case_id=CAST(? AS uuid) ORDER BY seq
            """,caseId);
    }
    private String timestamp(Object value) {
        if(value instanceof java.sql.Timestamp t) return t.toInstant().toString();
        if(value instanceof java.time.OffsetDateTime t) return t.toInstant().toString();
        throw new IllegalStateException("Unexpected JDBC timestamp: "+value.getClass());
    }
    public boolean verify(String caseId) {
        String previous="GENESIS";
        for(var e:events(caseId)) {
            String actual=sha(caseId+"|"+e.get("actor")+"|"+e.get("action")+"|"+e.get("detail")+
                "|"+timestamp(e.get("occurred_at"))+"|"+previous);
            if(!previous.equals(e.get("previous_hash")) || !actual.equals(e.get("event_hash")))return false;
            previous=(String)e.get("event_hash");
        }
        return true;
    }
}
