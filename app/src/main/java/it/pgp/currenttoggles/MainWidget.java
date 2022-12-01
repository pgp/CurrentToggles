package it.pgp.currenttoggles;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.HashMap;
import java.util.Map;

import it.pgp.currenttoggles.utils.Misc;


public class MainWidget extends AppWidgetProvider {

    public static final String LOG_PREFIX = "CURRENTTOGGLES";

    private static final String onDemandWifi = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_WIFI";
    private static final String onDemandData = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_DATA";
    private static final String onDemandHotspot = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_HOTSPOT";
    private static final String onDemandBluetooth = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_BLUETOOTH";
    private static final String onDemandGps = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_GPS";
    private static final String onDemandAutoBrightness = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_AUTO_BR";
    private static final String onDemandFlashlight = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_FLASH";
    private static final String onDemandAirplane = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_AIRPLANE";
    private static final String onDemandES = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_ES";
    private static final String onDemandTurnOffScreen = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_TURN_OFF_SCREEN";

    public static final Map<String,Integer> m = new HashMap<>();
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
    }

    public static void updateAllDirect(Context context) {
        Log.d(MainWidget.class.getName(),"updateAllDirect");
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, MainWidget.class));
        widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.buttons_widget);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        boolean notifsOff = Build.VERSION.SDK_INT >= 33 && !nm.areNotificationsEnabled();

        for(int appWidgetId : ids) {
            final int[] w_ids = new int[]{appWidgetId};
            Intent ii;
            PendingIntent pi;

            if(notifsOff) for(Map.Entry<String,Integer> entry : m.entrySet()) {
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
                case onDemandData:
                    Log.d(LOG_PREFIX,"onDemand Data");
                    MainActivity.toggleDataWifiBluetoothGps(context, "data", Misc::isDataConnectionEnabled);
                    break;
                case onDemandHotspot:
                    Log.d(LOG_PREFIX,"onDemand Hotspot");
                    MainActivity.toggleHotspot(context);
                    break;
                case onDemandBluetooth:
                    Log.d(LOG_PREFIX,"onDemand Bluetooth");
//                    MainActivity.toggleDataWifiBluetooth(context, "bluetooth", Misc::isBluetoothEnabled);
                    MainActivity.toggleBluetooth(context);
                    break;
                case onDemandGps:
                    Log.d(LOG_PREFIX,"onDemand gps");
                    MainActivity.toggleDataWifiBluetoothGps(context, "gps", Misc::isGpsEnabled);
                    break;
                case onDemandAutoBrightness:
                    Log.d(LOG_PREFIX,"onDemand Auto Brightness");
                    MainActivity.toggleAutoScreenBrightness(context);
                    break;
                case onDemandFlashlight:
                    Log.d(LOG_PREFIX,"onDemand Flashlight");
                    MainActivity.toggleFlashlight(context);
                    break;
                case onDemandAirplane:
                    Log.d(LOG_PREFIX,"onDemand Airplane");
                    MainActivity.toggleAirplane(context);
                    break;
                case onDemandES:
                    Log.d(LOG_PREFIX,"onDemand ES");
                    MainActivity.toggleEnergySaving(context);
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
