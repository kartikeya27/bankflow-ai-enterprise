package dev.portfolio.bankflow.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.portfolio.bankflow.model.BankPolicy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import java.io.IOException;
import java.util.*;
@Component
@DependsOnDatabaseInitialization
public class PolicyKnowledgeBase {
    private final JdbcTemplate db;
    private final EmbeddingService embedding;
    private final List<BankPolicy> catalog;
    public PolicyKnowledgeBase(JdbcTemplate db, EmbeddingService embedding, ObjectMapper mapper) throws IOException {
        this.db=db;this.embedding=embedding;
        try(var is=new ClassPathResource("policies.json").getInputStream()){
            catalog=mapper.readValue(is,new TypeReference<List<BankPolicy>>(){});
        }
    }
    @PostConstruct
    public void indexPolicies() {
        for(BankPolicy p:catalog) {
            // Re-index if provider or source changes; the actual policy version is explicit.
            double[] vector=embedding.embed(p.title()+" "+p.body());
            db.update("""
                INSERT INTO policy_documents(id,version,title,body,embedding_provider,embedding)
                VALUES (?,1,?,?,?,CAST(? AS vector))
                ON CONFLICT(id) DO UPDATE SET title=excluded.title,body=excluded.body,
                embedding_provider=excluded.embedding_provider, embedding=excluded.embedding,
                updated_at=now(),version=CASE WHEN policy_documents.body IS DISTINCT FROM excluded.body
                    THEN policy_documents.version+1 ELSE policy_documents.version END
                """,p.id(),p.title(),p.body(),embedding.provider(),EmbeddingService.vectorLiteral(vector));
        }
    }
    public List<BankPolicy> all(){return List.copyOf(catalog);}
    public List<BankPolicy> forFlags(List<String> flags, String narrative){
        Set<String> ids=new LinkedHashSet<>();
        if(flags.contains("LARGE_AMOUNT"))ids.add("P-001");
        if(flags.contains("NEW_BENEFICIARY"))ids.add("P-002");
        if(flags.contains("SANCTIONS_HIT"))ids.add("P-003");
        if(flags.contains("ACCOUNT_MISMATCH"))ids.add("P-004");
        ids.add("P-005");
        // Semantic/vector retrieval is ADVISORY; mandatory controls remain pinned.
        // Avoid admitting irrelevant approximate matches in the case of poor local embeddings.
        if(narrative != null && !narrative.isBlank()) {
            String searchVector=EmbeddingService.vectorLiteral(embedding.embed(narrative));
            var top=db.query("""
                SELECT id, 1 - (embedding <=> CAST(? AS vector)) AS similarity
                FROM policy_documents WHERE embedding_provider=? AND embedding IS NOT NULL
                ORDER BY embedding <=> CAST(? AS vector) LIMIT 2
                """,(rs,n)->Map.entry(rs.getString("id"),rs.getDouble("similarity")),
                searchVector, embedding.provider(), searchVector);
            for(var match:top) if(match.getValue()>=0.40) ids.add(match.getKey());
        }
        return catalog.stream().filter(p->ids.contains(p.id())).toList();
    }
}
