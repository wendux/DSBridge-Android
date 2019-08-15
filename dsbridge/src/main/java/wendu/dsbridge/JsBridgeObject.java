package wendu.dsbridge;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Keep;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * <pre>
 *     author : sunqiao
 *     e-mail : sunqiao@kayak.com.cn
 *     time   : 2019/08/15
 *     desc   :
 *     version: 1.0
 * </pre>
 */
public class JsBridgeObject {

    private WebViewEventImpl mWebViewEvent;

    public JsBridgeObject(WebViewEventImpl webViewEvent) {
        this.mWebViewEvent = webViewEvent;
    }

    @Keep
    @JavascriptInterface
    public boolean hasNativeMethod(Object args) throws JSONException {
        JSONObject jsonObject = (JSONObject) args;
        String methodName = jsonObject.getString("name").trim();
        String type = jsonObject.getString("type").trim();
        String[] nameStr = mWebViewEvent.parseNamespace(methodName);
        Object jsb = mWebViewEvent.javaScriptNamespaceInterfaces.get(nameStr[0]);
        if (jsb != null) {
            Class<?> cls = jsb.getClass();
            boolean asyn = false;
            Method method = null;
            try {
                method = cls.getMethod(nameStr[1],
                        Object.class, CompletionHandler.class);
                asyn = true;
            } catch (Exception e) {
                try {
                    method = cls.getMethod(nameStr[1], Object.class);
                } catch (Exception ex) {

                }
            }
            if (method != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    JavascriptInterface annotation = method.getAnnotation(JavascriptInterface.class);
                    if (annotation == null) {
                        return false;
                    }
                }
                if ("all".equals(type) || (asyn && "asyn".equals(type) || (!asyn && "syn".equals(type)))) {
                    return true;
                }

            }
        }
        return false;
    }

    @Keep
    @JavascriptInterface
    public String closePage(Object object) throws JSONException {
        mWebViewEvent.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (mWebViewEvent.javascriptCloseWindowListener == null
                        || mWebViewEvent.javascriptCloseWindowListener.onClose()) {
                    Context context = mWebViewEvent.getWebView().getContext();
                    if (context instanceof Activity) {
                        ((Activity) context).onBackPressed();
                    }
                }
            }
        });
        return null;
    }

    @Keep
    @JavascriptInterface
    public void disableJavascriptDialogBlock(Object object) throws JSONException {
        JSONObject jsonObject = (JSONObject) object;
        mWebViewEvent.alertBoxBlock = !jsonObject.getBoolean("disable");
    }

    @Keep
    @JavascriptInterface
    public void dsinit(Object jsonObject) {
        mWebViewEvent.dispatchStartupQueue();
    }

    @Keep
    @JavascriptInterface
    public void returnValue(final Object obj) {
        mWebViewEvent.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                JSONObject jsonObject = (JSONObject) obj;
                Object data = null;
                try {
                    int id = jsonObject.getInt("id");
                    boolean isCompleted = jsonObject.getBoolean("complete");
                    OnReturnValue handler = mWebViewEvent.handlerMap.get(id);
                    if (jsonObject.has("data")) {
                        data = jsonObject.get("data");
                    }
                    if (handler != null) {
                        handler.onValue(data);
                        if (isCompleted) {
                            mWebViewEvent.handlerMap.remove(id);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
