package org.bluetooth.pMon;

import android.bluetooth.BluetoothDevice;



public class BTDeviceListItem{
    BluetoothDevice mBtDevice = null;
    boolean selected = false;
    public BTDeviceListItem(final BluetoothDevice mBtDevice, boolean selected){
        super();
        this.mBtDevice = mBtDevice;
        this.selected = selected;

    }
    //BluetoothDevice methods:
    public String getName(){
        return mBtDevice.getName();
    }
    public String getAddress(){
        return mBtDevice.getAddress();
    }
    public boolean isSelected(){
        return selected;

    }
    public void setSelected (boolean selected){
        this.selected = selected;
    }


    //overide
    @Override
    public boolean equals(Object object)
    {
        boolean sameSame = false;

        if (object != null && object instanceof BTDeviceListItem)
        {
            sameSame = this.mBtDevice.equals (((BTDeviceListItem) object).mBtDevice);
        }

        return sameSame;
    }

//    @Override
//    public int hashCode() {
//        return mBtDevice.hashCode();
//    }
}
