package fr.seynax.solvia.backend.api.snapshot;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import fr.seynax.solvia.backend.api.MockMvcSupport;
import fr.seynax.solvia.domain.model.Position;
import fr.seynax.solvia.domain.model.PositionSnapshot;
import fr.seynax.solvia.infrastructure.persistence.position.PositionJdbcRepository;
import fr.seynax.solvia.infrastructure.persistence.position.PositionSnapshotJdbcRepository;

class PositionSnapshotControllerTest {

    @Test
    void createsPositionSnapshot() throws Exception {
        PositionSnapshotJdbcRepository snapshotRepository = mock(PositionSnapshotJdbcRepository.class);
        PositionJdbcRepository positionRepository = mock(PositionJdbcRepository.class);
        UUID positionId = UUID.randomUUID();
        when(positionRepository.findById(positionId)).thenReturn(Optional.of(new Position(
                positionId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("3.5"),
                true,
                Instant.parse("2026-06-01T12:00:00Z")
        )));

        MockMvc mvc = MockMvcSupport.standaloneMvc(new PositionSnapshotController(snapshotRepository, positionRepository));

        mvc.perform(post("/api/position-snapshots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "positionId": "%s",
                                  "valueDate": "2026-06-01",
                                  "quantity": 3.5,
                                  "marketValue": {
                                    "amount": 1800.50,
                                    "currencyCode": "EUR"
                                  }
                                }
                                """.formatted(positionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.positionId").value(positionId.toString()))
                .andExpect(jsonPath("$.quantity").value(3.5))
                .andExpect(jsonPath("$.marketValue.amount").value(1800.5))
                .andExpect(jsonPath("$.marketValue.currencyCode").value("EUR"))
                .andExpect(jsonPath("$.confidence").value("OBSERVED"));

        ArgumentCaptor<PositionSnapshot> captor = ArgumentCaptor.forClass(PositionSnapshot.class);
        verify(snapshotRepository).save(captor.capture());
    }
}
