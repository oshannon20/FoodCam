package com.victu.foodatory.user;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kakao.auth.ApiErrorCode;
import com.kakao.auth.ApiResponseCallback;
import com.kakao.auth.AuthService;
import com.kakao.auth.AuthType;
import com.kakao.auth.network.response.AccessTokenInfoResponse;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.victu.foodatory.R;
import com.victu.foodatory.home.HomeActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.util.exception.KakaoException;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;
import com.victu.foodatory.utils.RetrofitConnection;
import com.victu.foodatory.utils.RetrofitInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * 소셜API로그인(구글, 카카오, 네이버) 을 할 수 있는 액티비티이다.
 *
 * @author Shang
 * @version 1.0
 */


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // 구글
    private static final int GOOGLE_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    //    private SignInButton btn_google;
    private ImageView btn_google;
    private GoogleSignInOptions gso;

    // 네이버
    public static OAuthLogin mOAuthLoginInstance;
    OAuthLoginButton btn_naver;
    private Context mContext;
    private OAuthLoginHandler naverLoginHandler;

    // 카카오
    private ImageView btn_kakao;
    private SessionCallback callback;

    // 이메일 로그인, 회원가입 뷰
    private Button btn_email;
    private TextView txt_sign_up;

    // API 통해 받은 회원 토큰과 로그인타입
    String token, loginType;

    SharedPreferences autoLogin;
    SharedPreferences.Editor editor;

    Retrofit retrofit;
    RetrofitInterface uploadAPIs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        mContext = getApplicationContext();

        retrofit = RetrofitConnection.getRetrofitClient(this);
        uploadAPIs = retrofit.create(RetrofitInterface.class);

        autoLogin = getSharedPreferences("login", Activity.MODE_PRIVATE);
        editor = autoLogin.edit();

        btn_email = findViewById(R.id.btn_email);
        btn_email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, EmailLoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        txt_sign_up = findViewById(R.id.txt_sign_up);
        txt_sign_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btn_naver = findViewById(R.id.btn_naver);
        btn_google = findViewById(R.id.btn_google);

        btn_kakao = findViewById(R.id.btn_kakao);
        btn_kakao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kakao();
                Session.getCurrentSession().open(AuthType.KAKAO_LOGIN_ALL, LoginActivity.this);
            }
        });


        google();
        naver();

    }


    //FIXME: 오류 발생 activity has leaked window that was originally added here

    private void setDialog(boolean show){
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        //View view = getLayoutInflater().inflate(R.layout.progress);
        builder.setView(R.layout.dialog_progress);
        Dialog dialog = builder.create();
        if (show)
            dialog.show();
        else
            dialog.dismiss();
    }


    /**
     * 토큰을 서버로 전송해서 가입하는 메소드이다.
     */
    private void signUp() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mem_login_type", loginType);
        jsonObject.addProperty("mem_token", token);
        Log.d(TAG, "signUp: " + jsonObject);

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
                        editor.putString("loginType", loginType);
                        editor.apply();
                        Log.d(TAG, "onResponse: " + autoLogin.getInt("userId", 0) + "/" +
                                autoLogin.getString("accessToken", "") + "/" +
                                autoLogin.getString("refreshToken", ""));

                        // MyBodyActivity로 화면 전환한다.
                        moveActivity(false);


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



    private void login() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mem_login_type", loginType);
        jsonObject.addProperty("mem_token", token);
        Log.d(TAG, "login: " + jsonObject);


        Call call = uploadAPIs.signIn(jsonObject);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call call, Response response) {
                Log.d(TAG, "onResponse: " + response.code());

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
                        editor.putString("loginType", loginType);
                        editor.apply();
                        Log.d(TAG, "onResponse: " + autoLogin.getInt("userId", 0) + "/" +
                                autoLogin.getString("accessToken", "") + "/" +
                                autoLogin.getString("refreshToken", ""));


                        Log.d(TAG, "loginSuccess: 홈 액티비티로 이동");
                        moveActivity(true);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.d(TAG, "onResponse: failure" + response.message());
                    loginFailure();

                }

            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
                loginFailure();

            }
        });

    }

    /**
     * 액티비티 이동한다. true인 경우 HomeActivity, false인 경우 MyBodyActivity
     * @param isLogin
     */
    private void moveActivity(boolean isLogin) {
        //setDialog(false);

        if(isLogin){
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }else{
            Intent intent = new Intent(LoginActivity.this, MyBodyActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * 서버로 이메일을 전송하여 중복 여부를 확인한다.
     * 계정이 없는 경우 true - 회원가입 후 로그인
     * 계정이 있는 경우 false - 로그인
     */
    private void emailCheck() {
     //   setDialog(true);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("mem_login_type", loginType);
        hashMap.put("mem_token", token);
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
                        boolean signUP = jsonObject.getBoolean("result");
                        Log.d(TAG, "onResponse: 가입가능한가? " + signUP);
                        if (signUP) {
                            signUp();
                        } else {
                            login();
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


    protected void loginFailure() {
       // setDialog(false);
        Toast.makeText(mContext, "로그인 실패", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    /**
     * 카카오 로그인 관련 코드이다.
     */
    private void kakao() {
        // 카카오 로그인 콜백받기
        callback = new SessionCallback();
        Session.getCurrentSession().addCallback(callback);
        Session.getCurrentSession().checkAndImplicitOpen();

    }

    private class SessionCallback implements ISessionCallback {

        // 로그인 세션이 열렸을 때
        @Override
        public void onSessionOpened() {
            List<String> keys = new ArrayList<>();
//            keys.add("properties.nickname");
//            keys.add("properties.profile_image");
//            keys.add("kakao_account.email");

            UserManagement.getInstance().me(keys, new MeV2ResponseCallback() {
                @Override
                public void onFailure(ErrorResult errorResult) {
                    int result = errorResult.getErrorCode();

                    if (result == ApiErrorCode.CLIENT_ERROR_CODE) {
                        Toast.makeText(getApplicationContext(), "네트워크 연결이 불안정합니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "로그인 도중 오류가 발생했습니다: " + errorResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
                    }
                    loginFailure();
                }

                @Override
                public void onSessionClosed(ErrorResult errorResult) {
                    Toast.makeText(getApplicationContext(), "세션이 닫혔습니다. 다시 시도해 주세요: " + errorResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
                    loginFailure();
                }

                @Override
                public void onSuccess(MeV2Response result) {
                    Log.d(TAG, "onSuccess: " + result);

                    loginType = "K";
                    token = Session.getCurrentSession().getTokenInfo().getAccessToken();
                    Log.d(TAG, "kakao: " + token);
                    emailCheck();

                }
            });
        }

        // 로그인 세션이 정상적으로 열리지 않았을 때
        @Override
        public void onSessionOpenFailed(KakaoException e) {
            Toast.makeText(getApplicationContext(), "로그인 도중 오류가 발생했습니다. 인터넷 연결을 확인해주세요: " + e.toString(), Toast.LENGTH_SHORT).show();
            loginFailure();
        }
    }

    private void requestAccessTokenInfo() {
        AuthService.getInstance().requestAccessTokenInfo(new ApiResponseCallback<AccessTokenInfoResponse>() {
            @Override
            public void onSessionClosed(ErrorResult errorResult) {

            }

            @Override
            public void onNotSignedUp() {
                // not happened
            }

            @Override
            public void onFailure(ErrorResult errorResult) {
                Log.e(TAG, "failed to get access token info. msg=" + errorResult);
            }

            @Override
            public void onSuccess(AccessTokenInfoResponse accessTokenInfoResponse) {
                long userId = accessTokenInfoResponse.getUserId();
                Log.d(TAG, "this access token is for userId=" + userId);
                Log.d(TAG, "this access token " + token);

            }
        });
    }


    /**
     * 네이버 로그인 관련 코드
     */
    private void naver() {
        // 네이버 로그인 인스턴스 초기화
        mOAuthLoginInstance = OAuthLogin.getInstance();
        mOAuthLoginInstance.init(LoginActivity.this, "ecfa1vOcNOfBd6f0jpHD", "ihEy5wnQvI", "Foodatory");

        naverLoginHandler = new OAuthLoginHandler() {
            @Override
            public void run(boolean success) {
                if (success) {
                    Log.d(TAG, "run: 네이버 로그인 성공");

                    loginType = "N";
                    token = mOAuthLoginInstance.getAccessToken(mContext);
                    Log.d(TAG, "run: access 토큰 " + token);

                    //   String refreshToken = mOAuthLoginInstance.getRefreshToken(mContext);
                    //         Log.d(TAG, "run: refresh 토큰 " + refreshToken);

                    /*
                     * NEED_INIT: 초기화가 필요한 상태
                     * NEED_LOGIN: 로그인이 필요한 상태. 접근 토큰(access token)과 갱신 토큰(refresh token)이 모두 없습니다.
                     * NEED_REFRESH_TOKEN: 토큰 갱신이 필요한 상태. 접근 토큰은 없고, 갱신 토큰은 있습니다.
                     * OK: 접근 토큰이 있는 상태. 단, 사용자가 네이버의 내정보 > 보안설정 > 외부 사이트 연결 페이지에서 연동을 해제했다면 서버에서는 상태 값이 유효하지 않을 수 있습니다.
                     */

                    String state = mOAuthLoginInstance.getState(mContext).toString();
                    Log.d(TAG, "run: login 상태 " + state);

                    // token을 넘겨주어, 회원정보를 가져온다.
                    // 회원 정보 안 가져오는 걸로
//                    NaverProfile task = new NaverProfile();
//                    task.execute(token);

                    emailCheck();

                }
                // 로그인 실패
                else {
                    String errorCode = mOAuthLoginInstance.getLastErrorCode(mContext).getCode();
                    String errorDesc = mOAuthLoginInstance.getLastErrorDesc(mContext);
                    Log.d(TAG, "run: errorCode: " + errorCode + "errorDesc: " + errorDesc);
                    loginFailure();

                }
            }

        };

        btn_naver.setOAuthLoginHandler(naverLoginHandler);
    }


    private class RefreshTokenTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return mOAuthLoginInstance.refreshAccessToken(mContext);
        }

        protected void onPostExecute(String res) {
            Log.d(TAG, "onPostExecute: ");
        }

    }


    /**
     * 구글 로그인 관련 코드
     */
    private void google() {
        // 구글 로그인 환경설정
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();

        // 구글 로그인 버튼 클릭시 동작
        btn_google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
            }
        });
    }


    private void firebaseAuthWithGoogle(final GoogleSignInAccount signInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle: 1");
        AuthCredential credential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            loginType = "G";
                            token = task.getResult().getUser().getUid();
                            Log.d(TAG, "onComplete: 구글토큰 " + loginType + "/" + token);

                            emailCheck();

                        } else {
                            // 로그인이 실패하면, 사용자에게 메시지를 보여준다.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(getApplicationContext(), "로그인 실패: 아이디와 비밀번호를 다시 확인해주세요", Toast.LENGTH_SHORT).show();
                        }
                    }

                });
    }


    /**
     * 생명주기 관련 코드
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // 구글 로그인
        if (requestCode == GOOGLE_SIGN_IN) {
            Log.d(TAG, "onActivityResult: 구글 로그인");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // 구글 로그인에 성공했음. firebase로 인증한다.
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // 구글 로그인 실패
                Log.w(TAG, "Google sign in failed", e);
            }
        }

        // 카카오 로그인
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            Log.d(TAG, "onActivityResult: 카카오");
//            loginSuccess();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);

    }


    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
//        loginSuccess(currentUser);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Session.getCurrentSession().removeCallback(callback);
    }


//    class NaverProfile extends AsyncTask<String, Void, String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            String result = null;
//
//            String token = strings[0];// 네이버 로그인 접근 토큰;
//            Log.d(TAG, "doInBackground: " + token);
//            String header = "Bearer " + token; // Bearer 다음에 공백 추가
//            try {
//                String apiURL = "https://openapi.naver.com/v1/nid/me";
//                URL url = new URL(apiURL);
//                HttpURLConnection con = (HttpURLConnection) url.openConnection();
//                con.setRequestMethod("GET");
//                con.setRequestProperty("Authorization", header);
//                int responseCode = con.getResponseCode();
//                BufferedReader br;
//                if (responseCode == 200) { // 정상 호출
//                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
//                } else {  // 에러 발생
//                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
//                }
//                String inputLine;
//                StringBuffer response = new StringBuffer();
//
//                while ((inputLine = br.readLine()) != null) {
//                    response.append(inputLine);
//                }
//                result = response.toString();
//                br.close();
//                System.out.println(response.toString());
//            } catch (Exception e) {
//                System.out.println(e);
//            }
//            //result 값은 JSONObject 형태로 넘어옵니다.
//            return result;
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
//            super.onPostExecute(s);
//            try {
//                // 넘어온 result 값을 JSONObject 로 변환해 값을 가져온다.
//                JSONObject object = new JSONObject(s);
//                Log.d(TAG, "onPostExecute : " + s);
//
//                if (object.getString("resultcode").equals("00")) {
//                    JSONObject jsonObject = new JSONObject(object.getString("response"));
//
//
//                    email = jsonObject.getString("email");
//                    nickName = jsonObject.getString("nickname");
//                    imageUrl = jsonObject.getString("profile_image");
//
//                    if (email == null) {
//                        email = jsonObject.getString("id");        // 동일인 식별 정보: 네이버 아이디마다 고유하게 발급되는 유니크한 일련번호 값
//                    }
//                    if (nickName == null) {
//                        nickName = "null";
//                    }
//                    if (imageUrl == null) {
//                        imageUrl = "null";
//                    }
//
//                    Log.d(TAG, "onSuccess: " + email + " " + nickName + " " + imageUrl);
//
//
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

}