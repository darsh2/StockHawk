package com.udacity.stockhawk.sync;

import android.app.IntentService;
import android.content.Intent;

import com.udacity.stockhawk.util.DebugLog;

public class QuoteIntentService extends IntentService {
    public QuoteIntentService() {
        super(QuoteIntentService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DebugLog.logMethod();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        DebugLog.logMethod();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DebugLog.logMethod();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        DebugLog.logMethod();
        QuoteSyncJob.getQuotes(getApplicationContext());
    }
}
