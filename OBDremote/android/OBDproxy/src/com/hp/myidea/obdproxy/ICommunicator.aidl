package com.hp.myidea.obdproxy;

import com.hp.myidea.obdproxy.IMessageListener;
import com.hp.myidea.obdproxy.IRosterListener;

interface ICommunicator {
	void start();
	void stop();
	boolean sendMessage(in String to, in String msg);
	void startMessageListener(in IMessageListener listener);
    void stopMessageListener();
	void startRosterListener(in IRosterListener listener);
	void stopRosterListener();
}