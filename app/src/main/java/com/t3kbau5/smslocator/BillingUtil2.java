package com.t3kbau5.smslocator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Calendar;
import java.util.List;

/**
 * Created by Ben on 2018-03-03.
 */

public class BillingUtil2 implements PurchasesUpdatedListener, BillingClientStateListener, PurchaseHistoryResponseListener {

    private static final boolean TEST_MODE = false && BuildConfig.DEBUG; //use test mode if this is a debug build and I'm not doing other fancy stuff

    private static final String SKU_PREMIUM = "premium";


    private Activity activity;
    private BillingClient mBillingClient;
    private boolean hasPremium = false;
    private boolean buyingPremium = false;
    private SharedPreferences prefs;
    private FirebaseAnalytics mFirebaseAnalytics;

    public BillingUtil2(MainActivity activity, FirebaseAnalytics fa) {
        this.activity = activity;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        mFirebaseAnalytics = fa;

        mBillingClient = BillingClient.newBuilder(activity).setListener(this).build();
        mBillingClient.startConnection(this);

    }

    public void close() {
        if(mBillingClient.isReady())
            mBillingClient.endConnection();
    }

    public void updatePurchases() {
        mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, this);
    }

    public void updatePurchasesCached() {
        hasPremium = false;

        Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
        List<Purchase> purchases = purchasesResult.getPurchasesList();
        for(Purchase purchase : purchases) {
            if(purchase.getSku().equals(SKU_PREMIUM))
                hasPremium = true;
        }
    }

    public void buyPremium() {
        buyingPremium = true;
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSku(SKU_PREMIUM)
                .setType(BillingClient.SkuType.INAPP)
                .build();
        int responseCode = mBillingClient.launchBillingFlow(activity, flowParams);

    }

    public boolean isConnected(){
        return mBillingClient.isReady();
    }

    public boolean hasPremium(){
        return hasPremium;
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        if (responseCode == BillingClient.BillingResponse.OK
                && purchases != null) {

            hasPremium = false;

            for (Purchase purchase : purchases) {
                switch(purchase.getSku()) {
                    case SKU_PREMIUM:
                        hasPremium = true;
                        if(prefs.getBoolean("smsenabled", false) && prefs.getBoolean("admin", false))
                            ((MainActivity) activity).requestAdmin();

                        //((MainActivity) activity).updateStates();
                        break;
                    default:
                        // do nothing, for now
                }
            }

            // Support Freemium Stuff
            if(prefs.getBoolean("premium_is_freemium", false) && prefs.getLong("freemium_expiry", 0L) > Calendar.getInstance().getTimeInMillis()) {
                hasPremium = true;
            }else{
                prefs.edit().putBoolean("premium_is_freemium", false).apply();
            }
            prefs.edit().putBoolean("premium", hasPremium).apply();
            if(buyingPremium && hasPremium){
                ((MainActivity) activity).finish();
                ((MainActivity) activity).startActivity(((MainActivity) activity).getIntent());
            }
            buyingPremium = false;
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            CustomToast.makeText(activity, Utils.getStr(activity, R.string.error_purchase), CustomToast.LENGTH_LONG, 1);
            mFirebaseAnalytics.logEvent("cancel_purchase", null);
        } else {
            // Handle any other error codes.
        }

    }

    @Override
    public void onBillingSetupFinished(int responseCode) {
        if (responseCode == BillingClient.BillingResponse.OK) {
            updatePurchases();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.
    }

    @Override
    public void onPurchaseHistoryResponse(int responseCode, List<Purchase> purchasesList) {
        if(!TEST_MODE)
            onPurchasesUpdated(responseCode, purchasesList);
        else if(responseCode == BillingClient.BillingResponse.OK && purchasesList != null){
            for(Purchase purchase : purchasesList) {
                if(purchase.getSku().equals(SKU_PREMIUM)) {
                    mBillingClient.consumeAsync(purchase.getPurchaseToken(), new ConsumeResponseListener() {
                        @Override
                        public void onConsumeResponse(int responseCode, String purchaseToken) {
                            if(responseCode == BillingClient.BillingResponse.OK) {
                                CustomToast.makeText(activity, "Premium consumed", Toast.LENGTH_LONG, 2).show();
                            }
                        }
                    });
                }
            }
        }
    }
}
