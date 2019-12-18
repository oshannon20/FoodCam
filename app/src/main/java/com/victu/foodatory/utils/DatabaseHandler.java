package com.victu.foodatory.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.victu.foodatory.gallery.model.ImageData;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 자동 업로드 된 사진을 저장하기 위한 SQLite 클래스
 * 참고: https://www.tutlane.com/tutorial/android/android-sqlite-database-with-examples
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    public static final String TAG = "DatabaseHandler";

    private static final int DB_VERSION = 3;
    private static final String DB_NAME = "photoDB";
    private static final String TABLE_PHOTOS = "auto_uploaded_photos";
    private static final String COLUMN_ID = "id"; // 자동으로 증가하는 ID

    private static final String COLUMN_NAME = "name"; // 이미지 파일 이름
    private static final String COLUMN_PATH = "filePath"; // 이미지 파일 경로
    private static final String COLUMN_URI = "uri"; // Uri
    private static final String COLUMN_DATE = "date"; // Uri
    private static final String COLUMN_TIME = "time"; // 이미지 파일이 찍힌 시간

    public DatabaseHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate: ");
        String CREATE_TABLE = "CREATE TABLE " + TABLE_PHOTOS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_PATH + " TEXT,"
                + COLUMN_URI + " TEXT,"
                + COLUMN_DATE + " TEXT,"
                + COLUMN_TIME + " TEXT" + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: ");
        // 해당 테이블 이름이 존재한다면 삭제한다.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHOTOS);
        // 새로 테이블을 만든다.
        onCreate(db);
    }


    // 새로운 사진 정보를 추가한다.
    public void insertPhoto(String name, String filePath, String uri, String date, String time) {
        // Get the Data Repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        // Create a new map of values, where column names are the keys
        ContentValues cValues = new ContentValues();

        // 컬럼의 이름을 key로 하고, 데이터를 넣는다.
        cValues.put(COLUMN_NAME, name);
        cValues.put(COLUMN_PATH, filePath);
        cValues.put(COLUMN_URI, uri);
        cValues.put(COLUMN_DATE, date);
        cValues.put(COLUMN_TIME, time);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(TABLE_PHOTOS, null, cValues);
        Log.d(TAG, "insertUserDetails: " + newRowId);

        db.close();
    }

    /**
     * 테이블에 있는 모든 사진 정보를 가져온다.
     * log로 찍었을 때 값 볼 수 있도록 HashMap 사용
     * @return
     */
    public ArrayList<HashMap<String, String>> showPhotos() {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> photoList = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_PHOTOS;

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("id", cursor.getString(cursor.getColumnIndex(COLUMN_ID)));
            hashMap.put("name", cursor.getString(cursor.getColumnIndex(COLUMN_NAME)));
            hashMap.put("filePath", cursor.getString(cursor.getColumnIndex(COLUMN_PATH)));
            hashMap.put("uri", cursor.getString(cursor.getColumnIndex(COLUMN_URI)));
            hashMap.put("date", cursor.getString(cursor.getColumnIndex(COLUMN_DATE)));
            hashMap.put("time", cursor.getString(cursor.getColumnIndex(COLUMN_TIME)));
            photoList.add(hashMap);
        }
        cursor.close();
        return photoList;
    }

    /**
     * @return ArrayList로 리턴하여 recyclerView에 넣어줄 때 사용
     */
    public ArrayList<ImageData> selectPhotos() {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<ImageData> photoList = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_PHOTOS + " ORDER BY " + COLUMN_DATE + " DESC";

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
            String filePath = cursor.getString(cursor.getColumnIndex(COLUMN_PATH));
            String uri = cursor.getString(cursor.getColumnIndex(COLUMN_URI));
            String date = cursor.getString(cursor.getColumnIndex(COLUMN_DATE));
            String time = cursor.getString(cursor.getColumnIndex(COLUMN_TIME));

            ImageData imageData = new ImageData(id, name, filePath, uri, date, time);
            photoList.add(imageData);

        }
        cursor.close();
        return photoList;
    }




    // id로 특정 사진 정보를 가져온다.
    public ArrayList<HashMap<String, String>> selectPhotoById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> photoList = new ArrayList<>();

        Cursor cursor = db.query(TABLE_PHOTOS, new String[]{COLUMN_ID, COLUMN_NAME, COLUMN_PATH, COLUMN_URI, COLUMN_TIME},
                COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor.moveToNext()) {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("id", cursor.getString(cursor.getColumnIndex(COLUMN_ID)));
            hashMap.put("name", cursor.getString(cursor.getColumnIndex(COLUMN_NAME)));
            hashMap.put("filePath", cursor.getString(cursor.getColumnIndex(COLUMN_PATH)));
            hashMap.put("uri", cursor.getString(cursor.getColumnIndex(COLUMN_URI)));
            hashMap.put("date", cursor.getString(cursor.getColumnIndex(COLUMN_DATE)));
            hashMap.put("time", cursor.getString(cursor.getColumnIndex(COLUMN_TIME)));
            photoList.add(hashMap);
        }
        cursor.close();
        return photoList;
    }


    // 사진 정보를 지운다.
    public void deletePhotoById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PHOTOS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // 사진 정보를 지운다.
    public void deletePhotos() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PHOTOS, null, null);
        db.close();
    }


}
