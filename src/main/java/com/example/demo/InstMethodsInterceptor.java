package com.example.demo;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * @author gongdewei 2023/4/16
 */
public class InstMethodsInterceptor {
    public InstMethodsInterceptor(String interceptorClass, ClassLoader classLoader) {
    }

    @RuntimeType
    public Object intercept(@This Object obj, @AllArguments Object[] allArguments, @SuperCall Callable<?> zuper,
                            @Origin Method method) throws Throwable {

        Object originResult = zuper.call();
        System.out.printf("intercept: obj: %s, allArguments: %s, super: %s, method: %s, originResult: %s\n",
                obj, Arrays.asList(allArguments), zuper, method, originResult);
        return originResult;
    }


}
