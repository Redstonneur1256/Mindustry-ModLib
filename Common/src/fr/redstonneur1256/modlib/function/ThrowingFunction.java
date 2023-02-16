package fr.redstonneur1256.modlib.function;

public interface ThrowingFunction<T, R, E extends Throwable> {

    R accept(T t) throws E;

}
