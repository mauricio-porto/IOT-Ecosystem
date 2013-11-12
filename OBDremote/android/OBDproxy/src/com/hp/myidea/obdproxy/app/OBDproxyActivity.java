package com.hp.myidea.obdproxy.app;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.hp.myidea.obdproxy.R;
import com.hp.myidea.obdproxy.service.BluetoothReceiver;

import eu.lighthouselabs.obd.commands.SpeedObdCommand;
import eu.lighthouselabs.obd.commands.control.CommandEquivRatioObdCommand;
import eu.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import eu.lighthouselabs.obd.commands.engine.MassAirFlowObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelEconomyObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelEconomyWithMAFObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelLevelObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelTrimObdCommand;
import eu.lighthouselabs.obd.commands.temperature.AmbientAirTemperatureObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;
import eu.lighthouselabs.obd.enums.FuelTrim;
import eu.lighthouselabs.obd.enums.FuelType;
import eu.lighthouselabs.obd.reader.IPostListener;
import eu.lighthouselabs.obd.reader.io.ObdCommandJob;
import eu.lighthouselabs.obd.reader.io.ObdGatewayService;
import eu.lighthouselabs.obd.reader.io.ObdGatewayServiceConnection;

public class OBDproxyActivity extends Activity {

    private static final String TAG = OBDproxyActivity.class.getSimpleName();

    public static final String OBDPROXY_PREFS = "OBDproxySharedPrefs";

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    private boolean isConfigured = false;

    private BluetoothReceiver btReceiver;

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

    private Handler mHandler = new Handler();

    /**
     * Callback for ObdGatewayService to update UI.
     */
    private IPostListener mListener = null;
    private Intent mServiceIntent = null;
    //private ObdGatewayServiceConnection mServiceConnection = null;

    private SensorManager sensorManager = null;
    private Sensor orientSensor = null;
    private SharedPreferences prefs = null;

    private int speed = 1;
    private double maf = 1;
    private float ltft = 0;
    private double equivRatio = 1;

