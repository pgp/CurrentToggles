package it.pgp.currenttoggles;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
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

    public static void toggleDataWifiBluetoothGps(Context context, String channel, II ii) { // channel: "data" or "wifi"
        String[][] cmdsAndErrors = {
                {channel + " currently DISABLED -> enabling...", "enable"},
                {channel + " currently ENABLED -> disabling...", "disable"}
        };
        int i = ii.isEnabled(context) ? 1 : 0;
        Toast.makeText(context, cmdsAndErrors[i][0], Toast.LENGTH_SHORT).show();
        try {
            if(!("gps".equals(channel))) {
                // svc data enable VS svc data disable
                // svc wifi enable VS svc wifi disable
                RootHandler.executeCommandAndWaitFor("svc "+channel+" "+cmdsAndErrors[i][1], null, true, null);
            }
            else {
                String command;
                String cmdPrefix;
                String[] toggles;
                String commonPrefix = "settings put secure ";
                if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // handle GPS till Android 9
                    cmdPrefix = "location_providers_allowed ";
                    toggles = new String[]{"+gps,+network,+wifi","-gps,-network,-wifi"};
                }
                else { // handle GPS from Android 10 onwards
                    cmdPrefix = "location_mode ";
                    toggles = new String[]{"3","0"};
                }
                command = commonPrefix+cmdPrefix+toggles[i];
                RootHandler.executeCommandAndWaitFor(command,null,true,null);
            }
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
                    "settings put global airplane_mode_on "+(1-airplaneEnabled)+" && am broadcast -a android.intent.action.AIRPLANE_MODE",
                    null, true, null);
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

    public static void toggleAutoScreenBrightness(Context context) {
        ContentResolver resolver = context.getContentResolver();
        try {
            boolean isAuto = Settings.System.getInt(resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            Settings.System.putInt(resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL); // set to 0 again, even if it is already 0
            if(!isAuto) {
                Settings.System.putInt(resolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC); // now it auto-adapts
            }
            Toast.makeText(context, "Brightness set to " + (isAuto ? "manual" : "auto"), Toast.LENGTH_SHORT).show();
        }
        catch(Settings.SettingNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context, "Unable to modify auto/manual brightness setting", Toast.LENGTH_SHORT).show();
        }
        catch(SecurityException e) {
            e.printStackTrace();
            Toast.makeText(context, "Please grant system settings write permission in order to use this toggle", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

    // we don't want to register any TorchCallback, just start with flash off assumption and then toggle from there
    static boolean flashlightEnabled = false;
    public static void toggleFlashlight(Context context) {
        CameraManager camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String cameraId;
        try {
            cameraId = camManager.getCameraIdList()[0];
        }
        catch(Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Unable to access flashlight", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean b = !flashlightEnabled;
        try {
            camManager.setTorchMode(cameraId, b);
            flashlightEnabled = b;
            Toast.makeText(context, "Flashlight "+(b?"ON":"OFF"), Toast.LENGTH_SHORT).show();
        }
        catch(Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Unable to toggle flashlight status", Toast.LENGTH_SHORT).show();
        }
    }

    public static void toggleEnergySaving(Context context) {
        String getCmd = "settings get global low_power";
        String[] msgs = {"Energy saving currently DISABLED -> enabling...", "Energy saving currently ENABLED -> disabling..."};
        StringBuilder output = new StringBuilder();
        try {
            int exitValue = RootHandler.executeCommandAndWaitFor(getCmd, null, true, output);
            if(exitValue != 0) throw new Exception("Exit value for toggleEnergySaving get command: "+exitValue);
            int ESState = Integer.parseInt(output.toString().trim());
            if(ESState != 0 && ESState != 1) throw new Exception("Unexpected parsed ES state: "+ESState);
            Toast.makeText(context, msgs[ESState], Toast.LENGTH_SHORT).show();
            ESState = 1 - ESState;
            RootHandler.executeCommandAndWaitFor("settings put global low_power "+ESState,null,true,null);
        }
        catch(Exception e) {
            e.printStackTrace();
            h.postDelayed(()->Toast.makeText(context, "Exception: "+e.getMessage(), Toast.LENGTH_SHORT).show(),1000);
        }
    }

    public void toggle(View v) {
        switch(v.getId()) {
            case R.id.toggleData:
                toggleDataWifiBluetoothGps(this, "data", Misc::isDataConnectionEnabled);
                break;
            case R.id.toggleWifi:
                toggleDataWifiBluetoothGps(this, "wifi", Misc::isWifiEnabled);
                break;
            case R.id.toggleBt:
//                toggleDataWifiBluetoothGps(this, "bluetooth", Misc::isBluetoothEnabled); // no need to do this using root
                toggleBluetooth(this);
                break;
            case R.id.toggleGps:
                toggleDataWifiBluetoothGps(this, "gps", Misc::isGpsEnabled);
                break;
            case R.id.toggleAutoBrightness:
                toggleAutoScreenBrightness(this);
                break;
            case R.id.toggleFlashlight:
                toggleFlashlight(this);
                break;
            case R.id.toggleAirplane:
                toggleAirplane(this);
                break;
            case R.id.toggleES:
                toggleEnergySaving(this);
        }
    }
}