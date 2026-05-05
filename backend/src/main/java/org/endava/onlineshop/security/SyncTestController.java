package org.endava.onlineshop.security;

import java.util.Map;
import org.endava.onlineshop.model.entities.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SyncTestController {

  @GetMapping("/sync-test")
  public Map<String, String> syncTest(@AuthenticationPrincipal User user) {
    return Map.of("status", "synced", "subject", user.getId().toString());
  }
}
