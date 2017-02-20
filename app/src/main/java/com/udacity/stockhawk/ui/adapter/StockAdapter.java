package com.udacity.stockhawk.ui.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.model.StockQuote;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {
    private final Context context;

    private ArrayList<StockQuote> stockQuotes;

    private final DecimalFormat dollarFormatWithPlus;
    private final DecimalFormat dollarFormat;
    private final DecimalFormat percentageFormat;

    private StockAdapterOnClickHandler clickHandler;

    public StockAdapter(Context context, StockAdapterOnClickHandler clickHandler) {
        this.context = context;

        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");

        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");

        this.clickHandler = clickHandler;
    }

    public void updateStockQuotes(ArrayList<StockQuote> stockQuotes) {
        if (this.stockQuotes == null) {
            this.stockQuotes = new ArrayList<>(stockQuotes.size());
        } else {
            this.stockQuotes.clear();
        }
        this.stockQuotes.addAll(stockQuotes);
        notifyDataSetChanged();
    }

    public String getSymbolAtPosition(int position) {
        return stockQuotes.get(position).symbol;
    }

    @Override
    public StockViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(context).inflate(R.layout.list_item_quote, parent, false);
        return new StockViewHolder(item);
    }

    @Override
    public void onBindViewHolder(StockViewHolder holder, int position) {
        holder.symbol.setText(stockQuotes.get(position).symbol);
        holder.price.setText(dollarFormat.format(stockQuotes.get(position).price));

        float rawAbsoluteChange = stockQuotes.get(position).absoluteChange;
        float percentageChange = stockQuotes.get(position).percentageChange;

        if (rawAbsoluteChange > 0) {
            holder.change.setTextColor(ContextCompat.getColor(context, R.color.material_green_700));
        } else {
            holder.change.setTextColor(ContextCompat.getColor(context, R.color.material_red_700));
        }

        String change = dollarFormatWithPlus.format(rawAbsoluteChange);
        String percentage = percentageFormat.format(percentageChange / 100);

        if (PrefUtils.getDisplayMode(context)
                .equals(context.getString(R.string.pref_display_mode_absolute_key))) {
            holder.change.setText(change);
        } else {
            holder.change.setText(percentage);
        }
    }

    @Override
    public int getItemCount() {
        if (stockQuotes != null) {
            return stockQuotes.size();
        }
        return 0;
    }

    public void releaseResources() {
        this.stockQuotes = null;
        this.clickHandler = null;
    }

    public interface StockAdapterOnClickHandler {
        void onStockClick(String symbol, String name, float price);
    }

    class StockViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.symbol)
        TextView symbol;

        @BindView(R.id.price)
        TextView price;

        @BindView(R.id.change)
        TextView change;

        StockViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            clickHandler.onStockClick(
                    stockQuotes.get(adapterPosition).symbol,
                    stockQuotes.get(adapterPosition).name,
                    stockQuotes.get(adapterPosition).price
            );
        }
    }
}
