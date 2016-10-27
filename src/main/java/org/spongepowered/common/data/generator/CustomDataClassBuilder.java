package org.spongepowered.common.data.generator;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.spongepowered.api.data.key.Key;

public class CustomDataClassBuilder {

    static final class Counter {
        private static int count;

        static int nextInt() {
            return count++;
        }
    }



    public static byte[] dump() throws Exception {

        final DataImpl data = new DataImpl();

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        FieldVisitor fv;
        MethodVisitor mv;

        final String name = data.manipulatorClassName;

        cw.visit(V1_8, ACC_PUBLIC + Opcodes.ACC_SUPER, name, null, "java/lang/Object",
                new String[]{Type.getInternalName(data.dataInterface)});

        final String thisDescriptor = "L" + name + ";";
        data.manipulatorDescriptor = thisDescriptor;

        // This is if the KeyContainer requires the bounded value builder, so do a for loop to validate possibly.
        cw.visitInnerClass("org/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder", "org/spongepowered/api/data/value/ValueFactory",
                "BoundedValueBuilder", ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT + ACC_INTERFACE);

        // filter through the key containers to generate the field names to be used for the rest of the generator
        for (KeyContainer container : data.containers) {
            final String keyFieldName = container.name.toUpperCase() + "_" + Integer.toString(container.name.hashCode() & 999) + "_" + Counter.nextInt();
            container.staticFieldName = keyFieldName;
            {
                // Set up the static field for the key
                fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, keyFieldName, Type.getInternalName(Key.class), null, null);
                fv.visitEnd();
            }
        }

        for (KeyContainer container : data.containers) {
            if (container.requiresBaseField) {
                final String fieldName = container.name + "$" + Integer.toString(container.name.hashCode() & 999) + "$" + Counter.nextInt();
                container.baseFieldName = fieldName;
                {
                    fv = cw.visitField(ACC_PRIVATE, fieldName, container.baseFieldType, null, null);
                    fv.visitEnd();
                }
            }
        }

