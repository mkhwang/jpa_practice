package com.mkhwang.jpa_practice.product.repository;

import org.springframework.stereotype.Component;

@Component
public class ThreadTestRepository {

  public int someWorkAndReturnInteger() {
    Thread current = Thread.currentThread();
    System.out.printf("[2] Current thread: [%s] ID: %d, State: %s%n",
            current.getName(),
            current.getId(),
            current.getState());
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return 7;
  }
}
