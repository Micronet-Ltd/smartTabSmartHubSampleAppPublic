package com.micronet_inc.smarthubsampleapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.micronet_inc.smarthubsampleapp.ADCs;
import com.micronet_inc.smarthubsampleapp.GPIs;
import com.micronet_inc.smarthubsampleapp.R;

import java.util.ArrayList;
import java.util.List;

import micronet.hardware.MicronetHardware;
import micronet.hardware.exception.MicronetHardwareException;

public class GpiAdcTextAdapter extends BaseAdapter {
    private final String TAG = getClass().getSimpleName();
    private Context mContext;
    private List<Pair<String, String>> pairList = new ArrayList<Pair<String, String>>();
    static MicronetHardware mh;

    public GpiAdcTextAdapter(Context c){
        mContext = c;
        mh = MicronetHardware.getInstance();
    }

    public void populateGpisAdcs(){
//        try {
            int[] adcVoltages = mh.getAllAnalogInput();
            pairList.clear();

            int i = 0;
            for (ADCs adcs : ADCs.values()) {
                pairList.add(new Pair<String, String>(adcs.getString(), String.valueOf(adcVoltages[i++]) + " mV"));
            }

            int[] gpiValues = mh.getAllPinInState();
            i = 0;
            for (GPIs gpis : GPIs.values()) {
                pairList.add(new Pair<String, String>(gpis.getString(), String.valueOf(gpiValues[i++])));
            }
//        }catch (MicronetHardwareException ex) {
//            Log.e(TAG, ex.toString());
//        }

    }

    @Override
    public int getCount() {
        return pairList.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class TextHolder {
        TextView title;
        TextView subitem;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        TextHolder holder = new TextHolder();
        View rowView = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_list, null);
        holder.title = (TextView) rowView.findViewById(R.id.textItem);
        holder.subitem = (TextView) rowView.findViewById(R.id.textSubItem);

        holder.title.setText((pairList.get(position).first));
        holder.subitem.setText((pairList.get(position).second));

        int c = ((ColorDrawable) rowView.getBackground()).getColor();
        if (isBrightColor(c)) {
            holder.title.setTextColor(Color.BLACK);
            holder.subitem.setTextColor(Color.BLACK);
        } else {
            holder.title.setTextColor(Color.WHITE);
            holder.subitem.setTextColor(Color.WHITE);
        }
        return rowView;
    }

    public static boolean isBrightColor(int color) {
        if (android.R.color.transparent == color)
            return true;

        boolean rtnValue = false;

        int[] rgb = {Color.red(color), Color.green(color), Color.blue(color)};

        // Brightness math based on:
        //   http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
        int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1]
                * rgb[1] * .691 + rgb[2] * rgb[2] * .068);

        // color is light
        if (brightness >= 200) {
            rtnValue = true;
        }

        return rtnValue;
    }
}