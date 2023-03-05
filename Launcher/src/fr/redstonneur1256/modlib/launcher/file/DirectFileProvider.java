package fr.redstonneur1256.modlib.launcher.file;

import org.jetbrains.annotations.ApiStatus;

import java.io.*;
import java.nio.file.Files;

@ApiStatus.Internal
public class DirectFileProvider implements FileProvider {

    private File file;

    public DirectFileProvider(File file) {
        this.file = file;
    }

    @Override
    public boolean exists(String path) {
        return new File(file, path).exists();
    }

    @Override
    public InputStream getStream(String path) throws IOException {
        File file = new File(this.file, path);
        return file.exists() ? Files.newInputStream(file.toPath()) : null;
    }

}
