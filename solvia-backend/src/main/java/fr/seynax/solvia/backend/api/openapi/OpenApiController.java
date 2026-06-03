package fr.seynax.solvia.backend.api.openapi;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenApiController {

    @GetMapping(value = "/api/openapi.yaml", produces = "application/yaml")
    public ResponseEntity<Resource> openApi() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/yaml"))
                .body(new ClassPathResource("openapi/solvia-api.yaml"));
    }
}
