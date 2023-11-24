package fr.redstonneur1256.modlib.launcher;

import org.jetbrains.annotations.Nullable;

public interface ClassTransformer {

    /**
     * Priority of the transformer, transformers get applied in natural order
     */
    default int getTransformerPriority() {
        return 0;
    }

    byte @Nullable [] transform(byte @Nullable [] clazz, String name) throws Exception;

}
