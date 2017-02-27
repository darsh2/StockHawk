package com.udacity.stockhawk.sync.event;

/**
 * Created by darshan on 14/2/17.
 */

public class DataUpdatedEvent {
    private long timeStamp = -1;
    private boolean isNewSymbolAdded = false;

    public DataUpdatedEvent(long timeStamp, boolean isNewSymbolAdded) {
        this.timeStamp = timeStamp;
        this.isNewSymbolAdded = isNewSymbolAdded;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public boolean getIsNewSymbolAdded() {
        return isNewSymbolAdded;
    }
}
