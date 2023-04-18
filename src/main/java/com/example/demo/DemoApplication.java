package com.example.demo;

import com.example.demo.test.Foo;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedTypeFactory;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.ImplementationContextFactory;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.RandomString;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private static void transformWithByteBuddy() {
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
            Map<TypeDescription, ImplementationContextFactory> implementationContextFactoryCache = new ConcurrentHashMap<>();
            ByteBuddy byteBuddy = new ByteBuddy()
//                    .with(new AuxiliaryType.NamingStrategy() {
//                        @Override
//                        public String name(TypeDescription instrumentedType) {
//                            return instrumentedType.getName() + "$auxiliary$" + RandomString.hashOf(instrumentedType.getCanonicalName().hashCode());
//                        }
//                    })
                    .with(new AuxiliaryType.NamingStrategy.Enumerating("auxiliary"))
                    .with(new NamingStrategy.AbstractBase() {
                        @Override
                        protected String name(TypeDescription superClass) {
                            return superClass.getName() + "$ByteBuddy$" + RandomString.hashOf(superClass.getCanonicalName().hashCode());
                        }
                    })
                    .with(new Implementation.Context.Factory() {
                        @Override
                        public Implementation.Context.ExtractableView make(TypeDescription instrumentedType, AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy, TypeInitializer typeInitializer, ClassFileVersion classFileVersion, ClassFileVersion auxiliaryClassFileVersion) {
                            return implementationContextFactoryCache.computeIfAbsent(instrumentedType, key ->
                                    new ImplementationContextFactory(instrumentedType, classFileVersion, auxiliaryTypeNamingStrategy, typeInitializer, auxiliaryClassFileVersion, Implementation.Context.FrameGeneration.DISABLED, RandomString.hashOf(instrumentedType.hashCode())));
                        }

                        @Override
                        public Implementation.Context.ExtractableView make(TypeDescription instrumentedType, AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy, TypeInitializer typeInitializer, ClassFileVersion classFileVersion, ClassFileVersion auxiliaryClassFileVersion, Implementation.Context.FrameGeneration frameGeneration) {
                            return implementationContextFactoryCache.computeIfAbsent(instrumentedType, key ->
                                    new ImplementationContextFactory(instrumentedType, classFileVersion, auxiliaryTypeNamingStrategy, typeInitializer, auxiliaryClassFileVersion, frameGeneration, RandomString.hashOf(instrumentedType.hashCode())));
                        }
                    })
                    .with(new InstrumentedTypeFactory())
                    ;


            new AgentBuilder.Default(byteBuddy)
                    .enableNativeMethodPrefix("_origin$")
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(ElementMatchers.named("com.example.demo.test.Foo"))
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                                return builder
                                        .visit(new MyAsmVisitorWrapper())
                                        .method(ElementMatchers.nameContainsIgnoreCase("sayHelloFoo"))
                                        .intercept(MethodDelegation.withDefaultConfiguration()
                                                .to(new InstMethodsInter(null, classLoader), "delegate$sayHelloFoo"));
                            }
                    )
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                                return builder
                                        .visit(new MyAsmVisitorWrapper())
                                        .method(ElementMatchers.nameContainsIgnoreCase("doSomething"))
                                        .intercept(MethodDelegation.withDefaultConfiguration()
                                                .to(new InstMethodsInter(null, classLoader), "delegate$doSomething"));
                            }
                    )
                    .with(new AgentBuilder.Listener.Adapter() {
                        @Override
                        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                            System.err.println(String.format("Transform Error: typeName: %s, classLoader: %s, module: %s, loaded: %s", typeName, classLoader, module, loaded));
                            throwable.printStackTrace();
                        }
                    })
//                    .with(new AgentBuilder.TransformerDecorator() {
//                                public ResettableClassFileTransformer decorate(ResettableClassFileTransformer classFileTransformer) {
//                                    return new ResettableClassFileTransformer.WithDelegation(classFileTransformer) {
//                                        @Override
//                                        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
//                                            //transform class
//                                            byte[] newClassfileBuffer = classFileTransformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
//                                            if (newClassfileBuffer != null) {
//                                                classfileBuffer = newClassfileBuffer;
//                                            }
//                                            if (!className.startsWith("com/example")) {
//                                                return classfileBuffer;
//                                            }
//                                            // remove duplicated fields
//                                            ClassReader classReader = new ClassReader(classfileBuffer);
//                                            ClassWriter classWriter = new ClassWriter(classReader, 0);
//                                            ClassVisitor classVisitor = new RemoveDuplicatedFieldsClassVisitor(Opcodes.ASM8, classWriter);
//                                            classReader.accept(classVisitor, 0);
//                                            classfileBuffer = classWriter.toByteArray();
//                                            return classfileBuffer;
//                                        }
//                                    };
//                                }
//                            }
//                    )
                    .installOn(ByteBuddyAgent.install());

            String result = new Foo().sayHelloFoo();
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
