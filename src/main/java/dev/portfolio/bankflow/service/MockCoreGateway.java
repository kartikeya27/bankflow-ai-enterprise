package dev.portfolio.bankflow.service;
import dev.portfolio.bankflow.model.CaseInput;
public class MockCoreGateway implements CoreBankGateway {
    @Override public Check verify(CaseInput input) {
        return new Check(true,"MOCK_CORE","Synthetic reference checked; no real bank accessed");
    }
}
