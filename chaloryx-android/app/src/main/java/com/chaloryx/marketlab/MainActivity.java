package com.chaloryx.marketlab;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 7418;
    private static final String LOCAL_ORIGIN = "appassets.androidplatform.net";
    private static final int MAX_EXPORT_BYTES = 12 * 1024 * 1024;
    private WebView webView;
    private ValueCallback<Uri[]> pendingFile;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(7, 17, 25));
        getWindow().setNavigationBarColor(Color.rgb(7, 17, 25));

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(7, 17, 25));
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        if (Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(true);
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        webView.addJavascriptInterface(new ExportBridge(), "ChaloryxAndroid");
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equalsIgnoreCase(uri.getScheme()) && LOCAL_ORIGIN.equalsIgnoreCase(uri.getHost())) return false;
                String scheme = uri.getScheme();
                if ("https".equalsIgnoreCase(scheme)) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                    catch (Exception e) { toast("No browser found for external link."); }
                }
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingFile != null) pendingFile.onReceiveValue(null);
                pendingFile = callback;
                try {
                    Intent intent = params.createIntent();
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(Intent.createChooser(intent, "Choose CSV or CHALORYX backup"), PICK_FILE);
                    return true;
                } catch (Exception ex) {
                    pendingFile = null;
                    callback.onReceiveValue(null);
                    toast("File picker is not available.");
                    return true;
                }
            }
        });
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && pendingFile != null) {
            pendingFile.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            pendingFile = null;
        }
    }

    private void toast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    private final class ExportBridge {
        @JavascriptInterface public void saveFile(String fileName, String mimeType, String base64) {
            if (fileName == null || base64 == null || mimeType == null) return;
            final String name = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            final String mime = mimeType.toLowerCase(Locale.ROOT);
            if (name.length() == 0 || name.length() > 120 || !name.startsWith("CHALORYX_") ||
                    !(name.endsWith(".csv") || name.endsWith(".json")) ||
                    !("text/csv".equals(mime) || "application/json".equals(mime)) ||
                    base64.length() > (MAX_EXPORT_BYTES * 4 / 3 + 8)) {
                toast("Export rejected: invalid filename or size.");
                return;
            }
            try {
                byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                if (decoded.length > MAX_EXPORT_BYTES) throw new IllegalArgumentException("File too large");
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, name);
                values.put(MediaStore.Downloads.MIME_TYPE, mime);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CHALORYX");
                values.put(MediaStore.Downloads.IS_PENDING, 1);
                Uri location = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (location == null) throw new Exception("Could not create download");
                try (OutputStream output = getContentResolver().openOutputStream(location)) {
                    if (output == null) throw new Exception("Could not write download");
                    output.write(decoded);
                } catch (Exception ex) {
                    getContentResolver().delete(location, null, null);
                    throw ex;
                }
                ContentValues ready = new ContentValues();
                ready.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(location, ready, null, null);
                toast("Saved to Downloads/CHALORYX: " + name);
            } catch (Exception ex) {
                toast("Export failed: " + ex.getMessage());
            }
        }
    }

    @Override protected void onDestroy() {
        if (pendingFile != null) { pendingFile.onReceiveValue(null); pendingFile = null; }
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
