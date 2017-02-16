package com.udacity.stockhawk.widget;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by darshan on 11/2/17.
 */

class StockQuoteRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;

    private String[] projection = new String[]{
            Contract.Quote.COLUMN_SYMBOL,
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
            String price = dollarFormat.format(cursor.getDouble(cursor.getColumnIndex(projection[1])));
            String percentageChange = percentageFormat.format(cursor.getDouble(cursor.getColumnIndex(projection[2])));
            widgetItems.add(new StockQuoteWidgetItem(symbol, price, percentageChange));
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
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.list_item_quote);
        remoteViews.setTextViewText(R.id.symbol, widgetItems.get(position).symbol);
        remoteViews.setTextViewText(R.id.price, widgetItems.get(position).price);
        remoteViews.setTextViewText(R.id.change, widgetItems.get(position).percentageChange);
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
