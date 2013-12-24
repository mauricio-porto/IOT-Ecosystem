/*
 * TODO put header
 */

package eu.lighthouselabs.obd.commands;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import eu.lighthouselabs.obd.reader.OBDReaderApplication;

import android.content.Context;
import android.util.Log;

/**
 * TODO put description
 */
public abstract class ObdBaseCommand {
    private static final String TAG = ObdBaseCommand.class.getSimpleName();
    private static String FILE_NAME = "ODBReader_log.txt";
    private Context appContext;
    private FileOutputStream fos;

    protected ArrayList<Integer> buffer = null;
	protected String cmd = null;
	protected boolean useImperialUnits = false;
	protected String rawData = null;

	/**
	 * Default ctor to use
	 * 
	 * @param command
	 *            the command to send
	 */
	public ObdBaseCommand(String command) {
		this.cmd = command;
		this.buffer = new ArrayList<Integer>();
		this.appContext = OBDReaderApplication.getAppContext();
	}

	/**
	 * Prevent empty instantiation
	 */
	private ObdBaseCommand() {
	}

	/**
	 * Copy ctor.
	 * 
	 * @param other
	 *            the ObdBaseCommand to copy.
	 */
	public ObdBaseCommand(ObdBaseCommand other) {
		this(other.cmd);
	}

	/**
	 * Sends the OBD-II request and deals with the response.
	 * 
	 * This method CAN be overriden in fake commands.
	 */
	public void run(InputStream in, OutputStream out) throws IOException,
			InterruptedException {
        this.fos = this.appContext.openFileOutput(FILE_NAME, Context.MODE_APPEND);
		sendCommand(out);
		readResult(in);
		this.fos.close();
	}

	/**
	 * Sends the OBD-II request.
	 * 
	 * This method may be overriden in subclasses, such as ObMultiCommand or
	 * TroubleCodesObdCommand.
	 * 
	 * @param out
	 *            The output stream.
	 */
	protected void sendCommand(OutputStream out) throws IOException,
			InterruptedException {
		// add the carriage return char
		cmd += "\r";

		// write to OutputStream, or in this case a BluetoothSocket
		Log.d(TAG, "sendCommand() will send: " + cmd);
		this.fos.write("\nSent: ".getBytes());
		this.fos.write(cmd.getBytes());
		out.write(cmd.getBytes());
		out.flush();
		this.fos.flush();

		/*
		 * HACK GOLDEN HAMMER ahead!!
		 * 
		 * TODO clean
		 * 
		 * Due to the time that some systems may take to respond, let's give it
		 * 500ms.
		 */
		Thread.sleep(200);
	}

	/**
	 * Resends this command.
	 * 
	 * 
	 */
	protected void resendCommand(OutputStream out) throws IOException,
			InterruptedException {
		out.write("\r".getBytes());
		out.flush();
		/*
		 * HACK GOLDEN HAMMER ahead!!
		 * 
		 * TODO clean this
		 * 
		 * Due to the time that some systems may take to respond, let's give it
		 * 500ms.
		 */
		// Thread.sleep(250);
	}

	/**
	 * Reads the OBD-II response.
	 * 
	 * This method may be overriden in subclasses, such as ObdMultiCommand.
	 */
	protected void readResult(InputStream in) throws IOException {
        readRawData(in);
        fillBuffer();
	}

    protected abstract void fillBuffer();

    void readRawData(InputStream in) throws IOException {
        byte b = 0;
        StringBuilder res = new StringBuilder();

        // read until '>' arrives
        while ((char) (b = (byte) in.read()) != '>')
            if ((char) b != ' ')
                res.append((char) b);

		/*
		 * Imagine the following response 41 0c 00 0d.
		 *
		 * ELM sends strings!! So, ELM puts spaces between each "byte". And pay
		 * attention to the fact that I've put the word byte in quotes, because
		 * 41 is actually TWO bytes (two chars) in the socket. So, we must do
		 * some more processing..
		 */
        //
        Log.d(TAG, "readRawData() received (no trim): " + res.toString());
        rawData = res.toString().trim();
        Log.d(TAG, "readRawData() will put rawData as (trimmed): " + rawData);
        this.fos.write("\nReceived: ".getBytes());
        this.fos.write(rawData.getBytes());
        this.fos.flush();
    }

	/**
	 * @return the raw command response in string representation.
	 */
	public String getResult() {
		if (rawData.contains("SEARCHING") || rawData.contains("DATA")) {
			rawData = "NODATA";
		}

		return rawData;
	}

	/**
	 * @return a formatted command response in string representation.
	 */
	public abstract String getFormattedResult();

	/******************************************************************
	 * Getters & Setters
	 */

	/**
	 * @return a list of integers
	 */
	public ArrayList<Integer> getBuffer() {
		return buffer;
	}

	/**
	 * Returns this command in string representation.
	 * 
	 * @return the command
	 */
	public String getCommand() {
		return cmd;
	}

	/**
	 * @return true if imperial units are used, or false otherwise
	 */
	public boolean useImperialUnits() {
		return useImperialUnits;
	}

	/**
	 * Set to 'true' if you want to use imperial units, false otherwise. By
	 * default this value is set to 'false'.
	 * 
	 * @param isImperial
	 */
	public void useImperialUnits(boolean isImperial) {
		this.useImperialUnits = isImperial;
	}

	/**
	 * @return the OBD command name.
	 */
	public abstract String getName();

}