package com.victu.foodatory.utils;

import com.google.gson.JsonObject;
import com.victu.foodatory.detail.FoodNutritionData;
import com.victu.foodatory.user.UserBodyData;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;


/**
 * Http Method(GET, POST, PUT, DELETE 등)와 자원의 정보를 정의할 인터페이스이다.
 * @author Shang
 * @version 1.0
 */


public interface RetrofitInterface {

    /**
     *          =================== 식사 추가 ======================
     */

    /**
     * CameraActivity에서 유저가 음식 사진을 찍었을 때나
     * 갤러리에서 이미지를 불러왔을 때, 이미지를 서버로 전송함
     * @param file
     * @param requestBody
     * @return
     */
    @Multipart  // 요청하는 body의 content type이 multipart(list of parts)이다.
    @POST("detect/fromapp") // BASE_URL 다음 주소 (마지막에 slash 빼야함)
    Call<String> detectFood(@Part MultipartBody.Part file, @Part("name") RequestBody requestBody);


    /**
     * 사용자가 핸드폰 카메라로 사진을 찍었을 경우,
     * 사진을 가져와서 서버로 전송해 인식한다.
     * @param file
     * @param params
     * @return
     */
    @Multipart
    @POST("detect/frombackground")
    Call<String> detectFoodFromBackground(@Part MultipartBody.Part file, @PartMap Map<String, RequestBody> params);


    /**
     * 인식한 음식이 부정확할 경우 유사음식을 추천받을 수 있다.
     * @param file
     * @param requestBody
     * @return
     */
    @Multipart
    @POST("detect/wrecommendation")
    Call<String> foodRecommendation(@Part MultipartBody.Part file, @Part("name") RequestBody requestBody);



    /**
     * 특정 음식의 영양정보를 가져옴
     * @Path("food_no") int postId로 들어간 값을 {food_no}에 넘겨준다.
     * @param postId 요청에 필요한 음식 번호
     * @return FoodNutritionData 객체를 JSON 형태로 반환
     * */
    @GET("foods/{food_no}")
    Call<List<FoodNutritionData>> getFoodDetail(@Path("food_no") int postId);


    /**
     * 식사를 추가할 때에는 헤더에 accessToken을 추가해야 한다.
     * @param body
     */
    @Headers("Content-Type: application/json")
    @POST("meals/add")
    Call<String> addMeal(@Header("Authorization") String token, @Body JsonObject body);


    /**
     *      ================    홈 화면  ===========================
     */

    /**
     * 날짜별로 등록한 식사 정보를 가져온다.
     * @param userId 회원고유번호
     * @param date 요청할 날짜
     * @return
     */
    @GET("meals/{mem_uid}/{date}")
    Call<String> getMealData(@Path("mem_uid") int userId, @Path("date") String date);

    /**
     * 회원 식단 가져오기
     * @param userId
     * @return
     */
    @GET("diet/{mem_uid}")
    Call<UserBodyData> getDiet(@Path("mem_uid") int userId);




    /**
     *       =================   회원 관련 요청=====================
     */

    /**
     * 이메일로 회원가입 할 때
     * @param body JsonObject (JSON아님)
     * @return
     */
    @Headers("Content-Type: application/json")
    @POST("members/add")
    Call<String> signUp(@Body JsonObject body);



    /**
     * 회원가입 시 이메일 중복 여부 확인
     * @param body JsonObject
     * @return
     */
    @Headers("Content-Type: application/json")
    @POST("members/check")
    Call<String> checkEmail(@Body RequestBody body);



    /**
     * 로그인
     * @param body JsonObject
     * @return
     */
    @Headers("Content-Type: application/json")
    @POST("auth/login")
    Call<String> signIn(@Body JsonObject body);


    /**
     * 로그인
     * @param body JsonObject
     * @return
     */
    @Headers("Content-Type: application/json")
    @POST("diet/add")
    Call<String> addDiet(@Body JsonObject body);


    /**
     *         ==================   갤러리 관련 요청  ==================
     */


    /**
     * 사진이 있는 경우에만 식사 데이터 출력한다.
     * @param userId 회원고유번호
     * @return
     */
    @GET("meals/{mem_uid}")
    Call<String> getMealPhoto(@Path("mem_uid") int userId);


    /**
     *
     * 사진 삭제 요청
     * @return
     */
    // @DELETE
    @HTTP(method = "DELETE", path = "image/edit", hasBody = true)
    Call<String> deletePhoto(@Body JsonObject body);


}