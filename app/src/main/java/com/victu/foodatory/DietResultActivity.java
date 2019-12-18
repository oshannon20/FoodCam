package com.victu.foodatory;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


/**
 * 신체 정보 입력 결과 적정 칼로리를 계산해 보여주는 액티비티이다.
 * @author Shang
 * @version 1.0
 */

public class DietResultActivity extends AppCompatActivity {
    private static final String TAG = "DietResultActivity";

    TextView txt_result;
    int calorie, mCarbohydrate, mProtein, mFat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_result);

        calorie = getIntent().getIntExtra("calorie", 0);

        txt_result = findViewById(R.id.txt_result);
    }




}
