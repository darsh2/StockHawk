package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.ui.activity.MainActivity;

/**
 * Created by darshan on 11/2/17.
 */

public class StockQuoteWidgetProvider extends AppWidgetProvider {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("SH-SQWP", "onReceive");
        Log.i("SH-SQWP", "Action: " + intent.getAction());
        super.onReceive(context, intent);
        if (!intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            return;
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, context.getPackageName()));
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i("SH-SQWP", "onUpdate");
        for (int i = 0, numWidgets = appWidgetIds.length; i < numWidgets; i++) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.stock_quote_widget_layout);
            remoteViews.setEmptyView(R.id.list_view_stock_quotes, R.id.text_view_empty_widget);

            Intent adapterIntent = new Intent(context, StockQuoteWidgetService.class);
            adapterIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            remoteViews.setRemoteAdapter(R.id.list_view_stock_quotes, adapterIntent);

            Intent onClickIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, onClickIntent, 0);
            remoteViews.setPendingIntentTemplate(R.id.list_view_stock_quotes, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds[i], R.id.list_view_stock_quotes);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
