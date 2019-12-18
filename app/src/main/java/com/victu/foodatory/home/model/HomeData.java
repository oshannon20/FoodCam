package com.victu.foodatory.home.model;

import android.util.Log;

import com.victu.foodatory.R;

import java.util.Arrays;
import java.util.List;

/**
 *  클래스이다. arrayList가 return값
 * @author Shang
 * @version 1.0, 작업 내용
 */

public class HomeData {

  public static List<Time> makeHomeData(List<Meal> mealList, String imagePath) {
    return Arrays.asList(makeTime(mealList, imagePath));
  }


  public static Time makeTime(List<Meal> mealList, String imagePath) {
    return new Time("", mealList, imagePath);
  }


  public static List<Meal> makeMeal() {
    Meal queen = new Meal("피자", 1700);
    Meal styx = new Meal("피자", 1700);
    Meal reoSpeedwagon = new Meal("피자", 1700);
    Meal boston = new Meal("피자", 1700);

    return Arrays.asList(queen, styx, reoSpeedwagon, boston);
  }

}

