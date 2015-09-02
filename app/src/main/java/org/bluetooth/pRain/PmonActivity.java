package org.bluetooth.pRain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.os.IBinder;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/* this activity's purpose is to show how to use particular type of devices in easy and fast way */
public class PmonActivity extends Activity {

    private static final String TAG = "PmonActivity";
    private static final int MAX_PROGRESS_ANGLE = 20;
    private static final short CANCEL_CONN_TO = 8000;
    private Handler mHandler = null;
    private Map<String, double[]> initialVec = new HashMap<String, double[]>(); //intial
    private Map<String, double[]> currentVec = new HashMap<String, double[]>(); //keep track of latest update.
    private Map<String, double[]> previousVec = new HashMap<String, double[]>();

    private BleWrapper mBleWrapper = null;
    private static final int ENABLE_BT_REQUEST_ID = 1;

    //
//    public static final String EXTRAS_DEVICE_NAME = PeripheralActivity.EXTRAS_DEVICE_NAME;
//    public static final String EXTRAS_DEVICE_ADDRESS = PeripheralActivity.EXTRAS_DEVICE_ADDRESS;
    private EditText mConsole = null;
    private TextView mTextView = null;
    private ListView listview = null;
    private TextView angle1 = null;
    private TextView angle2 = null;
    private TextView angle3 = null;
    private TextView batt1;
    private TextView batt2;
    private TextView batt3;
    private TextView status1;
    private TextView status2;
    private TextView status3;
    private ProgressBar progress1, progress2, progress3;
    private boolean s1Online =false;
    private boolean s2Online=false;
    private boolean s3Online=false;
    private boolean s1Found=false;
    private boolean s2Found=false;
    private boolean s3Found=false;

    private  AlertDialog alert11;
    private pMonService mPmonService;
    private pMonSensorItemAdapter mListAdapter;
    private double sensitivity = 1f;
    public static final String SENSOR1 = "pRain1";
    public static final String SENSOR2 = "pRain2";
    public static final String SENSOR3 = "pRain3";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // BLE related stuff:
        // create BleWrapper with empty callback object except uiDeficeFound function (we need only that here)
        mBleWrapper = new BleWrapper(this, new BleWrapperUiCallbacks.Null() {
            @Override
            public void uiDeviceFound(final BluetoothDevice device, final int rssi, final byte[] record) {
                handleFoundDevice(device, rssi, record);
            }
        });

        // check if we have BT and BLE on board
        if (mBleWrapper.checkBleHardwareAvailable() == false) {
            bleMissing();

        }
        mBleWrapper.initialize();
        mBleWrapper.startScanning();
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mBleWrapper == null) return;
//
//                mBleWrapper.stopScanning();
//            }
//        }, 2000);
        setContentView(R.layout.activity_pmon);
//        listview = (ListView) findViewById(R.id.listView);
//        mListAdapter = new pMonSensorItemAdapter(this);
        angle1 = (TextView) findViewById(R.id.angle1);
        angle2 = (TextView) findViewById(R.id.angle2);
        angle3 = (TextView) findViewById(R.id.angle3);

        batt1 = (TextView) findViewById(R.id.batt1);
        batt2 = (TextView) findViewById(R.id.batt2);
        batt3 = (TextView) findViewById(R.id.batt3);

        status1 = (TextView) findViewById(R.id.status1);
        status2 = (TextView) findViewById(R.id.status2);
        status3 = (TextView) findViewById(R.id.status3);

        progress1 = (ProgressBar) findViewById(R.id.progressBar1);
        progress2 = (ProgressBar) findViewById(R.id.progressBar2);
        progress3 = (ProgressBar) findViewById(R.id.progressBar3);
