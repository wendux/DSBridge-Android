
# DSBridge

[![](https://jitpack.io/v/wendux/DSBridge-Android.svg)](https://jitpack.io/#wendux/DSBridge-Android) [![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit-license.php) 
>DSBridge is currently the best Javascript bridge  in the world , by which we can call functions synchronously and asynchronously between web and Native . Moreover, both android and ios  are supported  ! 

### Notice

**This  branch(master)  contains preview features,  you should use the latest stable version (now is 2.0 )**



## Download

1. Add the JitPack repository to your build file

   ```java
   allprojects {
     repositories {
      ...
      maven { url 'https://jitpack.io' }
     }
   }
   ```

2. Add the dependency

   ```java
   dependencies {
   	compile 'com.github.wendux:DSBridge-Android:2.0-SNAPSHOT'

   	// support  the x5 browser core of tencent
   	// compile 'com.github.wendux:DSBridge-Android:x5-SNAPSHOT'
   	//compile 'com.github.wendux:DSBridge-Android:master-SNAPSHOT'
   }
   ```

## Installation

1. Implement apis in Java

   ```java
   public class JsApi{
       //for synchronous invocation
       @JavascriptInterface
       String testSyn(JSONObject jsonObject) throws JSONException {
           // The return value type can only be  String
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
   dwebView.addJavascriptObject(new JsApi(), null);
   ```

3. Call Java api in Javascript, and declare a global  javascript function for the following java invocation.

   - Init dsBridge

     ```javascript
     //cdn
     //For master <script src="https://unpkg.com/dsbridge/dist/dsbridge.js"> </script>
     //For 2.0.0<script src="https://unpkg.com/dsbridge@2.0.0/dist/dsbridge.js"> </script>
     //npm
     //For master: npm install dsbridge
     //For 2.0.0: npm install dsbridge@2.0.0
     var dsBridge=require("dsbridge")
     ```

   - Call API

     ```javascript

     //Call synchronously 
     var str=dsBridge.call("testSyn", {msg: "testSyn"});

     //Call asynchronously
     dsBridge.call("testAsyn", {msg: "testAsyn"}, function (v) {
       alert(v);
     })
     //Register javascript function for Native invocation
      dsBridge.register('addValue',function(l,r){
          return l+r;
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

## API Reference

### Java API

In Java, the object that implements the javascript interfaces is called Javascript Object.

##### `addJavascriptObject(Object object, String namespace)`

Add the  Javascript Object with supplied namespace into DWebView. The javascript can then call  javascript interface  with `bridge.call("namespace.interface")`. 

If the namespace is empty, the Javascript Object have no namespace. The javascript can  call  javascript interface  with `bridge.call("interface")`. 

Example:

In Java

```javascript
public class JsApiTest{
   @JavascriptInterface
    public String test(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("msg") + "［syn call］";
    }
}

public class JsApiTest1{
   @JavascriptInterface
    public String test1(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("msg") + "［syn call］";
    }
}
dwebView.addJavascriptObject(new JsApiTest(), null);//without namespace
dwebView.addJavascriptObject(new JsApiTest1(),"test1");//namespace is "test1"
```

In Javascript

```javascript
//call java api without namespace
dsBridge.call("testSyn", {msg: "testSyn"})
//call java api with namespace "test1"
dsBridge.call("test1.testSyn", {msg: "testSyn"})
```



##### `removeJavascriptObject(String namespace)`

Remove the Javascript Object with supplied namespace.

##### `callHandler(String handlerName, Object[] args)`

##### `webview.callHandler(String handlerName, Object[] args,OnReturnValue handler)`

Call the javascript handler called `handlerName`. If a `handler` is given, the javascript handler can respond.

Example:

```java
webView.callHandler("addValue",new Object[]{1,6},new OnReturnValue(){
    @Override
    public void onValue(String retValue) {
        Log.d("jsbridge","call succeed,return value is: "+retValue);
    }
});
webView.callHandler("append",new Object[]{"I","love","you"},new OnReturnValue(){
    @Override
    public void onValue(String retValue) {
        Log.d("jsbridge","call succeed, append string is: "+retValue);
    }
});
```



##### `disableJavascriptAlertBoxSafetyTimeout(bool disable)`

UNSAFE. Speed up bridge message passing by disabling the setTimeout safety check. It is only safe to disable this safety check if you do not call any of the javascript popup box functions (alert, confirm, and prompt). If you call any of these functions from the bridged javascript code, the app will hang.

Example:

```javascript
webview.disableJavascriptAlertBoxSafetyTimeout(true);
```



##### setJavascriptCloseWindowListener(JavascriptCloseWindowListener listener)`

DWebView calls `listener.onclose` when JavaScript calls `window.close`. the default handler is closing the current active activity.  you can provide a listener to add your hanlder .

Example:

```javascript
webView.setJavascriptCloseWindowListener(new DWebView.JavascriptCloseWindowListener() {
    @Override
    public boolean onClose() {
        Log.d("jsbridge","window.close is called in Javascript");
        //If return false,the default handler will be prevented. 
        return false;
    }
});
```



##### `hasJavascriptMethod(String handlerName, MethodExistCallback existCallback)`

Test whether the handler exist in javascript. 

Example:

```javascript
webView.hasJavascriptMethod("addValue", new DWebView.MethodExistCallback() {
    @Override
    public void onResult(boolean exist) {
        Log.d("jsbridge", "method exist:" + exist);
    }
});
```



### Javascript API

##### dsBridge 

"dsBridge" is accessed after dsBridge Initialization .



##### `dsBridge.call(method,[args,callback])`

Call Java api synchronously and asynchronously。

`method`: Java method name

`args`: arguments with json object

`callback(String returnValue)`:callback to handle the result. **only asynchronous invocation required**.



##### `dsBridge.register(methodName|namespace,function|methodObject)`

#####`dsBridge.registerAsyn(methodName|namespace,function|methodObject)`

Register javascript synchronous and asynchronous  method for Native invocation. There are two types of invocation

1. Just register a method. For example:

   In Javascript

   ```javascript
   dsBridge.register('addValue',function(l,r){
        return l+r;
   })
   dsBridge.registerAsyn('append',function(arg1,arg2,arg3,responseCallback){
        responseCallback(arg1+" "+arg2+" "+arg3);
   })
   ```

   In Java

   ```java
   webView.callHandler("addValue",new Object[]{1,6},new OnReturnValue(){
     @Override
     public void onValue(String retValue) {
       Log.d("jsbridge","call succeed,return value is: "+retValue);
     }
   });

    webView.callHandler("append",new Object[]{"I","love","you"},new OnReturnValue(){
      @Override
      public void onValue(String retValue) {
        Log.d("jsbridge","call succeed, append string is: "+retValue);
      }
    });
   ```

   ​

2. Register a methodObject with supplied namespace. For example:

   In Javascript

   ```java
   // namespace test for synchronous
   dsBridge.register("test",{
    tag:"test",
    test1:function(){
   		return this.tag+"1"
    },
    test2:function(){
   	return this.tag+"2"
    }
   })
   //namespace test1 for asynchronous calls  
   dsBridge.registerAsyn("test1",{
    tag:"test1",
    test1:function(responseCallback){
   	return responseCallback(this.tag+"1")
    },
    test2:function(responseCallback){
   	return responseCallback(this.tag+"2")
    },
   })
   ```

   In Java

   ```javascript
   webView.callHandler("test.test1",null,new OnReturnValue(){
       @Override
       public void onValue(String retValue) {
           Log.d("jsbridge","Namespace test.test1: "+retValue);
       }
   });

   webView.callHandler("test1.test1",null,new OnReturnValue(){
       @Override
       public void onValue(String retValue) {
           Log.d("jsbridge","Namespace test.test1: "+retValue);
       }
   });
   ...
   ```



##### `hasNativeMethod(handlerName)`

Test whether the handler exist in Java. 

```javascript
dsBridge.hasNativeMethod('testAsyn')
//test namespace method
dsBridge.hasNativeMethod('test.testAsyn')
```



##### `dsBridge.disableJavascriptAlertBoxSafetyTimeout(disable)`

Calling `dsBridge.disableJavascriptAlertBoxSafetyTimeout(...)` has the same effect as calling `webview disableJavscriptAlertBoxSafetyTimeout(...)` in Java.

Example:

```javascript
//disable
dsBridge.disableJavascriptAlertBoxSafetyTimeout()
//enable
dsBridge.disableJavascriptAlertBoxSafetyTimeout(false)
```



## Notice

### Java api signature

In order to be compatible with IOS and Android, we make the following convention  on native api signature:

1. The tye of return value must be **String;** if not need, just return null.
2. The arguments  passed by   JSONObject, if the api doesn't need argument, you still need declare the jsonObject argument. 

### More about DWebview

In DWebview, the following functions will execute in main thread automatically, you need not to switch thread by yourself.

```java
void loadUrl( String url) 
void loadUrl(final String url, Map<String, String> additionalHttpHeaders)
void evaluateJavascript(String script) 
```

###  alert/confirm/prompt dialog
For alert/confirm/prompt dialog, DSBridge has implemented them  all by default, if you want to custom them, override the corresponding  callback in WebChromeClient class.
### Finally

If you like DSBridge, please star to let more people know it , Thank you  😄.
