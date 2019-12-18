package com.victu.foodatory;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.victu.foodatory.camera.CameraActivity;
import com.victu.foodatory.gallery.GalleryActivity;
import com.victu.foodatory.gallery.TestActivity;
import com.victu.foodatory.user.LoginActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn1 = findViewById(R.id.btn1);
        btn1.setText("Camera");
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
                finish();

            }
        });


        Button btn2 = findViewById(R.id.btn2);
        btn2.setText("my gallery");
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
                startActivity(intent);
                finish();
            }
        });


        Button btn3 = findViewById(R.id.btn3);
        btn3.setText("select and delete");
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TestActivity.class);
                startActivity(intent);
                finish();
            }
        });


        Button btn4 = findViewById(R.id.btn4);
        btn4.setText("open gallery");
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 1);

            }
        });

        Button btn5 = findViewById(R.id.btn5);
        btn5.setText("login");
        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();

            }
        });



    }

    // 갤러리 버튼 눌렀을 때 동작하는 코드
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 1: // 갤러리에서 사진파일을 가지고 온다.
                    try {
                        Log.d(TAG, "onActivityResult: 1");
                        // 사진 데이터의 Uri를 가져온다.
                        Uri currImageURI = data.getData();

                        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                        intent.setData(currImageURI);
                        startActivity(intent);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

            }
        }

    }



}