// load settings file from the storage, break if not exists.
        try {
            readSettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO: Check availability of sensors based on device address from readings.

        showConnectingDialog();


//        Button adjustButton = (Button) findViewById(R.id.button);

        // Get device info from scanActivity.
//        Intent intent = getIntent();
//        mBTPeripherals = new HashMap<String, String>();

//        ArrayList<String> mDeviceAddrs = intent.getStringArrayListExtra(EXTRAS_DEVICE_ADDRESS);
//        ArrayList<String> mDeviceName = intent.getStringArrayListExtra(EXTRAS_DEVICE_NAME);


        //put parced inent info to mBTperipheral hashmap.
//        for (int j = 0; j < mDeviceAddrs.size(); j++) {
//            mBTPeripherals.put(mDeviceAddrs.get(j), mDeviceName.get(j));
//        }
        //Do not Need to bind agian.
//        Intent pMonServiceIntent = new Intent(this, pMonService.class);
//        bindService(pMonServiceIntent, mServiceConnection, 0);

//        mConsole = (EditText) findViewById(R.id.hr_console_item);
        Log.d("MYLOG", "Creating activity");

        // Show the Up button in the action bar.
//        getActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.main_title));
//        mConsole = (EditText) findViewById(R.id.hr_console_item);
        mTextView = (TextView) findViewById(R.id.hr_text_view);

        mHandler = new Handler();


        Log.d("MYLOG", "Activity created");

//        adjustButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//
//                for (final Map.Entry<String, double[]> entry : currentVec.entrySet()) {
//                    initialVec.put(entry.getKey(), entry.getValue());
//
//                }
//                mHandler.post(new Runnable() {
//                    public void run() {
//                        try {
//                            writeSettings(initialVec);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
//            }
//
//
//        });

    }
    private boolean alreadyFoundMe = false;
    private boolean alreadyFoundAll = false;
    private void handleFoundDevice(BluetoothDevice device, int rssi, byte[] record) {

        //check if the sensor found matches the one loaded in settings
        if (!alreadyFoundMe && !initialVec.containsKey(device.getAddress())){
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setMessage("New sensors detected, if you have just replaced your old sensor, " +
                    "please update your personal settings, the sensor will not be usable until you have done so. Otherwise just ignore this message.");//Fixme: add to string res.
            builder1.setCancelable(false);
            builder1.setPositiveButton("OK, I understand.",null);


            AlertDialog alert11 = builder1.create();
            alert11.show();
            alreadyFoundMe = true;
        }

        switch(device.getName()){
            case SENSOR1:
                s1Found = true;
                break;
            case SENSOR2:
                s2Found = true;
                break;
            case SENSOR3:
                s3Found = true;
                break;
            default: break;
        }
       if (s1Found && s2Found && s3Found ) if (!alreadyFoundAll) {

           if(mBleWrapper != null){
           mBleWrapper.stopScanning();
           mBleWrapper.close();
           mBleWrapper = null;
           }
           startPmonService();

       }
    }


