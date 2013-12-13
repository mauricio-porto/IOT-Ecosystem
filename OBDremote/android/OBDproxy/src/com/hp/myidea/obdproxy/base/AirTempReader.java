/**
 * 
 */
package com.hp.myidea.obdproxy.base;

import com.hp.myidea.obdproxy.IResultReader;

/**
 * @author mauricio
 *
 */
public class AirTempReader implements IResultReader {

    @Override
    public String readResult(byte[] input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String readFormattedResult(byte[] input) {
        String res = new String(input);

        if (!"NODATA".equals(res)) {
            // ignore first two bytes [hh hh] of the response
            temperature = prepareTempValue(buffer.get(2));
            
            // convert?
            if (useImperialUnits)
                res = String.format("%.1f%s", getImperialUnit(), "F");
            else
                res = String.format("%.0f%s", temperature, "C");
        }

        return res;
    }

}
