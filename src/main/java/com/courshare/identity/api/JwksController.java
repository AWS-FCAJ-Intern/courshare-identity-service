package com.courshare.identity.api;

import com.courshare.identity.infrastructure.JwtService;
import com.courshare.identity.infrastructure.RsaKeyHelper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class JwksController {

    private final JwtService jwtService;

    public JwksController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<Map<String, Object>>> getJwks() {
        Map<String, Object> jwk = RsaKeyHelper.toJwk(jwtService.getKeyId(), jwtService.getPublicKey());
        return Collections.singletonMap("keys", Collections.singletonList(jwk));
    }
}
