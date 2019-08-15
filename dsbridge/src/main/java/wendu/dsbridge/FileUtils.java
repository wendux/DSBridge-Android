package wendu.dsbridge;

import android.util.Log;

import java.io.File;

/**
 * <pre>
 *     author : sunqiao
 *     e-mail : sunqiao@kayak.com.cn
 *     time   : 2019/08/15
 *     desc   :
 *     version: 1.0
 * </pre>
 */
public class FileUtils {
    public static void deleteFile(File file) {
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
