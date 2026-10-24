package dev.portfolio.bankflow.model;

import java.time.Instant;
import java.util.List;

public record CaseResult(
    String id, CaseInput input, String riskLevel, String status,
    List<String> flags, List<BankPolicy> policies, List<AgentStep> trace,
    String recommendation, String explanation, String llmProvider,
    Instant createdAt, Instant reviewedAt, String reviewedBy, String reviewNote,
    long latencyMs
) {
    public CaseResult withReview(String nextStatus, String reviewer, String note) {
        return new CaseResult(id, input, riskLevel, nextStatus, flags, policies, trace,
            recommendation, explanation, llmProvider, createdAt, Instant.now(), reviewer, note, latencyMs);
    }
}
