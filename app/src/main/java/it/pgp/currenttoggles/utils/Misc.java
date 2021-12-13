package it.pgp.currenttoggles.utils;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

// https://stackoverflow.com/questions/12806709/how-to-tell-if-mobile-network-data-is-enabled-or-disabled-even-when-connected

public class Misc {
    @TargetApi(Build.VERSION_CODES.O)
    public static boolean isDataConnectionEnabled(Context context) {
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.isDataEnabled();
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
