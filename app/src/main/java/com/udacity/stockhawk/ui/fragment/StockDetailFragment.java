package com.udacity.stockhawk.ui.fragment;

import android.content.Context;
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
import java.util.Date;
import java.util.Locale;
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

    @BindView(R.id.text_view_marker)
    TextView textViewMarker;

    private IMarker marker;

    private DisposableSingleObserver<ArrayList<Entry>> disposableSingleObserver;

    private ArrayList<String> date;
    private ArrayList<Entry> stockQuotes;

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
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return date.get((int) value);
        }
    }

    private class StockCloseAxisValueFormatter implements IAxisValueFormatter {
        private DecimalFormat decimalFormat;

        StockCloseAxisValueFormatter() {
            decimalFormat = new DecimalFormat("####.##");
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            Timber.d("Value: " + value);
            return decimalFormat.format(value) + "$";
        }
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

        date = new ArrayList<>(numHistoricalQuotes);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());

        stockQuotes = new ArrayList<>(numHistoricalQuotes);

        // The stock quotes are in descending order of date.
        for (int i = numHistoricalQuotes - 1, entryX = 0; i >= 0; i--) {
            String[] entry = historicalQuotes[i].split(", ");
            stockQuotes.add(new Entry(entryX++, (new BigDecimal(entry[1])).floatValue()));
            date.add(simpleDateFormat.format(new Date(Long.parseLong(entry[0]))));
        }
        return stockQuotes;
    }

    private void drawGraph(ArrayList<Entry> arrayList) {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Timber.d("onDestroyView");

        lineChart.setMarker(null);
        marker = null;

        disposableSingleObserver.dispose();
        if (disposableSingleObserver.isDisposed()) {
            Timber.d("isDisposed");
        }
    }

    private class CustomMarkerView extends MarkerView {
        private MPPointF offset;

        public CustomMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);
            offset = new MPPointF(0, 0);
        }

        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            int xPosition = (int) e.getX();
            float stockQuote = e.getY();
            textViewMarker.setText(date.get(xPosition) + ", $" + stockQuote);
            super.refreshContent(e, highlight);
        }

        @Override
        public MPPointF getOffset() {
            return offset;
        }
    }
}
