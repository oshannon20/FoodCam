package com.victu.foodatory.detail;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.victu.foodatory.R;
import com.victu.foodatory.utils.RetrofitConnection;
import com.victu.foodatory.utils.RetrofitInterface;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class FoodDetailActivity extends AppCompatActivity {
    private static final String TAG = "FoodDetailActivity";

    TextView txt_name, txt_category, txt_calorie, txt_weight;
    TextView txt_sodium, txt_carbohydrate, txt_sugar, txt_fat;
    TextView txt_saturated_fat, txt_trans_fat, txt_cholesterol, txt_protein;

    TextView percent_sodium, percent_carbohydrate, percent_sugar, percent_fat, percent_saturated_fat, percent_trans_fat, percent_cholesterol, percent_protein;



    String name, group;
    double calorie, carbohydrate, sugar, protein, fat, transFat, saturatFat, cholesterol, weight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_detail);

        int foodNo = getIntent().getIntExtra("food_no", 1);
        Log.d(TAG, "onCreate: " + foodNo);

        initViews();


        // RetrofitConnection 클래스에서 레트로핏 객체를 가져온다.
        Retrofit retrofit = RetrofitConnection.getRetrofitClient(this);
        // RetrofitInterface를 생성한다.
        RetrofitInterface uploadAPIs = retrofit.create(RetrofitInterface.class);
        Call call = uploadAPIs.getFoodDetail(foodNo);

        call.enqueue(new Callback<List<FoodNutritionData>>() {

            @Override
            public void onResponse(Call<List<FoodNutritionData>> call, Response<List<FoodNutritionData>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: successful " + response.message());

                    List<FoodNutritionData> data = response.body();
                    FoodNutritionData foodNutrition = data.get(0);

                    viewData(foodNutrition);



                } else {
                    Log.d(TAG, "onResponse: failure");
                    Log.d(TAG, "onResponse: " + response.errorBody());
                }

            }

            @Override
            public void onFailure(Call<List<FoodNutritionData>> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });


    }

    private void viewData(FoodNutritionData foodNutrition) {
        txt_name.setText(foodNutrition.getFOOD_NAME());
        //txt_category.setText(foodNutrition.getFOOD_GROUPS());

        txt_weight.setText(foodNutrition.getWEIGHT() + getString(R.string.g));
        txt_calorie.setText(foodNutrition.getENERC() + getString(R.string.kcal));

        Log.d(TAG, "onResponse: " + foodNutrition.getCHOTDF());
        Log.d(TAG, "onResponse: " + foodNutrition.getPROCNP());
        Log.d(TAG, "onResponse: " + foodNutrition.getNA());
        Log.d(TAG, "onResponse: " + foodNutrition.getSUGAR());
        Log.d(TAG, "onResponse: " + foodNutrition.getFAT());

        float carbohydrate = foodNutrition.getCHOTDF();
        int pCarbohydrate = (int) (100*(carbohydrate/324));

        txt_carbohydrate.setText( carbohydrate + "g");
        txt_sugar.setText(foodNutrition.getSUGAR() + "g");
        txt_sodium.setText( foodNutrition.getNA() + "mg");
        txt_cholesterol.setText( foodNutrition.getCHOLE() + "mg");
        txt_protein.setText( foodNutrition.getPROCNP() + "g");
        txt_fat.setText( foodNutrition.getFAT() + "g");
        txt_saturated_fat.setText( foodNutrition.getFASATF() + "g");
        txt_trans_fat.setText( foodNutrition.getFATRNF() + "g");
        txt_cholesterol.setText( foodNutrition.getCHOLE() + "mg");

        // DV

        percent_carbohydrate.setText(pCarbohydrate+ "%");


    }

    private void initViews() {

        txt_name = findViewById(R.id.txt_name);
        txt_category = findViewById(R.id.txt_category);
        txt_calorie = findViewById(R.id.txt_calorie);
        txt_weight = findViewById(R.id.txt_weight);

        txt_sodium = findViewById(R.id.txt_sodium);
        txt_carbohydrate = findViewById(R.id.txt_carbohydrate);
        txt_sugar = findViewById(R.id.txt_sugar);
        txt_fat = findViewById(R.id.txt_fat);
        txt_saturated_fat = findViewById(R.id.txt_saturated_fat);
        txt_trans_fat = findViewById(R.id.txt_trans_fat);
        txt_cholesterol = findViewById(R.id.txt_cholesterol);
        txt_protein = findViewById(R.id.txt_protein);

        percent_carbohydrate = findViewById(R.id.percent_carbohydrate);


    }
}

