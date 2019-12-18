package com.victu.foodatory.home.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 섭취한 음식 모델클래스이다.
 * @author Shang
 * @version 1.0, 작업 내용
 */

public class Meal implements Parcelable {

  private String name;
  private int calorie;

  public Meal(String name, int calorie) {
    this.name = name;
    this.calorie = calorie;
  }

  protected Meal(Parcel in) {
    name = in.readString();
  }

  public String getName() {
    return name;
  }


  public String getCalorie() {
    return String.valueOf(calorie); // int-> String값으로 변경해주어야 한다.
  }



//  @Override
//  public boolean equals(Object o) {
//    if (this == o) return true;
//    if (!(o instanceof Meal)) return false;
//
//    Meal artist = (Meal) o;
//
//    if (isFavorite() != artist.isFavorite()) return false;
//    return getName() != null ? getName().equals(artist.getName()) : artist.getName() == null;
//
//  }

//  @Override
//  public int hashCode() {
//    int result = getName() != null ? getName().hashCode() : 0;
//    result = 31 * result + (isFavorite() ? 1 : 0);
//    return result;
//  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(name);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<Meal> CREATOR = new Creator<Meal>() {
    @Override
    public Meal createFromParcel(Parcel in) {
      return new Meal(in);
    }

    @Override
    public Meal[] newArray(int size) {
      return new Meal[size];
    }
  };
}

