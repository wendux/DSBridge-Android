package wendu.dsbridge;

import android.annotation.TargetApi;
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
            public String call(String methodName, String args) {
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
                    String callback = "";
                    try {
                        callback = arg.getString("_dscbstub");
                        arg.remove("_dscbstub");
                        method = cls.getDeclaredMethod(methodName,
                                new Class[]{JSONObject.class, CompletionHandler.class});
                        asyn = true;
                    } catch (Exception e) {
                        method = cls.getDeclaredMethod(methodName, new Class[]{JSONObject.class});
                    }

                    if (method == null) {
                        error = "ERROR! \n Not find method \"" + methodName + "\" implementation! ";
                        Log.e("SynWebView", error);
                        evaluateJavascript(String.format("alert(decodeURIComponent(\"%s\"})", error));
                        return "";
                    }
                    JavascriptInterface annotation = method.getAnnotation(JavascriptInterface.class);
                    if (annotation != null) {
                        Object ret;
                        method.setAccessible(true);
                        if (asyn) {
                            final String cb = callback;
                            ret = method.invoke(jsb, arg, new CompletionHandler() {

                                @Override
                                public void complete(String retValue) {
                                   complete(retValue,true);
                                }

                                @Override
                                public void complete() {
                                    complete("",true);
                                }

                                @Override
                                public void setProgressData(String value) {
                                  complete(value,false);
                                }

                                private void complete(String retValue,boolean complete) {
                                    try {
                                        if (retValue == null) retValue = "";
                                        retValue = URLEncoder.encode(retValue, "UTF-8").replaceAll("\\+", "%20");
                                        String script = String.format("%s(decodeURIComponent(\"%s\"));", cb, retValue);
                                        if(complete) {
                                            script += "delete window."+cb;
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
                    evaluateJavascript(String.format("alert('ERROR! \\n调用失败：函数名或参数错误 ［%s］')", e.getMessage()));
                    e.printStackTrace();
                }
                return "";
            }
            int i=0;

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
            injectJs();
            if (webChromeClient != null) {
                webChromeClient.onProgressChanged(view, newProgress);
            } else {
                super.onProgressChanged(view, newProgress);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            injectJs();
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
        }    @Override
        public boolean onJsAlert(WebView view, String url, final String message, final JsResult result) {
            if (webChromeClient != null) {
                if (webChromeClient.onJsAlert(view, url, message, result)) {
                    return true;
                }
            }
            Dialog alertDialog = new AlertDialog.Builder(getContext()).
                    setTitle("提示").
                    setMessage(message).
                    setPositiveButton("确定", new DialogInterface.OnClickListener() {
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
                new AlertDialog.Builder(getContext()).setTitle("提示")
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton("确定", listener)
                        .setNegativeButton("取消", listener).show();
                return true;

            }

        }

        @Override
        public boolean onJsPrompt(WebView view, String url, final String message,
                                  String defaultValue, final JsPromptResult result) {
            super.onJsPrompt(view,url,message,defaultValue,result);
            if (webChromeClient != null && webChromeClient.onJsPrompt(view, url, message, defaultValue, result)) {
                return true;
            } else {
                final EditText editText = new EditText(getContext());
                editText.setText(defaultValue);
                if (defaultValue!=null){
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
                        .setPositiveButton("确定", listener)
                        .setNegativeButton("取消", listener)
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

        private void injectJs() {
            evaluateJavascript("function getJsBridge(){window._dsf=window._dsf||{};return{call:function(b,a,c){\"function\"==typeof a&&(c=a,a={});if(\"function\"==typeof c){window.dscb=window.dscb||0;var d=\"dscb\"+window.dscb++;window[d]=c;a._dscbstub=d}a=JSON.stringify(a||{});return window._dswk?prompt(window._dswk+b,a):\"function\"==typeof _dsbridge?_dsbridge(b,a):_dsbridge.call(b,a)},register:function(b,a){\"object\"==typeof b?Object.assign(window._dsf,b):window._dsf[b]=a}}}dsBridge=getJsBridge();");
        }
    };

    private void _evaluateJavascript(String script) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJavascript(script, null);
        } else {
            loadUrl("javascript:" + script);
        }
    }

    //如果当前在主线程，不要直接调用post,这可能会延迟js执行
    public void evaluateJavascript(final String script) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            _evaluateJavascript(script);
        } else {
            post(new Runnable() {
                @Override
                public void run() {
                    _evaluateJavascript(script);
                }
            });
        }
    }

    @Override
    public void clearCache(boolean includeDiskFiles) {
        super.clearCache(includeDiskFiles);
        CookieManager.getInstance().removeAllCookie();
        Context context = getContext();
        //清理Webview缓存数据库
        try {
            context.deleteDatabase("webview.db");
            context.deleteDatabase("webviewCache.db");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //WebView 缓存文件
        File appCacheDir = new File(APP_CACAHE_DIRNAME);
        File webviewCacheDir = new File(context.getCacheDir()
                .getAbsolutePath() + "/webviewCache");

        //删除webview 缓存目录
        if (webviewCacheDir.exists()) {
            deleteFile(webviewCacheDir);
        }
        //删除webview 缓存 缓存目录
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
    public void loadUrl(final String url) {
        post(new Runnable() {
            @Override
            public void run() {
                DWebView.super.loadUrl(url);
            }
        });
    }

    public void callHandler(String method, Object[] args) {
      callHandler(method,args,null);
    }

    public void callHandler(String method, Object[] args, final OnReturnValue handler) {
        if (args == null) args = new Object[0];
        String arg = new JSONArray(Arrays.asList(args)).toString();
        String script = String.format("(window._dsf.%s||window.%s).apply(window._dsf||window,%s)", method,method, arg);
        if(handler!=null){
            script = String.format("%s.returnValue(%d,%s)",BRIDGE_NAME,callID, script);
            handlerMap.put(callID++, handler);
        }
        evaluateJavascript(script);

    }

    @Override
    public void loadUrl(final String url, final Map<String, String> additionalHttpHeaders) {
        post(new Runnable() {
            @Override
            public void run() {
                DWebView.super.loadUrl(url, additionalHttpHeaders);
            }
        });
    }

    public void setJavascriptInterface(Object object) {
        jsb = object;
    }
}
