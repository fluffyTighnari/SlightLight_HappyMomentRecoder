package com.fluffytighnari.weiguang;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A very small shell: the whole web app is bundled in assets and served from a
 * virtual HTTPS origin so that IndexedDB, getUserMedia and friends keep working
 * exactly as they do in a secure browser context.
 *
 * No AndroidX, no third-party libraries — the APK stays tiny and offline.
 */
public final class MainActivity extends Activity {

    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/index.html";
    private static final int BG = 0xFF1A1411;

    private static final int REQ_WEB_PERMISSION = 41;
    private static final int REQ_PICK_FILE = 42;
    private static final int REQ_SAVE_FILE = 43;

    private WebView web;

    private PermissionRequest pendingPermission;
    private ValueCallback<Uri[]> pendingChooser;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final StringBuilder permissionLog = new StringBuilder();

    /** Export staging file, filled chunk by chunk from JS, then copied to the user's chosen place. */
    private File saveFile;
    private FileOutputStream saveStream;
    private String saveName = "weiguang-backup.json";
    private String saveMime = "application/json";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            init(state);
        } catch (Throwable error) {
            showError(error);
        }
    }

    private void init(Bundle state) {
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);

        web = new WebView(this);
        web.setBackgroundColor(BG);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setLongClickable(false);
        web.setHapticFeedbackEnabled(false);

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setUserAgentString(settings.getUserAgentString() + " WeiguangShell/0.1.4");

        web.setWebViewClient(new ShellClient());
        web.setWebChromeClient(new ShellChrome());
        web.addJavascriptInterface(new Bridge(), "WeiguangShell");
        WebView.setWebContentsDebuggingEnabled(false);

        note("android " + Build.VERSION.SDK_INT);
        note("webview " + webViewVersion());

        setContentView(web, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Light status/navigation icons come from the theme, so no post-attach
        // window-insets juggling is needed here.

        if (state != null) {
            try {
                web.restoreState(state);
            } catch (Throwable ignored) {
                web.loadUrl(START_URL);
            }
        } else {
            web.loadUrl(START_URL);
        }
    }

    /** If the shell cannot start, say why on screen instead of closing silently. */
    private void showError(Throwable error) {
        StringBuilder text = new StringBuilder();
        text.append("微光没能打开\n\n");
        text.append(error.toString()).append("\n\n");
        for (StackTraceElement line : error.getStackTrace()) {
            text.append("  at ").append(line).append('\n');
        }
        Throwable cause = error.getCause();
        if (cause != null) {
            text.append("\nCaused by: ").append(cause).append('\n');
            for (StackTraceElement line : cause.getStackTrace()) {
                text.append("  at ").append(line).append('\n');
            }
        }
        text.append("\nAndroid ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                android.content.pm.PackageInfo info = WebView.getCurrentWebViewPackage();
                if (info != null) {
                    text.append("WebView ").append(info.versionName).append('\n');
                }
            } catch (Throwable ignored) {
                // best effort only
            }
        }
        text.append("\n把这段文字截图发给我就好。");

        android.widget.TextView view = new android.widget.TextView(this);
        view.setText(text);
        view.setTextIsSelectable(true);
        view.setTextSize(12);
        view.setTextColor(0xFFF1EADF);
        view.setBackgroundColor(BG);
        view.setPadding(48, 64, 48, 64);

        android.widget.ScrollView scroller = new android.widget.ScrollView(this);
        scroller.setBackgroundColor(BG);
        scroller.addView(view);
        setContentView(scroller);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override
    protected void onPause() {
        // Stops the room's animation loop while the app is in the background.
        if (web != null) web.onPause();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        closeSaveStream();
        if (saveFile != null) {
            saveFile.delete();
            saveFile = null;
        }
        if (web != null) {
            ViewGroup parent = (ViewGroup) web.getParent();
            if (parent != null) parent.removeView(web);
            web.removeJavascriptInterface("WeiguangShell");
            web.loadUrl("about:blank");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    /* ------------------------------------------------------------------ */
    /* Serving the bundled page over a trustworthy origin                  */
    /* ------------------------------------------------------------------ */

    private final class ShellClient extends WebViewClient {

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri url = request.getUrl();
            if (!"https".equals(url.getScheme()) || !HOST.equals(url.getHost())) return null;

            String path = url.getPath();
            if (path == null || path.isEmpty() || "/".equals(path)) path = "/index.html";

            String name = path.substring(1).replace('\\', '/');
            if (name.indexOf("..") >= 0) return text("text/plain", 404, "");
            // The page is already local; an extra cache layer would only serve stale copies.
            if ("sw.js".equals(name)) return text("application/javascript", 404, "");

            try {
                if ("index.html".equals(name)) {
                    String html = readAsset(name);
                    if (html == null) return text("text/html", 404, "not found");
                    byte[] bytes = injectShell(html).getBytes(StandardCharsets.UTF_8);
                    return new WebResourceResponse("text/html", "utf-8",
                            200, "OK", noStore(), new ByteArrayInputStream(bytes));
                }
                InputStream in = getAssets().open(name);
                return new WebResourceResponse(mimeOf(name), "utf-8",
                        200, "OK", noStore(), in);
            } catch (IOException e) {
                return text("text/plain", 404, "not found");
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri url = request.getUrl();
            String scheme = url.getScheme();
            if ("blob".equals(scheme) || "data".equals(scheme) || "about".equals(scheme)) return false;
            return !HOST.equals(url.getHost());
        }
    }

    private static String injectShell(String html) {
        String tag = "<script src=\"shell.js\"></script>";
        String charset = "<meta charset=\"utf-8\">";
        int at = html.indexOf(charset);
        if (at >= 0) {
            int end = at + charset.length();
            return html.substring(0, end) + tag + html.substring(end);
        }
        int head = html.indexOf("<head>");
        if (head >= 0) {
            int end = head + "<head>".length();
            return html.substring(0, end) + tag + html.substring(end);
        }
        return tag + html;
    }

    private String readAsset(String name) throws IOException {
        try (InputStream in = getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(8192, in.available()));
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String mimeOf(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".html")) return "text/html";
        if (n.endsWith(".js")) return "application/javascript";
        if (n.endsWith(".css")) return "text/css";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".webmanifest") || n.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    private static Map<String, String> noStore() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Cache-Control", "no-store, no-cache, must-revalidate");
        headers.put("Pragma", "no-cache");
        return headers;
    }

    private static WebResourceResponse text(String mime, int code, String body) {
        return new WebResourceResponse(mime, "utf-8", code, code == 200 ? "OK" : "Not Found",
                noStore(), new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }

    /* ------------------------------------------------------------------ */
    /* Camera / microphone                                                */
    /* ------------------------------------------------------------------ */

    private final class ShellChrome extends WebChromeClient {

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            final String[] resources = request.getResources();
            note("ask " + TextUtils.join(",", resources));

            ui.post(() -> {
                List<String> wanted = new ArrayList<>();
                for (String resource : resources) {
                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                            || PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                        wanted.add(resource);
                    }
                }
                if (wanted.isEmpty()) {
                    note("deny unsupported");
                    request.deny();
                    return;
                }

                List<String> missing = new ArrayList<>();
                for (String resource : wanted) {
                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                        need(missing, android.Manifest.permission.RECORD_AUDIO);
                    } else {
                        need(missing, android.Manifest.permission.CAMERA);
                    }
                }
                if (missing.isEmpty()) {
                    note("grant " + TextUtils.join(",", wanted));
                    request.grant(wanted.toArray(new String[0]));
                    return;
                }

                if (pendingPermission != null) {
                    pendingPermission.deny();
                    pendingPermission = null;
                }
                pendingPermission = request;
                requestPermissions(missing.toArray(new String[0]), REQ_WEB_PERMISSION);
            });
        }

        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                         FileChooserParams params) {
            if (pendingChooser != null) {
                pendingChooser.onReceiveValue(null);
                pendingChooser = null;
            }
            pendingChooser = callback;
            try {
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "选择备份文件"), REQ_PICK_FILE);
                return true;
            } catch (Exception e) {
                pendingChooser = null;
                return false;
            }
        }
    }

    private void need(List<String> list, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
                && !list.contains(permission)) {
            list.add(permission);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        if (code != REQ_WEB_PERMISSION) {
            super.onRequestPermissionsResult(code, permissions, results);
            return;
        }
        boolean granted = true;
        for (int result : results) {
            if (result != PackageManager.PERMISSION_GRANTED) granted = false;
        }
        PermissionRequest request = pendingPermission;
        pendingPermission = null;
        if (request == null) return;

        if (granted) {
            note("grant " + TextUtils.join(",", request.getResources()));
            request.grant(request.getResources());
        } else {
            note("deny user");
            request.deny();
            Toast.makeText(this, "需要在系统设置里允许相机 / 麦克风", Toast.LENGTH_LONG).show();
        }
    }

    private void note(String line) {
        synchronized (permissionLog) {
            if (permissionLog.length() > 400) permissionLog.setLength(0);
            permissionLog.append(line).append("; ");
        }
        android.util.Log.d("Weiguang", line);
    }

    /* ------------------------------------------------------------------ */
    /* Import                                                            */
    /* ------------------------------------------------------------------ */

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        if (code == REQ_PICK_FILE) {
            ValueCallback<Uri[]> callback = pendingChooser;
            pendingChooser = null;
            if (callback != null) {
                callback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            }
            return;
        }

        if (code == REQ_SAVE_FILE) {
            if (result == RESULT_OK && data != null && data.getData() != null) {
                final Uri target = data.getData();
                final File source = saveFile;
                saveFile = null;
                if (source != null) {
                    new Thread(() -> copyTo(source, target)).start();
                }
            } else {
                Toast.makeText(this, "已取消保存", Toast.LENGTH_SHORT).show();
                if (saveFile != null) {
                    saveFile.delete();
                    saveFile = null;
                }
            }
            return;
        }

        super.onActivityResult(code, result, data);
    }

    private void copyTo(File source, Uri target) {
        boolean ok = false;
        try (InputStream in = new FileInputStream(source);
             OutputStream out = getContentResolver().openOutputStream(target, "w")) {
            if (out != null) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                out.flush();
                ok = true;
            }
        } catch (Exception ignored) {
            // reported below
        }
        source.delete();
        final boolean saved = ok;
        ui.post(() -> Toast.makeText(this,
                saved ? "备份已保存" : "保存失败了", Toast.LENGTH_SHORT).show());
    }

    /* ------------------------------------------------------------------ */
    /* Export bridge (see assets/shell.js)                                */
    /* ------------------------------------------------------------------ */

    public final class Bridge {

        @JavascriptInterface
        public String webViewVersion() {
            return MainActivity.this.webViewVersion();
        }

        @JavascriptInterface
        public String permissionLog() {
            synchronized (permissionLog) {
                return permissionLog.toString().trim();
            }
        }

        @JavascriptInterface
        public void toast(String message) {
            if (message == null) return;
            final String text = message.length() > 160 ? message.substring(0, 160) : message;
            ui.post(() -> Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show());
        }

        @JavascriptInterface
        public void beginSave(String name, String mime) {
            closeSaveStream();
            if (saveFile != null) {
                saveFile.delete();
                saveFile = null;
            }
            try {
                saveFile = new File(getCacheDir(), "weiguang-export.tmp");
                saveStream = new FileOutputStream(saveFile);
                saveName = cleanName(name);
                saveMime = (mime == null || mime.isEmpty()) ? "application/json" : mime;
            } catch (Exception e) {
                closeSaveStream();
                if (saveFile != null) {
                    saveFile.delete();
                    saveFile = null;
                }
                fail("无法创建临时文件");
            }
        }

        @JavascriptInterface
        public void appendChunk(String base64) {
            if (saveStream == null || base64 == null) return;
            try {
                saveStream.write(Base64.decode(base64, Base64.DEFAULT));
            } catch (Exception e) {
                fail("写入失败");
            }
        }

        @JavascriptInterface
        public void endSave() {
            if (saveStream == null) {
                fail("没有可保存的内容");
                return;
            }
            try {
                saveStream.flush();
                saveStream.close();
            } catch (IOException ignored) {
                // the copy below will notice a truncated file
            }
            saveStream = null;
            final String name = saveName;
            final String mime = saveMime;
            ui.post(() -> openSavePicker(name, mime));
        }

        @JavascriptInterface
        public void saveFailed(String reason) {
            fail(reason);
        }
    }

    private void openSavePicker(String name, String mime) {
        if (saveFile == null || !saveFile.exists() || saveFile.length() == 0) {
            Toast.makeText(this, "没有可保存的内容", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(mime);
            intent.putExtra(Intent.EXTRA_TITLE, name);
            startActivityForResult(intent, REQ_SAVE_FILE);
        } catch (Exception e) {
            Toast.makeText(this, "这台设备没有文件选择器", Toast.LENGTH_LONG).show();
            saveFile.delete();
            saveFile = null;
        }
    }

    private void fail(String reason) {
        closeSaveStream();
        if (saveFile != null) {
            saveFile.delete();
            saveFile = null;
        }
        final String message = (reason == null || reason.isEmpty()) ? "保存失败了" : reason;
        ui.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void closeSaveStream() {
        if (saveStream != null) {
            try {
                saveStream.close();
            } catch (IOException ignored) {
                // nothing left to do
            }
            saveStream = null;
        }
    }

    private static String cleanName(String name) {
        if (name == null || name.trim().isEmpty()) return "weiguang-backup.json";
        String clean = name.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "-").trim();
        if (clean.length() > 80) clean = clean.substring(0, 80);
        if (!clean.toLowerCase(Locale.ROOT).endsWith(".json")) clean = clean + ".json";
        return clean;
    }

    private String webViewVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PackageInfo info = WebView.getCurrentWebViewPackage();
                if (info != null && info.versionName != null) return info.versionName;
            } catch (Throwable ignored) {
                // best effort only
            }
        }
        return "";
    }
}
