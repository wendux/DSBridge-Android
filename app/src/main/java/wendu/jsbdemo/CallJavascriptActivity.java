package wendu.jsbdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;

import wendu.dsbridge.DWebView;
import wendu.dsbridge.OnReturnValue;

public class CallJavascriptActivity extends AppCompatActivity implements View.OnClickListener {

    DWebView dWebView;

    public <T extends View> T getView(int viewId) {
        View view = findViewById(viewId);
        return (T) view;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_javascript);
        getView(R.id.addValue).setOnClickListener(this);
        getView(R.id.append).setOnClickListener(this);
        getView(R.id.startTimer).setOnClickListener(this);
        getView(R.id.synAddValue).setOnClickListener(this);
        getView(R.id.synGetInfo).setOnClickListener(this);
        getView(R.id.asynAddValue).setOnClickListener(this);
        getView(R.id.asynGetInfo).setOnClickListener(this);
        getView(R.id.hasMethodAddValue).setOnClickListener(this);
        getView(R.id.hasMethodXX).setOnClickListener(this);
        getView(R.id.hasMethodAsynAddValue).setOnClickListener(this);
        getView(R.id.hasMethodAsynXX).setOnClickListener(this);
        DWebView.setWebContentsDebuggingEnabled(true);
        dWebView= getView(R.id.webview);
        dWebView.loadUrl("file:///android_asset/native-call-js.html");



    }


    void showToast(Object o) {
        Toast.makeText(this, o.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addValue:
                dWebView.callHandler("addValue", new Object[]{3, 4}, new OnReturnValue<Integer>() {
                    @Override
                    public void onValue(Integer retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.append:
                dWebView.callHandler("append", new Object[]{"I", "love", "you"}, new OnReturnValue<String>() {
                    @Override
                    public void onValue(String retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.startTimer:
                dWebView.callHandler("startTimer", new OnReturnValue<Integer>() {
                    @Override
                    public void onValue(Integer retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.synAddValue:
                dWebView.callHandler("syn.addValue", new Object[]{5, 6}, new OnReturnValue<Integer>() {
                    @Override
                    public void onValue(Integer retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.synGetInfo:
                dWebView.callHandler("syn.getInfo", new OnReturnValue<JSONObject>() {
                    @Override
                    public void onValue(JSONObject retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.asynAddValue:
                dWebView.callHandler("asyn.addValue", new Object[]{5, 6}, new OnReturnValue<Integer>() {
                    @Override
                    public void onValue(Integer retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.asynGetInfo:
                dWebView.callHandler("asyn.getInfo", new OnReturnValue<JSONObject>() {
                    @Override
                    public void onValue(JSONObject retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.hasMethodAddValue:
                dWebView.hasJavascriptMethod("addValue", new OnReturnValue<Boolean>() {
                    @Override
                    public void onValue(Boolean retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.hasMethodXX:
                dWebView.hasJavascriptMethod("XX", new OnReturnValue<Boolean>() {
                    @Override
                    public void onValue(Boolean retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.hasMethodAsynAddValue:
                dWebView.hasJavascriptMethod("asyn.addValue", new OnReturnValue<Boolean>() {
                    @Override
                    public void onValue(Boolean retValue) {
                        showToast(retValue);
                    }
                });
                break;
            case R.id.hasMethodAsynXX:
                dWebView.hasJavascriptMethod("asyn.XX", new OnReturnValue<Boolean>() {
                    @Override
                    public void onValue(Boolean retValue) {
                        showToast(retValue);
                    }
                });
                break;
        }

    }
}
