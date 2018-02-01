import bridge from "./index"
bridge.call("test",function (retValue) {
  alert(retValue);  
})
var t=bridge.call("xx");

bridge.register("addVlue",function (l,r) {
    return l+r;
})

bridge.register("echo",{
    a:5
})

bridge.registerAsyn("echo",{
    b:6
})

bridge.hasNativeMethod("test","asyn")

bridge.disableJavascriptDialogBlock(true)