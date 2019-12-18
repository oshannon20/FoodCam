package com.victu.foodatory.home;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import com.victu.foodatory.camera.CameraActivity;
import com.victu.foodatory.gallery.GalleryActivity;
import com.victu.foodatory.home.model.Meal;
import com.victu.foodatory.detail.FoodDetailActivity;
import com.victu.foodatory.R;
import com.victu.foodatory.settings.SettingsActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.victu.foodatory.home.model.HomeData;
import com.victu.foodatory.user.UserBodyData;
import com.victu.foodatory.utils.RetrofitConnection;
import com.victu.foodatory.utils.RetrofitInterface;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.kakao.util.helper.Utility.getPackageInfo;

/**
 * 홈 화면 액티비티이다.
 * 해당 날짜에 섭취한 칼로리, 탄/단/지 통계를 볼 수 있다.
 * 날짜/시간별로 섭취한 음식의 정보와 사진을 볼 수 있다.
 *
 * @author Shang
 * @version 1.0, 작업 내용
 */

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    // 뷰
    ImageView img_home, img_back, img_next;
    LinearLayout bottom_menu2, bottom_menu3, bottom_menu4, bottom_menu5;
    TextView txt_home, txt_calorie, txt_cho, txt_pro, txt_fat, txt_date;
    ProgressDialog pDialog;


    int[] calorieId = new int[]{R.id.txt_calorieB, R.id.txt_calorieL, R.id.txt_calorieD, R.id.txt_calorieS};
