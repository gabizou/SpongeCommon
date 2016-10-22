package org.spongepowered.common.data.generator.strategy;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.spongepowered.common.data.generator.DataImpl;
import org.spongepowered.common.data.generator.KeyContainer;

public interface ValueStrategy {

    void visit(ClassWriter cw, FieldVisitor fv, MethodVisitor mv, DataImpl data, KeyContainer container, String methodName, String methodDescriptor);

}
