package wendu.dsbridge;

import android.content.Context;
import android.util.AttributeSet;

import com.tencent.smtt.sdk.WebView;


/**
 * Created by du on 16/12/29.
 */

public class DWebView extends WebView implements WebViewEvent.View {


    public DWebView(Context context) {
        super(context);
    }

    public DWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public DWebView getWebView() {
        return this;
    }

}
