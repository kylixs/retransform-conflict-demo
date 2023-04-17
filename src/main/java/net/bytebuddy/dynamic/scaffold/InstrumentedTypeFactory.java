package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.RecordComponentDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.LoadedTypeInitializer;

import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * @author gongdewei 2023/4/16
 */
public class InstrumentedTypeFactory implements InstrumentedType.Factory {
    @Override
    public InstrumentedType.WithFlexibleName represent(TypeDescription typeDescription) {
        return new InstrumentedTypeDefault(typeDescription.getName(),
                typeDescription.getModifiers(),
                typeDescription.getSuperClass(),
                typeDescription.getTypeVariables().asTokenList(is(typeDescription)),
                typeDescription.getInterfaces().accept(TypeDescription.Generic.Visitor.Substitutor.ForDetachment.of(typeDescription)),
                typeDescription.getDeclaredFields().asTokenList(is(typeDescription)),
                typeDescription.getDeclaredMethods().asTokenList(is(typeDescription)),
                typeDescription.getRecordComponents().asTokenList(is(typeDescription)),
                typeDescription.getDeclaredAnnotations(),
                TypeInitializer.None.INSTANCE,
                LoadedTypeInitializer.NoOp.INSTANCE,
                typeDescription.getDeclaringType(),
                typeDescription.getEnclosingMethod(),
                typeDescription.getEnclosingType(),
                typeDescription.getDeclaredTypes(),
                typeDescription.isAnonymousType(),
                typeDescription.isLocalType(),
                typeDescription.isRecord(),
                typeDescription.isNestHost()
                        ? TargetType.DESCRIPTION
                        : typeDescription.getNestHost(),
                typeDescription.isNestHost()
                        ? typeDescription.getNestMembers().filter(not(is(typeDescription)))
                        : Collections.<TypeDescription>emptyList());
    }

    @Override
    public InstrumentedType.WithFlexibleName subclass(String name, int modifiers, TypeDescription.Generic superClass) {
        return new InstrumentedTypeDefault(name,
                modifiers,
                superClass,
                Collections.<TypeVariableToken>emptyList(),
                Collections.<TypeDescription.Generic>emptyList(),
                Collections.<FieldDescription.Token>emptyList(),
                Collections.<MethodDescription.Token>emptyList(),
                Collections.<RecordComponentDescription.Token>emptyList(),
                Collections.<AnnotationDescription>emptyList(),
                TypeInitializer.None.INSTANCE,
                LoadedTypeInitializer.NoOp.INSTANCE,
                TypeDescription.UNDEFINED,
                MethodDescription.UNDEFINED,
                TypeDescription.UNDEFINED,
                Collections.<TypeDescription>emptyList(),
                false,
                false,
                false,
                TargetType.DESCRIPTION,
                Collections.<TypeDescription>emptyList());
    }

}
