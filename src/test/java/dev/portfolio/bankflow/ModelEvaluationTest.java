package dev.portfolio.bankflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.portfolio.bankflow.model.CaseInput;
import dev.portfolio.bankflow.service.CaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
/** Versioned offline risk-policy evaluation gate. Does not claim Bedrock quality evaluation. */
@SpringBootTest(properties={"app.ai.mode=mock","app.ai.embedding-mode=mock","app.core.mode=mock"})
class ModelEvaluationTest {
    @Autowired CaseService service;
    @Autowired ObjectMapper mapper;
    @Test void riskAndPolicyGoldensMustAllPass()throws Exception{
        var data=mapper.readTree(new ClassPathResource("evals/banking-cases.json").getInputStream());
        int passed=0;
        for(var row:data){
            var input=new CaseInput("EVAL-"+UUID.randomUUID().toString().substring(0,12),
                new BigDecimal(row.get("amount").asText()),"USD",row.get("newBeneficiary").asBoolean(),
                row.get("sanctionsHit").asBoolean(),row.get("accountMismatch").asBoolean(),
                "Synthetic evaluation fixture");
            var result=service.create(input);
            assertEquals(row.get("risk").asText(),result.riskLevel());
            assertEquals(row.get("status").asText(),result.status());
            assertTrue(result.policies().stream().anyMatch(p->p.id().equals(row.get("policy").asText())));
            passed++;
        }
        assertEquals(5,passed);
    }
}
