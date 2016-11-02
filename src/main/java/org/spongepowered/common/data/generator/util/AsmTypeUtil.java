package org.spongepowered.common.data.generator.util;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.MethodVisitor;

public class AsmTypeUtil {

    public static void addIntegerInstruction(MethodVisitor mv, int value) {
        if (value == 0) {
            mv.visitInsn(ICONST_0);
        } else if (value == 1) {
            mv.visitInsn(ICONST_1);
        } else if (value == 2) {
            mv.visitInsn(ICONST_2);
        } else if (value == 3) {
            mv.visitInsn(ICONST_3);
        } else if (value == 4) {
            mv.visitInsn(ICONST_4);
        } else if (value == 5) {
            mv.visitInsn(ICONST_5);
        } else if (value == -1) {
            mv.visitInsn(ICONST_M1);
        } else if (value <= Byte.MAX_VALUE && value >= Byte.MIN_VALUE) {
            mv.visitIntInsn(BIPUSH, value);
        } else if (value <= Short.MAX_VALUE && value >= Short.MIN_VALUE) {
            mv.visitIntInsn(SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    public static void addShortInstruction(MethodVisitor mv, short value) {
        if (value == 0) {
            mv.visitInsn(ICONST_0);
        } else if (value == 1) {
            mv.visitInsn(ICONST_1);
        } else if (value == 2) {
            mv.visitInsn(ICONST_2);
        } else if (value == 3) {
            mv.visitInsn(ICONST_3);
        } else if (value == 4) {
            mv.visitInsn(ICONST_4);
        } else if (value == 5) {
            mv.visitInsn(ICONST_5);
        } else if (value == -1) {
            mv.visitInsn(ICONST_M1);
        } else if (value <= Byte.MAX_VALUE && value >= Byte.MIN_VALUE) {
            mv.visitIntInsn(BIPUSH, value);
        } else {
            mv.visitIntInsn(SIPUSH, value);
        }
    }

    public static void addByteInstruction(MethodVisitor mv, byte value) {
        if (value == 0) {
            mv.visitInsn(ICONST_0);
        } else if (value == 1) {
            mv.visitInsn(ICONST_1);
        } else if (value == 2) {
            mv.visitInsn(ICONST_2);
        } else if (value == 3) {
            mv.visitInsn(ICONST_3);
        } else if (value == 4) {
            mv.visitInsn(ICONST_4);
        } else if (value == 5) {
            mv.visitInsn(ICONST_5);
        } else if (value == -1) {
            mv.visitInsn(ICONST_M1);
        } else {
            mv.visitIntInsn(BIPUSH, value);
        }
    }

    public static void addLongInstruction(MethodVisitor mv, long value) {
        if (value == 0) {
            mv.visitInsn(ICONST_0);
        } else if (value == 1) {
            mv.visitInsn(ICONST_1);
        } else if (value == 2) {
            mv.visitInsn(ICONST_2);
        } else if (value == 3) {
            mv.visitInsn(ICONST_3);
        } else if (value == 4) {
            mv.visitInsn(ICONST_4);
        } else if (value == 5) {
            mv.visitInsn(ICONST_5);
        } else if (value == -1) {
            mv.visitInsn(ICONST_M1);
        } else if (value <= Byte.MAX_VALUE && value >= Byte.MIN_VALUE) {
            mv.visitIntInsn(BIPUSH, (byte) value);
        } else if (value <= Short.MAX_VALUE && value >= Short.MIN_VALUE) {
            mv.visitIntInsn(SIPUSH, (short) value);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    public static void addFloatInstruction(MethodVisitor mv, float value) {
        if (value == 0.0f) {
            mv.visitInsn(FCONST_0);
        } else if (value == 1.0f) {
            mv.visitInsn(FCONST_1);
        } else if (value == 2.0f) {
            mv.visitInsn(FCONST_2);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    public static void addDoubleInstruction(MethodVisitor mv, double value) {
        if (value == 0.0d) {
            mv.visitInsn(DCONST_0);
        } else if (value == 1.0d) {
            mv.visitInsn(DCONST_1);
        } else {
            mv.visitLdcInsn(value);
        }
    }
}
