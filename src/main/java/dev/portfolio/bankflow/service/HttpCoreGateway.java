package dev.portfolio.bankflow.service;
import dev.portfolio.bankflow.model.CaseInput;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
public class HttpCoreGateway implements CoreBankGateway {
    private final RestClient client;
    private final String apiKey;
    public HttpCoreGateway(String baseUrl, String apiKey) {
        this.apiKey=apiKey;
        HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var factory=new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(3));
        client=RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
    @Override public Check verify(CaseInput input) {
        for(int i=0;i<2;i++) {
            try {
                // Read-only sandbox validation; never calls money-moving APIs.
                Check result=client.post().uri("/sandbox/payments/validate")
                    .header("X-Demo-Api-Key",apiKey)
                    .header("Idempotency-Key",input.reference())
                    .body(Map.of("reference",input.reference(),"currency",input.currency(),
                        "amount",input.amount(),"accountMismatch",input.accountMismatch()))
                    .retrieve().body(Check.class);
                if(result==null) throw new IllegalStateException("Empty core response");
                return result;
            }catch(RestClientException ex){if(i==1)throw new IllegalStateException("Core gateway unavailable; investigation blocked",ex);}
        }
        throw new IllegalStateException("Core gateway unavailable");
    }
}
