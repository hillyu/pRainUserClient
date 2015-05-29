package org.bluetooth.pMon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

/* this activity's purpose is to show how to use particular type of devices in easy and fast way */
public class PmonActivity extends Activity {

    private static final String TAG = "PmonActivity";
    private Handler mHandler = null;
    private Map<String, double[]> initialVec = new HashMap<String,double[]>(); //intial
    private Map<String, double[]> currentVec = new HashMap<String,double[]>(); //keep track of latest update.
    private Map<String, double[]> previousVec = new HashMap<String,double[]>();

    //
//    public static final String EXTRAS_DEVICE_NAME = PeripheralActivity.EXTRAS_DEVICE_NAME;
//    public static final String EXTRAS_DEVICE_ADDRESS = PeripheralActivity.EXTRAS_DEVICE_ADDRESS;
    private EditText mConsole = null;
    private TextView mTextView = null;
    private ListView listview = null;

    private pMonService mPmonService;
    private pMonSensorItemAdapter mListAdapter;
    private double sensitivity = 0.15f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pmon);
        listview = (ListView) findViewById(R.id.listView);
        mListAdapter = new pMonSensorItemAdapter(this);

        Button adjustButton = (Button) findViewById(R.id.button);

        // Get device info from scanActivity.
//        Intent intent = getIntent();
//        mBTPeripherals = new HashMap<String, String>();

//        ArrayList<String> mDeviceAddrs = intent.getStringArrayListExtra(EXTRAS_DEVICE_ADDRESS);
//        ArrayList<String> mDeviceName = intent.getStringArrayListExtra(EXTRAS_DEVICE_NAME);


        //put parced inent info to mBTperipheral hashmap.
//        for (int j = 0; j < mDeviceAddrs.size(); j++) {
//            mBTPeripherals.put(mDeviceAddrs.get(j), mDeviceName.get(j));
//        }
        Intent pMonServiceIntent = new Intent(this, pMonService.class);
        bindService(pMonServiceIntent, mServiceConnection, 0);

//        mConsole = (EditText) findViewById(R.id.hr_console_item);
        Log.d("MYLOG", "Creating activity");

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Heart Rate Demo");
//        mConsole = (EditText) findViewById(R.id.hr_console_item);
        mTextView = (TextView) findViewById(R.id.hr_text_view);

        mHandler = new Handler();


        Log.d("MYLOG", "Activity created");

        adjustButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                for (Map.Entry<String, double []> entry : currentVec.entrySet()){
                    initialVec.put(entry.getKey(),entry.getValue());
                }

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("Resuming activity");
        listview.setAdapter(mListAdapter);
        registerReceiver(mPmonReceiver,mIntentFilter());


    }

    @Override
    protected void onPause() {
        super.onPause();

//		disableNotificationForHr(); //TODO: BUGgy so disabled.
//		disconnectFromDevice();
//		closeGatt();
        unregisterReceiver(mPmonReceiver);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            mPmonService = ((pMonService.LocalBinder) service).getService();
            //Check if it is the service is already running if not initialize it.
            if (!mPmonService.isInitialized()) {
                Log.e(TAG, "unInitialized Service connected, this is wrong!");
//                if (!mPmonService.initialize()) {
//                    Log.e(TAG, "Unable to initialize pMonService");
//                    finish();
//                }
            }
            //TODO: do something everytime the client reconnected to service
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mPmonService = null;
        }
    };

    //	private void getAndDisplayHrValue() {
