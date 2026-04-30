package com.rkolivetv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "RKO";
    private static final String PREFS = "RKO_PREFS";
    private static final String KEY_STREAM_URL = "stream_url";

    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SharedPreferences prefs;
    private String lastStreamUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        lastStreamUrl = prefs.getString(KEY_STREAM_URL, "");

        // Root layout with pull-to-refresh
        swipeRefreshLayout = new SwipeRefreshLayout(this);
        swipeRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        webView = new WebView(this);
        swipeRefreshLayout.addView(webView);
        setContentView(swipeRefreshLayout);

        // Pull to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            webView.reload();
            swipeRefreshLayout.postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 500);
        });
        swipeRefreshLayout.setColorSchemeColors(
                android.graphics.Color.parseColor("#4a9eff"),
                android.graphics.Color.parseColor("#3a8eef")
        );

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page loaded: " + url);
                injectBridge(view);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "Navigation to: " + url);

                // Allow local assets
                if (url.startsWith("file://")) return false;

                // Block ads and redirects
                if (isAdUrl(url)) {
                    Log.d(TAG, "Blocking ad redirect: " + url);
                    // Go back immediately
                    if (view.canGoBack()) {
                        view.goBack();
                    }
                    return true;
                }

                // Allow HLS streams and video content
                if (url.contains(".m3u8") || url.contains(".ts") || url.contains("myturn1")
                        || url.contains("azplay") || url.contains("trl01") || url.contains("muc00")
                        || url.contains("plu00") || url.contains("mut00")) {
                    return false;
                }

                // Allow streaming sites
                if (url.contains("webcric") || url.contains("iplstreams") || url.contains("yonotv")) {
                    return false;
                }

                // Block everything else (popup ads, external redirects)
                Log.d(TAG, "Blocking external URL: " + url);
                if (view.canGoBack()) {
                    view.goBack();
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "WebView error: " + description);
                // If error on main page, reload
                if (failingUrl.contains("index.html") || errorCode == WebViewClient.ERROR_HOST_LOOKUP) {
                    view.loadUrl("file:///android_asset/index.html");
                }
            }
        });

        String loadUrl = "file:///android_asset/index.html";
        if (!lastStreamUrl.isEmpty()) {
            loadUrl += "?stream=" + Uri.encode(lastStreamUrl);
        }
        webView.loadUrl(loadUrl);
        Log.d(TAG, "Loading: " + loadUrl);
    }

    private void injectBridge(WebView view) {
        String js = "javascript:(function() {" +
                "var lastUrl = '';" +
                "setInterval(function() {" +
                "  var video = document.querySelector('video');" +
                "  if (video && video.src && video.src !== lastUrl && !video.src.startsWith('blob:')) {" +
                "    lastUrl = video.src;" +
                "    localStorage.setItem('lastStreamUrl', video.src);" +
                "    if (typeof Android !== 'undefined') Android.onVideoPlaying(video.src);" +
                "  }" +
                "  var iframe = document.getElementById('yonoFrame');" +
                "  if (iframe && iframe.src && iframe.style.display !== 'none' && iframe.src !== 'about:blank') {" +
                "    localStorage.setItem('lastStreamUrl', iframe.src);" +
                "    if (typeof Android !== 'undefined') Android.onVideoPlaying(iframe.src);" +
                "  }" +
                "}, 500);" +
                "})();";
        view.loadUrl(js);
    }

    private boolean isAdUrl(String url) {
        String[] adPatterns = {
                "popup", "ad.", "ads.", "banner", "popunder",
                "doubleclick", "googleads", "facebook", "tiktok",
                "redirect", "tracking", "analytics", "telemetry"
        };
        for (String pattern : adPatterns) {
            if (url.contains(pattern)) return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // When minimized, save stream URL and start floating player
        String streamUrl = prefs.getString(KEY_STREAM_URL, "");
        if (!streamUrl.isEmpty()) {
            startFloatingPlayer(streamUrl);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // When coming back, stop floating player and restore WebView
        try {
            stopService(new Intent(this, FloatingService.class));
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service: " + e.getMessage());
        }
        webView.onResume();
    }

    private void startFloatingPlayer(String url) {
        Log.d(TAG, "Starting floating player: " + url);
        Intent serviceIntent = new Intent(this, FloatingService.class);
        serviceIntent.putExtra("streamUrl", url);
        startService(serviceIntent);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            moveTaskToBack(true);
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void onVideoPlaying(String url) {
            if (url != null && !url.isEmpty()) {
                prefs.edit().putString(KEY_STREAM_URL, url).apply();
                lastStreamUrl = url;
            }
        }
    }
}
