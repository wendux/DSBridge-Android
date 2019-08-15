package wendu.dsbridge;

import android.util.Log;

import java.io.File;


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
