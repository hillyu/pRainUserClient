package org.bluetooth.pMon;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class pMonSensorItemAdapter extends BaseAdapter {

//    private ArrayList<BluetoothDevice> mDevices;
//    private ArrayList<byte[]> mPU;
    private ArrayList<String> mDevicesNames;
    private ArrayList<String> mDevicesAddress;
    private ArrayList<Float> mAngle;
    private ArrayList<Float> mTemperature;
    private LayoutInflater mInflater;

    public pMonSensorItemAdapter(Activity par) {
        super();
        mDevicesNames =new ArrayList<String>();
        mDevicesAddress = new ArrayList<String>();
        mAngle = new ArrayList<Float>();
        mTemperature = new ArrayList<Float>();
        mInflater = par.getLayoutInflater();
    }

    public int addAngle(String deviceName, String deviceAddress, float angle) {
        int ii = mDevicesAddress.indexOf(deviceAddress);
        if( ii == -1) {
            mDevicesNames.add(deviceName);
            mAngle.add(angle);
            mDevicesAddress.add(deviceAddress);
            mTemperature.add((float)0); //to fill out the empty field so the adapter an be properly shown.
            notifyDataSetChanged();

        } else{
            mAngle.set(ii, angle);

        }
        return ii;
    }
    public void addTemperature(String deviceName, String deviceAddress,  float temperature) {
        int ii = mDevicesAddress.indexOf(deviceAddress);
        if( ii != -1) {
            mDevicesNames.add(deviceName);
            mTemperature.add(temperature);
            mDevicesAddress.add(deviceAddress);
            mAngle.add((float)0); //to fill out the empty field so the adapter an be properly shown.
            notifyDataSetChanged();

        } else {
            mTemperature.set(ii,temperature);
        }
    }

    public String getDeviceAddress(int index) {
        return mDevicesAddress.get(index);
    }
    public String getDeviceName(int index) {
        return mDevicesNames.get(index);
    }

    public float getAngle(int index) {
        return mAngle.get(index);
    }

    public void clearList() {
        mDevicesNames.clear();
        mDevicesAddress.clear();
        mAngle.clear();
        mTemperature.clear();
    }

    @Override
    public int getCount() {
        return mDevicesAddress.size();
    }

    @Override
    public Object getItem(int position) {
        return getDeviceAddress(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // get already available view or create new if necessary
        FieldReferences fields;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.activity_sensor_data_itemlist, null);
            fields = new FieldReferences();
            fields.deviceAddress = (TextView)convertView.findViewById(R.id.deviceAddress);
            fields.deviceName    = (TextView)convertView.findViewById(R.id.deviceName);
            fields.angle    = (TextView)convertView.findViewById(R.id.angle);
            fields.temperature    = (TextView)convertView.findViewById(R.id.temperature);
            convertView.setTag(fields);




        } else {
            fields = (FieldReferences) convertView.getTag();
        }

        // set proper values into the view
        float angle = mAngle.get(position);
        float temperature = mTemperature.get(position);
        String temperatureString = (temperature == 0) ? "N/A" : temperature + " °C";
        String angleString = String.format("%.0f",angle) + "°";
        String name = mDevicesNames.get(position);
        String address = mDevicesAddress.get(position);
        if(name == null || name.length() <= 0) name = "Unknown Device";

        fields.deviceName.setText(name);
        fields.deviceAddress.setText(address);
        fields.angle.setText(angleString);
        fields.angle.setTag(address);
        fields.temperature.setText(temperatureString);

        return convertView;
    }

    private class FieldReferences {
        TextView deviceName;
        TextView deviceAddress;
        TextView angle;
        TextView temperature;
//        CheckBox deviceSelectionStatus;



    }


//
}
