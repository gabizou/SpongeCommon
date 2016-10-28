package org.spongepowered.common.data.generator.strategy.value;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.ValueFactory;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.common.data.generator.DataImpl;
import org.spongepowered.common.data.generator.KeyContainer;
import org.spongepowered.common.data.generator.key.BoundedKeyObject;
import org.spongepowered.common.data.generator.strategy.GetterValueStrategy;
import org.spongepowered.common.data.value.SpongeValueFactory;

public class BoundedIntStrategyGetter implements GetterValueStrategy {

    @Override
    public void visit(ClassWriter cw, FieldVisitor fv, MethodVisitor mv, DataImpl data, KeyContainer container, String methodName, String methodDescriptor) {
        final String className = data.manipulatorClassName;
        final String thisDescriptor = data.manipulatorDescriptor;
        final BoundedKeyObject keyObject = (BoundedKeyObject) container.keyObject;
        final String baseFieldName = container.baseFieldName;
        mv = cw.visitMethod(ACC_PUBLIC, methodName, "()" + Type.getInternalName(MutableBoundedValue.class), null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitFieldInsn(GETSTATIC, className, container.staticFieldName, Type.getInternalName(Key.class));
        mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(SpongeValueFactory.class), "boundedBuilder",
                "(" + Type.getInternalName(Key.class) + ")" + Type.getInternalName(ValueFactory.BoundedValueBuilder.class), false);
        mv.visitInsn(ICONST_0);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ValueFactory.BoundedValueBuilder.class), "defaultValue",
                "(" + Type.getInternalName(Object.class) + ")" + Type.getInternalName(ValueFactory.BoundedValueBuilder.class), true);
        mv.visitInsn(ICONST_0);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ValueFactory.BoundedValueBuilder.class), "minimum",
                "(" + Type.getInternalName(Object.class) + ")" + Type.getInternalName(ValueFactory.BoundedValueBuilder.class), true);
        mv.visitIntInsn(BIPUSH, 10); // Need to dynamically determine
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ValueFactory.BoundedValueBuilder.class), "maximum",
                "(" + Type.getInternalName(Object.class) + ")" + Type.getInternalName(ValueFactory.BoundedValueBuilder.class), true);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, baseFieldName, "I");
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ValueFactory.BoundedValueBuilder.class), "actualValue",
                "(" + Type.getInternalName(Object.class) + ")" + Type.getInternalName(ValueFactory.BoundedValueBuilder.class), true);
        Label l5 = new Label();
        mv.visitLabel(l5);

        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ValueFactory.BoundedValueBuilder.class), "build",
                "()" + Type.getInternalName(MutableBoundedValue.class), true);
        Label l6 = new Label();
        mv.visitLabel(l6);

        mv.visitInsn(ARETURN);
        Label l7 = new Label();
        mv.visitLabel(l7);
        mv.visitLocalVariable("this", thisDescriptor, null, l0, l7, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        
    }
}
