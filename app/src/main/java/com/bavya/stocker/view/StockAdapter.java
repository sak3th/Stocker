package com.bavya.stocker.view;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bavya.stocker.R;
import com.bavya.stocker.model.Stock;

import java.util.ArrayList;
import java.util.HashMap;


public class StockAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = StockAdapter.class.getSimpleName();

    private static final int ITEM_STOCK = 1;

    private ArrayList<Stock> mStocks;
    private HashMap<Stock, Integer> mIdMap;
    private ItemActionListener mItemActionListener;
    private ItemTouchListener mItemTouchListener;

    public StockAdapter(ItemActionListener l) {
        mStocks = new ArrayList<Stock>();
        mIdMap = new HashMap<Stock, Integer>();
        mItemActionListener = l;
        mItemTouchListener = new ItemTouchListener();
        setHasStableIds(true);
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

    public void resetItemViews() {
        mItemTouchListener.resetItem();
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
            ((StockViewHolder) holder).bind(getItem(position), mItemTouchListener);
        }
    }

    public Stock getItem(int position) {
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
        private View mLayout, mFg;
        private TextView mName, mPrice, mDelete, mUpdate;

        public StockViewHolder(View view) {
            super(view);
            mLayout = view.findViewById(R.id.layoutStockItem);
            mFg = view.findViewById(R.id.layoutStockFg);
            mName = (TextView) view.findViewById(R.id.tvStockName);
            mPrice = (TextView) view.findViewById(R.id.tvStockPrice);
            mDelete = (TextView) view.findViewById(R.id.tvStockDelete);
            mUpdate = (TextView) view.findViewById(R.id.tvStockUpdate);
        }

        public void bind(Stock stock, View.OnTouchListener l) {
            mStock = stock;
            mLayout.setOnTouchListener(mItemTouchListener);
            mDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mItemTouchListener.animateDeleteItem();
                    mItemActionListener.onDeleteRequested(mStock);
                    removeStock(mStock);
                }
            });
            mUpdate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mItemTouchListener.resetItem();
                    mItemActionListener.onUpdateRequested(mStock);
                }
            });
            String name = stock.name + " " + "[" + stock.symbol.toUpperCase() + "]";
            //String price = stock.getCurrency() + stock.getQuote().getPrice();
            String price = stock.currency + stock.price;
            mName.setText(name);
            mPrice.setText(price);
        }
    }

    private class ItemTouchListener implements View.OnTouchListener {
        float mDownX;
        boolean mValidSwipe, mItemPressed;
        TextView mDelete, mUpdate;
        View mFg;
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            int action = e.getAction();
            boolean deleteSwipe = false, updateSwipe = false;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (!mItemPressed && !mValidSwipe) {
                        mDownX = e.getX();
                        mItemPressed = true;
                        mDelete = (TextView) v.findViewById(R.id.tvStockDelete);
                        mDelete.setEnabled(false);
                        mUpdate = (TextView) v.findViewById(R.id.tvStockUpdate);
                        mUpdate.setEnabled(false);
                        mFg = v.findViewById(R.id.layoutStockFg);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mItemPressed) {
                        float x = e.getX() - mDownX;
                        deleteSwipe = x >= (mDelete.getLeft() + mDelete.getWidth() * 0.5f);
                        updateSwipe = x >= (mUpdate.getLeft() + (mUpdate.getWidth() * 0.5f));
                        mDelete.setEnabled(deleteSwipe);
                        mUpdate.setEnabled(updateSwipe);
                        if (x > 0) {
                            mDelete.setVisibility(View.VISIBLE);
                            mUpdate.setVisibility(View.VISIBLE);
                            mFg.setTranslationX(x);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mItemPressed) {
                        float x = e.getX() - mDownX;
                        deleteSwipe = x >= (mDelete.getLeft() + mDelete.getWidth() * 0.5f);
                        updateSwipe = x >= (mUpdate.getLeft() + (mUpdate.getWidth() * 0.5f));
                        float rx = 0;
                        if (deleteSwipe) {
                            rx = mDelete.getRight();
                        }
                        if (updateSwipe) {
                            rx = mUpdate.getRight();
                        }

                        if (rx != 0) {
                            float tx = rx - mFg.getLeft();
                            mFg.animate().translationX(tx).setDuration(300);
                            mItemPressed = false;
                        } else {
                            animateBackInPlace();
                        }
                        break;
                    }
                    // fall through
                default:
                    if (mItemPressed || mValidSwipe) {
                        animateBackInPlace();
                    }
                    break;
            }
            deleteSwipe = deleteSwipe || mValidSwipe;
            if (mItemPressed && deleteSwipe != mValidSwipe) {
                mValidSwipe = deleteSwipe;
                mItemActionListener.onSwipe(mValidSwipe);
            }
            return true;
        }

        private void animateBackInPlace() {
            mFg.animate().translationX(0).setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mFg = null;
                            mDelete.setEnabled(false);
                            mDelete.setVisibility(View.GONE);
                            mDelete = null;
                            mUpdate.setEnabled(false);
                            mUpdate.setVisibility(View.GONE);
                            mUpdate = null;
                            mItemPressed = mValidSwipe = false;
                            mItemActionListener.onSwipe(false);
                        }
                    });
        }

        public void animateDeleteItem() {
            if (mFg != null) {

                mFg.animate().setDuration(300).translationX(-1 * mFg.getWidth())
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                if (mDelete != null) {
                                    mDelete.setEnabled(false);
                                    mDelete.setVisibility(View.GONE);
                                    mDelete = null;
                                }
                                if (mUpdate != null) {
                                    mUpdate.setEnabled(false);
                                    mUpdate.setVisibility(View.GONE);
                                    mUpdate = null;
                                }
                                mItemPressed = mValidSwipe = false;
                                mItemActionListener.onSwipe(false);
                                mFg.setTranslationX(0);
                                mFg = null;
                            }
                        });

            }
        }

        public void resetItem() {
            if (mFg != null) {
                if (mDelete != null) {
                    mDelete.setEnabled(false);
                    mDelete.setVisibility(View.GONE);
                    mDelete = null;
                }
                if (mUpdate != null) {
                    mUpdate.setEnabled(false);
                    mUpdate.setVisibility(View.GONE);
                    mUpdate = null;
                }
                mItemPressed = mValidSwipe = false;
                mItemActionListener.onSwipe(false);
                mFg.setTranslationX(0);
                mFg = null;
            }
        }
    }

    public interface ItemActionListener {
        void onUpdateRequested(Stock stock);
        void onDeleteRequested(Stock stock);
        void onSwipe(boolean valid);
    }
}
