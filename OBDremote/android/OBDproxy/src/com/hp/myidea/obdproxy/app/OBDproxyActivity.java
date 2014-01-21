package com.hp.myidea.obdproxy.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.hp.myidea.obdproxy.R;
import com.hp.myidea.obdproxy.base.OBDConnector;
import com.hp.myidea.obdproxy.drawable.CoolantGaugeView;
import com.hp.myidea.obdproxy.service.OBDProxy;

public class OBDproxyActivity extends Activity {

    private static final String TAG = OBDproxyActivity.class.getSimpleName();

    public static final String OBDPROXY_PREFS = "OBDproxySharedPrefs";

    private Writer writer;

    private boolean isBound = false;
    private Messenger messageReceiver = null;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private CoolantGaugeView coolView;
    private TextView tempText;
    private TextView rpmText;
    private TextView speedText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // If the adapter is null, then Bluetooth is not supported
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();   // TODO: localize!!!
            finish();
            return;
        }

        this.coolView = (CoolantGaugeView)findViewById(R.id.coolant_gauge);
        this.tempText = (TextView)findViewById(R.id.cool_temp_text);
        this.rpmText = (TextView)findViewById(R.id.rpm_text);
        this.speedText = (TextView)findViewById(R.id.speed_text);

        this.startOBDProxy();

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (!this.isBound) {
            this.isBound = this.bindService(new Intent("com.hp.myidea.obdproxy.service.OBDProxy"), this.proxyServiceConnection, Context.BIND_AUTO_CREATE);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String fileName = sdf.format(Calendar.getInstance().getTime()) + ".log";
        
        try {
            this.writer = new BufferedWriter(new FileWriter(new File(getFilesDir(), fileName)));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "ERROR: ", e);
            // nothing to do
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "ERROR: ", e);
        }

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

        try {
            this.writer.close();
        } catch (IOException e) {
            // nothing to do
        }

        this.unbindProxyService();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String[] results = {"OK","CANCELED","FIRST_USER"};
        Log.d(TAG, "onActivityResult with code: " + ((resultCode < 2)?results[1+resultCode]:"User defined"));
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When BluetoothDeviceList returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras().getString(BluetoothDeviceList.EXTRA_DEVICE_ADDRESS);
                // Attempt to connect to the device
                Log.d(TAG, "\n\n\n\nonActivityResult() - O ENDERECO DO DEVICE EH: " + address + "\n\n\n\n");
                if (address != null) {
                    this.sendTextToService(OBDProxy.CONNECT_TO, address);
                }
                break;
            }
            // User did not enable Bluetooth or an error occurred
            Log.d(TAG, "\t\t\tOBD selection failed. Giving up...");
            Toast.makeText(this, R.string.none_paired, Toast.LENGTH_SHORT).show();
            finish();
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so attempt to connect a device
                this.sendTextToService(OBDProxy.CONNECT_TO, null);
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                this.stopOBDProxy();
                finish();
            }
            break;
        }
    }

    private void startOBDProxy() {
        Log.d(TAG, "startOBDProxy()");
        Intent intent = new Intent(OBDProxy.ACTION_START);
        intent.setClass(this, OBDProxy.class);
        startService(intent);
    }

    private void stopOBDProxy() {
        Log.d(TAG, "stopOBDProxy()");
        Intent intent = new Intent(OBDProxy.ACTION_STOP);
        intent.setClass(this, OBDProxy.class);
        stopService(intent);
    }

    private ServiceConnection proxyServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "OBDProxy connected");
            if (service == null) {
                Log.e(TAG, "Connection to the OBDProxy service failed. Giving up...");
                return;
            }
            messageReceiver = new Messenger(service);
            try {
                Message msg = Message.obtain(null, OBDProxy.REGISTER_HANDLER);
                msg.replyTo = serviceMsgReceiver;
                messageReceiver.send(msg);
            } catch (RemoteException e) {
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "OBDProxy disconnected");
        }

    };

    private void unbindProxyService() {
        Log.d(TAG, "unbindProxyService() - supposing it is bound");
        if (this.isBound) {
            if (messageReceiver != null) {
                try {
                    Message msg = Message.obtain(null, OBDProxy.UNREGISTER_HANDLER);
                    msg.replyTo = serviceMsgReceiver;
                    messageReceiver.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            this.unbindService(proxyServiceConnection);
        } else {
            Log.d(TAG, "unbindProxyService() - \tBut it was not!!!");
        }
        this.isBound = false;
    }

    /**
     * Handler of incoming messages from OBDProxy.
     */
   final Handler serviceMessages = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what < 0) {
                return;
            }
            Log.d(TAG, "Received message: " + msg.what);
            switch (msg.what) {
            case OBDProxy.OBD_DATA:
                final String data = msg.getData().getString(OBDProxy.TEXT_MSG);
                Log.d(TAG, "Received data: " + data);
                try {
                    writer.write(data + "\n");
                } catch (Exception e) {
                    // nothing to do
                }
                try {
                    showReceivedData(data);
                } catch (JSONException e) {
                    Log.e(TAG, "Error: ", e);
                }
                break;
            case OBDConnector.BT_DISABLED:
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                break;
            case OBDConnector.OBD_NOT_CONFIGURED:
                // Launch the BluetoothDeviceList to see devices and do scan
                Intent serverIntent = new Intent(OBDproxyActivity.this, BluetoothDeviceList.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                break;
            case OBDConnector.OBD_CONNECTED:
                break;
            case OBDConnector.CONNECTING:
                Toast.makeText(OBDproxyActivity.this, R.string.title_connecting, Toast.LENGTH_SHORT).show();
                break;
            case OBDConnector.NOT_RUNNING:
                break;
            default:
                break;
            }
        }
    };
    final Messenger serviceMsgReceiver = new Messenger(serviceMessages);

    private void sendTextToService(int what, String text) {
        if (messageReceiver != null) {
            Message msg = Message.obtain(null, what);
            Bundle bundle = new Bundle();
            bundle.putString(OBDProxy.TEXT_MSG, text);
            msg.setData(bundle);
            try {
                messageReceiver.send(msg);
            } catch (RemoteException e) {
                // Nothing to do
            }
        } else {
            Log.d(TAG, "sendTextToService() - NO Service handler to receive!");
        }       
    }

    private void showReceivedData(String dataReceived) throws JSONException {
/*
 * {"timestamp":"2014-01-12_19_48_11", // name:String
 * "location":[-30.14781,-51.21668], // name:array[number, number]
 * "data":{"10":["Engine RPM",831],  // name:objectData, onde objectData é
 * "9":["Engine Load",99],           // name:array[String, number]
 * "11":["Vehicle Speed",0],
 * "6":["Ambient Air Temperature",24],
 * "7":["Coolant Temperature",35],
 * "8":["Intake Air Temperature",24],
 * "12":["Mass Air Flow",7]}}
 *
*/

        JSONObject jsonReceived = new JSONObject(dataReceived);
        String timestamp = jsonReceived.getString("timestamp");
        JSONArray arrayLocation = jsonReceived.getJSONArray("location");
        JSONObject objectData = jsonReceived.getJSONObject("data");

        JSONArray param = objectData.getJSONArray("10");    // Engine RPM
        final int rpm = param.getInt(1);

        param = objectData.getJSONArray("11");    // Vehicle Speed
        final int speed = param.getInt(1);

        param = objectData.getJSONArray("7");    // Coolant Temperature
        final int coolantTemp = param.getInt(1);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                coolView.setTemp(coolantTemp);
                tempText.setText(coolantTemp + " C");
                rpmText.setText(rpm + "");
                speedText.setText(speed + " km/h");
            }
        });
    }
}