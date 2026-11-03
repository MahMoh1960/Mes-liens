package com.meslinks.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import org.json.JSONArray;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

public class MainActivity extends Activity {

    private static final int REQ_FILE = 1;
    private static final int REQ_PICK = 2;
    private static final int REQ_VOICE = 3;
    private static final String ACTION_VOICE = "com.meslinks.app.VOICE";
    private boolean pendingVoice = false;
    private WebView web;
    private ValueCallback<Uri[]> filePathCb;
    private String pendingShared = null;
    private boolean pageLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF0B1B3A);
        getWindow().setNavigationBarColor(0xFF0B1B3A);

        web = new WebView(this);
        web.setBackgroundColor(0xFF0B1B3A);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);

        web.addJavascriptInterface(new Bridge(), "Android");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                if (pendingShared != null) {
                    pushShared(pendingShared);
                    pendingShared = null;
                }
                if (pendingVoice) {
                    pendingVoice = false;
                    pushVoice();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String u = request.getUrl().toString();
                if (u.startsWith("file:///android_asset/")) return false;
                openExternal(u);
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (filePathCb != null) filePathCb.onReceiveValue(null);
                filePathCb = cb;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    filePathCb = null;
                    return false;
                }
                return true;
            }
        });

        handleIntent(getIntent());
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null && !text.trim().isEmpty()) {
                if (pageLoaded) pushShared(text);
                else pendingShared = text;
            }
            // évite de rejouer le partage à la rotation
            intent.setAction(Intent.ACTION_MAIN);
        } else if (ACTION_VOICE.equals(intent.getAction())) {
            if (pageLoaded) pushVoice();
            else pendingVoice = true;
            intent.setAction(Intent.ACTION_MAIN);
        }
    }

    private void pushVoice() {
        web.post(new Runnable() {
            @Override
            public void run() {
                web.evaluateJavascript("window.startVoice && window.startVoice();", null);
            }
        });
    }

    private void pushShared(String text) {
        final String js = "window.onShared && window.onShared(" + JSONObject.quote(text) + ");";
        web.post(new Runnable() {
            @Override
            public void run() {
                web.evaluateJavascript(js, null);
            }
        });
    }

    private void openExternal(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            if (filePathCb != null) {
                filePathCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                filePathCb = null;
            }
            return;
        }
        if (requestCode == REQ_PICK) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                }
                String name = "";
                Cursor c = null;
                try {
                    c = getContentResolver().query(uri, null, null, null, null);
                    if (c != null && c.moveToFirst()) {
                        int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (idx >= 0) name = c.getString(idx);
                    }
                } catch (Exception ignored) {
                } finally {
                    if (c != null) c.close();
                }
                String mime = getContentResolver().getType(uri);
                if (mime == null) mime = "*/*";
                final String js = "window.onPicked && window.onPicked(" + JSONObject.quote(uri.toString()) + ","
                        + JSONObject.quote(name == null ? "" : name) + "," + JSONObject.quote(mime) + ");";
                web.post(new Runnable() {
                    @Override
                    public void run() {
                        web.evaluateJavascript(js, null);
                    }
                });
            }
            return;
        }
        if (requestCode == REQ_VOICE) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (res != null && !res.isEmpty()) {
                    JSONArray arr = new JSONArray();
                    for (String x : res) arr.put(x);
                    final String js = "window.onVoice && window.onVoice(" + arr.toString() + ");";
                    web.post(new Runnable() {
                        @Override
                        public void run() {
                            web.evaluateJavascript(js, null);
                        }
                    });
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.onBack && window.onBack()) === true", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (!"true".equals(value)) finish();
            }
        });
    }

    private String iconB64(Drawable d) {
        try {
            Bitmap bmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            d.setBounds(0, 0, 48, 48);
            d.draw(c);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
            return "data:image/png;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private class Bridge {
        @JavascriptInterface
        public void openUrl(final String url) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    openExternal(url);
                }
            });
        }

        @JavascriptInterface
        public void pickFile() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("*/*");
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        startActivityForResult(i, REQ_PICK);
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        @JavascriptInterface
        public void openFile(final String uri, final String mime) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(Uri.parse(uri), (mime == null || mime.isEmpty()) ? "*/*" : mime);
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Impossible d'ouvrir le fichier / تعذر فتح الملف", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @JavascriptInterface
        public String getApps() {
            try {
                PackageManager pm = getPackageManager();
                Intent main = new Intent(Intent.ACTION_MAIN, null);
                main.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
                JSONArray arr = new JSONArray();
                Set<String> seen = new HashSet<String>();
                for (ResolveInfo ri : list) {
                    String pkg = ri.activityInfo.packageName;
                    if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
                    JSONObject o = new JSONObject();
                    o.put("p", pkg);
                    o.put("n", String.valueOf(ri.loadLabel(pm)));
                    o.put("i", iconB64(ri.loadIcon(pm)));
                    arr.put(o);
                }
                return arr.toString();
            } catch (Exception e) {
                return "[]";
            }
        }

        @JavascriptInterface
        public void openApp(final String pkg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
                        if (i == null) {
                            Toast.makeText(MainActivity.this, "Application introuvable / التطبيق غير موجود", Toast.LENGTH_LONG).show();
                            return;
                        }
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Impossible d'ouvrir l'application / تعذر فتح التطبيق", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @JavascriptInterface
        public void listen(final String lang) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
                        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
                        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez… / تكلم…");
                        startActivityForResult(i, REQ_VOICE);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Reconnaissance vocale indisponible / الميزة غير متاحة", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @JavascriptInterface
        public void pinVoice() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Build.VERSION.SDK_INT >= 26) {
                            ShortcutManager sm = getSystemService(ShortcutManager.class);
                            if (sm != null && sm.isRequestPinShortcutSupported()) {
                                Intent in = new Intent(MainActivity.this, MainActivity.class);
                                in.setAction(ACTION_VOICE);
                                ShortcutInfo info = new ShortcutInfo.Builder(MainActivity.this, "voice")
                                        .setShortLabel("Voix Mes Liens")
                                        .setIcon(Icon.createWithResource(MainActivity.this, R.drawable.ic_mic))
                                        .setIntent(in)
                                        .build();
                                sm.requestPinShortcut(info, null);
                                return;
                            }
                        }
                        Toast.makeText(MainActivity.this, "Maintenez l'icône de l'application, puis le raccourci vocal / اضغط مطولاً على أيقونة التطبيق", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Raccourci indisponible / الاختصار غير متاح", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @JavascriptInterface
        public void copy(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("lien", text));
                }
            });
        }

        @JavascriptInterface
        public void share(final String title, final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("text/plain");
                        i.putExtra(Intent.EXTRA_SUBJECT, title);
                        i.putExtra(Intent.EXTRA_TEXT, text);
                        startActivity(Intent.createChooser(i, title));
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }
}
