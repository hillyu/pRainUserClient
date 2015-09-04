package org.bluetooth.pRain;

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
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class pRainService extends Service {
    private static final String TAG = "pRainService"; //degug tag;
    private static final int DATA_RATE_TIMEOUT = 200;
    private static final int BATTERY_QUERY_TIMEOUT = 60000;

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
    final static private UUID dev_info = BleDefinedUUIDs.Service.DEVICE_INFO;
    final static private UUID pMon_MPU6050_Characteristic = BleDefinedUUIDs.Characteristic.PMON_MPU6050;
    final static private UUID pMon_BMP_Characteristic = BleDefinedUUIDs.Characteristic.PMON_BMP;
    final static private UUID pMon_BAT_Characteristic = BleDefinedUUIDs.Characteristic.BATTERY_LEVEL;
    final static private UUID nrf_service = BleDefinedUUIDs.Service.NRF_SERVICE;
    final static private UUID nrf_mpu_notify_Characteristic = BleDefinedUUIDs.Characteristic.NRF_MPU_NOTIFY;
    final static private UUID nrf_CCCD = BleDefinedUUIDs.Descriptor.CHAR_CLIENT_CONFIG;
    //String Constant
    public static final String EXTRAS_DEVICE_NAME = PeripheralActivity.EXTRAS_DEVICE_NAME;
    public static final String EXTRAS_DEVICE_ADDRESS = PeripheralActivity.EXTRAS_DEVICE_ADDRESS;
    public static final String EXTRA_CHARACTERISTIC = "org.bluetooth.pMon.EXTRA_CHARACTERISTIC";
    public static final String EXTRAS_DATA = "org.bluetooth.pMon.EXTRA_DATA";


    //name actions
    public final static String ACTION_DATA_AVAILABLE =
            "org.bluetooth.pMon.ACTION_DATA_AVAILABLE";
    public final static String ACTION_GATT_CONNECTED =
            "org.bluetooth.pMon.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "org.bluetooth.pMon.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "org.bluetooth.pMon.ACTION_GATT_SERVICES_DISCOVERED";


    public pRainService() {
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
        pRainService getService() {
            return pRainService.this;
        }
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        closeGatt();
        return super.onUnbind(intent);
    }
    @Override
    public void onDestroy() {
        closeGatt();

    }

    /**
     * Initializes the service .
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (!isInitialized)
        {
            // first check if BT/BLE is available and enabled
            if (initBt() == false) return false;
            if (isBleAvailable() == false) return false;
            if (isBtEnabled() == false) return false;

            // then connect to the device indicated in the Intent received;
            for (Map.Entry<String, String> entry : mBTPeripherals.entrySet()) {

                String address = entry.getKey();
                BluetoothDevice device = mBTAdapter.getRemoteDevice(address);
                connectToDevice(device);
//            Log.d(TAG, "Device: " + entry.getValue() + " connected!");
            }
            isInitialized = true; //set the status of service so the client would know if initialization
        }
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
//        Log.d("MYLOG", "Checking if BLE hardware is available");

        boolean hasBle = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        if (hasBle && mBTManager != null && mBTAdapter != null) {
//            Log.d("MYLOG", "BLE hardware available");
        } else {
//            Log.d("MYLOG", "BLE hardware is missing!");
            return false;
        }
        return true;
    }

    private boolean isBtEnabled() {
//        Log.d(TAG, "Checking if BT is enabled");
        if (mBTAdapter.isEnabled()) {
//            Log.d("MYLOG", "BT is enabled");
        } else {
//            Log.d("MYLOG", "BT is disabled. Use Setting to enable it and then come back to this app");
            return false;
        }
        return true;
    }


    private void connectToDevice(BluetoothDevice mDevice) {
//        Log.d("MYLOG", "Connecting to the device NAME: " + mDevice.getName() + " HWADDR: "
//                + mDevice.getAddress());
        //mBTGatt = mBTDevice.connectGatt(this, true, mGattCallback);
        BluetoothGatt tmpGatt = mDevice.connectGatt(this, true, mGattCallback);
        mGattList.add(tmpGatt);
    }

    private void disconnectFromDevice() {

        Iterator<BluetoothGatt> it = mGattList.iterator();
        while (it.hasNext()) {

            BluetoothGatt gattToDisconnect = it.next();
//            Log.d("MYLOG", "Disconnecting from device: " + gattToDisconnect.getDevice().getAddress());
            gattToDisconnect.disconnect();
        }
    }

    private void closeGatt() {
        Iterator<BluetoothGatt> it = mGattList.iterator();
        while (it.hasNext()) {

            BluetoothGatt gattToClose = it.next();
//            Log.d("MYLOG", "Disconnecting from device: " + gattToClose.getDevice().getAddress());
            gattToClose.close();
        }
        mGattList.clear();
    }

    private void discoverServices() {
//        Log.d("MYLOG", "Starting discovering services");
        mBTGatt.discoverServices();
    }

    private void enableNotificationForHr() {
//        Log.d(TAG, "Enabling notification for Heart Rate");
        boolean success = mBTGatt.setCharacteristicNotification(mBTValueCharacteristic, true);
        if (!success) {
//            Log.d(TAG, "Enabling notification failed!");
            return;
        }

        BluetoothGattDescriptor descriptor = mBTValueCharacteristic.getDescriptor(
                BleDefinedUUIDs.Descriptor.CHAR_CLIENT_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBTGatt.writeDescriptor(descriptor);
//            Log.d(TAG, "Notification enabled");
        } else {
//            Log.d(TAG, "Could not get descriptor for characteristic! Notification are not enabled.");
        }
    }

    private void disableNotificationForHr() {
//        Log.d(TAG, "Disabling notification for Heart Rate");
        boolean success = mBTGatt.setCharacteristicNotification(mBTValueCharacteristic, false);
        if (!success) {
//            Log.d(TAG, "Disabling notification failed!");
            return;
        }

        BluetoothGattDescriptor descriptor = mBTValueCharacteristic.getDescriptor(
                BleDefinedUUIDs.Descriptor.CHAR_CLIENT_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            mBTGatt.writeDescriptor(descriptor);
//            Log.d(TAG, "Notification disabled");
        } else {
            Log.d(TAG, "Could not get descriptor for characteristic! Notification could be " +
                    "still enabled.");
        }
    }
//  generic boradcast method.
    private void broadcastUpdate(BluetoothGatt gatt, final String action) {
        final Intent intent = new Intent(action);
        final String address = gatt.getDevice().getAddress();
        String deviceName = gatt.getDevice().getName();
        intent.putExtra(EXTRAS_DEVICE_NAME, deviceName);
        intent.putExtra(EXTRAS_DEVICE_ADDRESS,address);
        sendBroadcast(intent);
    }

    //Data handler
    private void handleSensorData(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        final String characteristicName = characteristic.getUuid().toString().substring(4, 8);
        final String address = gatt.getDevice().getAddress();
        String deviceName = gatt.getDevice().getName();
//        Log.d(TAG, "Received data from:" + characteristicName + characteristic.getValue().toString() + address+ "@"+deviceName);

        //fireup broadcast message via intent.
        final Intent intent = new Intent(ACTION_DATA_AVAILABLE);

        intent.putExtra(EXTRAS_DEVICE_NAME, deviceName);
        intent.putExtra(EXTRAS_DEVICE_ADDRESS,address);
        intent.putExtra(EXTRA_CHARACTERISTIC, characteristic.getUuid().toString());
        intent.putExtra(EXTRAS_DATA, characteristic.getValue());

        sendBroadcast(intent);

//        //create Thread to write data to storage.
//        Runnable writeRawDataToStorage = new Runnable() {
//            @Override
//            public void run() {


//            }
//        };
        if(!characteristic.getUuid().equals(pMon_BAT_Characteristic)) //do not write batt info.
//        mHandler.post(writeRawDataToStorage);
        writeRawDataToStorage(characteristicName,address,characteristic.getValue());

    }

    private void writeRawDataToStorage(String characteristicName, String deviceAddress, byte [] value) {
        if (isExternalStorageWritable()) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd");
            Date now = new Date();
            String filename = formatter.format(now) + "_" + characteristicName +".raw";

            File file = new File(getStorageDir("postureData"), filename); //initialize file with storageDir using the filename defined in filename variable

            // get current timestamp
            Long tsLong = System.currentTimeMillis();
            try {
                FileOutputStream fos = new FileOutputStream(file, true);
 /*
       * To create DataOutputStream object from FileOutputStream use,
       * DataOutputStream(OutputStream os) constructor.
       *
       */

                DataOutputStream dos = new DataOutputStream(fos);
                    /*
        * To write an int value to a file, use
        * void writeInt(int i) method of Java DataOutputStream class.
        *
        * This method writes specified int to output stream as 4 bytes value.
        */

                dos.write(0xAA);//header
                dos.writeLong(tsLong);
                dos.write(value);
                dos.writeBytes(deviceAddress);
//                        dos.write(bmpraw);
//                    dos.writeShort(event);
                dos.write(0x0a);//tail total 32


        /*
         * To close DataOutputStream use,
         * void close() method.
         *
         */

                dos.close();

                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /* Bluetooth GAtt call back funcition override */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Device connected" + gatt.getDevice().getAddress());
                //broadcast the connected status to client
                broadcastUpdate(gatt,ACTION_GATT_CONNECTED);
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Device disconnected" + gatt.getDevice().getAddress());
                broadcastUpdate(gatt, ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered");
                //let the client know via action broadcast;
                broadcastUpdate(gatt, ACTION_GATT_SERVICES_DISCOVERED);
                BluetoothGattService tmpService = gatt.getService(pMon_Services);
                if(tmpService != null) {
                    final BluetoothGattCharacteristic mpu6050 = tmpService.getCharacteristic(pMon_MPU6050_Characteristic);
                    final BluetoothGattCharacteristic bmp = tmpService.getCharacteristic(pMon_BMP_Characteristic);
                    Runnable mpuTO = new Runnable() {
                        @Override
                        public void run() {
                            gatt.readCharacteristic(mpu6050);
                        }
                    };
                    Runnable bmpTO = new Runnable() {
                        @Override
                        public void run() {
                            gatt.readCharacteristic(bmp);
                        }
                    };
                    mHandler.postDelayed(mpuTO, 2000);
                    //                mHandler.postDelayed(bmpTO, 3000); //0.5 seconds //Fixme: Disabled BMP for now.

                }
                //battery service and characteristic
                BluetoothGattService battService = gatt.getService(dev_info);
                if(battService != null) {
                    final BluetoothGattCharacteristic battlevel = battService.getCharacteristic(pMon_BAT_Characteristic);
                    Runnable batTO = new Runnable() {
                        @Override
                        public void run() {
                            gatt.readCharacteristic(battlevel);
                        }
                    };

                    mHandler.postDelayed(batTO, 10000); //0.5 seconds
                }
                //TODO: turn on notify on nrf51 board, since it supports notification.
                BluetoothGattService mNRFService = gatt.getService(nrf_service);
                if (mNRFService != null) {
                    final BluetoothGattCharacteristic mNRF_MPU_NOTIFY_Characteristic = mNRFService.getCharacteristic(nrf_mpu_notify_Characteristic);
                    gatt.setCharacteristicNotification(mNRF_MPU_NOTIFY_Characteristic, true);
                    //standard descriptor setting. needed as documentation specified.
                    BluetoothGattDescriptor descriptor = mNRF_MPU_NOTIFY_Characteristic.getDescriptor(nrf_CCCD);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
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
            if (characteristic.getUuid().equals(nrf_mpu_notify_Characteristic)){
//                Log.d (TAG, characteristic.getUuid().toString());
                handleSensorData(gatt, characteristic);
//                Log.d(TAG, characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0).toString());
            }
        }

        /* the rest of callbacks are not interested for us */

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         int status) {

            handleSensorData(gatt, characteristic);
//            Log.d(TAG, characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0).toString());
            //request after delay fro another read:
            Runnable timeout = new Runnable() {
                @Override
                public void run() {
                    gatt.readCharacteristic(characteristic);
                }
            };
            if(characteristic.getUuid().equals(pMon_BAT_Characteristic)){
                mHandler.postDelayed(timeout, BATTERY_QUERY_TIMEOUT);
            }else
            mHandler.postDelayed(timeout, DATA_RATE_TIMEOUT); //10 seconds
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

    //Checking file storage availability, and setup the path variable for data storage.
    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    // get an public directory for persistent data storage.
    public File getStorageDir(String dirName) {
        // Get the directory for the user's public pictures directory.
//        File file = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_DOCUMENTS), dirName);
        File file = new File(Environment.getExternalStorageDirectory() + "/Documents/" + dirName);

        if (!file.mkdirs() && !file.isDirectory()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }



}