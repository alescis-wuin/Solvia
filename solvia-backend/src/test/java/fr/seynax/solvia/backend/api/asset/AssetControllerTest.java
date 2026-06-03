package fr.seynax.solvia.backend.api.asset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import fr.seynax.solvia.backend.api.MockMvcSupport;
import fr.seynax.solvia.domain.model.Asset;
import fr.seynax.solvia.infrastructure.persistence.asset.AssetJdbcRepository;

class AssetControllerTest {

    @Test
    void createsAsset() throws Exception {
        AssetJdbcRepository repository = mock(AssetJdbcRepository.class);
        MockMvc mvc = MockMvcSupport.standaloneMvc(new AssetController(repository));

        mvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bitcoin",
                                  "type": "CRYPTO_ASSET",
                                  "currencyCode": "USD",
                                  "symbol": "BTC"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bitcoin"))
                .andExpect(jsonPath("$.type").value("CRYPTO_ASSET"))
                .andExpect(jsonPath("$.symbol").value("BTC"));

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(repository).save(captor.capture());
    }
}
