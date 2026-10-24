package dev.portfolio.bankflow.config;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component
@Profile("prod")
public class ProductionGuard implements ApplicationRunner {
    @Value("${app.core.mode}") String coreMode;
    @Value("${app.core.url}") String coreUrl;
    @Value("${app.ai.mode}") String modelMode;
    @Value("${app.ai.embedding-mode}") String embeddingMode;
    @Override public void run(ApplicationArguments args) {
        if(!"http".equals(coreMode) || !coreUrl.startsWith("https://"))
            throw new IllegalStateException("Production CORE_MODE must be http with an HTTPS approved sandbox endpoint");
        if(!"bedrock".equals(modelMode) || !"bedrock".equals(embeddingMode))
            throw new IllegalStateException("Production requires explicitly enabled Bedrock inference and embeddings");
    }
}
