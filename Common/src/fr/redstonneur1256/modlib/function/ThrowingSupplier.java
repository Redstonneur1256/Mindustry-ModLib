package fr.redstonneur1256.modlib.function;

public interface ThrowingSupplier<T, E extends Throwable> {

    T get() throws E;

}