//    TextView txt_calorieB, txt_calorieL, txt_calorieD, txt_calorieS;

    PieChart pieChart;
    ProgressBar progressBar;

    // 리사이클러뷰
    public TimeAdapter adapter;
    Integer[] recyclerId = new Integer[]{R.id.recycler_breakfast, R.id.recycler_lunch, R.id.recycler_dinner, R.id.recycler_snack};
    String[] time = {"아침", "점심", "저녁", "간식"};

    // 차트 기준데이터
    int mCalorie, mCarbohydrate, mProtein, mFat;

    // 서버 전송 파라미터
    private int userId;
    String selectedDate; // 선택한 날짜

    Retrofit retrofit;
    RetrofitInterface uploadAPIs;

    SimpleDateFormat dateFormat;
    Calendar calendar;

    //TODO: 리사이클러뷰 클릭 리스너 설정해야 함


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        retrofit = RetrofitConnection.getRetrofitClient(this);
        uploadAPIs = retrofit.create(RetrofitInterface.class);

        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        calendar = Calendar.getInstance();

        // 1. 뷰 초기화
        initViews();
        initBottomMenu();

        // 2. 오늘 날짜, 회원 아이디 가져오기
        // 서버에 데이터 요청 -> recyclerView 생성
        SharedPreferences autoLogin = getSharedPreferences("login", Activity.MODE_PRIVATE);
        userId = autoLogin.getInt("userId", 0);
        Log.d(TAG, "onCreate: userId: " + userId);

        selectedDate = getDate(0);

        getChartData();
        getMealData();
    }

    /**
     * default로 오늘날짜를 반환하며
     * 사용자가 선택한 날짜
     */
    private String getDate(int type) {
        // back버튼 클릭한 경우
        if (type == -1) {
            Log.d(TAG, "getDate: 하루 전 날짜 계산");
            calendar.add(Calendar.DATE, type);
        }

        // next버튼 클릭한 경우
        if(type == 1) {
            Log.d(TAG, "getDate: 다음 날짜 계산");
            calendar.add(Calendar.DATE, type);
        }

        String date = dateFormat.format(calendar.getTime());
        Log.d(TAG, "getDate: " + date);
        return date;
    }


    /**
     *      =================== 서버에 데이터 요청하고 받는 코드  ========================
     */

    /**
     * 유저아이디(요청 param)를 보내서 회원의 하루권장칼로리(response)를 가져온다.
     * 응답이 완료되면 calNutrition을 호출한다.
     */
    private void getChartData() {
        Call call = uploadAPIs.getDiet(userId);
        call.enqueue(new Callback<UserBodyData>() {
            @Override
            public void onResponse(Call<UserBodyData> call, Response<UserBodyData> response) {
                Log.d(TAG, "onResponse: " + response.message());

                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        mCalorie = response.body().getCalorie();
                    }else{
                        mCalorie = 1800;
                    }
                    Log.d(TAG, "onResponse: " + mCalorie);

                    calNutritions(mCalorie);

                } else {
                    Log.d(TAG, "onResponse: 서버 오류");
                }

            }

            @Override
            public void onFailure(Call<UserBodyData> call, Throwable t) {
                Log.d(TAG, "onFailure: ");

            }
        });

    }


    /**
     * 하루권장칼로리를 서버로부터 성공적으로 가져온 다음 호출되는 메소드이다.
     *
     * @param calorie 하루권장칼로리
     */
    private void calNutritions(int calorie) {
        /*
         * 탄수화물,단백질 1g 4kcal, 지방은 1g 9kcal
         * 탄:단:지 = 5:3:2
         * */

        mCarbohydrate = (int) ((calorie * 0.5) / 4);
        mProtein = (int) ((calorie * 0.3) / 4);
        mFat = (int) ((calorie * 0.2) / 9);

        Log.d(TAG, "calNutritions: " + mCarbohydrate + "/" + mProtein + "/" + mFat);
    }


    /**
     * 서버에서 오늘날짜의 식사 리스트를 가져온다.
     */
    private void getMealData() {

        Call call = uploadAPIs.getMealData(userId, selectedDate);
        call.enqueue(new Callback<String>() {

            @Override
            public void onResponse(Call call, Response response) {
                Log.d(TAG, "onResponse: " + response.code());

                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: 성공");
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        setMealData(jsonObject);

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
     * 서버에서 받은 jsonObject 를 풀어서 arraylist에 넣어준 다음, 리사이클러뷰를 생성한다.
     *
     * @param jsonObject
     */
    //TODO: 데이터 아무것도 없을 경우 처리.
    private void setMealData(JSONObject jsonObject) {
        try {
            Log.d(TAG, "setMealData: " + jsonObject);
            int totalCalorie = 0; // 하루동안의 총 칼로리
            double totalCho = 0;
            double totalFat = 0;
            double totalPro = 0;


            for (int i = 0; i < 4; i++) {
                JSONArray jsonArray = jsonObject.getJSONArray(time[i]);             // 아침, 점심, 저녁, 간식에 먹은 음식을 담은 JSONArray
                //Log.d(TAG, "setMealData: " + jsonArray);
                // 각 식사 시간마다 음식리스트와, 영양성분 보여줌
                List<Meal> mealList = new ArrayList<>();
                int timeCalorie = 0;
                double timeCho = 0;
                double timeFat = 0;
                double timePro = 0;
                String imagePath = ""; // 일단은 식사마다 이미지 대표사진. 나중에 list로 변경

                for (int j = 0; j < jsonArray.length(); j++) {
                    JSONObject item = jsonArray.getJSONObject(j); // 각 음식의 정보를 담은 JSONObject
                    // Log.d(TAG, "setMealData:  " + item);

                    // 데이터 가져온다.
                    int calorie = item.getInt("calorie");
                    String name = item.getString("food_name");
                    Log.d(TAG, "setMealData: " + calorie + "/" + name);
                    double cho = item.getDouble("cho"); // 탄수화물
                    double fat = item.getDouble("fat"); //  지방
                    double pro = item.getDouble("pro"); // 단백질
                    imagePath = item.getString("meal_photo_path");

                    // recyclerView 에 나타낼 정보
                    Meal meal = new Meal(name, calorie);
                    mealList.add(meal);

                    timeCalorie = timeCalorie + calorie;
                    timeCho = timeCho + cho;
                    timeFat = timeFat + fat;
                    timePro = timePro + pro;

                }
                setCalorieByTime(calorieId[i], timeCalorie);
                setRecyclerView(recyclerId[i], mealList, imagePath); // 각 식사 시간마다 recyclerView 생성

                totalCalorie = totalCalorie + timeCalorie;
                totalCho = totalCho + timeCho;
                totalFat = totalFat + timeFat;
                totalPro = totalPro + timePro;
            }

            // 하루동안의 영양소 총합 계산
            Log.d(TAG, "setMealData: " + totalCalorie + "/" + totalCho + "/" + totalFat + "/" + totalPro);
            // setPieChart(totalCho, totalFat, totalPro);

            setProgressBar(totalCalorie, mCalorie);
            setPieChart(totalCho, totalFat, totalPro);
            pDialog.dismiss();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     *      ================= 뷰 관련 코드 =========================
     *      뷰 찾기, 클릭 리스너 설정, 뷰에 데이터 넣기
     */

    /**
     * 각 식사 시간마다 총 칼로리를 recyclerView 위에 보여준다.
     *
     * @param id      TextView 아이디값
     * @param calorie 총 칼로리
     */
    private void setCalorieByTime(int id, int calorie) {
        TextView textView = findViewById(id);
        String text = calorie + "kcal";
        textView.setText(text);
    }

    /**
     * 각 식사 시간마다 리사이클러뷰 생성
     *
     * @param id       식사시간 구분(아침/점심/저녁/간식)
     * @param mealList 음식 정보 들어있는 리스트
     */
    private void setRecyclerView(Integer id, List<Meal> mealList, String imagePath) {
        RecyclerView recyclerView = findViewById(id);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // notifyItemChanged() 수행시 변경사항에 대한 fade animation 수행된다.
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof DefaultItemAnimator) {
            ((DefaultItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        if(mealList.isEmpty()){
            Log.d(TAG, "setRecyclerView: 아이템 없음");
        }else{
            Log.d(TAG, "setRecyclerView: 아이템 있음");
            adapter = new TimeAdapter(HomeData.makeHomeData(mealList, imagePath));
            recyclerView.setAdapter(adapter);
            
        }
    }


    /**
     * 파이차트에 영양소(탄/단/지) 데이터를 넣어준다. 파라미터는 오늘 섭취한 영양소
     * cf. 총 기준 영양소는 멤버변수
     */
    //TODO: 영양소 초과한 경우 색상 변경. 남은 칼로리 나타낼 것.

    private void setPieChart(double totalCho, double totalFat, double totalPro) {
        // 입력값이 없는 경우, 차트 기본 셋팅
        if (totalCho == 0 && totalFat == 0 && totalPro == 0) {
            totalCho = 0.5;
            totalFat = 0.2;
            totalPro = 0.3;
        }

        // 차트 설정
        pieChart = findViewById(R.id.chart_nutrition);
        pieChart.setUsePercentValues(true); // 값 퍼센트로 보여줌
        pieChart.getDescription().setEnabled(false); // 설명 없음
        pieChart.getLegend().setEnabled(false); // 범례 표시 안 함
        pieChart.setDrawHoleEnabled(false); // 가운데 구멍 없음
        pieChart.setRotationEnabled(false); // 회전 안 됨
        pieChart.setDrawEntryLabels(false); // 라벨 표시 안함

        // 라벨별로 데이터 색을 지정해준다.
        ArrayList<Integer> colors = new ArrayList<>();

        colors.add(getResources().getColor(R.color.carbohydrate)); // 탄수화물
        colors.add(getResources().getColor(R.color.protein)); // 단백질
        colors.add(getResources().getColor(R.color.fat)); // 지방


        // 데이터 넣기
        ArrayList<PieEntry> entries = new ArrayList<>();

        entries.add(new PieEntry((float) totalCho, "탄수화물"));
        entries.add(new PieEntry((float) totalFat, "단백질"));
        entries.add(new PieEntry((float) totalPro, "지방"));

        PieDataSet dataSet = new PieDataSet(entries, null);
        dataSet.setColors(colors); // 데이터 색 지정

        // 데이터 값 표시
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));         // 비율로 표시
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.BLACK);
        pieChart.setData(data);

        // 범주 표시
        // 뷰에는 정수값으로 나타내야 하기 때문에 변환해준다.
        int carbohydrate = (int) totalCho;
        int Fat = (int) totalFat;
        int protein = (int) totalPro;

        String cho = "탄수화물 " + carbohydrate + " / " + mCarbohydrate + "g";
        String fat = "지방 " + Fat + " / " + mFat + "g";
        String pro = "단백질 " + protein + " / " + mProtein + "g";

        txt_cho.setText(cho);
        txt_fat.setText(fat);
        txt_pro.setText(pro);

    }


    /**
     * 프로그레스바에 칼로리 데이터를 넣어준다.
     *
     * @param intake 하루 섭취한 실제 칼로리
     * @param total  회원의 하루 권장섭취 칼로리
     */
    private void setProgressBar(int intake, int total) {
        String text = "칼로리 " + intake + " / " + total + "kcal";
        txt_calorie.setText(text);
        progressBar.setMax(total);
        progressBar.setProgress(intake);
    }


    private void initViews() {
        // 맨 위 날짜 선택
        img_back = findViewById(R.id.img_back);
        img_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDate = getDate(-1);
                changeDate(selectedDate);
            }
        });

        img_next = findViewById(R.id.img_next);
        img_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDate = getDate(1);
                changeDate(selectedDate);
            }
        });

        // 날짜 텍스트 클릭시 달력이 나타난다.
        txt_date = findViewById(R.id.txt_date);
        txt_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog();
            }
        });


        // 차트 관련
        progressBar = findViewById(R.id.bar_calorie);

        txt_calorie = findViewById(R.id.txt_calorie);
        txt_pro = findViewById(R.id.txt_pro);
        txt_cho = findViewById(R.id.txt_cho);
        txt_fat = findViewById(R.id.txt_fat);

        // 프로그레스 다이얼로그
        pDialog = new ProgressDialog(HomeActivity.this);
        pDialog.setMessage("잠시만 기다려주세요.");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(false);
    }

    private void openDialog() {
        CalendarDialog dialog = new CalendarDialog();
        dialog.setDialogListener(new CalendarDialog.CalendarDialogListener() {
            @Override
            public void onPositiveClicked(String date) {
                Log.d(TAG, "onPositiveClicked: " + date);
                changeDate(date);
            }
        });

        dialog.show(getSupportFragmentManager(), "dialog");
    }


    private void changeDate(String date) {
        selectedDate = date;

        //TODO: 날짜 형식 변경
        // 뷰에 보이는 날짜 형식 (11월 03일 수요일 -11월 04일 어제 - 11월 05일 오늘)
        txt_date.setText(date);

        // 프로그레스 다이얼로그
        getMealData();
        pDialog.show();
    }


    private void initBottomMenu() {

        // 뷰 찾기
        img_home = findViewById(R.id.img_home);
        txt_home = findViewById(R.id.txt_home);
        bottom_menu2 = findViewById(R.id.bottom_menu2);
        bottom_menu3 = findViewById(R.id.bottom_menu3);
        bottom_menu4 = findViewById(R.id.bottom_menu4);
        bottom_menu5 = findViewById(R.id.bottom_menu5);

        // 하단 메뉴 색상 변경
        img_home.setColorFilter(getResources().getColor(R.color.colorPrimaryDark));
        txt_home.setTextColor(getResources().getColor(R.color.colorPrimaryDark));

        // 하단 메뉴 이동 설정
        bottom_menu2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, FoodDetailActivity.class);
                startActivity(intent);
                finish();
            }
        });

        bottom_menu3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, CameraActivity.class);
                startActivity(intent);
                 finish();
            }
        });

        bottom_menu4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, GalleryActivity.class);
                startActivity(intent);
                finish();
            }
        });

        bottom_menu5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intent);
                //   finish();
            }
        });

    }


//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        adapter.onSaveInstanceState(outState);
//    }
//
//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        adapter.onRestoreInstanceState(savedInstanceState);
//    }
}
