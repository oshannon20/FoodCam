package com.victu.foodatory.home;


import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.victu.foodatory.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CalendarDialog extends AppCompatDialogFragment {
    private static final String TAG = "CalendarDialog";

    private CalendarView calendarView;
    private CalendarDialogListener customDialogListener;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_calendar, null);

        builder.setView(dialogView).setCancelable(true);

        calendarView = dialogView.findViewById(R.id.calendar);

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                Calendar c = Calendar.getInstance();
                c.set(Calendar.YEAR, year);
                c.set(Calendar.MONTH, month);
                c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                String date = dateFormat.format(c.getTime());
                Log.d(TAG, "onSelectedDayChange: " + date);
                customDialogListener.onPositiveClicked(date);
                dismiss();
            }
        });


        return builder.create();

    }



    // 인터페이스 설정
    interface CalendarDialogListener {
        void onPositiveClicked(String date);
    //    void onNegativeClicked();
    }

    // 호출할 리스너 초기화
    public void setDialogListener(CalendarDialogListener customDialogListener){
        this.customDialogListener = customDialogListener;
    }

}