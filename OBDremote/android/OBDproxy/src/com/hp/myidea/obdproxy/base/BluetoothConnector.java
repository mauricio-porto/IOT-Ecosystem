/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.myidea.obdproxy.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothConnector {
    // Debugging
    private static final String TAG = BluetoothConnector.class.getSimpleName();
    private static final boolean D = true;

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); 

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;

    private ConnectThread mConnectThread;
    private ListenerThread mListenerThread;
    private int mState;

    // Message types sent
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADRESS = "device_address";
    public static final String TOAST = "toast";

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_FAILED = 4;     // failed to connect
    public static final int STATE_LOST = 5;       // an existing connection was lost

    /**
     * Constructor. Prepares a new BluetoothReceiverActivity session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothConnector(Context context, Handler handler) {
        mHandler = handler;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(BluetoothConnector.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mListenerThread != null) {mListenerThread.cancel(); mListenerThread = null;}

        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mListenerThread != null) {mListenerThread.cancel(); mListenerThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ListenerThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mListenerThread != null) {mListenerThread.cancel(); mListenerThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mListenerThread = new ListenerThread(socket);
        mListenerThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(BluetoothConnector.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothConnector.DEVICE_NAME, device.getName());
        bundle.putString(BluetoothConnector.DEVICE_ADRESS, device.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mListenerThread != null) {mListenerThread.cancel(); mListenerThread = null;}
        setState(STATE_NONE);
    }

    /**
     * Write to the ListenerThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ListenerThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ListenerThread r;
        // Synchronize a copy of the ListenerThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mListenerThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Read from the ListenerThread in an unsynchronized manner
     * 
     * @see ListenerThread#read()
     */
    public byte[] read() {
        // Create temporary object
        ListenerThread r;
        // Synchronize a copy of the ListenerThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return null;
            r = mListenerThread;
        }
        // Perform the read unsynchronized
        return r.read();
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void sayConnectionFailed(BluetoothDevice device) {
        setState(STATE_FAILED);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothConnector.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothConnector.TOAST, "Unable to connect device: " + device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothConnector.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothConnector.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            int retries = 5;
            boolean succeed = false;
            while (!succeed && retries-- > 0) {
	            try {
	                // This is a blocking call and will only return on a
	                // successful connection or an exception
	                mmSocket.connect();
	                succeed = true;
	            } catch (IOException e) {
	            	Log.e(TAG, "Connection failed: ", e);
	            	Log.d(TAG, "Will retry " + retries + " times.");
	            	try {
						ConnectThread.sleep(1000);
					} catch (InterruptedException e1) {
						// Nothing to do
					}
	            }
            }
            if (!succeed) {
                sayConnectionFailed(mmDevice);
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                BluetoothConnector.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothConnector.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ListenerThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ListenerThread(BluetoothSocket socket) {
            Log.d(TAG, "create ListenerThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mListenerThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {

/*                try {
                    // Read from the InputStream
                	buffer = new byte[256];
                    bytes = mmInStream.read(buffer);
                    if (bytes > 0) {
                    	byte[] readBuffer = new byte[bytes];
                    	System.arraycopy(buffer, 0, readBuffer, 0, bytes);
                        // Send the obtained bytes to the UI Activity
                        mHandler.obtainMessage(BluetoothConnector.MESSAGE_READ, readBuffer.length, -1, readBuffer).sendToTarget();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
*/
            }
        }

        /**
         * Read from the connected InputStream
         * 
         * @return - byte array read
         */
        public byte[] read() {
            // Read from the InputStream
            byte[] buffer = new byte[1024];
            int bytes;
            byte[] readBuffer = null;
            try {
                bytes = mmInStream.read(buffer);
                if (bytes > 0) {
                    readBuffer = new byte[bytes];
                    System.arraycopy(buffer, 0, readBuffer, 0, bytes);
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception during read", e);
                connectionLost();
            }
            return readBuffer;
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         * 
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(BluetoothConnector.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
