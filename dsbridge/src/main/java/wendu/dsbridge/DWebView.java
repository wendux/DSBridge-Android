package wendu.dsbridge;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Keep;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by du on 16/12/29.
 */

public class DWebView extends WebView {
    private static final String BRIDGE_NAME = "_dsbridge";
    private static final String LOG_TAG = "dsBridge";
    private static boolean isDebug = false;
    private List javaScriptInterfaces = new ArrayList();
    private String APP_CACAHE_DIRNAME;
    int callID = 0;
    private static final int EXEC_SCRIPT = 1;
    private static final int LOAD_URL = 2;
    private static final int LOAD_URL_WITH_HEADERS = 3;
    WebChromeClient webChromeClient;
    MyHandler mainThreadHandler = null;

    class MyHandler extends Handler {
        //  Using WeakReference to avoid memory leak
        WeakReference<Activity> mActivityReference;

        MyHandler(Activity activity) {
            mActivityReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final Activity activity = mActivityReference.get();
            if (activity != null) {
                switch (msg.what) {
                    case EXEC_SCRIPT:
                        _evaluateJavascript((String) msg.obj);
                        break;
                    case LOAD_URL:
                        DWebView.super.loadUrl((String) msg.obj);
                        break;
                    case LOAD_URL_WITH_HEADERS: {
                        RequestInfo info = (RequestInfo) msg.obj;
                        DWebView.super.loadUrl(info.url, info.headers);
                    }
                    break;
                }
            }
        }
    }

    class RequestInfo {
        String url;
        Map<String, String> headers;

        RequestInfo(String url, Map<String, String> additionalHttpHeaders) {
            this.url = url;
            this.headers = additionalHttpHeaders;
        }
    }

    Map<Integer, OnReturnValue> handlerMap = new HashMap<>();

    public interface MethodExistCallback{
        void onResult(boolean exist);
    }

    public DWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DWebView(Context context) {
        super(context);
        init();
    }

