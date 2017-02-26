package com.udacity.stockhawk.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.util.DebugLog;

/**
 * Created by darshan on 11/2/17.
 */

public class StockQuoteWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        DebugLog.logMethod();
        return new StockQuoteRemoteViewsFactory(getApplicationContext());
    }
}
