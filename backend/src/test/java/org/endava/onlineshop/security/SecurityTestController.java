package org.endava.onlineshop.security;

import org.endava.onlineshop.model.entities.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

  @GetMapping("/test/current-user-id")
  String currentUserId(@AuthenticationPrincipal User user) {
    return user.getId().toString();
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
