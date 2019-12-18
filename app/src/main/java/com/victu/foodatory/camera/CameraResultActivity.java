package com.victu.foodatory.camera;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.victu.foodatory.SearchActivity;
import com.victu.foodatory.home.HomeActivity;
import com.victu.foodatory.detail.FoodDetailActivity;
import com.victu.foodatory.R;
import com.victu.foodatory.utils.RetrofitConnection;
import com.victu.foodatory.utils.RetrofitInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage;

public class CameraResultActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private static final String TAG = "CameraResultActivity";

    // 뷰
    private ImageView img_photo, img_back;
    private TextView txt_result, txt_date, txt_calorie;
    private Spinner spinner_time;
    private Button btn_done, btn_search;

    private RecyclerView recyclerView;
    private CameraResultAdapter cameraResultAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    // 서버로 넘겨줄 데이터
    private String imagePath;

    private String mTime;

    private Bitmap originalBitmap; // 원본 이미지 사진

    private ArrayList<DetectionData> detectionArrayList; // 이미지 인식 결과 담는 리스트

    Retrofit retrofit;
    RetrofitInterface uploadAPIs;

    SharedPreferences autoLogin;
    int userId;
    String token;


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
        test();
        getImage();

    }

    private void test() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);

        /**
         * https://stackoverflow.com/questions/31662416/show-collapsingtoolbarlayout-title-only-when-collapsed
         */
        AppBarLayout appBarLayout =  findViewById(R.id.appbar);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbar.setTitle("날짜");
                    isShow = true;
                } else if(isShow) {
                    collapsingToolbar.setTitle(" ");//careful there should a space between double quote otherwise it wont work
                    isShow = false;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sample_actions, menu);
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

//        txt_date = findViewById(R.id.txt_date);

//        img_back = findViewById(R.id.img_back);
//        img_back.setOnClickListener(this);

