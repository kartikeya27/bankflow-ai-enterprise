package dev.portfolio.bankflow.service;

import dev.portfolio.bankflow.model.BankPolicy;
import dev.portfolio.bankflow.model.CaseInput;
import java.util.List;

public class MockTextGenerator implements TextGenerator {
    @Override public String provider() { return "MOCK (no LLM call)"; }
    @Override public String generate(CaseInput input, String risk, List<String> flags, List<BankPolicy> policies) {
        String citations = String.join(", ", policies.stream().map(BankPolicy::id).toList());
        String control = flags.contains("SANCTIONS_HIT") ? "Hold for compliance escalation; approval is forbidden."
            : "Route to an authenticated reviewer; no money is moved.";
        return "Synthetic case " + input.reference() + " was assessed as " + risk
            + " risk using policy references " + citations + ". " + control;
    }
}
