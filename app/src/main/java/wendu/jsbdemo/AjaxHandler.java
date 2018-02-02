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
 * This class handles the Ajax requests forwarded by fly.js in DWebView
 * More about fly.js see https://github.com/wendux/fly
 */

public class AjaxHandler {
    public static void onAjaxRequest(final JSONObject requestData, final CompletionHandler handler){

        // Define response structure
        final Map<String, Object> responseData=new HashMap<>();
        responseData.put("statusCode",0);

        try {
            int timeout =requestData.getInt("timeout");
            // Create a okhttp instance and set timeout
            final OkHttpClient okHttpClient = new OkHttpClient
                    .Builder()
                    .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                    .build();

            // Determine whether you need to encode the response result.
            // And encode when responseType is stream.
            String contentType="";
            boolean encode=false;
            String responseType=requestData.optString("responseType",null);
            if(responseType!=null&&responseType.equals("stream")){
                encode=true;
            }

            Request.Builder rb= new Request.Builder();
            rb.url(requestData.getString("url"));
            JSONObject headers=requestData.getJSONObject("headers");

            // Set request headers
            Iterator iterator = headers.keys();
            while(iterator.hasNext()){
                String  key = (String) iterator.next();
                String value = headers.getString(key);
                String lKey=key.toLowerCase();
                if(lKey.equals("cookie")){
                    // Here you can use CookieJar to manage cookie in a unified way with you native code.
                    continue;
                }
                if(lKey.toLowerCase().equals("content-type")){
                    contentType=value;
                }
                rb.header(key,value);
            }

            // Create request body
            if(requestData.getString("method").equals("POST")){
                RequestBody requestBody=RequestBody
                        .create(MediaType.parse(contentType),requestData.getString("data"));
                rb.post(requestBody) ;
            }
            // Create and send HTTP requests
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
                    // If encoding is needed, the result is encoded by Base64 and returned
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
