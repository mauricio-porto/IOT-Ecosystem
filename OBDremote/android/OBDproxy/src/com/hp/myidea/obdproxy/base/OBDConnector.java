/**
 * 
 */
package com.hp.myidea.obdproxy.base;

import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.hp.myidea.obdproxy.IServiceProxy;
import com.hp.myidea.obdproxy.R;
import com.hp.myidea.obdproxy.app.OBDproxyActivity;
import com.hp.myidea.obdproxy.service.OBDProxy;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.commands.SpeedObdCommand;
import eu.lighthouselabs.obd.commands.control.CommandEquivRatioObdCommand;
import eu.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import eu.lighthouselabs.obd.commands.engine.MassAirFlowObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelEconomyObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelEconomyWithMAFObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelLevelObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelTrimObdCommand;
import eu.lighthouselabs.obd.commands.temperature.AmbientAirTemperatureObdCommand;
import eu.lighthouselabs.obd.enums.FuelTrim;
import eu.lighthouselabs.obd.enums.FuelType;
import eu.lighthouselabs.obd.reader.IPostListener;
import eu.lighthouselabs.obd.reader.io.ObdCommandJob;

/**
 * @author mapo
 *
 */
public class OBDConnector {

    private static final String TAG = OBDConnector.class.getSimpleName();

    // Message types sent from the BluetoothConnector Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_DEVICE_NAME = 4;

    // Bluetooth and OBD statuses
    public static final int NONE = -1;
    public static final int OBD_NOT_CONFIGURED = 0;
    public static final int BT_DISABLED = 1;
    public static final int OBD_CONNECTED = 2;
    public static final int CONNECTING = 3;
    public static final int OBD_DATA = 4;
    public static final int LOCATION_DATA = 5;
    public static final int NOT_RUNNING = 6;

    public static enum BT_STATUS {
        OBD_NOT_CONFIGURED,
        BT_DISABLED,
        OBD_CONNECTED,
        CONNECTING,
        OBD_DATA,
        NOT_RUNNING
    }

    private int mOBDStatus = NONE;

    // Key names received
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADRESS = "device_address";

    // MAC address of the OBD device
    private String obdBluetoothAddress = null;
    // Name of the connected device
    private String mConnectedDeviceName = null;
    private boolean obdConnected = false;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothConnector connector;
    private IPostListener listener;

    //private ObdCommand[] paramList;
    private ArrayList<ObdCommand> paramList;

    private int speed = 1;
    private double maf = 1;
    private float ltft = 0;
    private double equivRatio = 1;

    private Handler mHandler = new Handler();
    private Handler anotherHandler = new Handler();
    
    private Context owner;
    private IServiceProxy serviceProxy;

    /**
     * 
     */
    public OBDConnector(IServiceProxy service, IPostListener lstnr) {
        super();
        if (service == null) {
            throw new IllegalArgumentException("MUST provide the IServiceProxy");
        }
        if (lstnr == null) {
            throw new IllegalArgumentException("MUST provide the IPostListener");
        }
        this.owner = service.getServiceContext();
        this.serviceProxy = service;
        this.listener = lstnr;
        this.init();
    }

    private void init() {
        if ((mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) == null) {
            Toast.makeText(owner, "Bluetooth is not available", Toast.LENGTH_LONG).show();   // TODO: localize!!!
            return;
        }

        // Connect to the OBD device
        if (!mBluetoothAdapter.isEnabled()) {
            this.mOBDStatus = BT_DISABLED;
            this.serviceProxy.notifyUser("Select to enable bluetooth.", "Must enable bluetooth.");
            return;
        }
        if (!this.connectKnownDevice()) {
            this.mOBDStatus = OBD_NOT_CONFIGURED;
            this.serviceProxy.notifyUser("Select to configure OBD device.", "OBD device not configured.");
            return;
        }

        this.paramList = new ArrayList<ObdCommand>();
        this.paramList.add(new SpeedObdCommand());
        this.paramList.add(new EngineRPMObdCommand());
        this.paramList.add(new FuelLevelObdCommand());
        this.paramList.add(new MassAirFlowObdCommand());
        this.paramList.add(new AmbientAirTemperatureObdCommand());
    }

    public boolean connectKnownDevice() {
        if (obdConnected) {
            Log.d(TAG, "\n\n\n\n\n\nconnectDevice():: obdConnected says it is already connected!!!! Wrong?!?!?!");
            return true;
        }
        this.restoreState();
        if (this.obdBluetoothAddress != null && this.obdBluetoothAddress.length() > 0) {
            this.connectDevice(this.obdBluetoothAddress);
            return true;
        }
        return false;       
    }

