package com.udacity.stockhawk.widget;

/**
 * Created by darshan on 12/2/17.
 */

class StockQuoteWidgetItem {
    String symbol;
    String price;
    String percentageChange;

    StockQuoteWidgetItem(String symbol, String price, String percentageChange) {
        this.symbol = symbol;
        this.price = price;
        this.percentageChange = percentageChange;
    }
}
