
# DSBridge

[![](https://jitpack.io/v/wendux/DSBridge-Android.svg)](https://jitpack.io/#wendux/DSBridge-Android) [![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit-license.php) 
>The modern cross-platform JavaScript bridge, through which you can invoke each other's functions synchronously or asynchronously between JavaScript and native applications.

### Notice

**This  branch(master)  includes some preview features,  you should use the latest stable version (now is 2.0 )**

DSBridge v3.0.0 preview change list  	DSBridge v3.0.0 预览版更新列表



## Installation

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
       compile 'com.github.wendux:DSBridge-Android:master-SNAPSHOT'
       //compile 'com.github.wendux:DSBridge-Android:2.0-SNAPSHOT'
   	// support  the x5 browser core of tencent
   	// compile 'com.github.wendux:DSBridge-Android:x5-SNAPSHOT'
   }
   ```

## Usage

1. Implement apis in Java

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

   For security reason, Java api must be with "@JavascriptInterface" annotation .

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
     //<script src="https://unpkg.com/dsbridge@3.0.6/dist/dsbridge.js"> </script>
     //npm
     //npm install dsbridge@3.0.6
     var dsBridge=require("dsbridge")
     ```

   - Call Java API and register a javascript api for Java

     ```javascript

     //Call synchronously 
     var str=dsBridge.call("testSyn","testSyn");

     //Call asynchronously
     dsBridge.call("testAsyn","testAsyn", function (v) {
       alert(v);
     })

     //Register javascript function for Native invocation
      dsBridge.register('addValue',function(l,r){
          return l+r;
      })
     ```

4. Call Javascript function in java

   ```java
   dwebView.callHandler("addValue",new Object[]{3,4},new OnReturnValue<Integer>(){
          @Override
          public void onValue(Integer retValue) {
             Log.d("jsbridge","call succeed,return value is "+retValue);
          }
   });
   ```

## API Reference

### Java API

In Java, the object that implements the javascript interfaces is called Javascript Object.

##### `dwebview.addJavascriptObject(Object object, String namespace)`

Add the  Javascript Object with supplied namespace into DWebView. The javascript can then call  javascript interface  with `bridge.call("namespace.interface",...)`. 

If the namespace is empty, the Javascript Object have no namespace. The javascript can  call  javascript interface  with `bridge.call("interface",...)`. 

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

Remove the Javascript Object with supplied namespace.



##### `dwebview.callHandler(String handlerName, Object[] args)`

##### `dwebview.callHandler(String handlerName, OnReturnValue handler)`

##### `dwebview.callHandler(String handlerName, Object[] args,OnReturnValue handler)`

Call the javascript handler called `handlerName`. If a `handler` is given, the javascript handler can respond.

Example:

```java
dWebView.callHandler("addValue", new Object[]{3, 4}, new OnReturnValue<Integer>() {
  @Override
  public void onValue(Integer retValue) {
    Log.d("jsbridge","call succeed, addValue(3,4): "+retValue);
  }
});

dWebView.callHandler("append",new Object[]{"I","love","you"},new OnReturnValue<String>((){
    @Override
    public void onValue(String retValue) {
        Log.d("jsbridge","call succeed, append string is: "+retValue);
    }
});
```



##### `dwebview.disableJavascriptDialogBlock(bool disable)`

BE CAREFUL to use. if you call any of the javascript popup box functions (`alert`,` confirm`, and `prompt`), the app will hang, and the javascript execution flow will be blocked. if you don't want to block the javascript execution flow, call this method, the  popup box functions will return  immediately(  `confirm` return true, and the `prompt` return empty string).

Example:

```javascript
dwebview.disableJavascriptDialogBlock(true);
```

if you want to recover enabling the block,  just call this method with the argument value `false` .



##### dwebview.setJavascriptCloseWindowListener(JavascriptCloseWindowListener listener)`

DWebView calls `listener.onclose` when JavaScript calls `window.close`. the default handler is closing the current active activity.  you can provide a listener to add your hanlder .

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



##### `dwebview.hasJavascriptMethod(String handlerName, OnReturnValue<Boolean> existCallback`

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



### Javascript API

##### dsBridge 

"dsBridge" is accessed after dsBridge Initialization .



##### `dsBridge.call(method,[args,callback])`

Call Java api synchronously and asynchronously。

`method`: Java method name

`args`: arguments with json object

`callback(String returnValue)`:callback to handle the result. **only asynchronous invocation required**.



##### `dsBridge.register(methodName|namespace,function|synApiObject)`

##### `dsBridge.registerAsyn(methodName|namespace,function|asyApiObject)`

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
    },
   })
   ```

   In Java

   ```java
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



##### `dsBridge.hasNativeMethod(handlerName,[type])`

Test whether the handler exist in Java. 

`type`: optional ("all","syn","asyn"), default is "all"

```javascript
dsBridge.hasNativeMethod('testAsyn') 
//test namespace method
dsBridge.hasNativeMethod('test.testAsyn')
// test if exist a asynchronous function that named "testSyn"
dsBridge.hasNativeMethod('testSyn','asyn') //false
```



##### `dsBridge.disableJavascriptDialogBlock(disable)`

Calling `dsBridge.disableJavascriptDialogBlock(...)` has the same effect as calling `dwebview disableJavascriptDialogBlock(...)` in Java.

Example:

```javascript
//disable
dsBridge.disableJavascriptDialogBlock()
//enable
dsBridge.disableJavascriptDialogBlock(false)
```



## Notice

### Java API signature

In order to be compatible with IOS and Android, we make the following convention  on native API signature:

1. For synchronous API
    **` public any handler(Object msg) `**

   ​    The argument type must be Object, and the type of return value  is not limited.

2. For asynchronous API

    **`public void handler(Object arg, CompletionHandler handler)`**

### More about DWebView

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
