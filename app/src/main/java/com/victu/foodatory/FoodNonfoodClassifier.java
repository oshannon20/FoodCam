package com.victu.foodatory;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;

import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.List;
import java.util.Map;


/*
    Food/Non-Food 분류기
 */
public class FoodNonfoodClassifier {
    private final String TAG = this.getClass().getSimpleName();

    // asset폴더에 들어있는 파일들
    private static final String MODEL_PATH = "fnf.tflite";
    private static final String LABEL_PATH = "labels.txt";

    //레이블 리스트. (Food, Non-Food 2개밖에 없다)
    private List<String> labels;

    // tensorflow lite 모듈 초기화 관련 변수들
    private MappedByteBuffer tfliteModel;
    private Interpreter tflite;

    // === Input Buffer ===
    // 모델에 이미지를 넣으려면 Bitmap을 ByteArray로 변환하고 이 inputBuffer사용해서 넣어야 함
    private ByteBuffer inputBuffer;
    // float 사이즈
    private static final int BYTE_SIZE_OF_FLOAT = 4;
    // input 사이즈. fnf.tflite 모델에서 요구하는 input값
    private static final int DIM_BATCH_SIZE = 1;       //한 번에 처리할 이미지 수
    private static final int DIM_IMG_SIZE_X = 300;
    private static final int DIM_IMG_SIZE_Y = 300;
    private static final int DIM_PIXEL_SIZE = 3;        //이미지 채널. (RGB이미지라서 3)

    // === Output Buffer ===
    private final TensorBuffer outputBuffer;
    private final TensorProcessor outputProcessor;


    //FoodNonfoodClassifier 생성자
    // Activity -> Context 로 변경
    public FoodNonfoodClassifier(Context context) throws IOException{
        //모델 로드
        tfliteModel = FileUtil.loadMappedFile(context, MODEL_PATH);
        tflite = new Interpreter(tfliteModel);
        labels = FileUtil.loadLabels(context, LABEL_PATH);

        //=== 모델에 넣을 inputBuffer 생성===
        inputBuffer = ByteBuffer.allocateDirect(
                BYTE_SIZE_OF_FLOAT * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        inputBuffer.order(ByteOrder.nativeOrder());

        //=== 결과를 담을 outputBuffer 생성 ===
        //로드한 모델에서 요구하는 output 형식정보 가져온다
        int[] outputShape = tflite.getOutputTensor(0).shape(); // {1, NUM_CLASSES}
        DataType outputDataType = tflite.getOutputTensor(0).dataType();
        outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType);
        //결과를 정리할 outputProcessor도 생성해둔다
        outputProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

    }

    //이미지(bitmap) 받아서 음식인지 아닌지 분류하고 Map형식으로 리턴
    public Map<String, Float> runInference(Bitmap bitmap) {
        //bitmap 이미지 전처리 (bitmap -> inputBuffer)
        preprocess(bitmap);

        //모델 실행
        //이미지 정보가 담긴 inputBuffer와 결과를 받아올 outputBuffer를 넘겨서 실행한다.
        tflite.run(inputBuffer, outputBuffer.getBuffer().rewind());

        //outputBuffer에 담긴 결과를 <String:Float>형식의 Map으로 변환
        Map<String, Float> outputMap = new TensorLabel(labels, outputProcessor.process(outputBuffer)).getMapWithFloatValue();

        return outputMap;
    }

    //모델에 넣을 이미지 전처리
    //Bitmap -> ByteArray
    private void preprocess(Bitmap bitmap) {
        if (bitmap == null || inputBuffer == null) {
            return;
        }

        // 버퍼 리셋
        inputBuffer.rewind();

        int width = bitmap.getWidth(); //300
        int height = bitmap.getHeight(); //300

//        long startTime = SystemClock.uptimeMillis();

        //bitmap의 pixel마다 있는 RGB값을 float값으로 inputBuffer에 채워넣는다
        int[] pixelValues = new int[width * height];
        bitmap.getPixels(pixelValues, 0, width, 0, 0, width, height);
        int pixel = 0;
        int pixelvalue;
        for (int i = 0; i<width ; i++) {
            for (int j=0 ; j<height ; j++) {
                pixelvalue = pixelValues[pixel++];
                inputBuffer.putFloat((pixelvalue >> 16 & 0xFF) / 255f);
                inputBuffer.putFloat((pixelvalue >> 8 & 0xFF) / 255f);
                inputBuffer.putFloat((pixelvalue & 0xFF) / 255f);
            }
        }
        //모델에 넣을 inputBuffer 준비 완료


//        long endTime = SystemClock.uptimeMillis();
//        Log.d(TAG, "Time cost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }

    //output 정리하는데 쓰는 메소드
    private TensorOperator getPostprocessNormalizeOp() {
        float PROBABILITY_MEAN = 0.0f;
        float PROBABILITY_STD = 1.0f;
        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
    }

}
