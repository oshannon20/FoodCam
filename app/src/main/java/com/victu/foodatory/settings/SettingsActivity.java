package com.victu.foodatory.settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.kakao.auth.ApiErrorCode;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.UnLinkResponseCallback;
import com.nhn.android.naverlogin.OAuthLogin;
import com.victu.foodatory.R;
import com.victu.foodatory.camera.CameraActivity;
import com.victu.foodatory.gallery.GalleryActivity;
import com.victu.foodatory.home.HomeActivity;
import com.victu.foodatory.user.LoginActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

/**
 * 설정 액티비티이다.
 * ~~~을 할 수 있다.
 *
 * @author Shang
 * @version 1.0
 */

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    ImageView img_settings, img_profile;
    LinearLayout bottom_menu1, bottom_menu2, bottom_menu3, bottom_menu4;
    TextView txt_settings, txt_name, txt_email, txt_logout, txt_delete;

    String loginType; // 어느 소셜API로 가입했는지
    int clickType; // 로그아웃 클릭시 1, 회원탈퇴 클릭시 2

    SharedPreferences autoLogin;
    SharedPreferences.Editor editor;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        context = getApplicationContext();

        autoLogin = getSharedPreferences("login", Context.MODE_PRIVATE);
        editor = autoLogin.edit();
        loginType = autoLogin.getString("loginType", "");
        Log.d(TAG, "onClick: 로그인 타입" + loginType);

        initBottomMenu();
        initViews();

    }


    /**
     *      회원탈퇴, 로그아웃 관련
     */

    private void naverAccount() {
        if (clickType == 1) {
            OAuthLogin mOAuthLoginInstance = OAuthLogin.getInstance();
            mOAuthLoginInstance.logout(SettingsActivity.this);
            moveActivity();

        } else {
            new DeleteTokenTask().execute();
            moveActivity();

        }
    }

    private class DeleteTokenTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            OAuthLogin mOAuthLoginInstance = OAuthLogin.getInstance();

            boolean isSuccessDeleteToken = mOAuthLoginInstance.logoutAndDeleteToken(context);

            if (!isSuccessDeleteToken) {
                Log.d(TAG, "doInBackground: 네이버 토큰 삭제 실패");
                // 서버에서 토큰 삭제에 실패했어도 클라이언트에 있는 토큰은 삭제되어 로그아웃된 상태입니다.
                // 클라이언트에 토큰 정보가 없기 때문에 추가로 처리할 수 있는 작업은 없습니다.
                Log.d(TAG, "errorCode:" + mOAuthLoginInstance.getLastErrorCode(context));
                Log.d(TAG, "errorDesc:" + mOAuthLoginInstance.getLastErrorDesc(context));
            }

            return null;
        }
    }

    private void kakaoAccount() {
        if (clickType == 1) {
            UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
                @Override
                public void onCompleteLogout() {
                    Log.d(TAG, "onCompleteLogout: 카카오 로그아웃");
                    moveActivity();
                }
            });

        }else{

            new AlertDialog.Builder(SettingsActivity.this)
                    .setMessage("탈퇴하시겠습니까?")
                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            UserManagement.getInstance().requestUnlink(new UnLinkResponseCallback() {
                                @Override
                                public void onFailure(ErrorResult errorResult) {
                                    Log.d(TAG, "onFailure: 카카오");
                                    int result = errorResult.getErrorCode();

                                    if(result == ApiErrorCode.CLIENT_ERROR_CODE) {
                                        Toast.makeText(getApplicationContext(), "네트워크 연결이 불안정합니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "회원탈퇴에 실패했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onSessionClosed(ErrorResult errorResult) {
                                    Log.d(TAG, "onSessionClosed: 카카오");
                                    Toast.makeText(getApplicationContext(), "로그인 세션이 닫혔습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                                    moveActivity();

                                }

                                @Override
                                public void onNotSignedUp() {
                                    Log.d(TAG, "onNotSignedUp: 카카오");
                                    Toast.makeText(getApplicationContext(), "가입되지 않은 계정입니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                                    moveActivity();

                                }

                                @Override
                                public void onSuccess(Long result) {
                                    Log.d(TAG, "onSuccess: 카카오 회원탈퇴");
                                    Toast.makeText(getApplicationContext(), "회원탈퇴에 성공했습니다.", Toast.LENGTH_SHORT).show();
                                    moveActivity();
                                }
                            });

                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();

        }

    }

    private void googleAccount() {
        // 구글 로그인 환경설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();

        if (clickType == 1) {
            // Firebase sign out
            mAuth.signOut();

            // Google sign out
            mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "onComplete: 구글 로그아웃");
                           moveActivity();

                        }
                    });

        } else {
            mGoogleSignInClient.revokeAccess()
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "onComplete: 구글 회원탈퇴");
                                 moveActivity();
                        }
                    });
        }
    }




    /**
     * 로그아웃/회원탈퇴 텍스트뷰 클릭시 동작하는 메소드이다.
     * loginType과 clickType을 구분하여 처리한다.
     * @param v
     */
    public void accountRequest(View v) {
        if (v.getId() == R.id.txt_logout) {
            clickType = 1;
            Log.d(TAG, "accountRequest: 로그아웃 클릭 " + clickType);
        } else {
            clickType = 2;
            Log.d(TAG, "accountRequest: 회원탈퇴 클릭 " + clickType);
        }

        switch (loginType) {
            case "G":
                Log.d(TAG, "accountRequest: " + loginType);
                googleAccount();
                break;

            case "N":
                Log.d(TAG, "accountRequest: " + loginType);
                naverAccount();
                break;

            case "K":
                Log.d(TAG, "accountRequest: " + loginType);
                kakaoAccount();
                break;

            case "E":
                Log.d(TAG, "accountRequest: " + loginType);
                moveActivity();
                break;
        }
    }

    private void initViews() {
        // 프로필 설정
        txt_name = findViewById(R.id.txt_name);
        txt_email = findViewById(R.id.txt_email);
        img_profile = findViewById(R.id.img_profile);
        txt_logout = findViewById(R.id.txt_logout);
        txt_delete = findViewById(R.id.txt_delete_account);

    }

    public void initBottomMenu() {

        // 하단 메뉴 찾기
        img_settings = findViewById(R.id.img_settings);
        txt_settings = findViewById(R.id.txt_settings);
        bottom_menu1 = findViewById(R.id.bottom_menu1);
        bottom_menu2 = findViewById(R.id.bottom_menu2);
        bottom_menu3 = findViewById(R.id.bottom_menu3);
        bottom_menu4 = findViewById(R.id.bottom_menu4);

        // 하단 메뉴 색상 변경
        img_settings.setColorFilter(getResources().getColor(R.color.colorPrimaryDark));
        txt_settings.setTextColor(getResources().getColor(R.color.colorPrimaryDark));

        // 하단 메뉴 이동 설정

        bottom_menu1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        bottom_menu2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(SettingsActivity.this, CameraActivity.class);
//                startActivity(intent);
//                finish();

            }
        });

        bottom_menu3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, CameraActivity.class);
                startActivity(intent);
                finish();
            }
        });

        bottom_menu4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, GalleryActivity.class);
                startActivity(intent);
            }
        });


    }

    private void moveActivity() {
        editor.clear();
        editor.apply();

        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    //    finish();
    }



}