        // Required constructor
        {
            mv = cw.visitMethod(0, "<init>", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitInsn(RETURN);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l2, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        // First method.... the myInt() that returns the API Value
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC, "myInt", "()Lorg/spongepowered/api/data/value/mutable/Value;",
                            "()Lorg/spongepowered/api/data/value/mutable/Value<Ljava/lang/Integer;>;", null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitFieldInsn(GETSTATIC, name, "MY_INT_KEY$aabc000111",
                    Type.getInternalName(Key.class));
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
            mv.visitFieldInsn(GETFIELD, name, "myInt$aabc0001", "I");
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
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC, "enchantment", "()Lorg/spongepowered/api/data/value/mutable/ListValue;",
                            "()Lorg/spongepowered/api/data/value/mutable/ListValue<Lorg/spongepowered/api/data/meta/ItemEnchantment;>;", null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitTypeInsn(NEW, "org/spongepowered/common/data/value/mutable/SpongeListValue");
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETSTATIC, name, "MY_ENCHANTMENT_KEY$aabbcc000011",
                    Type.getInternalName(Key.class));
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, "myEnchantment$aabcls001",
                    "Ljava/util/List;");
            mv.visitMethodInsn(INVOKESPECIAL, "org/spongepowered/common/data/value/mutable/SpongeListValue", "<init>",
                    "(Lorg/spongepowered/api/data/key/Key;Ljava/util/List;)V", false);
            mv.visitInsn(ARETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l1, 0);
            mv.visitMaxs(4, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "myIntValue", "()I", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, "myInt$aabc0001", "I");
            mv.visitInsn(IRETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "setMyInt", "(I)V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitFieldInsn(PUTFIELD, name, "myInt$aabc0001", "I");
            Label l1 = new Label();
            mv.visitLabel(l1);

            mv.visitInsn(RETURN);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l2, 0);
            mv.visitLocalVariable("value", "I", null, l0, l2, 1);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC, "getEnchantment", "()Ljava/util/List;",
                            "()Ljava/util/List<Lorg/spongepowered/api/data/meta/ItemEnchantment;>;", null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, "myEnchantment$aabcls001",
                    "Ljava/util/List;");
            mv.visitInsn(ARETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC, "setEnchantment", "(Ljava/util/List;)V",
                            "(Ljava/util/List<Lorg/spongepowered/api/data/meta/ItemEnchantment;>;)V", null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("myEnchantment cannot be null!");
            mv.visitMethodInsn(INVOKESTATIC, "com/google/common/base/Preconditions", "checkNotNull",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "java/util/List");
            mv.visitFieldInsn(PUTFIELD, name, "myEnchantment$aabcls001",
                    "Ljava/util/List;");
            Label l1 = new Label();
            mv.visitLabel(l1);

            mv.visitInsn(RETURN);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l2, 0);
            mv.visitLocalVariable("enchantment", "Ljava/util/List;", "Ljava/util/List<Lorg/spongepowered/api/data/meta/ItemEnchantment;>;", l0, l2,
                    1);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "getContentVersion", "()I", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitInsn(ICONST_1);
            mv.visitInsn(IRETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "toContainer", "()Lorg/spongepowered/api/data/DataContainer;", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitTypeInsn(NEW, "org/spongepowered/api/data/MemoryDataContainer");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "org/spongepowered/api/data/MemoryDataContainer", "<init>", "()V", false);
            mv.visitFieldInsn(GETSTATIC, "org/spongepowered/api/data/Queries", "CONTENT_VERSION", "Lorg/spongepowered/api/data/DataQuery;");
            mv.visitInsn(ICONST_1);
            Label l1 = new Label();
            mv.visitLabel(l1);

            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/spongepowered/api/data/MemoryDataContainer", "set",
                    "(Lorg/spongepowered/api/data/DataQuery;Ljava/lang/Object;)Lorg/spongepowered/api/data/DataContainer;", false);
            mv.visitFieldInsn(GETSTATIC, name, "MY_INT_KEY$aabc000111",
                    Type.getInternalName(Key.class));
            Label l2 = new Label();
            mv.visitLabel(l2);

            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/key/Key", "getQuery", "()Lorg/spongepowered/api/data/DataQuery;", true);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, "myInt$aabc0001", "I");
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/DataContainer", "set",
                    "(Lorg/spongepowered/api/data/DataQuery;Ljava/lang/Object;)Lorg/spongepowered/api/data/DataContainer;", true);
            mv.visitFieldInsn(GETSTATIC, name, "MY_ENCHANTMENT_KEY$aabbcc000011",
                    Type.getInternalName(Key.class));
            Label l3 = new Label();
            mv.visitLabel(l3);

            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/key/Key", "getQuery", "()Lorg/spongepowered/api/data/DataQuery;", true);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, "myEnchantment$aabcls001",
                    "Ljava/util/List;");
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/DataContainer", "set",
                    "(Lorg/spongepowered/api/data/DataQuery;Ljava/lang/Object;)Lorg/spongepowered/api/data/DataContainer;", true);
            Label l4 = new Label();
            mv.visitLabel(l4);

            mv.visitInsn(ARETURN);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l5, 0);
            mv.visitMaxs(3, 1);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC, "get", "(Lorg/spongepowered/api/data/key/Key;)Ljava/util/Optional;",
                            "<E:Ljava/lang/Object;>(Lorg/spongepowered/api/data/key/Key<+Lorg/spongepowered/api/data/value/BaseValue<TE;>;>;)Ljava/util/Optional<TE;>;",
                            null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, name, "MY_INT_KEY$aabc000111",
                    Type.getInternalName(Key.class));
            Label l1 = new Label();
            mv.visitJumpInsn(IF_ACMPNE, l1);
            Label l2 = new Label();
            mv.visitLabel(l2);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, "myInt$aabc0001", "I");
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "of", "(Ljava/lang/Object;)Ljava/util/Optional;", false);
            mv.visitInsn(ARETURN);
            mv.visitLabel(l1);

            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, name, "MY_ENCHANTMENT_KEY$aabbcc000011",
                    Type.getInternalName(Key.class));
            Label l3 = new Label();
            mv.visitJumpInsn(IF_ACMPNE, l3);
            Label l4 = new Label();
            mv.visitLabel(l4);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, "myEnchantment$aabcls001",
                    "Ljava/util/List;");
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "of", "(Ljava/lang/Object;)Ljava/util/Optional;", false);
            mv.visitInsn(ARETURN);
            mv.visitLabel(l3);

            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;", false);
            mv.visitInsn(ARETURN);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l5, 0);
            mv.visitLocalVariable("key", Type.getInternalName(Key.class),
                    "Lorg/spongepowered/api/data/key/Key<+Lorg/spongepowered/api/data/value/BaseValue<TE;>;>;", l0, l5, 1);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC, "getValue", "(Lorg/spongepowered/api/data/key/Key;)Ljava/util/Optional;",
                            "<E:Ljava/lang/Object;V::Lorg/spongepowered/api/data/value/BaseValue<TE;>;>(Lorg/spongepowered/api/data/key/Key<TV;>;)Ljava/util/Optional<TV;>;",
                            null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, name, "MY_INT_KEY$aabc000111",
                    Type.getInternalName(Key.class));
            Label l1 = new Label();
            mv.visitJumpInsn(IF_ACMPNE, l1);
            Label l2 = new Label();
            mv.visitLabel(l2);

            mv.visitFieldInsn(GETSTATIC, name, "MY_INT_KEY$aabc000111",
                    Type.getInternalName(Key.class));
            mv.visitMethodInsn(INVOKESTATIC, "org/spongepowered/common/data/value/SpongeValueFactory", "boundedBuilder",
                    "(Lorg/spongepowered/api/data/key/Key;)Lorg/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder;", false);
            mv.visitInsn(ICONST_0);
            Label l3 = new Label();
            mv.visitLabel(l3);

            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder", "defaultValue",
                    "(Ljava/lang/Object;)Lorg/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder;", true);
            mv.visitInsn(ICONST_0);
            Label l4 = new Label();
            mv.visitLabel(l4);

            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder", "minimum",
                    "(Ljava/lang/Object;)Lorg/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder;", true);
            mv.visitIntInsn(BIPUSH, 10);
            Label l5 = new Label();
            mv.visitLabel(l5);

            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder", "maximum",
                    "(Ljava/lang/Object;)Lorg/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder;", true);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, "myInt$aabc0001", "I");
            Label l6 = new Label();
            mv.visitLabel(l6);

            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder", "actualValue",
                    "(Ljava/lang/Object;)Lorg/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder;", true);
            Label l7 = new Label();
            mv.visitLabel(l7);

            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/value/ValueFactory$BoundedValueBuilder", "build",
                    "()Lorg/spongepowered/api/data/value/mutable/MutableBoundedValue;", true);
            Label l8 = new Label();
            mv.visitLabel(l8);

            mv.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "of", "(Ljava/lang/Object;)Ljava/util/Optional;", false);
            mv.visitInsn(ARETURN);
            mv.visitLabel(l1);

            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, name, "MY_ENCHANTMENT_KEY$aabbcc000011",
                    Type.getInternalName(Key.class));
            Label l9 = new Label();
            mv.visitJumpInsn(IF_ACMPNE, l9);
            Label l10 = new Label();
            mv.visitLabel(l10);

            mv.visitTypeInsn(NEW, "org/spongepowered/common/data/value/mutable/SpongeListValue");
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETSTATIC, name, "MY_ENCHANTMENT_KEY$aabbcc000011",
                    Type.getInternalName(Key.class));
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, "myEnchantment$aabcls001",
                    "Ljava/util/List;");
            mv.visitMethodInsn(INVOKESPECIAL, "org/spongepowered/common/data/value/mutable/SpongeListValue", "<init>",
                    "(Lorg/spongepowered/api/data/key/Key;Ljava/util/List;)V", false);
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "of", "(Ljava/lang/Object;)Ljava/util/Optional;", false);
            mv.visitInsn(ARETURN);
            mv.visitLabel(l9);

            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;", false);
            mv.visitInsn(ARETURN);
            Label l11 = new Label();
            mv.visitLabel(l11);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l11, 0);
            mv.visitLocalVariable("key", Type.getInternalName(Key.class), "Lorg/spongepowered/api/data/key/Key<TV;>;", l0, l11, 1);
            mv.visitMaxs(4, 2);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC, "supports", "(Lorg/spongepowered/api/data/key/Key;)Z", "(Lorg/spongepowered/api/data/key/Key<*>;)Z",
                            null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, name, "MY_INT_KEY$aabc000111",
                    Type.getInternalName(Key.class));
            Label l1 = new Label();
            mv.visitJumpInsn(IF_ACMPEQ, l1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, name, "MY_ENCHANTMENT_KEY$aabbcc000011",
                    Type.getInternalName(Key.class));
            Label l2 = new Label();
            mv.visitJumpInsn(IF_ACMPNE, l2);
            mv.visitLabel(l1);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(ICONST_1);
            Label l3 = new Label();
            mv.visitJumpInsn(GOTO, l3);
            mv.visitLabel(l2);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(ICONST_0);
            mv.visitLabel(l3);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
            mv.visitInsn(IRETURN);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l4, 0);
            mv.visitLocalVariable("key", Type.getInternalName(Key.class), "Lorg/spongepowered/api/data/key/Key<*>;", l0, l4, 1);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "getKeys", "()Ljava/util/Set;", "()Ljava/util/Set<Lorg/spongepowered/api/data/key/Key<*>;>;", null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitFieldInsn(GETSTATIC, name, "KEYS",
                    "Lcom/google/common/collect/ImmutableSet;");
            mv.visitInsn(ARETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC, "getValues", "()Ljava/util/Set;",
                            "()Ljava/util/Set<Lorg/spongepowered/api/data/value/immutable/ImmutableValue<*>;>;", null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitTypeInsn(NEW, "java/util/HashSet");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashSet", "<init>", "()V", false);
            mv.visitVarInsn(ASTORE, 1);
            Label l1 = new Label();
            mv.visitLabel(l1);

            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, name, "myInt",
                    "()Lorg/spongepowered/api/data/value/mutable/Value;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/value/mutable/Value", "asImmutable",
                    "()Lorg/spongepowered/api/data/value/immutable/ImmutableValue;", true);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashSet", "add", "(Ljava/lang/Object;)Z", false);
            mv.visitInsn(POP);
            Label l2 = new Label();
            mv.visitLabel(l2);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, name, "enchantment",
                    "()Lorg/spongepowered/api/data/value/mutable/ListValue;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/value/mutable/ListValue", "asImmutable",
                    "()Lorg/spongepowered/api/data/value/immutable/ImmutableCollectionValue;", true);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashSet", "add", "(Ljava/lang/Object;)Z", false);
            mv.visitInsn(POP);
            Label l3 = new Label();
            mv.visitLabel(l3);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ARETURN);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l4, 0);
            mv.visitLocalVariable("values", "Ljava/util/HashSet;",
                    "Ljava/util/HashSet<Lorg/spongepowered/api/data/value/immutable/ImmutableValue<*>;>;", l1, l4, 1);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC, "fill",
                            "(Lorg/spongepowered/api/data/DataHolder;Lorg/spongepowered/api/data/merge/MergeFunction;)Ljava/util/Optional;",
                            "(Lorg/spongepowered/api/data/DataHolder;Lorg/spongepowered/api/data/merge/MergeFunction;)Ljava/util/Optional<Lorg/spongepowered/api/data/manipulator/generator/testing/DummyManipulator;>;",
                            null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l1, 0);
            mv.visitLocalVariable("dataHolder", "Lorg/spongepowered/api/data/DataHolder;", null, l0, l1, 1);
            mv.visitLocalVariable("overlap", "Lorg/spongepowered/api/data/merge/MergeFunction;", null, l0, l1, 2);
            mv.visitMaxs(1, 3);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC, "from", "(Lorg/spongepowered/api/data/DataContainer;)Ljava/util/Optional;",
                            "(Lorg/spongepowered/api/data/DataContainer;)Ljava/util/Optional<Lorg/spongepowered/api/data/manipulator/generator/testing/DummyManipulator;>;",
                            null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, name, "MY_INT_KEY$aabc000111",
                    Type.getInternalName(Key.class));
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/key/Key", "getQuery", "()Lorg/spongepowered/api/data/DataQuery;", true);
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/DataContainer", "contains", "(Lorg/spongepowered/api/data/DataQuery;)Z",
                    true);
            Label l1 = new Label();
            mv.visitJumpInsn(IFEQ, l1);
            Label l2 = new Label();
            mv.visitLabel(l2);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, name, "MY_INT_KEY$aabc000111",
                    Type.getInternalName(Key.class));
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/key/Key", "getQuery", "()Lorg/spongepowered/api/data/DataQuery;", true);
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/DataContainer", "getInt",
                    "(Lorg/spongepowered/api/data/DataQuery;)Ljava/util/Optional;", true);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
            mv.visitFieldInsn(PUTFIELD, name, "myInt$aabc0001", "I");
            mv.visitLabel(l1);

            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, name, "MY_ENCHANTMENT_KEY$aabbcc000011",
                    Type.getInternalName(Key.class));
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/key/Key", "getQuery", "()Lorg/spongepowered/api/data/DataQuery;", true);
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/DataContainer", "contains", "(Lorg/spongepowered/api/data/DataQuery;)Z",
                    true);
            Label l3 = new Label();
            mv.visitJumpInsn(IFEQ, l3);
            Label l4 = new Label();
            mv.visitLabel(l4);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, name, "MY_ENCHANTMENT_KEY$aabbcc000011",
                    Type.getInternalName(Key.class));
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/key/Key", "getQuery", "()Lorg/spongepowered/api/data/DataQuery;", true);
            mv.visitLdcInsn(Type.getType("Lorg/spongepowered/api/data/meta/ItemEnchantment;"));
            mv.visitMethodInsn(INVOKEINTERFACE, "org/spongepowered/api/data/DataContainer", "getSerializableList",
                    "(Lorg/spongepowered/api/data/DataQuery;Ljava/lang/Class;)Ljava/util/Optional;", true);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "java/util/List");
            mv.visitFieldInsn(PUTFIELD, name, "myEnchantment$aabcls001",
                    "Ljava/util/List;");
            mv.visitLabel(l3);

            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "of", "(Ljava/lang/Object;)Ljava/util/Optional;", false);
            mv.visitInsn(ARETURN);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l5, 0);
            mv.visitLocalVariable("container", "Lorg/spongepowered/api/data/DataContainer;", null, l0, l5, 1);
            mv.visitMaxs(4, 2);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC, "set",
                            "(Lorg/spongepowered/api/data/key/Key;Ljava/lang/Object;)Lorg/spongepowered/api/data/manipulator/generator/testing/DummyManipulator;",
                            "<E:Ljava/lang/Object;>(Lorg/spongepowered/api/data/key/Key<+Lorg/spongepowered/api/data/value/BaseValue<TE;>;>;TE;)Lorg/spongepowered/api/data/manipulator/generator/testing/DummyManipulator;",
                            null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, name, "MY_INT_KEY$aabc000111",
                    Type.getInternalName(Key.class));
            Label l1 = new Label();
            mv.visitJumpInsn(IF_ACMPNE, l1);
            Label l2 = new Label();
            mv.visitLabel(l2);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
            mv.visitFieldInsn(PUTFIELD, name, "myInt$aabc0001", "I");
            mv.visitLabel(l1);

            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETSTATIC, name, "MY_ENCHANTMENT_KEY$aabbcc000011",
                    Type.getInternalName(Key.class));
            Label l3 = new Label();
            mv.visitJumpInsn(IF_ACMPNE, l3);
            Label l4 = new Label();
            mv.visitLabel(l4);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, "java/util/List");
            mv.visitFieldInsn(PUTFIELD, name, "myEnchantment$aabcls001",
                    "Ljava/util/List;");
            mv.visitLabel(l3);

            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ARETURN);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l5, 0);
            mv.visitLocalVariable("key", Type.getInternalName(Key.class),
                    "Lorg/spongepowered/api/data/key/Key<+Lorg/spongepowered/api/data/value/BaseValue<TE;>;>;", l0, l5, 1);
            mv.visitLocalVariable("value", "Ljava/lang/Object;", "TE;", l0, l5, 2);
            mv.visitMaxs(2, 3);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "copy", "()Lorg/spongepowered/api/data/manipulator/generator/testing/DummyManipulator;", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitTypeInsn(NEW, name);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V", false);
            mv.visitVarInsn(ASTORE, 1);
            Label l1 = new Label();
            mv.visitLabel(l1);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, "myInt$aabc0001", "I");
            mv.visitFieldInsn(PUTFIELD, name, "myInt$aabc0001", "I");
            Label l2 = new Label();
            mv.visitLabel(l2);

            mv.visitTypeInsn(NEW, "java/util/ArrayList");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
            mv.visitVarInsn(ASTORE, 2);
            Label l3 = new Label();
            mv.visitLabel(l3);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, name, "myEnchantment$aabcls001",
                    "Ljava/util/List;");
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;", true);
            mv.visitVarInsn(ASTORE, 3);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitFrame(Opcodes.F_APPEND, 3,
                    new Object[]{name, "java/util/ArrayList", "java/util/Iterator"}, 0,
                    null);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
            Label l5 = new Label();
            mv.visitJumpInsn(IFEQ, l5);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "org/spongepowered/api/data/meta/ItemEnchantment");
            mv.visitVarInsn(ASTORE, 4);
            Label l6 = new Label();
            mv.visitLabel(l6);

            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);
            mv.visitInsn(POP);
            Label l7 = new Label();
            mv.visitLabel(l7);

            mv.visitJumpInsn(GOTO, l4);
            mv.visitLabel(l5);

            mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitFieldInsn(PUTFIELD, name, "myEnchantment$aabcls001",
                    "Ljava/util/List;");
            Label l8 = new Label();
            mv.visitLabel(l8);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ARETURN);
            Label l9 = new Label();
            mv.visitLabel(l9);
            mv.visitLocalVariable("itemEnchantment", "Lorg/spongepowered/api/data/meta/ItemEnchantment;", null, l6, l7, 4);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l9, 0);
            mv.visitLocalVariable("dummyManipulator", thisDescriptor, null, l1, l9, 1);
            mv.visitLocalVariable("objects", "Ljava/util/ArrayList;", "Ljava/util/ArrayList<Lorg/spongepowered/api/data/meta/ItemEnchantment;>;", l3,
                    l9, 2);
            mv.visitMaxs(2, 5);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC, "asImmutable",
                            "()Lorg/spongepowered/api/data/manipulator/generator/testing/ImmutableDummyManipulator;", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC + Opcodes.ACC_BRIDGE + Opcodes.ACC_SYNTHETIC, "asImmutable",
                            "()Lorg/spongepowered/api/data/manipulator/ImmutableDataManipulator;", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, name, "asImmutable",
                    "()Lorg/spongepowered/api/data/manipulator/generator/testing/ImmutableDummyManipulator;", false);
            mv.visitInsn(ARETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC + Opcodes.ACC_BRIDGE + Opcodes.ACC_SYNTHETIC, "copy", "()Lorg/spongepowered/api/data/manipulator/DataManipulator;",
                            null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, name, "copy",
                    "()Lorg/spongepowered/api/data/manipulator/generator/testing/DummyManipulator;", false);
            mv.visitInsn(ARETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv =
                    cw.visitMethod(ACC_PUBLIC + Opcodes.ACC_BRIDGE + Opcodes.ACC_SYNTHETIC, "set",
                            "(Lorg/spongepowered/api/data/key/Key;Ljava/lang/Object;)Lorg/spongepowered/api/data/manipulator/DataManipulator;", null,
                            null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, name, "set",
                    "(Lorg/spongepowered/api/data/key/Key;Ljava/lang/Object;)Lorg/spongepowered/api/data/manipulator/generator/testing/DummyManipulator;",
                    false);
            mv.visitInsn(ARETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l1, 0);
            mv.visitMaxs(3, 3);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "copy", "()Lorg/spongepowered/api/data/value/ValueContainer;", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, name, "copy",
                    "()Lorg/spongepowered/api/data/manipulator/generator/testing/DummyManipulator;", false);
            mv.visitInsn(ARETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", thisDescriptor, null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);

            mv.visitFieldInsn(GETSTATIC, "org/spongepowered/api/data/manipulator/generator/testing/DummyCustomDataData", "MY_INT_KEY",
                    Type.getInternalName(Key.class));
            mv.visitFieldInsn(PUTSTATIC, name, "MY_INT_KEY$aabc000111",
                    Type.getInternalName(Key.class));
            Label l1 = new Label();
            mv.visitLabel(l1);

            mv.visitFieldInsn(GETSTATIC, "org/spongepowered/api/data/manipulator/generator/testing/DummyCustomDataData", "MY_ENCHANTMENT_KEY",
                    Type.getInternalName(Key.class));
            mv.visitFieldInsn(PUTSTATIC, name, "MY_ENCHANTMENT_KEY$aabbcc000011",
                    Type.getInternalName(Key.class));
            Label l2 = new Label();
            mv.visitLabel(l2);

            mv.visitFieldInsn(GETSTATIC, name, "MY_INT_KEY$aabc000111",
                    Type.getInternalName(Key.class));
            mv.visitFieldInsn(GETSTATIC, name, "MY_ENCHANTMENT_KEY$aabbcc000011",
                    Type.getInternalName(Key.class));
            mv.visitMethodInsn(INVOKESTATIC, "com/google/common/collect/ImmutableSet", "of",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableSet;", false);
            mv.visitFieldInsn(PUTSTATIC, name, "KEYS",
                    "Lcom/google/common/collect/ImmutableSet;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

}
