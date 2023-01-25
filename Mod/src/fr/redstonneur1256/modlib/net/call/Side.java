package fr.redstonneur1256.modlib.net.call;

import arc.func.Boolp;
import mindustry.Vars;

public enum Side {

    // Server and client booleans are inverted because they are being called from
    // the proxy class on the opposite side of where we want to call this method.

    /**
     * Server side method, can only be called from the client
     */
    SERVER(() -> Vars.net.client()),
    /**
     * Client side method, can only be called from the server
     */
    CLIENT(() -> Vars.net.server()),
    /**
     * Common method, can be called from both side
     */
    BOTH(() -> Vars.net.active());

    private final Boolp tester;

    Side(Boolp tester) {
        this.tester = tester;
    }

    public boolean available() {
        return tester.get();
    }

}
