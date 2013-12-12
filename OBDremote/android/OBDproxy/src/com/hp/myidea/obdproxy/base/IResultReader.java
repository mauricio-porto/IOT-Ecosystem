/**
 * 
 */
package com.hp.myidea.obdproxy.base;

/**
 * @author mauricio
 *
 */
public interface IResultReader {
    public String readResult(byte[] input);
    public String readFormattedResult(byte[] input);
}
