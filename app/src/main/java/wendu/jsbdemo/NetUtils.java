package wendu.jsbdemo;

import java.net.CookieManager;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * Created by du on 2017/9/16.
 */

public class NetUtils {
    public static CookieManager cookieManager=new CookieManager();
    public static Map<String, Object> request(String method, String url, String param, JSONObject headers) throws Exception {
        URL uri = new URL(url);
        method = method.toUpperCase();
        HttpURLConnection urlCon = (HttpURLConnection) uri.openConnection();
        urlCon.setRequestMethod(method);
        urlCon.setConnectTimeout(10000);
        handleRequestHeaders(urlCon,headers);
        if (urlCon instanceof HttpsURLConnection) {
            addCertVerifier((HttpsURLConnection) urlCon);
        }
        if (method.equals("POST")) {
            urlCon.setDoOutput(true);
            urlCon.setDoInput(true);
            if (!param.trim().isEmpty()) {
                PrintWriter pw = new PrintWriter(urlCon.getOutputStream());
                pw.print(param);
                pw.flush();
                pw.close();
            }
        }
        Map<String, Object> response=new HashMap<>();
        response.put("responseText",inputStream2String(urlCon.getInputStream()));
        response.put("statusCode",urlCon.getResponseCode());
        Map<String, List<String>> responseHeaders= new HashMap<>(urlCon.getHeaderFields());
        responseHeaders.remove(null);
        responseHeaders=handleResponseHeaders(urlCon,responseHeaders);
        response.put("headers",responseHeaders);
        return response;

    }

    //对于https请求，进行证书校验
    private static void   addCertVerifier(HttpsURLConnection urlCon) throws Exception {
       // 在此做证书校验
       // urlCon.setSSLSocketFactory(getSSLSocketFactory());
        urlCon.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                //return "api.dtworkroom.com".equals(hostname);
               HostnameVerifier hv=HttpsURLConnection.getDefaultHostnameVerifier();
               return hv.verify("*.dtworkroom.com",session);
            }
        });
    }

    //预处理请求头
    private static  void handleRequestHeaders(HttpURLConnection connection,JSONObject headers) throws Exception {

        Iterator iterator = headers.keys();
        while(iterator.hasNext()){
            String  key = (String) iterator.next();
            String value = headers.getString(key);
            if(!key.toLowerCase().equals("cookie")){
                //请求cookie
                connection.setRequestProperty(key, value);
            }
        }
        List<HttpCookie> cookies= cookieManager.getCookieStore().get(connection.getURL().toURI());
        cookies.toString();
    }

    private static  Map<String, List<String>> handleResponseHeaders(HttpURLConnection connection, Map<String, List<String>> responseHeaders) throws Exception {
        //获取响应头中的cookies，端上统一管理cookie
        cookieManager.put(connection.getURL().toURI(),responseHeaders);
        responseHeaders.remove("set-cookie");
        return responseHeaders;
    }


    private static String inputStream2String(InputStream is) {
        String result = "";
        String line;
        InputStreamReader inputReader = new InputStreamReader(is);
        BufferedReader bufReader = new BufferedReader(inputReader);
        try {
            while ((line = bufReader.readLine()) != null)
                result += line + "\r\n";
            bufReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
