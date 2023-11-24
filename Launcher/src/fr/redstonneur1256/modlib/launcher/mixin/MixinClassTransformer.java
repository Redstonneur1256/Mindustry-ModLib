package fr.redstonneur1256.modlib.launcher.mixin;

import fr.redstonneur1256.modlib.launcher.ClassTransformer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MixinClassTransformer implements ClassTransformer {


    private final Object transformer;
    private final Method method;

    public MixinClassTransformer() throws ReflectiveOperationException {
        Class<?> transformerClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer");

        Constructor<?> constructor = transformerClass.getDeclaredConstructor();
        constructor.setAccessible(true);

        transformer = constructor.newInstance();
        method = transformerClass.getDeclaredMethod("transformClassBytes", String.class, String.class, byte[].class);
        method.setAccessible(true);
    }

    @Override
    public int getTransformerPriority() {
        // always load mixins first, transform/generate classes
        return Integer.MIN_VALUE;
    }

    @Override
    public byte[] transform(byte[] clazz, String name) throws Exception {
        return (byte[]) method.invoke(transformer, name, name, clazz);
    }

}
