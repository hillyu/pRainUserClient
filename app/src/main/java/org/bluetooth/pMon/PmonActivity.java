package org.bluetooth.pMon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
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
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

/* this activity's purpose is to show how to use particular type of devices in easy and fast way */
public class PmonActivity extends Activity {

    private static final String TAG = "PmonActivity";
    private Handler mHandler = null;
//    private Map<String, String> mBTPeripherals; //map <address, name>

    //
//    public static final String EXTRAS_DEVICE_NAME = PeripheralActivity.EXTRAS_DEVICE_NAME;
//    public static final String EXTRAS_DEVICE_ADDRESS = PeripheralActivity.EXTRAS_DEVICE_ADDRESS;
    private EditText mConsole = null;
    private TextView mTextView = null;

    private pMonService mPmonService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hrdemo);

        // Get device info from scanActivity.
//        Intent intent = getIntent();
//        mBTPeripherals = new HashMap<String, String>();

//        ArrayList<String> mDeviceAddrs = intent.getStringArrayListExtra(EXTRAS_DEVICE_ADDRESS);
//        ArrayList<String> mDeviceName = intent.getStringArrayListExtra(EXTRAS_DEVICE_NAME);



        //put parced inent info to mBTperipheral hashmap.
//        for (int j = 0; j < mDeviceAddrs.size(); j++) {
//            mBTPeripherals.put(mDeviceAddrs.get(j), mDeviceName.get(j));
//        }
        Intent pMonServiceIntent = new Intent(this,pMonService.class);
        bindService(pMonServiceIntent, mServiceConnection, 0);

        mConsole = (EditText) findViewById(R.id.hr_console_item);
        Log.d("MYLOG", "Creating activity");

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Heart Rate Demo");
        mConsole = (EditText) findViewById(R.id.hr_console_item);
        mTextView = (TextView) findViewById(R.id.hr_text_view);

        mHandler = new Handler();


        Log.d("MYLOG", "Activity created");
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


    }

    @Override
    protected void onPause() {
        super.onPause();

//		disableNotificationForHr(); //TODO: BUGgy so disabled.
//		disconnectFromDevice();
//		closeGatt();
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
               Log.e(TAG,"unInitialized Service connected, this is wrong!");
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

    // Handles various events fired by the Service that has been bound to.
    private final BroadcastReceiver mPmonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //TODO: do something for data recieved via intent's ACTIONs.
        }
    };


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

}
