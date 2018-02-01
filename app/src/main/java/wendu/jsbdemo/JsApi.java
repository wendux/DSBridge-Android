package wendu.jsbdemo;

import android.os.CountDownTimer;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

import wendu.dsbridge.CompletionHandler;

/**
 * Created by du on 16/12/31.
 */

public class JsApi{
    @JavascriptInterface
    public String testSyn(Object msg)  {
        return msg + "［syn call］";
    }

    @JavascriptInterface
    public void testAsyn(Object msg, CompletionHandler handler) throws JSONException {
        handler.complete(msg+" [ asyn call]");
    }

    @JavascriptInterface
    public String testNoArgSyn(Object arg) throws JSONException {
        return  "testNoArgSyn called [ syn call]";
    }

    @JavascriptInterface
    public void testNoArgAsyn(Object object,CompletionHandler handler) throws JSONException {
        handler.complete( "testNoArgAsyn   called [ asyn call]");
    }


    //@JavascriptInterface
    //此方法没有@JavascriptInterface标注将不会被调用
    public void testNever(Object jsonObject) throws JSONException {
        //return jsonObject.getString("msg") + "[ never call]";
    }

    @JavascriptInterface
    public void callProgress(Object args, final CompletionHandler handler) throws JSONException {

        new CountDownTimer(11000, 1000) {
            int i=10;
            @Override
            public void onTick(long millisUntilFinished) {
                //setProgressData can be called many times util complete be called.
                handler.setProgressData((i--));

            }
            @Override
            public void onFinish() {
                //complete the js invocation with data; handler will expire when complete is called
                handler.complete();

            }
        }.start();
    }

    /**
     *
     * @param requestData
     * @param handler
     *
     * Note: This method is for Fly.js
     * In browsers, Ajax requests are sent by browsers, and Fly can
     * redirect requests to native, more about Fly see  https://github.com/wendux/fly
     */

    @JavascriptInterface
    public void onAjaxRequest(Object requestData, CompletionHandler handler){
        // Handle ajax request redirected by Fly
        AjaxHandler.onAjaxRequest((JSONObject)requestData,handler);
    }


}