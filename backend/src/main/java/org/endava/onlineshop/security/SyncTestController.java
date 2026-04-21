package org.endava.onlineshop.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SyncTestController {

    @GetMapping("/sync-test")
    public Map<String, String> syncTest(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "status", "synced",
                "subject", jwt.getSubject()
        );
    }
}

