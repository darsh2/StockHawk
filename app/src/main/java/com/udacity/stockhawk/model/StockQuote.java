package com.udacity.stockhawk.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by darshan on 13/2/17.
 */

public class StockQuote implements Parcelable {
    public String symbol;
    public String name;
    public float price;
    public float absoluteChange;
    public float percentageChange;

    public StockQuote(String symbol, String name, float price,
                      float absoluteChange, float percentageChange) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.absoluteChange = absoluteChange;
        this.percentageChange = percentageChange;
    }

    public StockQuote(Parcel source) {
        symbol = source.readString();
        name = source.readString();
        price = source.readFloat();
        absoluteChange = source.readFloat();
        percentageChange = source.readFloat();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(symbol);
        dest.writeString(name);
        dest.writeFloat(price);
        dest.writeFloat(absoluteChange);
        dest.writeFloat(percentageChange);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new StockQuote(source);
        }

        @Override
        public Object[] newArray(int size) {
            return new StockQuote[0];
        }
    };
}
