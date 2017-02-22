package com.udacity.stockhawk.sync.event;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by darshan on 17/1/17.
 */

public class ErrorEvent {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ NETWORK_ERROR, SYMBOL_NOT_FOUND_ERROR })
    public @interface ErrorCode {}

    public static final int NETWORK_ERROR = 100;
    public static final int SYMBOL_NOT_FOUND_ERROR = 101;

    private final int errorCode;
    private final String symbol;

    public ErrorEvent(int errorCode, String symbol) {
        this.errorCode = errorCode;
        this.symbol = symbol;
    }

    @ErrorCode
    public int getCode() {
        return errorCode;
    }

    public @Nullable String getSymbol() {
        return symbol;
    }
}
