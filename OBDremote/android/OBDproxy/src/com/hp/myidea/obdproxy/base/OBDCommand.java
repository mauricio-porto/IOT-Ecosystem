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
    SET_DEFAULTS("set defaults", "AT D", null, null),
    RESET_ALL("reset OBD", "AT Z", null, null),
    ECHO_OFF("echo off", "AT E0", null, null),
    LINE_FEED_OFF("linefeed off", "AT L0", null, null),
    SELECT_AUTO_PROTOCOL("select protocol auto","AT SP 0", null, null),
    TIME_OUT("set timeout","AT ST F0", null, null),

    // Comandos de leitura de dados correntes (current data - ver http://en.wikipedia.org/wiki/OBD-II_PIDs)
    AMBIENT_AIR_TEMPERATURE("Ambient Air Temperature", "01 46", new TemperatureReader(), "ambTemp"),
    COOLANT_TEMPERATURE("Coolant Temperature", "01 05", new TemperatureReader(), "coolant"),
    INTAKE_AIR_TEMPERATURE("Intake Air Temperature", "01 0F", new TemperatureReader(), "intake"),
    ENGINE_LOAD("Engine Load", "01 04", new PercentualReader(), "load"),
    ENGINE_RPM("Engine RPM", "01 0C", new RPMReader(), "rpm"),
    SPEED("Vehicle Speed", "01 0D", new SpeedReader(), "speed"),
    MAF("Mass Air Flow", "01 10", new MAFReader(), "maf"),
    FUEL_LEVEL("Fuel Level", "01 2F", new FuelReader(), "fuel");

    private final String name;
    private final String obdCode;
    private final IResultReader reader;
    private final String mnemo;

    private OBDCommand(String name, String obdCode, IResultReader reader, String mnemo) {
        this.name = name;
        this.obdCode = obdCode;
        this.reader = reader;
        this.mnemo =mnemo;
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

    public String getMnemo() {
        return this.mnemo;
    }
    
}
