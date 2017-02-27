package com.udacity.stockhawk.util;

import android.util.Log;

import com.udacity.stockhawk.BuildConfig;

/**
 * <p>Created by darshan on 12/2/17.
 *
 * <p>Custom logger class to ease logging. It logs the method it is called
 * in via the {@link StackTraceElement} and also provides a helper method
 * to log any message.
 *
 * <p>The primary reason for creating this logger is for the tag generation
 * based on the class it is called in. Helps debugging as one knows which
 * class the code has reached by just looking at the tag. See {@link #getTag(String)}
 * for more information on how tags are generated from class name.
 */

public class DebugLog {
    private static final boolean isDebugBuild = BuildConfig.DEBUG;
    private static boolean loggingEnabled = true;

    private static final int requiredMethodIndex = 3;

    private static boolean isLoggingEnabled() {
        return isDebugBuild && loggingEnabled;
    }

    public static void setLoggingEnabled(boolean loggingEnabled) {
        DebugLog.loggingEnabled = loggingEnabled;
    }

    /**
     * Creates log entry with message being the method name it
     * was called from.
     */
    public static void logMethod() {
        if (!isLoggingEnabled()) {
            return;
        }
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String className = stackTraceElements[requiredMethodIndex].getClassName();
        String methodName = stackTraceElements[requiredMethodIndex].getMethodName();
        Log.i(getTag(className), methodName);
    }

    /**
     * Log the particular message.
     */
    public static void logMessage(String message) {
        if (!isLoggingEnabled()) {
            return;
        }
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String className = stackTraceElements[requiredMethodIndex].getClassName();
        Log.i(getTag(className), message);
    }

    /**
     * Returns tag based on tag name. Tag is of the following format:
     * "DL-" + EveryUpperCaseCharacterInClassName
     * ex: If className is {@link com.udacity.stockhawk.sync.event.DataUpdatedEvent},
     * then tag = "DL-DUE" (quotes for clarity). If className is
     * {@link com.udacity.stockhawk.ui.activity.StockListActivity} then tag = "DL-SLA"
     * (quotes for clarity).
     */
    private static String getTag(String className) {
        StringBuilder tag = new StringBuilder("DL-");
        for (char e : className.toCharArray()) {
            if (Character.isUpperCase(e)) {
                tag.append(e);
            }
        }
        return tag.toString();
    }
}
