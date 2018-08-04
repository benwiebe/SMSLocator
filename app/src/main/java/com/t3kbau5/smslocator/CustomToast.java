package com.t3kbau5.smslocator;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

public class CustomToast extends Toast {

    private CustomToast(Context context) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = Objects.requireNonNull(inflater).inflate(R.layout.customtoast_layout, null);
        this.setView(view);
    }

    private CustomToast(Context context, int type) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = Objects.requireNonNull(inflater).inflate(R.layout.customtoast_layout, null);
        switch (type) {
            case 1:
                view.findViewById(R.id.ctll).setBackgroundResource(R.drawable.toast_negative);
                break;
            case 2:
                view.findViewById(R.id.ctll).setBackgroundResource(R.drawable.toast_positive);
        }
        this.setView(view);
    }

    public static CustomToast makeText(Context context, String text, int duration) {
        CustomToast ct = new CustomToast(context);
        TextView tv = ct.getView().findViewById(R.id.text);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        ct.setDuration(duration);
        //ct.setGravity(Gravity.BOTTOM, 0, -150);
        return ct;
    }

    public static CustomToast makeText(Context context, String text, int duration, int type) {
        CustomToast ct = new CustomToast(context, type);
        TextView tv = ct.getView().findViewById(R.id.text);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        ct.setDuration(duration);
        //ct.setGravity(Gravity.BOTTOM, 0, -150);
        return ct;
    }

}
