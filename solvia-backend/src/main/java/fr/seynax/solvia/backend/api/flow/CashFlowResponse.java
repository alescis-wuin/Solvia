package fr.seynax.solvia.backend.api.flow;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import fr.seynax.solvia.backend.api.money.MoneyResponse;
import fr.seynax.solvia.domain.model.CashFlow;
import fr.seynax.solvia.domain.model.CashFlowType;

public record CashFlowResponse(
        UUID id,
        UUID accountId,
        CashFlowType type,
        LocalDate valueDate,
        MoneyResponse amount,
        String label,
        Instant recordedAt
) {

    public static CashFlowResponse from(CashFlow cashFlow) {
        return new CashFlowResponse(
                cashFlow.id(),
                cashFlow.accountId(),
                cashFlow.type(),
                cashFlow.valueDate(),
                MoneyResponse.from(cashFlow.amount()),
                cashFlow.label(),
                cashFlow.recordedAt()
        );
    }
}
