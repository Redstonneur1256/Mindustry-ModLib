package fr.redstonneur1256.modlib.launcher.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Util {

    public static byte[] readFully(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(stream.available());
        byte[] buffer = new byte[8192];
        int read;
        while((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

}
