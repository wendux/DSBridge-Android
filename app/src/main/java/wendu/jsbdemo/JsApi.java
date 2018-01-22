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
    public String testSync(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("msg") + "［sync call］";
    }

    //@JavascriptInterface
    //此方法没有@JavascriptInterface标注将不会被调用
    public String testNever(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("msg") + "[never call]";
    }

    @JavascriptInterface
    public String testNoArgSync(JSONObject jsonObject) throws JSONException {
        return "testNoArgSyn called [sync call]";
    }

    @JavascriptInterface
    public void testNoArgAsync(JSONObject jsonObject, CompletionHandler handler) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("result", "testNoArgAsync called [async call]");
        handler.complete(result.toString());
    }

    @JavascriptInterface
    public void testAsync(JSONObject jsonObject, CompletionHandler handler) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("result", jsonObject.getString("msg") + " [async call]");
        handler.complete(result.toString());
    }

    @JavascriptInterface
    public void callProgress(JSONObject jsonObject, final CompletionHandler handler) throws JSONException {
        new CountDownTimer(11000, 1000) {
            int i = 10;
            @Override
            public void onTick(long millisUntilFinished) {
                // setProgressData can be called many times util complete be called.
                JSONObject result = new JSONObject();
                try {
                    result.put("result", i--);
                } catch (JSONException e) {}
                handler.setProgressData(result.toString());
            }
            @Override
            public void onFinish() {
                // complete the js invocation with data; handler will expire when complete is called
                handler.complete("");
            }
        }.start();
    }
}