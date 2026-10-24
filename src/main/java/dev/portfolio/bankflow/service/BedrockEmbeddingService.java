package dev.portfolio.bankflow.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import java.util.Map;
public class BedrockEmbeddingService implements EmbeddingService, AutoCloseable {
    private final BedrockRuntimeClient client;
    private final ObjectMapper mapper;
    private final String model;
    public BedrockEmbeddingService(String region, String model, ObjectMapper mapper) {
        this.client = BedrockRuntimeClient.builder().region(Region.of(region)).build();
        this.model=model; this.mapper=mapper;
    }
    @Override public String provider() { return "BEDROCK:"+model; }
    @Override public double[] embed(String text) {
        try {
            String request = mapper.writeValueAsString(Map.of("inputText", text, "dimensions",1024,"normalize",true));
            var response = client.invokeModel(r -> r.modelId(model).contentType("application/json")
                .accept("application/json").body(SdkBytes.fromUtf8String(request)));
            var values = mapper.readTree(response.body().asUtf8String()).path("embedding");
            if(!values.isArray() || values.size()!=1024) throw new IllegalStateException("Unexpected embedding dimension");
            double[] result=new double[1024];
            for(int i=0;i<result.length;i++)result[i]=values.get(i).asDouble();
            return result;
        } catch(Exception e){throw new IllegalStateException("Bedrock embedding failed",e);}
    }
    @Override public void close(){client.close();}
}
