package it.pgp.currenttoggles.utils;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

public class Misc {
    public static boolean isDataConnectionEnabled(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.isDataEnabled();
        }
        else {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            try {
                Class<?> cmClass = Class.forName(cm.getClass().getName());
                Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
                method.setAccessible(true);
                return (Boolean)method.invoke(cm);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public static boolean isWifiEnabled(Context context) {
        WifiManager wifimanager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifimanager == null) return false; // WIFI_STATE.NO_ADAPTER_FOUND;
        return wifimanager.isWifiEnabled();
    }

    public static boolean isAirplaneModeEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public static boolean isBluetoothEnabled(Context unused){
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }
}
