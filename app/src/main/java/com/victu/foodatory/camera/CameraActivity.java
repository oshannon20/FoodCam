package com.victu.foodatory.camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.FileCallback;
import com.otaliastudios.cameraview.PictureResult;
import com.victu.foodatory.R;
import com.victu.foodatory.home.HomeActivity;
import com.victu.foodatory.utils.RetrofitConnection;
import com.victu.foodatory.utils.RetrofitInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";

    // 레이아웃 뷰들
    private ImageView img_closed, img_flash, img_shutter, img_gallery, img_search, img_result;
    private CameraView cameraKitView;
    AlertDialog dialog;

    private String filePath;
    private Uri uri;
    private String result;

    Retrofit retrofit;
    RetrofitInterface uploadAPIs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initViews();

        // RetrofitConnection 클래스에서 레트로핏 객체를 가져온다.
        retrofit = RetrofitConnection.getRetrofitClient(this);
        // RetrofitInterface를 생성한다.
        uploadAPIs = retrofit.create(RetrofitInterface.class);


        // 권한 확인한다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 권한 상태 확인한다.
            if (!hasPermissions(PERMISSIONS)) {
                // 권한 설정이 안 되어 있다면 사용자에게 요청한다.
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

    }


    private void initViews() {
        setCamera();

     //   img_result = findViewById(R.id.img_result);

        // 셔터를 눌렀을 때
        img_shutter = findViewById(R.id.img_shutter);
        img_shutter.setOnClickListener(photoOnClickListener);
        img_shutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: ");
                cameraKitView.takePicture();
            }
        });


        // 갤러리에서 사진을 불러올 때
        img_gallery = findViewById(R.id.img_gallery);
        img_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkStoragePermission()) {
                    // 저장공간 권한 허가 안 됨. 요청해야 함.
                    requestStoargePermission();
                } else {
                    // 권한 설정 됨
                    Log.d(TAG, "onClick: 갤러리 사진 클릭");
                    pickGallery();

                }


            }
        });

    }

    /**
     * 카메라
     */
    private void setCamera() {
        cameraKitView = findViewById(R.id.camera);
        cameraKitView.setLifecycleOwner(this);
        cameraKitView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                cameraKitView.close();

                // 파일명을 설정한다. (Image_현재시간.jpg)
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String fileName = "Image_" + timeStamp + ".jpg";

                // 파일을 만든다.
                File file = new File(getFilesDir(), fileName);
                result.toFile(file, new FileCallback() {
                    @Override
                    public void onFileReady(@Nullable File file) {
                        Log.d(TAG, "onFileReady: file " + file);

                        filePath = rotatePhoto(file);

                        Context context = CameraActivity.this;
                        uri = FileProvider.getUriForFile(context, 
                                context.getPackageName() + ".provider",
                                file);

                        Log.d(TAG, "onActivityResult: " + uri);
                        Log.d(TAG, "onActivityResult: " + filePath);

                        showProgressDialog();

                        //uploadToServer(filePath);

                    }

                });


            }
        });
    }

    private String rotatePhoto(File file) {
        String filePath = file.toString();
        Log.d(TAG, "onFileReady: filePath " + filePath);

        Bitmap bmp = BitmapFactory.decodeFile(filePath);

        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }

    private Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage)
            throws IOException {

        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(input);
        else
            ei = new ExifInterface(selectedImage.getPath());

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }


    private void pickGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 1);
    }

    /**
     *          인식 관련 코드
     */


    /**
     * Retrofit 라이브러리를 이용해 이미지 파일을 전송하고 인식된 음식이름과 위치를 가져오는 메소드이다.
     *
     * @ param String 파일경로
     * @ 참고 https://androidclarified.com/android-image-upload-example/
     */
    private void uploadToServer(String filePath) {
        // 파일경로로부터 파일 객체를 생성한다.
        File file = new File(filePath);

        // 이미지 파일이 포함된 RequestBody를 생성한다.
        RequestBody fileReqBody = RequestBody.create(MediaType.parse("image/*"), file);
        Log.d(TAG, "uploadToServer: " + fileReqBody);

        // fileReqBody, 파일이름, part이름이 포함된 MultipartBody.Part를 생성한다.
        MultipartBody.Part part = MultipartBody.Part.createFormData("upload", file.getName(), fileReqBody);
        Log.d(TAG, "uploadToServer: " + part);

        // 설명이 있는 텍스트 파일이 포함된 RequestBody를 생성한다.
        final RequestBody description = RequestBody.create(MediaType.parse("text/plain"), "description");
        Log.d(TAG, "uploadToServer: " + description);


        Call call = uploadAPIs.postFoodImage(part, description);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    // 성공적으로 서버에서 데이터 불러왔을 때
                    if (response.body() != null) {
                        try {
                            // response 데이터를 json에 담아준다.
                            JSONObject jsonObject = new JSONObject(response.body().toString());
                            Log.e(TAG, "onResponse: " + jsonObject);

                            // 인식이 되었는지 여부
                            boolean detectionExists = jsonObject.getBoolean("detectionExists");
                            Log.d(TAG, "onResponse: " + detectionExists);

                            if (detectionExists) {
                                result = response.body().toString();
                                moveActivity();

                            } else {
                                // 사진에서 음식을 찾을 수 없는 경우
                                dismissProgressDialog();
                                failedToDetect();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                } else {
                    // 서버와 연결은 되었으나, 오류가 발생했을 때

                    dismissProgressDialog();
                    Toast.makeText(CameraActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onResponse:failure " + response.message());

                }
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                // 서버와 연결 실패
                dismissProgressDialog();
                Log.d(TAG, "onFailure: " + t.getMessage());
                Toast.makeText(CameraActivity.this, "인터넷 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dismissProgressDialog() {
        dialog.dismiss();
        cameraKitView.open();
    }

    /**
     * 인식중임을 알리는 다이얼로그 창이 나타난다.
     */
    private void showProgressDialog() {
        cameraKitView.close();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_detect_progress, null, false);
        builder.setView(view);
        ImageView imageView = view.findViewById(R.id.img_logo);
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate);
        imageView.setAnimation(animation);

        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.show();
    }


    /**
     * 인식된 음식이 없을 경우 다이얼로그 창이 나타난다.
     **/
    private void failedToDetect() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_detect_fail, null, false);
        builder.setView(view);

        Button btn_camera = view.findViewById(R.id.btn_camera);
        Button btn_search = view.findViewById(R.id.btn_search);
        Button btn_cancel = view.findViewById(R.id.btn_cancel);

        final AlertDialog dialog = builder.create();

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(CameraActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btn_camera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();


            }
        });

        btn_search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //finish();
                dialog.dismiss();
            }
        });

        dialog.show();
    }



    /**
     * detectionExists = true인 경우 액티비티 이동
     */
    private void moveActivity() {
        dialog.dismiss();
        Log.d(TAG, "moveActivity: ");
        // 파일을 저장했으면 결과 액티비티로 이동한다.
        Intent intent = new Intent(CameraActivity.this, CameraResultActivity.class);
        intent.putExtra("result", result);
        intent.putExtra("filePath", filePath);
        intent.setData(uri);
        startActivity(intent);
        finish();
    }


    /**
     * 여기서부터는 카메라 관련 코드이다.
     */

    private View.OnClickListener photoOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "onClick: ");
            cameraKitView.takePicture();

        }
    };


    // 갤러리 버튼 눌렀을 때 동작하는 코드
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 1: // 갤러리에서 사진파일을 가지고 온다.
                    try {

                        // 사진 데이터의 Uri를 가져온다.
                        uri = data.getData();
                        filePath = getRealPath(uri);
                        Log.d(TAG, "onActivityResult: " + uri);
                        Log.d(TAG, "onActivityResult: " + filePath);

                        showProgressDialog();
                        uploadToServer(filePath);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

            }
        }

    }

    /**
     * 이미지 데이터의 uri로부터 파일경로를 얻는 메소드이다.
     *
     * @ param Uri 이미지주소
     * @ return String 파일경로
     * 참고 https://stackoverflow.com/questions/13209494/how-to-get-the-full-file-path-from-uri
     * api 레벨마다 경로 얻는 방법이 다름
     */
    private String getRealPath(Uri uri) {
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        String id = wholeID.split(":")[1];

        String[] column = {MediaStore.Images.Media.DATA};

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{id}, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    /**
     * 여기서부터는 권한 관련 코드
     */
    static final int PERMISSIONS_REQUEST_CODE = 1000;
    String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    private boolean hasPermissions(String[] permissions) {
        int result;

        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions) {

            result = ContextCompat.checkSelfPermission(this, perms);

            if (result == PackageManager.PERMISSION_DENIED) {
                //허가 안된 퍼미션 발견
                return false;
            }
        }

        //모든 퍼미션이 허가되었음
        return true;
    }


    private static final int STOARGE_REQUEST_CODE = 400;
    String storagePermission[];

    private void requestStoargePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STOARGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    // handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case STOARGE_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean writeStoargeAccepted = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (writeStoargeAccepted) {
                        pickGallery();
                    } else {
                        Toast.makeText(this, "권한 거부됨", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }

    }


    /**
     *      생명주기
     */

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: ");
        super.onStop();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        cameraKitView.open();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        cameraKitView.close();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        cameraKitView.destroy();
    }
}
