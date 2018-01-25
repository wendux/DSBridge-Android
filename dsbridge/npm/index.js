window._dsf=window._dsf||{};
var bridge={
        call: function (method, args, cb) {
            var ret = '';
            if (typeof args == 'function') {
                cb = args;
                args = {};
            }
            if (typeof cb == 'function') {
                window.dscb = window.dscb || 0;
                var cbName = 'dscb' + window.dscb++;
                window[cbName] = cb;
                args['_dscbstub'] = cbName;
            }
            args = JSON.stringify(args || {})

            if (window._dswk) {
                ret = prompt(window._dswk + method, args);
            } else {
                if (typeof _dsbridge == 'function') {
                    ret = _dsbridge(method, args);

                } else {
                    ret = _dsbridge.call(method, args);
                }
            }
            return ret;
        },
        register:function(name,fun){
            if(typeof name=="object"){
                Object.assign(window._dsf,name)
            }else {
                window._dsf[name] = fun;
            }
        },
        hasNativeMethod:function(name){
          return this.call("_hasNativeMethod",{"name":name})=='1';
        },
        disableJavascriptAlertBoxSafetyTimeout: function (disable) {
             this.call("_disableJavascriptAlertBoxSafetyTimeout", {disable:disable !== false})
        },
  }

    bridge.register("_hasJavascriptMethod",function(name){
    return !!window._dsf[name]
    })

    window.close=function(){
        bridge.call("_closePage")
    }

module.exports=bridge;