package it.pgp.currenttoggles;

import android.app.Activity;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

import it.pgp.currenttoggles.utils.Misc;
import it.pgp.currenttoggles.utils.RootHandler;
import it.pgp.currenttoggles.utils.oreoap.MyOnStartTetheringCallback;
import it.pgp.currenttoggles.utils.oreoap.MyOreoWifiManager;
import it.pgp.currenttoggles.utils.oreoap.PreOreoWifiManager;

public class MainActivity extends Activity {

    public interface II {
        boolean isEnabled(Context context);
    }

    public static final Handler h = new Handler(Looper.getMainLooper());

    public static void postToast(Context context, String msg) {
        h.post(()->Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }

    public static final String toastOffColor = "8d2626"; // red
    public static final String toastOnColor = "38761d"; // green

    public static void showToast(Context context, String msg, boolean onOff) {
        String colorString = onOff ? toastOnColor : toastOffColor;
        Toast toast = Toast.makeText(context, Html.fromHtml("<font color='#"+colorString+"' ><b>" + msg + "</b></font>"), Toast.LENGTH_LONG);
        h.post(toast::show);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1234) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if(nm.areNotificationsEnabled()) {
                MainWidget.updateAllDirect(getBaseContext());
                Toast.makeText(this, "Widget notifications enabled", Toast.LENGTH_SHORT).show();
            }
            else Toast.makeText(this, "Please grant notifications permissions on Android 13 in order to show toast messages from widget", Toast.LENGTH_SHORT).show();
            finishAffinity();
        }
        else if(requestCode == 1235) finishAffinity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO if needed, use same XFiles mechanism for avoiding repeated intents
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            if(extras.getString("NOTIFS") != null) {
                Intent ii = new Intent();
                ii.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
//                ii.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // needed only when launching directly from widget
                ii.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
                startActivityForResult(ii, 1234);
            }
            else if(extras.getString("HOTSPOT_OPTIONS") != null) {
                Intent options = new Intent(Intent.ACTION_MAIN, null);
                options.addCategory(Intent.CATEGORY_LAUNCHER);
                options.setComponent(new ComponentName("com.android.settings", "com.android.settings.TetherSettings"));
                startActivityForResult(options, 1235);
                return;
            }
        }
        setContentView(R.layout.activity_main);
    }

    public static void launchWriteSettings(Context context) {
        postToast(context, "Please grant system settings write permission in order to use this toggle");
        Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    public static void toggleWifi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean stateToSet = !wifiManager.isWifiEnabled();
        if(!wifiManager.setWifiEnabled(stateToSet)) {
            // try with root
            toggleDataWifiBluetoothGps(context, "wifi", Misc::isWifiEnabled);
        }
        else showToast(context, "Wifi "+(stateToSet?"enabled":"disabled"), stateToSet);
    }

    public static void toggleDataWifiBluetoothGps(Context context, String channel, II ii) { // channel: "data" or "wifi"
        String[][] cmdsAndErrors = {
                {channel + " currently DISABLED -> enabling...", "enable"},
                {channel + " currently ENABLED -> disabling...", "disable"}
        };
        int i = ii.isEnabled(context) ? 1 : 0;
        showToast(context, cmdsAndErrors[i][0], i == 0);
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
            postToast(context, "IOException");
        }
    }

    public static void toggleAirplane(Context context) {
        String[] msgs = {"Airplane Currently DISABLED -> enabling...", "Airplane Currently ENABLED -> disabling..."};
        int airplaneEnabled = Misc.isAirplaneModeEnabled(context) ? 1 : 0;
        showToast(context, msgs[airplaneEnabled], airplaneEnabled == 0);
        try {
            String command = Build.VERSION.SDK_INT < 30 ?
            "settings put global airplane_mode_on "+(1-airplaneEnabled)+" && am broadcast -a android.intent.action.AIRPLANE_MODE" :
            "cmd connectivity airplane-mode "+(airplaneEnabled == 0 ? "enable" : "disable");
            RootHandler.executeCommandAndWaitFor(
                    command,
                    null, true, null);
        }
        catch(IOException e) {
            e.printStackTrace();
            h.postDelayed(()->Toast.makeText(context, "IOException: "+e.getMessage(), Toast.LENGTH_SHORT).show(),1000);
        }
    }

    public static void toggleBluetooth(Context context) {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        String msg = "Bluetooth ";
        boolean currentState = bta.isEnabled();
        if(currentState) {
            bta.disable();
            msg += "disabled";
        }
        else {
            bta.enable();
            msg += "enabled";
        }
        showToast(context, msg, !currentState);
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
            postToast(context, "Brightness set to " + (isAuto ? "manual" : "auto"));
        }
        catch(Settings.SettingNotFoundException e) {
            e.printStackTrace();
            postToast(context, "Unable to modify auto/manual brightness setting");
        }
        catch(SecurityException e) {
            e.printStackTrace();
            launchWriteSettings(context);
        }
    }

    public static final String TORCH_MODE_PATH = "TORCH_MODE_PATH";
    public static final File workDir = new File("/sys/class/leds");

    public static String detectTorchModePath() {
        Log.d("CTORCH","Detecting torch mode file path...");
        int exitValue;
        HashSet<String> s1 = new HashSet<>();
        HashSet<String> s2 = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        try {
            // check for subdirs containing the "led:" substring
            exitValue = RootHandler.executeCommandAndWaitFor("ls -1d *led\\:*", workDir, true, sb);
            if(exitValue != 0) throw new Exception("led folders not found under /sys/class/leds");
            Collections.addAll(s1, sb.toString().split("[\\r\\n]+"));
            sb = new StringBuilder();
            // check for subdirs containing the "torch" substring
            exitValue = RootHandler.executeCommandAndWaitFor("ls -1d *torch*", workDir, true, sb);
            if(exitValue != 0) throw new Exception("torch folders not found under /sys/class/leds");
            Collections.addAll(s2, sb.toString().split("[\\r\\n]+"));

            s1.retainAll(s2); // intersection of s1 and s2 is put into s1
            if(s1.isEmpty()) throw new Exception("no common torch/led folder under /sys/class/leds");
            return s1.iterator().next();
        }
        catch(Exception e) {
            e.printStackTrace();
            Log.e("CTORCH","Unable to detect torch mode path");
            return "N/A";
        }
    }

    // 0: torch is off
    // 1: torch is on
    // -1: unable to detect (no root?)
    public static int detectTorchMode(Context context) {
        SharedPreferences settings = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        String torchModePath = settings.getString(TORCH_MODE_PATH, "");

        if("".equals(torchModePath)) { // no torch mode file path detection has been performed yet
            torchModePath = detectTorchModePath();
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(TORCH_MODE_PATH,torchModePath).apply();
        }
        if("N/A".equals(torchModePath)) return -1; // torch mode file path detection has been performed (in another run or just now), and there were errors
        else {
            try {
                StringBuilder sb = new StringBuilder();
                int exitValue = RootHandler.executeCommandAndWaitFor("cat "+torchModePath+"/brightness", workDir, true, sb);
                if(exitValue != 0) {
                    Log.e("CTORCH","no brightness file found under /sys/class/leds/" + torchModePath);
                    return -1;
                }
                return Integer.parseInt(sb.toString().trim()) == 0 ? 0 : 1;
            }
            catch(Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
    }

    /*
    when root is not available, current torch status can't be queried because /sys/class/leds is not accessible;
    in that case, just start assuming torch is off - this will result in having to press the torch button twice
    if you previously changed its status an odd number of times from outside this app/widget,
    i.e. from the system drop-down menu
    */
    public static boolean flashlightEnabled = false;
    public static void toggleFlashlight(Context context) {
        int flashlightStatus = detectTorchMode(context);
        if(flashlightStatus >= 0) flashlightEnabled = flashlightStatus != 0;
        CameraManager camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String cameraId;
        try {
            cameraId = camManager.getCameraIdList()[0];
        }
        catch(Exception e) {
            e.printStackTrace();
            postToast(context, "Unable to access flashlight");
            return;
        }
        boolean b = !flashlightEnabled;
        String resultMsg;
        try {
            camManager.setTorchMode(cameraId, b);
            flashlightEnabled = b;
            resultMsg = "Flashlight "+(b?"ON":"OFF");
            showToast(context, resultMsg, b);
        }
        catch(Exception e) {
            e.printStackTrace();
            postToast(context, "Unable to toggle flashlight status");
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
            showToast(context, msgs[ESState], ESState == 0);
            ESState = 1 - ESState;
            RootHandler.executeCommandAndWaitFor("settings put global low_power "+ESState,null,true,null);
        }
        catch(Exception e) {
            e.printStackTrace();
            h.postDelayed(()->Toast.makeText(context, "Exception: "+e.getMessage(), Toast.LENGTH_SHORT).show(),1000);
        }
    }

    public static void toggleHotspot(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            launchWriteSettings(context);
        }
        else {
            String resultMsg;
            boolean targetState = false;
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                PreOreoWifiManager apManager = new PreOreoWifiManager(context);
                targetState = !apManager.isApOn();
                if(apManager.configApState(targetState)) resultMsg = "AP "+(targetState?"started":"stopped");
                else {
                    targetState = false;
                    resultMsg = "Unable to change AP state";
                }
            }
            else {
                MyOreoWifiManager apManager = new MyOreoWifiManager(context);
                if(apManager.isTetherActive()) {
                    apManager.stopTethering();
                    resultMsg = "AP stopped";
                }
                else {
                    apManager.startTethering(new MyOnStartTetheringCallback());
                    resultMsg = "AP started";
                    targetState = true;
                }
            }
            showToast(context, resultMsg, targetState);
        }
    }

    public static void turnOffAndLockScreen(Context context) {
        int exitCode = -1;
        try {
            exitCode = RootHandler.executeCommandAndWaitFor("input keyevent KEYCODE_POWER",null,true,null);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        if(exitCode != 0) postToast(context, "Unable to turn off screen");
    }

    public void toggle(View v) {
        switch(v.getId()) {
            case R.id.toggleHotspot:
                toggleHotspot(this);
                break;
            case R.id.toggleData:
                toggleDataWifiBluetoothGps(this, "data", Misc::isDataConnectionEnabled);
                break;
            case R.id.toggleWifi:
                toggleWifi(this);
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
                break;
            case R.id.turnOffScreen:
                turnOffAndLockScreen(this);
        }
    }
}