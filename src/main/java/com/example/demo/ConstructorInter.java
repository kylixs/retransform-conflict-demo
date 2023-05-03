package com.example.demo;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.util.Arrays;

/**
 * The actual byte-buddy's interceptor to intercept constructor methods. In this class, it provides a bridge between
 * byte-buddy and sky-walking plugin.
 */
public class ConstructorInter {
    private String constructorInterceptorClassName;
    private ClassLoader classLoader;


    /**
     * @param constructorInterceptorClassName class full name.
     */
    public ConstructorInter(String constructorInterceptorClassName, ClassLoader classLoader) {
        this.constructorInterceptorClassName = constructorInterceptorClassName;
        this.classLoader = classLoader;
    }

    /**
     * Intercept the target constructor.
     *
     * @param obj          target class instance.
     * @param allArguments all constructor arguments
     */
    @RuntimeType
    public void intercept(@This Object obj, @AllArguments Object[] allArguments) {
        System.out.println(String.format("ConstructorInter: target: %s, args: %s", obj, Arrays.asList(allArguments)));
    }
}