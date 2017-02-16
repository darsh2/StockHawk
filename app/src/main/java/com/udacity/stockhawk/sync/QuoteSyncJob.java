package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.event.DataUpdatedEvent;
import com.udacity.stockhawk.sync.event.ErrorEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

public final class QuoteSyncJob {
    private static final int ONE_OFF_ID = 2;
    private static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    private static final int PERIOD = 3000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 2;

    private QuoteSyncJob() {
    }

    static void getQuotes(Context context) {
        log("Running sync job");

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        try {
            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            log(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }

            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            log(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>(stockArray.length);
            ArrayList<ContentValues> keyStatsCVs = new ArrayList<>(stockArray.length);

            while (iterator.hasNext()) {
                String symbol = iterator.next();

                Stock stock = quotes.get(symbol);
                StockQuote quote = stock.getQuote();

                // WARNING! Don't request historical data for a stock that doesn't exist!
                // The request will hang forever X_x
                List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);
                StringBuilder historyBuilder = new StringBuilder();
                for (HistoricalQuote it : history) {
                    historyBuilder.append(it.getDate().getTimeInMillis());
                    historyBuilder.append(", ");
                    historyBuilder.append(it.getClose());
                    historyBuilder.append("\n");
                }

                ContentValues quoteCV = new ContentValues();
                quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                quoteCV.put(Contract.Quote.COLUMN_NAME, stock.getName());
                quoteCV.put(Contract.Quote.COLUMN_PRICE, quote.getPrice().floatValue());
                quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, quote.getChange().floatValue());
                quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, quote.getChangeInPercent().floatValue());
                quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());
                quoteCVs.add(quoteCV);

                ContentValues keyStatsCV = new ContentValues();
                keyStatsCV.put(Contract.KeyStats.COLUMN_SYMBOL, symbol);
                keyStatsCV.put(Contract.KeyStats.COLUMN_DAY_LOW, quote.getDayLow().floatValue());
                keyStatsCV.put(Contract.KeyStats.COLUMN_DAY_HIGH, quote.getDayHigh().floatValue());
                keyStatsCV.put(Contract.KeyStats.COLUMN_OPEN, quote.getOpen().floatValue());
                keyStatsCV.put(Contract.KeyStats.COLUMN_PREV_CLOSE, quote.getPreviousClose().floatValue());
                keyStatsCV.put(Contract.KeyStats.COLUMN_VOLUME, quote.getVolume().floatValue());
                keyStatsCV.put(Contract.KeyStats.COLUMN_MARKET_CAP, stock.getStats().getMarketCap().floatValue());
                keyStatsCV.put(Contract.KeyStats.COLUMN_YEAR_LOW, quote.getYearLow().floatValue());
                keyStatsCV.put(Contract.KeyStats.COLUMN_YEAR_HIGH, quote.getYearHigh().floatValue());
                keyStatsCVs.add(keyStatsCV);
            }

            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()])
                    );

            context.getContentResolver()
                    .bulkInsert(
                            Contract.KeyStats.URI,
                            keyStatsCVs.toArray(new ContentValues[keyStatsCVs.size()])
                    );

            log("Process: " + android.os.Process.myPid());
            EventBus.getDefault().post(new DataUpdatedEvent(System.currentTimeMillis()));
            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
            context.sendBroadcast(dataUpdatedIntent);

        } catch (IOException ioException) {
            log("Error fetching stock quotes");
            ioException.printStackTrace();
            EventBus.getDefault().post(new ErrorEvent(ErrorEvent.NETWORK_ERROR));

        } catch (NullPointerException nullPointerException) {
            log("Stock symbol not found");
            nullPointerException.printStackTrace();
            EventBus.getDefault().post(new ErrorEvent(ErrorEvent.SYMBOL_NOT_FOUND_ERROR));
        }
    }

    private static void schedulePeriodic(Context context) {
        log("Scheduling a periodic task");

        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(builder.build());
    }


    public static synchronized void initialize(final Context context) {
        schedulePeriodic(context);
        syncImmediately(context);
    }

    public static synchronized void syncImmediately(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);

        } else {
            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.schedule(builder.build());
        }
    }

    private static final String tag = "DL-QSJ";
    private static final boolean DEBUG = true;
    private static void log(String message) {
        if (DEBUG) {
            Log.i(tag, message);
        }
    }
}