    private final SensorEventListener orientListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            String dir = "";
            if (x >= 337.5 || x < 22.5) {
                dir = "N";
            } else if (x >= 22.5 && x < 67.5) {
                dir = "NE";
            } else if (x >= 67.5 && x < 112.5) {
                dir = "E";
            } else if (x >= 112.5 && x < 157.5) {
                dir = "SE";
            } else if (x >= 157.5 && x < 202.5) {
                dir = "S";
            } else if (x >= 202.5 && x < 247.5) {
                dir = "SW";
            } else if (x >= 247.5 && x < 292.5) {
                dir = "W";
            } else if (x >= 292.5 && x < 337.5) {
                dir = "NW";
            }
            TextView compass = (TextView) findViewById(R.id.compass_text);
            updateTextView(compass, dir);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }
    };

    public void updateTextView(final TextView view, final String txt) {
        new Handler().post(new Runnable() {
            public void run() {
                view.setText(txt);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        this.startBTReceiver();

        mListener = new IPostListener() {
            public void stateUpdate(ObdCommandJob job) {
                String cmdName = job.getCommand().getName();
                String cmdResult = job.getCommand().getFormattedResult();

                Log.d(TAG, FuelTrim.LONG_TERM_BANK_1.getBank() + " equals " + cmdName + "?");

                if (AvailableCommandNames.ENGINE_RPM.getValue().equals(cmdName)) {
                    TextView tvRpm = (TextView) findViewById(R.id.rpm_text);
                    tvRpm.setText(cmdResult);
                } else if (AvailableCommandNames.SPEED.getValue().equals(cmdName)) {
                    TextView tvSpeed = (TextView) findViewById(R.id.spd_text);
                    tvSpeed.setText(cmdResult);
                    speed = ((SpeedObdCommand) job.getCommand()).getMetricSpeed();
                } else if (AvailableCommandNames.MAF.getValue().equals(cmdName)) {
                    maf = ((MassAirFlowObdCommand) job.getCommand()).getMAF();
                    addTableRow(cmdName, cmdResult);
                } else if (FuelTrim.LONG_TERM_BANK_1.getBank().equals(cmdName)) {
                    ltft = ((FuelTrimObdCommand) job.getCommand()).getValue();
                } else if (AvailableCommandNames.EQUIV_RATIO.getValue().equals(cmdName)) {
                    equivRatio = ((CommandEquivRatioObdCommand) job.getCommand()).getRatio();
                    addTableRow(cmdName, cmdResult);
                } else {
                    addTableRow(cmdName, cmdResult);
                }
            }
        };

    }

    private void startBTReceiver() {
        Log.d(TAG, "\t\t\t\t\tWILL START!!!!");
        Intent intent = new Intent(BluetoothReceiver.ACTION_START);
        intent.setClass(this, BluetoothReceiver.class);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!this.isBound) {
            this.isBound = this.bindService(new Intent("com.hp.myidea.obdproxy.service.BluetoothReceiver"), this.btReceiverConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
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
                    this.sendTextToService(BluetoothReceiver.CONNECT_TO, address);
                }
                break;
            }
            // User did not enable Bluetooth or an error occurred
            Log.d(TAG, "\t\t\tHRM selection failed. Giving up...");
            Toast.makeText(this, R.string.none_paired, Toast.LENGTH_SHORT).show();
            finish();
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so attempt to connect a device
                this.sendTextToService(BluetoothReceiver.CONNECT_TO, null);
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        }
    }

    private void updateConfig() {
        Intent configIntent = new Intent(this, ConfigActivity.class);
        startActivity(configIntent);
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
            return true;
        case STOP_LIVE_DATA:
            stopLiveData();
            return true;
        case SETTINGS:
            updateConfig();
            return true;
            // case COMMAND_ACTIVITY:
            // staticCommand();
            // return true;
        }
        return false;
    }

    private void startLiveData() {
        Log.d(TAG, "Starting live data..");

/*        if (!mServiceConnection.isRunning()) {
            Log.d(TAG, "Service is not running. Going to start it..");
            startService(mServiceIntent);
        }
*/
        // start command execution
        mHandler.post(mQueueCommands);
    }

    private void stopLiveData() {
        Log.d(TAG, "Stopping live data..");

/*        if (mServiceConnection.isRunning())
            stopService(mServiceIntent);
*/
        // remove runnable
        mHandler.removeCallbacks(mQueueCommands);
    }

    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder build = new AlertDialog.Builder(this);
        switch (id) {
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

/*        if (mServiceConnection.isRunning()) {
            startItem.setEnabled(false);
            stopItem.setEnabled(true);
            settingsItem.setEnabled(false);
            commandItem.setEnabled(false);
        } else {
            stopItem.setEnabled(false);
            startItem.setEnabled(true);
            settingsItem.setEnabled(true);
            commandItem.setEnabled(false);
        }
*/        return true;
    }

    private void addTableRow(String key, String val) {
        TableLayout tl = (TableLayout) findViewById(R.id.data_table);
        TableRow tr = new TableRow(this);
        MarginLayoutParams params = new ViewGroup.MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(TABLE_ROW_MARGIN, TABLE_ROW_MARGIN, TABLE_ROW_MARGIN, TABLE_ROW_MARGIN);
        tr.setLayoutParams(params);
        tr.setBackgroundColor(Color.BLACK);
        TextView name = new TextView(this);
        name.setGravity(Gravity.RIGHT);
        name.setText(key + ": ");
        TextView value = new TextView(this);
        value.setGravity(Gravity.LEFT);
        value.setText(val);
        tr.addView(name);
        tr.addView(value);
        tl.addView(tr, new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        /*
         * TODO remove this hack
         * 
         * let's define a limit number of rows
         */
        if (tl.getChildCount() > 10)
            tl.removeViewAt(0);
    }

    /**
	 * 
	 */
    private Runnable mQueueCommands = new Runnable() {
        public void run() {
            /*
             * If values are not default, then we have values to calculate MPG
             */
            Log.d(TAG, "SPD:" + speed + ", MAF:" + maf + ", LTFT:" + ltft);
            if (speed > 1 && maf > 1 && ltft != 0) {
                FuelEconomyWithMAFObdCommand fuelEconCmd = new FuelEconomyWithMAFObdCommand(FuelType.DIESEL, speed, maf, ltft, false /* TODO */);
                TextView tvMpg = (TextView) findViewById(R.id.fuel_econ_text);
                String liters100km = String.format("%.2f", fuelEconCmd.getLitersPer100Km());
                tvMpg.setText("" + liters100km);
                Log.d(TAG, "FUELECON:" + liters100km);
            }

/*            if (mServiceConnection.isRunning())
                queueCommands();
*/
            // run again in 2s
            mHandler.postDelayed(mQueueCommands, 2000);
        }
    };

    /**
	 * 
	 */
    private void queueCommands() {
        final ObdCommandJob airTemp = new ObdCommandJob(new AmbientAirTemperatureObdCommand());
        final ObdCommandJob speed = new ObdCommandJob(new SpeedObdCommand());
        final ObdCommandJob fuelEcon = new ObdCommandJob(new FuelEconomyObdCommand());
        final ObdCommandJob rpm = new ObdCommandJob(new EngineRPMObdCommand());
        final ObdCommandJob maf = new ObdCommandJob(new MassAirFlowObdCommand());
        final ObdCommandJob fuelLevel = new ObdCommandJob(new FuelLevelObdCommand());
        final ObdCommandJob ltft1 = new ObdCommandJob(new FuelTrimObdCommand(FuelTrim.LONG_TERM_BANK_1));
        final ObdCommandJob ltft2 = new ObdCommandJob(new FuelTrimObdCommand(FuelTrim.LONG_TERM_BANK_2));
        final ObdCommandJob stft1 = new ObdCommandJob(new FuelTrimObdCommand(FuelTrim.SHORT_TERM_BANK_1));
        final ObdCommandJob stft2 = new ObdCommandJob(new FuelTrimObdCommand(FuelTrim.SHORT_TERM_BANK_2));
        final ObdCommandJob equiv = new ObdCommandJob(new CommandEquivRatioObdCommand());

        // mServiceConnection.addJobToQueue(airTemp);
/*        mServiceConnection.addJobToQueue(speed);
        // mServiceConnection.addJobToQueue(fuelEcon);
        mServiceConnection.addJobToQueue(rpm);
        mServiceConnection.addJobToQueue(maf);
        mServiceConnection.addJobToQueue(fuelLevel);
        // mServiceConnection.addJobToQueue(equiv);
        mServiceConnection.addJobToQueue(ltft1);
*/        // mServiceConnection.addJobToQueue(ltft2);
        // mServiceConnection.addJobToQueue(stft1);
        // mServiceConnection.addJobToQueue(stft2);
    }

    private ServiceConnection btReceiverConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "BluetoothReceiver connected");
            if (service == null) {
                Log.e(TAG, "Connection to the BluetoothReceiver service failed. Giving up...");
                return;
            }
            receiverSvcConnected = true;

            messageReceiver = new Messenger(service);
            try {
                Message msg = Message.obtain(null, BluetoothReceiver.REGISTER_HANDLER);
                msg.replyTo = serviceMsgReceiver;
                messageReceiver.send(msg);
            } catch (RemoteException e) {
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "BluetoothReceiver disconnected");
            receiverSvcConnected = false;
        }

    };

    private void unbindBTReceiver() {
        Log.d(TAG, "unbindBluetoothReceiver() - supposing it is bound");
        if (this.isBound) {
            if (messageReceiver  != null) {
                try {
                    Message msg = Message.obtain(null, BluetoothReceiver.UNREGISTER_HANDLER);
                    msg.replyTo = serviceMsgReceiver;
                    messageReceiver.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            this.unbindService(btReceiverConnection);
        } else {
            Log.d(TAG, "unbindHRMReceiver() - \tBut it was not!!!");
        }
        this.receiverSvcConnected = false;
        this.isBound = false;
    }

    /**
     * Handler of incoming messages from BluetoothReceiver.
     */
   final Handler serviceMessages = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what < 0) {
                return;
            }
            Log.i(TAG, "Received message: " + BluetoothReceiver.BT_STATUS.values()[msg.what]);
            switch (msg.what) {
            case BluetoothReceiver.OBD_DATA:
                break;
            case BluetoothReceiver.BT_DISABLED:
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                break;
            case BluetoothReceiver.OBD_NOT_CONFIGURED:
                // Launch the BluetoothDeviceList to see devices and do scan
                Intent serverIntent = new Intent(OBDproxyActivity.this, BluetoothDeviceList.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                break;
            case BluetoothReceiver.OBD_CONNECTED:
                break;
            case BluetoothReceiver.CONNECTING:
                Toast.makeText(OBDproxyActivity.this, R.string.title_connecting, Toast.LENGTH_SHORT).show();
                break;
            case BluetoothReceiver.NOT_RUNNING:
                serviceRunning = false;
                //startActivityForResult(new Intent().setClass(CardioTalk.this, Controller.class), REQUEST_START_SERVICE);
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
            bundle.putString(BluetoothReceiver.TEXT_MSG, text);
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