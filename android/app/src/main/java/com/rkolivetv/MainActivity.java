package com.rkolivetv;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "RKO";
    private static final String PREFS = "RKO_PREFS";
    private static final String KEY_CURRENT_URL = "current_stream_url";

    private WebView webView;
    private SharedPreferences prefs;
    private static final String LOCAL_URL = "file:///android_asset/index.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate start");
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        FrameLayout root = new FrameLayout(this);
        setContentView(root);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        webView = new WebView(this);
        webView.setLayoutParams(params);
        root.addView(webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        // JavaScript interface to get current stream URL
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        // Inject JS to report current playing URL to Android
        String jsBridge = "javascript:(function() {" +
                "var lastUrl = '';" +
                "setInterval(function() {" +
                "  var video = document.querySelector('video');" +
                "  if (video && video.src && video.src !== lastUrl) {" +
                "    lastUrl = video.src;" +
                "    Android.onVideoPlaying(video.src);" +
                "  }" +
                "}, 1000);" +
                "})();";

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page loaded: " + url);
                view.loadUrl(jsBridge);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        webView.loadUrl(LOCAL_URL);
        Log.d(TAG, "Loading local assets");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop - checking for active stream");

        String streamUrl = prefs.getString(KEY_CURRENT_URL, "");

        if (!streamUrl.isEmpty()) {
            Log.d(TAG, "Active stream found: " + streamUrl);
            startFloatingPlayer(streamUrl);
        } else {
            Log.d(TAG, "No active stream, no floating player");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - stopping floating player if running");
        // Stop the floating service when we come back to app
        try {
            stopService(new Intent(this, FloatingService.class));
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service: " + e.getMessage());
        }
        webView.onResume();
    }

    private void startFloatingPlayer(String url) {
        Log.d(TAG, "Starting floating player for: " + url);

        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Log.d(TAG, "No overlay permission, requesting...");
            new AlertDialog.Builder(this)
                    .setTitle("Floating Player")
                    .setMessage("Enable overlay permission to play video over other apps like Facebook and TikTok.")
                    .setPositiveButton("Enable", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, 1234);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        Intent serviceIntent = new Intent(this, FloatingService.class);
        serviceIntent.putExtra("url", url);
        startService(serviceIntent);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // Don't close immediately, go to home (triggers onStop -> floating)
            moveTaskToBack(true);
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void onVideoPlaying(String url) {
            Log.d(TAG, "Video playing: " + url);
            if (url != null && !url.isEmpty()) {
                prefs.edit().putString(KEY_CURRENT_URL, url).apply();
            }
        }
    }
}
