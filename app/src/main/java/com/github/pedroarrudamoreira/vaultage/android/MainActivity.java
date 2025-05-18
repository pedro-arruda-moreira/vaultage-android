package com.github.pedroarrudamoreira.vaultage.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.CheckBox;

import java.io.IOException;

public class MainActivity extends Activity {

    private static final String OFFLINE = "offline";
    private WebView webView;

    private CheckBox checkBox;

    boolean isOfflineModeEnabled() {
        return checkBox.isChecked();
    }

    private class ColorChanger {

        private final Window window;

        private ColorChanger(Window window) {
            this.window = window;
        }
        @JavascriptInterface
        public void set(String color) {
            int colorCode = Color.parseColor("#" + color);
            window.setStatusBarColor(colorCode);
            checkBox.setBackgroundColor(colorCode);
            checkBox.setTextColor(Color.WHITE);
        }
    }

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window window = getWindow();

        AssetManager assetManager = getApplicationContext().getAssets();

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        final ColorChanger colorChanger = new ColorChanger(window);

        String serverAddress = getString(R.string.server_url);

        Uri serverUri = Uri.parse(serverAddress);
        webView = findViewById(R.id.activity_main_webview);
        checkBox = findViewById(R.id.checkBox);
        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        checkBox.setChecked(preferences.getBoolean(OFFLINE, false));
        checkBox.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit().putBoolean(OFFLINE, checked).apply();
        });
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webView.setWebChromeClient(new WebChromeClient(this));
        webView.addJavascriptInterface(colorChanger, "colorChanger");
        try {
            webView.setWebViewClient(new WebViewClient(
                    serverUri,
                    assetManager.open("custom.js"),
                    this
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        webView.loadUrl(serverAddress);

    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

}
