package com.udacity.stockhawk.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.model.StockQuote;
import com.udacity.stockhawk.sync.QuoteSyncJob;
import com.udacity.stockhawk.sync.event.DataUpdatedEvent;
import com.udacity.stockhawk.sync.event.ErrorEvent;
import com.udacity.stockhawk.ui.activity.MainActivity;
import com.udacity.stockhawk.ui.adapter.StockAdapter;
import com.udacity.stockhawk.ui.dialog.AddStockDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by darshan on 14/1/17.
 */

public class StockListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler {

    public static final String TAG = StockListFragment.class.getName();

    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_view)
    RecyclerView stockRecyclerView;

    private ItemTouchHelper itemTouchHelper;

    private StockAdapter adapter;
    private ArrayList<StockQuote> stockQuotes;

    private CompositeDisposable disposables = new CompositeDisposable();

    @BindView(R.id.error)
    TextView error;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        log("onCreateView");
        log("Process: " + android.os.Process.myPid());
        View view = inflater.inflate(R.layout.fragment_stocks_list, container, true);
        ButterKnife.bind(this, view);

        stockQuotes = new ArrayList<>();
        adapter = new StockAdapter(getContext(), this);
        stockRecyclerView.setAdapter(adapter);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);
        EventBus.getDefault().register(this);
        initialize();

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());
                PrefUtils.removeStock(getActivity(), symbol);
                getActivity().getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);
            }
        });
        itemTouchHelper.attachToRecyclerView(stockRecyclerView);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    /**
     * Intimates user about validity of stock quotes being fetched, ie,
     * if there is no internet connection, load stock quotes from db.
     * Starts the periodic and one off job to fetch stock quotes.
     */
    public void initialize() {
        log("initialize");
        swipeRefreshLayout.setRefreshing(true);
        if (!networkUp()) {
            loadStockQuotes();
            showSnackbar(getString(R.string.toast_error_no_internet_connection));
        }
        QuoteSyncJob.initialize(getContext());
    }

    @Override
    public void onRefresh() {
        log("onRefresh");
        error.setVisibility(View.GONE);
        if (!networkUp()) {
            loadStockQuotes();
            showSnackbar(getString(R.string.toast_error_no_internet_connection));
        }
        QuoteSyncJob.syncImmediately(getContext());
    }

    private boolean networkUp() {
        log("networkUp");
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    @OnClick(R.id.fab)
    public void button() {
        AddStockDialog addStockDialog = (new AddStockDialog());
        addStockDialog.setTargetFragment(this, 1);
        addStockDialog.show(getFragmentManager(), "dialog");
    }

    public void addStock(String symbol) {
        if (symbol == null
                || symbol.isEmpty()
                || !symbol.matches("^[A-Z]+$")) {
            showSnackbar(getString(R.string.stock_symbol_constraint));
            return;
        }

        if (!networkUp()) {
            showSnackbar(getString(R.string.internet_connectivity_unavailable));
            return;
        }

        swipeRefreshLayout.setRefreshing(true);
        PrefUtils.addStock(getActivity(), symbol);
        QuoteSyncJob.syncImmediately(getActivity());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(getActivity());
            setDisplayModeMenuItemIcon(item);
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(getContext())
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEvent(ErrorEvent event) {
        log("onErrorEvent");
        swipeRefreshLayout.setRefreshing(false);
        if (event.getCode() == ErrorEvent.NETWORK_ERROR) {
            showSnackbar(getString(R.string.error_network));
        } else if (event.getCode() == ErrorEvent.SYMBOL_NOT_FOUND_ERROR) {
            showSnackbar(getString(R.string.error_stock_symbol_not_found));
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(
                stockRecyclerView.getRootView(),
                message,
                Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.toast_action_ok), new View.OnClickListener() {
            @Override
            public void onClick(View v) {}
        }).show();
    }

    private final String[] QUOTE_PROJECTION = {
            Contract.Quote.COLUMN_SYMBOL,
            Contract.Quote.COLUMN_NAME,
            Contract.Quote.COLUMN_PRICE,
            Contract.Quote.COLUMN_ABSOLUTE_CHANGE,
            Contract.Quote.COLUMN_PERCENTAGE_CHANGE
    };


    private static final String tag = "DL-SLF";
    private static final boolean DEBUG = true;
    private static void log(String message) {
        if (DEBUG) {
            Log.i(tag, message);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataUpdatedEvent(DataUpdatedEvent event) {
        log("onDataUpdatedEvent");
        swipeRefreshLayout.setRefreshing(true);
        if (event.timeStamp != -1) {
            loadStockQuotes();
        }
    }

    private void loadStockQuotes() {
        log("loadStockQuotes");
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
                        updateAdapterView();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log("stockQuotesSingleObserver - onError");
                        log(e.toString() + "\n\n" + e.getMessage());
                        e.printStackTrace();
                    }
                });
        disposables.add(disposableSingleObserver);
    }

    private boolean retrieveStockQuotesFromDb() {
        log("retrieveStockQuotesFromDb");
        if (getContext() == null) {
            throw new NullPointerException("getContext() returned null");
        }

        Cursor cursor = getContext().getContentResolver().query(
                Contract.Quote.URI,
                QUOTE_PROJECTION,
                null,
                null,
                null
        );
        if (cursor == null) {
            throw new NullPointerException("Cursor is null");
        }

        if (stockQuotes == null) {
            stockQuotes = new ArrayList<>(cursor.getCount());
        } else {
            stockQuotes.clear();
        }
        while (cursor.moveToNext()) {
            stockQuotes.add(new StockQuote(
                    cursor.getString(cursor.getColumnIndex(QUOTE_PROJECTION[0])),
                    cursor.getString(cursor.getColumnIndex(QUOTE_PROJECTION[1])),
                    cursor.getFloat(cursor.getColumnIndex(QUOTE_PROJECTION[2])),
                    cursor.getFloat(cursor.getColumnIndex(QUOTE_PROJECTION[3])),
                    cursor.getFloat(cursor.getColumnIndex(QUOTE_PROJECTION[4]))
            ));
        }
        cursor.close();
        return true;
    }

    private void updateAdapterView() {
        log("updateAdapterView");
        swipeRefreshLayout.setRefreshing(false);
        if (stockQuotes.size() > 0) {
            error.setVisibility(View.GONE);
            adapter.updateStockQuotes(stockQuotes);
            return;
        }

        if (PrefUtils.getStocks(getContext()).size() == 0) {
            error.setText(getString(R.string.error_no_stocks));
        } else {
            error.setText(getString(R.string.error_no_network));
        }
        error.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStockClick(String symbol, String name, float price) {
        log("Symbol clicked: " +  symbol + ", " + name);
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).onStockClick(symbol, name, price);
        }
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
        log("onDestroyView");

        disposables.dispose();
        if (disposables.isDisposed()) {
            log("isDisposed");
        }
        adapter.releaseResources();
        itemTouchHelper.attachToRecyclerView(null);
    }
}