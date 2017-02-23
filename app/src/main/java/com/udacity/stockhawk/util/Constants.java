package com.udacity.stockhawk.util;

import com.udacity.stockhawk.data.Contract;

/**
 * Created by darshan on 15/1/17.
 */

public class Constants {
    public static final String INTENT_EXTRA_STOCK_SYMBOL = "StockSymbol";
    public static final String INTENT_EXTRA_STOCK_NAME = "StockName";
    public static final String INTENT_EXTRA_STOCK_PRICE = "StockPrice";

    public static final String BUNDLE_STOCK_NAME = "StockName";
    public static final String BUNDLE_STOCK_SYMBOL = "StockSymbol";
    public static final String BUNDLE_STOCK_PRICE = "StockPrice";
    public static final String BUNDLE_STOCK_QUOTE_DATES = "Dates";
    public static final String BUNDLE_STOCK_QUOTES = "StockQuotes";
    public static final String BUNDLE_TAB_SELECTED_INDEX = "TabSelectedIndex";
    public static final String BUNDLE_STOCK_QUOTES_SINCE = "StockQuotesSince";
    public static final String BUNDLE_STOCK_KEY_STATS = "StockKeyStats";

    public static final String[] KEY_STATS_COLUMN_NAMES = {
            Contract.KeyStats.COLUMN_DAY_LOW,
            Contract.KeyStats.COLUMN_DAY_HIGH,
            Contract.KeyStats.COLUMN_OPEN,
            Contract.KeyStats.COLUMN_PREV_CLOSE,
            Contract.KeyStats.COLUMN_VOLUME,
            Contract.KeyStats.COLUMN_MARKET_CAP,
            Contract.KeyStats.COLUMN_YEAR_LOW,
            Contract.KeyStats.COLUMN_YEAR_HIGH
    };
    public static final int POSITION_DAY_LOW = 0;
    public static final int POSITION_DAY_HIGH = 1;
    public static final int POSITION_OPEN = 2;
    public static final int POSITION_PREV_CLOSE = 3;
    public static final int POSITION_VOLUME = 4;
    public static final int POSITION_MARKET_CAP = 5;
    public static final int POSITION_YEAR_LOW = 6;
    public static final int POSITION_YEAR_HIGH = 7;

    public static final float ONE_MILLION = 1000000f;
    public static final float ONE_BILLION = 1000000000f;
}
