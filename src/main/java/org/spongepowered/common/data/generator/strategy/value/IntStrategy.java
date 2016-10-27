package org.spongepowered.common.data.generator.strategy.value;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.spongepowered.common.data.generator.DataImpl;
import org.spongepowered.common.data.generator.KeyContainer;
import org.spongepowered.common.data.generator.strategy.ValueStrategy;

public class IntStrategy implements ValueStrategy {

    @Override
    public void visit(ClassWriter cw, FieldVisitor fv, MethodVisitor mv, DataImpl data, KeyContainer container, String methodName,
            String methodDescriptor) {

    }
}
