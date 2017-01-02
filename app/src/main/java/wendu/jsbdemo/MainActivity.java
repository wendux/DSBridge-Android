package wendu.jsbdemo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import wendu.dsbridge.CompletionHandler;
import wendu.dsbridge.DWebView;
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
                webView.callHandler("test",new Object[]{1,"hello"},new CompletionHandler(){
                    @Override
                    public void complete(String retValue) {
                        Log.d("jsbridge","call succeed,return value is "+retValue);
                    }
                });

                webView.callHandler("test",null);
            }
        });

    }
}
