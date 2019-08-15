package wendu.dsbridge;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.webkit.ValueCallback;


public interface FileChooser {
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void openFileChooser(ValueCallback valueCallback, String acceptType);

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    void openFileChooser(ValueCallback<Uri> valueCallback,
                         String acceptType, String capture);
}