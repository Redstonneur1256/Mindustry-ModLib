package fr.redstonneur1256.modlib.patch;

import java.util.Arrays;

public class MultiClassLoader extends ClassLoader {

    private ClassLoader[] loaders;

    public MultiClassLoader(ClassLoader parent) {
        this(parent, new ClassLoader[0]);
    }

    public MultiClassLoader(ClassLoader parent, ClassLoader[] loaders) {
        super(parent);
        this.loaders = loaders;
    }

    public void addClassLoader(ClassLoader loader) {
        ClassLoader[] loaders = Arrays.copyOf(this.loaders, this.loaders.length + 1);
        loaders[loaders.length - 1] = loader;
        this.loaders = loaders;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for(ClassLoader loader : loaders) {
            Class<?> clazz = loader.loadClass(name);
            if(clazz != null) {
                if(resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }
        }
        return getParent().loadClass(name);
    }

}
