package com.udacity.stockhawk.sync.event;

/**
 * Created by darshan on 14/2/17.
 */

public class DataUpdatedEvent {
    public long timeStamp = -1;

    public DataUpdatedEvent(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
