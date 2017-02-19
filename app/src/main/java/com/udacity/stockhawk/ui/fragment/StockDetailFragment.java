package com.udacity.stockhawk.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.util.Constants;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by darshan on 17/1/17.
 */

public class StockDetailFragment extends Fragment {
    public static final String TAG = StockDetailFragment.class.getName();

    private String stockSymbol;
    private String stockName;
    private String stockPrice;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    /*
    Stock symbol view
     */
    @BindView(R.id.text_view_stock_symbol)
    TextView textViewStockSymbol;

    @BindView(R.id.text_view_stock_price)
    TextView textViewStockPrice;

    @BindView(R.id.text_view_day_range)
    TextView textViewDayRange;

    /*
    Chart view
     */
    @BindView(R.id.line_chart)
    LineChart lineChart;

    @BindView(R.id.text_view_marker)
    TextView textViewMarker;

    private IMarker marker;

    /*
    Key Stats view
     */
    @BindView(R.id.text_view_open)
    TextView textViewOpen;

    @BindView(R.id.text_view_prev_close)
    TextView textViewPreviousClose;

    @BindView(R.id.text_view_volume)
    TextView textViewVolume;

    @BindView(R.id.text_view_market_cap)
    TextView textViewMarketCap;

    @BindView(R.id.text_view_year_low)
    TextView textViewYearLow;

    @BindView(R.id.text_view_year_high)
    TextView textViewYearHigh;

    private CompositeDisposable disposables;

    private ArrayList<Long> date;
    private ArrayList<Entry> stockQuotes;

    private ArrayList<String> stockKeyStats;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");
        if (getActivity() == null
                || getActivity().getIntent() == null) {
            return;
        }

        Intent intent = getActivity().getIntent();
        stockSymbol = intent.getStringExtra(Constants.INTENT_EXTRA_STOCK_SYMBOL);
        stockName = intent.getStringExtra(Constants.INTENT_EXTRA_STOCK_NAME);
        stockPrice = intent.getStringExtra(Constants.INTENT_EXTRA_STOCK_PRICE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        log("onCreateView");
        View view = inflater.inflate(R.layout.fragment_stock_detail, container, false);
        ButterKnife.bind(this, view);

        toolbar.setTitle(stockName);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        textViewStockSymbol.setText(stockSymbol);
        textViewStockPrice.setText(stockPrice);

        disposables = new CompositeDisposable();
        loadStockKeyStats();
        initChartView();

        return view;
    }

    private void initChartView() {
        log("initChartView");
        styleLineChart();
        styleDateAxis();
        styleStockCloseAxis();

        loadHistoricalStockQuotes();
    }

    private void styleLineChart() {
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setMaxHighlightDistance(300);

        lineChart.setBackgroundColor(Color.WHITE);
        /*
        Required to set lineChart's background color to the specified
        value. Ref: http://stackoverflow.com/a/32624619/3946664
         */

        lineChart.setDrawGridBackground(false);

        lineChart.setHighlightPerDragEnabled(false);

        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);

