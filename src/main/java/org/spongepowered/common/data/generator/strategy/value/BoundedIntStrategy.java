package org.spongepowered.common.data.generator.strategy.value;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.spongepowered.common.data.generator.CustomDataClassBuilder;
import org.spongepowered.common.data.generator.DataImpl;
import org.spongepowered.common.data.generator.KeyContainer;
import org.spongepowered.common.data.generator.key.BoundedKeyObject;
import org.spongepowered.common.data.generator.strategy.ValueStrategy;

public class BoundedIntStrategy implements ValueStrategy {

    @Override
    public void visit(ClassWriter cw, FieldVisitor fv, MethodVisitor mv, DataImpl data, KeyContainer container, String methodName, String methodDescriptor) {
        final String className = data.manipulatorClassName;
        final String thisDescriptor = data.manipulatorDescriptor;
        final BoundedKeyObject keyObject = (BoundedKeyObject) container.keyObject;
        final String baseFieldName = container.baseFieldName;
        mv = cw.visitMethod(ACC_PUBLIC, methodName, "()Lorg/spongepowered/api/data/value/mutable/MutableBoundedValue;", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitFieldInsn(GETSTATIC, className, container.staticFieldName, CustomDataClassBuilder.API_KEY_CLASS);
        mv.visitMethodInsn(INVOKESTATIC, "org/spongepowered/common/data/value/SpongeValueFactory", "boundedBuilder",
                "(Lorg/spongepowered/api/data/key/Key;)Lorg/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder;", false);
        mv.visitInsn(ICONST_0);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder", "defaultValue",
                "(Ljava/lang/Object;)Lorg/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder;", true);
        mv.visitInsn(ICONST_0);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder", "minimum",
                "(Ljava/lang/Object;)Lorg/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder;", true);
        mv.visitIntInsn(BIPUSH, 10);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder", "maximum",
                "(Ljava/lang/Object;)Lorg/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder;", true);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, baseFieldName, "I");
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder", "actualValue",
                "(Ljava/lang/Object;)Lorg/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder;", true);
        Label l5 = new Label();
        mv.visitLabel(l5);

        mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder", "build",
                "()Lorg/spongepowered/api/data/value/mutable/MutableBoundedValue;", true);
        Label l6 = new Label();
        mv.visitLabel(l6);

        mv.visitInsn(ARETURN);
        Label l7 = new Label();
        mv.visitLabel(l7);
        mv.visitLocalVariable("this", thisDescriptor, null, l0, l7, 0);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        
    }
}
