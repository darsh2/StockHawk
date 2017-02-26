package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.event.DataUpdatedEvent;
import com.udacity.stockhawk.sync.event.ErrorEvent;
import com.udacity.stockhawk.util.DebugLog;

import org.greenrobot.eventbus.EventBus;

import java.io.FileNotFoundException;
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

    /**
     * Interval within which the job should recur. Increasing
     * period to 15 minutes since {@link JobInfo#getMinPeriodMillis()}
     * has a minimum default of 15 minutes as the interval for
     * a periodic job.
     */
    private static final int PERIOD = 15 * 60 * 1000;

    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 2;

    /**
     * Used to inform event subscribers about the addition of a new
     * stock symbol. If a new stock symbol has been successfully
     * added, the app widget has to be updated.
     */
    private static boolean isNewSymbolAdded = false;

    private QuoteSyncJob() {
    }

    static void getQuotes(Context context) {
        DebugLog.logMethod();

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        String currentSymbol = null;
        try {
            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            DebugLog.logMessage(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }

            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            DebugLog.logMessage(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>(stockArray.length);
            ArrayList<ContentValues> keyStatsCVs = new ArrayList<>(stockArray.length);

            while (iterator.hasNext()) {
                String symbol = iterator.next();
                currentSymbol = symbol;

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

            EventBus.getDefault().post(new DataUpdatedEvent(System.currentTimeMillis(), isNewSymbolAdded));

        } catch (FileNotFoundException fileNotFoundException) {
            /*
            Earlier adding an invalid stock symbol threw NullPointerException
            but now it throws FileNotFoundException.
             */
            DebugLog.logMessage("Stock symbol not found");
            fileNotFoundException.printStackTrace();
            EventBus.getDefault().post(new ErrorEvent(ErrorEvent.SYMBOL_NOT_FOUND_ERROR, currentSymbol));

        } catch (IOException ioException) {
            DebugLog.logMessage("IOException: Error fetching stock quotes");
            ioException.printStackTrace();
            EventBus.getDefault().post(new ErrorEvent(ErrorEvent.NETWORK_ERROR, null));
        }

        // Reset flag
        isNewSymbolAdded = false;
    }

    private static void schedulePeriodic(Context context) {
        DebugLog.logMethod();
        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));
        /*
        Setting a linear back off policy because the periodic job of
        fetching stock quotes happens once every fifteen minutes. Using
        an exponential back off policy is would imply long wait periods
        where the stock quotes are not updated even though internet
        connectivity may have been restored.
         */
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(builder.build());
    }

    public static synchronized void initialize(final Context context) {
        schedulePeriodic(context);
        syncImmediately(context, false);
    }

    public static synchronized void syncImmediately(Context context, boolean isNewSymbol) {
        DebugLog.logMethod();
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
            isNewSymbolAdded = isNewSymbol;

        } else {
            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.schedule(builder.build());
        }
    }
}
