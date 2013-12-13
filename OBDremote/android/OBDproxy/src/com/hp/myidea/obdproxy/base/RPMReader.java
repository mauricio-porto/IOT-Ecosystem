/**
 * 
 */
package com.hp.myidea.obdproxy.base;

import com.hp.myidea.obdproxy.IResultReader;

/**
 * @author mapo
 *
 */
public class RPMReader implements IResultReader {

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
            // ignore first two bytes [41 0C] of the response
            int a = buffer.get(2);
            int b = buffer.get(3);
            _rpm = (a * 256 + b) / 4;
        }
        return String.format("%d%s", _rpm, " RPM");
*/
        return res;
    }

}
