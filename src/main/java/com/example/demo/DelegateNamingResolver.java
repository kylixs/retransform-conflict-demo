package com.example.demo;

import net.bytebuddy.utility.RandomString;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Design to generate fixed delegate field name in retransform class
 */
public class DelegateNamingResolver {
    private static Map<String, AtomicInteger> NAME_CACHE = new ConcurrentHashMap<>();
    private final String enhanceOriginClassName;
    private final String fileNamePrefix;

    public DelegateNamingResolver(String enhanceOriginClassName) {
        this.enhanceOriginClassName = enhanceOriginClassName;
        fileNamePrefix = "sw_delegate$" + RandomString.hashOf(enhanceOriginClassName.hashCode()) + "$";
    }

    public String next() {
        AtomicInteger index = NAME_CACHE.computeIfAbsent(enhanceOriginClassName, key -> new AtomicInteger(0));
        return fileNamePrefix + index.incrementAndGet();
    }

    public static DelegateNamingResolver get(String enhanceOriginClassName) {
        return new DelegateNamingResolver(enhanceOriginClassName);
    }

    /**
     * do reset before global retransform
     */
    public static void reset() {
        NAME_CACHE.clear();
    }
}
