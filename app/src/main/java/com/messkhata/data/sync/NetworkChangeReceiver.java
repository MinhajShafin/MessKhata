package com.messkhata.data.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Monitors network connectivity changes and triggers offline queue processing
 * when connection is restored.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkChangeReceiver";
    private static ConnectivityManager.NetworkCallback networkCallback;
    private static boolean wasConnected = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        // This is for older Android versions using broadcast receiver
        if (isNetworkAvailable(context)) {
            Log.d(TAG, "Network connected - processing offline queue");
            processOfflineQueue(context);
        }
    }

    /**
     * Register network callback for modern Android versions
     */
    public static void registerNetworkCallback(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null)
            return;

        if (networkCallback != null) {
            // Already registered
            return;
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.d(TAG, "Network available - processing offline queue");

                // Only process if we were disconnected before
                if (!wasConnected) {
                    wasConnected = true;
                    processOfflineQueue(context);
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.d(TAG, "Network lost");
                wasConnected = false;
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                    @NonNull NetworkCapabilities capabilities) {
                super.onCapabilitiesChanged(network, capabilities);

                boolean hasInternet = capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET);
                boolean isValidated = capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED);

                if (hasInternet && isValidated && !wasConnected) {
                    Log.d(TAG, "Network capabilities restored - processing offline queue");
                    wasConnected = true;
                    processOfflineQueue(context);
                }
            }
        };

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        Log.d(TAG, "Network callback registered");
    }

    /**
     * Unregister network callback
     */
    public static void unregisterNetworkCallback(Context context) {
        if (networkCallback == null)
            return;

        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
                Log.d(TAG, "Network callback unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
    }

    /**
     * Check if network is available
     */
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null)
            return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null)
            return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /**
     * Process the offline queue
     */
    private static void processOfflineQueue(Context context) {
        OfflineQueueManager queueManager = OfflineQueueManager.getInstance(context);

        if (queueManager.hasPendingOperations()) {
            int pendingCount = queueManager.getPendingCount();
            Log.d(TAG, "Processing " + pendingCount + " pending operations");

            queueManager.processQueue(new OfflineQueueManager.QueueProcessCallback() {
                @Override
                public void onQueueProcessed(int successCount, int failureCount) {
                    Log.d(TAG, "Queue processed: " + successCount + " success, " +
                            failureCount + " failed");

                    // Trigger a full sync after processing queue
                    if (successCount > 0) {
                        // Data was synced, now fetch any remote changes
                        // This could be enhanced to only sync specific entities
                    }
                }

                @Override
                public void onQueueEmpty() {
                    Log.d(TAG, "No pending operations in queue");
                }
            });
        }
    }
}
