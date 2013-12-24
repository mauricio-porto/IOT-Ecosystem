/**
 * 
 */
package com.hp.myidea.obdproxy.base;

import com.hp.myidea.obdproxy.IResultReader;

/**
 * @author mapo
 *
 */
public class MAFReader extends OBDResponseReader implements IResultReader {

    private float maf = 0.0f;

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

        if (!"NODATA".equals(res)) {
            maf = (int) getValue(input) / 100;
            res = String.format("%.0f%s", maf, " grams/sec");
        }

        return res;
    }

}
