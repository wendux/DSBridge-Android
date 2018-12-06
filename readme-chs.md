# DSBridge for Android

![dsBridge](https://github.com/wendux/DSBridge-IOS/raw/master/img/dsbridge.png)

[![](https://jitpack.io/v/wendux/DSBridge-Android.svg)](https://jitpack.io/#wendux/DSBridge-Android)
![language](https://img.shields.io/badge/language-Java-yellow.svg)
[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit-license.php)
[![](https://travis-ci.org/wendux/DSBridge-Android.svg?branch=master)](https://travis-ci.org/wendux/DSBridge-Android)
[![GitHub last commit](https://img.shields.io/github/last-commit/wendux/DSBridge-Android.svg?color=blue)](https://github.com/wendux/DSBridge-Android/tree/master)
![](https://img.shields.io/badge/minSdkVersion-11-yellow.svg)
[![x5](https://img.shields.io/badge/support%20x5-yes-blue.svg)](https://github.com/wendux/DSBridge-Android/tree/x5-3.0)

> 三端易用的现代跨平台 Javascript bridge， 通过它，你可以在Javascript和原生之间同步或异步的调用彼此的函数.


### 注意

DSBridge v3.0 是一个里程碑版本，和v2.0相比，有许多变化，需要注意的是v3.0**不兼容**之前版本，但是我们也会继续维护v2.0分支，所以，如果你是v2.0的使用者，请放心继续使用v2.0，如果你是新用户，请使用>=v3.0.

[DSBridge v3.0.0 更新列表](https://github.com/wendux/DSBridge-Android/issues/31)  
腾讯X5内核支持：https://github.com/wendux/DSBridge-Android/tree/x5-3.0


## 特性

1. Android、IOS、Javascript 三端易用，轻量且强大、安全且健壮。

2. 同时支持同步调用和异步调用

3. 支持以类的方式集中统一管理API

4. 支持API命名空间

5. 支持调试模式

6. 支持API存在性检测

7. 支持进度回调：一次调用，多次返回

8. 支持Javascript关闭页面事件回调

9. 支持Javascript 模态/非模态对话框

10. 支持腾讯X5内核

  ​

## 安装

1. 添加 JitPack repository 到gradle脚本中

   ```groovy
   allprojects {
     repositories {
      ...
      maven { url 'https://jitpack.io' }
     }
   }
   ```

2. 添加依赖

   ```groovy
   dependencies {
   	//compile 'com.github.wendux:DSBridge-Android:3.0-SNAPSHOT'
   	//support the x5 browser core of tencent
   	//compile 'com.github.wendux:DSBridge-Android:x5-3.0-SNAPSHOT'
   }
   ```

## 示例

请参考工程目录下的 `wendu.jsbdemo/` 包。运行 `app` 工程并查看示例交互。

如果要在你自己的项目中使用 dsBridge :

## 使用

1.  新建一个Java类，实现API

   ```java
   public class JsApi{
       //同步API
       @JavascriptInterface
       public String testSyn(Object msg)  {
           return msg + "［syn call］";
       }

       //异步API
       @JavascriptInterface
       public void testAsyn(Object msg, CompletionHandler<String> handler) {
           handler.complete(msg+" [ asyn call]");
       }
   }
   ```

   可以看到，DSBridge正式通过类的方式集中、统一地管理API。由于安全原因，所有Java API 必须有"@JavascriptInterface" 标注。

2. 添加API类实例到 DWebView .

   ```javascript
   import wendu.dsbridge.DWebView
   ...
   DWebView dwebView= (DWebView) findViewById(R.id.dwebview);
   dwebView.addJavascriptObject(new JsApi(), null);
   ```

3. 在Javascript中调用原生 (Java/Object-c/swift) API ,并注册一个 javascript API供原生调用.

   - 初始化 dsBridge

     ```javascript
     //cdn方式引入初始化代码(中国地区慢，建议下载到本地工程)
     //<script src="https://unpkg.com/dsbridge@3.1.3/dist/dsbridge.js"> </script>
     //npm方式安装初始化代码
     //npm install dsbridge@3.1.3
     var dsBridge=require("dsbridge")
     ```

   - 调用原生API ,并注册一个 javascript API供原生调用.

     ```javascript

     //同步调用
     var str=dsBridge.call("testSyn","testSyn");

     //异步调用
     dsBridge.call("testAsyn","testAsyn", function (v) {
       alert(v);
     })

     //注册 javascript API 
      dsBridge.register('addValue',function(l,r){
          return l+r;
      })
     ```

4. 在Java中调用 Javascript API 

   ```java
   dwebView.callHandler("addValue",new Object[]{3,4},new OnReturnValue<Integer>(){
        @Override
        public void onValue(Integer retValue) {
           Log.d("jsbridge","call succeed,return value is "+retValue);
        }
   });
   ```




## Java API 签名

为了兼容IOS，我们约定 Java API 签名，**注意，如果API签名不合法，则不会被调用**！签名如下：

1. 同步API.

   **` public any handler(Object msg) `**

   参数必须是 `Object` 类型，**并且必须申明**（如果不需要参数，申明后不适用即可）。返回值类型没有限制，可以是任意类型。

2. 异步 API.

   **`public void handler(Object arg, CompletionHandler handler)`**



## 命名空间

命名空间可以帮助你更好的管理API，这在API数量多的时候非常实用，比如在混合应用中。DSBridge (>= v3.0.0) 支持你通过命名空间将API分类管理，并且命名空间支持多级的，不同级之间只需用'.' 分隔即可。



## 调试模式

在调试模式时，发生一些错误时，将会以弹窗形式提示，并且原生API如果触发异常将不会被自动捕获，因为在调试阶段应该将问题暴露出来。如果调试模式关闭，错误将不会弹窗，并且会自动捕获API触发的异常，防止crash。强烈建议在开发阶段开启调试模式，可以通过如下代码开启调试模式

```java
DWebView.setWebContentsDebuggingEnabled(true)
```



## 进度回调

通常情况下，调用一个方法结束后会返回一个结果，是一一对应的。但是有时会遇到一次调用需要多次返回的场景，比如在javascript钟调用端上的一个下载文件功能，端上在下载过程中会多次通知javascript进度, 然后javascript将进度信息展示在h5页面上，这是一个典型的一次调用，多次返回的场景，如果使用其它Javascript bridge,  你将会发现要实现这个功能会比较麻烦，而DSBridge本省支持进度回调，你可以非常简单方便的实现一次调用需要多次返回的场景，下面我们实现一个倒计时的例子：

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
           //handler will invalid when complete is called
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

完整的示例代码请参考demo工程。



## Javascript 弹出框

DSBridge已经实现了 Javascript的弹出框函数(alert/confirm/prompt)，如果你想自定义它们，通过`WebChromeClient`重写相关函数即可。DSBridge实现的对话框默认设置是模态的，这会挂起UI线程，如果你需要非模态对话框，请参考`dwebview.disableJavascriptDialogBlock(bool disable)` 。



## 安全

在Android 4.2(API17)之前 `webview.addJavascriptInterface` 存在安全漏洞，DSBridge内部在4.2以下的设备上不会使用` webview.addJavascriptInterface`，而是通过其它方式通信，在4.2之后会使用 `webview.addJavascriptInterface` 。同时，为了防止Javascript调用未授权的原生函数，所有Java API 必须有"@JavascriptInterface" 标注，所以在任何版本的Android系统下，您可以放心使用DSBridge！



## DWebView

DWebView中，如果在非主线程调用下列方法时，它们内部会自动分发到主线程中执行，你再也无需手动切换。

```java
void loadUrl( String url) 
void loadUrl( String url, Map<String, String> additionalHttpHeaders)
void evaluateJavascript(String script) 
```



## API 列表

### Java API

在Java中我们把实现了供 javascript调用的 API类的实例 成为 **Java API object**.

##### `dwebview.addJavascriptObject(Object object, String namespace)`

添加一个Java API object到DWebView ，并为它指定一个命名空间。然后，在 javascript 中就可以通过`bridge.call("namespace.api",...)`来调用Java API object中的原生API了。

如果命名空间是空(null或空字符串）, 那么这个添加的  Java API object就没有命名空间。在 javascript 通过 `bridge.call("api",...)`调用。

**示例**:

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

通过命名空间名称移除相应的Java API object 。



##### `dwebview.callHandler(String handlerName, Object[] args)`

##### `dwebview.callHandler(String handlerName, OnReturnValue handler)`

##### `dwebview.callHandler(String handlerName, Object[] args,OnReturnValue handler)`

调用 javascript API。`handlerName`  为javascript API 的名称，可以包含命名空间；参数以数组传递，`args`数组中的元素依次对应javascript API的形参； `handler` 用于接收javascript API的返回值，**注意：handler将在主线程中被执行**。

示例:

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

**小心使用**. 如果你在javascript中调用弹窗函数(`alert`,` confirm`, 或 `prompt`)， 那么APP将会挂起，因为这些弹窗都是**模态**的，会阻塞APP主线程，此时javascript执行流也会阻塞。如果你想避免阻塞，可以通过此API禁止，禁止后，一旦 javascript中调用了这些弹窗函数，APP将弹出**非模态**对话框，并立即返回，(  `confirm` 会返回 `true`,  `prompt` 返回空字符串)。

禁止Javascript对话框阻塞:

```javascript
dwebview.disableJavascriptDialogBlock(true);
```

如果你想恢复**模态**对话框，传 `false` 调用即可.



##### `dwebview.setJavascriptCloseWindowListener(JavascriptCloseWindowListener listener)`

当 Javascript中调用`window.close`时，DWebView会触发此监听器，如果未设置，DWebView默认会关闭掉当前Activity. 

Example:

```java
dwebview.setJavascriptCloseWindowListener(new DWebView.JavascriptCloseWindowListener() {
    @Override
    public boolean onClose() {
        Log.d("jsbridge","window.close is called in Javascript");
        //如果返回false,则阻止DWebView默认处理. 
        return false;
    }
});
```



##### `dwebview.hasJavascriptMethod(String handlerName, OnReturnValue<Boolean> existCallback)`

检测是否存在指定的 javascript API，`handlerName`可以包含命名空间. 

示例:

```java
 dWebView.hasJavascriptMethod("addValue", new OnReturnValue<Boolean>() {
    @Override
    public void onValue(Boolean retValue) {
     showToast(retValue);
    }
 });
```



##### `DWebView.setWebContentsDebuggingEnabled(boolean enabled)`

设置调试模式。在调试模式时，发生一些错误时，将会以弹窗形式提示，并且原生API如果触发异常将不会被自动捕获，因为在调试阶段应该将问题暴露出来。如果调试模式关闭，错误将不会弹窗，并且会自动捕获API触发的异常，防止crash。强烈建议在开发阶段开启调试模式。



### Javascript API

##### dsBridge 

"dsBridge" 在初始化之后可用 .

##### `dsBridge.call(method,[arg,callback])`

同步或异步的调用Java API。

`method`: Java API 名称， 可以包含命名空间。

`arg`:传递给Java API 的参数。只能传一个，如果需要多个参数时，可以合并成一个json对象参数。

`callback(String returnValue)`: 处理Java API的返回结果. 可选参数，**只有异步调用时才需要提供**.



##### `dsBridge.register(methodName|namespace,function|synApiObject)`

##### `dsBridge.registerAsyn(methodName|namespace,function|asynApiObject)`

注册同步/异步的Javascript API. 这两个方法都有两种调用形式：

1. 注册一个普通的方法，如:

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
   webView.callHandler("addValue",new Object[]{1,6},new OnReturnValue<String>(){
     @Override
     public void onValue(String retValue) {
       Log.d("jsbridge","call succeed,return value is: "+retValue);
     }
   });

   webView.callHandler("append",new Object[]{"I","love","you"},new OnReturnValue<String>(){
      @Override
      public void onValue(String retValue) {
        Log.d("jsbridge","call succeed, append string is: "+retValue);
      }
   });
   ```

   ​

2. 注册一个对象，指定一个命名空间:

   **In Javascript**

   ```javascript
   //namespace test for synchronous calls
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

   > 因为Javascript并不支持函数重载，所以不能在同一个Javascript对象中定义同名的同步函数和异步函数

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

检测Java中是否存在名为`handlerName`的API, `handlerName` 可以包含命名空间. 

`type`: 可选参数，`["all"|"syn"|"asyn" ]`, 默认是 "all".

```javascript
//检测是否存在一个名为'testAsyn'的API(无论同步还是异步)
dsBridge.hasNativeMethod('testAsyn') 
//检测test命名空间下是否存在一个’testAsyn’的API
dsBridge.hasNativeMethod('test.testAsyn')
// 检测是否存在一个名为"testSyn"的异步API
dsBridge.hasNativeMethod('testSyn','asyn') //false
```



##### `dsBridge.disableJavascriptDialogBlock(disable)`

调用 `dsBridge.disableJavascriptDialogBlock(...)` 和在Java中调用 `dwebview.disableJavascriptDialogBlock(...)` 作用一样.

示例:

```javascript
//disable
dsBridge.disableJavascriptDialogBlock()
//enable
dsBridge.disableJavascriptDialogBlock(false)
```



## 和 fly.js一起使用

当dsBridge遇见  [Fly.js](https://github.com/wendux/fly)  时，将会打开一个新的世界。[fly.js传送门](https://github.com/wendux/fly)

正如我们所知，在浏览器中，ajax请求受同源策略限制，不能跨域请求资源。然而，  [Fly.js](https://github.com/wendux/fly) 有一个强大的功能就是支持请求重定向：将ajax请求通过任何Javascript bridge重定向到端上，并且 [Fly.js](https://github.com/wendux/fly) 官方已经提供的 dsBridge 的 adapter, 可以非常方便的协同dsBridge一起使用。由于端上没有同源策略的限制，所以 fly.js可以请求任何域的资源。

另一个典型的使用场景是在混合APP中，由于[Fly.js](https://github.com/wendux/fly) 可以将所有ajax请求转发到端上，所以，开发者就可以在端上进行统一的请求管理、证书校验、cookie管理、访问控制等。

具体的示例请查看demo.

## 最后

如果你喜欢DSBridge, 欢迎star，以便更多的人知道它, 谢谢 !
