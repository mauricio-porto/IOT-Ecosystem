/**
 * 
 */
package com.hp.myidea.obdproxy.base;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.hp.myidea.obdproxy.R;

import eu.lighthouselabs.obd.commands.ObdCommand;
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
import eu.lighthouselabs.obd.reader.IPostListener;
import eu.lighthouselabs.obd.reader.io.ObdCommandJob;

/**
 * @author mapo
 *
 */
public class OBDConnector {

    private static final String TAG = OBDConnector.class.getSimpleName();

    private BluetoothConnector connector;
    private IPostListener listener;

    //private ObdCommand[] paramList;
    private ArrayList<ObdCommand> paramList;

    private int speed = 1;
    private double maf = 1;
    private float ltft = 0;
    private double equivRatio = 1;

    private Handler mHandler = new Handler();
    private Handler anotherHandler = new Handler();

    /**
     * 
     */
    public OBDConnector(BluetoothConnector conn, IPostListener lstnr) {
        super();
        if (conn == null) {
            throw new IllegalArgumentException("MUST provide the BluetoothConenctor");
        }
        if (lstnr == null) {
            throw new IllegalArgumentException("MUST provide the IPostListener");
        }
        this.connector = conn;
        this.listener = lstnr;
        this.init();
    }

    private void init() {
        this.paramList = new ArrayList<ObdCommand>();
        this.paramList.add(new SpeedObdCommand());
        this.paramList.add(new EngineRPMObdCommand());
        this.paramList.add(new FuelLevelObdCommand());
        this.paramList.add(new MassAirFlowObdCommand());
        this.paramList.add(new AmbientAirTemperatureObdCommand());
    }

    public void startLiveData() {
        
    }

    public void stopLiveData() {
        
    }

    public void setParamList(ArrayList<ObdCommand> list) {
        if (list == null || list.size() == 0) {
            return;
        }
        this.paramList = list;
    }

    public ArrayList<ObdCommand> getParamList() {
        return this.paramList;
    }

    public boolean addParam(ObdCommand param) {
        boolean result = false;
        if (param != null && !this.paramList.contains(param)) {
            result = this.paramList.add(param);
        }
        return result;
    }

    public boolean removeParam(ObdCommand param) {
        boolean result = false;
        if (param != null && this.paramList.contains(param)) {
            result = this.paramList.remove(param);
        }
        return result;
    }

    private void startStopLiveData() {
        // Apenas para registrar como o OBD reader faz...
        mHandler.post(mQueueCommands);  // P/ iniciar
        mHandler.removeCallbacks(mQueueCommands); // P/ parar
    }

    private void repeatSomething() {
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                anotherHandler.post(new Runnable() {
                    
                    @Override
                    public void run() {
                        // The anotherHandler will run/call here the real job
                    }
                });
            }
        }).start();
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
