package fr.redstonneur1256.modlib.launcher.file;

import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;

@ApiStatus.Internal
public interface FileProvider {

    boolean exists(String path);

    InputStream getStream(String path) throws IOException;

}
