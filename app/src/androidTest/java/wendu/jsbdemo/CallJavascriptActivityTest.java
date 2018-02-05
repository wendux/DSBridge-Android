package wendu.jsbdemo;

import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import wendu.dsbridge.DWebView;
import wendu.dsbridge.OnReturnValue;

/**
 * Created by du on 2018/2/5.
 */
@RunWith (AndroidJUnit4.class)
public class CallJavascriptActivityTest extends ActivityInstrumentationTestCase2<CallJavascriptActivity> {

    private DWebView dWebView;

    public CallJavascriptActivityTest() {
        super(CallJavascriptActivity.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Injecting the Instrumentation instance is required
        // for your test to run with AndroidJUnitRunner.
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        CallJavascriptActivity activity=getActivity();
        dWebView= (DWebView) getActivity().findViewById(R.id.webview);
        //dWebView.loadUrl("file:///android_asset/js-call-native.html");
        DWebView.setWebContentsDebuggingEnabled(true);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void onClick(View v) throws Exception {
                dWebView.callHandler("addValue", new Object[]{3, 4}, new OnReturnValue<Integer>() {
                    @Override
                    public void onValue(Integer retValue) {
                       assertEquals(7,retValue.intValue());
                    }
                });
               SystemClock.sleep(5000);
    }

}