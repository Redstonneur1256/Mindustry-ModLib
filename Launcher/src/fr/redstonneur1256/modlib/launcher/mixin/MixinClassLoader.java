package fr.redstonneur1256.modlib.launcher.mixin;

import fr.redstonneur1256.modlib.launcher.util.Util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.*;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MixinClassLoader extends URLClassLoader {

    private static final Object transformer;
    private static final Method method;

    static {
        try {
            Class<?> transformerClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer");

            Constructor<?> constructor = transformerClass.getDeclaredConstructor();
            constructor.setAccessible(true);

            transformer = constructor.newInstance();
            method = transformerClass.getDeclaredMethod("transformClassBytes", String.class, String.class, byte[].class);
            method.setAccessible(true);
        } catch(Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private long totalFindTime;

    public MixinClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public MixinClassLoader(URL[] urls) {
        super(urls);
    }

    /**
     * Public friend :)
     */
    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        long start = System.nanoTime();
        try {
            int lastIndex = name.lastIndexOf('.');
            String packageName = lastIndex == -1 ? "" : name.substring(0, lastIndex);
            String classFileName = name.replace('.', '/') + ".class";

            URL url = getResource(classFileName);
            URLConnection connection = url == null ? null : url.openConnection();

            CodeSigner[] signers = null;
            if(lastIndex != -1 && connection instanceof JarURLConnection) {
                JarURLConnection jarConnection = (JarURLConnection) connection;
                JarFile file = jarConnection.getJarFile();

                // The URL provided by getResource points inside the jar causing foo's client to crash
                url = new URL("file:/" + URLEncoder.encode(file.getName(), "UTF-8"));

                if(file.getManifest() != null) {
                    JarEntry entry = file.getJarEntry(classFileName);
                    if(entry != null) {
                        signers = entry.getCodeSigners();
                    }

                    if(getPackage(packageName) == null) {
                        definePackage(packageName, file.getManifest(), url);
                    }
                }
            } else if(getPackage(packageName) == null) {
                definePackage(packageName, null, null, null, null, null, null, null);
            }
            byte[] rawClass = connection == null ? null : Util.readFully(connection.getInputStream());
            byte[] transformed = (byte[]) method.invoke(transformer, name, name, rawClass);

            if(transformed == null) {
                throw new ClassNotFoundException(name);
            }

            CodeSource codeSource = url == null ? null : new CodeSource(url, signers);

            Class<?> clazz = defineClass(name, transformed, 0, transformed.length, codeSource);
            if(clazz == null) {
                throw new ClassNotFoundException(name);
            }
            return clazz;
        } catch(ClassNotFoundException exception) {
            throw exception;
        } catch(Throwable throwable) {
            if(throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            throw new RuntimeException(throwable);
        } finally {
            totalFindTime += System.nanoTime() - start;
        }
    }

    public long getTotalFindTime() {
        return totalFindTime;
    }

    public double getTotalFindTimeMs() {
        return totalFindTime / 1_000_000.0;
    }

}
