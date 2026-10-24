package dev.portfolio.bankflow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"app.ai.mode=mock","app.ai.embedding-mode=mock","app.core.mode=mock"})
@AutoConfigureMockMvc
class CaseApiSecurityTest {
    @Autowired MockMvc mvc;
    @Test void healthIsPublic()throws Exception{mvc.perform(get("/actuator/health")).andExpect(status().isOk());}
    @Test void anonymousCannotReview()throws Exception {
        mvc.perform(post("/api/cases/1/decision").contentType("application/json")
            .content("{\"decision\":\"APPROVE\",\"note\":\"justification\"}"))
            .andExpect(status().isUnauthorized());
    }
    @Test void invalidInputIsRejected()throws Exception {
        mvc.perform(post("/api/cases").contentType("application/json")
            .content("{\"reference\":\"bad\",\"amount\":-1,\"currency\":\"USD\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void auditRequiresReviewer()throws Exception {
        mvc.perform(get("/api/cases/4f102ac0-0fa6-4ae7-a36e-099be2498f4f/audit"))
            .andExpect(status().isUnauthorized());
    }
}
