package com.udacity.stockhawk.sync;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.util.Log;

public class QuoteJobService extends JobService {
    private static final String tag = "DL-QJS";
    private static final boolean DEBUG = true;
    private static void log(String message) {
        if (DEBUG) {
            Log.i(tag, message);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log("onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        log("onStartJob");
        Intent nowIntent = new Intent(getApplicationContext(), QuoteIntentService.class);
        getApplicationContext().startService(nowIntent);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        log("onStopJob");
        return false;
    }
}
