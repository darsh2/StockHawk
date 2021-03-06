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
import com.udacity.stockhawk.sync.event.DataUpdatedEvent;
import com.udacity.stockhawk.util.Constants;
import com.udacity.stockhawk.util.DebugLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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

    /**
     * List of all dates in the historical data fetched
     * from API.
     */
    private ArrayList<Long> dates;

    /**
     * Stock quotes at all dates in {@link StockDetailFragment#dates
     * dates}.
     */
    private ArrayList<Entry> stockQuotes;

    /**
     * <p>Index of the tab that is currently selected (Shucks, such a helpful
     * comment). On restoring state, the time after which stock quotes
     * should be shown is known but it may not always be 2 years. Hence keep
     * track of the tab selected index to appropriately update UI.
     *
     * <p>By default always show stock quotes for two years and is hence set
     * to 3 as that is the tab index for 2 years.
     */
    private int tabSelectedIndex = 3;

    /**
     * Indicates the time after which stock quotes should be shown.
     */
    private long stockQuotesSince = Long.MIN_VALUE;

    /**
     * The actual dates to consider for adding to
     * the chart based on the user selected time period.
     */
    private ArrayList<Long> datesToShow;

    /**
     * The stock quotes corresponding to the time instants
     * in {@link StockDetailFragment#datesToShow datesToShow}.
     */
    private ArrayList<Entry> stockQuotesToShow;

    private LineDataSet lineDataSet;
    private LineData lineData;

    private ArrayList<String> stockKeyStats;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugLog.logMethod();
        if (getActivity() == null
                || getActivity().getIntent() == null) {
            return;
        }

        Intent intent = getActivity().getIntent();
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }
        stockSymbol = extras.getString(Constants.BUNDLE_STOCK_SYMBOL);
        stockName = extras.getString(Constants.BUNDLE_STOCK_NAME);
        stockPrice = extras.getString(Constants.BUNDLE_STOCK_PRICE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        DebugLog.logMethod();
        View view = inflater.inflate(R.layout.fragment_stock_detail, container, false);
        ButterKnife.bind(this, view);

        restoreInstanceState(savedInstanceState);

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

        loadStockKeyStats(false);
        initChartView();

        return view;
    }

    private void initChartView() {
        DebugLog.logMethod();
        styleLineChart();
        styleDateAxis();
        styleStockPriceAxis();
        styleDataSet();
        loadHistoricalStockQuotes(false);
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
            /*
            Consider the dates arrayList. It has following form:
            2 years from current date
            .
            .
            6 months from current date
            .
            .
            3 months from current date
            .
            .
            1 month from current date
            .
            .

            Initially, the stock quote entries corresponding to the entire
            dates arrayList is loaded into the chart. Hence label indices
            always correspond to the dates arrayList.

            However on changing the time period, the label indices either
            fall within the 1, 3, or 6 months range of the dates arrayList.
            Since we use datesToShow to indicate the modify time period, and
            we have indices corresponding to dates arrayList, we need to compute
            equivalent indices for datesToShow.

            dateToShow start from a particular month period and extend till the
            end of dates. Assume time period selected = 3 months from current date,
            dates.size() = 100, and dates.range 65 - 99 : 3 months from current
            date to current date
            Hence,
            datesToShow has all entries of dates from 65 - 99
            datesToShow.size() = 35
            datesToShow - dates mapping:
            0 - 65
            1 - 66
            2 - 67
            .
            .
            Label index corresponding to dates = 70
            Equivalent datesToShow index = 70 - (dates.size() - datesToShow.size())
                                         = 70 - 65
                                         = 5
             */
            position -= (dates.size() - datesToShow.size());
            if (position < 0 || position >= datesToShow.size()) {
                return "";
            }
            return simpleDateFormat.format(datesToShow.get(position));
        }
    }

    private void styleStockPriceAxis() {
        YAxis stockPriceAxis = lineChart.getAxisLeft();
        stockPriceAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        stockPriceAxis.setLabelCount(4, false);
        stockPriceAxis.setGranularity(1f);
        stockPriceAxis.setDrawGridLines(true);
        stockPriceAxis.setAxisLineColor(Color.BLACK);
        stockPriceAxis.setTextColor(Color.BLACK);
        stockPriceAxis.setValueFormatter(new StockPriceAxisValueFormatter());
    }

    private class StockPriceAxisValueFormatter implements IAxisValueFormatter {
        private DecimalFormat decimalFormat;

        StockPriceAxisValueFormatter() {
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

    /**
     * Fetch historical stock quotes from db and load the entries into the chart.
     * @param isDataUpdatedEvent Flag that indicates whether the {@link #stockQuotes stockQuotes} should
     *                           be forcefully loaded from db or not. If it is a case of restoring state,
     *                           then no point reloading from db. However if it is the first call to loading
     *                           stock quotes or it is called due to a {@link DataUpdatedEvent} then load
     *                           from db.
     */
    private void loadHistoricalStockQuotes(boolean isDataUpdatedEvent) {
        DebugLog.logMethod();

        if (!isDataUpdatedEvent && stockQuotes != null) {
            drawGraph();
            return;
        }

        Single<Boolean> stockQuotesSingle = Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                DebugLog.logMessage("stockQuotesSingle - call");
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
                        DebugLog.logMessage("stockQuotesSingleObserver - onSuccess");
                        drawGraph();
                    }

                    @Override
                    public void onError(Throwable e) {
                        DebugLog.logMessage("stockQuotesSingleObserver - onError");
                    }
                });
        disposables.add(disposableSingleObserver);
    }

    private boolean retrieveStockQuotesFromDb() {
        DebugLog.logMethod();
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

        if (dates != null) {
            dates.clear();
            stockQuotes.clear();
        } else {
            dates = new ArrayList<>(numHistoricalQuotes);
            stockQuotes = new ArrayList<>(numHistoricalQuotes);
        }

        // The stock quotes are in descending order of dates.
        for (int i = numHistoricalQuotes - 1, entryX = 0; i >= 0; i--) {
            String[] entry = historicalQuotes[i].split(", ");
            stockQuotes.add(new Entry(entryX++, (new BigDecimal(entry[1])).floatValue()));
            dates.add(Long.parseLong(entry[0]));
        }
        return true;
    }

    /**
     * Stores all the dates and stock quotes after the time specified
     * in {@link #stockQuotesSince stockQuotesSince}.
     */
    private void getStocksFromDate() {
        if (datesToShow == null) {
            datesToShow = new ArrayList<>();
            stockQuotesToShow = new ArrayList<>();
        } else {
            datesToShow.clear();
            stockQuotesToShow.clear();
        }

        for (int i = 0, l = dates.size(); i < l; i++) {
            if (dates.get(i) > stockQuotesSince) {
                datesToShow.add(dates.get(i));
                stockQuotesToShow.add(stockQuotes.get(i));
            }
        }
    }

    private void drawGraph() {
        DebugLog.logMethod();
        getStocksFromDate();

        // Clears all entries in the arrayList containing chart entries
        lineDataSet.clear();
        // Clears the dataSets list
        lineData.clearValues();

        for (int i = 0; i < stockQuotesToShow.size(); i++) {
            lineDataSet.addEntry(stockQuotesToShow.get(i));
        }
        lineData.addDataSet(lineDataSet);
        lineChart.setData(lineData);

        /*
        Indicate to the dataSet, data and chart that values have changed
        and recompute (ex: calculating min and max) various fields to
        ensure chart is correctly drawn.
         */
        lineDataSet.notifyDataSetChanged();
        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();

        /*
        Set listener for time period changes only after the chart
        is drawn to avoid recomputing graph entries and redrawing
        chart mid way during another draw operation.
         */
        setTabLayoutListener();
    }

    private void setTabLayoutListener() {
        if (onTabSelectedListener != null) {
            return;
        }

        if (tabLayout.getTabAt(tabSelectedIndex) != null) {
            tabLayout.getTabAt(tabSelectedIndex).select();
        }

        onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                /*
                X axis labels are disabled when time period
                selected is one month. Hence re-enable labels.
                 */
                lineChart.getXAxis().setDrawLabels(true);

                tabSelectedIndex = tab.getPosition();

                int months;
                Calendar calendar = Calendar.getInstance();

                switch (tabSelectedIndex) {
                    case 0: {
                        months = -1;
                        /*
                        Since the minimum number of labels is 2, the same
                        date is repeated twice in case viewing stock price
                        graph for one month. Hence disable label drawing
                        for one month period. This is just a temporary fix.
                         */
                        lineChart.getXAxis().setDrawLabels(false);
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
                stockQuotesSince = calendar.getTimeInMillis();

                /*
                Clear line chart marker for a particular entry as
                the values loaded into the chart now changes on
                changing the time period of viewing stocks.
                 */
                textViewMarker.setText("");
                lineChart.setSelected(false);

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
            /**
             * See {@link DateAxisValueFormatter#getFormattedValue(float, AxisBase)}
             * for details regarding the index computation
             */
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


    /**
     * Fetch stock key stats from db and update stock key stats view.
     * @param isDataUpdatedEvent Flag that indicates whether the {@link #stockKeyStats stockKeyStats} should
     *                           be forcefully loaded from db or not. If it is a case of restoring state,
     *                           then no point reloading from db. However if it is the first call to loading
     *                           stock quotes or it is called due to a {@link DataUpdatedEvent} then load
     *                           from db.
     */
    private void loadStockKeyStats(boolean isDataUpdatedEvent) {
        DebugLog.logMethod();

        if (!isDataUpdatedEvent && stockKeyStats != null) {
            updateKeyStatsView();
            return;
        }

        Single<Boolean> stockKeyStatsSingle = Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                DebugLog.logMessage("stockKeyStatsSingle - call");
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
                        DebugLog.logMessage("stockKeyStatsSingleObserver - onSuccess");
                        updateKeyStatsView();
                    }

                    @Override
                    public void onError(Throwable e) {
                        DebugLog.logMessage("stockKeyStatsSingleObserver - onError");
                        DebugLog.logMessage(e.toString() + "\n\n" + e.getMessage());
                        e.printStackTrace();
                    }
                });
        disposables.add(disposableSingleObserver);
    }

    private boolean retrieveStockKeyStatsFromDb() {
        DebugLog.logMethod();
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
        if (stockKeyStats != null) {
            stockKeyStats.clear();
        } else {
            stockKeyStats = new ArrayList<>(Constants.KEY_STATS_COLUMN_NAMES.length);
        }
        for (int i = 0, l = Constants.KEY_STATS_COLUMN_NAMES.length; i < l; i++) {
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
                new DecimalFormat("##.##M").format(volume)
        );

        // Display market cap as 'y billion'
        float marketCap = Float.parseFloat(stockKeyStats.get(Constants.POSITION_MARKET_CAP));
        marketCap /= Constants.ONE_BILLION;
        stockKeyStats.set(
                Constants.POSITION_MARKET_CAP,
                new DecimalFormat("##.##B").format(marketCap)
        );

        DebugLog.logMessage("KeyStats: " + stockKeyStats.toString());
        return true;
    }

    private void updateKeyStatsView() {
        DebugLog.logMethod();
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataUpdatedEvent(DataUpdatedEvent event) {
        if (event.getTimeStamp() != -1) {
            loadHistoricalStockQuotes(true);
            loadStockKeyStats(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Constants.BUNDLE_STOCK_NAME, stockName);
        outState.putString(Constants.BUNDLE_STOCK_SYMBOL, stockSymbol);
        outState.putString(Constants.BUNDLE_STOCK_PRICE, stockPrice);
        long[] datesArray = new long[dates.size()];
        for (int i = 0, l = dates.size(); i < l; i++) {
            datesArray[i] = dates.get(i);
        }
        outState.putLongArray(Constants.BUNDLE_STOCK_QUOTE_DATES, datesArray);
        outState.putParcelableArrayList(Constants.BUNDLE_STOCK_QUOTES, stockQuotes);
        outState.putInt(Constants.BUNDLE_TAB_SELECTED_INDEX, tabSelectedIndex);
        outState.putLong(Constants.BUNDLE_STOCK_QUOTES_SINCE, stockQuotesSince);
        outState.putStringArrayList(Constants.BUNDLE_STOCK_KEY_STATS, stockKeyStats);
    }

    /**
     * Restore values of various fields if present in savedInstanceState bundle.
     */
    private void restoreInstanceState(Bundle savedInstanceState) {
        DebugLog.logMethod();
        if (savedInstanceState == null) {
            return;
        }

        if (stockName != null) {
            stockName = savedInstanceState.getString(Constants.BUNDLE_STOCK_NAME);
            stockSymbol = savedInstanceState.getString(Constants.BUNDLE_STOCK_SYMBOL);
            stockPrice = savedInstanceState.getString(Constants.BUNDLE_STOCK_PRICE);
        }

        long[] datesArray = savedInstanceState.getLongArray(Constants.BUNDLE_STOCK_QUOTE_DATES);
        if (datesArray != null) {
            dates = new ArrayList<>(datesArray.length);
            for (int i = 0, l = datesArray.length; i < l; i++) {
                dates.add(datesArray[i]);
            }
        }
        stockQuotes = savedInstanceState.getParcelableArrayList(Constants.BUNDLE_STOCK_QUOTES);
        tabSelectedIndex = savedInstanceState.getInt(Constants.BUNDLE_TAB_SELECTED_INDEX, 3);
        stockQuotesSince = savedInstanceState.getLong(Constants.BUNDLE_STOCK_QUOTES_SINCE, Long.MIN_VALUE);
        stockKeyStats = savedInstanceState.getStringArrayList(Constants.BUNDLE_STOCK_KEY_STATS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        DebugLog.logMethod();

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
            DebugLog.logMessage("isDisposed");
        }
    }
}
