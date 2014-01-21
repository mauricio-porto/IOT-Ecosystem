/**
 * 
 */
package com.hp.myidea.obdproxy.base;

import com.hp.myidea.obdproxy.IResultReader;

/**
 * @author mauricio
 *
 */
public class TemperatureReader extends OBDResponseReader implements IResultReader {

    private float temperature = 0.0f;

    @Override
    public String readResult(byte[] input) {
        String res = new String(input);
        return res;
    }

    @Override
    public String readFormattedResult(byte[] input) {
        String res = new String(input);

        if (!"NODATA".equals(res)) {
            temperature = getValue(input) - 40;  // It ranges from -40 to 215 °C
            
            res = String.format("%.0f", temperature);
        }

        return res;
    }

}