        marker = new CustomMarkerView(getContext(), R.layout.marker_view);
        lineChart.setMarker(marker);
    }

    private void styleDateAxis() {
        XAxis dateAxis = lineChart.getXAxis();
        dateAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        dateAxis.setLabelCount(4, false);
        dateAxis.setDrawGridLines(true);
        dateAxis.setAxisLineColor(Color.BLACK);
        dateAxis.setTextColor(Color.BLACK);
        dateAxis.setValueFormatter(new DateAxisValueFormatter());
    }

    private void styleStockCloseAxis() {
        YAxis stockCloseAxis = lineChart.getAxisLeft();
        stockCloseAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        stockCloseAxis.setLabelCount(4, false);
        stockCloseAxis.setDrawGridLines(true);
        stockCloseAxis.setAxisLineColor(Color.BLACK);
        stockCloseAxis.setTextColor(Color.BLACK);
        stockCloseAxis.setValueFormatter(new StockCloseAxisValueFormatter());
    }

    private class DateAxisValueFormatter implements IAxisValueFormatter {
        private SimpleDateFormat simpleDateFormat;

        DateAxisValueFormatter() {
            simpleDateFormat = new SimpleDateFormat("MMM yy", Locale.getDefault());
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return simpleDateFormat.format(date.get((int) value));
        }
    }

    private class StockCloseAxisValueFormatter implements IAxisValueFormatter {
        private DecimalFormat decimalFormat;

        StockCloseAxisValueFormatter() {
            decimalFormat = new DecimalFormat("####.##");
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            //log("Value: " + value);
            return decimalFormat.format(value) + "$";
        }
    }

    private void loadHistoricalStockQuotes() {
        Single<ArrayList<Entry>> stockQuotesSingle = Single.fromCallable(new Callable<ArrayList<Entry>>() {
            @Override
            public ArrayList<Entry> call() throws Exception {
                log("stockQuotesSingle - call");
                return retrieveStockQuotesFromDb();
            }
        });
        stockQuotesSingle
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
        DisposableSingleObserver<ArrayList<Entry>> disposableSingleObserver = stockQuotesSingle
                .subscribeWith(new DisposableSingleObserver<ArrayList<Entry>>() {
                    @Override
                    public void onSuccess(ArrayList<Entry> value) {
                        log("stockQuotesSingleObserver - onSuccess");
                        drawGraph(value);
                    }

                    @Override
                    public void onError(Throwable e) {
                        log("stockQuotesSingleObserver - onError");
                    }
                });
        disposables.add(disposableSingleObserver);
    }

    private ArrayList<Entry> retrieveStockQuotesFromDb() {
        log("retrieveStockQuotesFromDb");
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

        date = new ArrayList<>(numHistoricalQuotes);
        stockQuotes = new ArrayList<>(numHistoricalQuotes);

        // The stock quotes are in descending order of date.
        for (int i = numHistoricalQuotes - 1, entryX = 0; i >= 0; i--) {
            String[] entry = historicalQuotes[i].split(", ");
            stockQuotes.add(new Entry(entryX++, (new BigDecimal(entry[1])).floatValue()));
            date.add(Long.parseLong(entry[0]));
        }
        return stockQuotes;
    }

    private void drawGraph(ArrayList<Entry> arrayList) {
        log("drawGraph");
        LineDataSet lineDataSet = new LineDataSet(arrayList, "Historical Stock Quotes");

        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(1.8f);
        lineDataSet.setColor(Color.GREEN);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillColor(Color.GREEN);
        lineDataSet.setFillAlpha(100);
        lineDataSet.setDrawHighlightIndicators(true);
        lineDataSet.setHighLightColor(Color.rgb(244, 117, 117));

        LineData data = new LineData(lineDataSet);
        data.setValueTextSize(9f);
        data.setDrawValues(false);

        lineChart.setData(data);
        lineChart.invalidate();
    }

    private void loadStockKeyStats() {
        log("loadStockKeyStats");
        Single<Boolean> stockKeyStatsSingle = Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                log("stockKeyStatsSingle - call");
                return retrieveStockKeyStatsFromDb();
            }
        });
        stockKeyStatsSingle
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
        DisposableSingleObserver<Boolean> disposableSingleObserver = stockKeyStatsSingle
                .subscribeWith(new DisposableSingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(Boolean value) {
                        log("stockKeyStatsSingleObserver - onSuccess");
                        updateKeyStatsView();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log("stockKeyStatsSingleObserver - onError");
                        log(e.toString() + "\n\n" + e.getMessage());
                        e.printStackTrace();
                    }
                });
        disposables.add(disposableSingleObserver);
    }

    private static final String tag = "CL-SDF";
    private static final boolean DEBUG = true;
    private static final void log(String message) {
        if (DEBUG) {
            Log.i(tag, message);
        }
    }

    private boolean retrieveStockKeyStatsFromDb() {
        log("retrieveStockKeyStatsFromDb");
        Cursor cursor = getContext().getContentResolver()
                .query(
                        Contract.KeyStats.makeUriForStockKeyStats(stockSymbol),
                        new String[] {
                                Contract.KeyStats.COLUMN_DAY_LOW,
                                Contract.KeyStats.COLUMN_DAY_HIGH,
                                Contract.KeyStats.COLUMN_OPEN,
                                Contract.KeyStats.COLUMN_PREV_CLOSE,
                                Contract.KeyStats.COLUMN_VOLUME,
                                Contract.KeyStats.COLUMN_MARKET_CAP,
                                Contract.KeyStats.COLUMN_YEAR_LOW,
                                Contract.KeyStats.COLUMN_YEAR_HIGH
                        },
                        null,
                        null,
                        null
                );
        if (cursor == null) {
            throw new NullPointerException();
        }

        cursor.moveToNext();
        stockKeyStats = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            stockKeyStats.add(String.valueOf(
                    cursor.getFloat(cursor.getColumnIndex(KEY_STATS_COLUMN_NAMES[i]))
            ));
            log(KEY_STATS_COLUMN_NAMES[i] + ": " + stockKeyStats.get(i));
        }
        cursor.close();

        log("KeyStats: " + stockKeyStats.toString());
        return true;
    }

    private void updateKeyStatsView() {
        log("updateKeyStatsView");
        textViewDayRange.setText(String.format(
                getString(R.string.day_range),
                stockKeyStats.get(0) + " - " + stockKeyStats.get(1)
        ));
        textViewOpen.setText(String.format(
                getString(R.string.key_stats_open), stockKeyStats.get(2)));
        textViewPreviousClose.setText(String.format(
                getString(R.string.key_stats_prev_close), stockKeyStats.get(3)));
        textViewVolume.setText(String.format(
                getString(R.string.key_stats_volume), stockKeyStats.get(4)));
        textViewMarketCap.setText(String.format(
                getString(R.string.key_stats_market_cap), stockKeyStats.get(5)));
        textViewYearLow.setText(String.format(
                getString(R.string.key_stats_year_low), stockKeyStats.get(6)));
        textViewYearHigh.setText(String.format(
                getString(R.string.key_stats_year_high), stockKeyStats.get(7)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        log("onDestroyView");

        lineChart.setMarker(null);
        marker = null;

        disposables.dispose();
        if (disposables.isDisposed()) {
            log("isDisposed");
        }
    }

    private class CustomMarkerView extends MarkerView {
        private MPPointF offset;
        private SimpleDateFormat simpleDateFormat;

        public CustomMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);
            offset = new MPPointF(0, 0);
            simpleDateFormat = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        }

        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            int xPosition = (int) e.getX();
            float stockQuote = e.getY();
            textViewMarker.setText(simpleDateFormat.format(date.get(xPosition)) + ", $" + stockQuote);
            super.refreshContent(e, highlight);
        }

        @Override
        public MPPointF getOffset() {
            return offset;
        }
    }

    private final String[] KEY_STATS_COLUMN_NAMES = {
            Contract.KeyStats.COLUMN_DAY_LOW,
            Contract.KeyStats.COLUMN_DAY_HIGH,
            Contract.KeyStats.COLUMN_OPEN,
            Contract.KeyStats.COLUMN_PREV_CLOSE,
            Contract.KeyStats.COLUMN_VOLUME,
            Contract.KeyStats.COLUMN_MARKET_CAP,
            Contract.KeyStats.COLUMN_YEAR_LOW,
            Contract.KeyStats.COLUMN_YEAR_HIGH
    };
}
