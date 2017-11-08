package com.indooratlas.android.example.indoor.wrld.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.indooratlas.android.example.indoor.wrld.R;


/**
 * Convenience class that renders given floor information as a Toast.
 */
public class FloorLevelView extends RelativeLayout {

    private TextView mShortName;
    private TextView mSubTitle;

    public FloorLevelView(Context context) {
        super(context);
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_floor_level, this, true);
        mShortName = (TextView) findViewById(R.id.number);
        mSubTitle = (TextView) findViewById(R.id.name);
    }

    public void setShortName(String number) {
        mShortName.setText(number);
    }

    public void setSubTitle(String name) {
        mSubTitle.setText(name);
    }

    public static void show(Context context, String shortName, String subtitle) {
        Toast toast = new Toast(context);
        FloorLevelView view = new FloorLevelView(context);
        view.setShortName(shortName);
        view.setSubTitle(subtitle);
        toast.setView(view);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
        toast.setMargin(0f, .15f);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }
}
