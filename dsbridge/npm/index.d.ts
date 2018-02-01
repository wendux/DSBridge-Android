interface DSBridge{
    call(handlerName:string,args?:any,responseCallback?:(retValue:any)=>void);
    register(handlerName:string,handler:()=>any);
    register(namespace:string,synApiObject:object);
    registerAsyn(handlerName:string,handler:()=>void);
    registerAsyn(namespace:string,asyApiObject:object);
    hasNativeMethod(handlerName:string,type?:("all"|"asyn"|"syn"));
    disableJavascriptDialogBlock(disable?:boolean);
}
declare const bridge:DSBridge;
export default bridge;
