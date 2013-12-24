/**
 * 
 */
package com.hp.myidea.obdproxy.base;

/**
 * @author mauricio
 *
 */
public abstract class OBDResponseReader {

    float value = 0.0f;
   
    protected float getValue(byte[] rawData) {
        // ignore first four bytes [hh hh] of the response - one byte each character
        byte[] data = new byte[rawData.length - 4];
        System.arraycopy(rawData, 4, data, 0, data.length);
        try {
            value = (float) Integer.parseInt(new String(data), 16);
        } catch (NumberFormatException e) {
            // Bad luck
        }
        return value;
    }
}
