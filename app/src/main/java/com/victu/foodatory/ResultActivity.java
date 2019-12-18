package com.victu.foodatory;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;


/*
    이미지 Uri를 가지고 음식인지 아닌지 분류해서 결과를 보여주는 액티비티

    Classifier를 생성하고
    fnf_classifier = new FoodNonfoodClassifier(this);

    resize한 이미지 bitmap 넘겨주면서 실행하면 됨
    fnf_classifier.runInference(scaledBitmap);

 */
public class ResultActivity extends AppCompatActivity {
    //이미지 분류기
    private FoodNonfoodClassifier fnf_classifier;

    Uri imageuri;   //넘겨받은 이미지 uri
    Bitmap bitmap;  //uri로 생성한 bitmap
    Bitmap scaledBitmap; //resize된 bitmap
    TextView txt_result;
    ImageView img_result;

    Map<String, Float> resultMap;   //결과 받아올 Map

    private static final int IMAGE_WIDTH = 300; //tf모델에서 요구하는 input 이미지 사이즈

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        txt_result = findViewById(R.id.txtResult);
        img_result = findViewById(R.id.imgResult);

        //Intent에서 이미지 uri 받아옴
//        imageuri = Uri.parse(getIntent().getExtras().getString("imageuri"));
        imageuri = getIntent().getData();


        // 약 10초 소요

        try {
            //Classifier 생성
            fnf_classifier = new FoodNonfoodClassifier(this);

            //uri에서 bitmap 생성. 나중에 사용자한테 보여줄 때도 사용
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageuri);

            //tf모델에 맞게 300x300 으로 resize
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_WIDTH, IMAGE_WIDTH, true);
        }catch(IOException e){e.printStackTrace();}


        //모델 실행
        //결과는 <String,Float> 형태의 Map으로 온다
        resultMap = fnf_classifier.runInference(scaledBitmap);

        //결과 Map -> String 변환
        if(resultMap != null){
            Log.d("test", "onCreate: " + resultMap);
            Log.d("test", "onCreate: " + resultMap.get("food")); // 0.65888906
            Log.d("test", "onCreate: " + resultMap.get("non-food"));

            StringBuilder resultString = new StringBuilder();
            DecimalFormat df = new DecimalFormat(".##");    //소수점 2자리까지만 표시
            for (String key : resultMap.keySet()) {
                resultString.append(key + "=" + df.format(resultMap.get(key)));
                resultString.append("\n");
            }
            //TextView 업데이트
            txt_result.setText(resultString.toString());
            // food =1.0
            // non-food=.0
        }

        if(bitmap != null){
            //ImageView 업데이트
            img_result.setImageBitmap(bitmap);
        }

    }

}