    public static void setWebContentsDebuggingEnabled(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(enabled);
        }
        isDebug = enabled;
    }

    @Keep
    void init() {
        mainThreadHandler = new MyHandler((Activity) getContext());
        APP_CACAHE_DIRNAME = getContext().getFilesDir().getAbsolutePath() + "/webcache";
        WebSettings settings = getSettings();
        settings.setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
        }
        settings.setAllowFileAccess(false);
        settings.setAppCacheEnabled(false);
        settings.setSavePassword(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportMultipleWindows(true);
        settings.setAppCachePath(APP_CACAHE_DIRNAME);
        if (Build.VERSION.SDK_INT >= 21) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        settings.setUseWideViewPort(true);
        super.setWebChromeClient(mWebChromeClient);
        addInternalJavascriptObject();
        super.addJavascriptInterface(new Object() {

            private void PrintDebugInfo(String error) {
                Log.d(LOG_TAG, error);
                if (isDebug) {
                    evaluateJavascript(String.format("alert('ERROR \\n%s')", error));
                }
            }

            @Keep
            @JavascriptInterface
            public String call(String methodName, String args) {
                String error = "Js bridge  called, but there is " +
                        "not a JavascriptInterface object, please add JavascriptInterface object first!";
                if (javaScriptInterfaces.isEmpty()) {
                    PrintDebugInfo(error);
                    return "";
                }
                JSONObject arg;
                try {
                    arg = new JSONObject(args);
                } catch (JSONException e) {
                    error = String.format("The argument of \"%s\" must be a JSON object string!", methodName);
                    PrintDebugInfo(error);
                    e.printStackTrace();
                    return "";
                }
                Method method = null;
                for (Object jsb : javaScriptInterfaces) {
                    Class<?> cls = jsb.getClass();
                    boolean asyn = false;
                    String callback = "";
                    try {
                        callback = arg.getString("_dscbstub");
                        arg.remove("_dscbstub");
                        method = cls.getDeclaredMethod(methodName,
                                new Class[]{JSONObject.class, CompletionHandler.class});
                        asyn = true;
                    } catch (Exception e) {
                        try {
                            method = cls.getDeclaredMethod(methodName, new Class[]{JSONObject.class});
                        } catch (Exception ex) {
                            continue;
                        }
                    }

                    JavascriptInterface annotation = method.getAnnotation(JavascriptInterface.class);
                    if (annotation == null) {
                        continue;
                    }

                    Object ret = null;
                    method.setAccessible(true);
                    try {
                        if (asyn) {
                            final String cb = callback;
                            ret = method.invoke(jsb, arg, new CompletionHandler() {

                                @Override
                                public void complete(String retValue) {
                                    complete(retValue, true);
                                }

                                @Override
                                public void complete() {
                                    complete("", true);
                                }

                                @Override
                                public void setProgressData(String value) {
                                    complete(value, false);
                                }

                                private void complete(String retValue, boolean complete) {
                                    try {
                                        if (retValue == null) retValue = "";
                                        retValue = URLEncoder.encode(retValue, "UTF-8").replaceAll("\\+", "%20");
                                        String script = String.format("%s(decodeURIComponent(\"%s\"));", cb, retValue);
                                        if (complete) {
                                            script += "delete window." + cb;
                                        }
                                        evaluateJavascript(script);
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } else {
                            ret = method.invoke(jsb, arg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        error = String.format("Call failed：The parameter of \"%s\" in Java is invalid.", methodName);
                        PrintDebugInfo(error);
                    }
                    if (ret == null) {
                        ret = "";
                    }
                    return ret.toString();
                }

                if (method == null) {
                    error = "Not find method \"" + methodName + "\" implementation! ";
                } else {
                    error = "Method " + methodName + " is not invoked, since  " +
                            "it is not declared with JavascriptInterface annotation! ";
                }
                PrintDebugInfo(error);
                return "";
            }

            @Keep
            @JavascriptInterface
            public void returnValue(int id, String value) {
                OnReturnValue handler = handlerMap.get(id);
                if (handler != null) {
                    handler.onValue(value);
                    handlerMap.remove(id);
                }
            }
        }, BRIDGE_NAME);

    }

    @Keep
    private void addInternalJavascriptObject(){
        addJavascriptObject(new Object(){
            @Keep
            @JavascriptInterface
            public String hasNativeMethod(JSONObject jsonObject) throws JSONException {
                String methodName=jsonObject.getString("name");
                for (Object jsb : javaScriptInterfaces) {
                    Class<?> cls = jsb.getClass();
                    Method[] methods=cls.getDeclaredMethods();
                    for(Method m : methods){
                        Log.d(LOG_TAG,m.getName());
                        if(m.getName().equals(methodName)){
                            JavascriptInterface annotation = m.getAnnotation(JavascriptInterface.class);
                            if (annotation != null) {
                               return "1";
                            }
                        }
                    }
                }
                return "0";
            }
        });
    }

    private void _evaluateJavascript(String script) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            DWebView.super.evaluateJavascript(script, null);
        } else {
            loadUrl("javascript:" + script);
        }
    }

    public void evaluateJavascript(final String script) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            _evaluateJavascript(script);
        } else {
            Message msg = new Message();
            msg.what = EXEC_SCRIPT;
            msg.obj = script;
            mainThreadHandler.sendMessage(msg);
        }
    }

    @Override
    public void loadUrl(String url) {
        Message msg = new Message();
        msg.what = LOAD_URL;
        msg.obj = url;
        mainThreadHandler.sendMessage(msg);
    }

    @Override
    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        Message msg = new Message();
        msg.what = LOAD_URL_WITH_HEADERS;
        msg.obj = new RequestInfo(url, additionalHttpHeaders);
        mainThreadHandler.sendMessage(msg);
    }

    public void callHandler(String method, Object[] args) {
        callHandler(method, args, null);
    }

    public void callHandler(String method, Object[] args, final OnReturnValue handler) {
        if (args == null) args = new Object[0];
        String arg = new JSONArray(Arrays.asList(args)).toString();
        String script = String.format("(window._dsf.%s||window.%s).apply(window._dsf||window,%s)", method, method, arg);
        if (handler != null) {
            script = String.format("%s.returnValue(%d,%s)", BRIDGE_NAME, callID, script);
            handlerMap.put(callID++, handler);
        }
        evaluateJavascript(script);

    }

    public void hasJavascriptMethod(String handlerName, final MethodExistCallback existCallback){
        callHandler("hasJavascriptMethod", new Object[]{handlerName}, new OnReturnValue() {
            @Override
            public void onValue(String retValue) {
               existCallback.onResult(retValue.equals("true"));
            }
        });
    }

    /**
     * @param object JavaScript interface object
     * @deprecated Use {@link #addJavascriptObject(Object) addJavascriptObject(Object)}
     * instead.
     */
    @Deprecated
    public void setJavascriptInterface(Object object) {
        addJavascriptObject(object);
    }

    /**
     * Add a java object which implemented the javascript interfaces to dsBridge .
     * @param object JavaScript interface object
     */
    public void addJavascriptObject(Object object) {
        javaScriptInterfaces.add(object);
    }

    /**
     * remove a java object from dsBridge.
     * @param object JavaScript interface object
     */
    public void removeInterfaceObject(Object object) {
        javaScriptInterfaces.remove(object);
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        webChromeClient = client;
    }

    private WebChromeClient mWebChromeClient = new WebChromeClient() {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (webChromeClient != null) {
                webChromeClient.onProgressChanged(view, newProgress);
            } else {
                super.onProgressChanged(view, newProgress);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (webChromeClient != null) {
                webChromeClient.onReceivedTitle(view, title);
            } else {
                super.onReceivedTitle(view, title);
            }
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            if (webChromeClient != null) {
                webChromeClient.onReceivedIcon(view, icon);
            } else {
                super.onReceivedIcon(view, icon);
            }
        }

        @Override
        public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
            if (webChromeClient != null) {
                webChromeClient.onReceivedTouchIconUrl(view, url, precomposed);
            } else {
                super.onReceivedTouchIconUrl(view, url, precomposed);
            }
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (webChromeClient != null) {
                webChromeClient.onShowCustomView(view, callback);
            } else {
                super.onShowCustomView(view, callback);
            }
        }

        @Override
        public void onShowCustomView(View view, int requestedOrientation,
                                     CustomViewCallback callback) {
            if (webChromeClient != null) {
                webChromeClient.onShowCustomView(view, requestedOrientation, callback);
            } else {
                super.onShowCustomView(view, requestedOrientation, callback);
            }
        }

        @Override
        public void onHideCustomView() {
            if (webChromeClient != null) {
                webChromeClient.onHideCustomView();
            } else {
                super.onHideCustomView();
            }
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog,
                                      boolean isUserGesture, Message resultMsg) {
            if (webChromeClient != null) {
                return webChromeClient.onCreateWindow(view, isDialog,
                        isUserGesture, resultMsg);
            }
            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
        }

        @Override
        public void onRequestFocus(WebView view) {
            if (webChromeClient != null) {
                webChromeClient.onRequestFocus(view);
            } else {
                super.onRequestFocus(view);
            }
        }

        @Override
        public void onCloseWindow(WebView window) {
            if (webChromeClient != null) {
                webChromeClient.onCloseWindow(window);
            } else {
                super.onCloseWindow(window);
            }
        }

        @Override
        public boolean onJsAlert(WebView view, String url, final String message, final JsResult result) {
            if (webChromeClient != null) {
                if (webChromeClient.onJsAlert(view, url, message, result)) {
                    return true;
                }
            }
            Dialog alertDialog = new AlertDialog.Builder(getContext()).
                    setMessage(message).
                    setCancelable(false).
                    setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            result.confirm();
                        }
                    })
                    .create();
            alertDialog.show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message,
                                   final JsResult result) {
            if (webChromeClient != null && webChromeClient.onJsConfirm(view, url, message, result)) {
                return true;
            } else {
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == Dialog.BUTTON_POSITIVE) {
                            result.confirm();
                        } else {
                            result.cancel();
                        }
                    }
                };
                new AlertDialog.Builder(getContext())
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, listener)
                        .setNegativeButton(android.R.string.cancel, listener).show();
                return true;

            }

        }

        @Override
        public boolean onJsPrompt(WebView view, String url, final String message,
                                  String defaultValue, final JsPromptResult result) {
            super.onJsPrompt(view, url, message, defaultValue, result);
            if (webChromeClient != null && webChromeClient.onJsPrompt(view, url, message, defaultValue, result)) {
                return true;
            } else {
                final EditText editText = new EditText(getContext());
                editText.setText(defaultValue);
                if (defaultValue != null) {
                    editText.setSelection(defaultValue.length());
                }
                float dpi = getContext().getResources().getDisplayMetrics().density;
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == Dialog.BUTTON_POSITIVE) {
                            result.confirm(editText.getText().toString());
                        } else {
                            result.cancel();
                        }
                    }
                };
                new AlertDialog.Builder(getContext())
                        .setTitle(message)
                        .setView(editText)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, listener)
                        .setNegativeButton(android.R.string.cancel, listener)
                        .show();
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                int t = (int) (dpi * 16);
                layoutParams.setMargins(t, 0, t, 0);
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                editText.setLayoutParams(layoutParams);
                int padding = (int) (15 * dpi);
                editText.setPadding(padding - (int) (5 * dpi), padding, padding, padding);
                return true;
            }

        }

        @Override
        public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
            if (webChromeClient != null) {
                return webChromeClient.onJsBeforeUnload(view, url, message, result);
            }
            return super.onJsBeforeUnload(view, url, message, result);
        }

        @Override
        public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota,
                                            long estimatedDatabaseSize,
                                            long totalQuota,
                                            WebStorage.QuotaUpdater quotaUpdater) {
            if (webChromeClient != null) {
                webChromeClient.onExceededDatabaseQuota(url, databaseIdentifier, quota,
                        estimatedDatabaseSize, totalQuota, quotaUpdater);
            } else {
                super.onExceededDatabaseQuota(url, databaseIdentifier, quota,
                        estimatedDatabaseSize, totalQuota, quotaUpdater);
            }
        }

        @Override
        public void onReachedMaxAppCacheSize(long requiredStorage, long quota, WebStorage.QuotaUpdater quotaUpdater) {
            if (webChromeClient != null) {
                webChromeClient.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
            }
            super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            if (webChromeClient != null) {
                webChromeClient.onGeolocationPermissionsShowPrompt(origin, callback);
            } else {
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }
        }

        @Override
        public void onGeolocationPermissionsHidePrompt() {
            if (webChromeClient != null) {
                webChromeClient.onGeolocationPermissionsHidePrompt();
            } else {
                super.onGeolocationPermissionsHidePrompt();
            }
        }


        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onPermissionRequest(PermissionRequest request) {
            if (webChromeClient != null) {
                webChromeClient.onPermissionRequest(request);
            } else {
                super.onPermissionRequest(request);
            }
        }


        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onPermissionRequestCanceled(PermissionRequest request) {
            if (webChromeClient != null) {
                webChromeClient.onPermissionRequestCanceled(request);
            } else {
                super.onPermissionRequestCanceled(request);
            }
        }

        @Override
        public boolean onJsTimeout() {
            if (webChromeClient != null) {
                return webChromeClient.onJsTimeout();
            }
            return super.onJsTimeout();
        }

        @Override
        public void onConsoleMessage(String message, int lineNumber, String sourceID) {
            if (webChromeClient != null) {
                webChromeClient.onConsoleMessage(message, lineNumber, sourceID);
            } else {
                super.onConsoleMessage(message, lineNumber, sourceID);
            }
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (webChromeClient != null) {
                return webChromeClient.onConsoleMessage(consoleMessage);
            }
            return super.onConsoleMessage(consoleMessage);
        }

        @Override
        public Bitmap getDefaultVideoPoster() {

            if (webChromeClient != null) {
                return webChromeClient.getDefaultVideoPoster();
            }
            return super.getDefaultVideoPoster();
        }

        @Override
        public View getVideoLoadingProgressView() {
            if (webChromeClient != null) {
                return webChromeClient.getVideoLoadingProgressView();
            }
            return super.getVideoLoadingProgressView();
        }

        @Override
        public void getVisitedHistory(ValueCallback<String[]> callback) {
            if (webChromeClient != null) {
                webChromeClient.getVisitedHistory(callback);
            } else {
                super.getVisitedHistory(callback);
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            if (webChromeClient != null) {
                return webChromeClient.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }
            return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
        }
    };

    @Override
    public void clearCache(boolean includeDiskFiles) {
        super.clearCache(includeDiskFiles);
        CookieManager.getInstance().removeAllCookie();
        Context context = getContext();
        try {
            context.deleteDatabase("webview.db");
            context.deleteDatabase("webviewCache.db");
        } catch (Exception e) {
            e.printStackTrace();
        }

        File appCacheDir = new File(APP_CACAHE_DIRNAME);
        File webviewCacheDir = new File(context.getCacheDir()
                .getAbsolutePath() + "/webviewCache");

        if (webviewCacheDir.exists()) {
            deleteFile(webviewCacheDir);
        }

        if (appCacheDir.exists()) {
            deleteFile(appCacheDir);
        }
    }

    public void deleteFile(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File files[] = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFile(files[i]);
                }
            }
            file.delete();
        } else {
            Log.e("Webview", "delete file no exists " + file.getAbsolutePath());
        }
    }

}
