package fr.redstonneur1256.modlib.launcher.file;

import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@ApiStatus.Internal
public class ZipFileProvider implements FileProvider {

    private ZipFile file;

    public ZipFileProvider(ZipFile file) {
        this.file = file;
    }

    @Override
    public boolean exists(String path) {
        return file.getEntry(path) != null;
    }

    @Override
    public InputStream getStream(String path) throws IOException {
        ZipEntry entry = file.getEntry(path);
        return entry == null ? null : file.getInputStream(entry);
    }

}
