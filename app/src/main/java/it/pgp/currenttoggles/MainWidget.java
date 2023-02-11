package it.pgp.currenttoggles;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.HashMap;
import java.util.Map;

import it.pgp.currenttoggles.utils.Misc;


public class MainWidget extends AppWidgetProvider {

    public static final String LOG_PREFIX = "CURRENTTOGGLES";

    private static final String onDemandWifi = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_WIFI";
    private static final String wifiOptions = "it.pgp.currenttoggles.appwidget.action.WIFI_OPTIONS";
    private static final String onDemandData = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_DATA";
    private static final String dataOptions = "it.pgp.currenttoggles.appwidget.action.DATA_OPTIONS";
    private static final String onDemandHotspot = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_HOTSPOT";
    private static final String hotspotOptions = "it.pgp.currenttoggles.appwidget.action.HOTSPOT_OPTIONS";
    private static final String onDemandBluetooth = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_BLUETOOTH";
    private static final String bluetoothOptions = "it.pgp.currenttoggles.appwidget.action.BLUETOOTH_OPTIONS";
    private static final String onDemandGps = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_GPS";
    private static final String gpsOptions = "it.pgp.currenttoggles.appwidget.action.GPS_OPTIONS";
    private static final String onDemandAutoBrightness = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_AUTO_BR";
    private static final String displayOptions = "it.pgp.currenttoggles.appwidget.action.DISPLAY_OPTIONS";
    private static final String onDemandFlashlight = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_FLASH";
    private static final String onDemandAirplane = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_AIRPLANE";
    private static final String airplaneOptions = "it.pgp.currenttoggles.appwidget.action.AIRPLANE_OPTIONS";
    private static final String onDemandES = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_ES";
    private static final String esOptions = "it.pgp.currenttoggles.appwidget.action.ES_OPTIONS";
    private static final String onDemandTurnOffScreen = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_TURN_OFF_SCREEN";

    public static final Map<String,Integer> m = new HashMap<>();
    public static final Map<String,Integer> o = new HashMap<>();

    static {
        m.put(onDemandWifi, R.id.toggle_wifi);
        m.put(onDemandData, R.id.toggle_data);
        m.put(onDemandHotspot, R.id.toggle_hotspot);
        m.put(onDemandBluetooth, R.id.toggle_bt);
        m.put(onDemandGps, R.id.toggle_gps);
        m.put(onDemandAutoBrightness, R.id.toggle_auto_brightness);
        m.put(onDemandFlashlight, R.id.toggle_flashlight);
        m.put(onDemandAirplane, R.id.toggle_airplane);
        m.put(onDemandES, R.id.toggle_es);
        m.put(onDemandTurnOffScreen, R.id.turnoff_screen);

        o.put(dataOptions, R.id.data_options);
        o.put(hotspotOptions, R.id.hotspot_options);
        o.put(wifiOptions, R.id.wifi_options);
        o.put(bluetoothOptions, R.id.bluetooth_options);
        o.put(gpsOptions, R.id.gps_options);
        o.put(displayOptions, R.id.display_options);
        o.put(airplaneOptions, R.id.airplane_options);
        o.put(esOptions, R.id.es_options);
    }


