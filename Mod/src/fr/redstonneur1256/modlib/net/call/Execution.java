package fr.redstonneur1256.modlib.net.call;

import arc.func.Cons;
import fr.redstonneur1256.modlib.MVars;

public enum Execution {

    /**
     * Code will be executed on main thread using {@code Core.app.post(Runnable)}
     */
    // Packets are directly being handled from the main thread, we just need to forward the execution
    MAIN(Runnable::run),
    /**
     * Code will be executed in an uncapped thread pool
     */
    POOLED(runnable -> MVars.net.getExecutor().submit(runnable));

    public final Cons<Runnable> action;

    Execution(Cons<Runnable> action) {
        this.action = action;
    }

    public void execute(Runnable runnable) {
        action.get(runnable);
    }

}
