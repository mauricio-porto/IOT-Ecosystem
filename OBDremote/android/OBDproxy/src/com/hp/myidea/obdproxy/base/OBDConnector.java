/**
 * 
 */
package com.hp.myidea.obdproxy.base;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.hp.myidea.obdproxy.IProxyService;
import com.hp.myidea.obdproxy.IResultReader;
import com.hp.myidea.obdproxy.app.OBDproxyActivity;

/**
 * @author mapo
 *
 */
public class OBDConnector {

    private static final String TAG = OBDConnector.class.getSimpleName();

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Key names received
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADRESS = "device_address";

    // Bluetooth and OBD statuses
    public static final int NONE = -1;
    public static final int OBD_NOT_CONFIGURED = 0;
    public static final int BT_DISABLED = 1;
    public static final int OBD_CONNECTED = 2;
    public static final int CONNECTING = 3;
    public static final int OBD_DATA = 4;
    public static final int NOT_RUNNING = 5;

    public static enum BT_STATUS {
        OBD_NOT_CONFIGURED,
        BT_DISABLED,
        OBD_CONNECTED,
        CONNECTING,
        OBD_DATA,
        NOT_RUNNING
    }

    private int mOBDStatus = NONE;

    // MAC address of the OBD device
    private String obdBluetoothAddress = null;
    // Name of the connected device
    private String mConnectedDeviceName = null;
    private boolean obdConnected = false;

    private BluetoothConnector connector;

    private Handler mHandler = new Handler();
    private Handler anotherHandler = new Handler();
    
    private Context owner;
    private IProxyService service;

    OBDCommand[] commandList = {
            OBDCommand.ENGINE_RPM,
            OBDCommand.SPEED,
            OBDCommand.AMBIENT_AIR_TEMPERATURE,
            OBDCommand.COOLANT_TEMPERATURE,
            OBDCommand.FUEL_LEVEL};

    /**
     * 
     */
    public OBDConnector(IProxyService service) {
        super();
        if (service == null) {
            throw new IllegalArgumentException("MUST provide the IProxyService");
        }
        this.service = service;
        this.owner = service.getServiceContext();
        this.init();
    }

    private void init() {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public int getOBDStatus() {
        return this.mOBDStatus;
    }

    public void stop() {
        if (this.connector != null) {
            this.connector.stop();
        }
    }

    public boolean isBluetoothEnabled() {
        if (this.mBluetoothAdapter == null || !this.mBluetoothAdapter.isEnabled()) {
            this.mOBDStatus = BT_DISABLED;
            return false;
        }
        return true;
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
        this.mOBDStatus = OBD_NOT_CONFIGURED;
        return false;       
    }

    public void connectDevice(String deviceAddress) {
        this.mOBDStatus = CONNECTING;
        if (this.connector == null) {
            this.connector = new BluetoothConnector(this.owner, btMsgHandler);
        }
        this.connector.connect(mBluetoothAdapter.getRemoteDevice(deviceAddress));
    }

    // The Handler that gets information back from the BluetoothConnector
    private final Handler btMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothConnector.MESSAGE_STATE_CHANGE:
                Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothConnector.STATE_CONNECTED:
                    mOBDStatus = OBD_CONNECTED;
                    obdConnected = true;
                    service.notifyOBDStatus();
                    startObdConnection();
                    break;
                case BluetoothConnector.STATE_CONNECTING:
                    mOBDStatus = CONNECTING;
                    service.notifyOBDStatus();
                    break;
                case BluetoothConnector.STATE_FAILED:
                    mOBDStatus = OBD_NOT_CONFIGURED;
                    service.notifyOBDStatus();
                    break;
                case BluetoothConnector.STATE_LOST:
                    stopObdConnection();
                    stop();
                    mOBDStatus = OBD_NOT_CONFIGURED;
                    service.notifyOBDStatus();
                    break;
                case BluetoothConnector.STATE_LISTEN:
                case BluetoothConnector.STATE_NONE:
                    break;
                }
                break;
            case BluetoothConnector.MESSAGE_READ:
                Log.d(TAG, "Data received.");
                if (msg.arg1 > 0) { // msg.arg1 contains the number of bytes read
                    Log.d(TAG, "\tRead size: " + msg.arg1);
                    byte[] readBuf = (byte[]) msg.obj;
                    byte[] readBytes = new byte[msg.arg1];
                    System.arraycopy(readBuf, 0, readBytes, 0, msg.arg1);
                    Log.d(TAG, "\tAs Hex: " + asHex(readBytes));
                }
                break;
            case BluetoothConnector.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                obdBluetoothAddress = msg.getData().getString(DEVICE_ADRESS);
                storeState();
                Toast.makeText(owner, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case BluetoothConnector.MESSAGE_TOAST:
                Toast.makeText(owner, msg.getData().getString(BluetoothConnector.TOAST), Toast.LENGTH_LONG).show();
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


    

    public void stopObdConnection() {
        mHandler.removeCallbacks(mQueueCommands);
    }

    private void startObdConnection() {
        //sendToDevice(OBDCommand.SET_DEFAULTS.getOBDcode());
        getOBDData(OBDCommand.RESET_ALL);
        getOBDData(OBDCommand.ECHO_OFF);
        getOBDData(OBDCommand.ECHO_OFF);
        getOBDData(OBDCommand.LINE_FEED_OFF);
        getOBDData(OBDCommand.TIME_OUT);

        getOBDData(OBDCommand.AMBIENT_AIR_TEMPERATURE);
        
        mHandler.post(mQueueCommands);
    }

    private String getOBDData(OBDCommand param) {
        if (param == null) {
            return null;
        }

        byte[] data = sendToDevice(param.getOBDcode());
        if (data != null && data.length > 0) {
            IResultReader reader = param.getReader();
            if (reader != null) {
                return reader.readFormattedResult(data);
            }
        }
        return null;
    }

    private byte[] sendToDevice(String msg) {
        byte[] bytes = null;
        if (this.connector != null) {
            // add the carriage return char
            msg += "\r";
            connector.write(msg.getBytes());
            Log.d(TAG, "Sent to scanner: " + msg);
            bytes = connector.read();
            if (bytes != null && bytes.length > 0) {
                Log.d(TAG, "Scanner response: " + new String(bytes));
            }
        }
        return bytes;
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
            for (int i = 0; i < commandList.length; i++) {
                OBDCommand command = commandList[i];
                String readParam = getOBDData(command);
                if (readParam != null) {
                    //Log.d(TAG, "\t\t " + command.getName() + ": " + readParam);
                    service.notifyDataReceived(readParam);
                } else {
                    Log.e(TAG, "\t\t " + command.getName() + " got null!!!");
                }
            }
            // run again in 2s
            mHandler.postDelayed(mQueueCommands, 2000);
        }
    };

}
