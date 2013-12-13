/**
 * 
 */
package com.hp.myidea.obdproxy.base;

import com.hp.myidea.obdproxy.IResultReader;

/**
 * @author mapo
 *
 */
public class SpeedReader implements IResultReader {

    /* (non-Javadoc)
     * @see com.hp.myidea.obdproxy.IResultReader#readResult(byte[])
     */
    @Override
    public String readResult(byte[] input) {
        String res = new String(input);
        return res;
    }

    /* (non-Javadoc)
     * @see com.hp.myidea.obdproxy.IResultReader#readFormattedResult(byte[])
     */
    @Override
    public String readFormattedResult(byte[] input) {
        String res = new String(input);
        
/*        if (!"NODATA".equals(res)) {
            //Ignore first two bytes [hh hh] of the response.
            metricSpeed = buffer.get(2);
            res = String.format("%d%s", metricSpeed, "km/h");

            if (useImperialUnits)
                res = String.format("%.2f%s", getImperialUnit(),
                        "mph");
        }
*/
        return res;
    }

}
