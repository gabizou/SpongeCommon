package org.spongepowered.common.data.generator.strategy.value;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.ValueFactory;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.common.data.generator.DataImpl;
import org.spongepowered.common.data.generator.KeyContainer;
import org.spongepowered.common.data.generator.ValueGroupDisambiguator;
import org.spongepowered.common.data.generator.key.BoundedKeyObject;
import org.spongepowered.common.data.generator.strategy.ValueStrategy;
import org.spongepowered.common.data.generator.util.AsmTypeUtil;
import org.spongepowered.common.data.value.SpongeValueFactory;

public class BoundedIntStrategy implements ValueStrategy<BoundedKeyObject<Integer>, Integer> {

    @Override
    public void visit(ClassWriter cw, FieldVisitor fv, DataImpl data, KeyContainer container, String methodName,
            String methodDescriptor, BoundedKeyObject<Integer> keyObject) {
        final String className = data.manipulatorClassName;
        final String baseFieldName = container.baseFieldName;

        final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, "()" + Type.getInternalName(MutableBoundedValue.class), null, null);
        mv.visitCode();

        // Create the builder with the key value
        mv.visitFieldInsn(GETSTATIC, className, container.staticFieldName, Type.getInternalName(Key.class));
        mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(SpongeValueFactory.class), "boundedBuilder",
                "(" + Type.getInternalName(Key.class) + ")" + Type.getInternalName(ValueFactory.BoundedValueBuilder.class), false);

        // Load the default value
        AsmTypeUtil.addIntegerInstruction(mv, keyObject.defaultValue);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ValueFactory.BoundedValueBuilder.class), "defaultValue",
                "(" + Type.getInternalName(Object.class) + ")" + Type.getInternalName(ValueFactory.BoundedValueBuilder.class), true);

        // Load the minimum value
        AsmTypeUtil.addIntegerInstruction(mv, keyObject.minimum);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ValueFactory.BoundedValueBuilder.class), "minimum",
                "(" + Type.getInternalName(Object.class) + ")" + Type.getInternalName(ValueFactory.BoundedValueBuilder.class), true);

        // Load the maximum value
        AsmTypeUtil.addIntegerInstruction(mv, keyObject.maximum);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ValueFactory.BoundedValueBuilder.class), "maximum",
                "(" + Type.getInternalName(Object.class) + ")" + Type.getInternalName(ValueFactory.BoundedValueBuilder.class), true);

        // Load the actual value
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, baseFieldName, "I");
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ValueFactory.BoundedValueBuilder.class), "actualValue",
                "(" + Type.getInternalName(Object.class) + ")" + Type.getInternalName(ValueFactory.BoundedValueBuilder.class), true);

        // Build the value
        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ValueFactory.BoundedValueBuilder.class), "build",
                "()" + Type.getInternalName(MutableBoundedValue.class), true);

        // Return
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        
    }

    @Override
    public BoundedKeyObject<Integer> generateKeyObject(ValueGroupDisambiguator disambiguator) {
        return null;
    }
}
