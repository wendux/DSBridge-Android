# DSBridge

[![](https://jitpack.io/v/wendux/DSBridge-Android.svg)](https://jitpack.io/#wendux/DSBridge-Android)   [![MIT Licence](https://img.shields.io/packagist/l/doctrine/orm.svg)](https://opensource.org/licenses/mit-license.php)

DSBridge 是H5页面与Native之间通信的桥梁，它有如下特点：

1. 跨平台；同时支持ios和android。
2. 双向调用；js可以调用native， native可以调用js
3. 不仅支持异步调用，而且页**支持同步调用**（dsbridge是唯一一个支持同步调用的javascript bridge）
4. 支持进度回调，多次返回（常用于文件下载进度、计时器等）
5. Android支持腾讯x5内核
6. 三端易用；无论是前端还是android或ios，使用都非常简单，极大的降低集成／学习成本

与WebViewJavascriptBridge的对比请移步 [DSBridge VS WebViewJavascriptBridge]( http://www.jianshu.com/p/d967b0d85b97)

## 安装

1. 添加 JitPack repository 

   ```javascript
   allprojects {
     repositories {
      ...
      maven { url 'https://jitpack.io' }
     }
   }
   ```

2. 添加依赖

   ```javascript
   dependencies {
   	
   	 compile 'com.github.wendux:DSBridge-Android:2.0-SNAPSHOT'

   	//  使用腾讯x5内核的使用该版本
   	// compile 'com.github.wendux:DSBridge-Android:x5-SNAPSHOT'
       
       // 主线版本
   	//compile 'com.github.wendux:DSBridge-Android:master-SNAPSHOT'
   }
   ```

## 使用

假设Native端实现了两个api: testSyn、testAsyn。参数以json传递， testSyn为同步api,执行结束后会直接返回结果，而testAsyn为一个异步api(可能会执行耗时操作)，执行结束后，结果异步返回。

### Android

1. Java中实现 API 

   ```java
   public class JsApi{
       //用于同步调用
       @JavascriptInterface
       String testSyn(JSONObject jsonObject) throws JSONException {
           // The return value type can only be  String
           return jsonObject.getString("msg") + "［syn call］";
       }
       //用于异步调用
       @JavascriptInterface
       void testAsyn(JSONObject jsonObject, CompletionHandler handler) throws JSONException {
           handler.complete(jsonObject.getString("msg")+" [asyn call]");
       }
   }
   ```
   为了安全起见，所有的API都必须有 “JavascriptInterface” 标注。

2. 将实现的API安装到 `DWebView` 

   ```java
   import wendu.dsbridge.DWebView
   ...
   DWebView dwebView= (DWebView) findViewById(R.id.dwebview);
   dwebView.setJavascriptInterface(new JsApi());
   ```

3. 在h5页面中调用 Java API

   - 初始化 DSBridge

     ```javascript
     window._dsbridge&&_dsbridge.init();
     ```

   - 调用 API

     ```javascript
     // 同步调用
     var str=dsBridge.call("testSyn", {msg: "testSyn"});

     // 异步调用
     dsBridge.call("testAsyn", {msg: "testAsyn"}, function (v) {
       alert(v);
     })
     //Register javascrit function for Native invocation
      dsBridge.register('addValue',function(l,r){
          return l+r;
      })
     ```

4. Native 调用 h5 中的 javascript API

   - Javascript 注册供 Native调用的 API

     ```java
     // 注册一个加法函数供 Native 调用
      dsBridge.register('addValue',function(l,r){
          return l+r;
      })
     ```

   - 在 Java 中调用 javascript API

     ```java
     webView.callHandler("addValue",new Object[]{1,"hello"},new OnReturnValue(){
            @Override
            public void onValue(String retValue) {
               Log.d("jsbridge","call succeed,return value is "+retValue);
            }
     });
     ```

     > 注意：Native调用javascript API时必须在 "PageFinished"之后进行


### IOS

 IOS中的使用方式请参考 [DSBridge-IOS](https://github.com/wendux/DSBridge-IOS) 。

## Javascript API 

### **`dsBridge`**

"dsBridge" 是一个全局对象, **在h5页面中初始化DSBridge后**便会可用，它有两个方法 "call" 和 "register";

### `bridge.call(method,[args,callback])`

功能：调用Native api

method: api函数名

args:参数，类型：json, 可选参数

callback(String returnValue):仅调用异步api时需要.

**同步调用**

如果你是一名经验丰富的开发者，想必看到第二行时已然眼睛一亮，想想node最被诟病的是什么，目前跨平台的jsbridge中没有一个能支持同步，所有需要获取值的调用都必须传一个回调，如果调用逻辑比较复杂，必将会出现“callback hell”。然而，DSBridge彻底改变了这一点。**支持同步是DSBridge的最大亮点之一**。

**异步调用**

对于一些比较耗时的api, DSBridge提供了异步支持，正如上例第三行代码所示，此时你需要传一个回调（如果没有参数，回调可作为第二个参数），当api完成时回调将会被调用，结果以字符串的形式传递。

### `dsBridge.register(methodName,function)`

注册 javascript API 供Native调用

## 注意

为了兼容 Android和IOS ，DSBridge对Native API的签名有两个要求：

1. 返回值必须是` String`， 如果没有返回值，直接返回`null`就行

2. API的参数通过 `JSONObject`传递，如果有些API没有参数，**你也需要申明**。

   ​
## 多次返回

通常情况下，调用一个方法结束后会返回一个结果，是一一对应的，现在，我们来思考如下场景：

有一个嵌入到app中显示文档下载列表的网页。要求点击网页中相应文件对应的下载按钮后，开始下载文件，并在该网页中显示下载进度。

**思考**：我们将文件下载的功能在natvie端实现，当点击网页上的某项时，我们通过js调用native的下载方法，native在下载的过程中，不断的向js返回进度, 然后js更新网页上的进度条，等到下载任务结束时，才算本次调用结束。而**这种调用的特征就是js的一次调用，对应native的“多次返回”**，考虑到native很多耗时任务都可能会多次返回（比如返回进度），DSBridge 对“多次返回”进行了支持，使用DSBridge 就可以非常方便的应对这种case了。

详细的示例请参考 [DSBridge实例－在网页中展示Native进度](https://juejin.im/post/5940eafbfe88c2006a483fb2)



## 调用Javascript

DWebView提供了三个api用于调用js

```java
void callHandler(String method, Object[] args) 
void callHandler(String method, Object[] args, CompletionHandler handler)
void evaluateJavascript(String script)
```

前两个api中，method 为函数名，args为参数数组，可以接受String 、int 、long、float、double等。

第一个api用于调用没有返回值的js函数，没有参数时传null即可。

第二个api用于需要返回值的场景，需要传递一个CompletionHandler接口对象，在complete(String returnValue)方法中处理返回值即可。

第三个api用于执行任意js代码，内部已做版本兼容处理。

**调用时机**

DWebview只有在javascript context初始化成功后才能正确执行js代码，而javascript context初始化完成的时机一般都比整个页面加载完毕要早，随然DSBridge能捕获到javascript context初始化完成的时机，但是一些js api可能声明在页面尾部，甚至单独的js文件中（**请务必不要这么做**），如果在javascript context刚初始化完成就调用js api, 此时js api 可能还没有注册，所以会失败，综上所述，如果是客户端主动调用 js应该在onPageFinished后调用。简单的示例如下：

```java
webView.setWebViewClient(new WebViewClient(){
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        //期望返回值
        webView.callHandler("test",new Object[]{1,"hello"},new CompletionHandler(){
            @Override
            public void complete(String retValue) {
                Log.d("jsbridge","call succeed,return value is "+retValue);
            }
        });
        //不期望返回值
        webView.callHandler("test",null);
    }
});
```



## DWebview更多

DWebview中下列函数会在主线程中执行，您不必在手动切换线程

```java
void loadUrl( String url) 
void loadUrl(final String url, Map<String, String> additionalHttpHeaders)
void evaluateJavascript(String script) 
```

DWebview已经实现 alert、prompt、comfirm对话框，您可以不做处理，也可以自定义。值得一提的是js 在调用alert函数正常情况下只要用户没有关闭alert对话框，js代码是会阻塞的，但是考虑到alert 对话框只有一个确定按钮，也就是说无论用户关闭还是确定都不会影响js代码流程，所以DWebview中在弹出alert对话框时会先给js返回，这样一来js就可以继续执行，而提示框等用户关闭时在关闭即可。如果你就是想要阻塞的alert，可以自定义。而DWebview的prompt、comfirm实现完全符合ecma标准，都是阻塞的。

## 最后

如果你喜欢，欢迎star！