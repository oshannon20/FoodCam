package com.victu.foodatory.user;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.victu.foodatory.R;


/**
 * 처음 가입을 하고 회원 신체 정보를 입력받는 액티비티이다. (1/3)
 * 나이, 키, 현재 몸무게를 입력받는다.
 *
 * @author Shang
 * @version 1.0
 */

public class MyBodyActivity extends AppCompatActivity {

    private static final String TAG = "MyBodyActivity";

    // 뷰 변수
    ImageView img_back;
    Button btn_next;
    EditText edit_height, edit_current_weight, edit_age;

    RadioGroup radioGroup;
    RadioButton radioButton;

    int birthYear;


    DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            birthYear = year;
            Log.d(TAG, "onDateSet: " + birthYear);
            edit_age.setText(String.valueOf(birthYear));
            Log.d(TAG, "onDateSet: ??");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_body);

        // 뷰를 초기화한다.
        initViews();


    }

    private void initViews() {
        // 레이아웃에서 뷰를 찾아 연결한다.
        img_back = findViewById(R.id.img_back);

        radioGroup = findViewById(R.id.radioGroup);


        edit_age = findViewById(R.id.edit_age);
        edit_age.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YearPickerDialog pd = new YearPickerDialog();
                pd.setListener(dateSetListener);
                pd.show(getSupportFragmentManager(), "YearMonthPickerTest");
                //TODO: editText.setText()
            }
        });

        //TODO: 키/몸무게 최대/최소값 설정할 것. textwatcher
        edit_height = findViewById(R.id.edit_height);
        edit_current_weight = findViewById(R.id.edit_current_weight);

        // 다음 버튼을 눌렀을 경우, 액티비티 이동한다.
        btn_next = findViewById(R.id.btn_next);
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //TODO: 값이 다 있는지 확인, 값을 담아서 인텐트로 전달한다.

                // 성별 값을 받아온다.
                int radioId = radioGroup.getCheckedRadioButtonId();
                radioButton = findViewById(radioId);
                String gender = radioButton.getText().toString();
                Log.d(TAG, "onClick: " + gender);

                if(gender.equals("여성")){
                    gender = "F";
                }else{
                    gender = "M";
                }


                // 키와 몸무게를 받아온다.
                String height = edit_height.getText().toString();
                String weight = edit_current_weight.getText().toString();

                Log.d(TAG, "onClick: 입력된 정보 " + gender + " 키" + height + " 몸무게 " + weight + " 출생 연도" + birthYear);

                if(height != null && weight != null){
                    Log.d(TAG, "onClick: 액티비티 이동");
                    Intent intent = new Intent(MyBodyActivity.this, MyBodyActivity2.class);
                    intent.putExtra("mGender", gender);
                    intent.putExtra("mWeight", weight);
                    intent.putExtra("mHeight", height);
                    intent.putExtra("mYear", birthYear);

                    startActivity(intent);
                   finish();

                }else{
                    // 버튼 활성화 안 됨.
                    Toast.makeText(MyBodyActivity.this, "값을 다 입력해주세요.", Toast.LENGTH_SHORT).show();
                }


            }
        });
    }





}
