package com.example.demo;

import net.bytebuddy.asm.Advice;

/**
 * @author gongdewei 2023/4/16
 */
public class HelloAdvice {
    @Advice.OnMethodEnter(inline = false)
    public static long invokeBeforeEnterMethod(
            @Advice.Origin String method) {
        System.out.println("Method invoked before enter method by: " + method);
        return System.currentTimeMillis();
    }

    @Advice.OnMethodExit(inline = false)
    public static void invokeAfterExitMethod(
            @Advice.Origin String method,
            @Advice.Enter long startTime
    ) {
        System.out.println("Method " + method + " took " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
