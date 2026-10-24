package dev.portfolio.bankflow.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.portfolio.bankflow.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class AppConfiguration {
    @Bean
    TextGenerator generator(@Value("${app.ai.mode:mock}") String mode,
            @Value("${app.ai.aws-region:us-east-1}") String region,
            @Value("${app.ai.bedrock-model-id:amazon.nova-lite-v1:0}") String model) {
        if ("bedrock".equals(mode)) return new BedrockTextGenerator(region,model);
        if ("mock".equals(mode)) return new MockTextGenerator();
        throw new IllegalArgumentException("AI_MODE must be mock or bedrock");
    }
    @Bean
    EmbeddingService embedding(@Value("${app.ai.embedding-mode:mock}") String mode,
            @Value("${app.ai.aws-region:us-east-1}") String region,
            @Value("${app.ai.embedding-model-id:amazon.titan-embed-text-v2:0}") String model,
            ObjectMapper mapper) {
        if ("bedrock".equals(mode)) return new BedrockEmbeddingService(region,model,mapper);
        if ("mock".equals(mode)) return new LocalEmbeddingService();
        throw new IllegalArgumentException("EMBEDDING_MODE must be mock or bedrock");
    }
}
