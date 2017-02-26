package com.udacity.stockhawk.sync.event;

/**
 * Created by darshan on 14/2/17.
 */

public class DataUpdatedEvent {
    public long timeStamp = -1;
    public boolean isNewSymbolAdded = false;

    public DataUpdatedEvent(long timeStamp, boolean isNewSymbolAdded) {
        this.timeStamp = timeStamp;
        this.isNewSymbolAdded = isNewSymbolAdded;
    }
}
