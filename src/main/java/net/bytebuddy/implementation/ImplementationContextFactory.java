package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.utility.RandomString;

/**
 * @author gongdewei 2023/4/20
 */
public class ImplementationContextFactory implements Implementation.Context.Factory {
    @Override
    public Implementation.Context.ExtractableView make(TypeDescription instrumentedType, AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                       TypeInitializer typeInitializer, ClassFileVersion classFileVersion,
                                                       ClassFileVersion auxiliaryClassFileVersion) {
        return this.make(instrumentedType, auxiliaryTypeNamingStrategy, typeInitializer, classFileVersion, auxiliaryClassFileVersion,
                Implementation.Context.FrameGeneration.GENERATE);
    }

    @Override
    public Implementation.Context.ExtractableView make(TypeDescription instrumentedType, AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                       TypeInitializer typeInitializer, ClassFileVersion classFileVersion,
                                                       ClassFileVersion auxiliaryClassFileVersion, Implementation.Context.FrameGeneration frameGeneration) {
        return new Implementation.Context.Default(instrumentedType, classFileVersion, auxiliaryTypeNamingStrategy, typeInitializer,
                auxiliaryClassFileVersion, frameGeneration,
                RandomString.hashOf(instrumentedType.getTypeName().hashCode()));
    }
}
