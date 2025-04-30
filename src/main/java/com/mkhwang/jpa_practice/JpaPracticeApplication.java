package com.mkhwang.jpa_practice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class JpaPracticeApplication {

  public static void main(String[] args) {
    SpringApplication.run(JpaPracticeApplication.class, args);
  }

}
