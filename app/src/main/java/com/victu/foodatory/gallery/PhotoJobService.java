package com.victu.foodatory.gallery;

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.google.gson.JsonObject;
import com.victu.foodatory.FoodNonfoodClassifier;
import com.victu.foodatory.camera.CameraActivity;
import com.victu.foodatory.utils.DatabaseHandler;
import com.victu.foodatory.utils.RetrofitConnection;
import com.victu.foodatory.utils.RetrofitInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


/**
 *
 * 참고: https://stackoverflow.com/questions/50872557/oreo-jobscheduler-not-working-when-new-picture-is-taken-by-the-camera
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class PhotoJobService extends JobService {
    public static final String TAG = "PhotoJobService";

   DatabaseHandler databaseHandler;

    // Path segments for image-specific URIs in the provider.
    static final List<String> EXTERNAL_PATH_SEGMENTS
            = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.getPathSegments();

    // The columns we want to retrieve about a particular image
    static final String[] PROJECTION = new String[] {
            MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATE_TAKEN
    };

    static final int PROJECTION_ID = 0;
    static final int PROJECTION_DATA = 1;
    static final int PROJECTION_TITLE = 2;
    static final int PROJECTION_TIME = 3;

    // 카메라가 사진을 저장하는 외부 저장 폴더
    static final String DCIM_DIR = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM).getPath();


    JobParameters mRunningParams;

    // 이 일이 현재 schedule 되고 있는지 여부를 확인한다.
    public static boolean isScheduled(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        List<JobInfo> jobs = js.getAllPendingJobs();
        if (jobs == null) {
            return false;
        }
        for (int i=0; i<jobs.size(); i++) {
            if (jobs.get(i).getId() == 11) {
                return true;
            }
        }
        return false;
    }

    // 만약 schedule 되고 있다면, 이 job을 취소한다.
    public static void cancelJob(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        js.cancel(11);
    }



    /*
    *  JobService 는 개발자가 작업을 정의해야 하는 클래스이다.
    * JobScheduler는 예약된 작업을 실행할 때, onStartJob을 호출한다. (중지는 onStopJob)
    *  JobScheduler 가 호출하는 onStartJob 등의 메소드는 모두 메인스레드에서 실행된다.
    * 따라서 작업을 처리하는 다른 서비스나 스레드를 만들어 무거운 작업을 위임해야 한다.
     * */

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.e("PhotoJobService", "JOB STARTED!");
        mRunningParams = params;

        databaseHandler=new DatabaseHandler(getApplicationContext());

        // Uri[] getTriggeredContentUris() : 이 Job에서 연결된 URI를 배열로 반환한다.
        // 만약 연결된 URI가 없다면 null을 리턴한다.
        //String[] getTriggeredContentAuthorities() : 이 Job에서 연결된 URI들의 권한을 배열로 반환한다.
        // 반환된 결과가 null이 아니라면, getTriggeredContentUris() 메서드를 사용하면 된다.


        // Did we trigger due to a content change?
        if (params.getTriggeredContentAuthorities() != null) {
            boolean rescanNeeded = false;
            if (params.getTriggeredContentUris() != null) {
                // If we have details about which URIs changed, then iterate through them
                // and collect either the ids that were impacted or note that a generic
                // change has happened.
                // 어떤 URI가 변경되었는지에 대한 세부정보가 있을 경우,
                // 반복문을 통해 영향을 받은 id를 수집하거나, 일반적이 변경이 발생되었음을 알림받는다.

                ArrayList<String> idList = new ArrayList<>();
                for (Uri uri : params.getTriggeredContentUris()) {
                    List<String> path = uri.getPathSegments();
                    if (path != null && path.size() == EXTERNAL_PATH_SEGMENTS.size()+1) {
                        // This is a specific file.
                        idList.add(path.get(path.size()-1));
                        Log.d(TAG, "onStartJob: " + path.get(path.size()-1)); //3855
                    } else {
                        // 일반적인 변화가 있을 경우
                        rescanNeeded = true;
                    }
                }

                if (idList.size() > 0) {
                    // If we found some ids that changed, we want to determine what they are.
                    // First, we do a query with content provider to ask about all of them.
                    StringBuilder selection = new StringBuilder();
                    for (int i=0; i<idList.size(); i++) {
                        if (selection.length() > 0) {
                            selection.append(" OR ");
                        }
                        selection.append(MediaStore.Images.ImageColumns._ID);
                        selection.append("='");
                        selection.append(idList.get(i)); // 3855
                        selection.append("'");
                        Log.d(TAG, "onStartJob: " + selection.toString());
                    }

                    // Now we iterate through the query, looking at the filenames of
                    // the items to determine if they are ones we are interested in.
                    Cursor cursor = null;
                    boolean haveFiles = false;
                    try {
                        cursor = getContentResolver().query(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                PROJECTION, selection.toString(), null, null);

                        while (cursor.moveToNext()) {
                            // DCIM 폴더에 있는 파일만 본다.
                            String filePath = cursor.getString(PROJECTION_DATA);
                            Log.d(TAG, "onStartJob:" + filePath); // /storage/emulated/0/DCIM/Camera/20191210_210728.jpg

                            /* 1. 이미지 파일인지 확인한다.
                             * filePath가 /storage/emulated/0/DCIM 로 시작하는 경우 갤러리 내 이미지 파일이다.
                             * 따라서 haveFiles 값을 true로 변경해주고, 이미지를 가져온다.
                             */
                            if (filePath.startsWith(DCIM_DIR)) {
                                Log.d(TAG, "onStartJob:DCIM_DIR = " + DCIM_DIR);
                                if (!haveFiles) {
                                    haveFiles = true;
                                }


                                String stringUri = "content://media/external/images/media/" + cursor.getInt(PROJECTION_ID);
                                Log.d(TAG, "onStartJob: " + stringUri);

                                Uri uri = Uri.parse(stringUri);
                                Log.d(TAG, "onStartJob: " + uri);

                                // 2. 음식 사진인지 확인하기 위해 URI를 가져온다.
                                if(checkFood(uri)){
                                    // 음식 사진이라면 DB에 저장한다.
//                                    String name = cursor.getString(PROJECTION_TITLE); // 20191210_222933.jpg
//                                    Log.d(TAG, "onStartJob: " + name);

                                    SimpleDateFormat dateFormat = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
                                    SimpleDateFormat timeFormat = new SimpleDateFormat ( "HH");

                                    long dateAdded = cursor.getLong(PROJECTION_TIME); // 1576041811811
                                    Log.d(TAG, "onStartJob: " + dateAdded);

                                    Calendar calendar = Calendar.getInstance();
                                    calendar.setTimeInMillis(dateAdded);

                                    String date = dateFormat.format(calendar.getTime());
                                    int currentTime = Integer.parseInt(timeFormat.format(calendar.getTime()));
                                    Log.d(TAG, "onStartJob: " + date);
                                    Log.d(TAG, "onStartJob: " + currentTime);

                                    String time;

                                    // 현재시간에 따라 식사시간을 설정해 spinner에 나타낸다.
                                    // 06-11시 아침, 11-14시 점심, 17-21 저녁, 그 외 간식
                                    if (currentTime >= 6 && currentTime <= 11) {
                                        time = "아침";
                                    } else if (currentTime >= 11 && currentTime <= 14) {
                                        time = "점심";
                                    } else if (currentTime >= 17 && currentTime <= 21) {
                                        time = "저녁";
                                    } else {
                                        time = "간식";
                                    }
                                    Log.d(TAG, "onStartJob: " + time);

                                    // 서버로 이미지 파일을 전송하여 저장한다.
                                    uploadToServer(filePath, time, date);

//                                    databaseHandler.insertPhoto(name, filePath, stringUri, date, time);
//                                    Log.d(TAG, "onStartJob: " + databaseHandler.showPhotos());

                                }

                            }
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "onStartJob: " + e.getMessage());
                        Log.e(TAG, "onStartJob: " + "Error: no access to media! ");
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
            } else {
                /*
                * 많은 변화가 한 번에 일어날 경우, URI에 대한 디테일을 가져 올 수 없으므로, rescan 해야 함을 알린다.
                * We don't have any details about URIs (because too many changed at once),
                * so just note that we need to do a full rescan.
                * */
                rescanNeeded = true;
            }

            /* 이미지를 다시 스캔할 필요성이 있을 때 */
            if (rescanNeeded) {
                //TODO: rescan 필요시 동작 작성
              //  Toast.makeText(this, "photos rescan needed", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onStartJob: Photos rescan needed!");

            }

        }

        else {
           // Toast.makeText(this, "no photos content", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onStartJob: (No photos content)");
        }

        /* jobFinished를 호출하면 JobScheduler에게 작업이 완료되었다는 것을 알려줍니다.
        * params는 완료된 Job에 대한 정보이고, wantsReschedule는 작업이 재실행되어야하는지를 의미합니다.
         */
        jobFinished(params, false); /*reschedule*/
        scheduleCameraJob(false);  /*immediate*/

        // true를 리턴하면 서비스는 아직 실행 중이고 다른 쓰레드에서 동작하고 있다는 것을 의미합니다.
        return true;
    }

    /**
     *
     * @param filePath
     * 참고: https://stackoverflow.com/questions/54586960/retrofit-post-request-with-form-data
     */
    private void uploadToServer(String filePath, String time, String date) {

        int userId = getSharedPreferences("login", Activity.MODE_PRIVATE).getInt("userId", 0);
        Log.d(TAG, "uploadToServer: userId " + userId);

        Log.d(TAG, "uploadToServer: 1");
        Retrofit retrofit = RetrofitConnection.getRetrofitClient(this);
        RetrofitInterface uploadAPIs = retrofit.create(RetrofitInterface.class);

        File file = new File(filePath);

        // 이미지 파일이 포함된 RequestBody를 생성한다.
        RequestBody fileReqBody = RequestBody.create(MediaType.parse("image/*"), file);

        // fileReqBody, 파일이름, part이름이 포함된 MultipartBody.Part를 생성한다.
        MultipartBody.Part part = MultipartBody.Part.createFormData("upload", file.getName(), fileReqBody);

        final Map<String, RequestBody> map = new HashMap<>();
        try {
            RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
            map.put("upload\"; filename=\"" + file.getName() + "\"", fileBody);
            map.put("uid", RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(userId)));
            map.put("meal_time", RequestBody.create(MediaType.parse("multipart/form-data"), time));
            map.put("meal_date", RequestBody.create(MediaType.parse("multipart/form-data"), date));

        } catch (Exception e) {
            e.printStackTrace();
        }


        Call call = uploadAPIs.postImageBackground(part, map);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    // 성공적으로 서버에서 데이터 불러왔을 때
                    Log.d(TAG, "uploadToServer onResponse: " + response.message());
                    Log.d(TAG, "uploadToServer onResponse: " + response.code());

                } else {
                    // 서버와 연결은 되었으나, 오류가 발생했을 때
                    Log.d(TAG, "uploadToServer: onResponse failure " + response.message());

                }
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                // 서버와 연결 실패
                Log.d(TAG, "uploadToServer: onFailure" + t.getMessage());
            }
        });
    }




    private boolean checkFood(Uri imageUri) {
        // 약 10초 소요
        Bitmap scaledBitmap;

        try {
            //Classifier 생성
            FoodNonfoodClassifier fnf_classifier = new FoodNonfoodClassifier(getApplicationContext());

            //uri에서 bitmap 생성. 나중에 사용자한테 보여줄 때도 사용
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri); //uri로 생성한 bitmap

            //tf모델에 맞게 300x300 으로 resize
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);

            //모델 실행
            //결과는 <String,Float> 형태의 Map으로 온다
            Map<String, Float> resultMap = fnf_classifier.runInference(scaledBitmap);


            //결과 Map -> String 변환
            if(resultMap != null){
                float foodRatio = resultMap.get("food");
                Log.d(TAG, "checkFood: " + foodRatio);

                // 음식이다.
                //TODO: 적절한 값 설정
                if(foodRatio>0.1){
                    Log.d(TAG, "checkFood: 사진에 음식 있음");
                    return true;
                }else{
                    Log.d(TAG, "checkFood: 사진에 음식 없음");
                }

            }else{
                return false;
            }

        }catch(IOException e){e.printStackTrace();}


       return false;
    }

    public static Uri getImageContentUri(Context context, String absPath) {
        Log.v(TAG, "getImageContentUri: " + absPath);

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                , new String[] { MediaStore.Images.Media._ID }
                , MediaStore.Images.Media.DATA + "=? "
                , new String[] { absPath }, null);

        if (cursor != null && cursor.moveToFirst()) {
            Log.d(TAG, "getImageContentUri: 1");
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI , Integer.toString(id));

        } else if (!absPath.isEmpty()) {
            Log.d(TAG, "getImageContentUri: 2");
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, absPath);
            return context.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            Log.d(TAG, "getImageContentUri: 3");
            return null;
        }
    }


    /**
     * 작업이 중단될 때 호출된다.
     * 예를 들어, 충전 중일 때만 동작할 수 있는 job이 있다고 해보자.
     * 충전중이라서 job이 실행중이다가 충전이 종료되었을 경우 jobscheduler는 onStopJob을 호출한다.
     * @param params
     * @return true(작업 재실행), false(작업이 다시 스케쥴링 되지 않음)
     */
     @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void scheduleCameraJob(Boolean Immediate) {
        Log.d(TAG, "scheduleCameraJob: ");
        final Uri MEDIA_URI = Uri.parse("content://" + MediaStore.AUTHORITY + "/");
        Log.d(TAG, "scheduleCameraJob: MEDIA_URI = " + MEDIA_URI); //  MEDIA_URI = content://media/

        // 작업이 언제, 어떤 상황에서 실행되어야 하는지 JobInfo에 정의한다.
        // JobInfo가 JobScheduler 에 전달되면, JobScheduler 는 적당한 때에 JobService를 실행시킨다.
        JobInfo.Builder builder = new JobInfo.Builder(11,
                new ComponentName(this, PhotoJobService.class.getName()));

        /*
         * JobInfo.TriggerContentUri() : content URI의 변화를 감지하기 위해 요구되는 파라미터들을 캡슐화한다.
         * Note: TriggerContentUri() 메서드는 setPeriodic(), setPersisted() 메서드와 같이 사용할 수 없다.
         *
         * JobInfo.Builder.addTriggerContentUri() : TriggerContentUri 객체를 JobInfo로 전달한다.
         * ContentObserver는 캡슐화된 content URI를 모니터링한다.
         * 만약 Job과 관련된 TriggerContentUri가 여러개 있으면, 시스템은 한개의 URI가 변하더라도 콜백을 전달한다.
         *
         * 어떤 URI의 하위 컨텐트의 변화를 감지하고 싶으면, TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS 플래그를 추가하면 된다.
         * 이 플래그는, ContentResolver.registerContentObserver()메서드의 매개변수인 notifyForDescendants와 대응하는 역할을 한다.
         *
         *  컨텐트의 변경을 계속 감지하려면, JobService가 콜백을 끝내기 전에 새로운 JobInfo 인스턴스를 만들어 스케쥴링한다.
         */

        // Look for specific changes to images in the provider.
        builder.addTriggerContentUri(new JobInfo.TriggerContentUri(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));

        // Also look for general reports of changes in the overall provider.
        builder.addTriggerContentUri(new JobInfo.TriggerContentUri(MEDIA_URI, 0));


        // immediate = false임
        // 이거 굳이 필요?
        if (Immediate) {
            // 10분의 1초 이내에 모든 media change를 가져온다.
            builder.setTriggerContentUpdateDelay(1); // 컨텐츠 변경이 감지 될 때부터 작업이 스케줄 될 때까지의 delay
            builder.setTriggerContentMaxDelay(100); // 컨텐츠 변경이 처음 감지 된 시점부터 작업이 스케줄 될 때까지 허용되는 max delay
        } else {
            builder.setTriggerContentUpdateDelay(1);
            builder.setTriggerContentMaxDelay(100);
        }

        JobInfo myCameraJob  = builder.build();
        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int result =jobScheduler.schedule(myCameraJob);
        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.e(TAG," JobScheduler OK");
        } else {
            Log.e(TAG," JobScheduler fails");
        }
    }
}