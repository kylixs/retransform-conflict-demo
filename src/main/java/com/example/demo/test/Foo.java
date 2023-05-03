package com.example.demo.test;

import java.util.Arrays;
import java.util.List;

/**
 * @author gongdewei 2023/4/16
 */
public class Foo {
    private String name;

    public Foo() {
        this("Tom");
    }

    public Foo(String name) {
        this.name = name;
    }

    public String sayHelloFoo(String ... peoples) {
        return "hello from " + Arrays.asList(peoples);
    }

    public String sayHelloFoo(Integer people) {
        return "hello from sid : " + people;
    }

    public List<String> doSomething(int in) {
        return Arrays.asList("" + in);
    }
}