//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                NavUtils.navigateUpFromSameTask(this);
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    protected void onResume() {
        super.onResume();
        log("Resuming activity");
//        listview.setAdapter(mListAdapter);
        registerReceiver(mPmonReceiver, mIntentFilter());


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
//        unbindService(mServiceConnection);
        Intent pMonServiceIntent = new Intent(this, pMonService.class);
        stopService(pMonServiceIntent);
    }
    /* check if user agreed to enable BT */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // user didn't want to turn on BT
        if (requestCode == ENABLE_BT_REQUEST_ID) {
            if (resultCode == Activity.RESULT_CANCELED) {
                btDisabled();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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

    //start the pMonService
    private void startPmonService() {


        ArrayList<String> tmpDeviceNames = new ArrayList<>();
        ArrayList<String> tmpDeviceAddrs = new ArrayList<>();

        Intent pMonServiceIntent = new Intent(this, pMonService.class);

        for (final Map.Entry<String, double[]> entry : initialVec.entrySet()) {

                tmpDeviceAddrs.add(entry.getKey());
                tmpDeviceNames.add("Default Device"); //Fixme: Consider Remove this later

        }

        if (!tmpDeviceAddrs.isEmpty()) {

            //pass it along to our background service: pMonService

            pMonServiceIntent.putStringArrayListExtra(PeripheralActivity.EXTRAS_DEVICE_NAME, tmpDeviceNames);
            pMonServiceIntent.putStringArrayListExtra(PeripheralActivity.EXTRAS_DEVICE_ADDRESS, tmpDeviceAddrs);

            startService(pMonServiceIntent);

        } else {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setMessage("Device Address Empty! Your personal setting is incorrect, please contact us!");//Fixme: add to string res.
            builder1.setCancelable(true);
            builder1.setPositiveButton("OK, I understand!",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });


            AlertDialog alert11 = builder1.create();
            alert11.show();
        }
//        startActivity(new Intent(this, PmonActivity.class));
    }


    //system related bt initialization functions.


    private void btDisabled() {
        Toast.makeText(this, "Sorry, BT has to be turned ON for us to work!", Toast.LENGTH_LONG).show();
        finish();
    }

    private void bleMissing() {
        Toast.makeText(this, "BLE Hardware is required but not available!", Toast.LENGTH_LONG).show();
        finish();
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //data processing:

    private void processData(String deviceName, String deviceAddress, String characteristic, byte[] data) {
//        Log.d(TAG, "Received data from:" + characteristic + data.toString() + deviceAddress + "@" + deviceName);
        if (!initialVec.containsKey(deviceAddress)) {
            double[] vec = {0, -1, 0};
            initialVec.put(deviceAddress, vec);
            currentVec.put(deviceAddress, vec);
        }
        if (characteristic.equals(BleDefinedUUIDs.Characteristic.PMON_MPU6050.toString()) || characteristic.equals(BleDefinedUUIDs.Characteristic.NRF_MPU_NOTIFY.toString())) {

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

                updateSensorDataOnUI(deviceName, angle);

//                int position = mListAdapter.addAngle(deviceName, deviceAddress, (float) angle);
//                updateListView(position, String.format("%.0f", angle) + "°");
//            Log.d(TAG,"received mpudata!");
            }
        }
        if (characteristic.equals(BleDefinedUUIDs.Characteristic.PMON_BMP.toString())) {
            //TODO: display BMP readings to UI

        }
        if (characteristic.equals(BleDefinedUUIDs.Characteristic.BATTERY_LEVEL.toString())) {
            //TODO: display BMP readings to UI
            int battery = 0;
            if (data != null && data.length > 0) {
                final ByteBuffer bb = ByteBuffer.allocate(20);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.put(data);
                bb.rewind();
                battery = (bb.getShort() & 0xffff);
            }
            switch (deviceName) {
                case SENSOR1:
                    batt1.setText("Battery Level: " + battery + "%");
                    break;
                case SENSOR2:
                    batt2.setText("Battery Level: " + battery + "%");
                    break;
                case SENSOR3:
                    batt3.setText("Battery Level: " + battery + "%");
                    break;
                default:
                    break;
            }
        }
    }


    // Handles various events fired by the Service that has been bound to.
    protected final BroadcastReceiver mPmonReceiver = new BroadcastReceiver() {
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
                    deviceName = intent.getStringExtra(pMonService.EXTRAS_DEVICE_NAME);
                    deviceAddress = intent.getStringExtra(pMonService.EXTRAS_DEVICE_ADDRESS);
                    switch (deviceName) {
                        case SENSOR1:
                            status1.setText(R.string.online);
                            s1Online = true;
                            break;
                        case SENSOR2:
                            status2.setText(R.string.online);
                            s2Online =true;
                            break;
                        case SENSOR3:
                            status3.setText(R.string.online);
                            s3Online =true;
                            break;
                        default:
                            break;


                    }
//                    if (status1.getText().equals(R.string.online) && status2.getText().equals(R.string.online) && status3.getText().equals(R.string.online)){
                    if (s1Online && s2Online && s3Online ){
                        //use a timer to delay dialog cancelation as at beginning of connecting to new sensor data is not pulled yet.
                        // doing this makes the app feel more resonsive.
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                cancelConnectingDialog();
                            }
                        },CANCEL_CONN_TO);


                    }
                    break;
                case pMonService.ACTION_GATT_DISCONNECTED:
                    //TODO: set disconnected status in UI, also may trigger alert to user to reconnect.
                    deviceName = intent.getStringExtra(pMonService.EXTRAS_DEVICE_NAME);
                    deviceAddress = intent.getStringExtra(pMonService.EXTRAS_DEVICE_ADDRESS);
                    showDisconnectinDetectedDialog();
                    switch (deviceName) {
                        case SENSOR1:
                            status1.setText(R.string.offline);
                            s1Online = false;
                            break;
                        case SENSOR2:
                            status2.setText(R.string.offline);
                            s2Online =false;
                            break;
                        case SENSOR3:
                            status3.setText(R.string.offline);
                            s3Online =false;
                            break;
                        default:
                            break;
                    }
                    break;
                case pMonService.ACTION_GATT_SERVICES_DISCOVERED:
                    //not quite useful but include for forward compatibility
                    break;
                default:
                    break;
            }

        }
    };

    private void cancelConnectingDialog() {
        alert11.cancel();
    }

    private void showConnectingDialog() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Searching for sensors... \nMake sure all the sensors are switched on!");//Fixme: add to string res.
        builder1.setCancelable(true);
        builder1.setPositiveButton("Exit",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        alert11 = builder1.create();
        alert11.show();
    }
    private void showDisconnectinDetectedDialog() {
        if (alert11.isShowing()) return;
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Device disconnection detected, try reconnecting...");//Fixme: add to string res.
        builder1.setCancelable(true);
        builder1.setPositiveButton("Exit",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        alert11 = builder1.create();
        alert11.show();
    }

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

//    public boolean updateListView(int position, String angle) {
//        int first = listview.getFirstVisiblePosition();
//        int last = listview.getLastVisiblePosition();
//        if(position < first || position > last) {
//            //just update your DataSet
//            //the next time getView is called
//            //the ui is updated automatically
//            return false;
//        }
//        else {
//            View convertView = listview.getChildAt(position - first);
//            //this is the convertView that you previously returned in getView
//            //just fix it (for example:)
////            seakBar bar = (SeakBar) convertView.findViewById(R.id.progress);
//            TextView mTextView = (TextView) convertView.findViewById(R.id.angle);
//            mTextView.setText(angle);
////            bar.setProgress(newProgress);
//            return true;
//        }
//    }

    public void updateSensorDataOnUI(String deviceName, double angle) {
        final String angleStr = String.format("%.0f", angle) + "°";
        final int angleProgress = (int) angle * 100 / MAX_PROGRESS_ANGLE;
        switch (deviceName) {
            case SENSOR1:
                mHandler.post(new Runnable() {
                    public void run() {
                        angle1.setText(angleStr);
                        progress1.setProgress(angleProgress);
                    }
                });

                break;
            case SENSOR2:
                mHandler.post(new Runnable() {
                    public void run() {
                        angle2.setText(angleStr);
                        progress2.setProgress(angleProgress);
                    }
                });
                break;
            case SENSOR3:
                mHandler.post(new Runnable() {
                    public void run() {
                        angle3.setText(angleStr);
                        progress3.setProgress(angleProgress);
                    }
                });
                break;
            default:
                break;

        }
    }
    //Fixme: this shoould be on a single thead.

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

    private void readSettings() throws IOException {

        File file = new File(getStorageDir("postureData"), "settings.txt"); //use hardcode settings.txt as setting file.
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (fis == null){   // Alert user to contact us for his settigns profile.

                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setMessage("No posture setting found on your device, please contact us to load the settings for you!");//Fixme: add to string res.
                builder1.setCancelable(false);
                builder1.setPositiveButton("OK, I understand!",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                System.exit(0);
                            }
                        });


                AlertDialog alert11 = builder1.create();
                alert11.show();


            return;}
        JsonReader reader = new JsonReader(new InputStreamReader(fis, "UTF-8"));
        try {
            readMessageArray(reader);
            return;
        } finally {
            reader.close();
        }

    }
    public void readMessageArray(JsonReader reader) throws IOException {
    reader.beginArray();
        while (reader.hasNext()){
            readMessage(reader);
        }
    }


    public void readMessage(JsonReader reader) throws IOException {

        String device = null;

        double[] vec = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("device")) {
                device = reader.nextString();
            } else if (name.equals("initVec") && reader.peek() != JsonToken.NULL) {
                vec = readDoublesArray(reader);
            } else {
                reader.skipValue();
            }
        }
        initialVec.put(device, vec);
        reader.endObject();
    }

    public double[] readDoublesArray(JsonReader reader) throws IOException {
        double[] doubles = new double[3];

        reader.beginArray();
        int ii = 0;
        while (reader.hasNext()) {
            doubles[ii] = reader.nextDouble();
            ii++;
        }
        reader.endArray();
        return doubles;
    }

    public void writeSettings(Map<String, double[]> vec) throws IOException {
        if (!isExternalStorageWritable()) {
            return;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd");
        Date now = new Date();
        String dateStr = "Settings Uploaded on: " + formatter.format(now);
        File file = new File(getStorageDir("postureData"), "settings.txt"); //use hardcode settings.txt as setting file.
        FileOutputStream fos = new FileOutputStream(file,false);
        //clear its content
        fos.close();

        fos = new FileOutputStream(file);

        final FileOutputStream finalFos = fos;
        JsonWriter writer = null;
        writer = new JsonWriter(new OutputStreamWriter(finalFos, "UTF-8"));
        writer.beginArray();
        for (final Map.Entry<String, double[]> entry : initialVec.entrySet()) {
            writer.beginObject();
            writer.name("device").value(entry.getKey());
            writer.name("initVec");
            writeDoublesArray(writer, entry.getValue());
            writer.endObject();
        }
        writer.endArray();
        writer.close();}


    public void writeDoublesArray(JsonWriter writer, double[] doubles) throws IOException {
        writer.beginArray();
        for (Double value : doubles) {
            writer.value(value);
        }
        writer.endArray();
    }


}


