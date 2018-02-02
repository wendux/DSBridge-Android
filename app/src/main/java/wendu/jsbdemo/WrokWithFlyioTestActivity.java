package wendu.jsbdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;

import wendu.dsbridge.CompletionHandler;
import wendu.dsbridge.DWebView;

public class WrokWithFlyioTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrok_with_flyio_test);
        DWebView dWebView= (DWebView) findViewById(R.id.webview);
        dWebView.addJavascriptObject(new Object(){

            /**
             * Note: This method is for Fly.js
             * In browser, Ajax requests are sent by browser, but Fly can
             * redirect requests to native, more about Fly see  https://github.com/wendux/fly
             * @param requestData passed by fly.js, more detail reference https://wendux.github.io/dist/#/doc/flyio-en/native
             * @param handler
             */
            @JavascriptInterface
            public void onAjaxRequest(Object requestData, CompletionHandler handler){
                // Handle ajax request redirected by Fly
                AjaxHandler.onAjaxRequest((JSONObject)requestData,handler);
            }

        },null);

        dWebView.loadUrl("file:///android_asset/fly.html");
    }
}
