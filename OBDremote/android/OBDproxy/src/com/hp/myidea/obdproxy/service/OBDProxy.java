/**
 * 
 */
package com.hp.myidea.obdproxy.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.hp.myidea.obdproxy.IProxyService;
import com.hp.myidea.obdproxy.R;
import com.hp.myidea.obdproxy.app.OBDproxyActivity;
import com.hp.myidea.obdproxy.base.OBDConnector;

/**
 * @author mapo
 *
 */
public class OBDProxy extends Service implements IProxyService {

    private static final String TAG = OBDProxy.class.getSimpleName();

    private static final int OBD_NOTIFICATIONS = 1;

    public static final String ACTION_START = "startService";
    public static final String ACTION_STOP = "stopService";

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

    private OBDConnector obdConnector;

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
        if ((BluetoothAdapter.getDefaultAdapter()) == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();   // TODO: localize!!!
            return;
        }

        this.obdConnector = new OBDConnector(this);

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
        if (!this.obdConnector.isBluetoothEnabled()) {
            this.notifyUser("Select to enable bluetooth.", "Must enable bluetooth.");
            return;
        }
        if (!this.obdConnector.connectKnownDevice()) {
            this.notifyUser("Select to configure OBD device.", "OBD device not configured.");
            return;
        }
        this.notifyUser("OBD Proxy is running. Select to see data and configure.", "OBD Proxy is running...");
    }

	private void stopAll() {
    	Log.d(TAG, "\n\n\n\nstopAll()\n\n\n\n");
        if (this.obdConnector != null) {
            this.obdConnector.stop();
        }

        this.notifyUser("Stopped. Select to start again.", "Stopping OBDproxy.");
		this.stopSelf();
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

    public void notifyOBDStatus() {
        if (activityHandler != null) {
            int status = this.obdConnector.getOBDStatus();
        	if (status > OBDConnector.NONE) {
        		Log.d(TAG, "notifyOBDStatus() - " + OBDConnector.BT_STATUS.values()[status]);
        	}
        	try {
				activityHandler.send(Message.obtain(null, status, null));
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
            	Log.d(TAG, "Received address: " + rcvdAddress);
            	if (rcvdAddress == null || rcvdAddress.length() == 0 ) {
            		obdConnector.connectKnownDevice();
            	} else {
            		obdConnector.connectDevice(rcvdAddress);
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
    public Context getServiceContext() {
        return this;
    }

}
