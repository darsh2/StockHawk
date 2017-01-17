package com.udacity.stockhawk.sync.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by darshan on 15/1/17.
 */

public class StockSymbolNotFoundReceiver extends BroadcastReceiver {
    private StockSymbolNotFoundListener listener;

    public interface StockSymbolNotFoundListener {
        void onStockSymbolNotFound();
    }

    public StockSymbolNotFoundReceiver(StockSymbolNotFoundListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (listener != null) {
            listener.onStockSymbolNotFound();
        }
    }
}
