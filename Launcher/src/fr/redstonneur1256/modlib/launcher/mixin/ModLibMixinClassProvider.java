package fr.redstonneur1256.modlib.launcher.mixin;

import org.spongepowered.asm.service.IClassProvider;

import java.net.URL;

public class ModLibMixinClassProvider implements IClassProvider {

    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, true);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, getClass().getClassLoader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return findClass(name, initialize);
    }

}
