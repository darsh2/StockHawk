package com.udacity.stockhawk.ui.fragment;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.model.StockQuote;
import com.udacity.stockhawk.sync.QuoteSyncJob;
import com.udacity.stockhawk.sync.event.DataUpdatedEvent;
import com.udacity.stockhawk.sync.event.ErrorEvent;
import com.udacity.stockhawk.ui.activity.StockListActivity;
import com.udacity.stockhawk.ui.adapter.StockAdapter;
import com.udacity.stockhawk.ui.dialog.AddStockDialog;
import com.udacity.stockhawk.util.Constants;
import com.udacity.stockhawk.util.DebugLog;
import com.udacity.stockhawk.widget.StockQuoteWidgetProvider;

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

    @BindView(R.id.toolbar)
    Toolbar toolbar;

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
        DebugLog.logMethod();
        View view = inflater.inflate(R.layout.fragment_stock_list, container, true);
        ButterKnife.bind(this, view);

        toolbar.setTitle(getString(R.string.app_name));
        if (getActivity() instanceof StockListActivity) {
            ((StockListActivity) getActivity()).setSupportActionBar(toolbar);
            ((StockListActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        stockQuotes = new ArrayList<>();
        adapter = new StockAdapter(getContext(), this);
        stockRecyclerView.setAdapter(adapter);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        stockRecyclerView.addItemDecoration(new DividerItemDecoration(stockRecyclerView.getContext(), DividerItemDecoration.VERTICAL));

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);
        EventBus.getDefault().register(this);
        initialize(savedInstanceState);

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                deleteStockQuote(stockQuotes.get(viewHolder.getAdapterPosition()).symbol);
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
    public void initialize(Bundle savedInstanceState) {
        DebugLog.logMethod();
        if (savedInstanceState != null
                && savedInstanceState.getParcelableArrayList(Constants.BUNDLE_STOCK_QUOTES) != null) {
            swipeRefreshLayout.setRefreshing(false);

            stockQuotes = savedInstanceState.getParcelableArrayList(Constants.BUNDLE_STOCK_QUOTES);
            updateRecyclerView();
            return;
        }

        swipeRefreshLayout.setRefreshing(true);
        if (!networkUp()) {
            loadStockQuotes();
            showToast(getString(R.string.toast_error_no_internet_connection));
        }
        QuoteSyncJob.initialize(getContext());
    }

    @Override
    public void onRefresh() {
        DebugLog.logMethod();
        error.setVisibility(View.GONE);
        if (!networkUp()) {
            loadStockQuotes();
            showToast(getString(R.string.toast_error_no_internet_connection));
        }
        QuoteSyncJob.syncImmediately(getContext(), false);
    }

    private boolean networkUp() {
        DebugLog.logMethod();
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
            showToast(getString(R.string.stock_symbol_constraint));
            return;
        }

        for (int i = 0, l = stockQuotes.size(); i < l; i++) {
            if (symbol.equals(stockQuotes.get(i).symbol)) {
                showToast(String.format(getString(R.string.stock_symbol_exists), symbol));
                return;
            }
        }

        /*
        Avoided saving stock to PrefUtils when there is no internet connection.
        This is because the added stock symbol may be invalid. Also, with the
        current logic in place, adding a stock when there is no internet
        connection does not refresh widget.
         */
        if (!networkUp()) {
            showToast(String.format(getString(R.string.toast_stock_added_no_connectivity), symbol));
            return;
        }

        swipeRefreshLayout.setRefreshing(true);
        PrefUtils.addStock(getActivity(), symbol);
        QuoteSyncJob.syncImmediately(getActivity(), true);
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (stockQuotes != null) {
            outState.putParcelableArrayList(Constants.BUNDLE_STOCK_QUOTES, stockQuotes);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEvent(ErrorEvent event) {
        DebugLog.logMessage("onErrorEvent");

        swipeRefreshLayout.setRefreshing(false);
        if (event.getCode() == ErrorEvent.NETWORK_ERROR) {
            showToast(getString(R.string.error_network));

        } else if (event.getCode() == ErrorEvent.SYMBOL_NOT_FOUND_ERROR) {
            String symbol = "Stock symbol";
            /*
            If data for an entered symbol is not available, delete it
            from the preferences as it is invalid.
             */
            if (event.getSymbol() != null) {
                symbol = event.getSymbol();
                PrefUtils.removeStock(getContext(), symbol);
            }

            showToast(String.format(getString(R.string.error_stock_symbol_not_found), symbol));
        }
    }

    private void showToast(String message) {
        Toast.makeText(
                getContext(),
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    private final String[] QUOTE_PROJECTION = {
            Contract.Quote.COLUMN_SYMBOL,
            Contract.Quote.COLUMN_NAME,
            Contract.Quote.COLUMN_PRICE,
            Contract.Quote.COLUMN_ABSOLUTE_CHANGE,
            Contract.Quote.COLUMN_PERCENTAGE_CHANGE
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataUpdatedEvent(DataUpdatedEvent event) {
        DebugLog.logMethod();
        if (event.getTimeStamp() == -1) {
            return;
        }

        /*
        Reload stock quotes from db.
         */
        swipeRefreshLayout.setRefreshing(true);
        loadStockQuotes();
        /*
        DataUpdatedEvent is fired since a new symbol was added.
        Update app widget to reflect the change.
         */
        if (event.getIsNewSymbolAdded()) {
            updateAppWidget();
        }
    }

    /**
     * Fetches stock quotes from db in a separate thread
     * and updates view in the main thread.
     */
    private void loadStockQuotes() {
        DebugLog.logMethod();
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
                        updateRecyclerView();
                    }

                    @Override
                    public void onError(Throwable e) {
                        DebugLog.logMessage("stockQuotesSingleObserver - onError");
                        DebugLog.logMessage(e.toString() + "\n\n" + e.getMessage());
                        e.printStackTrace();

                        /*
                        In case of any error on fetching stock quotes from db, hide
                        the refresh progress loader.
                         */
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
        disposables.add(disposableSingleObserver);
    }

    private boolean retrieveStockQuotesFromDb() {
        DebugLog.logMethod();
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

    private void updateRecyclerView() {
        DebugLog.logMethod();
        swipeRefreshLayout.setRefreshing(false);
        if (stockQuotes.size() > 0) {
            if (error.getVisibility() != View.GONE) {
                error.setVisibility(View.GONE);
            }
            if (stockRecyclerView.getVisibility() != View.VISIBLE) {
                stockRecyclerView.setVisibility(View.VISIBLE);
            }
            adapter.updateStockQuotes(stockQuotes);
            return;
        }

        /*
        Recycler view visibility is set to gone when there
        are no stocks since the empty recycler view showed
        a divider. This is a dirty fix and probably the
        divider should not have been shown.
         */
        if (stockRecyclerView.getVisibility() != View.GONE) {
            stockRecyclerView.setVisibility(View.GONE);
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
        DebugLog.logMessage("Symbol clicked: " +  symbol + ", " + name);
        Activity activity = getActivity();
        if (activity instanceof StockListActivity) {
            ((StockListActivity) activity).onStockClick(symbol, name, price);
        }
    }

    /**
     * Deletes stock from db and updates view.
     * First deletes stock quote from db. Deletes both
     * stock quotes and stock key stats. On successful
     * deletion, stock is removed from preferences and
     * views (app view and widget view) are updated.
     * @param symbol Stock symbol that is to be deleted
     */
    private void deleteStockQuote(final String symbol) {
        DebugLog.logMethod();
        Single<Boolean> deleteStockQuoteSingle = Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return deleteStockQuoteFromDb(symbol);
            }
        });
        deleteStockQuoteSingle
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
        DisposableSingleObserver<Boolean> disposableSingleObserver = deleteStockQuoteSingle
                .subscribeWith(new DisposableSingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(Boolean value) {
                        DebugLog.logMessage("deleteStockQuoteSingleObserver - onSuccess");
                        onDeleteSuccessful(symbol);
                    }

                    @Override
                    public void onError(Throwable e) {
                        DebugLog.logMessage("deleteStockQuoteSingleObserver - onError");
                        DebugLog.logMessage(e.getMessage());
                        e.printStackTrace();
                        showToast(String.format(getString(R.string.stock_quote_delete_unsuccessful), symbol));
                    }
                });
        disposables.add(disposableSingleObserver);
    }

    private boolean deleteStockQuoteFromDb(String symbol) {
        DebugLog.logMethod();
        // Delete stock quote
        int numRowsDeleted = getContext().getContentResolver()
                .delete(Contract.Quote.makeUriForStock(symbol), null, null);
        if (numRowsDeleted != 1) {
            return false;
        }
        // Delete stock key stats
        numRowsDeleted = getContext().getContentResolver()
                .delete(Contract.KeyStats.makeUriForStockKeyStats(symbol), null, null);
        return numRowsDeleted == 1;
    }

    private void onDeleteSuccessful(String symbol) {
        PrefUtils.removeStock(getContext(), symbol);

        for (int i = 0, l = stockQuotes.size(); i < l; i++) {
            if (stockQuotes.get(i).symbol.equals(symbol)) {
                stockQuotes.remove(i);
                break;
            }
        }
        updateRecyclerView();
        showToast(String.format(getString(R.string.stock_quote_delete_successful), symbol));

        updateAppWidget();
    }

    private void updateAppWidget() {
        DebugLog.logMethod();
        Context context = getContext();
        Intent intent = new Intent(context, StockQuoteWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] widgetIds = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(
                        context,
                        StockQuoteWidgetProvider.class
                ));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        context.sendBroadcast(intent);
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
        DebugLog.logMethod();

        disposables.dispose();
        if (disposables.isDisposed()) {
            DebugLog.logMessage("isDisposed");
        }
        adapter.releaseResources();
        itemTouchHelper.attachToRecyclerView(null);
    }
}