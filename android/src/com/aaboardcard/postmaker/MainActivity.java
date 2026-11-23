package com.aaboardcard.postmaker;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONObject;

public class MainActivity extends Activity {
    static final int APK_VER = 3;          // AndroidManifest versionCode 와 같게
    static final int BUNDLED_WEB = __WEBV__; // 앱에 들어있는 화면(index.html) 버전
    static final String BASE = "https://raw.githubusercontent.com/KOYUBIN/aaboardcard/main/dist/";
    static final String HOST = "appassets.androidplatform.net"; // 앱 내부 전용 주소(외부로 나가지 않음)

    private WebView web;
    private ValueCallback<Uri[]> pending;
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("app", MODE_PRIVATE);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (!HOST.equals(u.getHost())) return null;
                String p = u.getPath();
                try {
                    if (p == null || p.equals("/") || p.equals("/index.html"))
                        return new WebResourceResponse("text/html", "utf-8", new ByteArrayInputStream(pageBytes()));
                    if (p.startsWith("/fonts/") && !p.contains(".."))
                        return new WebResourceResponse("font/woff2", null, getAssets().open(p.substring(1)));
                } catch (Exception e) { }
                return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", new HashMap<String, String>(), new ByteArrayInputStream(new byte[0]));
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return !HOST.equals(r.getUrl().getHost()); // 앱 화면 밖으로는 이동하지 않음
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (pending != null) pending.onReceiveValue(null);
                pending = cb;
                String type = "*/*";
                String[] t = p.getAcceptTypes();
                if (t != null && t.length > 0 && t[0] != null && t[0].contains("/")) type = t[0];
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType(type);
                if (p.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try { startActivityForResult(Intent.createChooser(i, "파일 선택"), 1); }
                catch (Exception e) { pending = null; return false; }
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        loadPage();
    }

    File cached() { return new File(getFilesDir(), "index.html"); }

    int webVer() {
        int c = prefs.getInt("webv", 0);
        return (c > BUNDLED_WEB && cached().exists()) ? c : BUNDLED_WEB;
    }

