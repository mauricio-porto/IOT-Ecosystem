/**
 * 
 */
package com.hp.myidea.obdproxy.service;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.hp.myidea.obdproxy.R;
import com.hp.myidea.obdproxy.app.OBDproxyActivity;
import com.hp.myidea.obdproxy.base.BluetoothConnector;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.reader.IPostListener;
import eu.lighthouselabs.obd.reader.io.ObdCommandJob;

/**
 * @author mapo
 *
 */
public class OBDProxy extends Service implements IPostListener {

    private static final String TAG = OBDProxy.class.getSimpleName();

    private static final int OBD_NOTIFICATIONS = 1;

    public static final String ACTION_START = "startService";
    public static final String ACTION_STOP = "stopService";

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
    
    // Message types received from the activity messenger
    // MUST start by zero due the enum mapping
    public static final int CONNECT_TO = 0;
    public static final int GET_OBD_STATUS = 1;
    public static final int REGISTER_LISTENER = 2;
    public static final int UNREGISTER_LISTENER = 3;
    public static final int REGISTER_HANDLER = 4;
    public static final int UNREGISTER_HANDLER = 5;
    public static final int STOP_SERVICE = 6;

    public static enum ACTION {
    	CONNECT_TO,
    	GET_OBD_STATUS,
    	REGISTER_LISTENER,
    	UNREGISTER_LISTENER,
    	REGISTER_HANDLER,
    	UNREGISTER_HANDLER,
    	STOP_SERVICE
    }

    public static final String TOAST = "toast";
    public static final String TEXT_MSG = "text";
    public static final String BOOL_MSG = "bool";

    // Key names sent
    public static final String KEY_OBD_DATA = "OBD_data";
    public static final String KEY_LOCATION_DATA = "location_data";

    public static final int DIST_FREQ_RATIO = 75000;

    private static NotificationManager notifMgr;

    private Toast toast;

    private Messenger activityHandler = null;

    private Notification notifier;

    private boolean communicatorSvcConnected = false;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        String action = intent.getAction();
        if(action.equals("com.hp.myidea.obdproxy.service.OBDProxy")) {
            return activityMsgListener.getBinder();
        }
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        if ((mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();   // TODO: localize!!!
            return;
        }

        notifMgr = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);

        this.toast = Toast.makeText(this, TAG, Toast.LENGTH_LONG);
        this.toast.setGravity(Gravity.CENTER, 0, 0);

        this.notifier = new Notification(R.drawable.ic_launcher, "OBDproxy is running...", System.currentTimeMillis());

        this.notifier.setLatestEventInfo(this, "OBDproxy", "Your roaming service car", this.buildIntent());	// TODO: Localize!!!!
        this.notifier.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void handleCommand(Intent intent) {
        if (ACTION_START.equals(intent.getAction())) {
            Log.d(TAG, "\n\nhandleCommand() - START ACTION");
            this.init();
        } else if (ACTION_STOP.equals(intent.getAction())) {
            Log.d(TAG, "\n\nhandleCommand() - STOP ACTION");
            this.stopAll(); 
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
	    super.onDestroy();
    }

    private void init() {
    	Log.d(TAG, "init()\n\n\n\n");
        // Connect to the ARDUINO device
        if (!mBluetoothAdapter.isEnabled()) {
            this.mOBDStatus = BT_DISABLED;
            this.notifyUser("Select to enable bluetooth.", "Must enable bluetooth.");
            return;
        }
        if (!this.connectKnownDevice()) {
            this.mOBDStatus = OBD_NOT_CONFIGURED;
            this.notifyUser("Select to configure OBD device.", "OBD device not configured.");
            return;
        }
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
            this.connector = new BluetoothConnector(this, btMsgHandler);
        }
        this.connector.connect(mBluetoothAdapter.getRemoteDevice(deviceAddress));
    }

	private void stopAll() {
    	Log.d(TAG, "\n\n\n\nstopAll()\n\n\n\n");
        obdConnected = false;
        if (this.connector != null) {
            this.connector.stop();
        }

        this.notifyUser("Stopped. Select to start again.", "Stopping OBDproxy.");
		this.stopSelf();
    }

    private void restoreState() {
        // Restore state
        SharedPreferences state = this.getSharedPreferences(OBDproxyActivity.OBDPROXY_PREFS, 0);
        this.obdBluetoothAddress = state.getString("OBDBluetoothAddress", null);
    }

    private void storeState() {
        // Persist state
        SharedPreferences state = this.getSharedPreferences(OBDproxyActivity.OBDPROXY_PREFS, 0);
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
                    mOBDStatus = OBD_CONNECTED;
                    obdConnected = true;
                    notifyOBDStatus();
                    break;
                case BluetoothConnector.STATE_CONNECTING:
                    mOBDStatus = CONNECTING;
                    notifyOBDStatus();
                    break;
                case BluetoothConnector.STATE_FAILED:
                    mOBDStatus = OBD_NOT_CONFIGURED;
                    notifyOBDStatus();
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
                Toast.makeText(OBDProxy.this, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
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

    private PendingIntent buildIntent() {
        Intent intent = new Intent(this, OBDproxyActivity.class);

        //intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    /**
     * Show a notification
     */
    public void notifyUser(String action, String alert) {
        CharSequence serviceName = "OBDproxy";  //super.getText(R.string.service_name);
        CharSequence actionText = action;
        CharSequence notificationText = alert;
        this.notifier = new Notification(R.drawable.ic_launcher, notificationText, System.currentTimeMillis());
        this.notifier.setLatestEventInfo(this, serviceName, actionText, this.buildIntent());	// TODO: Localize!!!!
        notifMgr.notify(OBD_NOTIFICATIONS, this.notifier);
    }

    private void notifyOBDStatus() {
        if (activityHandler != null) {
        	if (this.mOBDStatus > NONE) {
        		Log.d(TAG, "notifyOBDStatus() - " + BT_STATUS.values()[this.mOBDStatus]);
        	}
        	try {
				activityHandler.send(Message.obtain(null, this.mOBDStatus, null));
			} catch (RemoteException e) {
				// Nothing to do
			}
        } else {
        	Log.d(TAG, "notifyOBDStatus() - NO Activity handler to receive!");
        }
    }

    /**
     * Handler of incoming messages from clients, i.e., OBDproxy activity.
     */
    final Handler activityMessages = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "Received message: " + ACTION.values()[msg.what]);
            switch (msg.what) {
            case GET_OBD_STATUS:
            	break;
            case CONNECT_TO:
            	String rcvdAddress = msg.getData().getString(TEXT_MSG);
            	Log.i(TAG, "Received address: " + rcvdAddress);
            	if (rcvdAddress == null || rcvdAddress.length() == 0 ) {
            		connectKnownDevice();
            	} else {
            		connectDevice(rcvdAddress);
            	}
            	break;
            case REGISTER_LISTENER:
            	break;
            case UNREGISTER_LISTENER:
            	break;
            case REGISTER_HANDLER:
            	activityHandler = msg.replyTo;
            	notifyOBDStatus();
            	break;
            case UNREGISTER_HANDLER:
            	activityHandler = null;
            	break;
            case STOP_SERVICE:
                stopAll();
                break;
            default:
            	break;
            }
        }
    };
    final Messenger activityMsgListener = new Messenger(activityMessages);

    @Override
    public void stateUpdate(ObdCommandJob job) {
        // TODO Auto-generated method stub
    }

}
