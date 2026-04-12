package org.endava.onlineshop.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurityTestController {

    @GetMapping("/test/authenticated")
    String authenticated() {
        return "authenticated";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test/admin")
    String admin() {
        return "admin";
    }

    @PostMapping("/test/echo")
    String echo(@RequestBody String payload) {
        return payload;
    }
}
