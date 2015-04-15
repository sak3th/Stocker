package com.bavya.stocker.activity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bavya.stocker.R;
import com.bavya.stocker.model.Stock;
import com.bavya.stocker.provider.Stocker;
import com.bavya.stocker.service.StockService;
import com.bavya.stocker.view.StockAdapter;

import java.util.ArrayList;


public class StocksFragment extends Fragment {
    private static final String TAG = "StockFragment";

    private RecyclerView mRecyclerView;
    private StockAdapter mAdapter;
    private TextView mTextViewAddMsg, mTextViewStatus;
    private View mStatusLayout;
    private SwipeRefreshLayout mSpinner;

    private StockAdapter.OnStockLongClickListener mStockLongClickListener;
    private Context mContext;
    private boolean mConnected;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((HomeActivity)activity).setStocksFragment(this);
        mStockLongClickListener = (StockAdapter.OnStockLongClickListener) activity;
        mContext = activity.getApplicationContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_stock_list, container, false);
        initViews(v);
        setRetainInstance(true);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new StockAdapter(mStockLongClickListener);
        mRecyclerView.setAdapter(mAdapter);
        getActivity().getContentResolver().registerContentObserver(
                Stocker.Stocks.CONTENT_URI, true, mDatabaseObserver);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllFromDatabase();
        refreshViews();
    }

    public void addStock(String symbol, int change) {
        Bundle bundle = new Bundle();
        bundle.putStringArray("key_symbols", new String[]{symbol});
        bundle.putIntArray("key_changes", new int[]{change});
        Log.d(TAG, "adding stock  " + symbol);
        //getLoaderManager().restartLoader(LOADER_GET_STOCKS, bundle, new GetStocksCallbacks());
        StockService.getStocks(getActivity(), new String[]{symbol}, new int[]{change});
        mTextViewStatus.setText(getString(R.string.status_fetching) + " " + symbol + "...");
        mStatusLayout.setVisibility(View.VISIBLE);
    }

    public void removeStock(Stock stock) {
        removeStockFromDb(stock);
    }

    public void updateStock(String symbol, int change) {
        updateStockInDb(symbol, change);
    }

    // TODO improve to delete directly
    private void removeStockFromDb(Stock stock) {
        int id = -1;
        ContentResolver cr = getActivity().getContentResolver();
        String[] projection = new String[]{Stocker.Stocks._ID};
        String select = "symbol = '" + stock.symbol + "'";
        Cursor cursor = cr.query(Stocker.Stocks.CONTENT_URI, projection, select, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                id = cursor.getInt(cursor.getColumnIndexOrThrow(Stocker.Stocks._ID));
            }
            cursor.close();
        }
        if (id != -1) {
            Uri uri = ContentUris.withAppendedId(Stocker.Stocks.CONTENT_URI, id);
            if (cr.delete(uri, null, null) > 0) {
                mAdapter.removeStock(stock);
                refreshViews();
            }
        }
    }

    private void updateStockInDb(String symbol, int change) {
        int id = -1;
        ContentResolver cr = getActivity().getContentResolver();
        String[] projection = new String[]{Stocker.Stocks._ID};
        String select = "symbol = '" + symbol + "'";
        Cursor cursor = cr.query(Stocker.Stocks.CONTENT_URI, projection, select, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                id = cursor.getInt(cursor.getColumnIndexOrThrow(Stocker.Stocks._ID));
            }
            cursor.close();
        }
        if (id != -1) {
            Uri uri = ContentUris.withAppendedId(Stocker.Stocks.CONTENT_URI, id);
            ContentValues values = new ContentValues();
            values.put(Stocker.Stocks.CHANGE, change);
            if (cr.update(uri, values, null, null) > 0) {
                mAdapter.updateStock(symbol, change);
            }
        }
    }

    private SwipeRefreshLayout.OnRefreshListener
            mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Log.d(TAG, "manual refresh");
            ContentResolver cr = getActivity().getContentResolver();
            String[] projection = new String[]{Stocker.Stocks.SYMBOL, Stocker.Stocks.CHANGE};
            Cursor c = cr.query(Stocker.Stocks.CONTENT_URI, projection, null, null, null);
            if (c != null) {
                if (c.getCount() > 0) {
                    String[] symbols = new String[c.getCount()];
                    int[] changes = new int[c.getCount()];
                    int i = 0;
                    while(c.moveToNext()) {
                        symbols[i] = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.SYMBOL));
                        changes[i] = c.getInt(c.getColumnIndexOrThrow(Stocker.Stocks.CHANGE));
                        i++;
                    }
                    mTextViewStatus.setText(R.string.status_updating);
                    StockService.getStocks(getActivity(), symbols, changes);
                }
                c.close();
            }
        }
    };

    private void loadAllFromDatabase()  {
        ContentResolver res = mContext.getContentResolver();
        Cursor c = res.query(Stocker.Stocks.CONTENT_URI,
                Stocker.Stocks.PROJECTION_ALL, null, null, null);
        if (c != null) {
            ArrayList<Stock> stocks = new ArrayList<Stock>(c.getCount());
            if (c.getCount() > 0) {
                while (c.moveToNext()) {
                    stocks.add(cursorToStock(c));
                }
            }
            c.close();
            mAdapter.changeStocks(stocks);
        }
    }

    private Stock cursorToStock(Cursor c) {
        Stock s = new Stock();
        s.name = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.NAME));
        s.symbol = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.SYMBOL));
        s.currency = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.CURRENCY));
        s.price = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.PRICE));
        s.change = c.getInt(c.getColumnIndexOrThrow(Stocker.Stocks.CHANGE));
        return s;
    }

    private ContentObserver mDatabaseObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            loadAllFromDatabase();
            mSpinner.setRefreshing(false);
            setNowAsLastUpdatedTime();
            refreshViews();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            loadAllFromDatabase();
            mSpinner.setRefreshing(false);
            setNowAsLastUpdatedTime();
            refreshViews();
        }
    };

    public void setConnected(boolean connected) {
        mConnected = connected;
        if (mConnected) {
            refreshViews();
        } else {
            mTextViewStatus.setText(R.string.status_internet_down);
        }
    }

    private long getLastUpdatedTime() {
        SharedPreferences prefs = getActivity().getSharedPreferences("stocker", 0);
        return prefs.getLong("key_last_time", 0);
    }

    private void setNowAsLastUpdatedTime() {
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("stocker", 0).edit();
        editor.putLong("key_last_time", System.currentTimeMillis());
        editor.commit();
    }

    private void refreshViews() {
        if (mAdapter.getItems() > 0) {
            mTextViewAddMsg.setVisibility(View.GONE);
            mStatusLayout.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
            long lastDuration = System.currentTimeMillis() - getLastUpdatedTime();
            long minutes = lastDuration / (1 * 60 * 1000);
            String sts = null;
            if (minutes < 1) {
                sts = getString(R.string.status_updated) + " " + getString(R.string.status_just_now);
            } else if (1 <= minutes && minutes < 2) {
                sts = getString(R.string.status_updated) + " " + getString(R.string.status_minute);
            } else if (2 <= minutes && minutes < 10){
                sts = getString(R.string.status_updated) + " " + minutes + " "
                        + getString(R.string.status_minutes);
            } else {
                sts = getString(R.string.status_updated) + " " + getString(R.string.status_while_ago);
            }
            mTextViewStatus.setText(sts);
        } else {
            mTextViewAddMsg.setVisibility(View.VISIBLE);
            mStatusLayout.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.GONE);
        }
    }

    private void initViews(View v) {
        mRecyclerView = (RecyclerView) v.findViewById(R.id.rvStocks);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            LinearLayoutManager linearLayoutMgr = new LinearLayoutManager(v.getContext());
            //linearLayoutMgr.setReverseLayout(true);
            mRecyclerView.setLayoutManager(linearLayoutMgr);
        } else {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(v.getContext(), 2);
            mRecyclerView.setLayoutManager(gridLayoutManager);
        }
        mRecyclerView.setHasFixedSize(true);
        mStatusLayout = (View) v.findViewById(R.id.layout_status);
        mTextViewAddMsg = (TextView) v.findViewById(R.id.tvAddStockMsg);
        mTextViewStatus = (TextView) v.findViewById(R.id.tvStatus);
        mSpinner = (SwipeRefreshLayout) v.findViewById(R.id.swipeRefreshLayout);
        //mSpinner.setColorSchemeColors(R.color.stocker_primary); // FIXME doesn't work - why?
        mSpinner.setColorScheme(R.color.stocker_primary);
        mSpinner.setOnRefreshListener(mRefreshListener);
    }
}
