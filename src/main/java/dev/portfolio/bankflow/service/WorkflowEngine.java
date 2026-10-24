package dev.portfolio.bankflow.service;
import dev.portfolio.bankflow.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;
/** Bounded branching state machine; the LLM cannot change state, risk or reviewer controls. */
@Component
public class WorkflowEngine {
    public record Result(String risk, String status, List<String> flags, List<BankPolicy> policies,
                         List<AgentStep> trace, String explanation, String provider, String recommendation){}
    private enum Stage { INTAKE, CORE, CLASSIFY, RETRIEVE, RISK, COMPLIANCE, EXPLAIN, REVIEW, END }
    private final CoreBankGateway core;
    private final PolicyKnowledgeBase kb;
    private final TextGenerator generator;
    private final int maxSteps;
    public WorkflowEngine(CoreBankGateway core, PolicyKnowledgeBase kb, TextGenerator generator,
            @Value("${app.workflow.max-steps:12}") int maxSteps){
        this.core=core;this.kb=kb;this.generator=generator;this.maxSteps=maxSteps;
    }
    public Result run(CaseInput input) {
        Stage stage=Stage.INTAKE;
        List<AgentStep> trace=new ArrayList<>();
        List<String> flags=new ArrayList<>();
        List<BankPolicy> policies=List.of();
        String risk="LOW",status="PENDING_REVIEW",explanation="",provider=generator.provider();
        for(int step=0;step<maxSteps;step++) {
            if(stage==Stage.END){
                String recommendation="ESCALATE — compliance hold";
                if(!input.sanctionsHit())recommendation="REVIEW — human decision required; no payment execution";
                return new Result(risk,status,List.copyOf(flags),policies,List.copyOf(trace),explanation,provider,recommendation);
            }
            switch(stage) {
                case INTAKE -> {
                    trace.add(new AgentStep("Intake", "Validated "+input.reference())); stage=Stage.CORE;
                }
                case CORE -> {
                    var check=core.verify(input);
                    if(!check.accepted()) throw new IllegalStateException("Core banking validation rejected case: "+check.detail());
                    trace.add(new AgentStep("Core Banking Integration", check.provider()+": "+check.detail()));stage=Stage.CLASSIFY;
                }
                case CLASSIFY -> {
                    if(input.amount().compareTo(new BigDecimal("10000"))>=0)flags.add("LARGE_AMOUNT");
                    if(input.newBeneficiary())flags.add("NEW_BENEFICIARY");
                    if(input.sanctionsHit())flags.add("SANCTIONS_HIT");
                    if(input.accountMismatch())flags.add("ACCOUNT_MISMATCH");
                    trace.add(new AgentStep("Classification", flags.toString()));stage=Stage.RETRIEVE;
                }
                case RETRIEVE -> {
                    policies=kb.forFlags(flags,input.narrative());
                    trace.add(new AgentStep("Vector RAG Retrieval",policies.stream().map(BankPolicy::id).toList().toString()));
                    stage=Stage.RISK;
                }
                case RISK -> {
                    risk=(input.sanctionsHit()||input.accountMismatch())?"HIGH":
                        (input.newBeneficiary()||flags.contains("LARGE_AMOUNT"))?"MEDIUM":"LOW";
                    trace.add(new AgentStep("Deterministic Risk",risk));
                    stage=input.sanctionsHit()?Stage.COMPLIANCE:Stage.EXPLAIN;
                }
                case COMPLIANCE -> {
                    status="ESCALATED_COMPLIANCE";
                    trace.add(new AgentStep("Compliance Supervisor","Sanctions hold; approval forbidden"));
                    stage=Stage.EXPLAIN;
                }
                case EXPLAIN -> {
                    try {
                        explanation=generator.generate(input,risk,flags,policies);
                        if(explanation==null||explanation.isBlank())throw new IllegalStateException("Empty model output");
                        var allowed=new HashSet<>(policies.stream().map(BankPolicy::id).toList());
                        var refs=java.util.regex.Pattern.compile("P-\\d{3}").matcher(explanation);
                        while(refs.find()) if(!allowed.contains(refs.group()))throw new IllegalStateException("Ungrounded policy citation");
                    } catch(Exception e) {
                        provider="SAFE_FALLBACK";
                        explanation="Model explanation unavailable or ungrounded. Enforced controls remain unchanged. Evidence: "+
                            policies.stream().map(BankPolicy::id).toList();
                    }
                    trace.add(new AgentStep("Grounded Explanation",provider));stage=Stage.REVIEW;
                }
                case REVIEW -> {
                    trace.add(new AgentStep("Human Review Router",status));stage=Stage.END;
                }
                default -> throw new IllegalStateException("Unhandled workflow stage");
            }
        }
        throw new IllegalStateException("Workflow exceeded max steps: "+maxSteps);
    }
}
