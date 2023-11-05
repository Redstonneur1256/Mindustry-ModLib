package fr.redstonneur1256.modlib;

import java.time.Instant;
import java.util.Properties;

public class ModLibProperties {

    public static final String VERSION;
    public static final String BUILD;
    public static final Instant BUILT;

    static {
        try {
            Properties properties = new Properties();
            properties.load(ModLibProperties.class.getResourceAsStream("/modlib.properties"));

            VERSION = properties.getProperty("version");
            BUILD = properties.getProperty("build");
            BUILT = Instant.parse(properties.getProperty("built"));
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

}
