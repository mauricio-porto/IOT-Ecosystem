/**
 * 
 */
package com.hp.myidea.obdproxy.base;

import com.hp.myidea.obdproxy.IResultReader;

/**
 * @author mapo
 *
 */
public class FuelReader extends OBDResponseReader implements IResultReader {

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

/*        if (!"NODATA".equals(getResult())) {
            // ignore first two bytes [hh hh] of the response
            fuelLevel = 100.0f * buffer.get(2) / 255.0f;
        }

        return String.format("%.1f%s", fuelLevel, "%");
*/
        return res;
    }

}
