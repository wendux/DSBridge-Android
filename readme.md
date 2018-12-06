
# DSBridge for Android

![dsBridge](https://github.com/wendux/DSBridge-IOS/raw/master/img/dsbridge.png)

[![](https://jitpack.io/v/wendux/DSBridge-Android.svg)](https://jitpack.io/#wendux/DSBridge-Android)
![language](https://img.shields.io/badge/language-Java-yellow.svg)
[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit-license.php)
[![](https://travis-ci.org/wendux/DSBridge-Android.svg?branch=master)](https://travis-ci.org/wendux/DSBridge-Android)
[![GitHub last commit](https://img.shields.io/github/last-commit/wendux/DSBridge-Android.svg?color=blue)](https://github.com/wendux/DSBridge-Android/tree/master)
![](https://img.shields.io/badge/minSdkVersion-11-yellow.svg)
[![x5](https://img.shields.io/badge/support%20x5-yes-blue.svg)](https://github.com/wendux/DSBridge-Android/tree/x5-3.0)

>Modern cross-platform JavaScript bridge, through which you can invoke each other's functions synchronously or asynchronously between JavaScript and native applications.

Chinese documentation [中文文档](https://github.com/wendux/DSBridge-Android/blob/master/readme-chs.md)       
DSBridge-IOS：https://github.com/wendux/DSBridge-IOS       
[Tencent x5 webcore support](https://github.com/wendux/DSBridge-Android/tree/x5-3.0)

### Notice

DSBridge v3.0 is a milestone version. Compared with v2.0, we have made a lot of changes. Note that v3.0 is **incompatible** with v2.0, but v2.0 will continue to maintain. If you are a new user, use >=v3.0.

[DSBridge v3.0.0 change list](https://github.com/wendux/DSBridge-Android/issues/31)  

## Features

1. The three ends of Android, IOS and Javascript are easy to use, light and powerful, secure and strong
2. Both synchronous and asynchronous calls are supported
3. Support **API Object**, which centrally implements  APIs in a Java Class or a Javascript object 
4. Support API namespace
5. Support debug mode
6. Support the test of whether API exists
7. Support **Progress Callback**: one call, multiple returns
8. Support event listener for Javascript to close the page
9. Support Modal and Modeless popup box for javascript
10. Support the X5 webcore of Tencent

## Installation

1. Add the JitPack repository to your build file

   ```groovy
   allprojects {
     repositories {
      ...
      maven { url 'https://jitpack.io' }
     }
   }
   ```

2. Add the dependency

   ```groovy
   dependencies {
    //compile 'com.github.wendux:DSBridge-Android:3.0-SNAPSHOT'
    //support the x5 browser core of Tencent
    //compile 'com.github.wendux:DSBridge-Android:x5-3.0-SNAPSHOT'
   }
   ```

## Examples

See the `wendu.jsbdemo/` package. run the `app` project and to see it in action.

To use  dsBridge in your own project:

## Usage

1. Implement APIs in a Java class 

   ```java
   public class JsApi{
       //for synchronous invocation
       @JavascriptInterface
       public String testSyn(Object msg)  {
           return msg + "［syn call］";
       }

       //for asynchronous invocation
       @JavascriptInterface
       public void testAsyn(Object msg, CompletionHandler handler) {
           handler.complete(msg+" [ asyn call]");
       }
   }
   ```

   For security reason, Java APIs must be with "@JavascriptInterface" annotation .

2. Add API object to DWebView .

   ```javascript
   import wendu.dsbridge.DWebView
   ...
   DWebView dwebView= (DWebView) findViewById(R.id.dwebview);
   dwebView.addJavascriptObject(new JsApi(), null);
   ```

3. Call Native (Java/Object-c/swift) API in Javascript, and register javascript API.

   - Init dsBridge

     ```javascript
     //cdn
     //<script src="https://unpkg.com/dsbridge@3.1.3/dist/dsbridge.js"> </script>
     //npm
     //npm install dsbridge@3.1.3
     var dsBridge=require("dsbridge")
     ```

   - Call Native API and register a javascript API for Native invocation.

     ```javascript

     //Call synchronously 
     var str=dsBridge.call("testSyn","testSyn");

     //Call asynchronously
     dsBridge.call("testAsyn","testAsyn", function (v) {
       alert(v);
     })

     //Register javascript API for Native
      dsBridge.register('addValue',function(l,r){
          return l+r;
      })
     ```

4. Call Javascript API in java

   ```java
   dwebView.callHandler("addValue",new Object[]{3,4},new OnReturnValue<Integer>(){
        @Override
        public void onValue(Integer retValue) {
           Log.d("jsbridge","call succeed,return value is "+retValue);
        }
   });
   ```



## Java API signature

In order to be compatible with IOS , we make the following convention  on Java API signature:

1. For synchronous API.

   **` public any handler(Object msg) `**

   The argument type must be Object and must be declared even if not need)，and the type of return value  is not limited.

2. For asynchronous API.

   **`public void handler(Object arg, CompletionHandler handler)`**

## Namespace

Namespaces can help you better manage your APIs, which is very useful in   hybrid applications, because these applications have a large number of APIs. DSBridge (>= v3.0.0) allows you to classify API with namespace. And the namespace can be multilevel, between different levels with '.' division.

## Debug mode

In debug mode, some errors will be prompted by a popup dialog , and the exception caused by the native APIs will not be captured to expose problems. We recommend that the debug mode be opened at the development stage.  You can open debug mode :

```java
DWebView.setWebContentsDebuggingEnabled(true)
```



## Progress Callback

Normally, when a API is called to end, it returns a result, which corresponds one by one. But sometimes a call need to repeatedly return multipule times,  Suppose that on the Native side, there is  a API to download the file, in the process of downloading, it will send the progress information to  Javascript  many times, then Javascript will  display  the progress information on the H5 page. Oh...You will find it is difficult to achieve this function. Fortunately, DSBridge supports **Progress Callback**. You can be very simple and convenient to implement a call that needs to be returned many times. Here's an example of a countdown：

In Java 

```java
@JavascriptInterface
public void callProgress(Object args, final CompletionHandler<Integer> handler) {
    new CountDownTimer(11000, 1000) {
        int i=10;
        @Override
        public void onTick(long millisUntilFinished) {
            //setProgressData can be called many times util complete be called.
            handler.setProgressData((i--));
        }
        @Override
        public void onFinish() {
           //complete the js invocation with data; 
           //handler will be invalid when complete is called
            handler.complete(0);
        }
    }.start();
}
```

In Javascript

```javascript
dsBridge.call("callProgress", function (value) {
    document.getElementById("progress").innerText = value
})
```

For the complete sample code, please refer to the demo project.



## Javascript popup box

For Javascript popup box functions (alert/confirm/prompt), DSBridge has implemented them  all  by default, if you want to custom them, override the corresponding  callback in WebChromeClient . The default dialog box  implemented by DSBridge is modal. This will block the UI thread. If you need modeless, please refer to `dwebview.disableJavascriptDialogBlock (bool disable)`.



## Security

Before Android 4.2 (API 17), `webview.addJavascriptInterface` has security vulnerabilities, and DSBridge doesn't use it  under 4.2 of the devices. Meanwhile, in order to prevent Javascript from calling unauthorized native functions, all Java APIs must be annotated with "@JavascriptInterface" , so you can use DSBridge safely.

## DWebView

In DWebview, the following functions will execute in main thread automatically, you need not to switch thread by yourself.

```java
void loadUrl( String url) 
void loadUrl(final String url, Map<String, String> additionalHttpHeaders)
void evaluateJavascript(String script) 
```



## API Reference

### Java API

In Java, the object that implements the javascript interfaces is called **Java API object**.

##### `dwebview.addJavascriptObject(Object object, String namespace)`

Add the Java API object with supplied namespace into DWebView. The javascript can then call  Java APIs  with `bridge.call("namespace.api",...)`. 

If the namespace is empty, the  Java API object have no namespace. The javascript can  call  Java APIs with `bridge.call("api",...)`. 

Example:

In Java

```javascript
public class JsEchoApi {
    @JavascriptInterface
    public Object syn(Object args) throws JSONException {
        return  args;
    }

    @JavascriptInterface
    public void asyn(Object args,CompletionHandler handler){
        handler.complete(args);
    }
}
//namespace is "echo"
dwebView.addJavascriptObject(new JsEchoApi(),"echo");
```

In Javascript

```javascript
// call echo.syn
var ret=dsBridge.call("echo.syn",{msg:" I am echoSyn call", tag:1})
alert(JSON.stringify(ret))  
// call echo.asyn
dsBridge.call("echo.asyn",{msg:" I am echoAsyn call",tag:2},function (ret) {
      alert(JSON.stringify(ret));
})
```



##### `dwebview.removeJavascriptObject(String namespace)`

Remove the  Java API object with supplied namespace.



##### `dwebview.callHandler(String handlerName, Object[] args)`

##### `dwebview.callHandler(String handlerName, OnReturnValue handler)`

##### `dwebview.callHandler(String handlerName, Object[] args,OnReturnValue handler)`

Call the javascript API. If a `handler` is given, the javascript handler can respond. the `handlerName` can contain the namespace.  **The handler will be called in main thread**.

Example:

```java

dWebView.callHandler("append",new Object[]{"I","love","you"},new OnReturnValue<String>((){
    @Override
    public void onValue(String retValue) {
        Log.d("jsbridge","call succeed, append string is: "+retValue);
    }
});
// call with namespace 'syn', More details to see the Demo project                    
dWebView.callHandler("syn.getInfo", new OnReturnValue<JSONObject>() {
    @Override
    public void onValue(JSONObject retValue) {
      showToast(retValue);
    }
});
```



##### `dwebview.disableJavascriptDialogBlock(bool disable)`

BE CAREFUL to use. if you call any of the javascript popup box functions (`alert`,` confirm`, and `prompt`), the app will hang, and the javascript execution flow will be blocked. if you don't want to block the javascript execution flow, call this method, the  popup box functions will return  immediately(  `confirm` return `true`, and the `prompt` return empty string).

Example:

```javascript
dwebview.disableJavascriptDialogBlock(true);
```

if you want to  enable the block,  just calling this method with the argument value `false` .



##### `dwebview.setJavascriptCloseWindowListener(JavascriptCloseWindowListener listener)`

DWebView calls `listener.onClose` when Javascript calls `window.close`. the default handler is closing the current active activity.  you can provide a listener to add your hanlder .

Example:

```java
dwebview.setJavascriptCloseWindowListener(new DWebView.JavascriptCloseWindowListener() {
    @Override
    public boolean onClose() {
        Log.d("jsbridge","window.close is called in Javascript");
        //If return false,the default handler will be prevented. 
        return false;
    }
});
```



##### `dwebview.hasJavascriptMethod(String handlerName, OnReturnValue<Boolean> existCallback)`

Test whether the handler exist in javascript. 

Example:

```java
 dWebView.hasJavascriptMethod("addValue", new OnReturnValue<Boolean>() {
    @Override
    public void onValue(Boolean retValue) {
     showToast(retValue);
    }
 });
```



##### `DWebView.setWebContentsDebuggingEnabled(boolean enabled)`

Set debug mode. if in debug mode, some errors will be prompted by a popup dialog , and the exception caused by the native APIs will not be captured to expose problems. We recommend that the debug mode be opened at the development stage. 



### Javascript API

##### dsBridge 

"dsBridge" is accessed after dsBridge Initialization .



##### `dsBridge.call(method,[arg,callback])`

Call Java api synchronously and asynchronously。

`method`: Java API name， can contain the namespace。

`arg`: argument, Only one  allowed,  if you expect multiple  parameters,  you can pass them with a json object.

`callback(String returnValue)`: callback to handle the result. **only asynchronous invocation required**.



##### `dsBridge.register(methodName|namespace,function|synApiObject)`

##### `dsBridge.registerAsyn(methodName|namespace,function|asyApiObject)`

Register javascript synchronous and asynchronous  API for Native invocation. There are two types of invocation

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

2. Register a Javascript API object with supplied namespace. For example:

   **In Javascript**

   ```java
   //namespace test for synchronous
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
     }
   })
   ```

   > Because JavaScript does not support function overloading, it is not possible to define asynchronous function and sync function of the same name。
   >

   **In Java**

   ```java
   webView.callHandler("test.test1",new OnReturnValue<String>(){
       @Override
       public void onValue(String retValue) {
           Log.d("jsbridge","Namespace test.test1: "+retValue);
       }
   });

   webView.callHandler("test1.test1",new OnReturnValue<String>(){
       @Override
       public void onValue(String retValue) {
           Log.d("jsbridge","Namespace test.test1: "+retValue);
       }
   });
   ```




##### `dsBridge.hasNativeMethod(handlerName,[type])`

Test whether the handler exist in Java, the `handlerName` can contain the namespace. 

`type`: optional`["all"|"syn"|"asyn" ]`, default is "all".

```javascript
dsBridge.hasNativeMethod('testAsyn') 
//test namespace method
dsBridge.hasNativeMethod('test.testAsyn')
// test if exist a asynchronous function that named "testSyn"
dsBridge.hasNativeMethod('testSyn','asyn') //false
```



##### `dsBridge.disableJavascriptDialogBlock(disable)`

Calling `dsBridge.disableJavascriptDialogBlock(...)` has the same effect as calling `dwebview.disableJavascriptDialogBlock(...)` in Java.

Example:

```javascript
//disable
dsBridge.disableJavascriptDialogBlock()
//enable
dsBridge.disableJavascriptDialogBlock(false)
```



## Work with fly.js

As we all know, In  browser, AJax request are restricted by same-origin policy, so the request cannot be initiated across the domain.  However,    [Fly.js](https://github.com/wendux/fly) supports forwarding the http request  to Native through any Javascript bridge, And fly.js has already provide the dsBridge adapter.Because the  Native side has no the same-origin policy restriction, fly.js can request any resource from any domain. 

Another typical scene is in the hybrid App, [Fly.js](https://github.com/wendux/fly)  will forward all requests to Native, then, the unified request management, cookie management, certificate verification, request filtering and so on are carried out on Native. 

For the complete sample code, please refer to the demo project.

## Finally

If you like DSBridge, please star to let more people know it , Thank you !
