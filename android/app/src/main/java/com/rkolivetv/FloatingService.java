package com.rkolivetv;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;

public class FloatingService extends Service {
    private WindowManager windowManager;
    private WebView webView;
    private View floatingView;
    private boolean isFloating = false;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "START_FLOATING":
                    startFloating(intent.getStringExtra("url"));
                    break;
                case "STOP_FLOATING":
                    stopFloating();
                    break;
                case "MOVE_FLOATING":
                    // Already floating, just continue
                    break;
            }
        }
        return START_STICKY;
    }

    private void startFloating(String url) {
        if (isFloating) return;

        // Check if permission granted
        if (!android.provider.Settings.canDrawOverlays(this)) {
            // Permission not granted - will use PiP instead
            stopSelf();
            return;
        }

        // Create floating view
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.floating_webview, null);
        
        webView = floatingView.findViewById(R.id.floatingWebView);
        setupWebView(webView);

        // Window params for floating
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;
        params.width = 600;
        params.height = 400;

        // Add drag functionality
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        // Close button
        floatingView.findViewById(R.id.closeBtn).setOnClickListener(v -> stopFloating());
        
        // Expand button
        floatingView.findViewById(R.id.expandBtn).setOnClickListener(v -> {
            // Expand to full PiP
            stopFloating();
            // Trigger PiP in main app
        });

        try {
            windowManager.addView(floatingView, params);
            isFloating = true;
            
            if (url != null) {
                webView.loadUrl(url);
            } else {
                webView.loadUrl("https://rko-live-tv.vercel.app");
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    private void setupWebView(WebView wv) {
        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
    }

    private void stopFloating() {
        if (isFloating && floatingView != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {}
            isFloating = false;
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopFloating();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}