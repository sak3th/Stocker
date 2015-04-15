package com.bavya.stocker.view;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bavya.stocker.R;
import com.bavya.stocker.model.Stock;

import java.util.ArrayList;


public class StockAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = StockAdapter.class.getSimpleName();

    private static final int ITEM_STOCK = 1;

    private ArrayList<Stock> mStocks;
    private OnStockLongClickListener mStockLongClickListener;

    public StockAdapter(OnStockLongClickListener l) {
        mStocks = new ArrayList<Stock>();
        mStockLongClickListener = l;
    }

    public void addStock(Stock stock) {
        if (stock == null) {
            return;
        }
        int index = mStocks.indexOf(stock);
        if (index == -1) {
            mStocks.add(stock);
            notifyItemInserted(mStocks.size() - 1);
        } else {
            mStocks.set(index, stock);
            notifyItemChanged(index);
        }
    }

    public void removeStock(Stock stock) {
        if (stock == null) {
            return;
        }
        int index = mStocks.indexOf(stock);
        if (index != -1) {
            mStocks.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void updateStock(String symbol, int change) {
        for (int i = 0; i < mStocks.size(); i++) {
            Stock s = mStocks.get(i);
            if (s.symbol.equals(symbol)) {
                s.change = change;
                mStocks.set(i, s);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void changeStocks(ArrayList<Stock> stocks) {
        mStocks = stocks;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh = null;
        switch (viewType) {
            case ITEM_STOCK:
                vh = new StockViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_stock, parent, false));
                break;
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof StockViewHolder) {
            ((StockViewHolder) holder).bind(getItem(position));
        }
    }

    private Stock getItem(int position) {
        return (mStocks != null) ? mStocks.get(position) : null ;
    }

    @Override
    public int getItemViewType(int position) {
        return ITEM_STOCK;
    }

    @Override
    public int getItemCount() {
        return (mStocks != null) ? mStocks.size() : 0;
    }

    private class StockViewHolder extends RecyclerView.ViewHolder {
        private Stock mStock;
        private View mLayout;
        private TextView mName, mPrice;

        public StockViewHolder(View view) {
            super(view);
            mLayout = view.findViewById(R.id.stockItemLayout);
            mName = (TextView) view.findViewById(R.id.tvStockName);
            mPrice = (TextView) view.findViewById(R.id.tvStockPrice);
            mLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mStockLongClickListener.onStockLongClick(mStock);
                    return true;
                }
            });
        }

        public void bind(Stock stock) {
            mStock = stock;
            String name = stock.name + " " + "[" + stock.symbol.toUpperCase() + "]";
            //String price = stock.getCurrency() + stock.getQuote().getPrice();
            String price = stock.currency + stock.price;
            mName.setText(name);
            mPrice.setText(price);
        }
    }

    public interface OnStockLongClickListener {
        public void onStockLongClick(Stock stock);
    }
}
