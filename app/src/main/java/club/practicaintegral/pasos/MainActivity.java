package club.practicaintegral.pasos;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.content.pm.PackageInfo;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de la app: muestra la misma web de siempre (diseño, música, avisos, pantalla del ordenador)
 * y le da los pasos del podómetro del teléfono a través de "AndroidPasos".
 */
public class MainActivity extends Activity {
    private static final String WEB = "https://gcalabro12.github.io/Contador-Pasos/";
    private static final String HOST = "gcalabro12.github.io";
    private WebView web;
    private LinearLayout overlay;
    private TextView overlayText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> jsErrors = new ArrayList<>();
    private boolean loaded;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0F172A);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0F172A);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        root.addView(buildOverlay(), new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        web.addJavascriptInterface(new Bridge(), "AndroidPasos");
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage m) {
                if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR && jsErrors.size() < 6)
                    jsErrors.add(m.message() + " (línea " + m.lineNumber() + ")");
                return false;
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (HOST.equals(u.getHost())) return false;
                // Otros enlaces (GitHub, ajustes…) se abren fuera
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (RuntimeException ignored) {}
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.startsWith("file:")) { loaded = true; overlay.setVisibility(View.GONE); return; }
                // ¿Se ve algo? Si la página quedó vacía, se explica en pantalla
                view.evaluateJavascript("(function(){try{return document.body.innerText.length}catch(e){return -1}})()", v -> {
                    int len;
                    try { len = Integer.parseInt(v.replace("\"", "")); } catch (RuntimeException e) { len = -1; }
                    if (len > 30) { loaded = true; overlay.setVisibility(View.GONE); }
                    else showProblem("La página se ha cargado vacía (" + len + ").");
                });
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (!req.isForMainFrame()) return;
                int code = err.getErrorCode();
                if (code == ERROR_HOST_LOOKUP || code == ERROR_CONNECT || code == ERROR_TIMEOUT) {
                    // Sin internet: página sencilla con los pasos, que sigue contando igual
                    view.loadUrl("file:///android_asset/sin-conexion.html");
                } else {
                    showProblem("Error al cargar (" + code + "): " + err.getDescription());
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest req, WebResourceResponse res) {
                if (req.isForMainFrame()) showProblem("El servidor respondió " + res.getStatusCode());
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler h, SslError e) {
                h.cancel();
                showProblem("Error de seguridad (SSL " + e.getPrimaryError() + "). Revisa la fecha y hora del teléfono.");
            }
        });
        web.loadUrl(WEB);
        handler.postDelayed(() -> { if (!loaded) showProblem("La página no termina de cargar."); }, 20000);

        askPermissions();
    }

    /** Capa nativa encima de la web: «Cargando…» y, si algo falla, qué ha pasado y botones para arreglarlo. */
    private View buildOverlay() {
        overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER);
        overlay.setBackgroundColor(0xFF0F172A);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        overlay.setPadding(pad, pad, pad, pad);
        overlayText = new TextView(this);
        overlayText.setTextColor(0xFFF8FAFC);
        overlayText.setTextSize(17);
        overlayText.setGravity(Gravity.CENTER);
        overlayText.setText("👣\n\nCargando Pasos…\n\nEl contador ya está funcionando aunque esta pantalla tarde.");
        overlay.addView(overlayText);
        Button retry = new Button(this);
        retry.setText("↻ Reintentar");
        retry.setOnClickListener(v -> {
            loaded = false; jsErrors.clear();
            overlayText.setText("👣\n\nCargando Pasos…");
            web.loadUrl(WEB);
            handler.postDelayed(() -> { if (!loaded) showProblem("La página no termina de cargar."); }, 20000);
        });
        overlay.addView(retry);
        Button webView = new Button(this);
        webView.setText("Actualizar «Android System WebView»");
        webView.setOnClickListener(v -> openStore("com.google.android.webview"));
        overlay.addView(webView);
        return overlay;
    }

    private void showProblem(String what) {
        if (loaded) return;
        String wv = "?";
        PackageInfo p = WebView.getCurrentWebViewPackage();
        if (p != null) wv = p.packageName + " " + p.versionName;
        StringBuilder sb = new StringBuilder("⚠️ No se puede mostrar la pantalla de Pasos.\n\n")
                .append(what)
                .append("\n\nEl contador de pasos sigue funcionando (mira la notificación).")
                .append("\n\nMándale una captura de esto a Claude.\n\n")
                .append("Android ").append(Build.VERSION.RELEASE).append(" · ").append(Build.MODEL)
                .append("\nWebView: ").append(wv);
        for (String e : jsErrors) sb.append("\n• ").append(e);
        overlayText.setText(sb.toString());
        overlayText.setTextSize(14);
        overlay.setVisibility(View.VISIBLE);
    }

    private void openStore(String pkg) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
        } catch (RuntimeException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
        }
    }

    private void askPermissions() {
        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.ACTIVITY_RECOGNITION);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.POST_NOTIFICATIONS);
        if (need.isEmpty()) StepService.start(this);
        else requestPermissions(need.toArray(new String[0]), 1);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        StepService.start(this);
        web.evaluateJavascript("window.nativeRefresh && window.nativeRefresh()", null);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else moveTaskToBack(true); // no se cierra: el contador y la música siguen
    }

    private boolean activityPermissionOk() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }

    /** Funciones que la web puede llamar con window.AndroidPasos.xxx() */
    class Bridge {
        @JavascriptInterface public String getToday() { return StepStore.todayJson(MainActivity.this); }

        @JavascriptInterface public String getHistory() { return StepStore.historyJson(MainActivity.this); }

        @JavascriptInterface public void setConfig(String json) {
            StepStore.setConfig(MainActivity.this, json);
            sendToService(new Intent(StepService.ACTION_REFRESH));
        }

        @JavascriptInterface public void speak(String text) {
            sendToService(new Intent(StepService.ACTION_SPEAK).putExtra("text", text));
        }

        @JavascriptInterface public void addSteps(int n) {
            int[] r = StepStore.addManual(MainActivity.this, n);
            sendToService(new Intent(StepService.ACTION_MANUAL).putExtra("before", r[0]).putExtra("after", r[1]));
        }

        @JavascriptInterface public void resetToday() {
            StepStore.resetToday(MainActivity.this);
            sendToService(new Intent(StepService.ACTION_REFRESH));
        }

        @JavascriptInterface public boolean permissionOk() { return activityPermissionOk(); }

        @JavascriptInterface public boolean hasSensor() { return StepService.hasSensor; }

        @JavascriptInterface public boolean batteryOk() {
            PowerManager pm = getSystemService(PowerManager.class);
            return pm.isIgnoringBatteryOptimizations(getPackageName());
        }

        /** Porcentaje de batería del teléfono (0-100), o -1 si no se sabe. */
        @JavascriptInterface public int battery() {
            BatteryManager bm = getSystemService(BatteryManager.class);
            int v = bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
            if (v <= 0) {
                Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (b != null) {
                    int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                    if (level >= 0 && scale > 0) v = level * 100 / scale;
                }
            }
            return v;
        }

        @JavascriptInterface public boolean charging() {
            BatteryManager bm = getSystemService(BatteryManager.class);
            return bm != null && bm.isCharging();
        }

        @JavascriptInterface public void openBatterySettings() {
            runOnUiThread(() -> {
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + getPackageName())));
                } catch (RuntimeException e) {
                    startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                }
            });
        }

        @JavascriptInterface public void openAppSettings() {
            runOnUiThread(() -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()))));
        }

        @JavascriptInterface public void askPermissions() { runOnUiThread(MainActivity.this::askPermissions); }

        @JavascriptInterface public String version() {
            try {
                return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                return "?";
            }
        }

        private void sendToService(Intent i) {
            if (!activityPermissionOk()) return;
            i.setClass(MainActivity.this, StepService.class);
            try { startForegroundService(i); } catch (RuntimeException ignored) {}
        }
    }
}
