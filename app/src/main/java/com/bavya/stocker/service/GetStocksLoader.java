package com.bavya.stocker.service;


import android.content.AsyncTaskLoader;
import android.content.Context;

import com.bavya.stocker.model.Stock;

import java.util.ArrayList;
import java.util.Map;

import yahoofinance.YahooFinance;

public class GetStocksLoader extends AsyncTaskLoader<ArrayList<Stock>> {
    private String[] mSymbols;
    private ArrayList<Stock> mStocks;

    public GetStocksLoader(Context context, String[] symbols) {
        super(context);
        mSymbols = symbols;
    }

    @Override
    public ArrayList<Stock> loadInBackground() {
        ArrayList<Stock> stocks = new ArrayList<Stock>();;
        try {
            Map<String, yahoofinance.Stock> stockMap = YahooFinance.get(mSymbols);
            for (String symbol : mSymbols) {
                yahoofinance.Stock stk = stockMap.get(symbol);
                if (!stk.getName().equals("N/A")) {
                    stocks.add(new Stock(stk));
                }
            }
        } catch (Exception e) {

        } finally {
            return stocks;
        }
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override public void deliverResult(ArrayList<Stock> stocks) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
        }
        mStocks = stocks;
        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(stocks);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override protected void onStartLoading() {
        if (mStocks != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mStocks);
        }
        if (takeContentChanged() || mStocks == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(ArrayList<Stock> stocks) {
        super.onCanceled(stocks);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();
        // Ensure the loader is stopped
        onStopLoading();
        if (mStocks != null) {
            mStocks = null;
        }
        if (mSymbols != null) {
            mSymbols = null;
        }
    }
}
