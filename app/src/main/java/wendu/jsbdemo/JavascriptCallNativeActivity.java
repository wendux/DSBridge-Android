package wendu.jsbdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import wendu.dsbridge.DWebView;
import wendu.dsbridge.WebViewEventImpl;

public class JavascriptCallNativeActivity extends AppCompatActivity {

    public WebViewEventImpl mWebViewEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_js_call_native);
        DWebView dwebView = findViewById(R.id.webview);
        // set debug mode
        mWebViewEvent = new WebViewEventImpl(dwebView);
        mWebViewEvent.setWebContentsDebuggingEnabled(true);
        mWebViewEvent.addJavascriptObject(new JsApi(), null);
        mWebViewEvent.addJavascriptObject(new JsEchoApi(), "echo");
        mWebViewEvent.loadUrl("file:///android_asset/js-call-native.html");
    }
}
