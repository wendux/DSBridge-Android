var bridge = {
    call: function (method, args, cb) {
        var ret = '';
        if (typeof args == 'function') {
            cb = args;
            args = {};
        }
        if (typeof cb == 'function') {
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
    register: function (name, fun, asyn) {
        var q = asyn ? window._dsaf : window._dsf
        if (!window._initCalled) {
            window._initCalled = true;
            setTimeout(function () {
                bridge.call("_init");
            }, 0)
        }
        if (typeof fun == "object") {
            q._obs[name] = fun;
        } else {
            q[name] = fun
        }
    },
    registerAsyn: function (name, fun) {
        this.register(name, fun, true);
    },
    hasNativeMethod: function (name) {
        return this.call("_hasNativeMethod", {
            "name": name
        }) == '1';
    },
    disableJavascriptAlertBoxSafetyTimeout: function (disable) {
        this.call("_disableJavascriptAlertBoxSafetyTimeout", {
            disable: disable !== false
        })
    }
};

! function () {
    if (window._dsf) return;
    var ob = {
        _dsf: {
            _obs: {}
        },
        _dsaf: {
            _obs: {}
        },
        dscb: 0,
        dsBridge: bridge,
        close: function () {
            bridge.call("_closePage")
        },
        _handleMessageFromJava: function (info) {
            var arg = JSON.parse(info.data);
            var ret = {
                id: info.callbackId,
                complete: true
            }
            var f = this._dsf[info.method];
            var af = this._dsaf[info.method]
            var callSyn = function (f, ob) {
                ret.data = f.apply(ob, arg) || ""
                bridge.call("_returnValue", ret)
            }
            var callAsyn = function (f, ob) {
                arg.push(function (data, complete) {
                    ret.data = data;
                    ret.complete = !!complete;
                    bridge.call("_returnValue", ret)
                })
                f.apply(ob, arg)
            }
            if (f) {
                callSyn(f, this._dsf);
            } else if (af) {
                callAsyn(af, this._dsaf);
            } else {
                name = info.method.split('.');
                if (name.length<2) return;
                var nsm=name.pop();
                var ns=name.join('.')
                var obs = this._dsf._obs;
                var ob = obs[ns] || {};
                var m = ob[nsm];
                if (m && typeof m == "function") {
                    callSyn(m, ob);
                    return;
                }
                obs = this._dsaf._obs;
                ob = obs[ns] || {};
                m = ob[nsm];
                if (m && typeof m == "function") {
                    callAsyn(m, ob);
                    return;
                }
            }
        }
    }
    for (var m in ob) {
        window[m] = ob[m]
    }
    bridge.register("_hasJavascriptMethod", function (name) {
        return !!window._dsf[name]
    })
}();

module.exports = bridge;