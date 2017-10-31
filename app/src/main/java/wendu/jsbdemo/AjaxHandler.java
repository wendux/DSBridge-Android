package wendu.jsbdemo;
import android.util.Base64;
import org.json.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import wendu.dsbridge.CompletionHandler;

/**
 * Created by du on 2017/10/31.
 *
 * This class handles the Ajax requests forwarded by fly.js in WebView
 *
 * More about fly.js see https://github.com/wendux/fly
 */

public class AjaxHandler {
    public static void onAjaxRequest(final JSONObject requestData, final CompletionHandler handler){
        final Map<String, Object> responseData=new HashMap<>();
        responseData.put("statusCode",0);

        try {
            //timeout值为0时代表不设置超时
            int timeout =requestData.getInt("timeout");
            //创建okhttp实例并设置超时
            final OkHttpClient okHttpClient = new OkHttpClient
                    .Builder()
                    .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                    .build();

            //判断是否需要将返回结果编码，responseType为stream时应编码
            String contentType="";
            boolean encode=false;
            String responseType=requestData.optString("responseType",null);
            if(responseType!=null&&responseType.equals("stream")){
                encode=true;
            }

            Request.Builder rb= new Request.Builder();
            rb.url(requestData.getString("url"));
            JSONObject headers=requestData.getJSONObject("headers");

            //设置请求头
            Iterator iterator = headers.keys();
            while(iterator.hasNext()){
                String  key = (String) iterator.next();
                String value = headers.getString(key);
                String lKey=key.toLowerCase();
                if(lKey.equals("cookie")){
                    //使用CookieJar统一管理cookie
                    continue;
                }
                if(lKey.toLowerCase().equals("content-type")){
                    contentType=value;
                }
                rb.header(key,value);
            }

            //创建请求体
            if(requestData.getString("method").equals("POST")){
                RequestBody requestBody=RequestBody
                        .create(MediaType.parse(contentType),requestData.getString("data"));
                rb.post(requestBody) ;
            }
            //创建并发送http请求
            Call call=okHttpClient.newCall(rb.build());
            final boolean finalEncode = encode;
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    responseData.put("responseText",e.getMessage());
                    handler.complete(new JSONObject(responseData).toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String data;
                    //如果需要编码，则对结果进行base64编码后返回
                    if(finalEncode){
                        data= Base64.encodeToString(response.body().bytes(),Base64.DEFAULT);
                    }else{
                        data=response.body().string();
                    }
                    responseData.put("responseText",data);
                    responseData.put("statusCode",response.code());
                    responseData.put("statusMessage",response.message());
                    Map<String, List<String>> responseHeaders= response.headers().toMultimap();
                    responseHeaders.remove(null);
                    responseData.put("headers",responseHeaders);
                    handler.complete(new JSONObject(responseData).toString());
                }
            });

        }catch (Exception e){
            responseData.put("responseText",e.getMessage());
            handler.complete(new JSONObject(responseData).toString());
        }
    }
}
