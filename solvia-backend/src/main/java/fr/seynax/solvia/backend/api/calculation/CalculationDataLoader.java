package fr.seynax.solvia.backend.api.calculation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import fr.seynax.solvia.domain.calculation.NetWorthCalculator.CalculationData;
import fr.seynax.solvia.domain.model.Account;
import fr.seynax.solvia.domain.model.AccountBalanceSnapshot;
import fr.seynax.solvia.domain.model.Asset;
import fr.seynax.solvia.domain.model.CashFlow;
import fr.seynax.solvia.domain.model.FxRate;
import fr.seynax.solvia.domain.model.Position;
import fr.seynax.solvia.domain.model.PositionSnapshot;
import fr.seynax.solvia.infrastructure.persistence.account.AccountJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.account.AccountSnapshotJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.asset.AssetJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.flow.CashFlowJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.market.FxRateJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.position.PositionJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.position.PositionSnapshotJdbcRepository;

@Component
class CalculationDataLoader {

    private final AccountJdbcRepository accountRepository;
    private final AssetJdbcRepository assetRepository;
    private final PositionJdbcRepository positionRepository;
    private final AccountSnapshotJdbcRepository accountSnapshotRepository;
    private final PositionSnapshotJdbcRepository positionSnapshotRepository;
    private final CashFlowJdbcRepository cashFlowRepository;
    private final FxRateJdbcRepository fxRateRepository;

    CalculationDataLoader(
            AccountJdbcRepository accountRepository,
            AssetJdbcRepository assetRepository,
            PositionJdbcRepository positionRepository,
            AccountSnapshotJdbcRepository accountSnapshotRepository,
            PositionSnapshotJdbcRepository positionSnapshotRepository,
            CashFlowJdbcRepository cashFlowRepository,
            FxRateJdbcRepository fxRateRepository
    ) {
        this.accountRepository = accountRepository;
        this.assetRepository = assetRepository;
        this.positionRepository = positionRepository;
        this.accountSnapshotRepository = accountSnapshotRepository;
        this.positionSnapshotRepository = positionSnapshotRepository;
        this.cashFlowRepository = cashFlowRepository;
        this.fxRateRepository = fxRateRepository;
    }

    CalculationData load() {
        List<Account> accounts = accountRepository.findAll();
        List<Asset> assets = assetRepository.findAll();
        List<Position> positions = new ArrayList<>();
        List<AccountBalanceSnapshot> accountSnapshots = new ArrayList<>();
        List<PositionSnapshot> positionSnapshots = new ArrayList<>();
        List<CashFlow> cashFlows = new ArrayList<>();

        for (Account account : accounts) {
            positions.addAll(positionRepository.findByAccountId(account.id()));
            accountSnapshots.addAll(accountSnapshotRepository.findByAccountId(account.id()));
            cashFlows.addAll(cashFlowRepository.findByAccountId(account.id()));
        }

        for (Position position : positions) {
            positionSnapshots.addAll(positionSnapshotRepository.findByPositionId(position.id()));
        }

        List<FxRate> fxRates = fxRateRepository.findAll();
        return new CalculationData(accounts, assets, positions, accountSnapshots, positionSnapshots, cashFlows, fxRates);
    }
}
