package me.nico9889.switchsimplescreenshot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.nico9889.switchsimplescreenshot.qrcode.QrAnalyzer;

public class MainActivity extends AppCompatActivity {
    private ExecutorService cameraExecutor;
    private ImageAnalysis imageAnalysis;
    private final String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_MEDIA_LOCATION   // FIXME: this work only on Android >=Q
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, PERMISSIONS, 10);

        }
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int REQUEST_CODE_PERMISSIONS = 10;
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, R.string.permissionDenied, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            Preview preview = new Preview.Builder().build();
            PreviewView viewFinder = findViewById(R.id.viewFinder);
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());


            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            imageAnalysis = new ImageAnalysis.Builder()
                    .setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
            QrAnalyzer qrAnalyzer = new QrAnalyzer();
            qrAnalyzer.setConnect(this::connectAndDownload);
            imageAnalysis.setAnalyzer(cameraExecutor, qrAnalyzer);
            try {
                if (cameraProvider != null) {
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                }
            } catch (Exception e) {
                Log.e("MainActivity:CameraProvider", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private boolean allPermissionsGranted() {
        boolean allGranted = true;
        for (String permission : PERMISSIONS) {
            System.out.println(permission);
            System.out.println(ContextCompat.checkSelfPermission(this, permission));
            allGranted &= ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    private void connectAndDownload(@NotNull String ssid, @NotNull String password) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // TODO: handle network change on devices with version less that Android Q
        } else {
            // If anyone from Google is reading this for some reasons I want let you know that I
            // hate this, not only as a developer but as a user too... This add a lot of extra time
            // because Android search for networks (most of the times this requires an entire
            // minute) and the user need to select it manually...
            // FIXME: find a better way to handle the network change... I hope there's one :-/
            WifiNetworkSpecifier wifiNetworkSpecifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .build();
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(wifiNetworkSpecifier)
                    .build();
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    onConnectionEstablished(network);
                }


            };
            connectivityManager.requestNetwork(networkRequest, networkCallback);
        }

        // FIXME: if the connection fails we need to restart the QR search!!!
        imageAnalysis.clearAnalyzer();
    }

    private void onConnectionEstablished(Network network) {
        Intent downloadIntent = new Intent(this, ScreenshotDownloadActivity.class);
        downloadIntent.putExtra("MainActivity.network", network);
        startActivity(downloadIntent);
    }
}