package com.udacity.stockhawk.ui.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.util.Constants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
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

    @BindView(R.id.line_chart)
    LineChart lineChart;

    private DisposableSingleObserver<ArrayList<Entry>> disposableSingleObserver;

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

        initChartView();

        return view;
    }

    private void initChartView() {
        Timber.d("initChartView");
        lineChart.setBackgroundColor(Color.WHITE);

        lineChart.getDescription().setEnabled(false);

        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        lineChart.setDrawGridBackground(true);
        lineChart.setMaxHighlightDistance(300);

        XAxis x = lineChart.getXAxis();
        x.setEnabled(false);

        YAxis y = lineChart.getAxisLeft();
        y.setLabelCount(6, false);
        y.setDrawGridLines(true);
        y.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        y.setAxisLineColor(Color.BLACK);
        y.setTextColor(Color.BLACK);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);

        loadHistoricalStockQuotes();
    }

    private void drawGraph(ArrayList<Entry> arrayList) {
        LineDataSet lineDataSet = new LineDataSet(arrayList, "Historical Stock Quotes");

        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setCubicIntensity(0.2f);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(1.8f);
        lineDataSet.setColor(Color.BLUE);
        lineDataSet.setFillColor(Color.BLACK);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setDrawHighlightIndicators(true);
        lineDataSet.setHighLightColor(Color.rgb(244, 117, 117));

        LineData data = new LineData(lineDataSet);
        data.setValueTextSize(9f);
        data.setDrawValues(false);

        lineChart.setData(data);
        lineChart.invalidate();
    }

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
                        drawGraph(value);
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
        for (int i = numHistoricalQuotes - 1, x = 0; i >= 0; i--) {
            String[] entry = historicalQuotes[i].split(", ");
            stockQuotes.add(new Entry(x++, (new BigDecimal(entry[1])).floatValue()));
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(Long.parseLong(entry[0]));
            Timber.d(calendar.getTime().toString(), entry[1]);
        }
        return stockQuotes;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Timber.d("onDestroyView");
        disposableSingleObserver.dispose();
        if (disposableSingleObserver.isDisposed()) {
            Timber.d("isDisposed");
        }
    }
}
