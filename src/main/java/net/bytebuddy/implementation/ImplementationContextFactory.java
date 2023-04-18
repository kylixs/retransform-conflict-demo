package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;

/**
 * @author gongdewei 2023/4/16
 */
public class ImplementationContextFactory extends Implementation.Context.Default {

    /**
     * Creates a new default implementation context.
     *
     * @param instrumentedType            The description of the type that is currently subject of creation.
     * @param classFileVersion            The class file version of the created class.
     * @param auxiliaryTypeNamingStrategy The naming strategy for naming an auxiliary type.
     * @param typeInitializer             The type initializer of the created instrumented type.
     * @param auxiliaryClassFileVersion   The class file version to use for auxiliary classes.
     * @param frameGeneration             Determines the frame generation to be applied.
     * @param suffix                      The suffix to append to the names of accessor methods.
     */
    public ImplementationContextFactory(TypeDescription instrumentedType, ClassFileVersion classFileVersion, AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy, TypeInitializer typeInitializer, ClassFileVersion auxiliaryClassFileVersion, FrameGeneration frameGeneration, String suffix) {
        super(instrumentedType, classFileVersion, auxiliaryTypeNamingStrategy, typeInitializer, auxiliaryClassFileVersion, frameGeneration, suffix);
    }
}
