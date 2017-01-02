package wendu.jsbdemo;
import android.webkit.JavascriptInterface;
import org.json.JSONException;
import org.json.JSONObject;
import wendu.dsbridge.CompletionHandler;

/**
 * Created by du on 16/12/31.
 */

public class JsApi{
    @JavascriptInterface
    String testSyn(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("msg") + "［syn call］";
    }

    //@JavascriptInterface
    //此方法没有@JavascriptInterface标注将不会被调用
    String testNever(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("msg") + "[ never call]";
    }

    @JavascriptInterface
    String testNoArgSyn(JSONObject jsonObject) throws JSONException {
        return  "testNoArgSyn called [ syn call]";
    }

    @JavascriptInterface
    void testNoArgAsyn(JSONObject jsonObject,CompletionHandler handler) throws JSONException {
        handler.complete( "testNoArgAsyn  called [ asyn call]");
    }

    @JavascriptInterface
    void testAsyn(JSONObject jsonObject, CompletionHandler handler) throws JSONException {
        handler.complete(jsonObject.getString("msg")+" [asyn call]");
    }
}