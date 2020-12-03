package me.nico9889.switchsimplescreenshot.qrcode;

import android.annotation.SuppressLint;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.Objects;
import java.util.function.BiConsumer;


// TODO: throttle QR Analysis to reduce CPU usage
public class QrAnalyzer implements ImageAnalysis.Analyzer {
    private boolean scanning = false;
    private BiConsumer<String, String> connect = null;
    private final BarcodeScannerOptions options =
            new BarcodeScannerOptions.Builder()
                    .build();
    private final BarcodeScanner scanner = BarcodeScanning.getClient(options);
    private String ssid = "";
    private String password = "";
    int encType = -1;

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        @SuppressLint("UnsafeExperimentalUsageError")
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            if (!scanning) {    // TODO: I'm not sure that this works as intended
                scanning = true;
                scanner.process(InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees()))
                        .addOnSuccessListener((barcodes) -> {
                            imageProxy.close();
                            for (Barcode barcode : barcodes) {
                                int valueType = barcode.getValueType();
                                if (valueType == Barcode.TYPE_WIFI) {
                                    ssid = Objects.requireNonNull(barcode.getWifi()).getSsid();
                                    password = barcode.getWifi().getPassword();
                                    encType = barcode.getWifi().getEncryptionType();
                                    connect.accept(ssid, password);
                                }
                            }
                            scanning = false;
                        })
                        .addOnFailureListener((exception) -> {
                            imageProxy.close();
                            scanning = false;
                            Log.e("QrAnalyzer:analyze", "Error on scanning QrCode", exception);
                        });
            }else{
                Log.d("QrAnalyzer:analyze", "Dropping frame");
                imageProxy.close();
            }
        }
    }

    public void setConnect(BiConsumer<String, String> connect){
        this.connect = connect;
    }
}