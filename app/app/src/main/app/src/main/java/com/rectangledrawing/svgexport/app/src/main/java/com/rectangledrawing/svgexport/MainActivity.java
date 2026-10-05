package com.rectangledrawing.svgexport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int SAVE_SVG_REQUEST = 1057;
    private WebView webView;
    private String pendingSvg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new SvgExportBridge(), "AndroidExport");

        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private final class SvgExportBridge {
        @JavascriptInterface
        public void saveSvg(String name, String svg) {
            if (svg == null || !svg.startsWith("<?xml")
                    || svg.length() > 2000000) {
                return;
            }

            final String safeName = (name == null ? "圆孔图.svg" : name)
                    .replaceAll("[\\\\/:*?\"<>|]", "_");

            runOnUiThread(() -> {
                pendingSvg = svg;

                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/svg+xml");
                intent.putExtra(Intent.EXTRA_TITLE, safeName);

                try {
                    startActivityForResult(intent, SAVE_SVG_REQUEST);
                } catch (Exception error) {
                    pendingSvg = null;
                    Toast.makeText(
                            MainActivity.this,
                            "无法打开文件保存窗口",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != SAVE_SVG_REQUEST) return;

        String content = pendingSvg;
        pendingSvg = null;

        if (resultCode != RESULT_OK || data == null
                || data.getData() == null || content == null) {
            return;
        }

        try (OutputStream output =
                     getContentResolver().openOutputStream(data.getData())) {
            if (output == null) {
                throw new IllegalStateException("No output stream");
            }

            output.write(content.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "SVG 已保存", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(
                    this,
                    "保存失败，请重新选择位置",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