    public static void updateAllDirect(Context context) {
        Log.d(MainWidget.class.getName(),"updateAllDirect");
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, MainWidget.class));
        widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.buttons_widget);
        boolean notifsOn = Build.VERSION.SDK_INT < 33; // Android 13 (Build.VERSION_CODES.TIRAMISU)
        if(!notifsOn) {
            notifsOn = ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).areNotificationsEnabled();
        }

        for(int appWidgetId : ids) {
            final int[] w_ids = new int[]{appWidgetId};
            Intent ii;
            PendingIntent pi;

            if(!notifsOn) for(Map.Entry<String,Integer> entry : m.entrySet()) {
                // this is not enough: we need a callback (onActivityResult) to redraw the widget on permissions granted
//                ii = new Intent();
//                ii.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
//                ii.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                ii.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());

                ii = new Intent(context, MainActivity.class);
                ii.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ii.putExtra("NOTIFS", "");

                ii.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, w_ids);
                pi = PendingIntent.getActivity(context, 0, ii, 0);
                remoteViews.setOnClickPendingIntent(entry.getValue(), pi);
            }
            else for(Map.Entry<String,Integer> entry : m.entrySet()) {
                ii = new Intent(context, MainWidget.class);
                ii.setAction(entry.getKey());
                ii.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, w_ids);
                pi = PendingIntent.getBroadcast(context, appWidgetId, ii, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(entry.getValue(), pi);
            }

            for(Map.Entry<String,Integer> entry : o.entrySet()) {
                ii = new Intent(context, MainWidget.class);
                ii.setAction(entry.getKey());
                ii.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, w_ids);
                pi = PendingIntent.getBroadcast(context, appWidgetId, ii, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(entry.getValue(), pi);
            }

            widgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String a = intent.getAction();
        Log.d(LOG_PREFIX,"onReceive action: "+intent.getAction());
        if (a == null) return;
        try {
            switch(a) {
                case onDemandWifi:
                    Log.d(LOG_PREFIX,"onDemand Wifi");
                    MainActivity.toggleWifi(context);
                    break;
                case wifiOptions:
                    Intent options = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    options.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(options);
                    break;
                case onDemandData:
                    Log.d(LOG_PREFIX,"onDemand Data");
                    MainActivity.toggleDataWifiBluetoothGps(context, "data", Misc::isDataConnectionEnabled);
                    break;
                case dataOptions:
                    options = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                    options.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(options);
                    break;
                case onDemandHotspot:
                    Log.d(LOG_PREFIX,"onDemand Hotspot");
                    MainActivity.toggleHotspot(context);
                    break;
                case hotspotOptions:
                    options = new Intent(context, MainActivity.class);
                    options.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    options.putExtra("HOTSPOT_OPTIONS", "");
                    context.startActivity(options);
                    break;
                case onDemandBluetooth:
                    Log.d(LOG_PREFIX,"onDemand Bluetooth");
//                    MainActivity.toggleDataWifiBluetooth(context, "bluetooth", Misc::isBluetoothEnabled);
                    MainActivity.toggleBluetooth(context);
                    break;
                case bluetoothOptions:
                    options = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                    options.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(options);
                    break;
                case onDemandGps:
                    Log.d(LOG_PREFIX,"onDemand gps");
                    MainActivity.toggleDataWifiBluetoothGps(context, "gps", Misc::isGpsEnabled);
                    break;
                case gpsOptions:
                    options = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    options.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(options);
                    break;
                case onDemandAutoBrightness:
                    Log.d(LOG_PREFIX,"onDemand Auto Brightness");
                    MainActivity.toggleAutoScreenBrightness(context);
                    break;
                case displayOptions:
                    options = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                    options.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(options);
                    break;
                case onDemandFlashlight:
                    Log.d(LOG_PREFIX,"onDemand Flashlight");
                    MainActivity.toggleFlashlight(context);
                    break;
                case onDemandAirplane:
                    Log.d(LOG_PREFIX,"onDemand Airplane");
                    MainActivity.toggleAirplane(context);
                    break;
                case airplaneOptions:
                    options = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                    options.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(options);
                    break;
                case onDemandES:
                    Log.d(LOG_PREFIX,"onDemand ES");
                    MainActivity.toggleEnergySaving(context);
                    break;
                case esOptions:
                    options = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                    options.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(options);
                    break;
                case onDemandTurnOffScreen:
                    Log.d(LOG_PREFIX,"onDemand Turn Off Screen");
                    MainActivity.turnOffAndLockScreen(context);
                    break;
                default:
                    break;
            }
            updateAllDirect(context);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
