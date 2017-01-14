package com.udacity.stockhawk.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.ui.fragment.StockListFragment;

public class MainActivity extends AppCompatActivity {
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
                    .commit();
        }
    }
}