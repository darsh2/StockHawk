package com.udacity.stockhawk;

import android.app.Application;

import java.util.logging.Level;

import timber.log.Timber;
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

        if (BuildConfig.DEBUG) {
            Timber.uprootAll();
            Timber.plant(new Timber.DebugTree());
        }
    }
}
