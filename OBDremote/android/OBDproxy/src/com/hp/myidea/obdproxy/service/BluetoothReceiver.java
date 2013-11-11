/**
 * 
 */
package com.hp.myidea.obdproxy.service;

import com.hp.myidea.obdproxy.base.BluetoothConnector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

/**
 * @author mapo
 *
 */
public class BluetoothReceiver extends Service {

    private static final String TAG = BluetoothReceiver.class.getSimpleName();

    private static final int OBD_NOTIFICATIONS = 1;

    public static final String ACTION_START = "startService";
    public static final String ACTION_STOP = "stopService";

    // Message types sent from the BluetoothConnector Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Message types received from the activity messenger
    // MUST start by zero due the enum mapping
    public static final int CONNECT_TO = 0;
    public static final int GET_OBD_STATUS = 1;
    public static final int REGISTER_LISTENER = 2;
    public static final int UNREGISTER_LISTENER = 3;
    public static final int REGISTER_HANDLER = 4;
    public static final int UNREGISTER_HANDLER = 5;
    public static final int SEND_MESSAGE = 6;

    public static enum ACTION {
    	CONNECT_TO,
    	GET_OBD_STATUS,
    	REGISTER_LISTENER,
    	UNREGISTER_LISTENER,
    	REGISTER_HANDLER,
    	UNREGISTER_HANDLER,
    	SEND_MESSAGE
    }

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

    // Key names received
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADRESS = "device_address";
    public static final String TOAST = "toast";
    public static final String TEXT_MSG = "text";
    public static final String BOOL_MSG = "bool";

    // Key names sent
    public static final String KEY_OBD_DATA = "OBD_data";
    public static final String KEY_LOCATION_DATA = "location_data";

    public static final int DIST_FREQ_RATIO = 75000;

    private static NotificationManager notifMgr;

    private Toast toast;
    private Vibrator vibrator;
    private boolean mustVibrate = false;

	private float defaultDuration = (float) 0.3;
	private int lastDist;

    private boolean running = false;

    private int mOBDStatus = NONE;

    private Messenger activityHandler = null;

    // MAC address of the OBD device
    private String obdBluetoothAddress = null;
    // Name of the connected device
    private String mConnectedDeviceName = null;
    private boolean obdConnected = false;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothConnector connector;
    private Notification notifier;

    private boolean communicatorSvcConnected = false;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        String action = intent.getAction();
        if(action.equals("com.hp.myidea.guidedroid.service.BluetoothReceiver") || action.equals("com.hp.myidea.guidedroid.BLUETOOTH_RECEIVER")) {
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
        this.vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        this.toast = Toast.makeText(this, TAG, Toast.LENGTH_LONG);
        this.toast.setGravity(Gravity.CENTER, 0, 0);

        this.notifier = new Notification(R.drawable.ic_launcher, "GuideDroid is running...", System.currentTimeMillis());

        this.notifier.setLatestEventInfo(this, "GuideDroid", "Your guide friend", this.buildIntent());	// TODO: Localize!!!!
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
        this.running = false;
	    super.onDestroy();
    }

    private void init() {
    	Log.d(TAG, "init()\n\n\n\n");

    	// Connect to the OBD device
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

        this.notifyUser("GuideDroid is running.", "GuideDroid is running...");
        this.running = true;
    }

	private void stopAll() {
    	Log.d(TAG, "\n\n\n\nstopAll()\n\n\n\n");
        obdConnected = false;
        if (this.connector != null) {
        	this.connector.stop();
        }
       
        this.notifyUser("Stopped. Select to start again.", "Stopping GuideDroid.");
		this.running = false;
    }

