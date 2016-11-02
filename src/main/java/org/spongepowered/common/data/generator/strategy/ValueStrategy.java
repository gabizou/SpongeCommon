package org.spongepowered.common.data.generator.strategy;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.spongepowered.common.data.generator.DataImpl;
import org.spongepowered.common.data.generator.KeyContainer;
import org.spongepowered.common.data.generator.ValueGroupDisambiguator;
import org.spongepowered.common.data.generator.key.KeyObject;

public interface ValueStrategy<T extends KeyObject<E>, E> {

    void visit(ClassWriter cw, FieldVisitor fv, DataImpl data, KeyContainer container, String methodName, String methodDescriptor,
            T keyObject);

    T generateKeyObject(ValueGroupDisambiguator disambiguator);

}
