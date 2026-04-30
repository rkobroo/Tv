package com.rkolivetv;

import android.app.PictureInPictureModeChangedInfo;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "RKO";
    private WebView webView;
    private static final String LOCAL_URL = "file:///android_asset/index.html";
    private boolean isInPipMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate start");

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

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page loaded: " + url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        webView.loadUrl(LOCAL_URL);
        Log.d(TAG, "Loading local assets");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        if (!isInPipMode) {
            webView.onResume();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause, isInPip=" + isInPipMode);
        if (!isInPipMode) {
            webView.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        isInPipMode = isInPictureInPictureMode;

        if (isInPictureInPictureMode) {
            Log.d(TAG, "Entered PiP mode");
            webView.onResume();
        } else {
            Log.d(TAG, "Exited PiP mode");
            webView.onResume();
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        Log.d(TAG, "User leaving - entering PiP");
        enterPipMode();
    }

    private void enterPipMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Rational aspectRatio = new Rational(16, 9);
            PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio);

            try {
                enterPictureInPictureMode(builder.build());
            } catch (IllegalStateException e) {
                Log.e(TAG, "PiP failed: " + e.getMessage());
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
