package fr.redstonneur1256.modlib.net.call;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Remote {

    @NotNull
    Side side() default Side.BOTH;

    @NotNull
    Execution execution() default Execution.MAIN;

}
