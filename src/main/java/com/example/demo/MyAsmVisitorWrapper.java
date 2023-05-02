package com.example.demo;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.FieldVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.pool.TypePool;

import java.util.ArrayList;
import java.util.List;

public class MyAsmVisitorWrapper implements AsmVisitorWrapper {

    @Override
    public int mergeWriter(int flags) {
        return flags;
    }

    @Override
    public int mergeReader(int flags) {
        return flags;
    }

    @Override
    public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, Implementation.Context implementationContext, TypePool typePool, FieldList<FieldDescription.InDefinedShape> fields, MethodList<?> methods, int writerFlags, int readerFlags) {
        if (classVisitor instanceof RemoveDuplicatedFieldsClassVisitor) {
            return classVisitor;
        }
        return new RemoveDuplicatedFieldsClassVisitor(Opcodes.ASM8, classVisitor);
    }

    static class RemoveDuplicatedFieldsClassVisitor extends ClassVisitor {

        private List<String> fieldNames = new ArrayList<>();

        public RemoveDuplicatedFieldsClassVisitor(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (fieldNames.contains(name)) {
                return null;
            }
            fieldNames.add(name);
            return super.visitField(access, name, descriptor, signature, value);
        }
    }
}
