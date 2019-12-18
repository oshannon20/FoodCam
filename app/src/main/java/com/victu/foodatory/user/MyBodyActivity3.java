package com.victu.foodatory.user;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.victu.foodatory.R;
import com.victu.foodatory.home.HomeActivity;
import com.victu.foodatory.utils.RetrofitConnection;
import com.victu.foodatory.utils.RetrofitInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


/**
 * 처음 가입을 하고 회원 신체 정보를 입력받는 액티비티이다. (3/3)
 * 활동수준을 입력받는다.
 * 최종 신체 정보들을 서버로 전송한다.
 *
 * @author Shang
 * @version 1.0
 */


public class MyBodyActivity3 extends AppCompatActivity {
    private static final String TAG = "MyBodyActivity3";


    // 뷰 변수
    ImageView img_back;
    Button btn_next;

    RadioGroup radioGroup;
    RadioButton radioButton;

    int mYear, mAge, mGoal, mHeight, mWeight;
    double mActivity;
    String mGender;

    int mCalorie;

    Retrofit retrofit;
    RetrofitInterface uploadAPIs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_body3);


        retrofit = RetrofitConnection.getRetrofitClient(this);
        uploadAPIs = retrofit.create(RetrofitInterface.class);

        getBodyData();


        // 뷰를 초기화한다.
        initViews();

    }

    /**
     * 인텐트로 넘어온 데이터를 받아 변수에 넣어준다.
     */
    private void getBodyData() {
        Intent intent = getIntent();
        mYear = intent.getIntExtra("mYear", 0); // 출생연도
        mAge = getAge(mYear);
        mGender = intent.getStringExtra("mGender");
        mHeight = Integer.parseInt(intent.getStringExtra("mHeight"));
        mWeight = Integer.parseInt(intent.getStringExtra("mWeight"));
        mGoal = intent.getIntExtra("mGoal", 0);
        Log.d(TAG, "getBodyData: 출생연도 "+ mYear + "/나이 " + mAge + "/성별 " + mGender + "/키 " + mHeight + "/몸무게 "+ mWeight + "/목표 " + mGoal);

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
                // 유저가 선택한 활동수준을 받아온다.
                int radioId = radioGroup.getCheckedRadioButtonId();
                radioButton = findViewById(radioId);
                String s = radioButton.getText().toString();
                Log.d(TAG, "onClick: " + s);
                mActivity = getActivity(s);

                // 입력한 값에 따른 권장칼로리 계산
                mCalorie = getCalorie();
                Log.d(TAG, "onClick: " + mCalorie);

                // 아침, 점심, 저녁, 간식 칼로리 비율 = 30:30:20:20 (default)

                // 탄/단/지 양 계산

                // 고지방, 고탄수화물, 평범한 식단, 다이어트 식단, 저탄수화물
                //calNutritions(DIET_CALORIE);

                sendData();




            }
        });

    }

    /**
     * 회원이 입력한 신체정보를 서버로 전송해 데이터베이스에 저장하는 메소드이다.
     */
    private void sendData() {
        SharedPreferences autoLogin = getSharedPreferences("login", Activity.MODE_PRIVATE);
        final int userId = autoLogin.getInt("userId", 0);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mem_uid", userId);
        jsonObject.addProperty("diet_purpose", mGoal);
        jsonObject.addProperty("diet_activity", mActivity);
        jsonObject.addProperty("diet_gender", mGender);
        jsonObject.addProperty("diet_height", mHeight);
        jsonObject.addProperty("diet_age", mYear);
        jsonObject.addProperty("diet_weight", mWeight);
        jsonObject.addProperty("diet_calorie", mCalorie);


        Log.d(TAG, "sendData: " + jsonObject);

        Call call = uploadAPIs.addDiet(jsonObject);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call call, Response response) {
                Log.d(TAG, "onResponse: " + response.code());

                if (response.isSuccessful()) {
                    // 액티비티 이동
                    Intent intent = new Intent(MyBodyActivity3.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Log.d(TAG, "onResponse: 서버 오류로 실패 ");

                }

            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());

            }
        });



    }


    /**
     * 입력한
     * @return 하루권장칼로리
     */
    private int getCalorie() {
        /*
        * 1. BMR(Basal Metabolic Rate, 기초대사율) 계산한다.
        * 여성 (10 × 체중) + (6.25 ×신장) - (5 × 연령) – 161
        * 남성 (10 × 체중) + (6.25 ×신장) - (5 × 연령) + 5
        * 2. 활동 수준에 따라 BMR에 가중치를 곱한다.
        * 3. 목표에 따라 2의 값에 칼로리를 더해준다. (-300/+300)
        * */

        double bmr = (10* mWeight) + (6.25* mHeight) - (5* mAge);
        double calorie;

        // 여성인 경우
        if(mGender.equals("F")){
            bmr = bmr -161;
            Log.d(TAG, "getDIET_CALORIE: 여성" + bmr);
            calorie = (mActivity *bmr) + mGoal;
        }
        // 남성인 경우
        else{
            bmr = bmr +5;
            Log.d(TAG, "getDIET_CALORIE: 남성" + bmr);
            calorie = (mActivity *bmr) + mGoal;
        }


        Log.d(TAG, "getDIET_CALORIE: " + calorie);

        return (int) calorie;
    }

    /**
     * 이 액티비티에서 유저가 선택한 활동수준에 따른 가중치를 반환한다.
     * @param s
     * @return
     */
    private double getActivity(String s) {
        // 약간 활동적
        if (s.equals(getResources().getString(R.string.level_slightly_active))) {
            return 1.375;
        }
        // 활동적
        else if (s.equals(getResources().getString(R.string.level_active))) {
            return 1.55;
        }
        // 매우 활동적
        else if (s.equals(getResources().getString(R.string.level_extremely_active))) {
            return 1.725;
        }
        // 앉아있는
        else {
            return 1.2;
        }
    }

    private void calculateBMI() {

    }

    /**
     * 출생연도에 따른 나이를 계산하는 메소드이다. (기준: 한국나이)
     *
     * @param birthYear 출생연도
     * @return 나이
     */

    public int getAge(int birthYear) {
        int age;

        final Calendar calenderToday = Calendar.getInstance();
        int currentYear = calenderToday.get(Calendar.YEAR);

        age = currentYear - birthYear + 1;

        return age;
    }




}
