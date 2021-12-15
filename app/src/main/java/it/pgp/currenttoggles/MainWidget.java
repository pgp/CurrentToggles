package it.pgp.currenttoggles;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import it.pgp.currenttoggles.utils.Misc;


public class MainWidget extends AppWidgetProvider {

    public static final String LOG_PREFIX = "CURRENTTOGGLES";

    private static final String onDemandWifi = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_WIFI";
    private static final String onDemandData = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_DATA";
    private static final String onDemandBluetooth = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_BLUETOOTH";
    private static final String onDemandAutoBrightness = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_AUTO_BR";
    private static final String onDemandAirplane = "it.pgp.currenttoggles.appwidget.action.ON_DEMAND_AIRPLANE";

    public static void updateAllDirect(Context context) {
        Log.d(MainWidget.class.getName(),"updateAllDirect");
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, MainWidget.class));
        widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.buttons_widget);

        for (int appWidgetId : ids) {
            final int[] w_ids = new int[]{appWidgetId};
            Intent ii;
            PendingIntent pi;

            ii = new Intent(context, MainWidget.class);
            ii.setAction(onDemandData);
            ii.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, w_ids);
            pi = PendingIntent.getBroadcast(
                    context, appWidgetId, ii,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.toggle_data, pi);

            ii = new Intent(context, MainWidget.class);
            ii.setAction(onDemandWifi);
            ii.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, w_ids);
            pi = PendingIntent.getBroadcast(
                    context, appWidgetId, ii,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.toggle_wifi, pi);

            ii = new Intent(context, MainWidget.class);
            ii.setAction(onDemandBluetooth);
            ii.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, w_ids);
            pi = PendingIntent.getBroadcast(
                    context, appWidgetId, ii,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.toggle_bt, pi);

            ii = new Intent(context, MainWidget.class);
            ii.setAction(onDemandAutoBrightness);
            ii.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, w_ids);
            pi = PendingIntent.getBroadcast(
                    context, appWidgetId, ii,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.toggle_auto_brightness, pi);

            ii = new Intent(context, MainWidget.class);
            ii.setAction(onDemandAirplane);
            ii.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, w_ids);
            pi = PendingIntent.getBroadcast(
                    context, appWidgetId, ii,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.toggle_airplane, pi);

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
                    MainActivity.toggleDataWifiBluetooth(context, "wifi", Misc::isWifiEnabled);
                    break;
                case onDemandData:
                    Log.d(LOG_PREFIX,"onDemand Data");
                    MainActivity.toggleDataWifiBluetooth(context, "data", Misc::isDataConnectionEnabled);
                    break;
                case onDemandBluetooth:
                    Log.d(LOG_PREFIX,"onDemand Bluetooth");
//                    MainActivity.toggleDataWifiBluetooth(context, "bluetooth", Misc::isBluetoothEnabled);
                    MainActivity.toggleBluetooth(context);
                    break;
                case onDemandAutoBrightness:
                    Log.d(LOG_PREFIX,"onDemand Auto Brightness");
                    MainActivity.toggleAutoScreenBrightness(context);
                    break;
                case onDemandAirplane:
                    Log.d(LOG_PREFIX,"onDemand Airplane");
                    MainActivity.toggleAirplane(context);
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
