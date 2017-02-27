package com.udacity.stockhawk.data;

import android.net.Uri;
import android.provider.BaseColumns;

public final class Contract {
    static final String AUTHORITY = "com.udacity.stockhawk";

    static final String PATH_QUOTE = "quote";
    static final String PATH_QUOTE_WITH_SYMBOL = "quote/*";

    static final String PATH_KEY_STATS = "key_stats";
    static final String PATH_KEY_STATS_WITH_SYMBOL = "key_stats/*";

    private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    private Contract() {
    }

    public static final class Quote implements BaseColumns {
        public static final Uri URI = BASE_URI.buildUpon().appendPath(PATH_QUOTE).build();

        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_PRICE = "price";
        public static final String COLUMN_ABSOLUTE_CHANGE = "absolute_change";
        public static final String COLUMN_PERCENTAGE_CHANGE = "percentage_change";
        public static final String COLUMN_HISTORY = "history";

        static final String TABLE_NAME = "quotes";

        public static Uri makeUriForStock(String symbol) {
            return URI.buildUpon().appendPath(symbol).build();
        }

        static String getStockFromUri(Uri queryUri) {
            return queryUri.getLastPathSegment();
        }
    }

    public static final class KeyStats implements BaseColumns {
        public static final Uri URI = BASE_URI.buildUpon().appendPath(PATH_KEY_STATS).build();

        static final String TABLE_NAME = "key_stats";

        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_DAY_LOW = "day_low";
        public static final String COLUMN_DAY_HIGH = "day_high";
        public static final String COLUMN_OPEN = "open";
        public static final String COLUMN_PREV_CLOSE = "prev_close";
        public static final String COLUMN_VOLUME = "volume";
        public static final String COLUMN_MARKET_CAP = "market_cap";
        public static final String COLUMN_YEAR_LOW = "year_low";
        public static final String COLUMN_YEAR_HIGH = "year_high";

        public static Uri makeUriForStockKeyStats(String symbol) {
            return URI.buildUpon().appendPath(symbol).build();
        }

        static String getStockFromUri(Uri queryUri) {
            return queryUri.getLastPathSegment();
        }
    }
}
