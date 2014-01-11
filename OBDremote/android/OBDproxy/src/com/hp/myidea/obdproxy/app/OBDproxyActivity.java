package com.hp.myidea.obdproxy.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.hp.myidea.obdproxy.R;
import com.hp.myidea.obdproxy.base.LogTextBox;
import com.hp.myidea.obdproxy.base.OBDConnector;
import com.hp.myidea.obdproxy.service.OBDProxy;

public class OBDproxyActivity extends Activity {

    private static final String TAG = OBDproxyActivity.class.getSimpleName();

    public static final String OBDPROXY_PREFS = "OBDproxySharedPrefs";

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    private boolean isConfigured = false;

    private OBDProxy btReceiver;

    private boolean receiverSvcConnected = false;
    private boolean isBound = false;
    private boolean serviceRunning = false;
    private Messenger messageReceiver = null;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_START_SERVICE = 3;
    
    /*
     * TODO put description
     */
    static final int NO_BLUETOOTH_ID = 0;
    static final int BLUETOOTH_DISABLED = 1;
    static final int NO_GPS_ID = 2;
    static final int START_LIVE_DATA = 3;
    static final int STOP_LIVE_DATA = 4;
    static final int SETTINGS = 5;
    static final int COMMAND_ACTIVITY = 6;
    static final int TABLE_ROW_MARGIN = 7;
    static final int NO_ORIENTATION_SENSOR = 8;

    private boolean useGPS = true;     // TODO: MUST BE A PREFERENCE
    private static final int USE_GPS_DIALOG = 0xb0b0ca;

    private LogTextBox dataView;

    public void updateTextView(final TextView view, final String txt) {
        new Handler().post(new Runnable() {
            public void run() {
                view.setText(txt);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();   // TODO: localize!!!
            finish();
            return;
        }
        this.dataView = (LogTextBox) findViewById(R.id.data_text);
        this.startOBDProxy();

        // Se GPS desligado e aceita usar GPS (settings), abre diálogo para habilitar GPS (tipo checkGPS)
        if (useGPS && !((LocationManager)getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showDialog(USE_GPS_DIALOG);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (!this.isBound) {
            this.isBound = this.bindService(new Intent("com.hp.myidea.obdproxy.service.OBDProxy"), this.btReceiverConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        this.unbindBTReceiver();
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
                Log.d(TAG, "\n\n\n\nonActivityResult() - O ENDERECO DO DEVICE EH: " + address + " e receciverSvcConnected diz: " + this.receiverSvcConnected + "\n\n\n\n");
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

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, START_LIVE_DATA, 0, "Start Live Data");
        menu.add(0, COMMAND_ACTIVITY, 0, "Run Command");
        menu.add(0, STOP_LIVE_DATA, 0, "Stop");
        menu.add(0, SETTINGS, 0, "Settings");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case START_LIVE_DATA:
            startLiveData();
            break;
        case STOP_LIVE_DATA:
            stopLiveData();
            break;
        case SETTINGS:
            break;
        default:
            break;
        }
        return true;
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

    private void startLiveData() {
        Log.d(TAG, "Starting live data..");

/*        if (!mServiceConnection.isRunning()) {
            Log.d(TAG, "Service is not running. Going to start it..");
            startService(mServiceIntent);
        }
*/
        // start command execution
        // mHandler.post(mQueueCommands);
    }

    private void stopLiveData() {
        Log.d(TAG, "Stopping live data..");

/*        if (mServiceConnection.isRunning())
            stopService(mServiceIntent);
*/
        // remove runnable
        // mHandler.removeCallbacks(mQueueCommands);
    }

    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder build = new AlertDialog.Builder(this);
        switch (id) {
        case USE_GPS_DIALOG:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.enable_gps)
                .setMessage(R.string.why_enable_gps)
                .setCancelable(false)  
                .setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {  
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }  
                });  
            builder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {  
                    public void onClick(DialogInterface dialog, int id) {  
                        dialog.cancel();  
                    }
                });
            return builder.create();  
        case NO_BLUETOOTH_ID:
            build.setMessage("Sorry, your device doesn't support Bluetooth.");
            return build.create();
        case BLUETOOTH_DISABLED:
            build.setMessage("You have Bluetooth disabled. Please enable it!");
            return build.create();
        case NO_GPS_ID:
            build.setMessage("Sorry, your device doesn't support GPS.");
            return build.create();
        case NO_ORIENTATION_SENSOR:
            build.setMessage("Orientation sensor missing?");
            return build.create();
        }
        return null;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem startItem = menu.findItem(START_LIVE_DATA);
        MenuItem stopItem = menu.findItem(STOP_LIVE_DATA);
        MenuItem settingsItem = menu.findItem(SETTINGS);
        MenuItem commandItem = menu.findItem(COMMAND_ACTIVITY);
        return true;
    }

    private ServiceConnection btReceiverConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "OBDProxy connected");
            if (service == null) {
                Log.e(TAG, "Connection to the OBDProxy service failed. Giving up...");
                return;
            }
            receiverSvcConnected = true;

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
            receiverSvcConnected = false;
        }

    };

    private void unbindBTReceiver() {
        Log.d(TAG, "unbindBluetoothReceiver() - supposing it is bound");
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
            this.unbindService(btReceiverConnection);
        } else {
            Log.d(TAG, "unbindBTReceiver() - \tBut it was not!!!");
        }
        this.receiverSvcConnected = false;
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dataView.append(data);
                        dataView.append("\n");
                    }
                });
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
                serviceRunning = false;
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

}