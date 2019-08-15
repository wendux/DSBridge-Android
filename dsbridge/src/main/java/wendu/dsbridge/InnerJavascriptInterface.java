package wendu.dsbridge;

import android.support.annotation.Keep;
import android.util.Log;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

public class InnerJavascriptInterface {

    private static final String LOG_TAG = "dsBridge";

    private WebViewEventImpl mWebViewEvent;

    public InnerJavascriptInterface(WebViewEventImpl webViewEvent) {
        mWebViewEvent = webViewEvent;
    }

    private void PrintDebugInfo(String error) {
        Log.d(LOG_TAG, error);
        if (mWebViewEvent.isDebug) {
            mWebViewEvent.evaluateJavascript(String.format("alert('%s')", "DEBUG ERR MSG:\\n" + error.replaceAll("\\'", "\\\\'")));
        }
    }

    @Keep
    @JavascriptInterface
    public String call(String methodName, String argStr) {
        String error = "Js bridge  called, but can't find a corresponded " +
                "JavascriptInterface object , please check your code!";
        String[] nameStr = mWebViewEvent.parseNamespace(methodName.trim());
        methodName = nameStr[1];
        Object jsb = mWebViewEvent.javaScriptNamespaceInterfaces.get(nameStr[0]);
        JSONObject ret = new JSONObject();
        try {
            ret.put("code", -1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (jsb == null) {
            PrintDebugInfo(error);
            return ret.toString();
        }
        Object arg = null;
        Method method = null;
        String callback = null;

        try {
            JSONObject args = new JSONObject(argStr);
            if (args.has("_dscbstub")) {
                callback = args.getString("_dscbstub");
            }
            if (args.has("data")) {
                arg = args.get("data");
            }
        } catch (JSONException e) {
            error = String.format("The argument of \"%s\" must be a JSON object string!", methodName);
            PrintDebugInfo(error);
            e.printStackTrace();
            return ret.toString();
        }


        Class<?> cls = jsb.getClass();
        boolean asyn = false;
        try {
            method = cls.getMethod(methodName,
                    Object.class, CompletionHandler.class);
            asyn = true;
        } catch (Exception e) {
            try {
                method = cls.getMethod(methodName, Object.class);
            } catch (Exception ex) {
                e.printStackTrace();
            }
        }

        if (method == null) {
            error = "Not find method \"" + methodName + "\" implementation! please check if the  signature or namespace of the method is right ";
            PrintDebugInfo(error);
            return ret.toString();
        }


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            JavascriptInterface annotation = method.getAnnotation(JavascriptInterface.class);
            if (annotation == null) {
                error = "Method " + methodName + " is not invoked, since  " +
                        "it is not declared with JavascriptInterface annotation! ";
                PrintDebugInfo(error);
                return ret.toString();
            }
        }

        Object retData;
        method.setAccessible(true);
        try {
            if (asyn) {
                final String cb = callback;
                method.invoke(jsb, arg, new CompletionHandler() {

                    @Override
                    public void complete(Object retValue) {
                        complete(retValue, true);
                    }

                    @Override
                    public void complete() {
                        complete(null, true);
                    }

                    @Override
                    public void setProgressData(Object value) {
                        complete(value, false);
                    }

                    private void complete(Object retValue, boolean complete) {
                        try {
                            JSONObject ret = new JSONObject();
                            ret.put("code", 0);
                            ret.put("data", retValue);
                            //retValue = URLEncoder.encode(ret.toString(), "UTF-8").replaceAll("\\+", "%20");
                            if (cb != null) {
                                //String script = String.format("%s(JSON.parse(decodeURIComponent(\"%s\")).data);", cb, retValue);
                                String script = String.format("%s(%s.data);", cb, ret.toString());
                                if (complete) {
                                    script += "delete window." + cb;
                                }
                                //Log.d(LOG_TAG, "complete " + script);
                                mWebViewEvent.evaluateJavascript(script);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                retData = method.invoke(jsb, arg);
                ret.put("code", 0);
                ret.put("data", retData);
                return ret.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            error = String.format("Call failed：The parameter of \"%s\" in Java is invalid.", methodName);
            PrintDebugInfo(error);
            return ret.toString();
        }
        return ret.toString();
    }

}