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
    public String testSyn(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("msg") + "［syn call］";
    }

    //@JavascriptInterface
    //此方法没有@JavascriptInterface标注将不会被调用
    public String testNever(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("msg") + "[ never call]";
    }

    @JavascriptInterface
    public String testNoArgSyn(JSONObject jsonObject) throws JSONException {
        return  "testNoArgSyn called [ syn call]";
    }

    @JavascriptInterface
    public void testNoArgAsyn(JSONObject jsonObject,CompletionHandler handler) throws JSONException {
        handler.complete( "testNoArgAsyn  called [ asyn call]");
    }

    @JavascriptInterface
    public void testAsyn(JSONObject jsonObject, CompletionHandler handler) throws JSONException {
        handler.complete(jsonObject.getString("msg")+" [ asyn call]");
    }
}