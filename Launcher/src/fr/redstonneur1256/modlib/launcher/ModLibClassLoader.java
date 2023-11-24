package fr.redstonneur1256.modlib.launcher;

import fr.redstonneur1256.modlib.launcher.util.Util;

import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModLibClassLoader extends URLClassLoader {

    private List<ClassTransformer> transformers;
    private long totalClassLoadingTime;

    public ModLibClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.transformers = new ArrayList<>();
        this.totalClassLoadingTime = 0;
    }

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
            if (lastIndex != -1 && connection instanceof JarURLConnection) {
                JarURLConnection jarConnection = (JarURLConnection) connection;
                JarFile file = jarConnection.getJarFile();

                // The URL provided by getResource points inside the jar causing foo's client to crash
                url = new URL("file:/" + URLEncoder.encode(file.getName(), "UTF-8"));

                if (file.getManifest() != null) {
                    JarEntry entry = file.getJarEntry(classFileName);
                    if (entry != null) {
                        signers = entry.getCodeSigners();
                    }

                    if (getPackage(packageName) == null) {
                        definePackage(packageName, file.getManifest(), url);
                    }
                }
            } else if (getPackage(packageName) == null) {
                definePackage(packageName, null, null, null, null, null, null, null);
            }
            byte[] rawClass = connection == null ? null : Util.readFully(connection.getInputStream());

            for (ClassTransformer transformer : transformers) {
                rawClass = transformer.transform(rawClass, name);
            }

            if (rawClass == null) {
                throw new ClassNotFoundException(name);
            }

            CodeSource codeSource = url == null ? null : new CodeSource(url, signers);

            Class<?> clazz = defineClass(name, rawClass, 0, rawClass.length, codeSource);
            if (clazz == null) {
                throw new ClassNotFoundException(name);
            }
            return clazz;
        } catch (ClassNotFoundException exception) {
            throw exception;
        } catch (Throwable throwable) {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            throw new RuntimeException(throwable);
        } finally {
            totalClassLoadingTime += System.nanoTime() - start;
        }
    }

    /**
     * Total time taken to load/transform classes, in nanoseconds
     */
    public long getTotalClassLoadingTime() {
        return totalClassLoadingTime;
    }

    public void addTransformer(ClassTransformer transformer) {
        transformers.add(transformer);
        transformers.sort(Comparator.comparingInt(ClassTransformer::getTransformerPriority));
    }

    public void removeTransformer(ClassTransformer transformer) {
        transformers.remove(transformer);
    }

    public List<ClassTransformer> getTransformers() {
        return Collections.unmodifiableList(transformers);
    }

}
