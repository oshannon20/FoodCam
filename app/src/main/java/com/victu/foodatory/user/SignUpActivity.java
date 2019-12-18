package com.victu.foodatory.user;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.victu.foodatory.R;
import com.victu.foodatory.utils.RetrofitConnection;
import com.victu.foodatory.utils.RetrofitInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.regex.Pattern;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


/**
 * 이메일로 회원가입을 할 수 있는 액티비티이다.
 *
 * @author Shang
 * @version 1.0
 */

public class SignUpActivity extends AppCompatActivity {
    // 변수
    private static final String TAG = "SignUpActivity";

 //   private String regex = "^(?=.*\\d)(?=.*[~`!@#$%\\^&*()-])(?=.*[a-zA-Z]).{8,}$";
    private String regex = "^" +
            "(?=.*[0-9])" +         //at least 1 digit
            "(?=.*[a-zA-Z])" +      //any letter
            "(?=.*[-()~!@#$%^&+=])" +    //at least 1 special character
            "(?=\\S+$)" +           //no white spaces
            ".{8,}" +               //at least 8 characters
            "$";

    private Pattern passwordPattern = Pattern.compile(regex);


    EditText emailText, passwordText, confirmPasswordText;
    Button signUpButton;
    TextView loginButton;

    private String email, password, passwordConfirm;

    Retrofit retrofit;
    RetrofitInterface uploadAPIs;

    private SharedPreferences autoLogin;
    private SharedPreferences.Editor editor;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        retrofit = RetrofitConnection.getRetrofitClient(this);
        uploadAPIs = retrofit.create(RetrofitInterface.class);

        autoLogin = getSharedPreferences("login", Activity.MODE_PRIVATE);
        editor = autoLogin.edit();


