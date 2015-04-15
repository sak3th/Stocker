package com.bavya.stocker.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.bavya.stocker.R;
import com.bavya.stocker.activity.HomeActivity;
import com.bavya.stocker.model.Stock;
import com.bavya.stocker.provider.Stocker;
import com.bavya.stocker.utils.Styler;

import java.util.HashMap;
import java.util.Map;

import yahoofinance.YahooFinance;


public class StockService extends IntentService {
    private static final String TAG = "StockerService";

    public static final String ACTION_BOOT = Intent.ACTION_BOOT_COMPLETED;
    public static final String ACTION_GET_STOCKS = "bavya.stocker.action.GET_STOCKS";
    public static final String ACTION_REFRESH = "bavya.stocker.action.REFRESH_STOCKS";

    public static final String ACTION_STOCK_NOT_FOUND = "bavya.stocker.action.STOCK_NOT_FOUND";

    private static boolean BG_REFRESH_ENABLED = false;

    public static void startOnBootComplete(Context context) {
        Intent intent = new Intent(context, StockService.class);
        intent.setAction(ACTION_BOOT);
        context.startService(intent);
    }

    public static void getStocks(Context context, String[] symbols, int[] changes) {
        Intent intent = new Intent(context, StockService.class);
        intent.putExtra("key_symbols", symbols);
        intent.putExtra("key_changes", changes);
        intent.setAction(ACTION_GET_STOCKS);
        context.startService(intent);
    }

    public static void setBackgroundRefresh(Context context, boolean set) {
        if (BG_REFRESH_ENABLED != set) {
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(context, StockService.class);
            intent.setAction(ACTION_REFRESH);
            PendingIntent pendingIntent = PendingIntent.getService(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            if (set) {
                alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + (2 * 60 * 1000),
                        2 * 60 * 1000,
                        pendingIntent);
                BG_REFRESH_ENABLED = true;
                Log.d(TAG, "alarm set to watch stocks in bg");
            } else {
                alarmMgr.cancel(pendingIntent);
                BG_REFRESH_ENABLED = false;
                Log.d(TAG, "stock watcher alarm removed");
            }
        }
    }

