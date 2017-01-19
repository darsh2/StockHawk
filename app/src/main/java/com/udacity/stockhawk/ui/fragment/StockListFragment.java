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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.udacity.stockhawk.sync.QuoteSyncJob;
import com.udacity.stockhawk.sync.event.ErrorEvent;
import com.udacity.stockhawk.sync.receiver.StockSymbolNotFoundReceiver;
import com.udacity.stockhawk.ui.activity.MainActivity;
import com.udacity.stockhawk.ui.adapter.StockAdapter;
import com.udacity.stockhawk.ui.dialog.AddStockDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Created by darshan on 14/1/17.
 */

public class StockListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler {

    public static final String TAG = StockListFragment.class.getName();

    private static final int STOCK_LOADER = 0;

    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_view)
    RecyclerView stockRecyclerView;

    private StockAdapter adapter;

    @BindView(R.id.error)
    TextView error;

    private StockSymbolNotFoundReceiver stockSymbolNotFoundReceiver;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stocks_list, container, true);
        ButterKnife.bind(this, view);

        adapter = new StockAdapter(getContext(), this);
        stockRecyclerView.setAdapter(adapter);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);
        onRefresh();

        QuoteSyncJob.initialize(getContext());
        getLoaderManager().initLoader(STOCK_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
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
        }).attachToRecyclerView(stockRecyclerView);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onRefresh() {
        QuoteSyncJob.syncImmediately(getActivity());

        if (!networkUp() && adapter.getItemCount() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);

        } else if (!networkUp()) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), R.string.toast_no_connectivity, Toast.LENGTH_LONG).show();

        } else if (PrefUtils.getStocks(getActivity()).size() == 0) {
            Timber.d("WHYAREWEHERE");
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_stocks));
            error.setVisibility(View.VISIBLE);

        } else {
            error.setVisibility(View.GONE);
        }
    }

    private boolean networkUp() {
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
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

        /*
        stockSymbolNotFoundReceiver = new StockSymbolNotFoundReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_STOCK_SYMBOL_NOT_FOUND);
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(stockSymbolNotFoundReceiver, intentFilter);
         */
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();

        /*
        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(stockSymbolNotFoundReceiver);
         */
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
        Timber.d("onErrorEvent");
        if (event.getCode() == ErrorEvent.NETWORK_ERROR) {
            showSnackbar(getString(R.string.error_network));
        } else if (event.getCode() == ErrorEvent.SYMBOL_NOT_FOUND_ERROR) {
            showSnackbar(getString(R.string.error_stock_symbol_not_found));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS,
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);
        if (data.getCount() != 0) {
            error.setVisibility(View.GONE);
        }
        adapter.setCursor(data);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(null);
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

    /*
    @Override
    public void onStockSymbolNotFound() {
        showSnackbar("Stock symbol not found. Please enter a valid symbol.");
        swipeRefreshLayout.setRefreshing(false);
    }
    */

    public interface OnStockClickListener {
        void onStockClick(String symbol, String name);
    }

    @Override
    public void onStockClick(String symbol, String name) {
        Timber.d("Symbol clicked: %s %s", symbol, name);
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).onStockClick(symbol, name);
        }
    }
}

