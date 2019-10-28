package com.example.bluetoothcontroller;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

 //Кастомны адаптер для ListView с доступнымии Bluetooth устройствами

public class CustomArrayAdapter extends ArrayAdapter<CustomArrayAdapter.Device> {

    static ViewHolder holder;

    public static class Device {
        private String name;
        private String status;
        private boolean progress = false;

        public Device(String name, String status) {
            this.name = name;
            this.status = status;
        }

        public String getName(){
            return this.name;
        }

        public String getStatus(){
            return this.status;
        }

        public boolean getProgress(){
            return this.progress;
        }

        public void setProgress(boolean p){
            this.progress = p;
        }

        public void setStatus(String status){
            this.status = status;
        }
    }

    private Context mContext;
    private int mResource;
    private int lastPosition = -1;

    /**
     * Holds variables in a View
     */
    private static class ViewHolder {
        TextView name;
        TextView status;
        ImageView bl;
        View anim_bl;
    }

    /**
     * Default constructor for the PersonListAdapter
     * @param context
     * @param resource
     * @param objects
     */
    public CustomArrayAdapter(Context context, int resource, ArrayList<Device> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //get the persons information
        String name = getItem(position).getName();
        String status = getItem(position).getStatus();
        boolean progress = getItem(position).getProgress();

        //Create the person object with the information
        Device device = new Device(name, status);

        //create the view result for showing the animation
        final View result;

        //ViewHolder object
        ViewHolder holder;


        if(convertView == null){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);
            holder= new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.status = (TextView) convertView.findViewById(R.id.status);
            holder.bl = (ImageView) convertView.findViewById(R.id.item_bl);
            holder.anim_bl = (View) convertView.findViewById(R.id.item_anim_bl);

            if(progress){
                holder.bl.setVisibility(View.GONE);
                holder.anim_bl.setVisibility(View.VISIBLE);
            }else{
                holder.anim_bl.setVisibility(View.GONE);
                holder.bl.setVisibility(View.VISIBLE);
            }

            result = convertView;

            convertView.setTag(holder);
        }
        else{
            holder = (ViewHolder) convertView.getTag();
            result = convertView;
        }

        lastPosition = position;

        holder.name.setText(device.getName());
        holder.status.setText(device.getStatus());

        return convertView;
    }
}