    private boolean connectKnownDevice() {
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

    private void connectDevice(String deviceAddress) {
    	this.mOBDStatus = CONNECTING;
        if (this.connector == null) {
            this.connector = new BluetoothConnector(this, mHandler);
        }
        this.connector.connect(mBluetoothAdapter.getRemoteDevice(deviceAddress));
    }

    private void sendToDevice(String msg) {
        if (this.connector != null) {
            connector.write(msg.getBytes());
        }
    }

    private void restoreState() {
        // Restore state
        SharedPreferences state = this.getSharedPreferences(GuideDroid.GUIDE_DROID_PREFS, 0);
        this.obdBluetoothAddress = state.getString("ArduinoBluetoothAddress", null);
    }

    private void storeState() {
        // Persist state
        SharedPreferences state = this.getSharedPreferences(GuideDroid.GUIDE_DROID_PREFS, 0);
        SharedPreferences.Editor editor = state.edit();
        editor.putString("ArduinoBluetoothAddress", this.obdBluetoothAddress);
        editor.commit();
    }

    private PendingIntent buildIntent() {
        Intent intent = new Intent(this, GuideDroid.class);

        //intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    /**
     * Show a notification
     */
    private void notifyUser(String action, String alert) {
        CharSequence serviceName = "GuideDroid";  //super.getText(R.string.service_name);
        CharSequence actionText = action;
        CharSequence notificationText = alert;
        this.notifier = new Notification(R.drawable.ic_launcher, notificationText, System.currentTimeMillis());
        this.notifier.setLatestEventInfo(this, serviceName, actionText, this.buildIntent());	// TODO: Localize!!!!
        notifMgr.notify(OBD_NOTIFICATIONS, this.notifier);
        this.thumpthump();
    }

    /**
     * Show a toast with the given text.
     *
     * @param message string to show (if null, nothing will be shown)
     */
    private void showToast(String toastText) {
        if (toastText != null) {

            this.thumpthump();

            Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(TOAST, toastText);
            msg.setData(bundle);
            mHandler.sendMessage(msg);

        }
    }

    private void thumpthump() {
    	if (this.mustVibrate) {
    		this.vibrator.vibrate(new long[]{50, 200, 50, 50, 500, 200, 50, 50, 500, 200, 50, 50, 500}, -1);
    	}
    }

    // The Handler that gets information back from the BluetoothConnector
    private final Handler mHandler = new Handler() {
    	int counter = 0;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothConnector.STATE_CONNECTED:
                    BluetoothReceiver.this.mOBDStatus = OBD_CONNECTED;
                    obdConnected = true;
                    notifyBTState();
                    break;
                case BluetoothConnector.STATE_CONNECTING:
                    BluetoothReceiver.this.mOBDStatus = CONNECTING;
                    notifyBTState();
                    break;
                case BluetoothConnector.STATE_FAILED:
                	BluetoothReceiver.this.mOBDStatus = OBD_NOT_CONFIGURED;
                    notifyBTState();
                	break;
                case BluetoothConnector.STATE_LISTEN:
                case BluetoothConnector.STATE_NONE:
                    break;
                }
                break;
            case MESSAGE_READ:
                Log.d(TAG, "Data received.");
                if (msg.arg1 > 0) {	// msg.arg1 contains the number of bytes read
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
                showToast("Connected to " + mConnectedDeviceName);
                break;
            case MESSAGE_TOAST:
            	BluetoothReceiver.this.toast.setText(msg.getData().getString(TOAST));
            	BluetoothReceiver.this.toast.show();
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

    private void notifyNotRunning() {
        if (activityHandler != null) {
        	try {
				activityHandler.send(Message.obtain(null, NOT_RUNNING, null));
			} catch (RemoteException e) {
				// Nothing to do
			}
        }
    }

    private void notifyBTState() {
        if (activityHandler != null) {
        	if (this.mOBDStatus > NONE) {
        		Log.d(TAG, "notifyBTState() - " + BT_STATUS.values()[this.mOBDStatus]);
        	}
        	try {
				activityHandler.send(Message.obtain(null, this.mOBDStatus, null));
			} catch (RemoteException e) {
				// Nothing to do
			}
        } else {
        	Log.d(TAG, "notifyBTState() - NO Activity handler to receive!");
        }
    }

    /**
     * Handler of incoming messages from clients, i.e., GuideDroid activity.
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
            	activityHandler = null;
            	break;
            case REGISTER_HANDLER:
            	activityHandler = msg.replyTo;
            	// TODO: MUST notify if we are running already
/*            	if (!running) {
            		notifyNotRunning();
            		break;
            	}
*/            	notifyBTState();
            	break;
            case UNREGISTER_HANDLER:
            	activityHandler = null;
            	break;
            case SEND_MESSAGE:
            	String message = msg.getData().getString(TEXT_MSG);
            	if (message != null && message.length() > 0) {
            		sendToDevice(message);
            	}
            	break;
            default:
            	break;
            }
        }
    };
    final Messenger activityMsgListener = new Messenger(activityMessages);

}
