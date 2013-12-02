/**
 * 
 */
package com.hp.myidea.obdproxy;

import android.content.Context;

/**
 * @author mauricio
 *
 */
public interface IServiceProxy {
    public Context getServiceContext();
    public void notifyUser(String action, String alert);
    public void notifyBTState(int status);
}
