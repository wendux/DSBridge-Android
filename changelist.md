# DSBridge v3.0 change list

DSBridge v3.0 is a milestone, Compared with v2.0.X, we have made a lot of changes. Note that V3.0 is **incompatible** with V2.0, but v2.0 will continue to maintain. If you are a new user, use >=v3.0

#### In Java

1. **Deprecated**：~~setJavascriptInterface~~ , use `addJavascriptObject` instead.

2. **Deprecated**：~~setJavascriptContextInitedListener~~ ,`callHandler` can be called at any time.

3. **Deprecated**：~~DUIWebView~~ , `UIWebView` will not be supported ever.

4. **New**: `addJavascriptObject:(id) object namespace:(NSString *) namespace`  

5. **New**: `removeJavascriptObject:NSString * namespace`

6. **New**: `disableJavascriptDialogBlock:(bool) disable`

7. **New**: `hasJavascriptMethod:(NSString *) handlerName methodExistCallback:(void(^ )(bool exist))callback`

8. **New**: ` setJavascriptCloseWindowListener:(void(^)(void))callback`

9. **New**: `setDebugMode:(bool) debug`

10. **New feature**: Support  namespace

11. **New feature**: Can add multiple  API object

12. **Changed**: Object-c API signature changed

13. **Changed**: `callHandler` can be called at any time.

    ​


#### In Javascript

1. **New**: `hasNativeMethod(handlerName,[type])`
2. **New**: `disableJavascriptDialogBlock(disable)`
3. **New**: `registerAsyn(methodName|namespace,function|asyApiObject)`
4. **Changed**: `register(methodName|namespace,function|synApiObject)`
5. **New feature**: Support  namespace

# Why Only Support WKWebView?

### Advantages of WKWebView

It is well known that **WKWebView loads web pages faster and more efficiently than UIWebView**, and also **doesn't have as much memory overhead** for you.

Under the current timeline, most iOS apps only support iOS 9.0+.

### UIWebView Cross-Domain Access Vulnerability

The reason for the iOS platform cross-domain access vulnerability is due to UIWebView turning on the WebKitAllowUniversalAccessFromFileURLs and WebKitAllowFileAccessFromFileURLs options.

**WKWebView default allowFileAccessFromFileURLs and allowUniversalAccessFromFileURLs option is false.**