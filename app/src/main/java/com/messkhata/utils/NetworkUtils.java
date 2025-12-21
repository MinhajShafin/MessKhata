package com.messkhata.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Utility class for monitoring network connectivity.
 * Provides real-time updates about network availability.
 */
public class NetworkUtils {

    private static NetworkUtils instance;
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isNetworkAvailable = new MutableLiveData<>(false);
    private ConnectivityManager.NetworkCallback networkCallback;

    private NetworkUtils(Context context) {
        connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        initNetworkCallback();
        checkCurrentNetwork();
    }

    public static synchronized NetworkUtils getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkUtils(context.getApplicationContext());
        }
        return instance;
    }

    private void initNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                isNetworkAvailable.postValue(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                isNetworkAvailable.postValue(false);
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                                               @NonNull NetworkCapabilities capabilities) {
                boolean hasInternet = capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET);
                boolean isValidated = capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                isNetworkAvailable.postValue(hasInternet && isValidated);
            }
        };

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    private void checkCurrentNetwork() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(activeNetwork);
            if (capabilities != null) {
                boolean hasInternet = capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET);
                boolean isValidated = capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                isNetworkAvailable.postValue(hasInternet && isValidated);
            } else {
                isNetworkAvailable.postValue(false);
            }
        } else {
            isNetworkAvailable.postValue(false);
        }
    }


}
