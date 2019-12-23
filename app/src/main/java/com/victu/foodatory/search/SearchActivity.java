package com.victu.foodatory.search;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.victu.foodatory.R;
import com.victu.foodatory.camera.CameraResultAdapter;
import com.victu.foodatory.camera.DetectionData;
import com.victu.foodatory.utils.RetrofitConnection;
import com.victu.foodatory.utils.RetrofitInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SearchActivity extends AppCompatActivity {
    public static final String TAG = "SearchActivity";

    // 뷰
    private RecyclerView recyclerView;

    private Retrofit retrofit;
    private RetrofitInterface uploadAPIs;

    // 리사이클러뷰
    private ArrayList<RecommendationData> recommendationList;
    private SearchAdapter searchAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        retrofit = RetrofitConnection.getRetrofitClient(this);
        uploadAPIs = retrofit.create(RetrofitInterface.class);


        int foodNo = getIntent().getIntExtra("foodNo", 0);
        Log.d(TAG, "onCreate: " + foodNo);

        // foodNo=0이 아닌 경우 추천음식을 띄워준다.
        //TODO: 현재 test중이라 ==0이지만 추후 !=0으로 수정해야 함
        if(foodNo==0){
            String filePath = getIntent().getStringExtra("filePath");
            Log.d(TAG, "onCreate: " + filePath);

            recommendationList = new ArrayList<>();
            getRecommendations(filePath);

        }

        initViews();

    }

    private void getRecommendations(String filePath) {
        // 파일경로로부터 파일 객체를 생성한다.
        File file = new File(filePath);
        RequestBody fileReqBody = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("upload", file.getName(), fileReqBody);
        final RequestBody description = RequestBody.create(MediaType.parse("text/plain"), "description");

        Call call = uploadAPIs.foodRecommendation(part, description);

        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if(response.isSuccessful()){
                    Log.d(TAG, "onResponse: success " + response.code() + response.message());

                    String result = response.body().toString();
                    Log.d(TAG, "onResponse: " + result);

                    try {
                        JSONArray jsonArray = new JSONObject(result).getJSONArray("recommend_list");
                        Log.d(TAG, "onResponse: " + jsonArray);

                        for(int i=0; i<jsonArray.length(); i++){
                            String name = jsonArray.get(i).toString();
                            Log.d(TAG, "onResponse: " + name);
                            RecommendationData recommendationData = new RecommendationData(name, 0);
                            recommendationList.add(recommendationData);
                        }

                        setRecyclerView();


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
                else{
                    Log.d(TAG, "onResponse: failure " + response.code() + response.message());

                }

            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());

            }
        });

    }

    private void setRecyclerView() {
        recyclerView = findViewById(R.id.recycler_search_result);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        searchAdapter = new SearchAdapter(this, recommendationList);

        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(searchAdapter);
    }


    private void initViews() {

    }


}
