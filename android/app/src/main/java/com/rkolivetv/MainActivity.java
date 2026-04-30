package com.rkolivetv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "RKO";
    private WebView webView;
    private String currentUrl = "https://rko-live-tv.vercel.app";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate start");
        
        try {
            webView = new android.webkit.WebView(this);
            setContentView(webView);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setUseWideViewPort(true);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(android.webkit.WebView view, String url) {
                    Log.e(TAG, "Page loaded: " + url);
                }
            });
            webView.loadUrl(currentUrl);
            Log.e(TAG, "WebView loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}