    public StockService() {
        super("StockWatcher");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "Received " + action);
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                handleBoot();
                Toast.makeText(getApplicationContext(), "BOOT COMPLETE", Toast.LENGTH_LONG).show();
            } else if (ACTION_GET_STOCKS.equals(action)) {
                String[] symbols = intent.getStringArrayExtra("key_symbols");
                int[] changes = intent.getIntArrayExtra("key_changes");
                handleGetStocks(symbols, changes);
            } else if (ACTION_REFRESH.equals(action)) {
                handleActionRefresh();
            }
        }
    }

    private void handleBoot() {
        boolean refresh = false;
        ContentResolver resolver  = getContentResolver();
        Cursor c = resolver.query(Stocker.Stocks.CONTENT_URI,
                Stocker.Stocks.PROJECTION_ALL, null, null, null);
        if (c != null) {
            if (c.getCount() > 0) {
                refresh = true;
            }
            c.close();
        }
        if (refresh) {
            handleActionRefresh();
        }
        setBackgroundRefresh(getApplicationContext(), refresh);
    }

    private void handleGetStocks(String[] symbols, int[] changes) {
        HashMap<String, Integer> changeMap = new HashMap<String, Integer>();
        for (int i = 0; i < symbols.length; i++) {
            changeMap.put(symbols[i], changes[i]);
        }
        Map<String, yahoofinance.Stock> resMap = YahooFinance.get(symbols);
        for (Map.Entry<String, yahoofinance.Stock> entry : resMap.entrySet()) {
            yahoofinance.Stock stk = entry.getValue();
            if (stk.getName().equals("N/A")) {
                Intent intent = new Intent(ACTION_STOCK_NOT_FOUND);
                intent.putExtra("key_symbol", stk.getSymbol());
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            } else {
                Stock stock = new Stock(stk);
                stock.change = changeMap.get(entry.getKey());
                updateStockToDb(stock);
            }
        }
    }

    private void handleActionRefresh() {
        ContentResolver resolver  = getContentResolver();
        Cursor c = resolver.query(Stocker.Stocks.CONTENT_URI,
                Stocker.Stocks.PROJECTION_ALL, null, null, null);
        if (c != null) {
            if (c.getCount() > 0) {
                HashMap<String, Stock> stockMap = new HashMap<>();
                while (c.moveToNext()) {
                    Stock s = cursorToStock(c);
                    stockMap.put(s.symbol, s);
                }
                String[] symbols = stockMap.keySet().toArray(new String[stockMap.size()]);
                Map<String, yahoofinance.Stock> resMap = YahooFinance.get(symbols);
                for (String symbol : symbols) {
                    yahoofinance.Stock stk = resMap.get(symbol);
                    if (!stk.getName().equals("N/A")) {
                        Stock old = stockMap.get(symbol);
                        Stock cur = new Stock(stk);
                        cur.change = old.change;
                        updateStockToDb(cur);
                        notifyChanges(old, cur);
                    }
                }
            } else {
                setBackgroundRefresh(getApplicationContext(), false);
            }
            c.close();
        }
    }

    private void notifyChanges(Stock old, Stock cur) {
        double changePer = ((Double.valueOf(cur.price) - Double.valueOf(old.price))
                / Double.valueOf(old.price));
        Resources res = getResources();
        NotificationManager notiMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        CharSequence text = null;
        if (old.change < 0) {
            if (changePer <= (old.change/100)) {
                text = styledtextDropped(cur, Math.abs(changePer * 100));
            }
        } else if (old.change > 0) {
            if (changePer >= (old.change/100)) {
                text = styledTextRisen(cur, changePer * 100);
            }
        }
        if (text != null) {
            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                    intent, 0);
            Notification noti = new NotificationCompat.Builder(this)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(Notification.PRIORITY_MAX)
                    .build();
            noti.contentView = new RemoteViews(this.getPackageName(), R.layout.noti_layout);
            noti.contentView.setTextViewText(R.id.textViewNotif, text);
            notiMgr.notify(cur.symbol.hashCode(), noti);
        } else {
            notiMgr.cancel(cur.symbol.hashCode());
        }
    }

    private void updateStockToDb(Stock s) {
        ContentResolver cr = getContentResolver();
        ContentValues values = stockToValues(s);
        int id = findStockInDb(s);
        if (id == -1) {
            cr.insert(Stocker.Stocks.CONTENT_URI, values);
        } else {
            Uri uri = ContentUris.withAppendedId(Stocker.Stocks.CONTENT_URI, id);
            if (cr.update(uri, values, null, null) > 0) {
                // TODO notify update
            }
        }
    }

    private int findStockInDb(Stock s) {
        int id = -1;
        ContentResolver cr = getContentResolver();
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

    private Stock cursorToStock(Cursor c) {
        Stock s = new Stock();
        s.name = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.NAME));
        s.symbol = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.SYMBOL));
        s.currency = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.CURRENCY));
        s.price = c.getString(c.getColumnIndexOrThrow(Stocker.Stocks.PRICE));
        s.change = c.getInt(c.getColumnIndexOrThrow(Stocker.Stocks.CHANGE));
        return s;
    }

    private ContentValues stockToValues(Stock s) {
        ContentValues values = new ContentValues();
        values.put(Stocker.Stocks.NAME, s.name);
        values.put(Stocker.Stocks.SYMBOL, s.symbol);
        values.put(Stocker.Stocks.CURRENCY, s.currency);
        values.put(Stocker.Stocks.PRICE, s.price);
        values.put(Stocker.Stocks.CHANGE, s.change);
        return values;
    }

    private CharSequence styledTextRisen(Stock cur, double change) {
        Resources res = getResources();
        return Styler.concat(
                Styler.color(color(R.color.noti_black), Styler.bold(cur.symbol)),
                Styler.color(color(R.color.noti_black), res.getString(R.string.noti_txt_has)),
                Styler.color(color(R.color.noti_green), res.getString(R.string.noti_txt_risen)),
                Styler.color(color(R.color.noti_black), res.getString(R.string.noti_txt_to)),
                Styler.color(color(R.color.noti_black), Styler.bold((cur.currency + cur.price))),
                Styler.color(color(R.color.noti_black), (" (" + change + "%)")));
    }

    private CharSequence styledtextDropped(Stock cur, double change) {
        Resources res = getResources();
        return Styler.concat(
                Styler.color(color(R.color.noti_black), Styler.bold(cur.symbol)),
                Styler.color(color(R.color.noti_black), res.getString(R.string.noti_txt_has)),
                Styler.color(color(R.color.noti_red), res.getString(R.string.noti_txt_dropped)),
                Styler.color(color(R.color.noti_black), res.getString(R.string.noti_txt_to)),
                Styler.color(color(R.color.noti_black), Styler.bold((cur.currency + cur.price))),
                Styler.color(color(R.color.noti_black), (" (" + change + "%)")));
    }

    private int color(int id) {
        return getResources().getColor(id);
    }

}