//        spinner_time = findViewById(R.id.spinner_time);
//        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
//                R.array.meal_time, android.R.layout.simple_spinner_item);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner_time.setAdapter(adapter);
//        spinner_time.setOnItemSelectedListener(this);


        btn_search = findViewById(R.id.btn_search);
        btn_search.setOnClickListener(this);

        btn_done = findViewById(R.id.btn_done);
        btn_done.setOnClickListener(this);

        // 메인 홈 화면에서 추가 버튼을 누른 경우: 인텐트에서 날짜와 시간을 받아온다.
        String time = getIntent().getStringExtra("mTime");
        String date = getIntent().getStringExtra("mDate");
       // setDate(time, date);

    }

    /**
     * 스피너 선택
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mTime = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    /**
     * 버튼 클릭 리스너 설정
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
//            case R.id.img_back:
                // 카메라액티비티로 이동한다.
//                Intent intent = new Intent(CameraResultActivity.this, CameraActivity.class);
//                startActivity(intent);
//                finish();
//                break;

            case R.id.btn_search:
                // 검색 액티비티로 이동한다.
                moveSearchAcitivy(0);

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
    private void moveSearchAcitivy(int foodNo) {
        Intent intent = new Intent(CameraResultActivity.this, SearchActivity.class);
        startActivity(intent);
        finish();
    }

    //TODO: 수량변경시 칼로리도 변한다.
    //TODO: 전체 칼로리 계산

    /**
     * 전체 칼로리를 계산하는 메소드이다.
     * 결과 arraylist에서 칼로리를 가져와 더해준다.
     */
    private void calCalorie() {
        for (int i = 0; i < detectionArrayList.size(); i++) {

        }

    }


    /**
     * 완료 버튼을 클릭했을 때, 서버로 데이터를 전송하여 식사 정보를 저장한다.
     */
    private void saveMeal() {
        // 현재 시간 계산. server에서 연월일시분초까지 요구해서 일단 이 포맷으로 보냄. 나중에 수정?
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        String date = dateFormat.format(calendar.getTime());
        Log.d(TAG, "saveMeal: " + date);


        JsonArray jsonArray = new JsonArray();

        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("mem_uid", userId);
        jsonObj.addProperty("meal_photo_path", imagePath);


        for (int i = 0; i < detectionArrayList.size(); i++) {
            JsonObject jsonObject = new JsonObject();
            // 전체 식사 정보
            jsonObject.addProperty("mem_uid", userId);
            jsonObject.addProperty("meal_time", mTime);
            jsonObject.addProperty("meal_date", date);

            // 각각의 음식 정보
            jsonObject.addProperty("food_no", detectionArrayList.get(i).getFoodNo());
            jsonObject.addProperty("food_name", detectionArrayList.get(i).getFoodName());
            jsonObject.addProperty("calorie", detectionArrayList.get(i).getmCalorie());
            jsonObject.addProperty("cho", detectionArrayList.get(i).getCarbohydrate());
            jsonObject.addProperty("pro", detectionArrayList.get(i).getProtein());
            jsonObject.addProperty("fat", detectionArrayList.get(i).getFat());
            jsonObject.addProperty("weight", detectionArrayList.get(i).getmWeight());
            //TODO: 단위도 변경하도록 수정
            jsonObject.addProperty("unit", "g");

            jsonArray.add(jsonObject);
        }
        jsonObj.add("meal", jsonArray);

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
                    Intent i = new Intent(CameraResultActivity.this, HomeActivity.class);
                    startActivity(i);
                    finish();

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


    /**
     * 오늘 날짜와 식사시간을 입력해준다.
     *
     * @param time
     * @param date
     */
//    private void setDate(String time, String date) {
//        Log.d(TAG, "setDate: " + time + "/" + date);
//        // 하단 메뉴의 카메라 버튼을 누른 경우: 시스템에서 시간을 받아와 설정해준다.
//        if (time == null || date == null) {
//            SimpleDateFormat dateFormat = new SimpleDateFormat("MM월 dd일");
//            SimpleDateFormat timeFormat = new SimpleDateFormat("HH");
//
//            Calendar calendar = Calendar.getInstance();
//            date = dateFormat.format(calendar.getTime());
//            Log.d(TAG, "setDate: " + date);
//
//            int currentTime = Integer.parseInt(timeFormat.format(calendar.getTime()));
//            Log.d(TAG, "initViews: 오늘날짜 " + date + "/시간 " + currentTime);
//
//
////            // 현재시간에 따라 식사시간을 설정해 spinner에 나타낸다.
////            // 06-11시 아침, 11-14시 점심, 17-21 저녁, 그 외 간식
////            if (currentTime >= 6 && currentTime <= 11) {
////                spinner_time.setSelection(0);
////            } else if (currentTime >= 11 && currentTime <= 14) {
////                spinner_time.setSelection(1);
////            } else if (currentTime >= 17 && currentTime <= 21) {
////                spinner_time.setSelection(2);
////            } else {
////                spinner_time.setSelection(3);
////            }
//
//        }
//        // 메인 홈 화면에서 추가 버튼을 누른 경우
//        else {
////            if (time.equals("아침")) {
////                spinner_time.setSelection(0);
////            } else if (time.equals("점심")) {
////                spinner_time.setSelection(1);
////            } else if (time.equals("저녁")) {
////                spinner_time.setSelection(2);
////            } else {
////                spinner_time.setSelection(3);
////            }
//        }
//
//        // 날짜는 TextView에 값을 넣어준다.
//        txt_date.setText(date);
//    }


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
                moveSearchAcitivy(0);
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

            }

            // 음식의 수량을 변경한 경우
            @Override
            public void onSetWeight(int position) {

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

        String filePath = intent.getStringExtra("filePath");

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
                int foodNo = 1;
//                int foodNo = item.getInt("food_no");
                int calorie = item.getInt("enerc");
                int weight = item.getInt("weight");

                int carbohydrate = item.getInt("carbohydrate");
                int protein = item.getInt("protein");
                int fat = item.getInt("fat");

                int x1 = item.getInt("x1");
                int x2 = item.getInt("x2");
                int y1 = item.getInt("y1");
                int y2 = item.getInt("y2");

                Log.d(TAG, "onResponse: " + foodName + " " + x1 + " " + x2 + " " + y1 + " " + y2);
                Log.d(TAG, "onResponse: foodNo " + foodNo + "/calorie " + calorie + "/weight " + weight);


                DetectionData detection = new DetectionData();
                detection.setFoodName(foodName);
                detection.setFoodNo(foodNo);
                detection.setmCalorie(calorie);
                detection.setmWeight(weight);

                detection.setCarbohydrate(carbohydrate);
                detection.setFat(fat);
                detection.setProtein(protein);

                detection.setX1(x1);
                detection.setX2(x2);
                detection.setY1(y1);
                detection.setY2(y2);

                detectionArrayList.add(detection);
            }

            // 라벨을 나타낸다.
            showLabel();

            // 리사이클러뷰에 인식된 음식을 나타낸다.
            buildRecyclerView();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



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
