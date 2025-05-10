package com.mkhwang.jpa_practice.coupon;

public interface Specification<T> {
  boolean isSatisfiedBy(T candidate);

  default Specification<T> and(Specification<T> other) {
    return order -> this.isSatisfiedBy(order) && other.isSatisfiedBy(order);
  }

  default Specification<T> or(Specification<T> other) {
    return order -> this.isSatisfiedBy(order) || other.isSatisfiedBy(order);
  }
}