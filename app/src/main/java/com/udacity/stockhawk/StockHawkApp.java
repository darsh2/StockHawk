package com.udacity.stockhawk;

import android.app.Application;

import com.udacity.stockhawk.util.DebugLog;

import java.util.logging.Level;

import yahoofinance.YahooFinance;

public class StockHawkApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        /*
        Turn off logging to prevent numerous log entries being
        added each time data is fetched from Yahoo Finance API.
         */
        YahooFinance.logger.setLevel(Level.OFF);

        // Turn off logging
        DebugLog.setLoggingEnabled(false);
    }
}
