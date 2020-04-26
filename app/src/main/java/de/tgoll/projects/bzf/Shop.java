package de.tgoll.projects.bzf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class Shop implements PurchasesUpdatedListener {

    private static final String TIMESTAMP = "Shop-Last-Shown";
    static final String SKU_DARK_MODE = "dark_mode";
    private static final List<String> SKUs = Collections.singletonList(SKU_DARK_MODE);

    private Activity context;
    private SharedPreferences settings;
    private SimpleDateFormat format;
    private Dialog dialog;
    private Dialog loader;
    private BillingClient billing;
    private boolean loading;
    private View view;
    private View container;

    @SuppressLint({"InflateWarnings", "InflateParams"})
    Shop(@NonNull Activity context) {
        this.context = context;
        container = context.findViewById(R.id.navigation);
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        view = LayoutInflater.from(context).inflate(R.layout.dialog_shop, null);
        billing = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build();
        dialog = new MaterialAlertDialogBuilder(context)
            .setTitle(context.getResources().getString(R.string.shop_title))
            .setView(view)
            .setCancelable(true)
            .setPositiveButton(
                context.getResources().getString(R.string.shop_btn_ok),
                (dialog, btn) -> updateStampInSettings()
            )
            .create();
        loader = new MaterialAlertDialogBuilder(context)
                .setView(R.layout.dialog_loading)
                .setCancelable(false)
                .setBackground(new ColorDrawable(Color.TRANSPARENT))
                .create();
    }

    private void setLoading(boolean loading, boolean showDelayed) {
        this.loading = loading;
        if (loading && !showDelayed) loader.show();
        else loader.dismiss();
    }

    void show(boolean delayed) {

        setLoading(true, delayed);

        billing.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
               if (isErroneous(result, !delayed)) {
                   setLoading(false, delayed);
                   return;
               }

                // Request the list of In-App purchases via the Google Billing API
                SkuDetailsParams params = SkuDetailsParams
                    .newBuilder()
                    .setType(BillingClient.SkuType.INAPP)
                    .setSkusList(SKUs)
                    .build();
                billing.querySkuDetailsAsync(params, (result2, products) -> {
                    setLoading(false, delayed);
                    if (isErroneous(result2, !delayed)) return;

                    // Synchronize the state of purchased items with local cache in preferences
                    synchronizePurchasesWithSettings();

                    // If the user already owns all products, don't bother him with annoying popups
                    if (areAllPurchased(products)) return;

                    // We finally ready to show the Shop dialog
                    dialog.show();

                    // Handle Product individual updating
                    onProductsFetched(products);
                });
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.e("BZF", "Could not connect to Google Play In App Purchases Server");
            }
        });
    }

    private boolean isErroneous(BillingResult result, boolean showSnackbar) {
        int code = result.getResponseCode();
        if (code ==  BillingClient.BillingResponseCode.OK) return false;

        String message;
        Log.w("BZF", "Error with Google Play In App Purchases: " + result.getDebugMessage() + "(" + code + ")");
        if (!showSnackbar) return true;
        switch (code) {
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:   message = context.getString(R.string.shop_warn_service_unavailable); break;
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:   message = context.getString(R.string.shop_warn_billing_unavailable, code);   break;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:    message = context.getString(R.string.shop_warn_item_already_owned, code);    break;
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED: message = context.getString(R.string.shop_warn_feature_not_supported, code, result.getDebugMessage()); break;
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:      message = context.getString(R.string.shop_warn_feature_unavailable, code, result.getDebugMessage());   break;
            default: message = context.getString(R.string.shop_warn_general_error, code, result.getDebugMessage());
        }
        Snackbar.make(container, message, Snackbar.LENGTH_LONG).show();
        return true;
    }

    private boolean areAllPurchased(List<SkuDetails> products) {
        for (SkuDetails product : products) {
            if (!isPurchased(product.getSku())) return false;
        }
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isPurchased(String sku) {
        return isPurchased(settings, sku);
    }

    static boolean isPurchased(SharedPreferences settings, String sku) {
        return settings.contains(sku);
    }

    private void synchronizePurchasesWithSettings() {
        SharedPreferences.Editor editor = settings.edit();
        Purchase.PurchasesResult result = billing.queryPurchases(BillingClient.SkuType.INAPP);
        for(String sku : SKUs) {
            // For all possible IAPs...
            String token = null;
            for(Purchase purchase : result.getPurchasesList()) {
                // For all purchased IAPs ...

                // Save the token
                if (purchase.getSku().equals(sku)) {
                    token = purchase.getPurchaseToken();
                    Log.i("BZF", "User owns IAP \"" + purchase.getSku() + "\" with token " + purchase.getPurchaseToken());
                }
            }
            // If the user owns this IAP, cache it in preferences
            if (token != null) editor.putString(sku, token);

            // Otherwise remove this possible product from preferences
            else editor.remove(sku);
        }
        editor.apply();
    }

    private void onProductsFetched(List<SkuDetails> products) {
        for (SkuDetails product : products) {

            @IdRes int id;
            switch (product.getSku()) {
                case SKU_DARK_MODE: id = R.id.shop_btn_dark_mode; break;
                default: Log.w("BZF", "New Product with SKU \"" + product.getSku() + "\" found, which cannot be handled right now...");
                    continue;
            }

            BillingFlowParams flow = BillingFlowParams.newBuilder().setSkuDetails(product).build();
            Button button = view.findViewById(id);
            button.setEnabled(!isPurchased(product.getSku()));
            button.setOnClickListener(v -> billing.launchBillingFlow(context, flow));
            button.setText(product.getPrice());
        }
    }

    boolean isTimeToShowAgain() {
        try {
            Date lastShown = format.parse(settings.getString(TIMESTAMP, "2000-01-01"));
            if (lastShown == null) return true;
            Calendar c = Calendar.getInstance(Locale.getDefault());
            c.setTime(lastShown);
            c.add(Calendar.DATE, 14);
            Date shouldShowAgain = c.getTime();
            Log.i("BZF", "Shop Dialog last shown on " + format.format(lastShown) + ", will show again after " + format.format(shouldShowAgain));
            return new Date().after(shouldShowAgain);
        } catch (ParseException e) {
            return true;
        }
    }

    private void updateStampInSettings() {
        if (loading) return;
        settings.edit().putString(TIMESTAMP, format.format(new Date())).apply();
    }

    private void handlePurchase(Purchase purchase) {
        if (!purchase.getSku().equals(SKU_DARK_MODE)) {
            Log.w("BZF", "Unknown purchase: " + purchase.getSku());
            return;
        }
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
            Log.w("BZF", "Purchase \"" + purchase.getSku() + "\" is not yet in PURCHASED state");
            return;
        }

        if (!purchase.isAcknowledged()) {
            Log.i("BZF", "Purchase \"" + purchase.getSku() + "\" is not yet in acknowledged. Doing that now...");
            billing.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
                billingResult -> Log.i("BZF", "Purchase \"" + purchase.getSku() + "\" now acknowledged")
            );
        }

        Log.i("BZF", "Saving purchase \""+ SKU_DARK_MODE +"\" in Preferences: " + purchase.getPurchaseToken());
        settings.edit().putString(SKU_DARK_MODE, purchase.getPurchaseToken()).apply();

        dialog.dismiss();
        updateStampInSettings();
    }

    @Override
    public void onPurchasesUpdated(BillingResult result, @Nullable List<Purchase> purchases) {
        int code = result.getResponseCode();
        if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Log.w("BZF", "Could no complete purchase(s): " + result.getDebugMessage());
            Snackbar.make(container, context.getString(R.string.shop_warn_item_already_owned, code), Snackbar.LENGTH_LONG).show();
            return;
        }
        if (code != BillingClient.BillingResponseCode.OK) {
            Log.w("BZF", "Could no complete purchase(s): " + result.getDebugMessage());
            return;
        }
        if (purchases == null) {
            Log.w("BZF", "Could no complete purchase(s) because server send null list");
            return;
        }
        for (Purchase purchase : purchases) handlePurchase(purchase);
    }


}
