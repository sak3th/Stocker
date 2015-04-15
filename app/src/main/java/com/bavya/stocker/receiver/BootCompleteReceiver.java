package com.bavya.stocker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bavya.stocker.service.StockService;

public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Stocker", intent.getAction());
        StockService.startOnBootComplete(context);
    }
}
