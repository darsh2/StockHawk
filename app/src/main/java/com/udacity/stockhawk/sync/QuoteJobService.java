package com.udacity.stockhawk.sync;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

import com.udacity.stockhawk.util.DebugLog;

public class QuoteJobService extends JobService {
    @Override
    public void onCreate() {
        super.onCreate();
        DebugLog.logMethod();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DebugLog.logMethod();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        DebugLog.logMethod();
        DebugLog.logMessage("Job id: " + jobParameters.getJobId());
        Intent nowIntent = new Intent(getApplicationContext(), QuoteIntentService.class);
        getApplicationContext().startService(nowIntent);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        DebugLog.logMethod();
        DebugLog.logMessage("Job id: " + jobParameters.getJobId());
        /*
        Return true to indicate to the JobManager that the job
        should be rescheduled based on the retry criteria provided
        at the time of job creation. Required especially when
        scheduling an one off job to sync stock quotes immediately.
        For instance, consider the scenario where internet access is
        disrupted while fetching stock quotes. Hence onStopJob is
        called before the job can finish itself. In this scenario, it
        would be preferred if the job retries according to the retry
        criteria provided so that the stock quotes will be automatically
        refreshed rather than the user having to explicitly refresh it.
         */
        return true;
    }
}
