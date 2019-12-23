package com.victu.foodatory.user;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.victu.foodatory.gallery.GalleryActivity;
import com.victu.foodatory.R;
import com.victu.foodatory.home.HomeActivity;
import com.victu.foodatory.utils.RetrofitConnection;
import com.victu.foodatory.utils.RetrofitInterface;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class EmailLoginActivity extends AppCompatActivity {
    private static final String TAG = "EmailLoginActivity";

    EditText emailText, passwordText;
    Button loginButton;
    TextView signUpLik, socialLogin;


    String email, password;
    private ProgressDialog pDialog;

    Retrofit retrofit;
    RetrofitInterface uploadAPIs;

    SharedPreferences autoLogin;
    SharedPreferences.Editor editor;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_login);

        retrofit = RetrofitConnection.getRetrofitClient(this);
        uploadAPIs = retrofit.create(RetrofitInterface.class);

        autoLogin = getSharedPreferences("login", Activity.MODE_PRIVATE);
        editor = autoLogin.edit();


        // HomeActivity로 이동
        ImageView logo = findViewById(R.id.img_logo);
        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(EmailLoginActivity.this, HomeActivity.class);
                startActivity(i);
                finish();
            }
        });


        initViews();
    }

    private void displayLoader() {
        pDialog = new ProgressDialog(EmailLoginActivity.this);
        pDialog.setMessage("로그인 중입니다. 잠시만 기다려주세요.");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
        pDialog.show();

    }

    private void initViews() {
        loginButton = findViewById(R.id.btn_login);
        emailText = findViewById(R.id.input_email);
        passwordText = findViewById(R.id.input_password);
        signUpLik = findViewById(R.id.link_signup);
        socialLogin = findViewById(R.id.txt_social_login);

        socialLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(EmailLoginActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        });


        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = emailText.getText().toString();
                String password = passwordText.getText().toString();
                Log.d(TAG, "validate: " + email + "/" + password);
                password = SignUpActivity.sha256(password);
                Log.d(TAG, "validate: " + password);

                login(email, password);

            }
        });

        // 회원가입 페이지로 이동한다.
        signUpLik.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(EmailLoginActivity.this, SignUpActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }


    /**
     * @param email
     * @param password
     * @return
     */
    private void login(String email, String password) {

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mem_login_type", "E");
        jsonObject.addProperty("mem_email", email);
        jsonObject.addProperty("mem_password", password);


        Log.d(TAG, "login: " + jsonObject);


        Call call = uploadAPIs.signIn(jsonObject);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call call, Response response) {
                Log.d(TAG, "onResponse: " + response.code());
                Log.d(TAG, "onResponse: " + response.message());

                if (response.isSuccessful()) {
                    // 성공적으로 서버에서 데이터 불러왔을 때

                    try {
                        // response 데이터를 json에 담아준다.
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        Log.d(TAG, "onResponse: " + jsonObject);
                        int userId = jsonObject.getInt("mem_uid");

                        String accessToken = jsonObject.getString("access_token");
                        String refreshToken = jsonObject.getString("refresh_token");

                        // SharedPreferences에 유저 정보를 저장한다.
                        editor.putInt("userId", userId);
                        editor.putString("accessToken", accessToken);
                        editor.putString("refreshToken", refreshToken);
                        editor.putString("loginType", "E");
                        editor.apply();
                        Log.d(TAG, "onResponse: " + autoLogin.getInt("userId", 0) + "/" +
                                autoLogin.getString("accessToken", "") + "/" +
                                autoLogin.getString("refreshToken", ""));

                        onLoginSuccess();


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                } else {
                    Log.d(TAG, "onResponse: 회원정보 맞지 않음.");
                    onLoginFailed();

                }

            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
                onLoginFailed();


            }
        });

    }


    /**
     * 로그인 성공시 HomeActivity로 이동한다.
     */
    public void onLoginSuccess() {
        Log.d(TAG, "onLoginSuccess: ");
        loginButton.setEnabled(true);

        Intent intent = new Intent(EmailLoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 로그인 실패시 AlertDialog를 띄운다.
     */
    public void onLoginFailed() {
        Log.d(TAG, "onLoginFailed: ");

        AlertDialog.Builder builder = new AlertDialog.Builder(EmailLoginActivity.this);
        builder.setMessage("이메일 혹은 비밀번호를 다시 확인해주세요.");
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        loginButton.setEnabled(true);
    }


    public boolean validate() {
        boolean valid = true;


        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailText.setError("이메일을 입력해주세요.");
            valid = false;
        } else {
            emailText.setError(null);
        }

        if (password.isEmpty()) {
            passwordText.setError("비밀번호를 입력해주세요.");
            valid = false;
        } else {
            passwordText.setError(null);
        }

        return valid;
    }
}