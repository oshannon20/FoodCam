package com.victu.foodatory.camera;


import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.victu.foodatory.R;

import java.util.ArrayList;


public class CameraResultAdapter extends RecyclerView.Adapter<CameraResultAdapter.ResultViewHolder> {


    private static final String TAG = "CameraResultAdapter";

    private Context mContext;
    private ArrayList<DetectionData> dataArrayList;

    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onEditClick(int position); // 다른 음식으로 수정할 경우

        void onInfoClick(int position); // 음식의 상세영양정보를 보고 싶은 경우

        void onDeleteClick(int position); // 음식을 제거한 경우

        void onSetWeight(int position); // 음식의 수량을 변경한 경우

    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }


    public CameraResultAdapter(Context mContext, ArrayList<DetectionData> mTripList) {
        this.mContext = mContext;
        this.dataArrayList = mTripList;
    }

    @Override
    public ResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.item_detection_result, parent, false);
        return new ResultViewHolder(v, mListener);
    }


    @Override
    public void onBindViewHolder(final ResultViewHolder holder, int position) {
        DetectionData currentItem = dataArrayList.get(position);

        String name = currentItem.getFoodName();
        String weight = String.valueOf(currentItem.getmWeight());
        String calorie = String.valueOf(currentItem.getmCalorie());

        holder.txt_name.setText(name);
        holder.edit_quantity.setText(weight);
        holder.txt_calorie.setText(calorie + "kcal");

    }

    @Override
    public int getItemCount() {
        return (null != dataArrayList ? dataArrayList.size() : 0);
    }



    public class ResultViewHolder extends RecyclerView.ViewHolder implements AdapterView.OnItemSelectedListener {

        public TextView txt_name, txt_calorie;
        public Spinner spinner;
        public EditText edit_quantity;
        public ImageView img_info, img_delete, img_edit;

        public ResultViewHolder(View itemView, final OnItemClickListener listener) {
            super(itemView);

            txt_name = itemView.findViewById(R.id.txt_name);
            txt_calorie = itemView.findViewById(R.id.txt_calorie);
            edit_quantity = itemView.findViewById(R.id.edit_quantity);
            img_info = itemView.findViewById(R.id.img_info);
            img_delete = itemView.findViewById(R.id.img_delete);
            img_edit = itemView.findViewById(R.id.img_edit);


            //TODO: editText 완료 버튼 설정

            // 키패드에서 enter나 완료 실행시 listen하고 동작할 액션을 작성한다.
            edit_quantity.setOnEditorActionListener(new EditText.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

//                    if (listener != null) {
//                        int position = getAdapterPosition();
//                        if (position != RecyclerView.NO_POSITION) {
//                            listener.onSetWeight(position);
//                        }
//                    }

                    return false;
                }
            });


            img_info.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onInfoClick(position);
                        }
                    }
                }
            });


            img_edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onEditClick(position);
                        }
                    }
                }
            });

            img_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onDeleteClick(position);
                        }
                    }
                }
            });


            spinner = itemView.findViewById(R.id.spinner_size);

            // 추후 서버에서 받아온 serving size로 custom해야 함
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext,
                    R.array.serving_size, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(this);
        }


        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String text = parent.getItemAtPosition(position).toString();
            Log.d(TAG, "onItemSelected: " + text);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    } // PlaceViewHolder class closed



}