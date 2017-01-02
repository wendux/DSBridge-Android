# DSBridge

> DSBridge是目前地球上最好的IOS/Android   javascript bridge.

DSBridge-IOS:https://github.com/wendux/DSBridge-IOS
DSBridge-Android:https://github.com/wendux/DSBridge-Android

 **五分钟了解DSBridge**

## Web端

假设Native端实现了两个api: testSyn、testAsyn。参数以json传递， testSyn为同步api,执行结束后会直接返回结果，而testAsyn为一个异步api(可能会执行耗时操作)，执行结束后，结果异步返回。下面我们看看web端如何调用。

### Javascript调用Native

```javascript
var bridge = getJsBridge();
var str=bridge.call("testSyn", {msg: "testSyn"});
bridge.call("testAsyn", {msg: "testAsyn"}, function (v) {
  alert(v);
})
```

简单到不用解释！太优雅了。如果你体会不来，你也许应该去看看当今（马上将会成为历史）人气最高的[WebViewJavascriptBridge](https://github.com/marcuswestin/WebViewJavascriptBridge) ，相信你看完之后会回来的。虽说简单，但为了让你了然于胸，还是给出官方解释：

### **getJsBridge** 

功能：获取javascript bridge对象。

等等，貌似和我之前使用的其他库不一样，难道不需要像WebViewJavascriptBridge那样先声明一个setupWebViewJavascriptBridge的回调？你有这种疑问很正常，先给出答案：**不需要，DSBridge不需要前端任何安装代码，随用随取**。DSBridge的设计原则就是：让三端使用方式都是最简单的！  DSBridge获取bridge时，不依赖任何回调，也无需等待页面加载结束。ps: 这在ios>=8,android>sdk19上测试都没问题，  DSBridge也对ios7.0-8.0,android sdk16-19之间的版本做了兼容，但是考虑到测试覆盖面的问题，建议所有代码都在dom ready之后执行。

### bridge.call(method,[args,callback])

功能：调用Native api

method: api函数名

args:参数，类型：json, 可选参数

callback(String returnValue):仅调用异步api时需要.

**同步调用**

如果你是一名经验丰富的开发者，想必看到第二行时已然眼睛一亮，想想node最被诟病的是什么，目前跨平台的jsbridge中没有一个能支持同步，所有需要获取值的调用都必须传一个回调，如果调用逻辑比较复杂，必将会出现“callback hell”。然而，DSBridge彻底改变了这一点。**支持同步是DSBridge的最大亮点之一**。

**异步调用**

对于一些比较耗时的api, DSBridge提供了异步支持，正如上例第三行代码所示，此时你需要传一个回调（如果没有参数，回调可作为第二个参数），当api完成时回调将会被调用，结果以字符串的形式传递。

### 供Native调用Javascript api

假设网页中要提供一个函数test供native调用，只要将函数声明为全局函数即可：

```javascript
function test(arg1,arg2){
  return arg1+arg2;
}
```

如果你的代码是在一个闭包中，将函数挂在window上即可：

```javascript
window.test=function(arg1,arg2){
  	return arg1+arg2;
}	
```

这样一来端上即可调用。

## Android端

### 实现Api

API的实现非常简单，只需要将您要暴漏给js的api放在一个类中，然后统一注册即可。

```java
public class JsApi{
    @JavascriptInterface
    String testSyn(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("msg") + "［syn call］";
    }
    @JavascriptInterface
    void testAsyn(JSONObject jsonObject, CompletionHandler handler) throws JSONException {
        handler.complete(jsonObject.getString("msg")+" [asyn call]");
    }
}
```

testSyn为同步api, js在调用同步api时会等待native返回，返回后js继续往下执行。

testAsyn为异步api, 异步操作时调用handler.complete通知js，此时js中设置的回调将会被调用。

**为了安全起见，所有可供js调用的api必须添加@JavascriptInterface标注**

### 注册Api

```java
DWebView webView= (DWebView) findViewById(R.id.webview);
webView.setJavascriptInterface(new JsApi());
webView.loadUrl("xx");
```

请使用sdk中的DWebView，它在实现了js bridge的同时，还提供了一些其它的api.

第二句即为注册代码。

### 调用Javascript

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

DWebview只有在javascript context初始化成功后才能正确执行js代码，而javascript context初始化完成的时机一般都比整个页面加载完毕要早，随然DSBridge能捕获到javascript context初始化完成的时机，但是一些js api可能声明在页面尾部，甚至单独的js文件中（**请务必不要这么做**），如果在javascript context刚初始化完成就调用js api, 此时js api 可能还没有加载，所以会失败，综上所述，如果是客户端主动调用 js应该在onPageFinished后调用。简单的示例如下：

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



### DWebview更多

DWebview中下列函数会在主线程中执行，您不必在手动切换线程

```java
void loadUrl( String url) 
void loadUrl(final String url, Map<String, String> additionalHttpHeaders)
void evaluateJavascript(String script) 
```

DWebview已经实现 alert、prompt、comfirm对话框，您可以不做处理，也可以自定义。值得一提的是js 在调用alert函数正常情况下只要用户没有关闭alert对话框，js代码是会阻塞的，但是考虑到alert 对话框只有一个确定按钮，也就是说无论用户关闭还是确定都不会影响js代码流程，所以DWebview中在弹出alert对话框时会先给js返回，这样一来js就可以继续执行，而提示框等用户关闭时在关闭即可。如果你就是想要阻塞的alert，可以自定义。而DWebview的prompt、comfirm实现完全符合ecma标准，都是阻塞的。