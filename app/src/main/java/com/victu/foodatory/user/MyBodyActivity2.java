package com.victu.foodatory.user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.victu.foodatory.R;


/**
 * 처음 가입을 하고 회원 신체 정보를 입력받는 액티비티이다. (2/3)
 * 달성목표와 목표체중을 입력받는다.
 * @author Shang
 * @version 1.0
 */

public class MyBodyActivity2 extends AppCompatActivity {
    private static final String TAG = "MyBodyActivity2";


    // 뷰 변수
    ImageView img_back;
    Button btn_next;

    RadioGroup radioGroup;
    RadioButton radioButton;
    Intent i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_body2);

        i = getIntent();


        // 뷰를 초기화한다.
        initViews();

    }

    private void initViews() {
        // 레이아웃에서 뷰를 찾아 연결한다.
        img_back = findViewById(R.id.img_back);

        radioGroup = findViewById(R.id.radioGroup);

        // 다음 버튼을 눌렀을 경우
        btn_next = findViewById(R.id.btn_next);
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int radioId = radioGroup.getCheckedRadioButtonId();
                radioButton = findViewById(radioId);
                String s = radioButton.getText().toString();
                Log.d(TAG, "onClick: " + s);
                int goal = getGoal(s);

                Intent intent = new Intent(MyBodyActivity2.this, MyBodyActivity3.class);
                intent.putExtra("mGender", i.getStringExtra("mGender"));
                intent.putExtra("mWeight", i.getStringExtra("mWeight"));
                intent.putExtra("mHeight", i.getStringExtra("mHeight"));
                intent.putExtra("mYear", i.getIntExtra("mYear", 0));
                intent.putExtra("mGoal", goal);
                startActivity(intent);
                finish();
            }
        });


    }

    private int getGoal(String s) {
        // 체중 증가
        if(s.equals(getResources().getString(R.string.goal_gain))){
            return +500;
        }
        // 체중 유지
        else if(s.equals(getResources().getString(R.string.goal_main))){
            return +200;
        }
        // 체중 감량 (default)
        else{
            return 0;
        }
    }

}
