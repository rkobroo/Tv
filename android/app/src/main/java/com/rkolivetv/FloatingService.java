package com.rkolivetv;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class FloatingService extends Service {
    private static final String TAG = "RKO_Float";
    private static final String LOCAL_URL = "file:///android_asset/index.html";

    private WindowManager windowManager;
    private View floatingView;
    private WebView webView;
    private String streamUrl = "";

    private int viewWidth, viewHeight;
    private final int MIN_WIDTH = 300;
    private final int MIN_HEIGHT = 200;
    private final int INITIAL_WIDTH = 600;
    private final int INITIAL_HEIGHT = 400;

    private boolean isDragging = false;
    private boolean isResizing = false;
    private float dragStartX, dragStartY;
    private int viewStartX, viewStartY;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if ("STOP".equals(intent.getAction())) {
                stopSelf();
                return START_NOT_STICKY;
            }
            streamUrl = intent.getStringExtra("streamUrl") != null
                    ? intent.getStringExtra("streamUrl") : "";
        }
        showFloatingWindow();
        return START_STICKY;
    }

    private void showFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                stopSelf();
                return;
            }
        }

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.BLACK);

        viewWidth = dpToPx(INITIAL_WIDTH);
        viewHeight = dpToPx(INITIAL_HEIGHT);

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                viewWidth, viewHeight, layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;

        // Control bar
        LinearLayout controlBar = createControlBar();
        mainLayout.addView(controlBar);

        // WebView
        webView = new WebView(this);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 1.0f
        );
        webView.setLayoutParams(webParams);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Only allow local assets and HLS streams
                if (url.startsWith("file://") || url.startsWith("blob:")) {
                    return false;
                }
                if (url.contains(".m3u8") || url.contains(".ts") || url.contains("myturn1") || url.contains("azplay")) {
                    return false;
                }
                // Block everything else (ads, external redirects)
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Floating WebView loaded: " + url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        // Load the stream URL or the app with stream param
        if (!streamUrl.isEmpty()) {
            String loadUrl = LOCAL_URL + "?stream=" + streamUrl;
            webView.loadUrl(loadUrl);
        } else {
            webView.loadUrl(LOCAL_URL);
        }

        mainLayout.addView(webView);

        // Resize handle
        View resizeHandle = new View(this);
        LinearLayout.LayoutParams resizeParams = new LinearLayout.LayoutParams(
                dpToPx(30), dpToPx(30)
        );
        resizeParams.gravity = Gravity.END | Gravity.BOTTOM;
        resizeHandle.setLayoutParams(resizeParams);
        resizeHandle.setBackgroundColor(0x80FFFFFF);
        resizeHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isResizing = true;
                    dragStartX = event.getRawX();
                    dragStartY = event.getRawY();
                    viewStartX = viewWidth;
                    viewStartY = viewHeight;
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE && isResizing) {
                    int newX = viewStartX + (int) (event.getRawX() - dragStartX);
                    int newY = viewStartY + (int) (event.getRawY() - dragStartY);
                    viewWidth = Math.max(MIN_WIDTH, newX);
                    viewHeight = Math.max(MIN_HEIGHT, newY);
                    params.width = viewWidth;
                    params.height = viewHeight;
                    windowManager.updateViewLayout(mainLayout, params);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    isResizing = false;
                    return true;
                }
                return false;
            }
        });
        mainLayout.addView(resizeHandle);

        // Drag logic on the whole layout
        mainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isResizing) return true;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isDragging = true;
                    dragStartX = event.getRawX();
                    dragStartY = event.getRawY();
                    viewStartX = params.x;
                    viewStartY = params.y;
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE && isDragging) {
                    params.x = viewStartX + (int) (event.getRawX() - dragStartX);
                    params.y = viewStartY + (int) (event.getRawY() - dragStartY);
                    windowManager.updateViewLayout(mainLayout, params);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    isDragging = false;
                    return true;
                }
                return false;
            }
        });

        floatingView = mainLayout;

        try {
            windowManager.addView(mainLayout, params);
            Log.d(TAG, "Floating window shown");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add floating window: " + e.getMessage());
            stopSelf();
        }
    }

    private LinearLayout createControlBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(0xCC000000);
        bar.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));

        // Mute/Unmute button
        ImageButton muteBtn = new ImageButton(this);
        muteBtn.setImageResource(android.R.drawable.ic_lock_silent_mode);
        muteBtn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        muteBtn.setBackgroundColor(Color.TRANSPARENT);
        muteBtn.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(36), dpToPx(30)));
        muteBtn.setOnClickListener(v -> {
            if (webView != null) {
                webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
                webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
            }
        });

        // Refresh button
        ImageButton refreshBtn = new ImageButton(this);
        refreshBtn.setImageResource(android.R.drawable.ic_menu_rotate);
        refreshBtn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        refreshBtn.setBackgroundColor(Color.TRANSPARENT);
        refreshBtn.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(36), dpToPx(30)));
        refreshBtn.setOnClickListener(v -> {
            if (webView != null) webView.reload();
        });

        // Zoom/Fill button
        ImageButton zoomBtn = new ImageButton(this);
        zoomBtn.setImageResource(android.R.drawable.ic_menu_zoom);
        zoomBtn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        zoomBtn.setBackgroundColor(Color.TRANSPARENT);
        zoomBtn.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(36), dpToPx(30)));
        zoomBtn.setOnClickListener(v -> {
            viewWidth = dpToPx(900);
            viewHeight = dpToPx(600);
            WindowManager.LayoutParams p = (WindowManager.LayoutParams) floatingView.getLayoutParams();
            p.width = viewWidth;
            p.height = viewHeight;
            windowManager.updateViewLayout(floatingView, p);
        });

        // Open app button
        ImageButton appBtn = new ImageButton(this);
        appBtn.setImageResource(android.R.drawable.ic_menu_upload);
        appBtn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        appBtn.setBackgroundColor(Color.TRANSPARENT);
        appBtn.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(36), dpToPx(30)));
        appBtn.setOnClickListener(v -> {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launchIntent != null) {
                startActivity(launchIntent);
            }
            stopSelf();
        });

        // Close button
        ImageButton closeBtn = new ImageButton(this);
        closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        closeBtn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        closeBtn.setBackgroundColor(Color.TRANSPARENT);
        closeBtn.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(36), dpToPx(30)));
        closeBtn.setOnClickListener(v -> stopSelf());

        // Add space between buttons
        bar.addView(muteBtn);
        bar.addView(refreshBtn);
        bar.addView(zoomBtn);
        bar.addView(appBtn);
        bar.addView(closeBtn);

        return bar;
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
            }
        }
        Log.d(TAG, "Floating service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
