interface DSBridge {
    call(handlerName: string, args?: any, responseCallback?: (retValue: any) => void): any;
    call<T, R>(handlerName: string, args?: T, responseCallback?: (retValue: R) => void): R;

    register(handlerName: string, handler: () => any, async?: boolean): void;
    register<F>(handlerName: string, handler: F, async?: boolean): void;
    register(namespace: string, synApiObject: object, async?: boolean): void;

    registerAsyn(handlerName: string, handler: () => void): void;
    registerAsyn<F>(handlerName: string, handler: F): void;
    registerAsyn(namespace: string, asyApiObject: object): void;

    hasNativeMethod(handlerName: string, type?: ("all"|"asyn"|"syn")): boolean;
    disableJavascriptDialogBlock(disable?: boolean): void;
}

declare const bridge: DSBridge;

export default bridge;
