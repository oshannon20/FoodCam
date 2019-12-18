package com.victu.foodatory.home;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.victu.foodatory.R;
import com.victu.foodatory.home.model.Time;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder;

import static android.view.animation.Animation.RELATIVE_TO_SELF;

/**
 * 시간 리사이클러뷰홀더 클래스이다.
 * @author Shang
 * @version 1.0, 작업 내용
 */

public class TimeViewHolder extends GroupViewHolder {

  private TextView timeName;
  private ImageView arrow;
  private ImageView photo;

  public TimeViewHolder(View itemView) {
    super(itemView);
//    timeName =  itemView.findViewById(R.id.list_item_time_name);
    arrow =  itemView.findViewById(R.id.list_item_arrow);
    photo =  itemView.findViewById(R.id.list_item_time_photo);
  }

  public void setMealImage(ExpandableGroup mealImage) {
    if (mealImage instanceof Time) {
  //    timeName.setText(genre.getTitle());
      RequestOptions requestOptions = new RequestOptions();
      requestOptions = requestOptions.transform(new CenterCrop(), new RoundedCorners(30));


      //TODO: 갤러리에서 가져온 사진도 정방형으로 설정한 다음 RequestOptions.bitmapTransform(new RoundedCorners(30)) 으로 변경
      String imagePath = ((Time) mealImage).getIconResId();

      // 이미 한장인 경우
      if(!imagePath.equals("")){
        Glide.with(photo.getContext())
                .load(imagePath)
                .apply(requestOptions)
                .override(416, 416)
                .into(photo);
      }
      // 이미지가 없는 경우
      else{
        Log.d("test", "setMealImage: ");
        photo.setVisibility(View.GONE);

      }


    }

  }

  @Override
  public void expand() {
    animateExpand();
  }

  @Override
  public void collapse() {
    animateCollapse();
  }


  // 펼쳐질 때 화살표 애니메이션
  private void animateExpand() {
    RotateAnimation rotate =
        new RotateAnimation(360, 180, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 0.5f);
    rotate.setDuration(300);
    rotate.setFillAfter(true);
    arrow.setAnimation(rotate);
  }

  // 닫힐 때 화살표 애니메이션
  private void animateCollapse() {
    RotateAnimation rotate =
        new RotateAnimation(180, 360, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 0.5f);
    rotate.setDuration(300);
    rotate.setFillAfter(true);
    arrow.setAnimation(rotate);
  }
}
