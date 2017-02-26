package com.udacity.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.util.Constants;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import static com.udacity.stockhawk.R.id.price;
import static com.udacity.stockhawk.R.id.symbol;

/**
 * Created by darshan on 11/2/17.
 */

class StockQuoteRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;

    private String[] projection = new String[]{
            Contract.Quote.COLUMN_SYMBOL,
            Contract.Quote.COLUMN_NAME,
            Contract.Quote.COLUMN_PRICE,
            Contract.Quote.COLUMN_PERCENTAGE_CHANGE
    };

    private ArrayList<StockQuoteWidgetItem> widgetItems;

    private DecimalFormat dollarFormat;
    private DecimalFormat percentageFormat;

    private static void log(String message) {
        Log.i("SH-SQRVF", message);
    }

    StockQuoteRemoteViewsFactory(Context context) {
        log("StockQuoteRemoteViewsFactory");
        this.context = context;

        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);

        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");
    }

    @Override
    public void onCreate() {
        log("onCreate");
    }

    @Override
    public void onDataSetChanged() {
        log("onDataSetChanged");
        Cursor cursor = context.getContentResolver()
                .query(
                        Contract.Quote.URI,
                        projection,
                        null,
                        null,
                        null
                );
        if (cursor == null) {
            return;
        }

        if (widgetItems == null) {
            widgetItems = new ArrayList<>(cursor.getCount());
        } else {
            widgetItems.clear();
        }

        while (cursor.moveToNext()) {
            String symbol = cursor.getString(cursor.getColumnIndex(projection[0]));
            String name = cursor.getString(cursor.getColumnIndex(projection[1]));
            String price = dollarFormat.format(cursor.getDouble(cursor.getColumnIndex(projection[2])));
            String percentageChange = percentageFormat.format(cursor.getDouble(cursor.getColumnIndex(projection[3])) / 100.0);
            widgetItems.add(new StockQuoteWidgetItem(symbol, name, price, percentageChange));
        }
        cursor.close();
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
    }

    @Override
    public int getCount() {
        if (widgetItems == null) {
            return 0;
        }
        return widgetItems.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.list_item_widget_stock_quote);
        remoteViews.setTextViewText(symbol, widgetItems.get(position).symbol);
        remoteViews.setTextViewText(price, widgetItems.get(position).price);
        int colorId = R.color.material_green_700;
        if (widgetItems.get(position).percentageChange.charAt(0) == '-') {
            colorId = R.color.material_red_700;
        }
        remoteViews.setTextColor(R.id.change, ContextCompat.getColor(context.getApplicationContext(), colorId));
        remoteViews.setTextViewText(R.id.change, widgetItems.get(position).percentageChange);

        Bundle extras = new Bundle();
        extras.putString(Constants.BUNDLE_STOCK_SYMBOL, widgetItems.get(position).symbol);
        extras.putString(Constants.BUNDLE_STOCK_NAME, widgetItems.get(position).name);
        extras.putString(
                Constants.BUNDLE_STOCK_PRICE,
                widgetItems.get(position).price.substring(1)
        );
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        remoteViews.setOnClickFillInIntent(R.id.widget_row, fillInIntent);

        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
