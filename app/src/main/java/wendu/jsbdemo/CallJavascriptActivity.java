package wendu.jsbdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;

import wendu.dsbridge.DWebView;
import wendu.dsbridge.OnReturnValue;
import wendu.dsbridge.WebViewEventImpl;

public class CallJavascriptActivity extends AppCompatActivity implements View.OnClickListener{

    DWebView dWebView;
    public WebViewEventImpl mWebViewEvent;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_javascript);
        findViewById(R.id.addValue).setOnClickListener(this);
        findViewById(R.id.append).setOnClickListener(this);
        findViewById(R.id.startTimer).setOnClickListener(this);
        findViewById(R.id.synAddValue).setOnClickListener(this);
        findViewById(R.id.synGetInfo).setOnClickListener(this);
        findViewById(R.id.asynAddValue).setOnClickListener(this);
        findViewById(R.id.asynGetInfo).setOnClickListener(this);
        findViewById(R.id.hasMethodAddValue).setOnClickListener(this);
        findViewById(R.id.hasMethodXX).setOnClickListener(this);
        findViewById(R.id.hasMethodAsynAddValue).setOnClickListener(this);
        findViewById(R.id.hasMethodAsynXX).setOnClickListener(this);

        dWebView = findViewById(R.id.webview);
        mWebViewEvent = new WebViewEventImpl(dWebView);
        mWebViewEvent.setWebContentsDebuggingEnabled(true);
        mWebViewEvent.loadUrl("file:///android_asset/native-call-js.html");


    }


    void showToast(Object o) {
        Toast.makeText(this, o.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addValue:
                mWebViewEvent.callHandler("addValue", new Object[]{3, 4}, new OnReturnValue<Integer>() {
                    @Override
                    public void onValue(Integer retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.append:
                mWebViewEvent.callHandler("append", new Object[]{"I", "love", "you"}, new OnReturnValue<String>() {
                    @Override
                    public void onValue(String retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.startTimer:
                mWebViewEvent.callHandler("startTimer", new OnReturnValue<Integer>() {
                    @Override
                    public void onValue(Integer retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.synAddValue:
                mWebViewEvent.callHandler("syn.addValue", new Object[]{5, 6}, new OnReturnValue<Integer>() {
                    @Override
                    public void onValue(Integer retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.synGetInfo:
                mWebViewEvent.callHandler("syn.getInfo", new OnReturnValue<JSONObject>() {
                    @Override
                    public void onValue(JSONObject retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.asynAddValue:
                mWebViewEvent.callHandler("asyn.addValue", new Object[]{5, 6}, new OnReturnValue<Integer>() {
                    @Override
                    public void onValue(Integer retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.asynGetInfo:
                mWebViewEvent.callHandler("asyn.getInfo", new OnReturnValue<JSONObject>() {
                    @Override
                    public void onValue(JSONObject retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.hasMethodAddValue:
                mWebViewEvent.hasJavascriptMethod("addValue", new OnReturnValue<Boolean>() {
                    @Override
                    public void onValue(Boolean retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.hasMethodXX:
                mWebViewEvent.hasJavascriptMethod("XX", new OnReturnValue<Boolean>() {
                    @Override
                    public void onValue(Boolean retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.hasMethodAsynAddValue:
                mWebViewEvent.hasJavascriptMethod("asyn.addValue", new OnReturnValue<Boolean>() {
                    @Override
                    public void onValue(Boolean retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.hasMethodAsynXX:
                mWebViewEvent.hasJavascriptMethod("asyn.XX", new OnReturnValue<Boolean>() {
                    @Override
                    public void onValue(Boolean retValue) {
                        showToast(retValue);
                    }
                });
                break;
        }

    }
}