    public void connectDevice(String deviceAddress) {
        this.mOBDStatus = CONNECTING;
        if (this.connector == null) {
            this.connector = new BluetoothConnector(this.owner, btMsgHandler);
        }
        this.connector.connect(mBluetoothAdapter.getRemoteDevice(deviceAddress));
    }

    public void stop() {
        if (this.connector == null) {
            this.connector.stop();
        }
    }

    private void sendToDevice(String msg) {
        if (this.connector != null) {
            connector.write(msg.getBytes());
        }
    }

    private void restoreState() {
        // Restore state
        SharedPreferences state = this.owner.getSharedPreferences(OBDproxyActivity.OBDPROXY_PREFS, 0);
        this.obdBluetoothAddress = state.getString("OBDBluetoothAddress", null);
    }

    private void storeState() {
        // Persist state
        SharedPreferences state = this.owner.getSharedPreferences(OBDproxyActivity.OBDPROXY_PREFS, 0);
        SharedPreferences.Editor editor = state.edit();
        editor.putString("OBDBluetoothAddress", this.obdBluetoothAddress);
        editor.commit();
    }

    // The Handler that gets information back from the BluetoothConnector
    private final Handler btMsgHandler = new Handler() {
        int counter = 0;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothConnector.STATE_CONNECTED:
                    OBDConnector.this.mOBDStatus = OBD_CONNECTED;
                    obdConnected = true;
                    serviceProxy.notifyBTState(OBDConnector.this.mOBDStatus);
                    break;
                case BluetoothConnector.STATE_CONNECTING:
                    OBDConnector.this.mOBDStatus = CONNECTING;
                    serviceProxy.notifyBTState(OBDConnector.this.mOBDStatus);
                    break;
                case BluetoothConnector.STATE_FAILED:
                    OBDConnector.this.mOBDStatus = OBD_NOT_CONFIGURED;
                    serviceProxy.notifyBTState(OBDConnector.this.mOBDStatus);
                    break;
                case BluetoothConnector.STATE_LISTEN:
                case BluetoothConnector.STATE_NONE:
                    break;
                }
                break;
            case MESSAGE_READ:
                Log.d(TAG, "Data received.");
                if (msg.arg1 > 0) { // msg.arg1 contains the number of bytes read
                    Log.d(TAG, "\tRead size: " + msg.arg1);
                    byte[] readBuf = (byte[]) msg.obj;
                    byte[] readBytes = new byte[msg.arg1];
                    System.arraycopy(readBuf, 0, readBytes, 0, msg.arg1);
                    Log.d(TAG, "\tAs Hex: " + asHex(readBytes));
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                obdBluetoothAddress = msg.getData().getString(DEVICE_ADRESS);
                storeState();
                Toast.makeText(owner, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
            }
        }
    };

    private String asHex(byte[] buf) {
        char[] HEX_CHARS = "0123456789abcdef".toCharArray();

        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i) {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }

    // **********************************************
    // ATE AQUI, COISAS MOVIDAS DO OBDProxy
    // DAQUI PRA BAIXO, COISA NOVA
    // **********************************************

    public void startLiveData() {
        
    }

    public void stopLiveData() {
        
    }

    public void setParamList(ArrayList<ObdCommand> list) {
        if (list == null || list.size() == 0) {
            return;
        }
        this.paramList = list;
    }

    public ArrayList<ObdCommand> getParamList() {
        return this.paramList;
    }

    public boolean addParam(ObdCommand param) {
        boolean result = false;
        if (param != null && !this.paramList.contains(param)) {
            result = this.paramList.add(param);
        }
        return result;
    }

    public boolean removeParam(ObdCommand param) {
        boolean result = false;
        if (param != null && this.paramList.contains(param)) {
            result = this.paramList.remove(param);
        }
        return result;
    }

    private void startStopLiveData() {
        // Apenas para registrar como o OBD reader faz...
        mHandler.post(mQueueCommands);  // P/ iniciar
        mHandler.removeCallbacks(mQueueCommands); // P/ parar
    }

    private void repeatSomething() {
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                anotherHandler.post(new Runnable() {
                    
                    @Override
                    public void run() {
                        // The anotherHandler will run/call here the real job
                    }
                });
            }
        }).start();
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
                String liters100km = String.format("%.2f", fuelEconCmd.getLitersPer100Km());
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

}
