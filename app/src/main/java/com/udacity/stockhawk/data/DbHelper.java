package com.udacity.stockhawk.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.udacity.stockhawk.data.Contract.KeyStats;
import com.udacity.stockhawk.data.Contract.Quote;

class DbHelper extends SQLiteOpenHelper {
    static final String NAME = "StockHawk.db";
    private static final int VERSION = 2;

    DbHelper(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String quoteTableBuilder = "CREATE TABLE " + Quote.TABLE_NAME + " ("
                + Quote._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Quote.COLUMN_SYMBOL + " TEXT NOT NULL, "
                + Quote.COLUMN_NAME + " TEXT NOT NULL, "
                + Quote.COLUMN_PRICE + " REAL NOT NULL, "
                + Quote.COLUMN_ABSOLUTE_CHANGE + " REAL NOT NULL, "
                + Quote.COLUMN_PERCENTAGE_CHANGE + " REAL NOT NULL, "
                + Quote.COLUMN_HISTORY + " TEXT NOT NULL, "
                + "UNIQUE (" + Quote.COLUMN_SYMBOL + ") ON CONFLICT REPLACE);";
        db.execSQL(quoteTableBuilder);

        String keyStatsTableBuilder = "CREATE TABLE " + KeyStats.TABLE_NAME + " ("
                + KeyStats._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KeyStats.COLUMN_SYMBOL + " VARCHAR(10) NOT NULL, "
                + KeyStats.COLUMN_DAY_LOW + " REAL, "
                + KeyStats.COLUMN_DAY_HIGH + " REAL, "
                + KeyStats.COLUMN_OPEN + " REAL, "
                + KeyStats.COLUMN_PREV_CLOSE + " REAL, "
                + KeyStats.COLUMN_VOLUME + " REAL, "
                + KeyStats.COLUMN_MARKET_CAP + " REAL, "
                + KeyStats.COLUMN_YEAR_LOW + " REAL, "
                + KeyStats.COLUMN_YEAR_HIGH + " REAL, "
                + "UNIQUE (" + KeyStats.COLUMN_SYMBOL + ") ON CONFLICT REPLACE);";
        db.execSQL(keyStatsTableBuilder);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Quote.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + KeyStats.TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Quote.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + KeyStats.TABLE_NAME);
        onCreate(db);
    }
}
