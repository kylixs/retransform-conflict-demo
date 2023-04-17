package com.example.demo;

import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.FieldVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gongdewei 2023/4/17
 */
public class RemoveDuplicatedFieldsClassVisitor extends ClassVisitor {

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
