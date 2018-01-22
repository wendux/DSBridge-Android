package wendu.jsbdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import wendu.dsbridge.DWebView;
import wendu.dsbridge.OnReturnValue;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final DWebView webView= (DWebView) findViewById(R.id.webview);
        webView.setJavascriptInterface(new JsApi());
        webView.clearCache(true);
        webView.loadUrl("file:///android_asset/test.html");
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                webView.callHandler("addValue", new Object[] { 1, "hello" }, new OnReturnValue(){
                    @Override
                    public void onValue(String retValue) {
                        Log.d("jsbridge", "sync callHandler succeed, return value is " + retValue);
                    }
                });

                webView.callHandler("addValueAsync", new Object[] { 1, "hello" }, new OnReturnValue(){
                    @Override
                    public void onValue(String retValue) {
                        Log.d("jsbridge", "async callHandler succeed, return value is " + retValue);
                    }
                });
            }
        });

    }
}
