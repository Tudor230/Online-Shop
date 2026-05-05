package org.endava.onlineshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class OnlineShopApplication {

  public static void main(String[] args) {
    SpringApplication.run(OnlineShopApplication.class, args);
  }
}
