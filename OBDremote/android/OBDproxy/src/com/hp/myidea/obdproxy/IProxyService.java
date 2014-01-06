/**
 * 
 */
package com.hp.myidea.obdproxy;

import android.content.Context;

/**
 * @author mauricio
 *
 */
public interface IProxyService {

    public Context getServiceContext();
    public void notifyOBDStatus();
    public void notifyDataReceived(String data);
    public void showToastMsg(String msg);
}
