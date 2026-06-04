package fr.seynax.solvia.backend.api.openapi;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import fr.seynax.solvia.backend.api.MockMvcSupport;

class OpenApiControllerTest {

    @Test
    void returnsOpenApiContract() throws Exception {
        MockMvc mvc = MockMvcSupport.standaloneMvc(new OpenApiController());

        mvc.perform(get("/api/openapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi: 3.1.0")))
                .andExpect(content().string(containsString("/api/accounts")))
                .andExpect(content().string(containsString("/api/cash-flows")))
                .andExpect(content().string(containsString("/api/readiness")))
                .andExpect(content().string(containsString("ReadinessResponse")))
                .andExpect(content().string(containsString("databaseStatus")))
                .andExpect(content().string(containsString("backendStatus")));
    }
}
