/**
 * 
 */
package com.hp.myidea.obdproxy;

import java.util.Collection;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.hp.myidea.obdproxy.ICommunicator;
import com.hp.myidea.obdproxy.IMessageListener;
import com.hp.myidea.obdproxy.IRosterListener;

/**
 * @author mapo
 *
 */
public class XMPPCommunicator extends Service {

    private static final String TAG = XMPPCommunicator.class.getSimpleName();

    private static final int XMPP_NOTIFICATIONS = 1;

    private static NotificationManager notifMgr;
    private Toast toast;
    private Vibrator vibrator;

    private XMPPConnection connection;
    private boolean isConnected = false;

    private String userEmailAddress = "";
    private String userPassword = "";

    private ChatThread cThread = null;

    private IMessageListener messageListener = null;

    /* (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if(action.equals("com.hp.myidea.obdproxy.XMPPCommunicator")) {
            return messageBinder;
        }
        return null;
    }

    @Override
    public void onCreate() {
        notifMgr = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);

        this.init();
        
        String temp = this.isConnected?": connected":": disconnected";
        this.toast = Toast.makeText(this, TAG + temp, Toast.LENGTH_LONG);
        this.toast.setGravity(Gravity.CENTER, 0, 0);
        this.toast.show();
        this.vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void onDestroy() {
        if(this.cThread != null) {
            this.cThread.stopIt();
        }
        notifMgr.cancel(XMPP_NOTIFICATIONS);
    }

    private final ICommunicator.Stub messageBinder = new ICommunicator.Stub() {

        // TODO: implement method stubs

        public void start() throws RemoteException {
            Log.d(TAG, "Starting up...");
            // TODO Auto-generated method stub
            if (!XMPPCommunicator.this.isConnected) {
            	XMPPCommunicator.this.init();
            }
            if (!XMPPCommunicator.this.isConnected) {
            	// TODO what to do?
            	Log.e(TAG, "\n\n\n\n\nCould not start the XMPPCommunicator!!!\n\n\n\n\n");
            }
        }

        public void stop() throws RemoteException {
            Log.d(TAG, "Stoping.");
            // TODO Auto-generated method stub
            XMPPCommunicator.this.cThread.stopIt();
            XMPPCommunicator.this.disconnect();
            XMPPCommunicator.this.isConnected = false;
        }

		@Override
		public boolean sendMessage(String to, String msg) throws RemoteException {
			return sendMessageTo(to, msg);
		}

        @Override
        public void startRosterListener(IRosterListener listener) throws RemoteException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void stopRosterListener() throws RemoteException {
            // TODO Auto-generated method stub
        }

		@Override
		public void startMessageListener(IMessageListener listener) throws RemoteException {
			messageListener = listener;
		}

		@Override
        public void stopMessageListener() throws RemoteException {
			messageListener = null;
        }
		

    };

    private void init() {
        this.restoreState();
        if (!this.isConnected) {
            try {
                this.connect();
                if (this.isConnected) {
                    this.cThread = new ChatThread("ChatThread");
                    this.cThread.start();
                }
            } catch (XMPPException e) {
                Log.e(TAG, "Could not connect!", e);
            }
        }    	
    }

    private void restoreState() {
        // Restore state
        //SharedPreferences state = this.getSharedPreferences(ApplicationPreference.SHARED_PREFS_FILE, 0);
        //this.userEmailAddress = state.getString("emailAddress", "cardiotalk@gmail.com");
        //this.userPassword = state.getString("userPassword", "cardiotalk2011");
        this.userEmailAddress = "amarokMapo@15.185.92.3";
        this.userPassword = "mapoAmarok";
    }

    private boolean sendMessageTo(String to, String msg) {
        if (this.isConnected) {
            ChatManager chatmanager = connection.getChatManager();
            Chat chato = chatmanager.createChat(to, new MessageListener() {
                public void processMessage(Chat chat, Message message) {}
            });
            try {
                chato.sendMessage(msg);
            } catch (XMPPException e) {
                Log.e(TAG, "Fail: ", e);
                e.printStackTrace();
            }
        }
        return false;
    }

    private void connect() throws XMPPException {
        // Create the configuration for this new connection
        ConnectionConfiguration config = new ConnectionConfiguration("15.185.92.3", 5222);
        // config.setCompressionEnabled(true);
        // config.setSASLAuthenticationEnabled(true);

        this.connection = new XMPPConnection(config);
        // Connect to the server
        this.connection.connect();

        // You have to put this code before you login
        //SASLAuthentication.supportSASLMechanism("PLAIN", 0);

        // Log into the server
        connection.login(this.userEmailAddress, this.userPassword, OBDRemoteResourceFilter.RESOURCE);
        
        this.isConnected = this.connection.isAuthenticated();
    }

    private void disconnect() {
        if (this.isConnected) {
            connection.disconnect();
        }
    }

    private void startRosterListener() {
        if (!this.isConnected) {
            return;
        }
        Roster roster = this.connection.getRoster();
        roster.addRosterListener(new RosterListener() {
            @Override
            public void entriesDeleted(Collection<String> addresses) {
                
            }

            @Override
            public void entriesUpdated(Collection<String> addresses) {
                
            }

            @Override
            public void presenceChanged(Presence presence) {

            }

            @Override
            public void entriesAdded(Collection<String> addresses) {
                
            }
        });
    }

    /**
     * Show a notification for an arrived subscription request.
     */
    private void notifySubscriptionRequested() {
    }

