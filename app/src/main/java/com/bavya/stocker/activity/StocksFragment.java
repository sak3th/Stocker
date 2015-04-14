package com.bavya.stocker.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bavya.stocker.R;
import com.bavya.stocker.model.Stock;
import com.bavya.stocker.provider.Stocker;
import com.bavya.stocker.service.GetStocksLoader;
import com.bavya.stocker.view.StockAdapter;

import java.util.ArrayList;
import java.util.HashMap;


public class StocksFragment extends Fragment {
    private static final String TAG = "StockFragment";

    private static final int LOADER_READ_STOCKS = 0;
    private static final int LOADER_GET_STOCKS = 1;

    private RecyclerView mRecyclerView;
    private StockAdapter mAdapter;
    private TextView mTextViewAddMsg, mTextViewStatus;
    private View mStatusLayout;
    private SwipeRefreshLayout mSpinner;

    private StockAdapter.OnStockLongClickListener mStockLongClickListener;

    private boolean mConnected;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stock_list, container, false);
        initViews(v);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((HomeActivity)activity).setStocksFragment(this);
        mStockLongClickListener = (StockAdapter.OnStockLongClickListener) activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new StockAdapter(mStockLongClickListener);
        mRecyclerView.setAdapter(mAdapter);
        getLoaderManager().initLoader(LOADER_READ_STOCKS, null, mLoadStocksCallbacks);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshViews();
    }

    public void addStock(String symbol, int change) {
        Bundle bundle = new Bundle();
        bundle.putStringArray("key_symbols", new String[] { symbol });
        bundle.putIntArray("key_changes", new int[]{ change });
        Log.d(TAG, "adding stock  " + symbol);
        getLoaderManager().restartLoader(LOADER_GET_STOCKS, bundle, new GetStocksCallbacks());
        mTextViewStatus.setText(getString(R.string.status_fetching) + " " + symbol + "...");
        mStatusLayout.setVisibility(View.VISIBLE);
    }

    public void removeStock(Stock stock) {
        removeStockFromDb(stock);
    }

    public void updateStock(String symbol, int change) {
        updateStockInDb(symbol, change);
    }

    private LoaderManager.LoaderCallbacks<Cursor>
            mLoadStocksCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_READ_STOCKS:
                    Uri baseUri = Stocker.Stocks.CONTENT_URI;
                    // Now create and return a CursorLoader that will take care of
                    // creating a Cursor for the data being displayed.
                    return new CursorLoader(getActivity(), baseUri,
                            Stocker.Stocks.PROJECTION_ALL, null, null, null);
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
            // Swap the new cursor in.  (The framework will take care of closing the
            // old cursor once we return.)
            ArrayList<Stock> stocks = new ArrayList<Stock>();
            if (c != null) {
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    do {
                        Stock s = new Stock();
                        s.name = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.NAME));
                        s.symbol = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.SYMBOL));
                        s.currency = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.CURRENCY));
                        s.price = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.PRICE));
                        s.change = c.getInt(c.getColumnIndexOrThrow(Stocker.Stocks.CHANGE));
                        stocks.add(s);
                    } while (c.moveToNext());
                    mTextViewAddMsg.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mAdapter.changeStocks(stocks);
                    refreshViews();
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            // This is called when the last Cursor provided to onLoadFinished()
            // above is about to be closed.  We need to make sure we are no
            // longer using it.
            mAdapter.changeStocks(null);
            refreshViews();
        }
    };

    private class GetStocksCallbacks implements LoaderManager.LoaderCallbacks<ArrayList<Stock>> {
        private HashMap<String, Integer> mSymbolChangeMap;
        @Override
        public Loader<ArrayList<Stock>> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_GET_STOCKS:
                    mTextViewStatus.setText(R.string.status_updating);
                    String[] symbols = args.getStringArray("key_symbols");
                    int[] changes = args.getIntArray("key_changes");
                    mSymbolChangeMap = new HashMap<String, Integer>(changes.length);
                    for (int i = 0; i < changes.length; i++) {
                        mSymbolChangeMap.put(symbols[i], changes[i]);
                    }
                    return new GetStocksLoader(getActivity(), symbols);
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<ArrayList<Stock>> loader, ArrayList<Stock> stocks) {
            mSpinner.setRefreshing(false);
            String status = null;
            if (stocks.size() > 0) {
                 status = getString(R.string.status_updated) + " "
                        + getString(R.string.status_just_now);
                for (Stock s : stocks) {
                    s.change = mSymbolChangeMap.get(s.symbol);
                    addStockToDb(s);
                }
            } else {
                status = getString(R.string.status_no_data);
            }
            mTextViewStatus.setText(status);
            setNowAsLastUpdatedTime();
        }

        @Override
        public void onLoaderReset(Loader<ArrayList<Stock>> loader) {

        }
    }

    private void addStockToDb(Stock s) {
        ContentResolver cr = getActivity().getContentResolver();
        ContentValues values = new ContentValues();
        values.put(Stocker.Stocks.NAME, s.name);
        values.put(Stocker.Stocks.SYMBOL, s.symbol);
        values.put(Stocker.Stocks.CURRENCY, s.currency);
        values.put(Stocker.Stocks.PRICE, s.price);
        values.put(Stocker.Stocks.CHANGE, s.change);
        int id = findStockInDb(s);
        if (id == -1) {
            cr.insert(Stocker.Stocks.CONTENT_URI, values);
        } else {
            Uri uri = ContentUris.withAppendedId(Stocker.Stocks.CONTENT_URI, id);
            int num = cr.update(uri, values, null, null);
            if (num != 1) {
                Log.e(TAG, "addStockToDb updated " + num + " entries");
            }
        }
        mAdapter.addStock(new Stock(s));
        refreshViews();
    }

    private int findStockInDb(Stock s) {
        int id = -1;
        ContentResolver cr = getActivity().getContentResolver();
        String[] projection = new String[]{Stocker.Stocks._ID};
        String select = "symbol = '" + s.symbol + "'";
        Cursor cursor = cr.query(Stocker.Stocks.CONTENT_URI, projection, select, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                id = cursor.getInt(cursor.getColumnIndexOrThrow(Stocker.Stocks._ID));
            }
            cursor.close();
        }
        return id;
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

    private View.OnClickListener mAddClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(v.getContext(), "Add clicked", Toast.LENGTH_SHORT).show();
        }
    };

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
                    Bundle b = new Bundle();
                    b.putStringArray("key_symbols", symbols);
                    b.putIntArray("key_changes", changes);
                    getLoaderManager().restartLoader(LOADER_GET_STOCKS, b, new GetStocksCallbacks());
                }
                c.close();
            }
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
            Log.d(TAG, "getLastUpdatedTime(): " + getLastUpdatedTime());
            Log.d(TAG, "lastDuration: " + lastDuration);
            Log.d(TAG, "minutes " + minutes);
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
