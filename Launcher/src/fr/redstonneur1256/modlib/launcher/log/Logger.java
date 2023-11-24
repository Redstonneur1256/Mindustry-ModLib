package fr.redstonneur1256.modlib.launcher.log;

public class Logger {

    public static void log(String message, Object... args) {
        System.out.printf(message, args);
        System.out.println();
    }

    public static void err(String message, Object... args) {
        System.out.printf(message, args);
        System.out.println();
    }

    public static void err(Throwable throwable) {
        throwable.printStackTrace(System.out);
    }

}
