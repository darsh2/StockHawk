package com.udacity.stockhawk.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.ui.fragment.StockListFragment;
import com.udacity.stockhawk.util.Constants;
import com.udacity.stockhawk.util.DebugLog;

public class StockListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_list);

        DebugLog.logMethod();

        StockListFragment stockListFragment = null;
        if (getSupportFragmentManager() != null) {
            stockListFragment = (StockListFragment) getSupportFragmentManager().findFragmentByTag(StockListFragment.TAG);
        }

        if (stockListFragment == null) {
            stockListFragment = new StockListFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container_stocks_list, stockListFragment, StockListFragment.TAG)
                    .commit();
        }
    }

    public void onStockClick(String symbol, String name, float price) {
        Bundle extras = new Bundle();
        extras.putString(Constants.BUNDLE_STOCK_SYMBOL, symbol);
        extras.putString(Constants.BUNDLE_STOCK_NAME, name);
        extras.putString(Constants.BUNDLE_STOCK_PRICE, String.valueOf(price));

        Intent intent = new Intent(this, StockDetailActivity.class);
        intent.putExtras(extras);
        startActivity(intent);
    }
}