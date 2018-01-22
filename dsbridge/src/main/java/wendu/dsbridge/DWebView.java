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
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by du on 16/12/29.
 */

public class DWebView extends WebView {
    private static final String BRIDGE_NAME = "_dsbridge";
    private Object jsb;
    private String APP_CACAHE_DIRNAME;
    int callID = 0;
    private static final int EXEC_SCRIPT=1;
    private static final int LOAD_URL=2;
    private static final int LOAD_URL_WITH_HEADERS=3;
    MyHandler mainThreadHandler=null;

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
                switch (msg.what){
                    case EXEC_SCRIPT:_evaluateJavascript((String)msg.obj); break;
                    case LOAD_URL: DWebView.super.loadUrl((String)msg.obj); break;
                    case LOAD_URL_WITH_HEADERS:{
                        RequestInfo info= (RequestInfo)msg.obj;
                        DWebView.super.loadUrl(info.url,info.headers);
                    } break;
                }
            }
        }
    }
    class RequestInfo {
        String url;
        Map<String, String> headers;
        RequestInfo(String url,Map<String, String> additionalHttpHeaders){
            this.url=url;
            this.headers=additionalHttpHeaders;
        }
    }

    Map<Integer, OnReturnValue> handlerMap = new HashMap<>();

    public DWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DWebView(Context context) {
        super(context);
        init();
    }

    @Keep
    void init() {
        mainThreadHandler=new MyHandler((Activity) getContext());
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
        super.addJavascriptInterface(new Object() {
            @Keep
            @JavascriptInterface
            public String callHandler(String methodName, String args) {
                String error = "Js bridge method called, but there is " +
                        "not a JavascriptInterface object, please set JavascriptInterface object first!";
                if (jsb == null) {
                    Log.e("SynWebView", error);
                    return "";
                }

                Class<?> cls = jsb.getClass();
                try {
                    Method method;
                    boolean asyn = false;
                    JSONObject arg = new JSONObject(args);
                    String callbackId = "";
                    Log.e("SpringDebug @1", args);
                    try {
                        callbackId = arg.getString("_callbackId");
                        arg.remove("_callbackId");
                        method = cls.getDeclaredMethod(methodName,
                                new Class[]{JSONObject.class, CompletionHandler.class});
                        asyn = true;
                    } catch (Exception e) {
                        method = cls.getDeclaredMethod(methodName, new Class[]{JSONObject.class});
                    }

                    Log.e("SpringDebug @2 %s", asyn ? "true" : "false");

                    if (method == null) {
                        error = "ERROR! \n Not find method \"" + methodName + "\" implementation @chun! ";
                        Log.e("SynWebView", error);
                        evaluateJavascript(String.format("alert(decodeURIComponent(\"%s\"})", error));
                        return "";
                    }
                    JavascriptInterface annotation = method.getAnnotation(JavascriptInterface.class);
                    if (annotation != null) {
                        Object ret;
                        method.setAccessible(true);
                        if (asyn) {
                            final String cid = callbackId;
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

                                // FIXME currently, `retValue` should be a json string.
                                // a better way is, make it a serializable object, and stringify it inside of `complete`, our developers should be glad to see it
                                private void complete(String retValue, boolean complete) {
                                    try {
                                        if (retValue == null) retValue = "";
                                        // FIXME special process for no return value, we could make it more JAVA
                                        if (retValue == "") {
                                            JSONObject result = new JSONObject();
                                            result.put("result", "");
                                            retValue = result.toString();
                                        }
                                        String script = String.format(
                                                "%s.invokeCallback && %s.invokeCallback(%s, %s, %s);",
                                                BRIDGE_NAME, BRIDGE_NAME, cid, retValue, Boolean.toString(complete)
                                        );
                                        Log.d("complete script", script);
                                        evaluateJavascript(script);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } else {
                            ret = method.invoke(jsb, arg);
                        }
                        if (ret == null) {
                            ret = "";
                        }
                        return ret.toString();
                    } else {
                        error = "Method " + methodName + " is not invoked, since  " +
                                "it is not declared with JavascriptInterface annotation! ";
                        evaluateJavascript(String.format("alert('ERROR \\n%s')", error));
                        Log.e("SynWebView", error);
                    }
                } catch (Exception e) {
                    evaluateJavascript(String.format("alert('ERROR! \\nCall failed：Function does not exist or parameter is invalid［%s］')", e.getMessage()));
                    e.printStackTrace();
                }
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

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        webChromeClient = client;
    }

    WebChromeClient webChromeClient;

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
                if (defaultValue != null){
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

    private void _evaluateJavascript(String script) {
        // FIXME @allen do we need to support legacy Android versions under KITKAT?
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
        String argsString = new JSONArray(Arrays.asList(args)).toString();
        String callIDString = "";
        if (handler != null) {
            callIDString = Integer.toString(callID);
            handlerMap.put(callID++, handler);
        }
        String script = String.format("(%s.invokeHandler && %s.invokeHandler(\"%s\", %s, %s))", BRIDGE_NAME, BRIDGE_NAME, method, argsString, callIDString);
        Log.d("callHandler script", script);
        evaluateJavascript(script);
    }

    public void setJavascriptInterface(Object object) {
        jsb = object;
    }
}
