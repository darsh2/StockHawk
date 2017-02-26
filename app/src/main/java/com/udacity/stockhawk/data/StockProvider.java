package com.udacity.stockhawk.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.udacity.stockhawk.util.DebugLog;

public class StockProvider extends ContentProvider {

    static final int QUOTE = 100;
    static final int QUOTE_FOR_SYMBOL = 101;

    static final int KEY_STATS = 102;
    static final int KEY_STATS_FOR_SYMBOL = 103;

    public static UriMatcher uriMatcher = buildUriMatcher();

    private DbHelper dbHelper;

    static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(Contract.AUTHORITY, Contract.PATH_QUOTE, QUOTE);
        matcher.addURI(Contract.AUTHORITY, Contract.PATH_QUOTE_WITH_SYMBOL, QUOTE_FOR_SYMBOL);

        matcher.addURI(Contract.AUTHORITY, Contract.PATH_KEY_STATS, KEY_STATS);
        matcher.addURI(Contract.AUTHORITY, Contract.PATH_KEY_STATS_WITH_SYMBOL, KEY_STATS_FOR_SYMBOL);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        DebugLog.logMethod();
        DebugLog.logMessage("Uri " + uri.toString());

        Cursor returnCursor;
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        switch (uriMatcher.match(uri)) {
            case QUOTE: {
                DebugLog.logMessage("QUOTE");
                returnCursor = db.query(
                        Contract.Quote.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            case QUOTE_FOR_SYMBOL: {
                DebugLog.logMessage("QUOTE_FOR_SYMBOL");
                returnCursor = db.query(
                        Contract.Quote.TABLE_NAME,
                        projection,
                        Contract.Quote.COLUMN_SYMBOL + " = ?",
                        new String[]{ Contract.Quote.getStockFromUri(uri) },
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            case KEY_STATS: {
                DebugLog.logMessage("KEY_STATS");
                returnCursor = db.query(
                        Contract.KeyStats.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            case KEY_STATS_FOR_SYMBOL: {
                DebugLog.logMessage("KEY_STATS_FOR_SYMBOL");
                returnCursor = db.query(
                        Contract.KeyStats.TABLE_NAME,
                        projection,
                        Contract.KeyStats.COLUMN_SYMBOL + " = ?",
                        new String[]{ Contract.KeyStats.getStockFromUri(uri) },
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default: {
                throw new UnsupportedOperationException("Unknown URI:" + uri);
            }
        }

        returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return returnCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        DebugLog.logMethod();
        DebugLog.logMessage("Uri " + uri.toString());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Uri returnUri;

        switch (uriMatcher.match(uri)) {
            case QUOTE: {
                DebugLog.logMessage("QUOTE");
                db.insert(
                        Contract.Quote.TABLE_NAME,
                        null,
                        values
                );
                returnUri = Contract.Quote.URI;
                break;
            }

            case KEY_STATS: {
                DebugLog.logMessage("KEY_STATS");
                db.insert(
                        Contract.KeyStats.TABLE_NAME,
                        null,
                        values
                );
                returnUri = Contract.KeyStats.URI;
                break;
            }

            default: {
                throw new UnsupportedOperationException("Unknown URI:" + uri);
            }
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        DebugLog.logMethod();
        DebugLog.logMessage("Uri " + uri.toString());
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted;

        if (null == selection) {
            selection = "1";
        }
        switch (uriMatcher.match(uri)) {
            case QUOTE: {
                DebugLog.logMessage("QUOTE");
                rowsDeleted = db.delete(
                        Contract.Quote.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            }

            case QUOTE_FOR_SYMBOL: {
                DebugLog.logMessage("QUOTE_FOR_SYMBOL");
                String symbol = Contract.Quote.getStockFromUri(uri);
                rowsDeleted = db.delete(
                        Contract.Quote.TABLE_NAME,
                        '"' + symbol + '"' + " =" + Contract.Quote.COLUMN_SYMBOL,
                        selectionArgs
                );
                break;
            }

            case KEY_STATS: {
                DebugLog.logMessage("KEY_STATS");
                rowsDeleted = db.delete(
                        Contract.KeyStats.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            }

            case KEY_STATS_FOR_SYMBOL: {
                DebugLog.logMessage("KEY_STATS_FOR_SYMBOL");
                String symbol = Contract.KeyStats.getStockFromUri(uri);
                rowsDeleted = db.delete(
                        Contract.KeyStats.TABLE_NAME,
                        Contract.KeyStats.COLUMN_SYMBOL + " = " + '"' + symbol + '"',
                        selectionArgs
                );
                break;
            }

            default: {
                throw new UnsupportedOperationException("Unknown URI:" + uri);
            }
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        DebugLog.logMethod();
        DebugLog.logMessage("Uri " + uri.toString());
        final SQLiteDatabase db = dbHelper.getWritableDatabase();

        switch (uriMatcher.match(uri)) {
            case QUOTE: {
                DebugLog.logMessage("QUOTE");
                db.beginTransaction();
                int returnCount = 0;
                try {
                    long rowId;
                    for (int i = 0, l = values.length; i < l; i++) {
                        rowId = db.insert(
                                Contract.Quote.TABLE_NAME,
                                null,
                                values[i]
                        );
                        if (rowId != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return returnCount;
            }

            case KEY_STATS: {
                DebugLog.logMessage("KEY_STATS");
                db.beginTransaction();
                int returnCount = 0;
                try {
                    long rowId = -1;
                    for (int i = 0, l = values.length; i < l; i++) {
                        rowId = db.insert(
                                Contract.KeyStats.TABLE_NAME,
                                null,
                                values[i]
                        );
                        if (rowId != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return returnCount;
            }

            default: {
                return super.bulkInsert(uri, values);
            }
        }
    }

    @Override
    public void shutdown() {
        DebugLog.logMethod();
        dbHelper.close();
        super.shutdown();
    }
}
