# DSBridge v2.0

>DSBridge is currently the best Javascript bridge  in the world , by which we can call functions synchronously and asynchronously between web and Native . Moreover, both android and ios  are supported  ! 

DSBridge-IOS:https://github.com/wendux/DSBridge-IOS

DSBridge-Android:https://github.com/wendux/DSBridge-Android

2.0更新列表：https://juejin.im/post/593fa055128fe1006aff700a


## Usage

1. Implement apis in Java

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

2. Setup api class to DWebView  instance.

   ```javascript
   import wendu.dsbridge.DWebView
   ...
   DWebView dwebView= (DWebView) findViewById(R.id.dwebview);
   dwebView.setJavascriptInterface(new JsApi());
   ```

3. Call Java api in Javascript, and declare a global  javascript function for the following java invocation.

   ```javascript

   //Call synchronously 
   var str=dsBridge.call("testSyn", {msg: "testSyn"});
   //Call asynchronously
   dsBridge.call("testAsyn", {msg: "testAsyn"}, function (v) {
     alert(v);
   })

   //Register javascrit function for Native invocation
    dsBridge.register('addValue',function(r,l){
        return r+l;
    })
   ```

4. Call Javascript function in java

   ```java
   webView.callHandler("addValue",new Object[]{1,"hello"},new OnReturnValue(){
                       @Override
                       public void onValue(String retValue) {
                           Log.d("jsbridge","call succeed,return value is "+retValue);
              }
   });
   ```

​    Notice: Be sure that calling javascript functions must at  "PageFinished". 



## Javascript API introduction

### **dsBridge** 

"dsBridge" is a built-in object , it has two method "call" and "register";

### bridge.call(method,[args,callback])

Call Java api synchronously and asynchronously。

method: Java method name

args: arguments with json object

callback(String returnValue):callback to handle the result. **only asynchronous invocation required**.

### dsBridge.register(methodName,function)

Register javascript method for Native invocation.

methodName: javascript function name

function: javascript method body.

## Notice

### Java api signature

In order to be compatible with IOS and Android, we make the following convention  on native api signature:

1. The tye of return value must be String; if not need, just return null.
2. The arguments  passed by   JSONObject, if the api doesn't need argument, you still need declare the jsonObject argument. 

### More about DWebview

In DWebview, the following functions will execute in main thread automatically, you need not to switch thread by yourself.

```java
void loadUrl( String url) 
void loadUrl(final String url, Map<String, String> additionalHttpHeaders)
void evaluateJavascript(String script) 
```



### Alert dialog

In order to prevent unnecessary obstruction, the alert dialog was implemented asynchronously , that is to say, if you call alert in javascript , it will be returned directly no matter whether the user has to deal with. becase the code flow is not subject to the user operation no matter whether user  click ok button  or close the alert dialog. if you don't need this feature, you can custom the alert dialog by override "onJsAlert" callback in WebChromeClient class.

### Finally

If you like DSBridge, please star to let more people know it , Thank you  😄.
