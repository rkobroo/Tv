package com.rkolivetv;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;

public class FloatingService extends Service {
    private static final String TAG = "RKO_Floating";

    private WindowManager windowManager;
    private View floatingView;
    private WebView webView;
    private String currentUrl = "file:///android_asset/index.html";

    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getStringExtra("url") != null) {
            currentUrl = intent.getStringExtra("url");
        }
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        showFloatingWindow();
        return START_STICKY;
    }

    private void showFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Log.e(TAG, "Overlay permission not granted");
                stopSelf();
                return;
            }
        }

        // Create root layout
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        int width = dpToPx(320);
        int height = dpToPx(240);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width,
                height,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 20;
        params.y = 200;

        // Close button
        ImageButton closeBtn = new ImageButton(this);
        closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        closeBtn.setBackgroundColor(0xCC000000);
        closeBtn.setScaleType(ImageButton.ScaleType.CENTER);

        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                dpToPx(36),
                dpToPx(36),
                Gravity.TOP | Gravity.END
        );
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> {
            stopSelf();
        });

        // WebView
        webView = new WebView(this);
        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        webParams.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        webView.setLayoutParams(webParams);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(currentUrl);

        // Drag logic
        root.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int deltaX = (int) (event.getRawX() - initialTouchX);
                    int deltaY = (int) (event.getRawY() - initialTouchY);
                    params.x = initialX + deltaX;
                    params.y = initialY + deltaY;
                    windowManager.updateViewLayout(root, params);
                    return true;
            }
            return false;
        });

        root.addView(webView);
        root.addView(closeBtn);
        floatingView = root;

        try {
            windowManager.addView(root, params);
            Log.d(TAG, "Floating window added");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add view: " + e.getMessage());
            stopSelf();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                Log.e(TAG, "Error removing view: " + e.getMessage());
            }
            if (webView != null) {
                webView.destroy();
                webView = null;
            }
        }
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
