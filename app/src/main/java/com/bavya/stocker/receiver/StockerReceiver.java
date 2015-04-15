package com.bavya.stocker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import com.bavya.stocker.service.StockService;

public class StockerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Stocker", intent.getAction());
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            StockService.startOnBootComplete(context);
        } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            StockService.connectivityChanged(context);
        }
    }
}
