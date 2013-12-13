/**
 * 
 */
package com.hp.myidea.obdproxy.base;

import com.hp.myidea.obdproxy.IResultReader;

/**
 * @author mauricio
 *
 */
public enum OBDCommand {

    // Comandos necessarios para inicializacao
    SET_DEFAULTS("set defaults", "AT D", null),
    RESET_ALL("reset OBD", "AT Z", null),
    ECHO_OFF("echo off", "AT E0", null),
    LINE_FEED_OFF("linefeed off", "AT L0", null),
    SELECT_AUTO_PROTOCOL("select protocol auto","AT SP 0", null),
    TIME_OUT("set timeout","AT ST ", null),

    // Comandos de leitura de dados correntes (current data - ver http://en.wikipedia.org/wiki/OBD-II_PIDs)
    AMBIENT_AIR_TEMPERATURE("ambient air temperature", "01 46", new AirTempReader()),
    COOLANT_TEMPERATURE("coolant temperature", "01 05", new AirTempReader()),
    ENGINE_RPM("engine rpm", "01 0C", null),
    SPEED("speed", "01 0D", null),
    FUEL_LEVEL("fuel level", "01 2F", null);

    private final String name;
    private final String obdCode;
    private final IResultReader reader;

    private OBDCommand(String name, String obdCode, IResultReader reader) {
        this.name = name;
        this.obdCode = obdCode;
        this.reader = reader;
    }
    
    public String getName() {
        return this.name;
    }

    public String getOBDcode() {
        return this.obdCode;
    }
    
    public IResultReader getReader() {
        return this.reader;
    }
}
