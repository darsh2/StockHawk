package com.udacity.stockhawk.ui.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.util.Constants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by darshan on 17/1/17.
 */

public class StockDetailFragment extends Fragment {
    public static final String TAG = StockDetailFragment.class.getName();

    private String stockSymbol;
    private String stockName;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.text_view_stock_name)
    TextView textViewStockName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate");
        if (getActivity() == null
                || getActivity().getIntent() == null) {
            return;
        }

        Intent intent = getActivity().getIntent();
        stockSymbol = intent.getStringExtra(Constants.INTENT_EXTRA_STOCK_SYMBOL);
        stockName = intent.getStringExtra(Constants.INTENT_EXTRA_STOCK_NAME);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Timber.d("onCreateView");
        View view = inflater.inflate(R.layout.fragment_stock_detail, container, false);
        ButterKnife.bind(this, view);

        toolbar.setTitle(stockSymbol);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        Timber.d("Toolbar margin start: " + toolbar.getTitleMarginStart());
        textViewStockName.setPadding(toolbar.getTitleMarginStart(), 0, 0, 0);
        textViewStockName.setText(stockName);

        loadHistoricalStockQuotes();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposableSingleObserver.dispose();
        if (disposableSingleObserver.isDisposed()) {
            Timber.d("isDisposed");
        }
        Timber.d("onDestroyView");
    }

    private DisposableSingleObserver<ArrayList<Entry>> disposableSingleObserver;

    private void loadHistoricalStockQuotes() {
        Single<ArrayList<Entry>> stockQuotesSingle = Single.fromCallable(new Callable<ArrayList<Entry>>() {
            @Override
            public ArrayList<Entry> call() throws Exception {
                return retrieveStockQuotesFromDB();
            }
        });
        stockQuotesSingle
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
        disposableSingleObserver = stockQuotesSingle
                .subscribeWith(new DisposableSingleObserver<ArrayList<Entry>>() {
                    @Override
                    public void onSuccess(ArrayList<Entry> value) {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });
    }

    private ArrayList<Entry> retrieveStockQuotesFromDB() {
        Cursor cursor = getContext().getContentResolver()
                .query(
                        Contract.Quote.makeUriForStock(stockSymbol),
                        new String[]{ Contract.Quote.COLUMN_HISTORY },
                        null,
                        null,
                        null
                );
        if (cursor == null) {
            throw new NullPointerException();
        }
        String history = "";
        if (cursor.moveToNext()) {
            history = cursor.getString(cursor.getColumnIndex(Contract.Quote.COLUMN_HISTORY));
        }
        cursor.close();

        String[] historicalQuotes = history.split("\n");
        int numHistoricalQuotes = historicalQuotes.length;
        ArrayList<Entry> stockQuotes = new ArrayList<>(numHistoricalQuotes);
        for (int i = 0, l = historicalQuotes.length; i < l; i++) {
            String[] entry = historicalQuotes[i].split(", ");
            stockQuotes.add(new Entry(Float.parseFloat(entry[0]), (new BigDecimal(entry[1])).floatValue()));
            Timber.d(stockQuotes.get(i).getX() + ", " + stockQuotes.get(i).getY());
        }
        return stockQuotes;
    }
}
