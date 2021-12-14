package it.pgp.currenttoggles;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

import it.pgp.currenttoggles.utils.Misc;
import it.pgp.currenttoggles.utils.RootHandler;

public class MainActivity extends Activity {

    public interface II {
        boolean isEnabled(Context context);
    }

    public static final Handler h = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public static void toggleDataWifiBluetooth(Context context, String channel, II ii) { // channel: "data" or "wifi
        String[][] cmdsAndErrors = {
                {channel + " currently DISABLED -> enabling...", "enable"},
                {channel + " currently ENABLED -> disabling...", "disable"}
        };
        int i = ii.isEnabled(context) ? 1 : 0;
        Toast.makeText(context, cmdsAndErrors[i][0], Toast.LENGTH_SHORT).show();
        try {
            // svc data enable VS svc data disable
            // svc wifi enable VS svc wifi disable
            RootHandler.executeCommandAndWaitFor("svc", null, true, channel, cmdsAndErrors[i][1]);
        }
        catch (IOException e) {
            e.printStackTrace();
            h.postDelayed(()->Toast.makeText(context, "IOException", Toast.LENGTH_SHORT).show(),1000);
        }
    }

    public static void toggleAirplane(Context context) {
        String[] msgs = {"Airplane Currently DISABLED -> enabling...", "Airplane Currently ENABLED -> disabling..."};

        int airplaneEnabled = Misc.isAirplaneModeEnabled(context) ? 1 : 0;
        Toast.makeText(context, msgs[airplaneEnabled], Toast.LENGTH_SHORT).show();
        try {
            RootHandler.executeCommandAndWaitFor(
                    "settings put global airplane_mode_on "+(1-airplaneEnabled)+" && am broadcast -a android.intent.action.AIRPLANE_MODE", null, true);
        }
        catch(IOException e) {
            e.printStackTrace();
            h.postDelayed(()->Toast.makeText(context, "IOException", Toast.LENGTH_SHORT).show(),1000);
        }
    }

    public static void toggleBluetooth(Context context) {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        String msg = "Bluetooth ";
        if(bta.isEnabled()) {
            bta.disable();
            msg += "disabled";
        }
        else {
            bta.enable();
            msg += "enabled";
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public void toggle(View v) {
        switch(v.getId()) {
            case R.id.toggleData:
                toggleDataWifiBluetooth(this, "data", Misc::isDataConnectionEnabled);
                break;
            case R.id.toggleWifi:
                toggleDataWifiBluetooth(this, "wifi", Misc::isWifiEnabled);
                break;
            case R.id.toggleBt:
//                toggleDataWifiBluetooth(this, "bluetooth", Misc::isBluetoothEnabled);
                toggleBluetooth(this);
                break;
            case R.id.toggleAirplane:
                toggleAirplane(this);
        }
    }
}