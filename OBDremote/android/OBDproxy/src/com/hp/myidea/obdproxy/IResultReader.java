/**
 * 
 */
package com.hp.myidea.obdproxy;

/**
 * @author mauricio
 *
 */
public interface IResultReader {
    public String readResult(byte[] input);
    public String readFormattedResult(byte[] input);
}
