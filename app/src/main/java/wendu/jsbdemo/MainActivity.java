package wendu.jsbdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import wendu.dsbridge.DWebView;
import wendu.dsbridge.OnReturnValue;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final DWebView webView= (DWebView) findViewById(R.id.webview);
        // set debug model
        DWebView.setWebContentsDebuggingEnabled(true);

        webView.addJavascriptObject(new JsApi());
        webView.setJavascriptObjectWithNamespace(new JsApi(),"test");

        webView.loadUrl("file:///android_asset/test.html");

        //call javascript hanlders

        webView.callHandler("addValue",new Object[]{1,6},new OnReturnValue(){
            @Override
            public void onValue(String retValue) {
                Log.d("jsbridge","call succeed,return value is: "+retValue);
            }
        });
        webView.callHandler("append",new Object[]{"I","love","you"},new OnReturnValue(){
            @Override
            public void onValue(String retValue) {
                Log.d("jsbridge","call succeed, append string is: "+retValue);
            }
        });
        webView.callHandler("startTimer",null,new OnReturnValue(){
            @Override
            public void onValue(String retValue) {
                Log.d("jsbridge","The timer : "+retValue);
            }
        });

        //namespace test
        webView.callHandler("test.test1",null,new OnReturnValue(){
            @Override
            public void onValue(String retValue) {
                Log.d("jsbridge","Namespace test.test1: "+retValue);
            }
        });

        webView.callHandler("test.test2",null,new OnReturnValue(){
            @Override
            public void onValue(String retValue) {
                Log.d("jsbridge","Namespace test.test2: "+retValue);
            }
        });

        // test if th
        webView.hasJavascriptMethod("addValue", new DWebView.MethodExistCallback() {
            @Override
            public void onResult(boolean exist) {
                Log.d("jsbridge", "method exist:" + exist);
            }
        });


    }
}
