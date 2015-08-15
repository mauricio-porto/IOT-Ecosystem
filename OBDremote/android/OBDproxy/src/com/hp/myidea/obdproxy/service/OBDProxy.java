/**
 * 
 */
package com.hp.myidea.obdproxy.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.hp.myidea.obdproxy.ICommunicator;
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

    public static final String CAR_SERVICE_JID = "FerraroKaReno@ubuntu-jabber.net";    // Deverá ser configurável

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

    public static final String TEXT_MSG = "text";
    public static final String BOOL_MSG = "bool";

    // Key names sent
    public static final int OBD_DATA = 0xda1a;
    public static final int LOCATION_DATA = 0x1ada;

    public static final int DIST_FREQ_RATIO = 75000;

    private static NotificationManager notifMgr;

    private Messenger activityHandler = null;

    private Notification notifier;

    private OBDConnector obdConnector;

    private ICommunicator communicatorService = null;
    private boolean communicatorSvcConnected = false;

    private boolean initialized = false;

    private LocationManager locationManager;
    private String provider;
    private Criteria criteria;

    private Location lastLocation;
    private static final long MAX_GPS_WAIT = 10 * 1000; // TEN SECONDS
    private final Timer timer = new Timer();
    private static final long UPDATE_LOCATION_INTERVAL = 2 * 1000;

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
        this.notifier = new Notification(R.drawable.ic_launcher, "OBDproxy is running...", System.currentTimeMillis());
        this.notifier.setLatestEventInfo(this, "OBDproxy", "Your roaming service car", this.buildIntent());	// TODO: Localize!!!!
        this.notifier.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void handleCommand(Intent intent) {
        if (intent != null) {
            if (ACTION_START.equals(intent.getAction())) {
                Log.d(TAG, "\n\nhandleCommand() - START ACTION");
                //this.init();
            } else if (ACTION_STOP.equals(intent.getAction())) {
                Log.d(TAG, "\n\nhandleCommand() - STOP ACTION");
                this.stopAll(); 
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
	    super.onDestroy();
    }

    private void init() {
        Log.d(TAG, "\n\n\n\ninit()");
        if (this.initialized) {
            Log.d(TAG, "\t\t\tAlready initialized...");
            return;
        } else {
            Log.d(TAG, "\t\t\tWill initialize...");
        }
        if (!this.communicatorSvcConnected) {
            this.bindToXMPPService();
        }

        // Connect to the BT device
        if (!this.obdConnector.isBluetoothEnabled()) {
            this.notifyUser("Select to enable bluetooth.", "Must enable bluetooth.");
            return;
        }
        if (!this.obdConnector.connectKnownDevice()) {
            this.notifyUser("Select to configure OBD device.", "OBD device not configured.");
            return;
        }

        this.locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            // TODO: AJUSTA PROVIDER PARA GPS
            criteria = new Criteria(); 
            criteria.setAccuracy(Criteria.ACCURACY_FINE); 
            criteria.setAltitudeRequired(false); 
            criteria.setBearingRequired(false); 
            criteria.setCostAllowed(true); 
            criteria.setPowerRequirement(Criteria.POWER_HIGH);

            // provider = LocationManager.GPS_PROVIDER; 
            provider = locationManager.getBestProvider(criteria, true);
        } else {
            provider = LocationManager.NETWORK_PROVIDER;
        }

        this.locationManager.requestLocationUpdates(provider, UPDATE_LOCATION_INTERVAL, 0, locationListener);

        this.initialized = true;
        this.notifyUser("OBD Proxy is running. Select to see data and configure.", "OBD Proxy is running...");
    }

	private void stopAll() {
    	Log.d(TAG, "\n\n\n\nstopAll()\n\n\n\n");
        if (this.obdConnector != null) {
            this.obdConnector.stop();
        }
        if (this.communicatorSvcConnected) {
            try {
                this.communicatorService.sendMessage(OBDProxy.CAR_SERVICE_JID, "Bye, see ya.");
                this.communicatorService.stop();
            } catch (RemoteException e) {
                Log.e(TAG, "Call to Subscriptions Service failed.", e);
            }
            try {
                this.unbindService(communicatorServiceConnection);
            } catch (Throwable e) {
                // Nothing to do
            }
        }

        this.locationManager.removeUpdates(locationListener);

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
                init();
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

    @Override
    public void notifyDataReceived(String data) {
        if (activityHandler != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss", Locale.US);
            String nowStr = sdf.format(Calendar.getInstance().getTimeInMillis());
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            sb.append('"').append("timestamp").append('"').append(':');
            sb.append('"').append(nowStr).append('"').append(',');
            if (lastLocation != null) {
                sb.append('"').append("location").append('"').append(':').append('[');
                sb.append(Location.convert(lastLocation.getLatitude(), Location.FORMAT_DEGREES));
                sb.append(',');
                sb.append(Location.convert(lastLocation.getLongitude(), Location.FORMAT_DEGREES));
                sb.append(']').append(',');
            }
            sb.append(data);
            sb.append('}');
            data = sb.toString();
            
            Message msg = Message.obtain(null, OBDProxy.OBD_DATA);
            Bundle bundle = new Bundle();
            bundle.putString(OBDProxy.TEXT_MSG, data);
            msg.setData(bundle);
            try {
                activityHandler.send(msg);
                if (this.communicatorService != null) {
                    this.communicatorService.sendMessage(OBDProxy.CAR_SERVICE_JID, data);
                }
            } catch (RemoteException e) {
                // Nothing to do
            }
        } else {
            Log.d(TAG, "notifyDataReceived() - NO Activity handler to receive!");
        }
    }

    private boolean bindToXMPPService() {
        return bindService(new Intent("com.hp.myidea.obdproxy.XMPPCommunicator"), communicatorServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection communicatorServiceConnection = new ServiceConnection() {
        
        public void onServiceDisconnected(ComponentName name) {
            communicatorSvcConnected = false;
            Log.d(TAG, "XMPPCommunicator Service disconnected.");
        }
        
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "XMPPCommunicator Service connected.");
            communicatorService = ICommunicator.Stub.asInterface(service);
            communicatorSvcConnected = true;
            try {
                OBDProxy.this.communicatorService.start();
                OBDProxy.this.communicatorService.sendMessage(OBDProxy.CAR_SERVICE_JID, "Hello, say something...");
            } catch (RemoteException e) {
                Log.e(TAG, "Call to XMPPCommunicator Service failed.", e);
            }
        }
    };

    private final LocationListener locationListener = new LocationListener() {

        @Override 
        public void onLocationChanged(Location location) { 
            Log.d(TAG, "LocationListener::onLocationChanged()");
            lastLocation = location;
        }
    
        @Override 
        public void onProviderDisabled(String provider) { 
            Log.d(TAG, "LocationListener::onProviderDisabled()");
        }
    
        @Override 
        public void onProviderEnabled(String provider) { 
            Log.d(TAG, "LocationListener::onProviderEnabled()");
        }
    
        @Override 
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "LocationListener::onStatusChanged()");
        }
    }; 

}
