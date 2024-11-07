package com.github.pedroarrudamoreira.vaultage.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.webkit.ClientCertRequest;
import android.webkit.WebView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

class WebViewClient extends android.webkit.WebViewClient {

    private final Uri serverUri;
    private final String customJS;
    private final Activity activity;

    public WebViewClient(Uri serverUri, InputStream customJs, Activity activity) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read;
        byte[] buffer = new byte[512];
        while ((read = customJs.read(buffer)) > -1) {
            baos.write(buffer, 0, read);
        }
        this.serverUri = serverUri;
        this.customJS = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        this.activity = activity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        String hostname;

        hostname = serverUri.getHost();

        Uri uri = Uri.parse(url);
        if (url.startsWith("file:") || uri.getHost() != null && uri.getHost().endsWith(hostname)) {
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        view.getContext().startActivity(intent);
        return true;
    }

    /** <a href="https://github.com/gonativeio/gonative-android">https://github.com/gonativeio/gonative-android</a> */
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        String js = "(function() {" +
                "var parent = document.getElementsByTagName('head').item(0);" +
                "var script = document.createElement('script');" +
                "script.type = 'text/javascript';" +
                "script.innerHTML = window.atob('" + this.customJS + "');" +
                "parent.appendChild(script)" +
                "})()";

        view.evaluateJavascript(js, null);
    }

    /** <a href="https://github.com/gonativeio/gonative-android">https://github.com/gonativeio/gonative-android</a> */
    @Override
    public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {

        KeyChainAliasCallback callback = alias -> {
            if (alias == null) {
                request.ignore();
                return;
            }

            new GetKeyTask(activity, request).execute(alias);
        };

        KeyChain.choosePrivateKeyAlias(activity, callback, request.getKeyTypes(), request.getPrincipals(), request.getHost(),
                request.getPort(), null);
    }

    /** <a href="https://github.com/gonativeio/gonative-android">https://github.com/gonativeio/gonative-android</a> */
    private static class GetKeyTask extends AsyncTask<String, Void, Pair<PrivateKey, X509Certificate[]>> {
        private Activity activity;
        private ClientCertRequest request;

        public GetKeyTask(Activity activity, ClientCertRequest request) {
            this.activity = activity;
            this.request = request;
        }

        @Override
        protected Pair<PrivateKey, X509Certificate[]> doInBackground(String... strings) {
            String alias = strings[0];

            try {
                PrivateKey privateKey = KeyChain.getPrivateKey(activity, alias);
                X509Certificate[] certificates = KeyChain.getCertificateChain(activity, alias);
                return new Pair<>(privateKey, certificates);
            } catch (Exception e) {
                Log.e("SSL-CHOOSER", e.getLocalizedMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Pair<PrivateKey, X509Certificate[]> result) {
            if (result != null && result.first != null & result.second != null) {
                request.proceed(result.first, result.second);
            } else {
                request.ignore();
            }
        }
    }

}
