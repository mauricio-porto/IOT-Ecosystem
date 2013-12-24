package eu.lighthouselabs.obd.reader;

import android.app.Application;
import android.content.Context;

public class OBDReaderApplication extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        OBDReaderApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return OBDReaderApplication.context;
    }
}
