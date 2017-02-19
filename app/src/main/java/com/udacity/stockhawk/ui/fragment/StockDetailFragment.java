package com.udacity.stockhawk.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
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
import java.util.Calendar;
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

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    private TabLayout.OnTabSelectedListener onTabSelectedListener;

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

    private ArrayList<Long> dates;
    private ArrayList<Entry> stockQuotes;

    private ArrayList<Long> datesToShow;
    private ArrayList<Entry> stockQuotesToShow;

    private LineDataSet lineDataSet;
    private LineData lineData;

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
        styleDataSet();
        loadHistoricalStockQuotes();
    }

    private void styleLineChart() {
        lineChart.setTouchEnabled(true);

        lineChart.setDragEnabled(false);
        lineChart.setHighlightPerDragEnabled(false);

        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);

        /*
        Required to set lineChart's background color to the specified
        value. Ref: http://stackoverflow.com/a/32624619/3946664
         */
        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setDrawGridBackground(false);

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
        dateAxis.setGranularity(1f);
        dateAxis.setDrawGridLines(true);
        dateAxis.setAxisLineColor(Color.BLACK);
        dateAxis.setTextColor(Color.BLACK);
        dateAxis.setValueFormatter(new DateAxisValueFormatter());
    }

    private class DateAxisValueFormatter implements IAxisValueFormatter {
        private SimpleDateFormat simpleDateFormat;

        DateAxisValueFormatter() {
            simpleDateFormat = new SimpleDateFormat("MMM yy", Locale.getDefault());
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            int position = (int) value;
            position -= (dates.size() - datesToShow.size());
            if (position < 0 || position >= datesToShow.size()) {
                return "";
            }
            return simpleDateFormat.format(datesToShow.get(position));
        }
    }

    private void styleStockCloseAxis() {
        YAxis stockCloseAxis = lineChart.getAxisLeft();
        stockCloseAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        stockCloseAxis.setLabelCount(4, false);
        stockCloseAxis.setGranularity(1f);
        stockCloseAxis.setDrawGridLines(true);
        stockCloseAxis.setAxisLineColor(Color.BLACK);
        stockCloseAxis.setTextColor(Color.BLACK);
        stockCloseAxis.setValueFormatter(new StockCloseAxisValueFormatter());
    }

    private class StockCloseAxisValueFormatter implements IAxisValueFormatter {
        private DecimalFormat decimalFormat;

        StockCloseAxisValueFormatter() {
            decimalFormat = new DecimalFormat("####.##");
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return "$" + decimalFormat.format(value);
        }
    }

    private void styleDataSet() {
        lineDataSet = new LineDataSet(null, "Historical Stock Quotes");
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(1.8f);
        lineDataSet.setColor(Color.GREEN);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillColor(Color.GREEN);
        lineDataSet.setFillAlpha(100);
        lineDataSet.setDrawHighlightIndicators(true);
        lineDataSet.setHighLightColor(Color.rgb(244, 117, 117));

        lineData = new LineData(lineDataSet);
        lineData.setDrawValues(false);

        lineChart.setData(lineData);
    }

    private void loadHistoricalStockQuotes() {
        Single<Boolean> stockQuotesSingle = Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                log("stockQuotesSingle - call");
                return retrieveStockQuotesFromDb();
            }
        });
        stockQuotesSingle
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
        DisposableSingleObserver<Boolean> disposableSingleObserver = stockQuotesSingle
                .subscribeWith(new DisposableSingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(Boolean value) {
                        log("stockQuotesSingleObserver - onSuccess");
                        drawGraph();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log("stockQuotesSingleObserver - onError");
                    }
                });
        disposables.add(disposableSingleObserver);
    }

    private boolean retrieveStockQuotesFromDb() {
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

        dates = new ArrayList<>(numHistoricalQuotes);
        stockQuotes = new ArrayList<>(numHistoricalQuotes);

        // The stock quotes are in descending order of dates.
        for (int i = numHistoricalQuotes - 1, entryX = 0; i >= 0; i--) {
            String[] entry = historicalQuotes[i].split(", ");
            stockQuotes.add(new Entry(entryX++, (new BigDecimal(entry[1])).floatValue()));
            dates.add(Long.parseLong(entry[0]));
        }
        return getStocksFromDate(Long.MIN_VALUE);
    }

    private boolean getStocksFromDate(long from) {
        if (datesToShow == null) {
            datesToShow = new ArrayList<>();
            stockQuotesToShow = new ArrayList<>();
        } else {
            datesToShow.clear();
            stockQuotesToShow.clear();
        }

        for (int i = 0, l = dates.size(); i < l; i++) {
            if (dates.get(i) > from) {
                datesToShow.add(dates.get(i));
                stockQuotesToShow.add(stockQuotes.get(i));
            }
        }
        Log.i("ABCDE", "O: " + dates.size() + ", " + stockQuotes.size());
        Log.i("ABCDE", "M: " + datesToShow.size() + ", " + stockQuotesToShow.size());
        return true;
    }

    private void drawGraph() {
        log("drawGraph");
        lineDataSet.clear();
        lineData.clearValues();

        for (int i = 0; i < stockQuotesToShow.size(); i++) {
            lineDataSet.addEntry(stockQuotesToShow.get(i));
        }
        lineData.addDataSet(lineDataSet);
        lineChart.setData(lineData);

        lineDataSet.notifyDataSetChanged();
        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();

        setTabLayoutListener();
    }

    private void setTabLayoutListener() {
        if (onTabSelectedListener != null) {
            return;
        }

        /*
        Initially show chart of stock prices for
        two years. Hence select tab four which
        says 2 years.
         */
        if (tabLayout.getTabAt(3) != null) {
            tabLayout.getTabAt(3).select();
        }

        onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int months;
                Calendar calendar = Calendar.getInstance();
                switch (tab.getPosition()) {
                    case 0: {
                        months = -1;
                        lineChart.getXAxis().setLabelCount(2, false);
                        break;
                    }

                    case 1: {
                        months = -3;
                        lineChart.getXAxis().setLabelCount(3, false);
                        break;
                    }

                    case 2: {
                        months = -6;
                        lineChart.getXAxis().setLabelCount(3, false);
                        break;
                    }

                    default: {
                        months = -30;
                        lineChart.getAxisLeft().setLabelCount(4, false);
                        break;
                    }
                }
                calendar.add(Calendar.MONTH, months);

                /*
                Clear line chart marker for a particular entry as
                the values loaded into the chart now changes on
                changing the time period of viewing stocks.
                 */
                textViewMarker.setText("");
                lineChart.setSelected(false);

                getStocksFromDate(calendar.getTimeInMillis());
                drawGraph();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        };
        tabLayout.addOnTabSelectedListener(onTabSelectedListener);
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
            xPosition -= (dates.size() - datesToShow.size());
            float stockQuote = e.getY();
            textViewMarker.setText(simpleDateFormat.format(datesToShow.get(xPosition)) + ", $" + stockQuote);
            super.refreshContent(e, highlight);
        }

        @Override
        public MPPointF getOffset() {
            return offset;
        }
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

    private boolean retrieveStockKeyStatsFromDb() {
        log("retrieveStockKeyStatsFromDb");
        Cursor cursor = getContext().getContentResolver()
                .query(
                        Contract.KeyStats.makeUriForStockKeyStats(stockSymbol),
                        Constants.KEY_STATS_COLUMN_NAMES,
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
                    cursor.getFloat(cursor.getColumnIndex(Constants.KEY_STATS_COLUMN_NAMES[i]))
            ));
        }
        cursor.close();

        // Display volume as 'x million'
        float volume = Float.parseFloat(stockKeyStats.get(Constants.POSITION_VOLUME));
        volume /= Constants.ONE_MILLION;
        stockKeyStats.set(
                Constants.POSITION_VOLUME,
                new DecimalFormat("##.####M").format(volume)
        );

        // Display market cap as 'y billion'
        float marketCap = Float.parseFloat(stockKeyStats.get(Constants.POSITION_MARKET_CAP));
        marketCap /= Constants.ONE_BILLION;
        stockKeyStats.set(
                Constants.POSITION_MARKET_CAP,
                new DecimalFormat("##.####B").format(marketCap)
        );

        log("KeyStats: " + stockKeyStats.toString());
        return true;
    }

    private void updateKeyStatsView() {
        log("updateKeyStatsView");
        textViewDayRange.setText(String.format(
                getString(R.string.day_range),
                stockKeyStats.get(Constants.POSITION_DAY_LOW) + " - " + stockKeyStats.get(Constants.POSITION_DAY_HIGH)
        ));
        textViewOpen.setText(String.format(
                getString(R.string.key_stats_open), stockKeyStats.get(Constants.POSITION_OPEN)
        ));
        textViewPreviousClose.setText(String.format(
                getString(R.string.key_stats_prev_close), stockKeyStats.get(Constants.POSITION_PREV_CLOSE)
        ));
        textViewVolume.setText(String.format(
                getString(R.string.key_stats_volume), stockKeyStats.get(Constants.POSITION_VOLUME)
        ));
        textViewMarketCap.setText(String.format(
                getString(R.string.key_stats_market_cap), stockKeyStats.get(Constants.POSITION_MARKET_CAP)
        ));
        textViewYearLow.setText(String.format(
                getString(R.string.key_stats_year_low), stockKeyStats.get(Constants.POSITION_YEAR_LOW)
        ));
        textViewYearHigh.setText(String.format(
                getString(R.string.key_stats_year_high), stockKeyStats.get(Constants.POSITION_YEAR_HIGH)
        ));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        log("onDestroyView");

        /*
        Release references that various objects may be
        holding in order to allow for garbage collection.
         */

        lineDataSet.clear();
        lineData.clearValues();
        lineChart.clear();

        lineChart.setMarker(null);
        marker = null;

        tabLayout.removeOnTabSelectedListener(onTabSelectedListener);
        onTabSelectedListener = null;

        disposables.dispose();
        if (disposables.isDisposed()) {
            log("isDisposed");
        }
    }

    private static final String tag = "CL-SDF";
    private static final boolean DEBUG = true;
    private static final void log(String message) {
        if (DEBUG) {
            Log.i(tag, message);
        }
    }
}
