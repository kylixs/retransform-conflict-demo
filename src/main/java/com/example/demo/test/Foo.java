package com.example.demo.test;

import java.util.Arrays;
import java.util.List;

/**
 * @author gongdewei 2023/4/16
 */
public class Foo {
    public String sayHelloFoo() {
        return "hello from foo";
    }

    public List<String> doSomething(int in) {
        return Arrays.asList("" + in);
    }
}
