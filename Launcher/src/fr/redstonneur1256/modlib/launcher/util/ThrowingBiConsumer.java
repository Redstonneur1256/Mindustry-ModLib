package fr.redstonneur1256.modlib.launcher.util;

public interface ThrowingBiConsumer<T, U, E extends Throwable> {

    void accept(T t, U u) throws E;

}
