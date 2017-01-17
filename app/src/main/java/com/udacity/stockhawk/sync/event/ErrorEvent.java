package com.udacity.stockhawk.sync.event;

import android.support.annotation.IntDef;

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

    public ErrorEvent(int errorCode) {
        this.errorCode = errorCode;
    }

    @ErrorCode
    public int getCode() {
        return errorCode;
    }
}
