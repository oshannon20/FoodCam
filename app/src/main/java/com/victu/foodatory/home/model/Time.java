package com.victu.foodatory.home.model;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import java.util.List;

/**
 * 시간(아침, 점심, 저녁, 간식) 모델클래스이다.
 * @author Shang
 * @version 1.0, 작업 내용
 */

public class Time extends ExpandableGroup<Meal> {

  private String iconResId;

  public Time(String title, List<Meal> items, String iconResId) {
    super(title, items);
    this.iconResId = iconResId;
  }

  public String getIconResId() {
    return iconResId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Time)) return false;

    Time time = (Time) o;

    return getIconResId() == time.getIconResId();

  }


//
//  private int iconResId;
//
//  public Time(String title, List<Meal> items, int iconResId) {
//    super(title, items);
//    this.iconResId = iconResId;
//  }
//
//  public int getIconResId() {
//    return iconResId;
//  }
//
//  @Override
//  public boolean equals(Object o) {
//    if (this == o) return true;
//    if (!(o instanceof Time)) return false;
//
//    Time time = (Time) o;
//
//    return getIconResId() == time.getIconResId();
//
//  }
//
//  @Override
//  public int hashCode() {
//    return getIconResId();
//  }
//
//

}

