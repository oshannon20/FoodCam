package com.victu.foodatory;

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.victu.foodatory.gallery.PhotoJobService;
import com.victu.foodatory.home.HomeActivity;
import com.victu.foodatory.user.LoginActivity;

import gr.net.maroulis.library.EasySplashScreen;

public class SplashScreenActivity extends AppCompatActivity {
    private static final String TAG = "SplashScreenActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            scheduleCameraJob();
//        }
//

//        Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate);
//        animation.setInterpolator(new LinearInterpolator());
//        animation.setRepeatCount(Animation.INFINITE);
//        animation.setDuration(700);

        EasySplashScreen config = new EasySplashScreen(SplashScreenActivity.this)
                .withFullScreen()
              //  .withBackgroundResource(R.drawable.layout_gradient)
                .withTargetActivity(LoginActivity.class)
                .withLogo(R.drawable.logo_with_name);

        // 로딩화면 끝난 다음 어디로 갈지
        // 자동로그인 확인
        SharedPreferences autoLogin = getSharedPreferences("login", Activity.MODE_PRIVATE);
        int userId = autoLogin.getInt("userId", 0);
        Log.d(TAG, "onCreate: " + userId);

        // 로그인 화면으로 이동
        if(userId==0) {
            config.withTargetActivity(LoginActivity.class)
                    .withSplashTimeOut(1000);// 3초간 나타난다
        }
        // 메인 홈 화면으로 이동
        else {
            config.withTargetActivity(HomeActivity.class)
                    .withSplashTimeOut(1000);
        }

        config.getLogo().setMaxHeight(500);
        config.getLogo().setMaxWidth(500);
       // config.getLogo().setAnimation(animation);

        View easySplashScreen = config.create();
        setContentView(easySplashScreen);

    }


    //TODO: 앱 처음 켰을 때 실행.
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void scheduleCameraJob() {
        Log.d(TAG, "//scheduleCameraJob: ");
        final Uri MEDIA_URI = Uri.parse("content://" + MediaStore.AUTHORITY + "/");

        JobInfo.Builder builder = new JobInfo.Builder(11,
                new ComponentName(this, PhotoJobService.class.getName()));
        // Look for specific changes to images in the provider.
        builder.addTriggerContentUri(new JobInfo.TriggerContentUri(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
        // Also look for general reports of changes in the overall provider.
        builder.addTriggerContentUri(new JobInfo.TriggerContentUri(MEDIA_URI, 0));
        builder.setTriggerContentUpdateDelay(1);
        builder.setTriggerContentMaxDelay(100);
        JobInfo myCameraJob = builder.build();
        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(myCameraJob);
    }



}
