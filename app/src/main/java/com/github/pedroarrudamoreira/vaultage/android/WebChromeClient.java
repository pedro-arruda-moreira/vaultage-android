package com.github.pedroarrudamoreira.vaultage.android;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebView;

class WebChromeClient extends android.webkit.WebChromeClient {

    private final Context context;

    WebChromeClient(Context context) {
        this.context = context;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        switch (consoleMessage.messageLevel()) {
            case LOG:
                Log.i("[console.log]", consoleMessage.message());
                break;
            case DEBUG:
            case TIP:
                Log.d("[console.debug]", consoleMessage.message());
                break;
            case WARNING:
                Log.w("[console.warn]", consoleMessage.message());
                break;
            case ERROR:
                Log.e("[console.error]", consoleMessage.message());
                break;
        }
        return true;
    }


    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton("Ok", (dialog, which) -> result.confirm())
                .setOnDismissListener(dialog -> result.cancel()).show();
        return true;
    }
}
