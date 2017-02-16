package com.udacity.stockhawk.sync;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class QuoteIntentService extends IntentService {
    private static final String tag = "DL-QIS";
    private static final boolean DEBUG = true;
    private static void log(String message) {
        if (DEBUG) {
            Log.i(tag, message);
        }
    }

    public QuoteIntentService() {
        super(QuoteIntentService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log("onCreate");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        log("onStart");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        log("onHandleIntent");
        QuoteSyncJob.getQuotes(getApplicationContext());
    }
}
