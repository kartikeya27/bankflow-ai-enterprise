package dev.portfolio.bankflow;
import dev.portfolio.bankflow.model.*;
import dev.portfolio.bankflow.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"app.ai.mode=mock","app.ai.embedding-mode=mock","app.core.mode=mock"})
class CaseServiceTest {
    @Autowired CaseService cases;
    @Autowired AuditService audit;
    @Autowired PolicyKnowledgeBase knowledge;
    private CaseInput input(String amount,boolean newPayee,boolean sanctions,boolean mismatch) {
        return new CaseInput("TEST-"+UUID.randomUUID().toString().substring(0,12),
            new BigDecimal(amount),"USD",newPayee,sanctions,mismatch,"Synthetic compliance investigation");
    }
    @Test void persistedCaseAndVectorRetrieval() {
        var c=cases.create(input("15000",true,false,true));
        assertEquals("HIGH",c.riskLevel());
        assertTrue(c.policies().stream().anyMatch(p->p.id().equals("P-001")));
        assertTrue(c.policies().stream().anyMatch(p->p.id().equals("P-004")));
        assertEquals(c, cases.get(c.id()));
        assertTrue(c.trace().stream().anyMatch(s->s.agent().contains("Vector RAG")));
        assertTrue(audit.verify(c.id()));
    }
    @Test void sanctionsEscalationCannotBeApproved() {
        var c=cases.create(input("50",false,true,false));
        assertEquals("ESCALATED_COMPLIANCE",c.status());
        assertThrows(ResponseStatusException.class,()->cases.review(c.id(),new ReviewInput("APPROVE","Not allowed"),"reviewer"));
        assertEquals("ESCALATED_COMPLIANCE", cases.get(c.id()).status());
        assertTrue(audit.verify(c.id()));
    }
    @Test void rejectionPersistsWithAuditAndNoRepeatDecision() {
        var c=cases.create(input("99",false,false,false));
        var rejected=cases.review(c.id(),new ReviewInput("REJECT","Requires investigation"),"reviewer");
        assertEquals("REVIEW_REJECTED",cases.get(c.id()).status());
        assertEquals("reviewer",rejected.reviewedBy());
        assertThrows(ResponseStatusException.class,()->cases.review(c.id(),new ReviewInput("APPROVE","Cannot retry"),"reviewer"));
        assertTrue(audit.verify(c.id()));
        assertEquals(2,audit.events(c.id()).size());
    }
    @Test void duplicateReferenceIsIdempotent() {
        var source=input("125",false,false,false);
        var first=cases.create(source);
        var second=cases.create(source);
        assertEquals(first.id(),second.id());
        assertEquals(1,audit.events(first.id()).size());
        var changed=new CaseInput(source.reference(),new BigDecimal("130"),"USD",false,false,false,source.narrative());
        assertThrows(ResponseStatusException.class,()->cases.create(changed));
    }
}
