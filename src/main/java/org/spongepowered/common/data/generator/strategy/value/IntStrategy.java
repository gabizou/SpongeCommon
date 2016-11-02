package org.spongepowered.common.data.generator.strategy.value;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.common.data.generator.DataImpl;
import org.spongepowered.common.data.generator.KeyContainer;
import org.spongepowered.common.data.generator.ValueGroupDisambiguator;
import org.spongepowered.common.data.generator.key.KeyObject;
import org.spongepowered.common.data.generator.strategy.ValueStrategy;
import org.spongepowered.common.data.generator.util.AsmTypeUtil;
import org.spongepowered.common.data.value.mutable.SpongeValue;

public class IntStrategy implements ValueStrategy<KeyObject<Integer>, Integer> {

    @Override
    public void visit(ClassWriter cw, FieldVisitor fv, DataImpl data, KeyContainer container, String methodName,
            String methodDescriptor, KeyObject<Integer> keyObject) {
        final String className = data.manipulatorClassName;
        final String baseFieldName = container.baseFieldName;

        final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, "()" + Type.getInternalName(container.key.getValueToken().getRawType()), null, null);
        mv.visitCode();

        mv.visitTypeInsn(NEW, Type.getInternalName(SpongeValue.class));
        mv.visitInsn(DUP);

        // Create the builder with the key value
        mv.visitFieldInsn(GETSTATIC, className, container.staticFieldName, Type.getInternalName(Key.class));

        // Load the default value
        AsmTypeUtil.addIntegerInstruction(mv, keyObject.defaultValue);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);

        // Load the actual value
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, baseFieldName, "I");
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);

        // Build the value
        mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(SpongeValue.class), "<init>",
                "(" + Type.getInternalName(Key.class) + Type.getInternalName(Object.class) + Type.getInternalName(Object.class) + ")V", false);
        // Return
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

    }

    @Override
    public KeyObject<Integer> generateKeyObject(ValueGroupDisambiguator disambiguator) {
        return null;
    }
}
