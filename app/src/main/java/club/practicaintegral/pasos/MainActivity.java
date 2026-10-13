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
import android.webkit.JavascriptInterface;
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

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        web.addJavascriptInterface(new Bridge(), "AndroidPasos");
        web.setWebChromeClient(new WebChromeClient());
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
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                // Sin internet: página sencilla con los pasos, que sigue contando igual
                if (req.isForMainFrame()) view.loadUrl("file:///android_asset/sin-conexion.html");
            }
        });
        if (saved == null) web.loadUrl(WEB);
        else web.restoreState(saved);

        askPermissions();
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
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
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
