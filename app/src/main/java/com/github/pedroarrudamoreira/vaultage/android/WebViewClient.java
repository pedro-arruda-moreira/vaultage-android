package com.github.pedroarrudamoreira.vaultage.android;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.webkit.ClientCertRequest;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

class WebViewClient extends android.webkit.WebViewClient {

    static private class ContentType {
        private final String mime;
        private final String encoding;

        private ContentType(String mime, String encoding) {
            this.mime = mime;
            this.encoding = encoding;
        }
    }

    private static final Map<String, ContentType> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put(".js", new ContentType("application/javascript", "utf-8"));
        MIME_TYPES.put(".html", new ContentType("text/html", "utf-8"));
        MIME_TYPES.put(".css", new ContentType("text/css", "utf-8"));
        MIME_TYPES.put(".png", new ContentType("image/png", null));
        MIME_TYPES.put(".jpg", new ContentType("image/jpeg", null));
        MIME_TYPES.put(".jpeg", new ContentType("image/jpeg", null));
        MIME_TYPES.put(".gif", new ContentType("image/gif", null));
        MIME_TYPES.put(".ico", new ContentType("image/x-icon", null));
        MIME_TYPES.put(".json", new ContentType("application/json", "utf-8"));
        MIME_TYPES.put(".woff2", new ContentType("font/woff2", null));
    }

    private static ContentType getMimeType(String url) {
        // Get the file extension from the URL
        int lastDotIndex = url.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return null;
        }
        String extension = url.substring(lastDotIndex);

        // Check if the file extension is in the table of well-known MIME types
        return MIME_TYPES.get(extension);
    }


    private final Uri serverUri;
    private final String customJS;
    private final MainActivity activity;

    public WebViewClient(Uri serverUri, InputStream customJs, MainActivity activity) throws IOException {
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

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if(!activity.isOfflineModeEnabled()) {
            return super.shouldInterceptRequest(view, request);
        }
        String path = request.getUrl().getPath();
        if ("/".equals(path)) {
            path = "index.html";
        } else {
            // remove trailing slash
            path = path.substring(1);
        }
        // Check if the request is for a resource with one of the specified MIME types
        ContentType contentType = getMimeType(path);
        AssetManager assetManager = view.getContext().getAssets();
        InputStream fileContent;
        try {
            if (contentType != null) {
                fileContent = assetManager.open(path);
            } else {
                fileContent = null;
            }
        } catch (IOException e) {
            fileContent = null;
        }
        // Check if the file exists at the given path
        if (fileContent != null) {
            return new WebResourceResponse(
                    contentType.mime,
                    contentType.encoding,
                    fileContent
            );
        }
        // Proceed with the default behavior of loading the URL
        return super.shouldInterceptRequest(view, request);
    }


    /**
     * <a href="https://github.com/gonativeio/gonative-android">https://github.com/gonativeio/gonative-android</a>
     */
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

    /**
     * <a href="https://github.com/gonativeio/gonative-android">https://github.com/gonativeio/gonative-android</a>
     */
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

    /**
     * <a href="https://github.com/gonativeio/gonative-android">https://github.com/gonativeio/gonative-android</a>
     */
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