    static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] buf = new byte[16384]; int n;
        while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
        in.close();
        return o.toByteArray();
    }

    byte[] pageBytes() throws Exception {
        try {
            if (webVer() > BUNDLED_WEB) {
                byte[] d = readAll(new FileInputStream(cached()));
                if (new String(d, "UTF-8").contains("</html>")) return d;
            }
        } catch (Exception e) { }
        return readAll(getAssets().open("index.html"));
    }

    void loadPage() { web.loadUrl("https://" + HOST + "/index.html"); }

    void js(final String code) {
        runOnUiThread(new Runnable() { public void run() { web.evaluateJavascript(code, null); } });
    }

    static byte[] http(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000); c.setReadTimeout(20000); c.setUseCaches(false);
        if (c.getResponseCode() != 200) throw new Exception("http " + c.getResponseCode());
        return readAll(c.getInputStream());
    }

    void checkUpdate(final boolean silent) {
        new Thread(new Runnable() { public void run() {
            try {
                JSONObject v = new JSONObject(new String(http(BASE + "version.json?t=" + System.currentTimeMillis()), "UTF-8"));
                int apk = v.optInt("apk", 0), w = v.optInt("web", 0);
                if (apk > APK_VER) {
                    String u = v.optString("apkUrl", "");
                    if (u.startsWith("https://github.com/KOYUBIN/aaboardcard/")) { js("window.__upd&&__upd('apk','" + u + "')"); return; }
                }
                if (w > webVer()) {
                    byte[] d = http(BASE + "index.html?t=" + System.currentTimeMillis());
                    String h = new String(d, "UTF-8");
                    if (!h.contains("</html>")) throw new Exception("bad html");
                    File tmp = new File(getFilesDir(), "index.tmp");
                    FileOutputStream o = new FileOutputStream(tmp); o.write(d); o.close();
                    if (!tmp.renameTo(cached())) throw new Exception("rename");
                    prefs.edit().putInt("webv", w).apply();
                    js("window.__upd&&__upd('web','')");
                    return;
                }
                if (!silent) js("window.__upd&&__upd('none','')");
            } catch (Exception e) {
                if (!silent) js("window.__upd&&__upd('err','')");
            }
        } }).start();
    }

    Bitmap decode(Uri u) throws Exception {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), u), new ImageDecoder.OnHeaderDecodedListener() {
            public void onHeaderDecoded(ImageDecoder d, ImageDecoder.ImageInfo info, ImageDecoder.Source s) {
                d.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                int w = info.getSize().getWidth(), h = info.getSize().getHeight();
                float sc = Math.min(1f, 2160f / Math.max(w, h));
                if (sc < 1f) d.setTargetSize(Math.max(1, Math.round(w * sc)), Math.max(1, Math.round(h * sc)));
            }
        });
    }

    void deliver(final ArrayList<Uri> uris) {
        new Thread(new Runnable() { public void run() {
            int ok = 0;
            for (Uri u : uris) {
                try {
                    Bitmap bm = decode(u);
                    ByteArrayOutputStream o = new ByteArrayOutputStream();
                    bm.compress(Bitmap.CompressFormat.JPEG, 90, o);
                    bm.recycle();
                    js("window.__pickedOne&&__pickedOne('data:image/jpeg;base64," + Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP) + "')");
                    ok++;
                } catch (Throwable e) { /* 이 사진은 건너뜀 */ }
            }
            js("window.__pickedDone&&__pickedDone(" + ok + "," + uris.size() + ")");
        } }).start();
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        ArrayList<Uri> list = new ArrayList<Uri>();
        if (res == RESULT_OK && data != null) {
            ClipData cd = data.getClipData();
            if (cd != null && cd.getItemCount() > 0) { for (int i = 0; i < cd.getItemCount(); i++) list.add(cd.getItemAt(i).getUri()); }
            else if (data.getData() != null) list.add(data.getData());
        }
        if (req == 1) {
            if (pending == null) return;
            pending.onReceiveValue(list.isEmpty() ? null : list.toArray(new Uri[0]));
            pending = null;
        } else if (req == 2) {
            deliver(list);
        }
    }

    @Override public void onBackPressed() { moveTaskToBack(true); }

    class Bridge {
        private final HashMap<Integer, Object[]> outs = new HashMap<Integer, Object[]>();
        private int seq = 0;

        private Object[] open(String name) throws Exception {
            String n = name.toLowerCase();
            boolean img = n.endsWith(".png"), vid = n.endsWith(".mp4") || n.endsWith(".webm");
            String mime = img ? "image/png" : n.endsWith(".mp4") ? "video/mp4" : n.endsWith(".webm") ? "video/webm"
                        : n.endsWith(".zip") ? "application/zip" : n.endsWith(".json") ? "application/json" : "application/octet-stream";
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, (img ? "Pictures" : vid ? "Movies" : "Download") + "/AABoardCard");
            v.put(MediaStore.MediaColumns.IS_PENDING, 1);
            String vol = MediaStore.VOLUME_EXTERNAL_PRIMARY;
            Uri col = img ? MediaStore.Images.Media.getContentUri(vol) : vid ? MediaStore.Video.Media.getContentUri(vol) : MediaStore.Downloads.getContentUri(vol);
            Uri u = getContentResolver().insert(col, v);
            if (u == null) throw new Exception("insert");
            return new Object[]{ u, getContentResolver().openOutputStream(u) };
        }

        private void close(Object[] o) throws Exception {
            ((OutputStream) o[1]).close();
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.IS_PENDING, 0);
            getContentResolver().update((Uri) o[0], v, null, null);
        }

        @JavascriptInterface public String save(String name, String b64) {
            try {
                Object[] o = open(name);
                ((OutputStream) o[1]).write(Base64.decode(b64, Base64.DEFAULT));
                close(o);
                return "ok";
            } catch (Exception e) { return "fail"; }
        }

        // 큰 파일(영상)은 나눠서 받는다
        @JavascriptInterface public synchronized int saveBegin(String name) {
            try { Object[] o = open(name); int id = ++seq; outs.put(id, o); return id; }
            catch (Exception e) { return -1; }
        }

        @JavascriptInterface public synchronized String saveChunk(int id, String b64) {
            try { ((OutputStream) outs.get(id)[1]).write(Base64.decode(b64, Base64.DEFAULT)); return "ok"; }
            catch (Exception e) { return "fail"; }
        }

        @JavascriptInterface public synchronized String saveEnd(int id) {
            try { close(outs.remove(id)); return "ok"; }
            catch (Exception e) { return "fail"; }
        }

        @JavascriptInterface public void pick(final boolean multi) {
            runOnUiThread(new Runnable() { public void run() {
                try {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("image/*");
                    if (multi) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    startActivityForResult(Intent.createChooser(i, "사진 선택"), 2);
                } catch (Exception e) { js("window.__pickedDone&&__pickedDone(0,1)"); }
            } });
        }

        @JavascriptInterface public String info() { return "앱 " + APK_VER + " · 화면 " + webVer(); }

        @JavascriptInterface public void checkUpdate(boolean silent) { MainActivity.this.checkUpdate(silent); }

        @JavascriptInterface public void reload() {
            runOnUiThread(new Runnable() { public void run() { loadPage(); } });
        }

        @JavascriptInterface public void openUrl(String url) {
            if (url == null || !url.startsWith("https://github.com/KOYUBIN/aaboardcard/")) return;
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) { }
        }
    }
}
