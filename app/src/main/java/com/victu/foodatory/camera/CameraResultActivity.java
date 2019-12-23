package com.victu.foodatory.camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.victu.foodatory.search.SearchActivity;
import com.victu.foodatory.home.HomeActivity;
import com.victu.foodatory.detail.FoodDetailActivity;
import com.victu.foodatory.R;
import com.victu.foodatory.utils.RetrofitConnection;
import com.victu.foodatory.utils.RetrofitInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CameraResultActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "CameraResultActivity";

    // 뷰
    private ImageView img_photo;
    private TextView txt_calorie;
    private Button btn_done, btn_search;

    private RecyclerView recyclerView;
    private CameraResultAdapter cameraResultAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private MenuItem menu_time, menu_change_date;

    // 서버로 넘겨줄 데이터
    private String imagePath, mTime, mDate;

    // 원본 이미지 사진
    private Bitmap originalBitmap;

    // 이미지 인식 결과 담는 리스트
    private ArrayList<DetectionData> detectionArrayList;

    // 서버 요청하는 Retrofit
    Retrofit retrofit;
    RetrofitInterface uploadAPIs;

    // 회원 정보 담은 sharedPreferences
    SharedPreferences autoLogin;
    int userId;
    String token;

    // 인텐트로 넘겨받은 이미지 파일 주소
    private String filePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_result2);

        // RetrofitConnection 클래스에서 레트로핏 객체를 가져온다.
        retrofit = RetrofitConnection.getRetrofitClient(this);
        // RetrofitInterface를 생성한다.
        uploadAPIs = retrofit.create(RetrofitInterface.class);

        autoLogin = getSharedPreferences("login", Activity.MODE_PRIVATE);
        userId = autoLogin.getInt("userId", 0);
        token = autoLogin.getString("accessToken", "");
        Log.d(TAG, "onCreate: " + userId + "/" + token);


        detectionArrayList = new ArrayList<>();

        initViews();

        getImage();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sample_actions, menu);
        menu_time = menu.findItem(R.id.menu_time);
        menu_time.setTitle(mTime);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                moveHomeActivity();
                break;

            case R.id.menu_breakfast:
                mTime = "아침";
                break;

            case R.id.menu_lunch:
                mTime = "점심";
                break;

            case R.id.menu_dinner:
                mTime = "저녁";
                break;

            case R.id.menu_snack:
                mTime = "간식";
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        menu_time.setTitle(mTime);
        return true;
    }


    /**
     *                뷰 관련 코드
     */

    /**
     * 뷰 찾고 날짜와 시간 받아와서 나타내준다.
     */
    private void initViews() {
        img_photo = findViewById(R.id.img_photo);

        txt_calorie = findViewById(R.id.txt_calorie);

        btn_search = findViewById(R.id.btn_search);
        btn_search.setOnClickListener(this);

        btn_done = findViewById(R.id.btn_done);
        btn_done.setOnClickListener(this);

        // 메인 홈 화면에서 추가 버튼을 누른 경우: 인텐트에서 날짜와 시간을 받아온다.
        String time = getIntent().getStringExtra("mTime");
        String date = getIntent().getStringExtra("mDate");
        setDate(time, date);


        // 상단 바 레이아웃 설정

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);

        /**
         * https://stackoverflow.com/questions/31662416/show-collapsingtoolbarlayout-title-only-when-collapsed
         */
        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbar.setTitle(mDate);
                    collapsingToolbar.setCollapsedTitleTextColor(getResources().getColor(R.color.white));
                    isShow = true;
                } else if (isShow) {
                    collapsingToolbar.setTitle(" ");//careful there should a space between double quote otherwise it wont work
                    isShow = false;
                }
            }
        });

    }


    /**
     * 버튼 클릭 리스너 설정
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_search:
                // 검색 액티비티로 이동한다.
                moveSearchActivity(0);

                break;

            case R.id.btn_done:
                // 입력한 정보를 저장한다.
                saveMeal();
                break;

        }
    }

    /**
     * 검색 액티비티로 이동하는 메소드이다.
     * 각 아이템에서 edit버튼을 눌렀을 경우, 추천음식이 나타나고
     * 하단의 다른 음식 추가 버튼을 눌렀을 경우
     */

    /**
     * 검색 액티비티로 이동하는 메소드이다.
     * 각 아이템에서 edit 버튼을 눌렀을 경우 추천음식이 나타난다. foodNo
     * 하단의 다른 음식 추가 버튼을 눌렀을 경우 최근 검색 목록이 나타난다. foodNo=0
     *
     * @param foodNo
     */
    private void moveSearchActivity(int foodNo) {
        Intent intent = new Intent(CameraResultActivity.this, SearchActivity.class);
        intent.putExtra("foodNo", foodNo);
        intent.putExtra("filePath", filePath);
        startActivity(intent);
        //finish();
    }

    //TODO: 수량변경시 칼로리도 변한다.

    /**
     * 전체 칼로리를 계산하는 메소드이다.
     * 결과 arraylist에서 칼로리를 가져와 더해준다.
     */
    private void setCalorie() {
        int totalCalorie =0;

        for (int i = 0; i < detectionArrayList.size(); i++) {
            int getmCalorie = detectionArrayList.get(i).getmCalorie();
            totalCalorie = totalCalorie + getmCalorie;
        }

        Log.d(TAG, "calCalorie: " + totalCalorie);
        String text = totalCalorie + "kcal";
        txt_calorie.setText(text);
    }


    /**
     * 완료 버튼을 클릭했을 때, 서버로 데이터를 전송하여 식사 정보를 저장한다.
     */
    //FIXME: 토큰 만료되어서 그런가?
    private void saveMeal() {
        // 현재 시간 계산. server에서 연월일시분초까지 요구해서 일단 이 포맷으로 보냄. 나중에 수정?
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        String date = dateFormat.format(calendar.getTime());
        Log.d(TAG, "saveMeal: " + date);


        JsonArray jsonArray = new JsonArray();

        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("mem_uid", userId);
        jsonObj.addProperty("meal_photo_no", imagePath);
        jsonObj.addProperty("meal_type", mTime);
        jsonObj.addProperty("meal_datetime", date);


        for (int i = 0; i < detectionArrayList.size(); i++) {
            JsonObject jsonObject = new JsonObject();
            // 전체 식사 정보
            jsonObject.addProperty("mem_uid", userId);

            // 각각의 음식 정보
            jsonObject.addProperty("food_no", detectionArrayList.get(i).getFoodNo());
            jsonObject.addProperty("food_name", detectionArrayList.get(i).getFoodName());
            jsonObject.addProperty("food_calorie", detectionArrayList.get(i).getmCalorie());
            jsonObject.addProperty("food_carbohydrate", detectionArrayList.get(i).getCarbohydrate());
            jsonObject.addProperty("food_protein", detectionArrayList.get(i).getProtein());
            jsonObject.addProperty("food_fat", detectionArrayList.get(i).getFat());
            jsonObject.addProperty("food_amount", detectionArrayList.get(i).getmWeight());
            //TODO: 단위도 변경하도록 수정 (단위 int값. 200ml= 1 cup)
            jsonObject.addProperty("meal_unit", "g");

            jsonArray.add(jsonObject);
        }
        jsonObj.add("food", jsonArray);

        Log.d(TAG, "saveMeal: " + jsonObj);


        Call call = uploadAPIs.addMeal(token, jsonObj);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call call, Response response) {
                // 201 - Insert Success
                // 500 - Internal Server error
                Log.d(TAG, "onResponse: " + response.code());

                if (response.isSuccessful()) {
                    // 성공적으로 서버에서 데이터 불러왔을 때
                    // 메인 홈화면으로 이동한다.
                    moveHomeActivity();


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

    private void moveHomeActivity() {
        Intent i = new Intent(CameraResultActivity.this, HomeActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveHomeActivity();
    }

    /**
     * 오늘 날짜와 식사시간을 입력해준다.
     *
     * @param time
     * @param date
     */
    private void setDate(String time, String date) {
        Log.d(TAG, "setDate: " + time + "/" + date);

        // 하단 메뉴의 카메라 버튼을 누른 경우:
        // 인텐트로 전달된 값이 없으므로, 시스템에서 시간을 받아와 설정해준다.
        if (time == null || date == null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM월 dd일");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH");

            Calendar calendar = Calendar.getInstance();
            date = dateFormat.format(calendar.getTime());
            Log.d(TAG, "setDate: " + date);

            int currentTime = Integer.parseInt(timeFormat.format(calendar.getTime()));
            Log.d(TAG, "initViews: 오늘날짜 " + date + "/시간 " + currentTime);


            // 06-11시 아침, 11-14시 점심, 17-21 저녁, 그 외 간식
            if (currentTime >= 6 && currentTime <= 11) {
                mTime = "아침";
            } else if (currentTime >= 11 && currentTime <= 14) {
                mTime = "점심";
            } else if (currentTime >= 17 && currentTime <= 21) {
                mTime = "저녁";
            } else {
                mTime = "간식";
            }

        }
        // 메인 홈 화면에서 추가 버튼을 누른 경우
        else {
            if (time.equals("아침")) {
                mTime = "아침";
            } else if (time.equals("점심")) {
                mTime = "점심";
            } else if (time.equals("저녁")) {
                mTime = "저녁";
            } else {
                mTime = "간식";
            }
        }

        mDate = date;
    }


    /**
     *      인식된 음식 정보를 보여주고, 수정하는 코드
     */

    /**
     * 이미지에 음식 이름을 더하고, 이미지 아래에 리사이클러뷰를 나타낸다.
     */
    private void showLabel() {
        // 원본 이미지에 음식 이름을 더해 보여준다.
        Bitmap processedBitmap = drawLabel(originalBitmap, detectionArrayList);
        img_photo.setImageBitmap(processedBitmap);
    }

    private void buildRecyclerView() {
        recyclerView = findViewById(R.id.recycler_result);
        recyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        cameraResultAdapter = new CameraResultAdapter(this, detectionArrayList);

        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(cameraResultAdapter);


        cameraResultAdapter.setOnItemClickListener(new CameraResultAdapter.OnItemClickListener() {
            // 다른 음식으로 수정할 경우
            @Override
            public void onEditClick(int position) {

                //TODO: 일단 0으로 두고 나중에 추천음식 구현하면 foodNo 넘겨줄 것
                moveSearchActivity(0);

            }

            // 음식의 상세영양정보를 보고 싶은 경우
            @Override
            public void onInfoClick(int position) {
                Intent intent = new Intent(CameraResultActivity.this, FoodDetailActivity.class);
                intent.putExtra("food_no", detectionArrayList.get(position).getFoodNo());
                startActivity(intent);
                finish();
            }

            // 음식을 제거한 경우
            @Override
            public void onDeleteClick(int position) {
                detectionArrayList.remove(position);
                cameraResultAdapter.notifyItemRemoved(position);
                showLabel(); // 음식 지운 경우 레이블도 지워야 하므로 다시 그린다.
                setCalorie(); // 칼로리도 변경

            }

            // 수량을 변경한 경우
            @Override
            public void onWeightChanged(int position, int amount) {
                Log.d(TAG, "onWeightChanged: " + amount);
                // 기존 칼로리와 양
                int calorieBefore = detectionArrayList.get(position).getmCalorie();
                int amountBefore = detectionArrayList.get(position).getmWeight();
                Log.d(TAG, "onWeightChanged: 기존 " + calorieBefore + " " + amountBefore);

                // 변경 칼로리 = 기존 칼로리 x (변경양/기존양)
                // 가중치가 정수가 아니기 때문에 int -> float -> int 로 변경
                // int로 하면 0값 나옴.
                // https://stackoverflow.com/questions/4931892/why-does-the-division-of-two-integers-return-0-0-in-java
                float percentage = ((float) amount) / amountBefore;
                Log.d(TAG, "onWeightChanged: " + percentage);

                float cal = ((float) calorieBefore) * percentage;
                Log.d(TAG, "onWeightChanged: " + cal);

                int calorieAfter = (int) cal;
                Log.d(TAG, "onWeightChanged: " + calorieAfter);

                // 변경 칼로리와 양 arrayList에 넣어주기
                detectionArrayList.get(position).setmWeight(amount);
                detectionArrayList.get(position).setmCalorie(calorieAfter);

                Log.d(TAG, "onWeightChanged: 이후 " + calorieBefore + " " + amountBefore);

                // 전체 칼로리 다시 계산
                setCalorie();

                // recyclerView에 변경사항 나타내기
                cameraResultAdapter.notifyDataSetChanged();

            }

        });

    }

    /**
     *                사진 전송해서 결과 가져오는 코드
     **/

    /**
     * 이전 액티비티에서 사진과 결과를 가져온다.
     */
    private void getImage() {
        Intent intent = getIntent();

        filePath = intent.getStringExtra("filePath");

        String detectResult = intent.getStringExtra("result");
        Uri imageUri = intent.getData(); //사진의 uri
        Log.d(TAG, "getImage: " + filePath);
        Log.d(TAG, "getImage: " + imageUri);
        Log.d(TAG, "getImage: " + detectResult);

        try {
            originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Glide.with(this).load(imageUri).into(img_photo);
        handleResult(detectResult); // 결과를 처리한다.
    }

    /**
     * response로 온 String을 JSON에 담아 처리한다.
     * @param result CameraActivity에서 intent로 넘겨받은 인식 결과 string이다.
     */
    private void handleResult(String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            Log.e(TAG, "onResponse: " + jsonObject);

            // 파일 경로
            imagePath = jsonObject.getString("image");
            Log.d(TAG, "onResponse: " + imagePath);


            // 인식된 음식이 있다면, arrayList에 담아준다.
            JSONArray jsonArray = jsonObject.getJSONArray("detections");
            Log.d(TAG, "onResponse: " + jsonArray);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);

                String foodName = item.getString("food_name");
                //FIXME: foodNo 제대로 안 옴. (서버 확인할 것)

                int foodNo = item.getInt("food_no");
                int calorie = item.getInt("food_calorie");
                int amount = item.getInt("food_amount");

                int carbohydrate = item.getInt("food_carbohydrate");
                int protein = item.getInt("food_protein");
                int fat = item.getInt("food_fat");

                int x1 = item.getInt("x1");
                int x2 = item.getInt("x2");
                int y1 = item.getInt("y1");
                int y2 = item.getInt("y2");

                Log.d(TAG, "onResponse: " + foodName + " " + x1 + " " + x2 + " " + y1 + " " + y2);
                Log.d(TAG, "onResponse: foodNo " + foodNo + "/calorie " + calorie + "/weight " + amount);


                DetectionData detection = new DetectionData();
                detection.setFoodName(foodName);
                detection.setFoodNo(foodNo);
                detection.setmCalorie(calorie);
                detection.setmWeight(amount);

                detection.setCarbohydrate(carbohydrate);
                detection.setFat(fat);
                detection.setProtein(protein);

                detection.setX1(x1);
                detection.setX2(x2);
                detection.setY1(y1);
                detection.setY2(y2);

                detectionArrayList.add(detection);
            }

            // 리사이클러뷰에 인식된 음식을 나타낸다.
            buildRecyclerView();

            // 라벨을 나타낸다.
            showLabel();

            // 칼로리를 나타낸다.
            setCalorie();


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    //TODO: 중간값 주면 수정할 것
    //FIXME: 라벨 너무 큼
    /**
     * 인식한 음식의 이름을 이미지 위에 그려 bitmap을 반환한다.
     **/
    private Bitmap drawLabel(Bitmap originalBitmap, ArrayList<DetectionData> arrayList) {
        Bitmap newBitmap = null;

        Bitmap.Config config = originalBitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }

        newBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), config);
        Log.d(TAG, "drawLabel: " + originalBitmap.getWidth() + " " + originalBitmap.getHeight());

        // Canvas 객체는 bitmap에 무엇을 그릴지(line, circle, text 등) 정한다.
        Canvas newCanvas = new Canvas(newBitmap);

        newCanvas.drawBitmap(originalBitmap, 0, 0, null);


        for (int i = 0; i < arrayList.size(); i++) {
            int x = (arrayList.get(i).getX1() + arrayList.get(i).getX2()) / 2; // 라벨링이 위치할 x좌표
            int y = (arrayList.get(i).getY1() + arrayList.get(i).getY2()) / 2; // 라벨링이 위치할 x좌표
            Log.d(TAG, "drawLabel: " + x + " " + y);
            String label = arrayList.get(i).getFoodName(); // 음식의 이름

            // 텍스트 속성을 지정한다.
            Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintText.setColor(Color.WHITE); // 텍스트 컬러
            paintText.setTextSize(50); // 텍스트 사이즈

            Rect rectText = new Rect();
            paintText.getTextBounds(label, 0, label.length(), rectText);

            // 텍스트를 감싸는 사각형 박스 설정
            RectF mRect = new RectF();
            mRect.set(x - 10, y - 50, x + rectText.right + 10, y + 10); // 사각형의 위치를 지정

            Paint mPaint = new Paint();
            mPaint.setColor(Color.parseColor("#80000000"));
            newCanvas.drawRoundRect(mRect, 20, 20, mPaint);

            // 사각형과 텍스트를 그려준다.
            newCanvas.drawText(label, x, y, paintText);
        }


        return newBitmap;
    }


}
