package com.udacity.stockhawk.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.ui.fragment.StockDetailFragment;

import timber.log.Timber;

/**
 * Created by darshan on 17/1/17.
 */

public class StockDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        Timber.d("onCreate");

        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }

        StockDetailFragment stockDetailFragment = null;
        if (getSupportFragmentManager() != null) {
            stockDetailFragment = (StockDetailFragment) getSupportFragmentManager().findFragmentByTag(StockDetailFragment.TAG);
        }

        if (stockDetailFragment == null) {
            stockDetailFragment = new StockDetailFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, stockDetailFragment, StockDetailFragment.TAG)
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.d("onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Timber.d("onStop");
    }
}
