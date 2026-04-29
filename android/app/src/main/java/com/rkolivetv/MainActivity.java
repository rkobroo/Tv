public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private PictureInPictureController pipController;
    private boolean isInPipMode = false;
    private String currentVideoUrl = "";
    
    private static final String WEBSITE_URL = "https://rko-live-tv.vercel.app";
    private static final int CHECK_UPDATE_INTERVAL = 5 * 60 * 1000; // 5 minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if opened with video URL
        Intent intent = getIntent();
        String videoUrl = intent.getStringExtra("video_url");
        boolean startFloating = intent.getBooleanExtra("floating", false);
        
        setupWebView();
        
        if (videoUrl != null) {
            enterPictureInPictureMode(new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9))
                .build());
        }
        
        if (startFloating) {
            startFloatingOverlay();
        }
        
        startAutoRefresh();
    }
    
    // Check and start floating when leaving app
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        
        // Try overlay first, fallback to PiP
        if (canDrawOverlays()) {
            startFloatingService(currentVideoUrl.isEmpty() ? WEBSITE_URL : currentVideoUrl);
        } else if (webView != null && !isInPipMode) {
            enterPictureInPictureMode(new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9))
                .build());
        }
    }
    
    private boolean canDrawOverlays() {
        return android.provider.Settings.canDrawOverlays(this);
    }
    
    private void startFloatingOverlay() {
        if (canDrawOverlays()) {
            startFloatingService(WEBSITE_URL);
        } else {
            // Request permission
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
    
    private void startFloatingService(String url) {
        Intent intent = new Intent(this, FloatingService.class);
        intent.setAction("START_FLOATING");
        intent.putExtra("url", url);
        startForegroundService(intent);
    }

    private void startAutoRefresh() {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable refreshTask = new Runnable() {
            @Override
            public void run() {
                if (webView != null && !isInPipMode) {
                    webView.reload(); // Refresh page
                }
                handler.postDelayed(this, CHECK_UPDATE_INTERVAL);
            }
        };
        handler.postDelayed(refreshTask, CHECK_UPDATE_INTERVAL);
    }

    // Swipe down to refresh
    private void setupSwipeRefresh() {
        SwipeRefreshLayout swipe = new SwipeRefreshLayout(this);
        swipe.addView(webView);
        setContentView(swipe);
        
        swipe.setOnRefreshListener(() -> {
            webView.reload();
            swipe.setRefreshing(false);
        });
    }

    private void setupWebView() {
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        
        // Enable Picture-in-Picture
        webView.setPictureInPictureCallback(new WebView.PictureInPictureCallback() {
            @Override
            public void onEnterPictureInPicture() {
                isInPipMode = true;
            }

            @Override
            public void onExitPictureInPicture() {
                isInPipMode = false;
                if (webView != null) {
                    webView.reload();
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Inject PiP triggers
                injectPiPTriggers(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);
                // Auto-enter PiP when video plays
                enterPictureInPictureMode(new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9))
                    .build());
            }
        });

        setContentView(webView);
        
        // Load your website
        webView.loadUrl("https://rko-live-tv.vercel.app");
    }

    private void injectPiPTriggers(WebView view) {
        String js = "javascript:(function() {" +
            "document.addEventListener('visibilitychange', function() {" +
            "  if (document.hidden && document.pictureInPictureElement) {" +
            "    try { document.exitPictureInPicture(); } catch(e) {}" +
            "  }" +
            "});" +
            "window.autoEnterPiP = function() {" +
            "  if (document.pictureInPictureEnabled) {" +
            "    document.querySelector('video').requestPictureInPicture();" +
            "  }" +
            "};" +
            "})()";
        view.evaluateJavascript(js, null);
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Enter PiP when user leaves app
        if (webView != null && !webView.getUrl().equals("about:blank")) {
            enterPictureInPictureMode(new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9))
                .build());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            // Hide WebView but keep playing
            webView.setVisibility(View.GONE);
        } else {
            webView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String url = intent.getStringExtra("url");
        if (url != null) {
            webView.loadUrl(url);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Open in Browser");
        menu.add(0, 2, 0, "Refresh");
        menu.add(0, 3, 0, "Enter PiP");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl()));
                startActivity(browser);
                return true;
            case 2:
                webView.reload();
                return true;
            case 3:
                enterPictureInPictureMode(new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9))
                    .build());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }
}