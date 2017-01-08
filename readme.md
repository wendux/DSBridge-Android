# DSBridge

>DSBridge is currently the best Javascript bridge  in the world , by which we can call functions synchronously and asynchronously between web and Native . Moreover, both android and ios  are supported  ! 

DSBridge-IOS:https://github.com/wendux/DSBridge-IOS

DSBridge-Android:https://github.com/wendux/DSBridge-Android

Compare with WebViewJavascriptBridge: [DSBridge VS WebViewJavascriptBridge]( http://www.jianshu.com/p/d967b0d85b97)

中文文档请移步：http://www.jianshu.com/p/f9c51b4a8135

 **Five minutes to know DSBridge**

## Usage

1. Implement api in Java

   ```java
   public class JsApi{
       //for synchronous invocation
       @JavascriptInterface
       String testSyn(JSONObject jsonObject) throws JSONException {
           return jsonObject.getString("msg") + "［syn call］";
       }
       //for asynchronous invocation
       @JavascriptInterface
       void testAsyn(JSONObject jsonObject, CompletionHandler handler) throws JSONException {
           handler.complete(jsonObject.getString("msg")+" [asyn call]");
       }
   }
   ```

   For security reason, Java api must be with "@JavascriptInterface" annotation, For more detail about this topic, please google .

2. Setup api class to DWebView which instance.

   ```javascript
   import wendu.dsbridge.DWebView
   ...
   DWebView dwebView= (DWebView) findViewById(R.id.dwebview);
   dwebView.setJavascriptInterface(new JsApi());
   ```

3. Call Java api in Javascript, and declare a global  javascript function for java invocation.

   ```javascript
   //Call Java API
   var bridge = getJsBridge();
   //Call synchronously 
   var str=bridge.call("testSyn", {msg: "testSyn"});
   //Call asynchronously
   bridge.call("testAsyn", {msg: "testAsyn"}, function (v) {
     alert(v);
   })

   //Test will be called by Java
   function test(arg1,arg2){
     return arg1+arg2;
   }
   ```

4. Call Javascript function in java

   ```java
   dwebView.callHandler("test",new Object[]{1,"hello"},new CompletionHandler(){
      @Override
      public void complete(String retValue) {
        Log.d("jsbridge","call succeed,return value is "+retValue);
      }
    });
   ```

​    Notice: Be sure that calling javascript functions must at  "PageFinished". 



## Javascript API introduction

### **getJsBridge** 

Get the bridge object。 Although you can call it  anywhere in the page, we also advise you to call it after dom ready.

### bridge.call(method,[args,callback])

Call Java api synchronously and asynchronously。

method: Java method name

args: arguments with json object

callback(String returnValue):callback to handle the result. **only asynchronous invocation required**.

## Notice

### Java api signature

In order to be compatible with IOS and Android, we make the following convention  on native api signature:

1. The tye of return value must be String; if not need, just return null.
2. The arguments  passed by   JSONObject, if the api doesn't need argument, you still need declare the jsonObject argument. 

### More about DWebview

In DWebview, the fellowing functions will execute in main thread automaticlly, you need not to swith thread by yourself.

```java
void loadUrl( String url) 
void loadUrl(final String url, Map<String, String> additionalHttpHeaders)
void evaluateJavascript(String script) 
```



### Alert dialog

In order to prevent unnecessary obstruction, the alert dialog was implemented asynchronously , that is to say, if you call alert in javascript , it will be returned directly no matter whether the user has to deal with. becase the code flow is not subject to the user operation no matter whether user  click ok button  or close the alert dialog. if you don't need this feature, you can custom the alert dialog by override "onJsAlert" callback in WebChromeClient class.

### Finally

If you like DSBridge, please star to let more people know it , Thank you  😄.