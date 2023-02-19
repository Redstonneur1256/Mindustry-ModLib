package fr.redstonneur1256.examplemod.net;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.util.Log;
import fr.redstonneur1256.examplemod.ExampleKeyBinds;
import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.net.call.CallResult;
import fr.redstonneur1256.modlib.net.call.Execution;
import fr.redstonneur1256.modlib.net.call.Remote;
import fr.redstonneur1256.modlib.net.call.Side;
import fr.redstonneur1256.modlib.net.io.MTypeIO;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Player;

import java.util.concurrent.TimeUnit;

public class CustomCallExample {

    public interface CustomCall {

        /**
         * All methods must return {@link CallResult} or void, methods that must be available on server side
         * must have {@link Player} as their first parameter.
         * <p>
         * Note that if you want to use custom types as parameter or return type you will need to register them with
         * the {@link MTypeIO} class
         * <p>
         * The {@link Remote} annotation is optional.
         * It can be used to change on which thread the method should be called and allows to limit on which sides
         * the method might be called. Trying to call a method from a side where is disallowed will result in
         * an {@link IllegalStateException} being thrown.
         */
        @Remote(side = Side.SERVER, execution = Execution.MAIN)
        CallResult<Boolean> isFoo(Player player, String text);

    }

    public static void init() {
        MVars.net.registerCall(CustomCall.class, new CustomCall() {
            @Override
            public CallResult<Boolean> isFoo(Player player, String text) {
                System.out.println("isFoo() got called from player " + player.name + " with text " + text);
                if("foo".equals(text)) {
                    return CallResult.of(true);
                }
                // Exceptions can either be thrown directly or by returning CallResult#failed(Throwable)
                throw new RuntimeException("it's not foo");
            }
        });

        // For the client side, every game tick:
        Events.run(EventType.Trigger.update, () -> {
            // Check if the key J has been tapped
            if(Core.input.keyTap(ExampleKeyBinds.demo) && Vars.net.client()) {

                // From client side we need to check if the call class is available on the server using MVars.net.isCallAvailable
                // Trying to use an unavailable call class will lead in a NoSuchMethodError
                if(!MVars.net.isCallAvailable(CustomCall.class)) {
                    Log.warn("The custom call class is not available on the server, unable to call the method");
                    return;
                }

                // Obtain our custom call:
                CustomCall call = MVars.net.getCall(CustomCall.class);
                // Invoke the method (on client side passing the player parameter is ignored, on server side it will call at the player)
                CallResult<Boolean> result = call.isFoo(null, Core.input.keyDown(KeyCode.h) ? "notFoo" : "foo");
                // Listen for the result
                result.listen(
                        is -> Log.info("Is it foo ? @", is),
                        err -> Log.err("It might not be foo", err),
                        () -> Log.warn("too long, ignoring result"),
                        1, TimeUnit.SECONDS
                );
            }
        });
    }

}
