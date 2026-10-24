package dev.portfolio.bankflow.service;
import dev.portfolio.bankflow.model.BankPolicy;
import dev.portfolio.bankflow.model.CaseInput;
import java.util.List;
public interface TextGenerator {
    String generate(CaseInput input, String riskLevel, List<String> flags, List<BankPolicy> policies);
    String provider();
}