//    	byte[] raw = mBTValueCharacteristic.getValue();
//    	int index = ((raw[0] & 0x01) == 1) ? 2 : 1;
//    	int format = (index == 1) ? BluetoothGattCharacteristic.FORMAT_UINT8 : BluetoothGattCharacteristic.FORMAT_UINT16;
//    	int value = mBTValueCharacteristic.getIntValue(format, index);
//    	final String description = value + " bpm";
//
//    	runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				mTextView.setText(description);
//			}
//    	});
//	}
    //data processing:

    private void processData(String deviceName, String deviceAddress, String characteristic, byte[] data) {
        Log.d(TAG, "Received data from:" + characteristic + data.toString() + deviceAddress + "@" + deviceName);
        if (!initialVec.containsKey(deviceAddress)){
            double[] vec = {0,0,1};
            initialVec.put(deviceAddress,vec);
            currentVec.put(deviceAddress,vec);
        }
        if (characteristic.equals(BleDefinedUUIDs.Characteristic.PMON_MPU6050.toString())) {

            if (data != null && data.length > 0) {
//                final StringBuilder stringBuilder = new StringBuilder(data.length);
//                for (byte byteChar : data)
//                    stringBuilder.append(String.format("%02X ", byteChar));
//                mDataField.setText(new String(data) + "\n" + stringBuilder.toString());
                final ByteBuffer bb = ByteBuffer.allocate(20);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.put(data);
                bb.rewind();
                short x = bb.getShort();
                short y = bb.getShort();
                short z = bb.getShort();
                double[] accReading = new double[]{x, y, z};//put temparary value to rvreg register.


                //keepon reading for temperature sensor MPU6055 only.
                //Short tmp = bb.getShort();
                short gx = bb.getShort();
                short gy = bb.getShort();
                short gz = bb.getShort();

                //reading for quaternion
                short qw = bb.getShort();
                short qx = bb.getShort();
                short qy = bb.getShort();
                short qz = bb.getShort();
                currentVec.put(deviceAddress, accReading);
                double[] cv = {x, y, z};
                //do the low-pass filtering on cv;
                cv = lowPass(cv, previousVec.get(deviceAddress));
                //update the history of cv for next iteration.
                previousVec.put(deviceAddress, cv);
                //get the angle between rv  and cv;
                double angle = getAngle(initialVec.get(deviceAddress), cv);

                int position = mListAdapter.addAngle(deviceName, deviceAddress, (float) angle);
                updateListView(position, String.format("%.0f", angle) + "°");
//            Log.d(TAG,"received mpudata!");
            }
        }
        if (characteristic.equals(BleDefinedUUIDs.Characteristic.PMON_BMP.toString())) {
            //TODO: display BMP readings to UI

        }
    }

    // Handles various events fired by the Service that has been bound to.
    private final BroadcastReceiver mPmonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //TODO: do something for data recieved via intent's ACTIONs.
            final String action = intent.getAction();
            String deviceName;
            String deviceAddress;
            String characteristic;
            byte[] data;
            switch (action) {
                case pMonService.ACTION_DATA_AVAILABLE:
                    deviceName = intent.getStringExtra(pMonService.EXTRAS_DEVICE_NAME);
                    deviceAddress = intent.getStringExtra(pMonService.EXTRAS_DEVICE_ADDRESS);
                    characteristic = intent.getStringExtra(pMonService.EXTRA_CHARACTERISTIC);
                    data = intent.getByteArrayExtra(pMonService.EXTRAS_DATA);

                    processData(deviceName, deviceAddress, characteristic, data);
                    break;
                case pMonService.ACTION_GATT_CONNECTED:
                    //TODO: set connection status in UI
                    break;
                case pMonService.ACTION_GATT_DISCONNECTED:
                    //TODO: set disconnected status in UI, also may trigger alert to user to reconnect.
                    break;
                case pMonService.ACTION_GATT_SERVICES_DISCOVERED:
                    //not quite useful but include for forward compatibility
                    break;
                default:
                    break;
            }

        }
    };

    private static IntentFilter mIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(pMonService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(pMonService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(pMonService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(pMonService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // put new logs into the UI console
    private void log(final String txt) {
        if (mConsole == null) return;

        final String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(new Date());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConsole.setText(timestamp + " : " + txt + "\n" + mConsole.getText());
            }
        });
    }
//update listview;

    public boolean updateListView(int position, String angle) {
        int first = listview.getFirstVisiblePosition();
        int last = listview.getLastVisiblePosition();
        if(position < first || position > last) {
            //just update your DataSet
            //the next time getView is called
            //the ui is updated automatically
            return false;
        }
        else {
            View convertView = listview.getChildAt(position - first);
            //this is the convertView that you previously returned in getView
            //just fix it (for example:)
//            seakBar bar = (SeakBar) convertView.findViewById(R.id.progress);
            TextView mTextView = (TextView) convertView.findViewById(R.id.angle);
            mTextView.setText(angle);
//            bar.setProgress(newProgress);
            return true;
        }
    }


    //math functions:
    //use vector angel method. a.b=\a\*\b\*cos(angle(a,b)) to calculate angle.
    double getAngle(double[] initialVec, double[] currentVec) {
        double m1, m2, d;//2 mode for both vector. and dot product between two vec：d;
        m1 = m2 = d = 0;
        int i = 0;
        while (i < initialVec.length) {
            m1 += Math.pow(initialVec[i], 2);
            m2 += Math.pow(currentVec[i], 2);
            d += initialVec[i] * currentVec[i];
            i++;
        }
        return Math.toDegrees(Math.acos(d / (Math.sqrt(m1 * m2))));
    }

    //
    // low-pass filter for accelarometer.
    // @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
    // @see http://developer.android.com/reference/android/hardware/SensorEvent.html#values
    //
    protected double[] lowPass(double[] input, double[] output) {
        if (output == null) return input;

        for (int i = 0; i < input.length; i++) {
//            output[i] = output[i] + ALPHA * (input[i] - output[i]);
            output[i] = output[i] + sensitivity * (input[i] - output[i]);
        }
        return output;
    }


}
