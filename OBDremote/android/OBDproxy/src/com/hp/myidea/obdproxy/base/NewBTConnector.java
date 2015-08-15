package com.hp.myidea.obdproxy.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * @author mauricio
 * 
 * @see http://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3/18786701details
 *
 */
public class NewBTConnector {

	private BluetoohSocketWrapper bluetoothSocket;
	private BluetoothDevice device;
	private boolean secure;
	private BluetoothAdapter adapter;
	private List<UUID> uuidCandidates;
	private int candidate;

	/**
	 * @param device
	 *            the device
	 * @param secure
	 *            if connection should be done via a secure socket
	 * @param adapter
	 *            the Android BT adapter
	 * @param uuidCandidates
	 *            a list of UUIDs. if null or empty, the Serial PP id is used
	 */
	public NewBTConnector(BluetoothDevice device, boolean secure, BluetoothAdapter adapter, List<UUID> uuidCandidates) {
		this.device = device;
		this.secure = secure;
		this.adapter = adapter;
		this.uuidCandidates = uuidCandidates;

		if (this.uuidCandidates == null || this.uuidCandidates.isEmpty()) {
			this.uuidCandidates = new ArrayList<UUID>();
			this.uuidCandidates.add(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
		}
	}

	public BluetoohSocketWrapper connect() throws IOException {
		boolean success = false;
		while (selectSocket()) {
			adapter.cancelDiscovery();

			try {
				bluetoothSocket.connect();
				success = true;
				break;
			} catch (IOException e) {
				// try the fallback
				try {
					bluetoothSocket = new FallbackBluetoothSocket(bluetoothSocket.getUnderlyingSocket());
					Thread.sleep(500);
					bluetoothSocket.connect();
					success = true;
					break;
				} catch (FallbackException e1) {
					Log.w("BT", "Could not initialize FallbackBluetoothSocket classes.", e);
				} catch (InterruptedException e1) {
					Log.w("BT", e1.getMessage(), e1);
				} catch (IOException e1) {
					Log.w("BT", "Fallback failed. Cancelling.", e1);
				}
			}
		}
		if (!success) {
			throw new IOException("Could not connect to device: " + device.getAddress());
		}

		return bluetoothSocket;
	}

	private boolean selectSocket() throws IOException {
		if (candidate >= uuidCandidates.size()) {
			return false;
		}

		BluetoothSocket tmp;
		UUID uuid = uuidCandidates.get(candidate++);

		Log.i("BT", "Attempting to connect to Protocol: " + uuid);
		if (secure) {
			tmp = device.createRfcommSocketToServiceRecord(uuid);
			bluetoothSocket = new NativeBluetoothSocket(tmp);
		} else {
			//tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
		}

		return true;
	}

}
