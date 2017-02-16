package com.udacity.stockhawk.widget;

import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

/**
 * Created by darshan on 11/2/17.
 */

public class StockQuoteWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Log.i("GS-SQWS", "onGetViewFactory");
        return new StockQuoteRemoteViewsFactory(getApplicationContext());
    }
}
