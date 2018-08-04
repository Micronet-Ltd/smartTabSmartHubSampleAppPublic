/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.smarttabsmarthubsampleapp.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.micronet.smarttabsmarthubsampleapp.ADCs;
import com.micronet.smarttabsmarthubsampleapp.GPIs;
import com.micronet.smarttabsmarthubsampleapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import micronet.hardware.MicronetHardware;

public class GpiAdcTextAdapter extends BaseAdapter {

    private Context mContext;
    private List<Pair<String, String>> pairList = new ArrayList<>();
    private static MicronetHardware mh;

    public GpiAdcTextAdapter(Context c) {
        mContext = c;
        mh = MicronetHardware.getInstance();
    }

    public void populateGpisAdcs() {
        int[] adcVoltages = mh.getAllAnalogInput();
        pairList.clear();

        boolean canCommunicateWithMcu = true;

        // If the app cannot communicate with the mcu then set all values to empty strings.
        if (adcVoltages[0] == -1) {
            canCommunicateWithMcu = false;
        }

        int i = 0;
        for (ADCs adcs : ADCs.values()) {
            if (canCommunicateWithMcu) {
                String value;
                if (i != 10) {
                    value = String.valueOf(adcVoltages[i++]) + " mV";
                } else {
                    float temperature = ((float) adcVoltages[i++] - 500) / 10f;
                    value = String
                            .valueOf(String.format(java.util.Locale.US, "%.1f", temperature)
                                    + " \u2103");
                }

                pairList.add(new Pair<>(adcs.getString(), value));
            } else {
                pairList.add(new Pair<>(adcs.getString(), ""));
            }

        }

        int[] gpiValues = mh.getAllPinInState();
        i = 0;
        for (GPIs gpis : GPIs.values()) {
            if (canCommunicateWithMcu) {
                pairList.add(new Pair<>(gpis.getString(), String.valueOf(gpiValues[i++])));
            } else {
                pairList.add(new Pair<>(gpis.getString(), ""));
            }

        }
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

    class TextHolder {
        TextView title;
        TextView subitem;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        TextHolder holder = new TextHolder();
        @SuppressLint({"ViewHolder", "InflateParams"}) View rowView = ((LayoutInflater)
                Objects.requireNonNull(mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))).inflate(R.layout.item_list, null);
        holder.title = rowView.findViewById(R.id.textItem);
        holder.subitem = rowView.findViewById(R.id.textSubItem);

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

    private static boolean isBrightColor(int color) {
        if (android.R.color.transparent == color) {
            return true;
        }

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