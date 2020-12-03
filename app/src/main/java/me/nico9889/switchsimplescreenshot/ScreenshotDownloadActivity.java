package me.nico9889.switchsimplescreenshot;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class ScreenshotDownloadActivity extends AppCompatActivity {
    private final String TAG = "ScreenshotDownloadActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: print on screen something better than "TextView" :')
        setContentView(R.layout.activity_screenshotdownload);
        Log.d("ScreenshotDownloadActivity.response", "Activity changed");
        Network network = getIntent().getParcelableExtra("MainActivity.network");
        executeJSONRequest(network);
    }

    public void executeJSONRequest(Network network) {
        // Since the download is a Network operation and AsyncTask is deprecated we start a new
        // Thread to handle it.
        // TODO: find a better way to handle downloads and update UI to report the user that the
        //  download has ended
        Thread download_data = new Thread(() -> {
            try {
                // Here we download from the Nintendo Switch the JSON containing translation
                // strings and the names of the files available to download.
                URL data_url = new URL("http://192.168.0.1/data.json");
                URLConnection conn = network.openConnection(data_url);
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        conn.getInputStream()));
                JSONObject json_data = new JSONObject(in.readLine());
                in.close();

                // FIXME: move files download from here. The idea is to select manually the files
                //  to download
                // We are not interested in translation strings, so we get only the FileNames
                if (json_data.has("FileNames")) {
                    JSONArray files = json_data.getJSONArray("FileNames");
                    if (files.getString(0).endsWith("mp4")) {
                        saveVideo(network, files);
                    } else {
                        saveImages(network, files);
                    }
                }
                in.close();
            } catch (IOException | JSONException e) {
                // TODO: report some kind of error to the user
                Log.e(TAG, "Error while fetching data", e);
            }
        });
        download_data.start();
    }

    // FIXME: handle exceptions
    private void saveVideo(Network network, JSONArray files) throws JSONException, IOException {
        String filename = files.getString(0);
        URL file_url = new URL("http://192.168.0.1/img/" + filename);
        Log.d(TAG, "Downloading MP4 from " + file_url.toString());
        URLConnection file_conn = network.openConnection(file_url);
        InputStream file_stream = file_conn.getInputStream();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/" + getString(R.string.app_name));
            contentValues.put(MediaStore.Video.Media.IS_PENDING, true);
            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri,"w");
                FileOutputStream outputStream = new FileOutputStream(pfd.getFileDescriptor());
                byte[] buf = new byte[8192];
                int len;
                while((len=file_stream.read(buf))>0){
                    outputStream.write(buf, 0, len);
                }
                outputStream.flush();
                outputStream.close();
                file_stream.close();
                contentValues.put(MediaStore.Video.Media.IS_PENDING, false);
            }
        }
    }

    // FIXME: handle exceptions
    private void saveImages(Network network, JSONArray files) throws JSONException, IOException{
        for (int i = 0; i < files.length(); i++) {
            // FIXME: something is going wrong here and we lose some files
            String filename = files.getString(i);
            URL file_url = new URL("http://192.168.0.1/img/" + filename);
            Log.d(TAG, "Downloading IMG from " + file_url.toString());
            URLConnection file_conn = network.openConnection(file_url);
            InputStream file_stream = file_conn.getInputStream();

            // FIXME handle download on devices with version less than Android Q
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                final ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/" + getString(R.string.app_name));
                contentValues.put(MediaStore.Images.Media.IS_PENDING, true);
                ContentResolver resolver = getContentResolver();
                Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                if (uri != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(file_stream);
                    OutputStream outputStream = resolver.openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    file_stream.close();
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, false);
                }
            }
        }
    }
}
