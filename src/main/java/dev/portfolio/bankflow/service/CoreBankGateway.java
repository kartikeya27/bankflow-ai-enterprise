package dev.portfolio.bankflow.service;
import dev.portfolio.bankflow.model.CaseInput;
public interface CoreBankGateway {
    record Check(boolean accepted, String provider, String detail) {}
    Check verify(CaseInput input);
}
