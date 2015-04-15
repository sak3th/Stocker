package com.bavya.stocker.activity;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bavya.stocker.R;
import com.bavya.stocker.model.Stock;
import com.bavya.stocker.view.StockAdapter;


public class HomeActivity extends FragmentActivity implements View.OnClickListener,
        AddStockFragment.OnAddStockListener, EditStockFragment.OnUpdateStockListener,
        StockAdapter.OnStockLongClickListener {

    private ImageButton mButton;
    private StocksFragment mStocksFragment;

    private ConnectivityManager mConnMgr;
    private boolean mConnected, mFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setImmersive();
        mButton = (ImageButton) findViewById(R.id.fabAdd);
        mButton.setOnClickListener(this);
        mConnMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        mConnected = isConnected();
        registerReceiver(mConnectivityReceiver,
                new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
            StocksFragment fragment = new StocksFragment();
            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mConnectivityReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setImmersive();
        mButton.setVisibility(mConnected? View.VISIBLE : View.GONE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setImmersive();
    }

    @Override
    public void onClick(View v) {
        if (mConnected) {
            showAddDialog();
        } else {
            Toast.makeText(getApplicationContext(), "Cannot add stock now!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddDialog() {
        FragmentManager fragmentManager = getFragmentManager();
        AddStockFragment newFragment = new AddStockFragment();
        newFragment.show(fragmentManager, "dialog");
    }

    private void showEditDialog(Stock stock) {
        FragmentManager fragmentManager = getFragmentManager();
        EditStockFragment newFragment = new EditStockFragment();
        Bundle b = new Bundle();
        b.putParcelable("key_stock", stock);
        newFragment.setArguments(b);
        newFragment.show(fragmentManager, "dialog");
    }

    public void setStocksFragment(StocksFragment s) {
        mStocksFragment = s;
    }

    @Override
    public void onAddStock(String symbol, int change) {
        mStocksFragment.addStock(symbol, change);
    }

    @Override
    public void onUpdateStock(String symbol, int change) {
        mStocksFragment.updateStock(symbol, change);
    }

    @Override
    public void onDeleteStock(Stock stock) {
        mStocksFragment.removeStock(stock);
    }

    @Override
    public void onStockLongClick(Stock stock) {
        Log.d("HomeActivity", "onStockLongClick: " + stock);
        showEditDialog(stock);
    }

    private void animateButton(final boolean visible) {
        Animation anim = AnimationUtils.loadAnimation(getApplicationContext(),
                (visible ? R.anim.slide_up : R.anim.slide_down));
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                mButton.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        mButton.startAnimation(anim);
    }

    private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean connected = isConnected();
            if (mConnected != connected || mFirstTime) {
                mStocksFragment.setConnected(connected);
                mConnected = connected;
                animateButton(mConnected);
            }
            mFirstTime = false;
        }
    };

    public boolean isConnected() {
        NetworkInfo info = mConnMgr.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            return true;
        }
        return false;
    }

    public void setImmersive() {
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;

        // Navigation bar hiding:  Backwards compatible to ICS.
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        // Immersive mode: Backward compatible to KitKat.
        // Note that this flag doesn't do anything by itself, it only augments the behavior
        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
        // all three flags are being toggled together.
        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
        // Sticky immersive mode differs in that it makes the navigation and status bars
        // semi-transparent, and the UI flag does not get cleared when the user interacts with
        // the screen.
        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
}
