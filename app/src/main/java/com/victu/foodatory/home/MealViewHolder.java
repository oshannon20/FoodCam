package com.victu.foodatory.home;

import android.view.View;
import android.widget.TextView;

import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder;
import com.victu.foodatory.R;


/**
 * 음식 리사이클러뷰홀더 클래스이다.
 * @author Shang
 * @version 1.0, 작업 내용
 */


public class MealViewHolder extends ChildViewHolder {

  private TextView meal_name, meal_cal;

  public MealViewHolder(View itemView) {
    super(itemView);
    meal_name =  itemView.findViewById(R.id.list_item_meal_name);
    meal_cal =  itemView.findViewById(R.id.list_item_meal_calorie);

  }

  public void setMealData(String name, String calroie) {
    meal_name.setText(name);
    meal_cal.setText(calroie);
  }
}
