package in.naveens.mqttbroker;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefConfig {
    private static final String MY_PREFERENCE_NAME = "com.iot.kapelnisa";
    private static final String PREF_STATUS_KEY = "pref_status_key";

    // NOTIFICATION
    public static void saveNotify(Context context, boolean status) {
        SharedPreferences pref = context.getSharedPreferences(MY_PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(PREF_STATUS_KEY, status);
        editor.apply();
    }
    public static boolean loadNotify(Context context) {
        SharedPreferences pref = context.getSharedPreferences(MY_PREFERENCE_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean(PREF_STATUS_KEY, false);
    }

}