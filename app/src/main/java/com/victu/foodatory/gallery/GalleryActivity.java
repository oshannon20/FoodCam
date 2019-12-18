package com.victu.foodatory.gallery;

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.victu.foodatory.R;
import com.victu.foodatory.gallery.model.DateItem;
import com.victu.foodatory.gallery.model.GeneralItem;
import com.victu.foodatory.gallery.model.ImageData;
import com.victu.foodatory.gallery.model.ListItem;
import com.victu.foodatory.utils.RetrofitConnection;
import com.victu.foodatory.utils.RetrofitInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class GalleryActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, GalleryAdapter.GalleryAdapterListener {
    public static final String TAG = "GalleryActivity";

    RecyclerView recyclerView;
    ArrayList<ImageData> imageDataArrayList; // 이미지 정보를 담은 list
    List<ListItem> consolidatedList = new ArrayList<>(); // 날짜와 이미지 담은 최종 list
    GalleryAdapter mAdapter;

    private ActionModeCallback actionModeCallback;
    private ActionMode actionMode;

    Retrofit retrofit;
    RetrofitInterface uploadAPIs;
    int userId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        userId = getSharedPreferences("login", Activity.MODE_PRIVATE).getInt("userId", 0);
        Log.d(TAG, "onCreate: " + userId);

        retrofit = RetrofitConnection.getRetrofitClient(this);
        uploadAPIs = retrofit.create(RetrofitInterface.class);

        imageDataArrayList= new ArrayList<>();

        initViews();
        requestPhoto();




    }

    private void requestPhoto() {
        Call call = uploadAPIs.getMealPhoto(32);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call call, Response response) {
                if(response.isSuccessful()){
                    Log.d(TAG, "requestPhoto: success " + response.message());
                    // response 데이터를 json에 담아준다.

                    try {
                        JSONArray jsonArray = new JSONArray(response.body().toString());
                        Log.e(TAG, "onResponse: " + jsonArray);

                        for(int i=0; i<jsonArray.length(); i++){
                            int foodNo = jsonArray.getJSONObject(i).getInt("food_no");
                            String photoPath = jsonArray.getJSONObject(i).getString("meal_photo_path");
                            String mealDate = jsonArray.getJSONObject(i).getString("meal_date");
                            String date = mealDate.substring(0,13);

                            ImageData imageData = new ImageData(foodNo, photoPath, date);
                            imageDataArrayList.add(imageData);
                        }
                        Log.d(TAG, "onResponse: " + imageDataArrayList);
                        setRecyclerView();


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }else{
                    Log.d(TAG, "requestPhoto: failure " + response.message());
                }

            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Log.d(TAG, "requestPhoto: onFailure" + t.getMessage());
            }
        });


    }

    private void initViews() {
        // 툴바
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 액션모드 설정
        actionModeCallback = new ActionModeCallback();

        // 리사이클러뷰 설정
        recyclerView = findViewById(R.id.recycler_gallery);

        // 리사이클러뷰 레이아웃 매니저 설정
        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mAdapter.getItemViewType(position)) {
                    case 0: // viewType이 날짜일 경우
                        return 4;

                    case 1: // viewType이 이미지일 경우
                        return 1;

                    default:
                        return -1;
                }
            }
        });
        recyclerView.setLayoutManager(layoutManager);

    }


    /**
     * consolidatedList에 데이터를 넣어주어 리사이클러뷰를 생성한다.
     */

    private void setRecyclerView() {

      //  imageDataArrayList = databaseHandler.selectPhotos(); // SQLite에서 데이터를 가져온다.

        HashMap<String, List<ImageData>> groupedHashMap = groupDataIntoHashMap(imageDataArrayList);

        for (String date : groupedHashMap.keySet()) {
            DateItem dateItem = new DateItem();
            dateItem.setDate(date);
            consolidatedList.add(dateItem);

            for (ImageData imageData : groupedHashMap.get(date)) {
                GeneralItem generalItem = new GeneralItem();
                generalItem.setImageData(imageData);
                consolidatedList.add(generalItem);
            }
        }

        sortList();

        // mAdapter 설정
        mAdapter = new GalleryAdapter(this, consolidatedList, this);
        recyclerView.setAdapter(mAdapter);

    }

    //TODO: 날짜 역순으로 나타나도록 할 것.
    private void sortList() {
    }


    private HashMap<String, List<ImageData>> groupDataIntoHashMap(List<ImageData> imageDataList) {

        HashMap<String, List<ImageData>> groupedHashMap = new HashMap<>();

        for (ImageData imageData : imageDataList) {

            String hashMapKey = imageData.getDate();

            if (groupedHashMap.containsKey(hashMapKey)) {
                // The key is already in the HashMap; add the imageData object
                // against the existing key.
                groupedHashMap.get(hashMapKey).add(imageData);
            } else {
                // The key is not there in the HashMap; create a new key-value pair
                List<ImageData> list = new ArrayList<>();
                list.add(imageData);
                groupedHashMap.put(hashMapKey, list);
            }
        }
        Log.d(TAG, "groupDataIntoHashMap: " + groupedHashMap);
        return groupedHashMap;
    }

    @Override
    public void onRefresh() {
        // swipe refresh is performed, fetch the messages again
        // 페이징. 데이터 가져오기.
    }

    /**
     *      ===================== 툴바 관련 ====================
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    /**
     * 툴바에서 check버튼을 클릭했을 때 actionMode가 활성화된다.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_check) {
            actionMode = startSupportActionMode(actionModeCallback);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 어댑터 리스너
     */

    /**
     * 선택 아이콘 클릭시
     */
    @Override
    public void onIconClicked(int position) {
        Log.d(TAG, "onIconClicked: " + position);
        Toast.makeText(this, "onIconClicked", Toast.LENGTH_SHORT).show();

        // actionMode가 활성화 되어 있지 않은 경우
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback);
        }

        toggleSelection(position);
    }

    /**
     * actionMode 활성화) 클릭한 사진을 선택한다.
     * actionMode 비활성화) 다른 액티비티로 이동해서 원본사진 보여준다.
     */

    @Override
    public void onPhotoClicked(int position) {
        Log.d(TAG, "onPhotoClicked: " + position);
        Toast.makeText(this, "onPhotoClicked", Toast.LENGTH_SHORT).show();
        if (mAdapter.getSelectedItemCount() > 0) {
            enableActionMode(position);
        } else {
            //TODO: 다른 액티비티로 이동한다.
        }
    }


    /**
     * 갤러리 사진 길게 눌렀을 때, action Mode가 활성화되며
     * 누른 사진이 선택된다.
     */
    @Override
    public void onPhotoLongClicked(int position) {
        enableActionMode(position);
    }


    /**
     *      ==================== actionMode관련 ==========================
     */


    private void enableActionMode(int position) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback);
        }
        toggleSelection(position);
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);

        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();

        }

    }


    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_action_mode, menu);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    requestDelete();
                    mode.finish();
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.d(TAG, "onDestroyActionMode: ");
            actionMode = null;
            mAdapter.clearSelections();
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    //mAdapter.resetAnimationIndex();
                    // mAdapter.notifyDataSetChanged();
                }
            });

        }
    }



    /**
     * 서버로 사진 삭제 요청
     */
    private void requestDelete() {
        List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
        JsonArray jsonArray = new JsonArray();

        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            int no = selectedItemPositions.get(i);

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("meal_photo_path",imageDataArrayList.get(no).getFilePath());
            jsonArray.add(jsonObject);

            mAdapter.removeData(no);
        }
        Log.d(TAG, "requestDelete: " + jsonArray);

        JsonObject requestJson = new JsonObject();
        requestJson.add("photo", jsonArray);
        Log.d(TAG, "requestDelete: " + requestJson);


        Call call = uploadAPIs.deletePhoto(requestJson);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if(response.isSuccessful()){
                    Log.d(TAG, "requestDelete: " + response.message());

                    mAdapter.notifyDataSetChanged();

                }
                else{
                    Log.d(TAG, "requestDelete: " + response.message());
                }

            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Log.d(TAG, "requestDelete: onFailure " + t.getMessage());

            }
        });

    }






    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
