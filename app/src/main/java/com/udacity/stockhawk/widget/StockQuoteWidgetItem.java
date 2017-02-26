package com.udacity.stockhawk.widget;

/**
 * Created by darshan on 12/2/17.
 */

class StockQuoteWidgetItem {
    String symbol;
    String name;
    String price;
    String percentageChange;

    StockQuoteWidgetItem(String symbol, String name, String price, String percentageChange) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.percentageChange = percentageChange;
    }
}
