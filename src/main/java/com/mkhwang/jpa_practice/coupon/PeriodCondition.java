package com.mkhwang.jpa_practice.coupon;

import java.time.DayOfWeek;

public class PeriodCondition implements DiscountCondition {
  private DayOfWeek dayOfWeek;
  private LocalTime starttime;

  public boolean isSatisfiedBy(Screening screening) {
    return screening.getStartTime().getDayOfWeek().equals(this.dayOfWeek)
        && startTime.compareTo(screening.getStartTime()) <= 0;
  }
}
  