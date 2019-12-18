package com.victu.foodatory.utils;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


/**
 * 서버와 통신하는 라이브러리 Retrofit 객체를 생성하는 클래스이다.
 *
 * @author Shang
 * @version 1.0
 */


public class RetrofitConnection {

    //    private static final String BASE_URL = "http://15.164.136.176/";
    private static final String BASE_URL = "https://www.foodatory.xyz/api/v1/";

    private static Retrofit retrofit;


    public static Retrofit getRetrofitClient(Context context) {
        if (retrofit == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create()) // 데이터를 Gson 형태로 받아온다.
                    .build();
        }
        return retrofit;
    }


}
