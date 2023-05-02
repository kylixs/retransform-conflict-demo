package net.bytebuddy.agent.builder;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.utility.RandomString;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

/**
 * @author gongdewei 2023/4/21
 */
public class MyNativeMethodStrategy implements AgentBuilder.Default.NativeMethodStrategy{
    private String prefix;

    public MyNativeMethodStrategy(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public MethodNameTransformer resolve() {
        return new MethodNameTransformer.Prefixing(prefix) {
            @Override
            public String transform(MethodDescription methodDescription) {
                int hashCode = methodDescription.toString().hashCode();
                String name = super.transform(methodDescription) + "$" + RandomString.hashOf(hashCode);
                return name;
            }
        };
    }

    @Override
    public void apply(Instrumentation instrumentation, ClassFileTransformer classFileTransformer) {

    }
}
