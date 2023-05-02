package com.example.demo;

import com.example.demo.test.Foo;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(DemoApplication.class, args);
        Instrumentation instrumentation = ByteBuddyAgent.install();

        String clazzName = "com.example.demo.test.Foo";

        System.out.println("transform with bytebuddy: "+clazzName);
        transformWithByteBuddy();

        testRetransformClass(instrumentation, clazzName);
        testRetransformClass(instrumentation, TestController.class);
    }

    private static void testRetransformClass(Instrumentation instrumentation, String clazzName) throws Exception {
        testRetransformClass(instrumentation, Class.forName(clazzName));
    }

    private static void testRetransformClass(Instrumentation instrumentation, Class clazz) throws Exception {
        System.out.println("=========================");
        System.out.println("before retransform: "+ clazz.getName());

        List<String> classesBeforeReTransform = findClassesStartsWith(instrumentation, clazz.getName());
        printStrings(classesBeforeReTransform);
        System.out.println();

        if ("false".equalsIgnoreCase(System.getenv("retransform"))) {
            return;
        }
        System.out.println("retransform: " + clazz);
        reTransform(instrumentation, clazz);
        System.out.println();

        System.out.println("after retransform: "+ clazz);
        List<String> classesAfterReTransform = findClassesStartsWith(instrumentation, clazz.getName());
        printStrings(classesAfterReTransform);
        System.out.println();

        System.out.println("check retransform classes:");
        //check classes
        Comparator<String> comparator = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        };
        classesBeforeReTransform.sort(comparator);
        classesAfterReTransform.sort(comparator);
        if (classesAfterReTransform.equals(classesBeforeReTransform)) {
            System.out.println("retransform classes successful.");
        } else {
            System.out.println("retransform classes not equal.");
        }
    }

    private static void transformWithByteBuddy() throws Exception {
            // 1. delegate to static method
//            new AgentBuilder.Default()
//                    .type(ElementMatchers.named("com.example.demo.test.Foo"))
//                    .transform((builder, typeDescription, classLoader, module) -> builder
//                            .method(ElementMatchers.nameContainsIgnoreCase("sayHelloFoo"))
//                            .intercept(MethodDelegation.to(Bar.class))
//                    )
//                    .installOn(ByteBuddyAgent.install());

            // 2. advice
//            new AgentBuilder.Default()
//                    .type(ElementMatchers.named("com.example.demo.test.Foo"))
//                    .transform((builder, typeDescription, classLoader, module) -> builder
//                            .method(ElementMatchers.nameContainsIgnoreCase("sayHelloFoo"))
//                            .intercept(Advice.to(HelloAdvice.class))
//                    )
//                    .installOn(ByteBuddyAgent.install());

            // 3. interceptor
            ByteBuddy byteBuddy = new ByteBuddy()
                    .with(new AuxiliaryType.NamingStrategy.Suffixing("sw_auxiliary"))
                    .with(new NamingStrategy.Suffixing("sw_bytebuddy"))
                    .with(new Implementation.Context.Default.Factory.WithFixedSuffix("sw_synthetic") )
//                    .with(new ImplementationContextFactory())
//                    .with(new InstrumentedTypeFactory())
                    ;


        //.enableNativeMethodPrefix("_sw_origin$");
        AgentBuilder agentBuilder = new AgentBuilder.Default(byteBuddy);
        NativeMethodStrategyFactory.inject(agentBuilder, AgentBuilder.Default.class);

        // avoid duplicate field on re-transform
        String className = "com.example.demo.test.Foo";
        agentBuilder.with(AgentBuilder.DescriptionStrategy.Default.POOL_FIRST)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(ElementMatchers.named(className))
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
//                                DelegateNamingResolver.reset();
//                                DelegateNamingResolver delegateNamingResolver = DelegateNamingResolver.get(typeDescription.getTypeName());

                                return builder
                                        .visit(new MyAsmVisitorWrapper())
                                        .method(ElementMatchers.nameContainsIgnoreCase("sayHelloFoo"))
                                        .intercept(MethodDelegation.withDefaultConfiguration()
//                                                .to(new InstMethodsInterceptor(null, classLoader)));
                                                .to(new InstMethodsInterceptor(null, classLoader), "_sw_delegate$sayHelloFoo"));
                            }
                    )
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                                return builder
                                        .visit(new MyAsmVisitorWrapper())
                                        .method(ElementMatchers.nameContainsIgnoreCase("doSomething"))
                                        .intercept(MethodDelegation.withDefaultConfiguration()
//                                                .to(new InstMethodsInter(null, classLoader)));
                                                .to(new InstMethodsInterceptor(null, classLoader), "_sw_delegate$doSomething"));
                            }
                    )
                    .with(new AgentBuilder.Listener.Adapter() {
                        @Override
                        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                            System.err.println(String.format("Transform Error: typeName: %s, classLoader: %s, module: %s, loaded: %s", typeName, classLoader, module, loaded));
                            throwable.printStackTrace();
                        }
                    })
                    .installOn(ByteBuddyAgent.install());

        String result = new Foo().sayHelloFoo("zs");
        System.out.println("sayHello result: " + result);

        result = new Foo().sayHelloFoo("zs", "ls");
        System.out.println("sayHello result: " + result);

        result = new Foo().sayHelloFoo(1001);
        System.out.println("sayHello result: " + result);

        List<String> list = new Foo().doSomething(123);
            System.out.println("doSomething result: " + list);
    }



    private static void reTransform(Instrumentation instrumentation, Class clazz) throws UnmodifiableClassException {
        ClassFileTransformer transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
//				System.out.println(String.format("className=%s, classBeingRedefined=%s, classloader=%s, protectionDomain=%s, classfileBuffer=%d",
//						className, classBeingRedefined, loader, protectionDomain.getCodeSource(), classfileBuffer.length));
                return null;
            }
        };
        try {
            instrumentation.addTransformer(transformer, true);
            instrumentation.retransformClasses(clazz);
        } finally {
            instrumentation.removeTransformer(transformer);
        }

    }

    private static List<String> findClassesStartsWith(Instrumentation instrumentation, String className) {
        List<String> classNames = new ArrayList<>();
        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        for (Class<?> clazz : allLoadedClasses) {
            if (clazz.getName().startsWith(className)) {
                classNames.add(clazz.getName());
            }
        }
        return classNames;
    }

    private static void printStrings(List<String> list) {
        for (String str : list) {
            System.out.println(str);
        }
    }
}
