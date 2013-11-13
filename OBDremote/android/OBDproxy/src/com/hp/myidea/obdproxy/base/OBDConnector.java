/**
 * 
 */
package com.hp.myidea.obdproxy.base;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.hp.myidea.obdproxy.R;

import eu.lighthouselabs.obd.commands.SpeedObdCommand;
import eu.lighthouselabs.obd.commands.control.CommandEquivRatioObdCommand;
import eu.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import eu.lighthouselabs.obd.commands.engine.MassAirFlowObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelEconomyObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelEconomyWithMAFObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelLevelObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelTrimObdCommand;
import eu.lighthouselabs.obd.commands.temperature.AmbientAirTemperatureObdCommand;
import eu.lighthouselabs.obd.enums.FuelTrim;
import eu.lighthouselabs.obd.enums.FuelType;
import eu.lighthouselabs.obd.reader.io.ObdCommandJob;

/**
 * @author mapo
 *
 */
public class OBDConnector {

    private static final String TAG = OBDConnector.class.getSimpleName();

    private int speed = 1;
    private double maf = 1;
    private float ltft = 0;
    private double equivRatio = 1;

    private Handler mHandler = new Handler();

    /**
     * 
     */
    public OBDConnector() {
        // TODO Auto-generated constructor stub
    }

    /**
     * 
     */
    private Runnable mQueueCommands = new Runnable() {
        public void run() {
            /*
             * If values are not default, then we have values to calculate MPG
             */
            Log.d(TAG, "SPD:" + speed + ", MAF:" + maf + ", LTFT:" + ltft);
            if (speed > 1 && maf > 1 && ltft != 0) {
                FuelEconomyWithMAFObdCommand fuelEconCmd = new FuelEconomyWithMAFObdCommand(FuelType.DIESEL, speed, maf, ltft, false /* TODO */);
                String liters100km = String.format("%.2f", fuelEconCmd.getLitersPer100Km());
                Log.d(TAG, "FUELECON:" + liters100km);
            }

/*            if (mServiceConnection.isRunning())
                queueCommands();
*/
            // run again in 2s
            mHandler.postDelayed(mQueueCommands, 2000);
        }
    };

    /**
     * 
     */
    private void queueCommands() {
        final ObdCommandJob airTemp = new ObdCommandJob(new AmbientAirTemperatureObdCommand());
        final ObdCommandJob speed = new ObdCommandJob(new SpeedObdCommand());
        final ObdCommandJob fuelEcon = new ObdCommandJob(new FuelEconomyObdCommand());
        final ObdCommandJob rpm = new ObdCommandJob(new EngineRPMObdCommand());
        final ObdCommandJob maf = new ObdCommandJob(new MassAirFlowObdCommand());
        final ObdCommandJob fuelLevel = new ObdCommandJob(new FuelLevelObdCommand());
        final ObdCommandJob ltft1 = new ObdCommandJob(new FuelTrimObdCommand(FuelTrim.LONG_TERM_BANK_1));
        final ObdCommandJob ltft2 = new ObdCommandJob(new FuelTrimObdCommand(FuelTrim.LONG_TERM_BANK_2));
        final ObdCommandJob stft1 = new ObdCommandJob(new FuelTrimObdCommand(FuelTrim.SHORT_TERM_BANK_1));
        final ObdCommandJob stft2 = new ObdCommandJob(new FuelTrimObdCommand(FuelTrim.SHORT_TERM_BANK_2));
        final ObdCommandJob equiv = new ObdCommandJob(new CommandEquivRatioObdCommand());

        // mServiceConnection.addJobToQueue(airTemp);
/*        mServiceConnection.addJobToQueue(speed);
        // mServiceConnection.addJobToQueue(fuelEcon);
        mServiceConnection.addJobToQueue(rpm);
        mServiceConnection.addJobToQueue(maf);
        mServiceConnection.addJobToQueue(fuelLevel);
        // mServiceConnection.addJobToQueue(equiv);
        mServiceConnection.addJobToQueue(ltft1);
*/        // mServiceConnection.addJobToQueue(ltft2);
        // mServiceConnection.addJobToQueue(stft1);
        // mServiceConnection.addJobToQueue(stft2);
    }

}