        initViews();
        setFocus();
    }


    /**
     * 입력창에서 포커스가 넘어갔을 때 형식을 확인해준다.
     */
    private void setFocus() {

        emailText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                email = emailText.getText().toString().trim();

                // 포커스가 넘어갔을 때 이메일 형식을 확인하고, 형식이 올바르다면 emailCheck 메소드를 호출한다.
                if(!hasFocus){
                    if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailText.setError("이메일을 다시 입력해주세요.");
                    }else{
                        emailCheck();
                    }

                }
            }
        });

        passwordText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                password = passwordText.getText().toString().trim();

                if(!hasFocus){
                    Log.d(TAG, "onFocusChange: password");
                    if (password.isEmpty() || !passwordPattern.matcher(password).matches()) {
                        passwordText.setError("영문/숫자/특수문자 각각 1개 이상 포함 8자 이상으로 작성해주세요.");
                    }
                }
            }
        });

        confirmPasswordText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    if (passwordConfirm.isEmpty() || !passwordConfirm.equals(password)) {
                        confirmPasswordText.setError("비밀번호를 다시 입력해주세요.");
                    }
                }

            }
        });



    }

    /**
     * 서버로 이메일을 전송하여 중복 여부를 확인한다.
     */
    private void emailCheck() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("mem_login_type", "E");
        hashMap.put("mem_email", email);
        Log.d(TAG, "emailCheck: " + hashMap);

        //Header 더해준다.
        RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"),
                (new JSONObject(hashMap)).toString());

        Call call = uploadAPIs.checkEmail(body);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call call, Response response) {
                Log.d(TAG, "onResponse: " + response.code());
                Log.d(TAG, "onResponse: " + response.message());

                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: response success");

                    try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        // 계정이 존재하는 경우 false - 가입불가
                        // 계정이 존재하지 않는 경우 true - 가입가능
                        boolean signUp = jsonObject.getBoolean("result");
                        Log.d(TAG, "onResponse: 이메일 존재 여부 " + signUp);

                        // 이메일이 중복인 경우 에러메시지가 나타난다.
                        if(!signUp){
                            emailText.setError("이메일을 다시 입력해주세요.");
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.d(TAG, "onResponse: 서버 오류");
                }

            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());

            }
        });

    }




    /**
     * 이메일, 비밀번호를 서버로 전송해서 가입하는 메소드이다.
     */
    private void signUp() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mem_login_type", "E");
        jsonObject.addProperty("mem_email", email);
        jsonObject.addProperty("mem_password", password);

        Log.d(TAG, "emailCheck: " + jsonObject);

        Call call = uploadAPIs.signUp(jsonObject);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call call, Response response) {
                Log.d(TAG, "onResponse: " + response.code());

                if (response.isSuccessful()) {
                    try {
                        // response를 받아서 String에 넣어준다.
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        int userId = jsonObject.getInt("mem_uid");
                        String accessToken = jsonObject.getString("access_token");
                        String refreshToken = jsonObject.getString("refresh_token");

                        // SharedPreferences에 유저 정보를 저장한다.
                        editor.putInt("userId", userId);
                        editor.putString("accessToken", accessToken);
                        editor.putString("refreshToken", refreshToken);
                        editor.putString("loginType", "E");
                        editor.apply();
                        Log.d(TAG, "onResponse: " + autoLogin.getInt("userId", 0) +"/" +
                                autoLogin.getString("accessToken","") + "/" +
                                autoLogin.getString("refreshToken", ""));

                        // MyBodyActivity로 화면 전환한다.
                        signUpSuccess();


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }



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
     * 가입에 성공했을 때, MyBodyActivity로 이동한다.
     */
    private void signUpSuccess() {
        Intent intent = new Intent(SignUpActivity.this, MyBodyActivity.class);
        startActivity(intent);
        finish();
    }


    /**
     * 화면에서 뷰를 찾아주고, 버튼을 눌렀을 경우 동작을 정의한다.
     */
    private void initViews() {
        signUpButton = findViewById(R.id.btn_signup);
        loginButton = findViewById(R.id.btn_login);
        emailText = findViewById(R.id.input_email);
        passwordText = findViewById(R.id.input_password);
        confirmPasswordText = findViewById(R.id.input_password_confirm);

        // 가입버튼을 눌렀을 때
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                passwordConfirm = confirmPasswordText.getText().toString().trim();

                Log.d(TAG, "onClick: " + email + "/" +password + "/" +passwordConfirm);
                password = sha256(password);
                Log.d(TAG, "onClick: " + password);


                // 형식이 맞다면, 서버에 값을 전송한다.
                //TODO: 버튼 눌렀을 때 형식 확인 다시.
//                if(validate()){
                    signUp();


            }
        });

        // 로그인 버튼을 눌렀을 때, 로그인 액티비티로 돌아간다.
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }


    /**
     * @param str 사용자가 입력한 비밀번호
     * @return 암호화 한 값
     */
    public static String sha256(String str) {
        String SHA = "";
        try{
            MessageDigest sh = MessageDigest.getInstance("SHA-256");
            sh.update(str.getBytes());
            byte byteData[] = sh.digest();
            StringBuffer sb = new StringBuffer();
            for(int i = 0 ; i < byteData.length ; i++) sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
            SHA = sb.toString();
        }catch(NoSuchAlgorithmException e) { e.printStackTrace(); SHA = null; }
        return SHA;
    }



    // 회원가입2. 형식을 검사한다.
    public boolean validate() {
        boolean valid = true;

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailText.setError("이메일을 다시 입력해주세요.");
            emailText.requestFocus();
            valid = false;
        }

        if (password.isEmpty() || !passwordPattern.matcher(password).matches()) {
            passwordText.setError("영문/숫자/특수문자 각각 1개 이상 포함 8자 이상으로 작성해주세요.");
            passwordText.requestFocus();
            valid = false;
        }

        if (passwordConfirm.isEmpty() || !passwordConfirm.equals(password)) {
            confirmPasswordText.setError("비밀번호를 다시 입력해주세요.");
            confirmPasswordText.requestFocus();
            valid = false;
        }

        return valid;
    }


}