    /**
     * Show the arrived message in the notification bar.
     * Does not make sense. Just for tests purpose.
     * TODO : REMOVE IT!!!
     */
    private void notifyMessageReceived(String msg) {
    }

    private final class ChatThread extends Thread {

        /**
         * Thread running control
         */
        private volatile boolean running = false;

        private volatile boolean canceled = false;

        /**
         * Overriding default constructor for JobThread.
         */
        public ChatThread(String name) {
            super(name);
        }

        /**
         * Start running the JobThread
         */
        public synchronized void start() {
            if(this.running) {
                this.notifyAll();
            } else {
                this.running = true;
                super.start();
            }
        }

        /**
         * Stop running the JobThread
         */
        public synchronized void stopIt() {
            this.running= false;
            this.notifyAll();
        }

        public synchronized void cancelJob() {
            this.canceled = true;
        }

        public void run() {
            // Assume we've created an XMPPConnection name "connection".
            if (!XMPPCommunicator.this.isConnected) {
                Log.e(TAG, "Fail: trying to start the ChatThread without a live XMPP connection.");
                return;
            }
            ChatManager chatmanager = connection.getChatManager();
            Chat chato = chatmanager.createChat("FerraruKaReno@15.185.92.3", new MessageListener() {
                public void processMessage(Chat chat, Message message) {}
            });
            try {
                chato.sendMessage("Say something...");
            } catch (XMPPException e) {
                Log.e(TAG, "Fail: ", e);
                e.printStackTrace();
            }

            PacketFilter filter = new AndFilter(new OrFilter(new PacketTypeFilter(Message.class), new PacketTypeFilter(Presence.class)), new OBDRemoteResourceFilter());

            PacketCollector collector = connection.createPacketCollector(filter);

            while(this.running) {
                Packet packet = collector.nextResult();
                Log.d(TAG, "New packet arrived: " + packet.getFrom());
                // TODO: we are just echoing messages for now. We need do dispatch the message for the ChatManager service
                if (packet instanceof Message) {
                    Message msg = (Message) packet;
                    notifyMessageReceived(msg.getBody());
                    Log.d(TAG, "Got message:" + msg.getBody() +
                            " from: " + msg.getFrom() +
                            " who says her resource is " + StringUtils.parseResource(msg.getFrom()));
                    try {
                        chato.sendMessage("Recebi '" + msg.getBody() + "'. (Foi isso mesmo?)");
                    } catch (XMPPException e) {
                        Log.e(TAG, "Fail: ", e);
                        e.printStackTrace();
                    }
                } else if (packet instanceof Presence) {
                    if (((Presence)packet).getType() == Presence.Type.subscribe) {
                    	// TODO: Again, we need to dispatch this message to the ChatManager service
                    	notifySubscriptionRequested();
                        Log.d(TAG, "Someone trying to subscribe: " + ((Presence)packet).getFrom());
                    }
                }
            }
        }
    }
}
