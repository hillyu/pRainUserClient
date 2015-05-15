package org.bluetooth.pMon;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class pMonService extends Service {
    private static final String TAG = "pMonService"; //degug tag;
    private Handler mHandler = null;
    private final IBinder mBinder = new LocalBinder();
    private BluetoothManager mBTManager = null;
    private BluetoothAdapter mBTAdapter = null;
    private ArrayList<BluetoothGatt> mGattList = new ArrayList<BluetoothGatt>();
    private BluetoothDevice mBTDevice = null;
    private boolean mDevices;
    private BluetoothGatt mBTGatt = null;
    private BluetoothGattService mBTService = null;
    private BluetoothGattCharacteristic mBTValueCharacteristic = null;
    private Map<String, String> mBTPeripherals; //map <address, name>
    private boolean isInitialized = false;
    // UUDI od Heart Rate service:
    final static private UUID pMon_Services = BleDefinedUUIDs.Service.PMON_SERVICES;
    final static private UUID pMon_MPU6050_Characteristic = BleDefinedUUIDs.Characteristic.PMON_MPU6050;
    final static private UUID pMon_BMP_Characteristic = BleDefinedUUIDs.Characteristic.PMON_BMP;
    //String Constant
    public static final String EXTRAS_DEVICE_NAME = PeripheralActivity.EXTRAS_DEVICE_NAME;
    public static final String EXTRAS_DEVICE_ADDRESS = PeripheralActivity.EXTRAS_DEVICE_ADDRESS;


    public pMonService() {
        mHandler = new Handler();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void handleCommand(Intent intent) {

        mBTPeripherals = new HashMap<String, String>();
        ArrayList<String> mDeviceAddrs = intent.getStringArrayListExtra(EXTRAS_DEVICE_ADDRESS);
        ArrayList<String> mDeviceName = intent.getStringArrayListExtra(EXTRAS_DEVICE_NAME);
        //put parced inent info to mBTperipheral hashmap.
        for (int j = 0; j < mDeviceAddrs.size(); j++) {
            mBTPeripherals.put(mDeviceAddrs.get(j), mDeviceName.get(j));
        }
        this.initialize();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");]

        return mBinder;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public class LocalBinder extends Binder {
        pMonService getService() {
            return pMonService.this;
        }
    }

    /**
     * Initializes the service .
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.

        // first check if BT/BLE is available and enabled
        if (initBt() == false) return false;
        if (isBleAvailable() == false) return false;
        if (isBtEnabled() == false) return false;

        // then connect to the device indicated in the Intent received;
        for (Map.Entry<String, String> entry : mBTPeripherals.entrySet()) {

            String address = entry.getKey();
            BluetoothDevice device = mBTAdapter.getRemoteDevice(address);
            connectToDevice(device);
            Log.d(TAG, "Device: " + entry.getValue() + " connected!");
        }
        isInitialized = true; //set the status of service so the client would know if initialization
        // is needed
        return true;
    }


    //Bt related methods
    private boolean initBt() {
        mBTManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBTManager != null) mBTAdapter = mBTManager.getAdapter();

        return (mBTManager != null) && (mBTAdapter != null);
    }

    private boolean isBleAvailable() {
        Log.d("MYLOG", "Checking if BLE hardware is available");

        boolean hasBle = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        if (hasBle && mBTManager != null && mBTAdapter != null) {
            Log.d("MYLOG", "BLE hardware available");
        } else {
            Log.d("MYLOG", "BLE hardware is missing!");
            return false;
        }
        return true;
    }

    private boolean isBtEnabled() {
        Log.d(TAG, "Checking if BT is enabled");
        if (mBTAdapter.isEnabled()) {
            Log.d("MYLOG", "BT is enabled");
        } else {
            Log.d("MYLOG", "BT is disabled. Use Setting to enable it and then come back to this app");
            return false;
        }
        return true;
    }


    private void connectToDevice(BluetoothDevice mDevice) {
        Log.d("MYLOG", "Connecting to the device NAME: " + mDevice.getName() + " HWADDR: "
                + mDevice.getAddress());
        //mBTGatt = mBTDevice.connectGatt(this, true, mGattCallback);
        BluetoothGatt tmpGatt = mDevice.connectGatt(this, true, mGattCallback);
        mGattList.add(tmpGatt);
    }

    private void disconnectFromDevice() {
        Log.d("MYLOG", "Disconnecting from device");
        if (mBTGatt != null) mBTGatt.disconnect();
    }

    private void closeGatt() {
        if (mBTGatt != null) mBTGatt.close();
        mBTGatt = null;
    }

    private void discoverServices() {
        Log.d("MYLOG", "Starting discovering services");
        mBTGatt.discoverServices();
    }

    private void enableNotificationForHr() {
        Log.d(TAG, "Enabling notification for Heart Rate");
        boolean success = mBTGatt.setCharacteristicNotification(mBTValueCharacteristic, true);
        if (!success) {
            Log.d(TAG, "Enabling notification failed!");
            return;
        }

        BluetoothGattDescriptor descriptor = mBTValueCharacteristic.getDescriptor(
                BleDefinedUUIDs.Descriptor.CHAR_CLIENT_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBTGatt.writeDescriptor(descriptor);
            Log.d(TAG, "Notification enabled");
        } else {
            Log.d(TAG, "Could not get descriptor for characteristic! Notification are not enabled.");
        }
    }

    private void disableNotificationForHr() {
        Log.d(TAG, "Disabling notification for Heart Rate");
        boolean success = mBTGatt.setCharacteristicNotification(mBTValueCharacteristic, false);
        if (!success) {
            Log.d(TAG, "Disabling notification failed!");
            return;
        }

        BluetoothGattDescriptor descriptor = mBTValueCharacteristic.getDescriptor(
                BleDefinedUUIDs.Descriptor.CHAR_CLIENT_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            mBTGatt.writeDescriptor(descriptor);
            Log.d(TAG, "Notification disabled");
        } else {
            Log.d(TAG, "Could not get descriptor for characteristic! Notification could be " +
                    "still enabled.");
        }
    }


    //Data handler
    private void handleSensorData(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, characteristic.getValue().toString() + gatt.getDevice().getAddress());
    }


//private BluetoothAdapter.LeScanCallback mDeviceFoundCallback = new BluetoothAdapter.LeScanCallback() {
//    @Override
//    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
//
//        Log.d("MYLOG","Device with pMon service discovered. HW Address: "  + device.getAddress());
//        connectToDevice(device);
//    }
//};

    /* callbacks called for any action on HR Device */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Device connected" + gatt.getDevice().getAddress());
//            	discoverServices();//TODO: disabled for now.
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Device disconnected" + gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered");
//        		getHrService();//ToDO: disabled for now
                BluetoothGattService tmpService = gatt.getService(pMon_Services);
                BluetoothGattCharacteristic tmpCharacteristic = tmpService.getCharacteristic(
                        UUID.fromString("0000ffb6-0000-1000-8000-00805f9b34fb"));
                gatt.readCharacteristic(tmpCharacteristic);
            } else {
                Log.d(TAG, "Unable to discover services");
            }
        }

        // it is a shame that the current realtag don't support notification(notification is enabled but no
// data has been sent from notification.)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (characteristic.equals(mBTValueCharacteristic)) {
//            getAndDisplayHrValue();
            }
        }

        /* the rest of callbacks are not interested for us */

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         int status) {

            handleSensorData(gatt, characteristic);

            //request after delay fro another read:
            Runnable timeout = new Runnable() {
                @Override
                public void run() {
                    gatt.readCharacteristic(characteristic);
                }
            };
            mHandler.postDelayed(timeout, 10000); //10 seconds
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        }
    };


}