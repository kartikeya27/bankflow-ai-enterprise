package dev.portfolio.bankflow.config;
import dev.portfolio.bankflow.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class CoreConfiguration {
    @Bean CoreBankGateway coreGateway(@Value("${app.core.mode:mock}") String mode,
      @Value("${app.core.url:http://localhost:8090}") String url,
      @Value("${app.core.api-key:demo-core-key}") String key) {
        if("mock".equals(mode))return new MockCoreGateway();
        if("http".equals(mode))return new HttpCoreGateway(url,key);
        throw new IllegalArgumentException("CORE_MODE must be mock or http");
    }
}
