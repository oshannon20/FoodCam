package com.victu.foodatory.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.victu.foodatory.R;
import com.victu.foodatory.home.model.Meal;
import com.victu.foodatory.home.model.Time;
import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import java.util.List;


/**
 * 리사이클러뷰 어댑터 클래스이다.
 * @author Shang
 * @version 1.0, 작업 내용
 */

public class TimeAdapter extends ExpandableRecyclerViewAdapter<TimeViewHolder, MealViewHolder> {
  List<? extends ExpandableGroup> groups;


  public TimeAdapter(List<? extends ExpandableGroup> groups) {
    super(groups);
  }

  @Override
  public TimeViewHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_time, parent, false);
    return new TimeViewHolder(view);
  }

  @Override
  public MealViewHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_meal, parent, false);
    return new MealViewHolder(view);
  }

  @Override
  public void onBindChildViewHolder(MealViewHolder holder, int flatPosition,
                                    ExpandableGroup group, int childIndex) {

    final Meal meal = ((Time) group).getItems().get(childIndex);
    holder.setMealData(meal.getName(), meal.getCalorie() +"kcal");
  }

  @Override
  public void onBindGroupViewHolder(TimeViewHolder holder, int flatPosition,
                                    ExpandableGroup group) {

    holder.setMealImage(group);
  }

//  @Override
//  public int getItemCount() {
//    return (null != groups ? groups.size() : 0);
//  }


}
