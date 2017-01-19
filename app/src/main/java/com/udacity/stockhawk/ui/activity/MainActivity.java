package com.udacity.stockhawk.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.ui.fragment.StockListFragment;
import com.udacity.stockhawk.util.Constants;

public class MainActivity extends AppCompatActivity implements
        StockListFragment.OnStockClickListener {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StockListFragment stockListFragment = null;
        if (getSupportFragmentManager() != null) {
            stockListFragment = (StockListFragment) getSupportFragmentManager().findFragmentByTag(StockListFragment.TAG);
        }

        if (stockListFragment == null) {
            stockListFragment = new StockListFragment();
            stockListFragment.setHasOptionsMenu(true);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container_stocks_list, stockListFragment, StockListFragment.TAG)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onStockClick(String symbol, String name) {
        Intent intent = new Intent(this, StockDetailActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_STOCK_SYMBOL, symbol);
        intent.putExtra(Constants.INTENT_EXTRA_STOCK_NAME, name);
        startActivity(intent);
    }
}