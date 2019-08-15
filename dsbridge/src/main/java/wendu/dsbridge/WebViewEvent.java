package wendu.dsbridge;

import com.tencent.smtt.sdk.WebChromeClient;

import java.util.Map;

/**
 * <pre>
 *     author : sunqiao
 *     e-mail : sunqiao@kayak.com.cn
 *     time   : 2019/08/15
 *     desc   : webview行为外部代理,抽取
 *     version: 1.0
 * </pre>
 */
public interface WebViewEvent {

    void init();

    void setWebChromeClient(WebChromeClient client);

    void clearCache(boolean includeDiskFiles);

    void reload();

    void loadUrl(final String url);

    void loadUrl(final String url, final Map<String, String> additionalHttpHeaders);


    void addJavascriptObject(Object object, String nameSpace);


    void onDestroyed();

    public interface View {
        DWebView getWebView();
    }
}
